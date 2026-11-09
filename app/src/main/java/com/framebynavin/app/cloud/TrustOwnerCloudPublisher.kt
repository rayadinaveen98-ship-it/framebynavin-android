package com.framebynavin.app.cloud

import android.content.Context
import java.util.UUID

/**
 * Owner-only, manual immutable cloud publication of ONE encrypted local copy.
 *
 * This is not the legacy Creator/Personal Sync Now and never touches their
 * current cloud tables. The review must cover the exact local copy and the
 * existing remote head; a second phone's revision invalidates that approval.
 * A successful upload alone is not a backup: independently read back all
 * ciphertext, claim the head by CAS and re-fetch the new head.
 *
 * A source bundle containing unsupported settings or missing Creator/Personal
 * records is rejected BEFORE network upload. The existing remote backup stays
 * intact on conflict or failure, including a failed/partial Storage upload.
 */
internal class TrustOwnerCloudPublisher(
    context: Context,
    private val accounts: CloudSyncManager = CloudSyncManager(context),
    private val transport: TrustUniversalCloudTransport = TrustUniversalCloudTransport(),
) {
    private val copies = TrustOwnerEncryptedLocalCopy(context)

    internal data class Review(
        val ownerUserId: String,
        val localCopy: TrustOwnerEncryptedLocalCopy.Receipt,
        val remoteBefore: TrustCloudRevisionHead?,
        val localCopyIsOld: Boolean,
    ) {
        val alreadySavedRemote: Boolean get() = remoteBefore != null
        val wouldCreateNextRevision: Long
            get() = (remoteBefore?.revision ?: 0L) + 1L
    }

    internal data class Published(
        val ownerUserId: String,
        val copyId: String,
        val revision: Long,
        val encryptedBytes: Long,
        val verifiedSha256: String,
    ) {
        // We can prove these exact bytes were remotely stored. A full
        // cross-domain restore proof is still required before claiming that
        // "all Backlot data can be restored".
        val ciphertextVerifiedOnServer: Boolean get() = true
        val completeUniversalRestoreProven: Boolean get() = false
    }

    suspend fun inspectForOwnerReview(
        selected: TrustOwnerEncryptedLocalCopy.Receipt,
    ): Review {
        // CloudAccountGate uses a process-wide non-reentrant Mutex.
        // Finish the copy-catalog read BEFORE acquiring it for cloud head.
        val available = copies.availableCopiesForSignedInOwner().receipts
        return accounts.withFreshSession { session ->
        check(session.userId == selected.ownerId) {
            "This encrypted copy belongs to another signed-in Backlot user"
        }
        check(transport.phase(session) == "active") {
            "Universal cloud backup is unavailable or its deletion is pending"
        }
        // A catalog record is not an integrity proof. The publish action
        // checks local and decrypted bytes again under the owner operation lock.
        check(selected in available) { "Selected encrypted copy is unavailable" }
        val newest = available.maxOfOrNull { it.createdAtMillis } ?: selected.createdAtMillis
        val before = transport.head(session)
        check(before == null || before.revision < Long.MAX_VALUE)
        Review(
            ownerUserId = session.userId,
            localCopy = selected,
            remoteBefore = before,
            localCopyIsOld = selected.createdAtMillis < newest,
        )
        }
    }

    /**
     * Only called from the owner-initiated confirmation action. Do not run it
     * in a background worker, at startup or after legacy Sync Now.
     *
     * The exact phrase makes an older selected snapshot an intentional action
     * instead of a surprise replacement of the cloud "latest" pointer.
     */
    suspend fun publishOwnerApproved(
        reviewed: Review,
        passphrase: CharArray,
        exactConfirmation: String,
    ): Published {
        require(exactConfirmation == "BACK UP THIS ENCRYPTED COPY") {
            "Type BACK UP THIS ENCRYPTED COPY to confirm the selected revision"
        }
        check(reviewed.localCopy.ownerId == reviewed.ownerUserId)
        check(reviewed.remoteBefore?.revision != Long.MAX_VALUE)

        return copies.withVerifiedCopyForPublish(
            reviewed = reviewed.localCopy,
            passphrase = passphrase,
        ) { session, localFile, source ->
            check(session.userId == reviewed.ownerUserId) {
                "The account changed while preparing a cloud backup"
            }
            check(transport.phase(session) == "active") {
                "Remote backup deletion or an inactive owner blocks publication"
            }
            val freshHead = transport.head(session)
            TrustT1C4CloudRevisionPolicy.requireUnchanged(
                reviewed.remoteBefore, freshHead, session.userId,
            )
            check(source.missingRequiredSources.isEmpty() &&
                source.uncoveredPreferenceKeys.isEmpty()) {
                "Selected archive cannot be safely restored in full"
            }
            check(reviewed.localCopy.encryptedBytes in 1L..(50L * 1024 * 1024)) {
                "The encrypted archive exceeds the free-tier 50 MB per-object cap; " +
                    "the existing cloud backup was not replaced"
            }
            val descriptor = TrustCloudCiphertextDescriptor(
                ownerUserId = session.userId,
                objectPath = TrustT1C4CloudRevisionPolicy.newImmutablePath(
                    session.userId, UUID.randomUUID(),
                ),
                encryptedSha256 = reviewed.localCopy.encryptedSha256,
                encryptedBytes = reviewed.localCopy.encryptedBytes,
            )
            // All publish stages are recorded by the same immutable policy,
            // not arbitrary booleans returned by a provider API response.
            TrustT1C4TwoPhaseState.begin(freshHead, descriptor)
            transport.beginUpload(session)
            TrustT1C4CloudRevisionPolicy.requireUnchanged(
                freshHead, transport.head(session), session.userId,
            )
            transport.uploadNewImmutable(session, descriptor, localFile)
            // Remote Streaming SHA verifies actual downloaded ciphertext,
            // not a Storage metadata field nor the upload's HTTP status.
            val verified = transport.verifiedDownload(session, descriptor)
            TrustT1C4CloudRevisionPolicy.requireUnchanged(
                freshHead, transport.head(session), session.userId,
            )
            val committed = transport.claimRevision(
                session = session,
                previous = freshHead,
                proposed = descriptor,
                proof = verified,
            )
            check(committed.canClaimDurableRemoteCopy &&
                committed.readbackSha256 == descriptor.encryptedSha256 &&
                committed.remoteRevision == reviewed.wouldCreateNextRevision) {
                "Cloud head confirmation did not match the exact uploaded bytes"
            }
            Published(
                ownerUserId = session.userId,
                copyId = reviewed.localCopy.copyId,
                revision = requireNotNull(committed.remoteRevision),
                encryptedBytes = descriptor.encryptedBytes,
                verifiedSha256 = descriptor.encryptedSha256,
            )
        }
    }
}
