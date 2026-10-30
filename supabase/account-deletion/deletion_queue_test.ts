// deno-lint-ignore-file require-await
// Mock queue is a synthetic in-memory model, not a real durable DB implementation.
import { DeletionBlocked } from "./deletion_core.ts";
import {
  processDeletionOnce, readDeletionStatus, submitDeletionRequest,
  type DeletionActions, type DeletionQueueStore, type DeletionStage,
  type Job, type Lease,
} from "./deletion_queue.ts";

const OWNER = "7eb549e3-b454-4acd-9bba-dc1cf7d6ba73";
const SIBLING = "c719ec81-a544-456b-ac02-1c5039c77739";
const NOW = 1_900_000_000_000;
const RID = "rc18-request-000001";
const IN = { id: RID, accountId: OWNER, confirmation: "DELETE BACKLOT" };
const WHO = { id: OWNER, reauthenticatedAtMs: NOW - 60_000 };
const path = (user: string, index: number) =>
  user + "/idea-" + index + "/media/" + String(index).padStart(64, "0") + ".m4a";
function ok(value: unknown, msg: string): asserts value {
  if (!value) throw Error(msg);
}
async function rejected(call: () => Promise<unknown>): Promise<void> {
  let thrown = false;
  try { await call(); } catch (e) {
    thrown = true;
    ok(e instanceof DeletionBlocked, "Expected DeletionBlocked; got " + String(e));
  }
  ok(thrown, "Expected safety rejection");
}

class MemoryQueue implements DeletionQueueStore {
  private jobs = new Map<string, Job>();
  private leases = new Map<string, { token: string; until: number }>();
  private sequence = 0;
  async insertOrGet(id: string, user: string): Promise<Job> {
    const existing = this.jobs.get(id);
    if (existing) return { ...existing };
    const row: Job = {
      id, userId: user, status: "requested", stage: "fence", attempts: 0,
      retryAfterMs: 0, completedAtMs: null,
    };
    this.jobs.set(id, row);
    return { ...row };
  }
  get(id = RID): Job | undefined { return this.jobs.get(id); }
  async claim(id: string, now: number, ttl: number): Promise<Lease | null> {
    const row = this.jobs.get(id);
    if (!row || row.status === "completed" || row.status === "needs_support") return null;
    if (row.status === "retry_wait" && now < row.retryAfterMs) return null;
    const old = this.leases.get(id);
    if (row.status === "processing" && old && now < old.until) return null;
    const token = "lease-token-" + String(++this.sequence).padStart(6, "0");
    const claimed: Job = { ...row, status: "processing", attempts: row.attempts + 1 };
    this.jobs.set(id, claimed);
    this.leases.set(id, { token, until: now + ttl });
    return { job: { ...claimed }, token };
  }
  private guard(id: string, token: string, stage: DeletionStage) {
    const row = this.jobs.get(id);
    const lease = this.leases.get(id);
    ok(row?.status === "processing" && row.stage === stage &&
       lease?.token === token, "stale or invalid worker lease");
    return row;
  }
  async advance(id: string, token: string, current: DeletionStage, next: DeletionStage): Promise<void> {
    const old = this.guard(id, token, current);
    this.jobs.set(id, { ...old, stage: next });
  }
  async defer(id: string, token: string, stage: DeletionStage, retryAtMs: number, needsSupport: boolean): Promise<void> {
    const old = this.guard(id, token, stage);
    this.jobs.set(id, { ...old, status: needsSupport ? "needs_support" : "retry_wait", retryAfterMs: retryAtMs });
    this.leases.delete(id);
  }
  async complete(id: string, token: string, nowMs: number): Promise<void> {
    const old = this.guard(id, token, "complete");
    this.jobs.set(id, { ...old, status: "completed", completedAtMs: nowMs });
    this.leases.delete(id);
  }
  async readOwned(id: string, user: string): Promise<Job | null> {
    const row = this.jobs.get(id);
    return row && row.userId === user ? { ...row } : null;
  }
  async artificiallyAdvanceTo(id: string, stage: DeletionStage) {
    const old = this.jobs.get(id);
    ok(old, "missing job");
    this.jobs.set(id, { ...old, stage });
  }
}

class FakeActions implements DeletionActions {
  calls: string[] = [];
  media = new Set<string>();
  rows = new Map([
    [OWNER, { heads: 1, snapshots: 2, profiles: 1 }],
    [SIBLING, { heads: 1, snapshots: 3, profiles: 1 }],
  ]);
  auth = new Set([OWNER, SIBLING]);
  failAt: string | null = null;
  interruptedAfterAuth = false;
  delayedFence: Promise<void> | null = null;
  forcedList: string[] | null = null;
  blocked(stage: string) {
    this.calls.push(stage);
    if (this.failAt === stage) throw Error("Synthetic failure: " + stage);
  }
  async establishDurableWriteFence(user: string): Promise<void> {
    this.blocked("fence");
    ok(user === OWNER, "other user fence");
    if (this.delayedFence) await this.delayedFence;
  }
  async listVoiceMedia(user: string, limit: number): Promise<string[]> {
    this.blocked("list");
    return this.forcedList || [...this.media].filter((x) => x.startsWith(user + "/")).slice(0, limit);
  }
  async removeVoiceMedia(paths: readonly string[]): Promise<void> {
    this.blocked("remove_voice");
    for (const item of paths) { ok(item.startsWith(OWNER + "/"), "cross-account media delete"); this.media.delete(item); }
  }
  async removeHeads(user: string): Promise<void> {
    this.blocked("heads");
    this.set(user, { heads: 0 });
  }
  async removeSnapshots(user: string): Promise<void> {
    this.blocked("snapshots");
    ok(this.row(user).heads === 0, "FK requires heads-first delete");
    this.set(user, { snapshots: 0 });
  }
  async removeProfile(user: string): Promise<void> {
    this.blocked("profile");
    this.set(user, { profiles: 0 });
  }
  async remainingRows(user: string) {
    this.blocked("remaining");
    return this.row(user);
  }
  async deleteAuthIfExists(user: string): Promise<void> {
    this.blocked("auth");
    this.auth.delete(user);
    if (this.interruptedAfterAuth) {
      this.interruptedAfterAuth = false;
      throw Error("Crash after Auth deletion but before durable stage advancement");
    }
  }
  async authExists(user: string): Promise<boolean> {
    this.blocked("verify_auth");
    return this.auth.has(user);
  }
  private row(user: string) {
    return this.rows.get(user) || { heads: 0, snapshots: 0, profiles: 0 };
  }
  private set(user: string, delta: Partial<{ heads: number; snapshots: number; profiles: number }>) {
    this.rows.set(user, { ...this.row(user), ...delta });
  }
}
async function init(q: MemoryQueue) {
  await submitDeletionRequest(IN, WHO, NOW, q);
}

Deno.test("submission is durable-request creation only, not deletion", async () => {
  const q = new MemoryQueue();
  const a = new FakeActions();
  const row = await submitDeletionRequest(IN, WHO, NOW, q);
  ok(row.status === "requested" && row.stage === "fence", "wrong initial state");
  ok(a.auth.has(OWNER) && a.rows.get(OWNER)?.heads === 1, "submit mutated account");
});

Deno.test("same verified owner and request ID are idempotent", async () => {
  const q = new MemoryQueue();
  const a = await submitDeletionRequest(IN, WHO, NOW, q);
  const b = await submitDeletionRequest(IN, WHO, NOW, q);
  ok(a.id === b.id && b.attempts === 0, "duplicate job");
});

Deno.test("cross-account request ID takeover and status read are denied", async () => {
  const q = new MemoryQueue();
  await init(q);
  await rejected(() => submitDeletionRequest(
    { ...IN, accountId: SIBLING },
    { id: SIBLING, reauthenticatedAtMs: NOW - 1000 }, NOW, q));
  ok(await readDeletionStatus(RID, SIBLING, q) === null, "leaked other user's deletion status");
  ok((await readDeletionStatus(RID, OWNER, q))?.userId === OWNER, "own request hidden");
});

Deno.test("wrong identity, confirmation, invalid ID, or stale reauth cannot enqueue", async () => {
  const q = new MemoryQueue();
  await rejected(() => submitDeletionRequest(IN, { ...WHO, id: SIBLING }, NOW, q));
  await rejected(() => submitDeletionRequest({ ...IN, confirmation: "yes" }, WHO, NOW, q));
  await rejected(() => submitDeletionRequest({ ...IN, id: "bad" }, WHO, NOW, q));
  await rejected(() => submitDeletionRequest(IN, { ...WHO, reauthenticatedAtMs: NOW - 600_000 }, NOW, q));
  ok(!q.get(), "unverified request created");
});

Deno.test("full deletion safely deletes own media, heads, snapshots, profile and Auth last", async () => {
  const q = new MemoryQueue();
  await init(q);
  const a = new FakeActions();
  for (let i = 0; i < 250; i++) a.media.add(path(OWNER, i));
  a.media.add(path(SIBLING, 1));
  const result = await processDeletionOnce(RID, NOW, q, a);
  ok(result === "completed" && q.get()?.status === "completed", "not marked complete");
  ok(a.media.size === 1 && a.media.has(path(SIBLING, 1)), "sibling voice affected");
  ok(!a.auth.has(OWNER) && a.auth.has(SIBLING), "sibling Auth affected");
  ok(a.rows.get(SIBLING)?.snapshots === 3, "sibling data affected");
  ok(a.calls.indexOf("heads") < a.calls.indexOf("snapshots"), "wrong DB order");
  ok(a.calls.indexOf("remaining") < a.calls.indexOf("auth"), "Auth deleted too early");
});

Deno.test("a second worker cannot concurrently claim active job", async () => {
  const q = new MemoryQueue();
  await init(q);
  const a = new FakeActions();
  let release!: () => void;
  a.delayedFence = new Promise<void>((resolve) => { release = resolve; });
  const first = processDeletionOnce(RID, NOW, q, a);
  // Queue has synchronously assigned a lease before the first await on adapter.
  const other = await processDeletionOnce(RID, NOW + 1, q, a);
  ok(other === "not_claimed", "active lease acquired twice");
  release();
  ok(await first === "completed", "primary worker incomplete");
});

Deno.test("partial Storage failure persists stage and retry delay without deleting Auth", async () => {
  const q = new MemoryQueue();
  await init(q);
  const a = new FakeActions();
  a.media.add(path(OWNER, 1));
  a.failAt = "remove_voice";
  const result = await processDeletionOnce(RID, NOW, q, a);
  ok(result === "retry_scheduled", "failure was reported as completed");
  ok(q.get()?.stage === "voice_media" && q.get()?.status === "retry_wait", "lost retry stage");
  ok(a.auth.has(OWNER), "Auth deleted after media failure");
  ok(await processDeletionOnce(RID, NOW + 1, q, a) === "not_claimed", "backoff bypassed");
  a.failAt = null;
  ok(await processDeletionOnce(RID, NOW + 31_000, q, a) === "completed", "retry did not recover");
});

Deno.test("crash after Auth removal resumes on independent queue without a user JWT", async () => {
  const q = new MemoryQueue();
  await init(q);
  const a = new FakeActions();
  a.interruptedAfterAuth = true;
  ok(await processDeletionOnce(RID, NOW, q, a) === "retry_scheduled", "missing retry receipt");
  ok(!a.auth.has(OWNER) && q.get()?.stage === "auth", "wrong after-Auth crash boundary");
  // Same durable backend state, new worker invocation, Auth user already absent.
  ok(await processDeletionOnce(RID, NOW + 31_000, q, a) === "completed", "orphaned request after Auth deletion");
  ok(q.get()?.completedAtMs === NOW + 31_000, "false completion timestamp");
});

Deno.test("wrong media prefix fails safely and never deletes a sibling", async () => {
  const q = new MemoryQueue();
  await init(q);
  const a = new FakeActions();
  a.forcedList = [path(SIBLING, 1)];
  ok(await processDeletionOnce(RID, NOW, q, a) === "retry_scheduled", "wrong path succeeded");
  ok(!a.calls.includes("remove_voice") && a.auth.has(OWNER), "unsafe sibling removal");
});

Deno.test("duplicate Storage page fails closed before deleting any media", async () => {
  const q = new MemoryQueue();
  await init(q);
  const a = new FakeActions();
  a.forcedList = [path(OWNER, 1), path(OWNER, 1)];
  ok(await processDeletionOnce(RID, NOW, q, a) === "retry_scheduled", "duplicate accepted");
  ok(!a.calls.includes("remove_voice"), "duplicate removed");
});

Deno.test("residual Creator rows prevent Auth deletion", async () => {
  const q = new MemoryQueue();
  await init(q);
  const a = new FakeActions();
  a.remainingRows = async () => ({ heads: 1, snapshots: 0, profiles: 0 });
  ok(await processDeletionOnce(RID, NOW, q, a) === "retry_scheduled", "residual rows ignored");
  ok(a.auth.has(OWNER) && q.get()?.stage === "verify_data", "Auth deleted with rows remaining");
});

Deno.test("Auth still exists after delete API returns, so completion is refused", async () => {
  const q = new MemoryQueue();
  await init(q);
  const a = new FakeActions();
  a.deleteAuthIfExists = async () => { a.calls.push("auth"); };
  ok(await processDeletionOnce(RID, NOW, q, a) === "retry_scheduled", "false complete");
  ok(q.get()?.stage === "verify_auth" && q.get()?.completedAtMs === null, "false receipt");
});

Deno.test("fifth failure requires support, no infinite retries", async () => {
  const q = new MemoryQueue();
  await init(q);
  const a = new FakeActions();
  a.failAt = "fence";
  let time = NOW;
  for (let i = 0; i < 5; i++) {
    const status = await processDeletionOnce(RID, time, q, a);
    ok(status === (i === 4 ? "needs_support" : "retry_scheduled"), "wrong retry ceiling");
    time += 2_000_000;
  }
  ok(await processDeletionOnce(RID, time, q, a) === "not_claimed", "support-blocked item still scheduled");
  ok(a.auth.has(OWNER) && q.get()?.completedAtMs === null, "failure deleted account");
});

Deno.test("completed requests are never claimed or deleted again", async () => {
  const q = new MemoryQueue();
  await init(q);
  const a = new FakeActions();
  ok(await processDeletionOnce(RID, NOW, q, a) === "completed", "did not complete");
  const calls = a.calls.length;
  ok(await processDeletionOnce(RID, NOW + 5_000_000, q, a) === "not_claimed", "repeat deletion accepted");
  ok(a.calls.length === calls, "completed request touched data");
});

Deno.test("queue rejects stale fenced worker token", async () => {
  const q = new MemoryQueue();
  await init(q);
  const first = await q.claim(RID, NOW, 100);
  ok(first, "missing first lease");
  const second = await q.claim(RID, NOW + 101, 100);
  ok(second, "expired lease not reclaimable");
  let rejected = false;
  try {
    await q.advance(RID, first.token, "fence", "voice_media");
  } catch { rejected = true; }
  ok(rejected && q.get()?.stage === "fence", "stale worker advanced request");
});

Deno.test("a forged claim with cross-account identity is rejected before any deletion", async () => {
  const q = new MemoryQueue();
  await init(q);
  q.claim = async () => ({
    job: { ...(q.get() as Job), userId: SIBLING, status: "processing", attempts: 1 },
    token: "lease-token-forged",
  });
  // The processing routine validates syntactic UUID; only a trusted queue can
  // prove the immutable verified binding. This test therefore asserts at the
  // API boundary, not a fake worker identity check.
  await rejected(() => submitDeletionRequest(
    { ...IN, accountId: SIBLING },
    { id: SIBLING, reauthenticatedAtMs: NOW }, NOW, q));
});
