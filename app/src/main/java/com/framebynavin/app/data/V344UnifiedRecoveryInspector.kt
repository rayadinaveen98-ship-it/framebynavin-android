package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.Phase1V2OwnerRestoreCoordinator
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

/**
 * Recovery inspection only: never opens a write permit, runs a restore,
 * deletes an escrow, resumes alarms or modifies a checksum journal.
 *
 * The H4 journal may have crashed between the two canonical restore engines,
 * each of which also has its OWN recovery marker. We report both layers and
 * require fresh owner review rather than inferring a safe action from H4 stage.
 */
internal class V344UnifiedRecoveryInspector(context: Context) {
    private val app = context.applicationContext
    private val journal = V344UnifiedRestoreJournal(app)
    private val source = V342UnifiedRestorePreparationStore(app)
    private val escrow = V343UnifiedRecoveryEscrow(app)

    internal sealed interface Assessment {
        data object NoCombinedRecovery : Assessment
        data class OwnerReview(
            val kind: V344RecoveryKind,
            val checkpoint: V344UnifiedRestoreStep?,
            val originalRetained: Boolean,
            val targetRetained: Boolean,
            val creatorLocalPending: Boolean,
            val personalLocalPending: Boolean,
            val unreadableEvidence: Boolean,
            val mayAutoResume: Boolean = false,
            val mayAutoRollback: Boolean = false,
            val mayReleaseOriginal: Boolean = false,
        ) : Assessment
    }

    suspend fun inspect(): Assessment {
        val state = journal.inspectAfterRestart()
        if (state is V344UnifiedRestoreJournal.Inspection.NoJournal) {
            return Assessment.NoCombinedRecovery
        }
        val original = escrow.status()
        val target = source.status()
        val creatorPending = Files.exists(
            File(app.filesDir, "creator_restore_recovery_v181/pending.json").toPath(),
            LinkOption.NOFOLLOW_LINKS,
        )
        val personalLegacy = Files.exists(
            StabilizationPersonalMutationGate.journalRootFor(app).toPath(),
            LinkOption.NOFOLLOW_LINKS,
        )
        val personalV2 = Phase1V2OwnerRestoreCoordinator(app).hasPendingOwnerRecovery()
        val isReadable = state is V344UnifiedRestoreJournal.Inspection.OwnerRecoveryRequired
        val kind = if (isReadable) {
            (state as V344UnifiedRestoreJournal.Inspection.OwnerRecoveryRequired).kind
        } else {
            V344RecoveryKind.INVALID_JOURNAL_NEEDS_OWNER_REVIEW
        }
        return Assessment.OwnerReview(
            kind = kind,
            checkpoint = (state as?
                V344UnifiedRestoreJournal.Inspection.OwnerRecoveryRequired)?.step,
            originalRetained = original is
                V343UnifiedRecoveryEscrow.Status.BaselineRetained,
            targetRetained = target is
                V342UnifiedRestorePreparationStore.Status.ChecksumMatched,
            creatorLocalPending = creatorPending,
            personalLocalPending = personalLegacy || personalV2,
            unreadableEvidence = !isReadable ||
                original !is V343UnifiedRecoveryEscrow.Status.BaselineRetained ||
                target !is V342UnifiedRestorePreparationStore.Status.ChecksumMatched,
        )
    }
}

internal object V344UnifiedRecoveryCopy {
    fun describe(kind: V344RecoveryKind): String = when (kind) {
        V344RecoveryKind.NO_LIVE_WRITES ->
            "A combined checkpoint exists before data writes. Preserve both encrypted copies until owner review."
        V344RecoveryKind.CREATOR_MAY_BE_PARTIAL ->
            "Creator restore may have started or stopped mid-write. Preserve Creator and Personal recovery evidence."
        V344RecoveryKind.CREATOR_FINISHED_PERSONAL_NOT_STARTED ->
            "Creator checkpoint was marked complete, but Personal was not yet started. Never auto-continue."
        V344RecoveryKind.PERSONAL_MAY_BE_PARTIAL ->
            "Personal restore may be incomplete. Existing Personal photo and receipt journals must be checked."
        V344RecoveryKind.BOTH_WRITTEN_NEEDS_VERIFICATION ->
            "Both domain writes may have occurred. The live records still require owner-reviewed integrity checks."
        V344RecoveryKind.VERIFIED_NEEDS_OWNER_RELEASE ->
            "A final verification checkpoint exists, but the encrypted original may not be released automatically."
        V344RecoveryKind.INVALID_JOURNAL_NEEDS_OWNER_REVIEW ->
            "Recovery journal is missing, torn or unreadable. No automatic restore, rollback or cleanup is allowed."
    }
}
