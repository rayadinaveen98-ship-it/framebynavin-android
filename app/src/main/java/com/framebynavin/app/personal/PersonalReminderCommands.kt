package com.framebynavin.app.personal

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

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
    )
}

internal class PersonalReminderCommands(context: Context) {
    private val app = context.applicationContext
    private val store = PersonalReminderStore(app)
    private val delivery = PersonalDeliveryCoordinator(app)

    suspend fun create(draft: PersonalReminderDraft): PersonalReminder {
        val checked = draft.checked().toRecord()
        store.create(checked)
        delivery.reconcile()
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
                alert = checked.alert,
                pendingOccurrenceMillis = 0L,
                pendingFireAtMillis = 0L,
                pendingToken = "",
                presentedToken = "",
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
    }

    suspend fun setPaused(id: String, paused: Boolean) {
        store.update(id) { original ->
            original.copy(
                paused = paused,
                pendingOccurrenceMillis = 0L,
                pendingFireAtMillis = 0L,
                pendingToken = "",
                presentedToken = "",
                snoozedUntilMillis = 0L,
                updatedAtMillis = System.currentTimeMillis(),
            )
        }
        delivery.cancelPending(id)
        PersonalPlaybackService.stopForReminder(id)
        PersonalReminderNotifications.cancel(app, id)
        if (!paused) delivery.reconcile()
    }

    suspend fun delete(id: String) {
        // Stored record is removed first. A delivery racing before cancel then
        // fails lookup and cannot post a valid notification.
        store.delete(id)
        delivery.cancelPending(id)
        PersonalPlaybackService.stopForReminder(id)
        PersonalReminderNotifications.cancel(app, id)
    }
}
