package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.Phase1V2OwnerRestoreCoordinator
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A strict, read-only H3/H4 readiness assessment. The final journal-absence
 * recheck does acquire both existing data-store mutexes, but does NOT hold
 * them across source/escrow authentication and cannot be a commit permit.
 * It can answer whether the existing original/target ZIPs are independently
 * authenticated, refer to the same owner selection and match current data.
 */
internal data class V344UnifiedPrecommitReview(
    val candidate: V344UnifiedRestoreCheckpoint,
    val potentialPersonalAdditions: Int,
    val sourceAndEscrowAuthenticated: Boolean,
    val existingDomainGatesRechecked: Boolean = true,
    val noCommitAuthority: Boolean = true,
    val mayRunWithoutFreshOwnerConsent: Boolean = false,
)

internal object V344UnifiedPrecommitPolicy {
    fun propose(
        source: V342UnifiedRestorePreparationStore.Status.ChecksumMatched,
        escrow: V343UnifiedRecoveryRecord,
        originalStillMatchesPhone: Boolean,
        authenticatedSources: Boolean,
        personalConflicts: Int,
        localCreatorRecoveryPending: Boolean,
        localPersonalRecoveryPending: Boolean,
        ownerReviewed: Boolean,
        timestamp: Long,
        transactionId: String,
    ): V344UnifiedRestoreCheckpoint {
        require(ownerReviewed && authenticatedSources && originalStillMatchesPhone) {
            "Both encrypted ZIPs and current originals require explicit owner review"
        }
        require(personalConflicts == 0 &&
            !localCreatorRecoveryPending && !localPersonalRecoveryPending) {
            "An unresolved Personal conflict or independent restore journal blocks H3 planning"
        }
        require(source.transactionId == escrow.selectedSourceId &&
            source.sha256 == escrow.selectedSourceSha256 &&
            escrow.state == V343UnifiedRecoveryState.ESCROW_VERIFIED_NO_WRITE) {
            "Owner's target archive does not match the authenticated original escrow"
        }
        val candidate = V344UnifiedRestoreCheckpoint(
            transactionId = transactionId,
            sourceId = source.transactionId,
            sourceSha256 = source.sha256,
            escrowId = escrow.transactionId,
            escrowSha256 = escrow.rollbackSha256,
            creatorOriginalSha256 = escrow.creatorBeforeSha256,
            personalOriginalSha256 = escrow.personalBeforeSha256,
            step = V344UnifiedRestoreStep.PREPARED_NO_WRITES,
            createdAtMillis = timestamp,
            updatedAtMillis = timestamp,
        )
        return V344UnifiedRestorePolicy.validate(candidate)
    }
}

internal class V344UnifiedRestorePrecommit(context: Context) {
    private val app = context.applicationContext
    private val source = V342UnifiedRestorePreparationStore(app)
    private val escrow = V343UnifiedRecoveryEscrow(app)

    /**
     * No live data writes, no journal preparation. A previous (even malformed)
     * H4 journal ALWAYS wins; an unresolved Creator or Personal local recovery
     * also blocks planning. All facts must be rechecked later under an
     * exclusive Creator+Personal write fence before any commit is permitted.
     */
    suspend fun inspectOwnerReviewed(passphrase: CharArray):
        V344UnifiedPrecommitReview = withContext(Dispatchers.IO) {
        require(passphrase.size >= 12) { "Private owner passphrase required" }
        check(!V344UnifiedRestoreJournal.hasPendingFor(app)) {
            "An existing cross-domain recovery journal needs owner review"
        }
        val prepared = source.status() as?
            V342UnifiedRestorePreparationStore.Status.ChecksumMatched
            ?: error("Prepare the exact encrypted owner target first")
        val original = escrow.inspectEscrowRecordReadOnly()
        val verified = escrow.inspectRetained(passphrase)
        require(!verified.target.personalDiff.needsOwnerReview &&
            verified.target.personalDiff.conflicts == 0) {
            "The current Personal data has conflicts or incomplete integrity evidence"
        }
        val creatorPending = Files.exists(
            File(app.filesDir, "creator_restore_recovery_v181/pending.json").toPath(),
            LinkOption.NOFOLLOW_LINKS,
        )
        val personalPending = Files.exists(
            StabilizationPersonalMutationGate.journalRootFor(app).toPath(),
            LinkOption.NOFOLLOW_LINKS,
        ) || Phase1V2OwnerRestoreCoordinator(app).hasPendingOwnerRecovery()

        val proposed = V344UnifiedPrecommitPolicy.propose(
            source = prepared,
            escrow = original,
            originalStillMatchesPhone = verified.originalStillMatchesPhone,
            authenticatedSources = true,
            personalConflicts = verified.target.personalDiff.conflicts,
            localCreatorRecoveryPending = creatorPending,
            localPersonalRecoveryPending = personalPending,
            ownerReviewed = true,
            timestamp = System.currentTimeMillis().coerceAtLeast(1L),
            transactionId = UUID.randomUUID().toString(),
        )

        check(!V344UnifiedRestoreJournal.hasPendingFor(app) &&
            source.status() == prepared &&
            escrow.inspectEscrowRecordReadOnly() == original) {
            "Owner restore evidence changed during read-only precommit review"
        }
        // Both existing owner mutexes are acquired IN ONE ORDER. This is a
        // last-moment journal-absence check, not an atomic data fingerprint:
        // ordinary writes may resume after the gates are released.
        V345UnifiedOwnerDomainFence(app).withExclusiveReadCheck {
            check(!V344UnifiedRestoreJournal.hasPendingFor(app)) {
                "Combined recovery journal appeared while both stores were fenced"
            }
        }
        V344UnifiedPrecommitReview(
            candidate = proposed,
            potentialPersonalAdditions = verified.target.personalDiff.proposedAdditions,
            sourceAndEscrowAuthenticated = true,
        )
    }
}
