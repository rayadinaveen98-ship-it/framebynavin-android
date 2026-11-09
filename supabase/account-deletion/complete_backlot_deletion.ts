/**
 * FC13: extend the existing RC17 Creator deletion core to cover ALL Backlot
 * account-owned Personal and Trust scopes before deleting Supabase Auth.
 *
 * Source-only adapter COMPOSITION, not a deployed Edge Function. The existing
 * deleteAccountInOrder remains the single ordered Auth-last authority; there
 * is no parallel deletion engine or direct client service_role access.
 *
 * A production adapter must implement a durable write fence over every SQL
 * writer + Storage path, prove fresh reauthentication, and provide an
 * independent completion receipt. A network timeout never means "deleted".
 */
import {
  type AccountDeletionAdapter,
  type RemainingRows,
  DeletionBlocked,
  assertOwnedVoicePath,
} from "./deletion_core.ts";

const UUID =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const OWNED_PERSONAL =
  /^([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\/v2-[0-9a-f-]{36}\.zip$/i;
const OWNED_TRUST =
  /^([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\/revisions\/v1-[0-9a-f-]{36}\.enc$/i;

export type OtherDomainKind = "personal_v2" | "trust_backup";
export interface AdditionalRemainingRows {
  readonly personalSyncHeads: number;
  readonly personalArchiveHeads: number;
  readonly trustRevisionHeads: number;
  readonly trustRevisionHistory: number;
  readonly trustOwnerStates: number;
  readonly personalArchiveObjects: number;
  readonly trustBackupObjects: number;
}

/**
 * Adapter MUST list both live and orphaned Storage objects. No offset-based
 * pagination while deleting, as removal shifts subsequent pages.
 *
 * This interface is PRIVILEGED SERVER ONLY. Never add these methods to an
 * authenticated Android client or infer "empty" from a failed query.
 */
export interface CompleteBacklotDomainAdapter {
  /** Must fence Personal JSON/ZIP and Trust metadata + ALL Storage writers. */
  establishAdditionalDurableWriteFences(userId: string, requestId: string): Promise<void>;
  /** Check that write fences actually block an authenticated and privileged writer. */
  attestWriteFencesEffective(userId: string): Promise<boolean>;
  listAdditionalObjects(
    userId: string, kind: OtherDomainKind, limit: number,
  ): Promise<string[]>;
  removeAdditionalObjects(kind: OtherDomainKind, paths: readonly string[]): Promise<void>;
  deletePersonalSyncAndArchiveHeads(userId: string): Promise<void>;
  deleteTrustHeadsHistoryAndFence(userId: string): Promise<void>;
  remainingAdditional(userId: string): Promise<AdditionalRemainingRows>;
  /** Requires an independent durable, non-auth-FK request ID / receipt. */
  confirmAllDomainsDeleted(userId: string, requestId: string): Promise<void>;
}

export function assertAdditionalOwnedPath(
  userId: string,
  kind: OtherDomainKind,
  path: string,
): void {
  if (!UUID.test(userId) || !UUID.test(path.split("/")[0])) {
    throw new DeletionBlocked("Invalid additional Backlot storage owner");
  }
  const pattern = kind === "personal_v2" ? OWNED_PERSONAL : OWNED_TRUST;
  if (!pattern.test(path) || path.split("/")[0].toLowerCase() !== userId.toLowerCase()) {
    throw new DeletionBlocked("Refusing cross-account or unrecognized storage path");
  }
  const part = kind === "personal_v2"
    ? path.substring(userId.length + 4).replace(/\.zip$/i, "")
    : path.split("v1-")[1]?.replace(/\.enc$/i, "");
  if (!part || !UUID.test(part)) {
    throw new DeletionBlocked("Invalid immutable archive identity");
  }
}

/** Do not allow the Auth identity to disappear while ANY Backlot scope remains. */
export function additionalEmpty(data: AdditionalRemainingRows): boolean {
  return Object.values(data).every((count) => Number.isSafeInteger(count) && count === 0);
}

export function extendCreatorAccountDeletionToCompleteBacklot(
  creator: AccountDeletionAdapter,
  additional: CompleteBacklotDomainAdapter,
): AccountDeletionAdapter {
  let boundUser: string | null = null;
  let boundRequest: string | null = null;
  let fullyFenced = false;
  let additionalCleared = false;

  async function ensureEmpty(userId: string): Promise<void> {
    const remaining = await additional.remainingAdditional(userId);
    if (!additionalEmpty(remaining)) {
      throw new DeletionBlocked(
        "Personal or Trust records remain; Auth deletion is prohibited",
      );
    }
  }

  async function removeScoped(
    userId: string,
    kind: OtherDomainKind,
  ): Promise<void> {
    let total = 0;
    for (;;) {
      const names = await additional.listAdditionalObjects(userId, kind, 100);
      if (names.length > 100 || new Set(names).size !== names.length ||
          total + names.length > 10_000) {
        throw new DeletionBlocked("Unsafe Personal/Trust Storage enumeration");
      }
      if (names.length === 0) break;
      names.forEach((name) => assertAdditionalOwnedPath(userId, kind, name));
      // Storage API only. SQL deletion of storage.objects leaves real file bytes.
      await additional.removeAdditionalObjects(kind, names);
      total += names.length;
    }
    if ((await additional.listAdditionalObjects(userId, kind, 1)).length !== 0) {
      throw new DeletionBlocked("Scoped owner objects remain after removal");
    }
  }

  function mustBeFenced(userId: string): void {
    if (!fullyFenced || boundUser !== userId || !boundRequest) {
      throw new DeletionBlocked(
        "Complete Backlot deletion requires a persistent multi-domain write fence",
      );
    }
  }

  return {
    verifyFreshIdentity: () => creator.verifyFreshIdentity(),
    nowMs: () => creator.nowMs(),

    async lockAccount(userId, requestId) {
      // Existing Creator core already validated owner + recent server reauth.
      boundUser = userId;
      boundRequest = requestId;
      await creator.lockAccount(userId, requestId);
      await additional.establishAdditionalDurableWriteFences(userId, requestId);
      if (!await additional.attestWriteFencesEffective(userId)) {
        throw new DeletionBlocked("Personal/Trust database or Storage writes are not fenced");
      }
      fullyFenced = true;
    },

    async listVoiceObjects(userId, limit) {
      mustBeFenced(userId);
      const paths = await creator.listVoiceObjects(userId, limit);
      paths.forEach((path) => assertOwnedVoicePath(userId, path));
      return paths;
    },
    async removeVoiceObjects(paths) {
      if (!boundUser) throw new DeletionBlocked("No verified owner");
      mustBeFenced(boundUser);
      paths.forEach((path) => assertOwnedVoicePath(boundUser!, path));
      await creator.removeVoiceObjects(paths);
    },

    async deleteHeads(userId) {
      mustBeFenced(userId);
      // No SQL head deletion while private archive files still exist.
      await removeScoped(userId, "personal_v2");
      await removeScoped(userId, "trust_backup");
      await additional.deletePersonalSyncAndArchiveHeads(userId);
      await additional.deleteTrustHeadsHistoryAndFence(userId);
      additionalCleared = true;
      await creator.deleteHeads(userId);
    },
    async deleteSnapshots(userId) {
      mustBeFenced(userId);
      if (!additionalCleared) throw new DeletionBlocked("Personal scopes not cleared");
      await creator.deleteSnapshots(userId);
    },
    async deleteProfile(userId) {
      mustBeFenced(userId);
      await ensureEmpty(userId);
      await creator.deleteProfile(userId);
    },
    async remainingRows(userId): Promise<RemainingRows> {
      mustBeFenced(userId);
      await ensureEmpty(userId);
      return creator.remainingRows(userId);
    },
    async deleteAuthIdentity(userId) {
      mustBeFenced(userId);
      await ensureEmpty(userId);
      if ((await additional.listAdditionalObjects(userId, "personal_v2", 1)).length ||
          (await additional.listAdditionalObjects(userId, "trust_backup", 1)).length) {
        throw new DeletionBlocked("Archive Storage reappeared; owner write fence ineffective");
      }
      await creator.deleteAuthIdentity(userId);
    },
    async authIdentityExists(userId) {
      mustBeFenced(userId);
      return creator.authIdentityExists(userId);
    },
    async confirmComplete(userId, requestId) {
      mustBeFenced(userId);
      await ensureEmpty(userId);
      if (await creator.authIdentityExists(userId)) {
        throw new DeletionBlocked("Auth still exists; completion receipt refused");
      }
      await creator.confirmComplete(userId, requestId);
      await additional.confirmAllDomainsDeleted(userId, requestId);
    },
  };
}
