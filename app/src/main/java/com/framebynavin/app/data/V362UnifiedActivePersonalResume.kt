package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.Phase1V2OwnerRestoreCoordinator
import com.framebynavin.app.personal.Phase1V2RestoreFingerprint
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import com.framebynavin.app.personal.V301LocalRecoveryReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * H3 Task 1 — owner-approved forward recovery of a truly ACTIVE native v2
 * Personal transaction under an interrupted H4 BEFORE_PERSONAL checkpoint.
 *
 * NOT EXPOSED to UI, boot receivers or an automatic recovery worker. The
 * unfinished rollback and Creator-side recovery must exist before Restore All
 * is ever enabled. This narrow path deliberately never deletes original
 * escrow, reconciles reminders or releases combined recovery journals.
 *
 * Native v2 stage replay is delegated to the EXISTING v2 write-ahead engine:
 * it examines exact original/target ZIPs, current domain and photo forensics,
 * and resumes idempotent BEFORE or WRITTEN stages only under its own permit.
 * A completed-but-unadvanced native v2 journal instead belongs to V361.
 */
internal class V362UnifiedActivePersonalResume(context: Context) {
    private val app = context.applicationContext
    private val combined = V344UnifiedRestoreJournal(app)
    private val nativeInspector = V360UnifiedPersonalV2RecoveryInspector(app)

    suspend fun ownerApproveInternalOnly(
        passphrase: CharArray,
        ownerConfirmedForwardRecovery: Boolean,
    ): V362ActivePersonalResumeReceipt = withContext(Dispatchers.IO) {
        require(ownerConfirmedForwardRecovery && passphrase.size >= 12) {
            "Fresh owner consent and the encrypted owner recovery password are required"
        }
        val expected = combined.requireCheckpointReadOnly()
        require(expected.step == V344UnifiedRestoreStep.BEFORE_PERSONAL) {
            "Only an interrupted Personal forward stage can be resumed"
        }
        check(!V347OwnerRecoveryDecisionJournal.hasPendingFor(app)) {
            "An earlier owner recovery direction must be reviewed before forward replay"
        }

        // Do all long-running decryption OUTSIDE the Personal/Creator mutexes.
        val selected = V346UnifiedInterruptedRecoveryReview(app).inspect(
            ownerConfirmedReview = true, passphrase = passphrase,
        )
        check(selected.journal == expected &&
            !selected.creatorStandaloneRecoveryMarker &&
            selected.personalStandaloneRecoveryMarker) {
            "An ACTIVE native Personal v2 journal is required for this exact recovery"
        }
        // personalStandaloneRecoveryMarker reflects BOTH legacy and native
        // v2 in the general read-only inspector. Here the subsequent V360
        // strict inspector + V359 handoff binding prove that the single
        // allowed pending marker is exactly THIS combined native v2.
        // A legacy v1 journal is refused by V360 before a write permit opens.
        val recipe = V356UnifiedPersonalRecoveryRecipeReader(app)
            .inspectVerified(selected, passphrase).personalRecipe
        val native = nativeInspector.inspectReadOnly() as?
            V360UnifiedPersonalV2RecoveryInspector.Assessment.NativeV2Active
            ?: error("Native Personal recovery is not ACTIVE; never replay an archived v2 import")
        val record = (
            V359UnifiedPersonalV2HandoffJournal(app).inspectReadOnly() as?
                V359UnifiedPersonalV2HandoffJournal.Inspection.Bound
        )?.handoff ?: error("Combined Personal handoff is missing or damaged")
        check(V362ActiveResumePolicy.eligible(
            expected, selected, recipe, native, record,
            ownerConfirmedForwardRecovery,
        )) {
            "The native Personal journal is not bound to the original selected transaction"
        }

        // Personal first, Creator second. Ordinary owner writers cannot run
        // concurrently; the inner v2 forward permit still checks its native
        // checkpoint and forensic proof at every actual store/photo mutation.
        StabilizationPersonalMutationGate.withCombinedActiveV2Resume(
            app, expected, ownerConfirmedResume = true,
        ) { nativeCheckpoint ->
            check(nativeCheckpoint.transactionId == native.v2TransactionId &&
                nativeCheckpoint.step == native.checkpoint &&
                nativeCheckpoint.originalDigest == recipe.originalDigest &&
                nativeCheckpoint.targetDigest == recipe.mergedTargetDigest &&
                combined.requireCheckpointReadOnly() == expected &&
                !V347OwnerRecoveryDecisionJournal.hasPendingFor(app)) {
                "Native v2 owner recovery advanced or changed before exclusive lock"
            }
            val archived = CreatorDataGate.withPendingCombinedReadInspection(app, expected) {
                val finished = Phase1V2OwnerRestoreCoordinator(app)
                    .resumeInspectedForCombined(
                        checkpoint = nativeCheckpoint,
                        escrowPassphrase = passphrase,
                        selectedPassphrase = passphrase,
                    )
                check(finished.transactionId == nativeCheckpoint.transactionId &&
                    finished.completedAndJournalArchived &&
                    finished.originalDigest == recipe.originalDigest &&
                    finished.installedDigest == recipe.mergedTargetDigest &&
                    finished.importedNewRecords == recipe.incomingAdditions) {
                    "Native v2 resumed to an unexpected Personal target"
                }

                // The original v2 coordinator already verifies its own live
                // data and photos before journal archival; verify once more
                // before advancing the outer combined checkpoint.
                val current = V301LocalRecoveryReader(app).readV2()
                check(current.localIntegrityVerified &&
                    Phase1V2RestoreFingerprint.calculate(
                        current.components, current.photos,
                    ) == recipe.mergedTargetDigest) {
                    "Current Personal records, exact Done receipts or photo bytes differ"
                }
                val archived = nativeInspector.inspectReadOnly() as?
                    V360UnifiedPersonalV2RecoveryInspector.Assessment
                        .NativeV2CompletedAndArchived
                    ?: error("Native Personal v2 journal was not durably archived")
                check(archived.v2TransactionId == nativeCheckpoint.transactionId &&
                    archived.originalDigest == recipe.originalDigest &&
                    archived.mergedDigest == recipe.mergedTargetDigest &&
                    combined.requireCheckpointReadOnly() == expected) {
                    "Archived native v2 recovery disagrees with combined owner records"
                }

                // Return immutable native proof with H4 still unchanged.
                // The Creator read gate requires this original H4 checkpoint
                // on exit and cannot itself advance it.
                archived
            }
            check(nativeInspector.inspectReadOnly() == archived &&
                combined.requireCheckpointReadOnly() == expected) {
                "Native Personal completion evidence changed before H4 checkpoint"
            }
            // Creator read lock has been released; Personal's process mutex
            // remains held. Ordinary Creator edits are still blocked by H4.
            // No live records or media are modified by this checkpoint.
            val next = combined.checkpoint(
                expected,
                V344UnifiedRestoreStep.PERSONAL_WRITTEN,
                maxOf(System.currentTimeMillis(), expected.updatedAtMillis + 1L),
                otherRecoveryPending = {
                    V347OwnerRecoveryDecisionJournal.hasPendingFor(app)
                },
            )
            V362ActivePersonalResumeReceipt(
                combinedTransactionId = next.transactionId,
                nativeTransactionId = archived.v2TransactionId,
                installedPersonalDigest = recipe.mergedTargetDigest,
                stage = next.step,
            )
        }
    }
}

/** No partial-stage result can release H4 or resume alarm delivery. */
internal data class V362ActivePersonalResumeReceipt(
    val combinedTransactionId: String,
    val nativeTransactionId: String,
    val installedPersonalDigest: String,
    val stage: V344UnifiedRestoreStep,
) {
    init {
        require(stage == V344UnifiedRestoreStep.PERSONAL_WRITTEN &&
            installedPersonalDigest.matches(Regex("[0-9a-f]{64}"))) {
            "Active Personal repair does not establish complete combined restoration"
        }
    }
    val canReleaseOriginalEscrow: Boolean get() = false
    val combinedRestoreComplete: Boolean get() = false
    val canRestartAlarms: Boolean get() = false
}

/**
 * Pure source/owner handoff gate. This is NOT a writer token; actual native
 * stage authorization remains in the Personal process mutex and the native
 * encrypted-ZIP/record/photo forensics of the existing v2 recovery engine.
 */
internal object V362ActiveResumePolicy {
    fun eligible(
        h4: V344UnifiedRestoreCheckpoint,
        ownerProof: V346InterruptedRecoveryProof,
        recipe: V355UnifiedPersonalRecoveryRecipe,
        native: V360UnifiedPersonalV2RecoveryInspector.Assessment.NativeV2Active,
        handoff: V359PersonalV2Handoff,
        freshOwnerConsent: Boolean,
    ): Boolean =
        freshOwnerConsent &&
            h4.step == V344UnifiedRestoreStep.BEFORE_PERSONAL &&
            V346InterruptedRecoveryProofPolicy.matches(ownerProof, h4) &&
            !ownerProof.creatorStandaloneRecoveryMarker &&
            ownerProof.personalStandaloneRecoveryMarker &&
            // Only V360.NativeV2Active with exact V359 identity is permitted:
            // a pending legacy Personal marker cannot ever satisfy this.
            recipe.noMutationAuthority &&
            recipe.allOriginalPhotoReferencesPreserved &&
            V359PersonalV2HandoffCodec.matchesCombined(handoff, h4) &&
            handoff.personalV2TransactionId == native.v2TransactionId &&
            handoff.originalPersonalDigest == recipe.originalDigest &&
            handoff.mergedPersonalDigest == recipe.mergedTargetDigest
}
