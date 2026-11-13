package com.framebynavin.app.data

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.framebynavin.app.personal.PersonalDeliveryCoordinator
import com.framebynavin.app.personal.Phase1HabitAlarmScheduler
import com.framebynavin.app.reminders.ReminderRecoveryEngine
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.util.concurrent.TimeUnit

/**
 * The H4 combined journal is the LAST blocking owner journal to be moved.
 * Only a durable V379 release with its exact H4 archived history and V378
 * both-domain proof can arm either Creator or Personal alarms.
 *
 * This worker can retry after an interrupted post-release schedule. It does
 * NOT resume restores, remove encrypted ZIPs, mutate recovery media, or
 * silently acknowledge a missed reminder. Three existing schedulers own
 * recurrence, snooze lease and exact-alarm semantics.
 */
internal class V380UnifiedPostReleaseAlarmRearm(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val app = applicationContext
        val release = V379UnifiedReleaseJournal(app)
        var record = release.inspect() ?: return Result.success()
        if (record.step == V379ReleaseStep.ALARMS_RECONCILED)
            return Result.success()
        if (V344UnifiedRestoreJournal.hasPendingFor(app)) {
            Log.w(TAG, "Combined recovery remains blocked; refusing alarm reconciliation")
            return Result.retry()
        }
        val proof = V378UnifiedVerifiedEvidenceJournal(app).inspect()
            ?: error("Retained combined verification evidence disappeared")
        check(record.transactionId == proof.transactionId &&
            record.verificationSha256 ==
                V379UnifiedReleaseJournal.proofDigest(proof) &&
            record.checkpointSha256 == proof.checkpointSha256 &&
            record.outcome == proof.outcome) {
            "Post-release alarm reconciliation has unrelated owner evidence"
        }
        val history = File(
            File(app.filesDir, "backlot-unified-retained-recovery-history-v1"),
            record.transactionId,
        )
        val archivedH4 = File(history, V344UnifiedRestoreJournal.ROOT_NAME)
        val activeH4 = File(archivedH4, "active.journal")
        check(history.isDirectory &&
            !Files.isSymbolicLink(history.toPath()) &&
            archivedH4.isDirectory &&
            !Files.isSymbolicLink(archivedH4.toPath()) &&
            Files.isRegularFile(activeH4.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(activeH4.toPath())) {
            "Archived owner combined checkpoint is missing or unsafe"
        }
        val checkpoint = V344UnifiedRestoreCheckpointCodec.decode(
            activeH4.readBytes(),
        )
        check(checkpoint.transactionId == record.transactionId &&
            V379UnifiedReleaseJournal.sha256(
                V344UnifiedRestoreCheckpointCodec.encode(checkpoint),
            ) == record.checkpointSha256 &&
            V378UnifiedVerifiedEvidenceJournal(app).inspect() == proof) {
            "H4 retained archive differs from final signed-by-digest proof"
        }
        if (record.step == V379ReleaseStep.READY_TO_ARCHIVE_H4) {
            record = release.advance(record, V379ReleaseStep.H4_ARCHIVED)
        }
        check(record.step == V379ReleaseStep.H4_ARCHIVED) {
            "No verified H4 archive can authorize reminder scheduling"
        }
        // Idempotent existing application recovery engines. Failure of any
        // one keeps the release phase at H4_ARCHIVED for next retry.
        ReminderRecoveryEngine.reconcile(app)
        PersonalDeliveryCoordinator(app).reconcile()
        Phase1HabitAlarmScheduler(app).reconcile()
        release.advance(record, V379ReleaseStep.ALARMS_RECONCILED)
        Result.success()
    } catch (error: Exception) {
        Log.w(TAG, "Verified owner release: alarm reconciliation needs retry", error)
        Result.retry()
    }

    companion object {
        private const val TAG = "BacklotUnifiedRelease"
    }
}

internal object V380CombinedAlarmRearmSafetyNet {
    private const val JOB = "backlot-unified-owner-release-rearm-v1"

    /**
     * Called after the owner's manual final release, and on every app
     * startup to close a crash gap AFTER the last H4 atomic rename.
     * Never schedules alarms while H4 is still active.
     */
    fun enqueueIfReleased(context: Context) {
        val app = context.applicationContext
        val journal = V379UnifiedReleaseJournal(app)
        if (!journal.hasEvidence() ||
            V344UnifiedRestoreJournal.hasPendingFor(app)) return
        val record = journal.inspect() ?: return
        if (record.step !in setOf(
                V379ReleaseStep.READY_TO_ARCHIVE_H4,
                V379ReleaseStep.H4_ARCHIVED,
            )) return
        WorkManager.getInstance(app).enqueueUniqueWork(
            JOB,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<V380UnifiedPostReleaseAlarmRearm>()
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS,
                )
                .build(),
        )
    }
}
