package com.framebynavin.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An INTERNAL H3 prepare-only reservation, deliberately NOT exposed as a
 * normal owner action until real forward/rollback execution has been proven.
 *
 * The recovery marker freezes all normal gated Creator/Personal writes.
 * This function itself performs NO Creator/Personal restore, no alarm
 * reschedule, no database import, no media replacement. On a failed request
 * before journal.prepare there is no combined marker; after journal.prepare
 * the marker must remain until separately authenticated owner recovery.
 */
internal class V349UnifiedRestoreReservation(context: Context) {
    private val app = context.applicationContext
    private val journal = V344UnifiedRestoreJournal(app)
    private val source = V342UnifiedRestorePreparationStore(app)
    private val escrow = V343UnifiedRecoveryEscrow(app)

    suspend fun reserveOnly(
        passphrase: CharArray,
        ownerConfirmedPreparation: Boolean,
    ): V349UnwrittenReservation = withContext(Dispatchers.IO) {
        require(ownerConfirmedPreparation && passphrase.size >= 12) {
            "Fresh owner consent and private passphrase are required"
        }
        val reviewed = V344UnifiedRestorePrecommit(app)
            .inspectFullOwnerProof(passphrase, ownerReviewed = true)
        val candidate = reviewed.preflight.candidate
        check(V345UnifiedAuthenticatedPairPolicy.matchesCheckpoint(
            reviewed.allFourCanonicalSectionsAuthenticated, candidate,
        )) { "Source or rollback escrow failed canonical media checks" }

        V345UnifiedOwnerDomainFence(app).withExclusiveJournalReservation {
            // Recheck both existing owner stores while they are exclusively
            // locked. A stale escrow can NEVER be promoted to live restoration.
            val current = V343UnifiedBaselineReader(app).capture()
            check(V349UnifiedReservationPolicy.matchesOriginal(current, candidate)) {
                "Owner's current Creator or Personal records changed after escrow"
            }
            val selected = source.status() as?
                V342UnifiedRestorePreparationStore.Status.ChecksumMatched
                ?: error("Prepared encrypted owner target disappeared")
            val original = escrow.inspectEscrowRecordReadOnly()
            check(selected.transactionId == candidate.sourceId &&
                selected.sha256 == candidate.sourceSha256 &&
                original.transactionId == candidate.escrowId &&
                original.rollbackSha256 == candidate.escrowSha256) {
                "Owner recovery target or original escrow changed during lock acquisition"
            }
            // Single linearization point. From here every ordinary gated
            // writer becomes fail-closed, even after process death.
            check(!V347OwnerRecoveryDecisionJournal.hasPendingFor(app)) {
                "An earlier owner recovery direction needs review before a new reservation"
            }
            val prepared = journal.prepare(candidate, ownerApproved = true)
            check(prepared == candidate) { "Combined journal did not retain the reviewed checkpoint" }
            V349UnwrittenReservation(
                transactionId = prepared.transactionId,
                step = prepared.step,
                liveCreatorWrites = false,
                livePersonalWrites = false,
                restoreFinished = false,
            )
        }
    }
}

internal object V349UnifiedReservationPolicy {
    fun matchesOriginal(
        live: V343UnifiedBaselineFingerprint,
        journal: V344UnifiedRestoreCheckpoint,
    ): Boolean =
        journal.step == V344UnifiedRestoreStep.PREPARED_NO_WRITES &&
            live.valid &&
            live.creatorSha256 == journal.creatorOriginalSha256 &&
            live.personalSha256 == journal.personalOriginalSha256
}

internal data class V349UnwrittenReservation(
    val transactionId: String,
    val step: V344UnifiedRestoreStep,
    val liveCreatorWrites: Boolean,
    val livePersonalWrites: Boolean,
    val restoreFinished: Boolean,
) {
    init {
        require(step == V344UnifiedRestoreStep.PREPARED_NO_WRITES &&
            !liveCreatorWrites && !livePersonalWrites && !restoreFinished) {
            "The reservation cannot claim a completed combined restore"
        }
    }
}
