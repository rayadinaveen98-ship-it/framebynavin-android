package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity

/**
 * Phase B1's final read-only evidence handshake before a future transactional writer.
 *
 * The inputs must originate from a freshly authenticated V300 stage, an
 * independently authenticated V305 escrow, and a fresh in-lock local read.
 * This policy checks their mutual consistency. It NEVER grants permission to
 * mutate a device, start a restore, delete an escrow, or rearm alarms.
 *
 * Portable archives omit device-only alarm leases and CUSTOM music URIs.
 * This must be disclosed and accepted before any future commit.
 */
internal data class B01RestorePrecommitEvidence(
    val prepared: V304PreparedTransfer,
    val escrow: V305EscrowReceipt,
    val barrier: V305CommitBarrierReceipt,
    val liveLocal: V301LocalRecoverySnapshot,
    val authenticatedIncoming: V300PersonalComponents,
    val authenticatedIncomingPhotos: List<V290SpacePhotoEntity>,
    val proposedTarget: StabilizationRestoreTarget,
    val ownerRequested: Boolean,
    val acknowledgedNonPortableState: Boolean,
)

internal data class B01RestorePrecommitReceipt(
    val preparedId: String,
    val escrowId: String,
    val baselineDigest: String,
    val incomingDigest: String,
    val targetDigest: String,
    val incomingPhotoCount: Int,
) {
    /** Deliberately no live commit authority until the entire B transaction is tested. */
    val authorizesLiveWrite: Boolean get() = false
    val guaranteesExactAlarmOrCustomMusicRollback: Boolean get() = false
}

internal object B01RestorePrecommitPolicy {
    private val sha256 = Regex("[a-f0-9]{64}")

    fun inspect(evidence: B01RestorePrecommitEvidence): B01RestorePrecommitReceipt {
        require(evidence.ownerRequested) { "Recovery requires explicit owner approval" }
        require(evidence.acknowledgedNonPortableState) {
            "Owner must acknowledge custom audio and alarm leases are not portable"
        }
        val p = evidence.prepared
        val e = evidence.escrow
        val b = evidence.barrier
        val t = evidence.proposedTarget
        require(evidence.liveLocal.localIntegrityVerified &&
            t.preview.localIntegrityVerified &&
            !t.preview.requiresConflictReview &&
            t.preview.conflicts == 0) {
            "Phone inventory is damaged, incomplete, or conflicts with the archive"
        }
        require(p.state == "READY" && e.archiveBytes > 0L) {
            "Prepared transfer or encrypted rollback escrow is not ready"
        }
        require(listOf(
            p.baselineDigest, p.archiveDigest, e.baselineDigest, e.archiveSha256,
            b.localBaselineSha256, b.incomingArchiveSha256, b.rollbackArchiveSha256,
            t.startingDigest, t.targetDigest,
        ).all(sha256::matches)) {
            "A recovery integrity fingerprint is invalid"
        }
        require(p.transactionId == b.preparedId && e.id == b.rollbackEscrowId) {
            "Restore preparation and rollback identities do not match"
        }
        val current = V304RestorePlanPolicy.fingerprint(
            evidence.liveLocal.components, evidence.liveLocal.photos,
        )
        val incoming = V304RestorePlanPolicy.fingerprint(
            evidence.authenticatedIncoming, evidence.authenticatedIncomingPhotos,
        )
        val target = V304RestorePlanPolicy.fingerprint(
            t.merged, t.photos,
        )
        require(current == p.baselineDigest &&
            current == e.baselineDigest &&
            current == b.localBaselineSha256 &&
            current == t.startingDigest) {
            "Device changed since authenticated rollback and preparation"
        }
        require(incoming == p.archiveDigest && incoming == b.incomingArchiveSha256) {
            "Incoming archive no longer matches the authenticated preparation"
        }
        require(target == t.targetDigest) {
            "Proposed merged target has changed after conflict inspection"
        }
        require(p.photoCount == evidence.authenticatedIncomingPhotos.size &&
            p.photoCount == t.preview.photos.proposed) {
            "Prepared private photo inventory does not match archive inspection"
        }
        require(e.archiveSha256 == b.rollbackArchiveSha256) {
            "Independent encrypted rollback copy does not match its barrier"
        }
        require(!b.authorizedToCommit && !p.canCommitToLiveStorage &&
            !t.authorizedToWriteLive) {
            "Unsafe pre-commit authority escalation"
        }
        return B01RestorePrecommitReceipt(
            preparedId = p.transactionId,
            escrowId = e.id,
            baselineDigest = current,
            incomingDigest = incoming,
            targetDigest = target,
            incomingPhotoCount = p.photoCount,
        )
    }
}
