// deno-lint-ignore-file require-await
// In-memory adapter intentionally returns synchronously in async methods to match the production interface.
import {
  assertOwnedVoicePath,
  deleteAccountInOrder,
  DeletionBlocked,
  type AccountDeletionAdapter,
  type DeleteRequest,
  type RemainingRows,
  type VerifiedIdentity,
} from "./deletion_core.ts";

const OWNER = "0fdd35b2-4929-4b87-bccc-c27d46de048b";
const OTHER = "fdba0e6f-3cdb-4230-9f20-40c3b1fe265b";
const NOW = 1_900_000_000_000;
const REQUEST: DeleteRequest = {
  accountId: OWNER,
  requestId: "rc17-request-123456",
  confirmation: "DELETE BACKLOT",
};

function check(ok: unknown, message: string): asserts ok {
  if (!ok) throw new Error(message);
}
async function blocked(action: () => Promise<unknown>): Promise<void> {
  try {
    await action();
    throw new Error("Expected fail-closed deletion");
  } catch (error) {
    check(error instanceof DeletionBlocked, "Unexpected result: " + String(error));
  }
}
function media(user: string, number: number): string {
  return user + "/idea-" + number + "/media/" + String(number).padStart(64, "0") + ".m4a";
}

class Fake implements AccountDeletionAdapter {
  identity: VerifiedIdentity = { userId: OWNER, reauthenticatedAtMs: NOW - 30_000 };
  files = new Set<string>();
  rows = new Map<string, RemainingRows>([
    [OWNER, { heads: 1, snapshots: 5, profiles: 1 }],
    [OTHER, { heads: 1, snapshots: 2, profiles: 1 }],
  ]);
  users = new Set([OWNER, OTHER]);
  calls: string[] = [];
  completed = false;
  failStage: string | null = null;
  listOverride: ((user: string, limit: number) => string[]) | null = null;

  nowMs(): number { return NOW; }
  async verifyFreshIdentity(): Promise<VerifiedIdentity> {
    this.calls.push("verify");
    return this.identity;
  }
  private at(stage: string): void {
    this.calls.push(stage);
    if (this.failStage === stage) throw new Error("Simulated " + stage + " failure");
  }
  async lockAccount(user: string, requestId: string): Promise<void> {
    check(user === OWNER && requestId === REQUEST.requestId, "wrong lock target");
    this.at("lock");
  }
  async listVoiceObjects(user: string, limit: number): Promise<string[]> {
    this.at("list");
    if (this.listOverride) return this.listOverride(user, limit);
    return [...this.files].filter((p) => p.startsWith(user + "/")).slice(0, limit);
  }
  async removeVoiceObjects(paths: readonly string[]): Promise<void> {
    this.at("remove");
    for (const path of paths) {
      check(path.startsWith(OWNER + "/"), "must not delete sibling media");
      check(this.files.delete(path), "missing listed object");
    }
  }
  async deleteHeads(user: string): Promise<void> {
    this.at("heads");
    this.rows.set(user, { ...this.get(user), heads: 0 });
  }
  async deleteSnapshots(user: string): Promise<void> {
    this.at("snapshots");
    check(this.get(user).heads === 0, "heads must precede snapshots (FK RESTRICT)");
    this.rows.set(user, { ...this.get(user), snapshots: 0 });
  }
  async deleteProfile(user: string): Promise<void> {
    this.at("profile");
    this.rows.set(user, { ...this.get(user), profiles: 0 });
  }
  async remainingRows(user: string): Promise<RemainingRows> {
    this.at("verifyRows");
    return this.get(user);
  }
  private get(user: string): RemainingRows {
    return this.rows.get(user) ?? { heads: 0, snapshots: 0, profiles: 0 };
  }
  async deleteAuthIdentity(user: string): Promise<void> {
    this.at("deleteAuth");
    this.users.delete(user);
  }
  async authIdentityExists(user: string): Promise<boolean> {
    this.at("verifyAuth");
    return this.users.has(user);
  }
  async confirmComplete(user: string, requestId: string): Promise<void> {
    check(user === OWNER && requestId === REQUEST.requestId, "wrong completion target");
    this.at("complete");
    this.completed = true;
  }
}

Deno.test("deletes OWN voice media in batches; DB heads before snapshots; Auth last", async () => {
  const a = new Fake();
  for (let i = 0; i < 250; i++) a.files.add(media(OWNER, i));
  a.files.add(media(OTHER, 9));
  const done = await deleteAccountInOrder(REQUEST, a);
  check(done.status === "completed" && a.completed, "no completion receipt");
  check(!a.users.has(OWNER) && a.users.has(OTHER), "sibling Auth identity was changed");
  check(a.files.size === 1 && a.files.has(media(OTHER, 9)), "sibling storage was changed");
  check(a.rows.get(OTHER)?.snapshots === 2, "sibling Creator records were changed");
  const stages = ["lock", "remove", "heads", "snapshots", "profile", "verifyRows", "deleteAuth", "verifyAuth", "complete"];
  let previous = -1;
  for (const stage of stages) {
    const index = a.calls.indexOf(stage);
    check(index > previous, "unexpected deletion order: " + stage);
    previous = index;
  }
  check(a.calls.filter((s) => s === "remove").length === 3, "batching skipped media");
});

Deno.test("rejects arbitrary client account ID before lock or any mutation", async () => {
  const a = new Fake();
  await blocked(() => deleteAccountInOrder({ ...REQUEST, accountId: OTHER }, a));
  check(!a.calls.includes("lock"), "unverified deletion was locked");
});

Deno.test("requires exact deliberate confirmation", async () => {
  const a = new Fake();
  await blocked(() => deleteAccountInOrder({ ...REQUEST, confirmation: "DELETE" }, a));
  check(a.calls.length === 0, "confirmation should be first");
});

Deno.test("requires independent fresh server reauthentication", async () => {
  const a = new Fake();
  a.identity = { userId: OWNER, reauthenticatedAtMs: NOW - 360_000 };
  await blocked(() => deleteAccountInOrder(REQUEST, a));
  check(!a.calls.includes("lock"), "stale session triggered destructive work");
});

Deno.test("rejects malformed or invalid request IDs before identity", async () => {
  const a = new Fake();
  await blocked(() => deleteAccountInOrder({ ...REQUEST, requestId: "short" }, a));
  check(a.calls.length === 0, "invalid ID was accepted");
});

Deno.test("rejects sibling Storage paths without deleting anything", async () => {
  const a = new Fake();
  a.listOverride = () => [media(OTHER, 1)];
  await blocked(() => deleteAccountInOrder(REQUEST, a));
  check(!a.calls.includes("remove") && a.users.has(OWNER), "cross-user deletion was possible");
});

Deno.test("rejects path traversal and invalid objects", () => {
  for (const path of [
    OWNER + "/../media/secret.m4a",
    OWNER + "/idea-1/media/../../secret.m4a",
    OWNER + "/a\\b/media/file.m4a",
    OTHER + "/idea-1/media/file.m4a",
  ]) {
    try {
      assertOwnedVoicePath(OWNER, path);
      throw new Error("malicious path accepted");
    } catch (error) {
      check(error instanceof DeletionBlocked, "bad rejection for " + path);
    }
  }
});

Deno.test("rejects duplicate storage page before removal", async () => {
  const a = new Fake();
  a.listOverride = () => [media(OWNER, 1), media(OWNER, 1)];
  await blocked(() => deleteAccountInOrder(REQUEST, a));
  check(!a.calls.includes("remove"), "duplicate paths were accepted");
});

Deno.test("rejects storage enumeration larger than batch bound", async () => {
  const a = new Fake();
  a.listOverride = () => Array.from({ length: 101 }, (_, i) => media(OWNER, i));
  await blocked(() => deleteAccountInOrder(REQUEST, a));
  check(!a.calls.includes("remove"), "unbounded listing was processed");
});

Deno.test("Storage remove failure must leave Auth identity intact", async () => {
  const a = new Fake();
  a.files.add(media(OWNER, 1));
  a.failStage = "remove";
  let failed = false;
  try { await deleteAccountInOrder(REQUEST, a); } catch { failed = true; }
  check(failed && a.users.has(OWNER) && !a.completed, "premature deletion completion");
});

Deno.test("database head failure blocks Auth deletion", async () => {
  const a = new Fake();
  a.failStage = "heads";
  let failed = false;
  try { await deleteAccountInOrder(REQUEST, a); } catch { failed = true; }
  check(failed && !a.calls.includes("deleteAuth"), "Auth deleted after database error");
});

Deno.test("residual Creator rows block Auth deletion", async () => {
  const a = new Fake();
  a.remainingRows = async () => ({ profiles: 0, heads: 0, snapshots: 1 });
  await blocked(() => deleteAccountInOrder(REQUEST, a));
  check(a.users.has(OWNER) && !a.completed, "Auth deleted before zero-row proof");
});

Deno.test("remaining voice media detected after DB cleanup blocks Auth deletion", async () => {
  const a = new Fake();
  let listCalls = 0;
  a.listOverride = () => {
    listCalls++;
    if (listCalls <= 2) return [];
    return [media(OWNER, 11)];
  };
  await blocked(() => deleteAccountInOrder(REQUEST, a));
  check(a.users.has(OWNER) && !a.completed, "storage race wasn't blocked");
});

Deno.test("Auth deletion failure is never marked completed", async () => {
  const a = new Fake();
  a.failStage = "deleteAuth";
  let failed = false;
  try { await deleteAccountInOrder(REQUEST, a); } catch { failed = true; }
  check(failed && !a.completed, "Auth deletion failure marked completed");
});

Deno.test("Auth still present after delete API returns blocks completion receipt", async () => {
  const a = new Fake();
  a.deleteAuthIdentity = async () => { a.calls.push("deleteAuth"); };
  await blocked(() => deleteAccountInOrder(REQUEST, a));
  check(!a.completed, "false completion receipt");
});
