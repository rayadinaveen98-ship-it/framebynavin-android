package com.framebynavin.app.personal

import android.content.Context
import com.framebynavin.app.reminders.ReminderMusicMode
import com.framebynavin.app.widget.V224WidgetUpdater
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CancellationException

/**
 * One reliable write boundary for new Personal UI. This deliberately never
 * edits CreatorTask or forwards into creator reminder action receivers.
 */
internal data class PersonalReminderDraft(
    val title: String,
    val notes: String = "",
    val startDate: LocalDate,
    val time: LocalTime,
    val zoneId: ZoneId,
    val repeat: PersonalRepeat = PersonalRepeat.ONCE,
    val weeklyDays: Set<DayOfWeek> = emptySet(),
    val everyNDays: Int = 1,
    val alert: PersonalAlert = PersonalAlert.NOTIFICATION,
    val everyNHours: Int = 3,
    val hourlyUntil: LocalTime = LocalTime.of(23, 59),
    val purpose: PersonalPurpose = PersonalPurpose.CUSTOM,
    val voiceMusicMode: ReminderMusicMode = ReminderMusicMode.PIANO,
    val voiceMusicUri: String = "",
    val snoozeMinutes: Int = 10,
    val voiceRepeatCount: Int = 3,
    val voiceRepeatGapMillis: Long = 2_500L,
    val alarmDurationSeconds: Int = 60,
    val autoRetryEnabled: Boolean = false,
    val autoRetryMinutes: Int = 5,
    val autoRetryMaxAttempts: Int = 2,
) {
    fun checked(): PersonalReminderDraft {
        val candidate = toRecord().checked()
        require(PersonalRecurrence.nextAfter(candidate, System.currentTimeMillis()) != null) {
            "Choose a future date and time."
        }
        return this
    }

    fun toRecord() = PersonalReminder(
        title = title.trim(),
        notes = notes.trim(),
        startDate = startDate,
        time = time,
        zoneId = zoneId,
        repeat = repeat,
        weeklyDays = weeklyDays,
        everyNDays = everyNDays,
        alert = alert,
        everyNHours = everyNHours,
        hourlyUntil = hourlyUntil,
        purpose = purpose,
        voiceMusicMode = voiceMusicMode,
        voiceMusicUri = voiceMusicUri,
        snoozeMinutes = snoozeMinutes,
        voiceRepeatCount = voiceRepeatCount,
        voiceRepeatGapMillis = voiceRepeatGapMillis,
        alarmDurationSeconds = alarmDurationSeconds,
        autoRetryEnabled = autoRetryEnabled,
        autoRetryMinutes = autoRetryMinutes,
        autoRetryMaxAttempts = autoRetryMaxAttempts,
    )
}

/**
 * The reminder itself has ALREADY been durably committed. This is not a failed
 * Save, and retrying as a new reminder would duplicate the owner's schedule.
 */
internal class PersonalReminderCreatedButNeedsAttention(
    val savedReminder: PersonalReminder,
    cause: Throwable,
) : IllegalStateException(
    "Reminder saved, but alert scheduling or widget refresh needs attention. " +
        "Review your reminder and Android alert permissions.",
    cause,
)

internal data class PersonalDeletionOutcome(
    val deletedCount: Int,
    val cleanupWarning: String? = null,
)

internal class PersonalReminderCommands(context: Context) {
    private val app = context.applicationContext
    private val store = PersonalReminderStore(app)
    private val delivery = PersonalDeliveryCoordinator(app)

    suspend fun create(draft: PersonalReminderDraft): PersonalReminder {
        val checked = draft.checked().toRecord()
        store.create(checked)
        // Anything after store.create is post-commit. A background permission,
        // service or widget error must NOT masquerade as an unsaved reminder.
        try {
            delivery.reconcile()
            V224WidgetUpdater.refresh(app)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            throw PersonalReminderCreatedButNeedsAttention(checked, failure)
        }
        return checked
    }

    suspend fun revise(id: String, draft: PersonalReminderDraft) {
        val checked = draft.checked()
        // Store replaces the lease first, so late old PendingIntents fail token
        // verification. Cancel the old alarm next, then schedule the new lease.
        store.update(id) { old ->
            old.copy(
                title = checked.title.trim(),
                notes = checked.notes.trim(),
                startDate = checked.startDate,
                time = checked.time,
                zoneId = checked.zoneId,
                repeat = checked.repeat,
                weeklyDays = checked.weeklyDays,
                everyNDays = checked.everyNDays,
                everyNHours = checked.everyNHours,
                hourlyUntil = checked.hourlyUntil,
                purpose = checked.purpose,
                alert = checked.alert,
                voiceMusicMode = checked.voiceMusicMode,
                voiceMusicUri = checked.voiceMusicUri,
                snoozeMinutes = checked.snoozeMinutes,
                voiceRepeatCount = checked.voiceRepeatCount,
                voiceRepeatGapMillis = checked.voiceRepeatGapMillis,
                alarmDurationSeconds = checked.alarmDurationSeconds,
                autoRetryEnabled = checked.autoRetryEnabled,
                autoRetryMinutes = checked.autoRetryMinutes,
                autoRetryMaxAttempts = checked.autoRetryMaxAttempts,
                autoRetryCount = 0,
                autoRetryScheduledAtMillis = 0L,
                pendingOccurrenceMillis = 0L,
                pendingFireAtMillis = 0L,
                pendingToken = "",
                presentedToken = "",
                presentedAtMillis = 0L,
                followUpSentToken = "",
                followUpSentOccurrenceMillis = 0L,
                snoozedUntilMillis = 0L,
                lastAcknowledgedOccurrenceMillis = 0L,
                lastMissedOccurrenceMillis = 0L,
                lastResponse = PersonalResponse.NONE,
                lastResponseAtMillis = 0L,
                paused = false,
                updatedAtMillis = System.currentTimeMillis(),
            )
        }
        delivery.cancelPending(id)
        PersonalPlaybackService.stopForReminder(id)
        PersonalReminderNotifications.cancel(app, id)
        delivery.reconcile()
        V224WidgetUpdater.refresh(app)
    }

    suspend fun setPaused(id: String, paused: Boolean) {
        store.update(id) { original ->
            original.copy(
                paused = paused,
                pendingOccurrenceMillis = 0L,
                pendingFireAtMillis = 0L,
                pendingToken = "",
                presentedToken = "",
                presentedAtMillis = 0L,
                followUpSentToken = "",
                followUpSentOccurrenceMillis = 0L,
                snoozedUntilMillis = 0L,
                autoRetryCount = 0,
                autoRetryScheduledAtMillis = 0L,
                updatedAtMillis = System.currentTimeMillis(),
            )
        }
        delivery.cancelPending(id)
        PersonalPlaybackService.stopForReminder(id)
        PersonalReminderNotifications.cancel(app, id)
        if (!paused) delivery.reconcile()
        V224WidgetUpdater.refresh(app)
    }

    /**
     * One confirmed atomic deletion across PersonalReminderStore only.
     * If Android cleanup fails after commit, the selected reminders are ALREADY
     * deleted; surface a warning instead of suggesting a second delete.
     */
    suspend fun deleteManyReviewed(
        selection: Collection<PersonalDeletionTicket>,
    ): PersonalDeletionOutcome {
        val tickets = selection.toList()
        store.deleteManyReviewed(tickets)
        var cleanupWarning: String? = null
        // The cleanup IDs are durable in DataStore even if process death
        // happens before WorkManager can accept this task.
        val queueFailed = runCatching {
            PersonalDeletionCleanupScheduler.enqueue(app)
        }.isFailure
        try {
            val cleaned = PersonalDeletionCleanup(app).reconcile()
            if (!cleaned) {
                cleanupWarning = "Reminders deleted, but one or more Android " +
                    "alerts need background cleanup. Recovery will retry."
            }
        } catch (error: Exception) {
            cleanupWarning = "Reminders deleted, but Android alert cleanup " +
                "needs attention; the recovery job will retry."
        }
        if (queueFailed && cleanupWarning != null) {
            cleanupWarning += " The immediate retry could not be queued; " +
                "periodic recovery remains available."
        }
        runCatching { V224WidgetUpdater.refresh(app) }
            .onFailure {
                cleanupWarning = (cleanupWarning?.plus(" ") ?: "") +
                    "Widget refresh needs attention."
            }
        return PersonalDeletionOutcome(tickets.size, cleanupWarning)
    }

    suspend fun delete(id: String) {
        // Stored record is removed first. A delivery racing before cancel then
        // fails lookup and cannot post a valid notification.
        store.delete(id)
        delivery.cancelPending(id)
        PersonalPlaybackService.stopForReminder(id)
        PersonalReminderNotifications.cancel(app, id)
        V224WidgetUpdater.refresh(app)
    }
}
