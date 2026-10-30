/**
 * RC17: pure, dependency-injected account deletion safety core.
 *
 * NOT a Supabase Edge Function, NOT deployed, NO service credentials, and
 * deliberately cannot touch a live database on its own.
 *
 * A production adapter must enforce a persistent write fence (RLS + Storage)
 * before returning from lockAccount. Do not connect this core to a live
 * endpoint without proving that condition on a nonproduction Supabase project.
 */
export interface DeleteRequest {
  readonly accountId: string; // untrusted; must match the independently verified identity
  readonly requestId: string;
  readonly confirmation: string;
}

export interface VerifiedIdentity {
  readonly userId: string; // verified by server-side Auth, never JWT user_metadata
  readonly reauthenticatedAtMs: number; // MUST come from independent server reauth proof
}

export interface RemainingRows {
  readonly profiles: number;
  readonly heads: number;
  readonly snapshots: number;
}

export interface AccountDeletionAdapter {
  verifyFreshIdentity(): Promise<VerifiedIdentity>;
  nowMs(): number;
  /** MUST persist a write fence protecting BOTH public rows and private Storage. */
  lockAccount(userId: string, requestId: string): Promise<void>;
  /** Return at most limit paths. No offset: deletions shift pagination. */
  listVoiceObjects(userId: string, limit: number): Promise<string[]>;
  /** Remove exactly the paths supplied; fail on partial deletion. */
  removeVoiceObjects(paths: readonly string[]): Promise<void>;
  deleteHeads(userId: string): Promise<void>;
  deleteSnapshots(userId: string): Promise<void>;
  deleteProfile(userId: string): Promise<void>;
  remainingRows(userId: string): Promise<RemainingRows>;
  deleteAuthIdentity(userId: string): Promise<void>;
  authIdentityExists(userId: string): Promise<boolean>;
  /** Persist a durable completion receipt NOT coupled by FK to auth.users. */
  confirmComplete(userId: string, requestId: string): Promise<void>;
}

export class DeletionBlocked extends Error {
  constructor(message: string) {
    super(message);
    this.name = "DeletionBlocked";
  }
}

const USER_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const REQUEST_ID = /^[a-zA-Z0-9_-]{12,100}$/;
const CONFIRMATION = "DELETE BACKLOT";
const REAUTH_MAX_AGE_MS = 5 * 60_000;
const MAX_VOICE_OBJECTS = 10_000;
const BATCH_SIZE = 100;

/** Prevent unintended sibling account data from EVER reaching the delete adapter. */
export function assertOwnedVoicePath(userId: string, path: string): void {
  if (!USER_ID.test(userId) || !path.startsWith(userId + "/")) {
    throw new DeletionBlocked("Refusing voice object outside the verified account");
  }
  const parts = path.split("/");
  if (parts.length < 4 ||
    parts.some((part) => !/^[A-Za-z0-9._-]{1,160}$/.test(part) ||
      part === "." || part === "..")) {
    throw new DeletionBlocked("Refusing malformed voice object path");
  }
}

/**
 * Strict ordering:
 * 1. independently verify account and recent reauthentication
 * 2. durable server-side write fence
 * 3. remove private Storage through its API (NOT direct storage.objects SQL)
 * 4. delete sync heads, then snapshots (FK RESTRICT), then profile
 * 5. verify absolutely no user-owned records remain
 * 6. delete Auth LAST and verify disappearance
 * 7. mark the independent request completed
 *
 * On ANY failure, this function throws. It never reports "deleted" early.
 * A future production worker must durably record failures and retry safely.
 * Reauth, locking, retry persistence and rate limits remain adapter requirements.
 */
export async function deleteAccountInOrder(
  request: DeleteRequest,
  adapter: AccountDeletionAdapter,
): Promise<{ status: "completed"; requestId: string }> {
  if (request.confirmation !== CONFIRMATION) {
    throw new DeletionBlocked("Explicit account-deletion confirmation required");
  }
  if (!REQUEST_ID.test(request.requestId) || !USER_ID.test(request.accountId)) {
    throw new DeletionBlocked("Invalid account or deletion request ID");
  }
  const verified = await adapter.verifyFreshIdentity();
  if (verified.userId !== request.accountId || !USER_ID.test(verified.userId)) {
    throw new DeletionBlocked("Authenticated account does not match deletion target");
  }
  const age = adapter.nowMs() - verified.reauthenticatedAtMs;
  if (!Number.isFinite(age) || age < 0 || age > REAUTH_MAX_AGE_MS) {
    throw new DeletionBlocked("Fresh server-verified reauthentication is required");
  }

  // A real adapter MUST refuse to return until it proves the write fence is effective.
  await adapter.lockAccount(verified.userId, request.requestId);

  let removed = 0;
  while (true) {
    const paths = await adapter.listVoiceObjects(verified.userId, BATCH_SIZE);
    if (paths.length > BATCH_SIZE) {
      throw new DeletionBlocked("Unbounded voice-storage enumeration refused");
    }
    if (paths.length === 0) break;
    if (removed + paths.length > MAX_VOICE_OBJECTS) {
      throw new DeletionBlocked("Account exceeds automatic deletion limit; operator review required");
    }
    const unique = new Set(paths);
    if (unique.size !== paths.length) {
      throw new DeletionBlocked("Duplicate objects from storage pagination");
    }
    paths.forEach((path) => assertOwnedVoicePath(verified.userId, path));
    await adapter.removeVoiceObjects(paths);
    removed += paths.length;
  }
  if ((await adapter.listVoiceObjects(verified.userId, BATCH_SIZE)).length !== 0) {
    throw new DeletionBlocked("Voice storage still contains account media");
  }

  await adapter.deleteHeads(verified.userId);
  await adapter.deleteSnapshots(verified.userId);
  await adapter.deleteProfile(verified.userId);
  const remaining = await adapter.remainingRows(verified.userId);
  if (remaining.heads !== 0 || remaining.snapshots !== 0 ||
      remaining.profiles !== 0) {
    throw new DeletionBlocked("Creator account rows still exist; Auth deletion refused");
  }
  // A second storage verification catches accidental repopulation before Auth deletion.
  if ((await adapter.listVoiceObjects(verified.userId, BATCH_SIZE)).length !== 0) {
    throw new DeletionBlocked("Voice media reappeared; write fence is not effective");
  }

  await adapter.deleteAuthIdentity(verified.userId);
  if (await adapter.authIdentityExists(verified.userId)) {
    throw new DeletionBlocked("Authentication identity was not deleted");
  }
  await adapter.confirmComplete(verified.userId, request.requestId);
  return { status: "completed", requestId: request.requestId };
}
