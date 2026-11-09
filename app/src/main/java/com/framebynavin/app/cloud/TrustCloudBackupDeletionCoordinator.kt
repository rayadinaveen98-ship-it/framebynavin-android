package com.framebynavin.app.cloud

/**
 * FC13 owner-reviewed DELETE CLOUD BACKUP (NOT delete Backlot account).
 *
 * All operations are bound to the current refreshed Supabase user identity.
 * Deletion is fenced against new backup revisions before touching Storage.
 * Storage deletions are followed by an independent server-side emptiness check
 * before the revision/head pointers may be removed. Partial failure retains the
 * deletion fence, so no false success or silent resurrection is possible.
 *
 * The UI must not expose this while the review-only SQL contract and private
 * bucket are not installed or until owner identity verification is accepted.
 */
internal class TrustCloudBackupDeletionCoordinator(
    private val accounts: CloudSyncManager,
    private val transport: TrustUniversalCloudTransport = TrustUniversalCloudTransport(),
) {
    internal data class Review(
        val ownerId: String,
        val currentHead: TrustCloudRevisionHead?,
        val revisionFileCount: Int,
    ) {
        val canDelete: Boolean get() = currentHead != null || revisionFileCount > 0
    }

    suspend fun inspectForOwnerReview(): Review = accounts.withFreshSession { session ->
        val owner = session.userId
        TrustT1C4CloudRevisionPolicy.validateOwner(owner)
        check(transport.phase(session) == "active") {
            "An earlier cloud backup delete is incomplete. Do not start a new deletion."
        }
        Review(
            ownerId = owner,
            currentHead = transport.head(session),
            revisionFileCount = transport.listOwnerRevisions(session).size,
        )
    }

    /**
     * The owner must explicitly confirm the SAME head identity previously
     * reviewed. A changed phone/account/head refuses before writing a fence.
     */
    suspend fun deleteOwnerApproved(
        reviewed: Review,
        exactConfirmation: String,
    ): Review = accounts.withFreshSession { session ->
        require(exactConfirmation == "DELETE CLOUD BACKUP") {
            "Type DELETE CLOUD BACKUP to delete your remote encrypted archive"
        }
        check(session.userId == reviewed.ownerId) {
            "The signed-in account changed; nothing was deleted"
        }
        val current = transport.head(session)
        TrustT1C4CloudRevisionPolicy.requireUnchanged(
            reviewed.currentHead, current, session.userId,
        )
        val paths = transport.listOwnerRevisions(session)
        check(paths.size == reviewed.revisionFileCount) {
            "Encrypted file inventory changed; review again before deleting"
        }
        transport.fenceDeleteBackup(session, reviewed.currentHead, exactConfirmation)
        // If a Storage deletion fails, leave the owner in deletion_fenced
        // state for controlled recovery. Never report completion prematurely.
        paths.chunked(100).forEach { batch ->
            transport.removeOwnerRevisionBatch(session, batch)
        }
        transport.finishDeleteBackup(session)
        check(transport.head(session) == null &&
            transport.listOwnerRevisions(session).isEmpty() &&
            transport.phase(session) == "active") {
            "Cloud deletion is not verified yet; do not claim success"
        }
        Review(session.userId, null, 0)
    }

    /** Retry only a previously fenced deletion after explicit owner review. */
    suspend fun finishInterruptedOwnerApproved(
        expectedOwner: String,
        exactConfirmation: String,
    ): Review = accounts.withFreshSession { session ->
        require(exactConfirmation == "DELETE CLOUD BACKUP" &&
            session.userId == expectedOwner) {
            "Owner confirmation and same-account session required"
        }
        check(transport.phase(session) == "deleting_backup") {
            "No interrupted cloud backup deletion was found"
        }
        // Never try to resume while a different account is signed in.
        repeat(11) {
            val paths = transport.listOwnerRevisions(session)
            if (paths.isEmpty()) {
                transport.finishDeleteBackup(session)
                return@withFreshSession Review(session.userId, null, 0)
            }
            paths.take(100).let { batch ->
                transport.removeOwnerRevisionBatch(session, batch)
            }
        }
        error("Some owner ciphertext objects remain; support review required")
    }
}
