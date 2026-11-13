package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.Phase1V2RecoveryRoots
import com.framebynavin.app.personal.Phase1V2RestoreCheckpoint
import com.framebynavin.app.personal.Phase1V2RestoreFingerprint
import com.framebynavin.app.personal.Phase1V2RestoreStep
import com.framebynavin.app.personal.Phase1V2RestoreTargetPolicy
import com.framebynavin.app.personal.Phase1V2RestoreWriteAheadJournal
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import com.framebynavin.app.personal.V300EncryptedPersonalArchive
import com.framebynavin.app.personal.V300PersonalComponents
import com.framebynavin.app.personal.V301LocalRecoveryReader
import com.framebynavin.app.personal.V301LocalRecoverySnapshot
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * H3 Task 1, narrowly owner-approved recovery of an interruption AFTER the
 * authenticated V359 Personal handoff but BEFORE the native v2 journal could
 * be durably prepared.
 *
 * Only the native v2 PREPARED checkpoint is created; there are ZERO live
 * Personal/Creator/Room/photo writes and no alarm rescheduling. A future,
 * separately owner-confirmed V362 operation may then reconcile native v2.
 *
 * Under the existing H4 BEFORE_PERSONAL freeze, no ordinary data writer may
 * change the original. A last-moment stable Personal+Creator read must equal
 * the authenticated original escrow, including all 21-day Done instants and
 * current private JPEG checksums. The native mkdir+fsync is exclusive by its
 * known path; if a second caller races it, the already-existing journal wins
 * and this operation refuses to invent a successful receipt.
 */
internal class V363UnifiedPersonalPrejournalRecovery(context: Context) {
    private val app = context.applicationContext
    private val cross = V344UnifiedRestoreJournal(app)
    private val native = V360UnifiedPersonalV2RecoveryInspector(app)
    private val handoffs = V359UnifiedPersonalV2HandoffJournal(app)

    suspend fun ownerApproveNativeJournalOnly(
        passphrase: CharArray,
        ownerConfirmed: Boolean,
    ): V363PrejournalReceipt = withContext(Dispatchers.IO) {
        require(ownerConfirmed && passphrase.size >= 12) {
            "Owner must freshly approve native checkpoint recovery with the archive password"
        }
        val expected = cross.requireCheckpointReadOnly()
        check(expected.step == V344UnifiedRestoreStep.BEFORE_PERSONAL &&
            !V347OwnerRecoveryDecisionJournal.hasPendingFor(app)) {
            "Native Personal PREPARED recovery is unavailable outside the interrupted stage"
        }
        val status = native.inspectReadOnly() as?
            V360UnifiedPersonalV2RecoveryInspector.Assessment.HandoffBeforeNativePrepare
            ?: error("An active, archived, absent or unreadable v2 journal cannot be re-prepared")
        val handoff = (handoffs.inspectReadOnly() as?
            V359UnifiedPersonalV2HandoffJournal.Inspection.Bound)?.handoff
            ?: error("The exact, immutable Personal handoff is missing")
        check(V363PrejournalRecoveryPolicy.matches(
            expected, handoff, status, ownerConfirmed,
        )) {
            "The pending native Personal journal is not bound to this combined restore"
        }

        val ownerProof = V346UnifiedInterruptedRecoveryReview(app).inspect(
            ownerConfirmedReview = true, passphrase = passphrase,
        )
        check(ownerProof.journal == expected &&
            !ownerProof.creatorStandaloneRecoveryMarker &&
            !ownerProof.personalStandaloneRecoveryMarker) {
            "Another recovery or changed sealed source blocks native checkpoint preparation"
        }
        val recipe = V356UnifiedPersonalRecoveryRecipeReader(app)
            .inspectVerified(ownerProof, passphrase).personalRecipe
        check(recipe.originalDigest == handoff.originalPersonalDigest &&
            recipe.mergedTargetDigest == handoff.mergedPersonalDigest) {
            "Authenticated Personal original/target differ from the durable handoff"
        }
        val txDir = exactNativeTransaction(handoff.personalV2TransactionId)
        val original = authenticatePersonal(
            File(txDir, "original-escrow-encrypted.zip"), passphrase,
        )
        check(Phase1V2RestoreFingerprint.calculate(
            original.components, original.photos,
        ) == recipe.originalDigest) {
            "Native original escrow does not match the selected owner baseline"
        }
        val selected = authenticatePersonal(
            File(txDir, "incoming-encrypted.zip"), passphrase,
        )
        val merged = Phase1V2RestoreTargetPolicy.propose(
            V301LocalRecoverySnapshot(
                original.components, original.photos,
                localIntegrityVerified = true,
            ),
            selected.components, selected.photos,
        )
        check(merged.baselineDigest == recipe.originalDigest &&
            merged.targetDigest == recipe.mergedTargetDigest &&
            merged.preview.conflicts == 0) {
            "Native selected archive differs from the authenticated non-conflicting target"
        }

        // This is an independently authorized READ-ONLY snapshot, not a
        // restore write permit. Both normal owner domains remain frozen by
        // H4 even after the inspection scope ends.
        val localOriginalDigest = StabilizationPersonalMutationGate
            .withPendingCombinedReadInspection(app, expected) {
                CreatorDataGate.withPendingCombinedReadInspection(app, expected) {
                    val live = V301LocalRecoveryReader(app).readV2()
                    check(live.localIntegrityVerified) {
                        "Current Personal/Space photo integrity is incomplete"
                    }
                    Phase1V2RestoreFingerprint.calculate(
                        live.components, live.photos,
                    )
                }
            }
        check(localOriginalDigest == recipe.originalDigest &&
            cross.requireCheckpointReadOnly() == expected &&
            native.inspectReadOnly() == status &&
            !V347OwnerRecoveryDecisionJournal.hasPendingFor(app)) {
            "Personal data or the native recovery handoff changed before journal creation"
        }

        val checkpoint = Phase1V2RestoreCheckpoint(
            transactionId = handoff.personalV2TransactionId,
            preparedId = handoff.personalV2PreparedId,
            escrowId = handoff.personalV2EscrowId,
            originalDigest = handoff.originalPersonalDigest,
            targetDigest = handoff.mergedPersonalDigest,
            step = Phase1V2RestoreStep.PREPARED,
            createdAtMillis = handoff.createdAtMillis,
            updatedAtMillis = handoff.createdAtMillis,
        )
        val root = Phase1V2RecoveryRoots.alongsideLegacy(
            StabilizationPersonalMutationGate.journalRootFor(app),
        )
        // This is the ONLY write and is only to the native recovery journal.
        val recorded = Phase1V2RestoreWriteAheadJournal(root)
            .prepare(checkpoint, ownerRequested = true)
        check(recorded == checkpoint) {
            "Native v2 PREPARED journal differed from immutable owner handoff"
        }
        V363PrejournalReceipt(
            combinedTransactionId = expected.transactionId,
            nativeTransactionId = recorded.transactionId,
            step = recorded.step,
        )
    }

    private data class PersonalArchiveRows(
        val components: V300PersonalComponents,
        val photos: List<V290SpacePhotoEntity>,
    )

    private fun exactNativeTransaction(id: String): File {
        val root = File(app.filesDir, "backlot-personal-v2-encrypted-transactions")
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Untrusted native Personal archive root"
        }
        val directory = File(root, id)
        check(directory.isDirectory &&
            !Files.isSymbolicLink(directory.toPath()) &&
            directory.canonicalFile.parentFile == root.canonicalFile) {
            "Untrusted native Personal owner transaction"
        }
        return directory
    }

    private fun authenticatePersonal(
        encrypted: File,
        passphrase: CharArray,
    ): PersonalArchiveRows {
        check(encrypted.isFile &&
            !Files.isSymbolicLink(encrypted.toPath()) &&
            encrypted.canonicalFile.parentFile == encrypted.parentFile.canonicalFile &&
            encrypted.length() > 0L) {
            "Native encrypted Personal source or original escrow is untrusted"
        }
        return encrypted.inputStream().buffered().use { input ->
            V300EncryptedPersonalArchive.stageAndVerify(
                input, passphrase, app.cacheDir,
            ).use { verified ->
                PersonalArchiveRows(
                    verified.components, verified.photos.map { it.photo },
                )
            }
        }
    }
}

internal data class V363PrejournalReceipt(
    val combinedTransactionId: String,
    val nativeTransactionId: String,
    val step: Phase1V2RestoreStep,
) {
    init {
        require(step == Phase1V2RestoreStep.PREPARED) {
            "Preparing a native owner recovery journal is not a completed Personal restore"
        }
    }
    val anyPersonalDataRestored: Boolean get() = false
    val mayResumeAutomatically: Boolean get() = false
    val mayReleaseOriginalEscrow: Boolean get() = false
}

/**
 * Strict source-only gate. A handoff that already has any active/archived or
 * torn native v2 checkpoint MUST NOT be re-prepared under another identity.
 */
internal object V363PrejournalRecoveryPolicy {
    fun matches(
        combined: V344UnifiedRestoreCheckpoint,
        handoff: V359PersonalV2Handoff,
        missing: V360UnifiedPersonalV2RecoveryInspector.Assessment
            .HandoffBeforeNativePrepare,
        freshOwnerConsent: Boolean,
    ): Boolean =
        freshOwnerConsent &&
            combined.step == V344UnifiedRestoreStep.BEFORE_PERSONAL &&
            V359PersonalV2HandoffCodec.matchesCombined(handoff, combined) &&
            missing.v2TransactionId == handoff.personalV2TransactionId
}
