package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.Phase1V2RestoreCheckpoint
import com.framebynavin.app.personal.Phase1V2RestoreStep
import com.framebynavin.app.personal.Phase1V2RestoreWriteAheadJournal
import com.framebynavin.app.personal.Phase1V2RecoveryRoots
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

/**
 * STRICT read-only restart reconciliation of the combined H4 Personal stage
 * against its independently journaled native v2 restore.
 *
 * The native restore may finish and atomically archive its own VERIFYING
 * checkpoint before H4 advances from BEFORE_PERSONAL to PERSONAL_WRITTEN.
 * The H4 stage alone must NOT cause a second Personal import. Only an exact
 * V359 durable ID+SHA handoff can identify which native restore belongs to
 * the combined transaction; searching newest history by time is unsafe.
 *
 * This inspector mutates NO Creator/Personal data, H4/v2 journals, private
 * JPEGs, alarm leases, escrow or target ZIP files. Inconsistent, torn,
 * symlinked, missing and ambiguous evidence remain blocked for owner review.
 */
internal class V360UnifiedPersonalV2RecoveryInspector(
    context: Context,
    handoff: V359UnifiedPersonalV2HandoffJournal? = null,
) {
    private val app = context.applicationContext
    private val binding = handoff ?: V359UnifiedPersonalV2HandoffJournal(app)
    private val cross = V344UnifiedRestoreJournal(app)

    sealed interface Assessment {
        data object WrongCombinedStage : Assessment
        data object NoHandoff : Assessment
        data object UnreadableOrContradictory : Assessment
        data class HandoffBeforeNativePrepare(
            val v2TransactionId: String,
        ) : Assessment
        data class NativeV2Active(
            val checkpoint: Phase1V2RestoreStep,
            val v2TransactionId: String,
        ) : Assessment
        data class NativeV2CompletedAndArchived(
            val v2TransactionId: String,
            val originalDigest: String,
            val mergedDigest: String,
        ) : Assessment
    }

    suspend fun inspectReadOnly(): Assessment {
        val h4 = runCatching { cross.requireCheckpointReadOnly() }
            .getOrNull() ?: return Assessment.UnreadableOrContradictory
        if (h4.step != V344UnifiedRestoreStep.BEFORE_PERSONAL) {
            return Assessment.WrongCombinedStage
        }
        val stored = binding.inspectReadOnly()
        if (stored == V359UnifiedPersonalV2HandoffJournal.Inspection.NotPrepared) {
            return Assessment.NoHandoff
        }
        val r = (stored as?
            V359UnifiedPersonalV2HandoffJournal.Inspection.Bound)?.handoff
            ?: return Assessment.UnreadableOrContradictory
        if (!V359PersonalV2HandoffCodec.matchesCombined(r, h4)) {
            return Assessment.UnreadableOrContradictory
        }

        val legacy = StabilizationPersonalMutationGate.journalRootFor(app)
        val activeRoot = Phase1V2RecoveryRoots.alongsideLegacy(legacy)
        val completedRoot = File(
            app.filesDir, "backlot-personal-v2-completed-journals",
        )
        val finishedRoot = File(completedRoot, r.personalV2TransactionId)
        val hasActive = exists(activeRoot)
        val hasCompleted = exists(finishedRoot)
        val hasLegacy = exists(legacy)
        if (hasLegacy || (hasActive && hasCompleted)) {
            return Assessment.UnreadableOrContradictory
        }

        // An interrupted fsync/native v2 write may retain encrypted per-v2
        // ZIPs even when the native checkpoint never reached disk.
        val transactions = File(
            app.filesDir, "backlot-personal-v2-encrypted-transactions",
        )
        val encryptedFiles = File(transactions, r.personalV2TransactionId)
        if (!ownedChild(transactions, encryptedFiles) ||
            !regularOwned(encryptedFiles, "incoming-encrypted.zip") ||
            !regularOwned(encryptedFiles, "original-escrow-encrypted.zip")) {
            return Assessment.UnreadableOrContradictory
        }

        if (!hasActive && !hasCompleted) {
            return if (cross.requireCheckpointReadOnly() == h4)
                Assessment.HandoffBeforeNativePrepare(r.personalV2TransactionId)
            else Assessment.UnreadableOrContradictory
        }
        val root = if (hasActive) activeRoot else finishedRoot
        if (hasCompleted && !ownedChild(completedRoot, finishedRoot)) {
            return Assessment.UnreadableOrContradictory
        }
        if (hasActive && (
                activeRoot.canonicalFile.parentFile != app.filesDir.canonicalFile ||
                Files.isSymbolicLink(activeRoot.toPath())
            )) return Assessment.UnreadableOrContradictory

        val native = runCatching {
            Phase1V2RestoreWriteAheadJournal(root).inspectAfterRestart()
        }.getOrNull() ?: return Assessment.UnreadableOrContradictory
        if (!V359PersonalV2HandoffCodec.matchesV2(r, native) ||
            cross.requireCheckpointReadOnly() != h4) {
            return Assessment.UnreadableOrContradictory
        }

        if (hasActive) return Assessment.NativeV2Active(
            checkpoint = native.step,
            v2TransactionId = native.transactionId,
        )
        return if (native.step == Phase1V2RestoreStep.VERIFYING) {
            Assessment.NativeV2CompletedAndArchived(
                v2TransactionId = native.transactionId,
                originalDigest = native.originalDigest,
                mergedDigest = native.targetDigest,
            )
        } else Assessment.UnreadableOrContradictory
    }

    private fun exists(path: File): Boolean =
        Files.exists(path.toPath(), LinkOption.NOFOLLOW_LINKS)

    private fun ownedChild(parent: File, child: File): Boolean =
        exists(parent) && parent.isDirectory &&
            !Files.isSymbolicLink(parent.toPath()) &&
            exists(child) && child.isDirectory &&
            !Files.isSymbolicLink(child.toPath()) &&
            child.canonicalFile.parentFile == parent.canonicalFile &&
            parent.canonicalFile.parentFile == app.filesDir.canonicalFile

    private fun regularOwned(parent: File, name: String): Boolean {
        val f = File(parent, name)
        return f.isFile && !Files.isSymbolicLink(f.toPath()) &&
            f.canonicalFile.parentFile == parent.canonicalFile &&
            f.length() > 0L
    }
}
