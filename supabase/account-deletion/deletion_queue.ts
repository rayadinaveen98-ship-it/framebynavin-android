/**
 * RC18: offline durable deletion-request / retry WORKFLOW CONTRACT.
 * NOT an Edge Function. No SQL adapter, secrets, or live deployment.
 * Persistent store must implement atomic compare-and-set and fenced leases.
 */
import { assertOwnedVoicePath, DeletionBlocked } from "./deletion_core.ts";

export const STAGES = [
  "fence", "voice_media", "heads", "snapshots", "profile",
  "verify_data", "auth", "verify_auth", "complete",
] as const;
export type DeletionStage = typeof STAGES[number];
export type DeletionStatus =
  | "requested" | "processing" | "retry_wait" | "needs_support" | "completed";

export interface Job {
  readonly id: string;
  readonly userId: string;
  readonly status: DeletionStatus;
  readonly stage: DeletionStage;
  readonly attempts: number;
  readonly retryAfterMs: number;
  readonly completedAtMs: number | null;
}
export interface Lease {
  readonly job: Job;
  readonly token: string; // secure, opaque, store-generated fencing token
}
/** DB implementation must guarantee globally unique IDs and CAS on token + stage. */
export interface DeletionQueueStore {
  insertOrGet(id: string, verifiedUser: string): Promise<Job>;
  claim(id: string, nowMs: number, leaseDurationMs: number): Promise<Lease | null>;
  advance(id: string, token: string, expected: DeletionStage, next: DeletionStage): Promise<void>;
  defer(id: string, token: string, stage: DeletionStage, retryAtMs: number, needsSupport: boolean): Promise<void>;
  complete(id: string, token: string, nowMs: number): Promise<void>;
  readOwned(id: string, verifiedUser: string): Promise<Job | null>;
}
/** Privileged service adapter ONLY. Operations must be safe when retried. */
export interface DeletionActions {
  establishDurableWriteFence(userId: string): Promise<void>; // database AND Storage writes
  listVoiceMedia(userId: string, limit: number): Promise<string[]>;
  removeVoiceMedia(paths: readonly string[]): Promise<void>;
  removeHeads(userId: string): Promise<void>;
  removeSnapshots(userId: string): Promise<void>;
  removeProfile(userId: string): Promise<void>;
  remainingRows(userId: string): Promise<{ heads: number; snapshots: number; profiles: number }>;
  deleteAuthIfExists(userId: string): Promise<void>; // absent is success
  authExists(userId: string): Promise<boolean>;
}
export interface VerifiedUser {
  readonly id: string;
  readonly reauthenticatedAtMs: number; // server proof, never JWT user_metadata
}

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const OPAQUE = /^[A-Za-z0-9_-]{12,100}$/;
const MAX_AGE_MS = 300_000;
const LEASE_MS = 120_000;
const MAX_ATTEMPTS = 5;
const BATCH = 100;
const MAX_MEDIA = 10_000;

export async function submitDeletionRequest(
  input: { readonly id: string; readonly accountId: string; readonly confirmation: string },
  verified: VerifiedUser,
  nowMs: number,
  store: DeletionQueueStore,
): Promise<Job> {
  if (input.confirmation !== "DELETE BACKLOT" || !OPAQUE.test(input.id) ||
      !UUID.test(input.accountId) || !UUID.test(verified.id) || input.accountId !== verified.id) {
    throw new DeletionBlocked("Invalid request or independently verified user");
  }
  const age = nowMs - verified.reauthenticatedAtMs;
  if (!Number.isFinite(age) || age < 0 || age > MAX_AGE_MS) {
    throw new DeletionBlocked("Fresh server-verified reauthentication required");
  }
  const row = await store.insertOrGet(input.id, verified.id);
  if (row.id !== input.id || row.userId !== verified.id) {
    throw new DeletionBlocked("Request idempotency key belongs to a different account");
  }
  return row;
}

export async function readDeletionStatus(
  id: string, verifiedUser: string, store: DeletionQueueStore,
): Promise<Job | null> {
  if (!OPAQUE.test(id) || !UUID.test(verifiedUser)) {
    throw new DeletionBlocked("Invalid request identity");
  }
  const result = await store.readOwned(id, verifiedUser);
  if (result && (result.id !== id || result.userId !== verifiedUser)) {
    throw new DeletionBlocked("Cross-account status disclosure refused");
  }
  return result;
}

/**
 * Worker can resume after Auth has been removed: authority comes from the
 * immutable, server-verified, persistently recorded owner and fenced lease.
 * A fresh client JWT is NOT required for the final steps of an accepted job.
 *
 * The queue implementation must reject expired/stale tokens atomically.
 * API/server rate-limits, persistent fences, privileged keys, alerts and
 * actual database adapters remain unimplemented; this core cannot go live.
 */
export async function processDeletionOnce(
  requestId: string,
  nowMs: number,
  store: DeletionQueueStore,
  actions: DeletionActions,
): Promise<"not_claimed" | "completed" | "retry_scheduled" | "needs_support"> {
  if (!OPAQUE.test(requestId) || !Number.isFinite(nowMs) || nowMs <= 0) {
    throw new DeletionBlocked("Invalid work item");
  }
  const lease = await store.claim(requestId, nowMs, LEASE_MS);
  if (!lease) return "not_claimed";
  const { job, token } = lease;
  if (job.id !== requestId || !UUID.test(job.userId) ||
      job.status !== "processing" || !OPAQUE.test(token) ||
      !Number.isInteger(job.attempts) || job.attempts < 1 || job.attempts > MAX_ATTEMPTS ||
      !STAGES.includes(job.stage)) {
    throw new DeletionBlocked("Invalid or unauthorized deletion lease");
  }
  let stage: DeletionStage = job.stage;
  try {
    for (;;) {
      const user = job.userId;
      switch (stage) {
        case "fence":
          await actions.establishDurableWriteFence(user);
          break;
        case "voice_media": {
          let removed = 0;
          for (;;) {
            const paths = await actions.listVoiceMedia(user, BATCH);
            if (paths.length > BATCH || new Set(paths).size !== paths.length ||
                removed + paths.length > MAX_MEDIA) {
              throw new DeletionBlocked("Unsafe Storage pagination");
            }
            if (paths.length === 0) break;
            for (const path of paths) assertOwnedVoicePath(user, path);
            await actions.removeVoiceMedia(paths);
            removed += paths.length;
          }
          break;
        }
        case "heads":
          await actions.removeHeads(user);
          break;
        case "snapshots":
          await actions.removeSnapshots(user);
          break;
        case "profile":
          await actions.removeProfile(user);
          break;
        case "verify_data": {
          const rows = await actions.remainingRows(user);
          if (rows.heads !== 0 || rows.snapshots !== 0 || rows.profiles !== 0) {
            throw new DeletionBlocked("Creator rows remain");
          }
          if ((await actions.listVoiceMedia(user, BATCH)).length !== 0) {
            throw new DeletionBlocked("Voice media remains after cleanup");
          }
          break;
        }
        case "auth":
          await actions.deleteAuthIfExists(user);
          break;
        case "verify_auth":
          if (await actions.authExists(user)) {
            throw new DeletionBlocked("Auth account still present");
          }
          break;
        case "complete":
          await store.complete(requestId, token, nowMs);
          return "completed";
      }
      const next = STAGES[STAGES.indexOf(stage) + 1];
      if (!next) throw new DeletionBlocked("Unrecognized deletion stage");
      await store.advance(requestId, token, stage, next);
      stage = next;
    }
  } catch (_error) {
    // Failed requests stay visible and retryable; never report success early.
    const needsSupport = job.attempts >= MAX_ATTEMPTS;
    const backoffMs = Math.min(3_600_000, 30_000 * 2 ** (job.attempts - 1));
    await store.defer(requestId, token, stage, nowMs + backoffMs, needsSupport);
    return needsSupport ? "needs_support" : "retry_scheduled";
  }
}
