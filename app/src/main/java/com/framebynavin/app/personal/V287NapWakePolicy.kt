package com.framebynavin.app.personal

import android.content.Context
import com.framebynavin.app.widget.V224WidgetUpdater
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * V287 opt-in nap end. One stable Personal reminder ID for any selected duration.
 * Changing the duration cannot create a second wake alarm and never rewrites
 * the existing V285 2 PM nap-start reminder or Creator projects.
 */
internal object V287NapWakePolicy {
    const val ID = "life21-v1-nap-wake"
    val START: LocalTime = LocalTime.of(14, 0)
    val EDITING_RESUMES: LocalTime = LocalTime.of(15, 15)

    internal enum class Duration(val minutes: Int, val display: String) {
        ONE_HOUR(60, "60 MIN"),
        NINETY_MINUTES(90, "90 MIN"),
        TWO_HOURS(120, "120 MIN");
    }

    fun endTime(duration: Duration): LocalTime = START.plusMinutes(duration.minutes.toLong())

    fun overlapsEditing(duration: Duration): Boolean =
        endTime(duration).isAfter(EDITING_RESUMES)

    fun startDateForNewSetup(nowMillis: Long, zone: ZoneId): LocalDate {
        val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone)
        // Never surprise the owner with a nap alarm this afternoon if the
        // intended 2 PM nap *start* already passed before the setup was saved.
        return if (now.toLocalTime().isBefore(START)) now.toLocalDate()
            else now.toLocalDate().plusDays(1)
    }

    fun template(duration: Duration, startDate: LocalDate, zone: ZoneId): PersonalReminder =
        PersonalReminder(
            id = ID,
            title = "Wake up from your afternoon nap",
            notes = "Nap started at 2:00 PM. " + duration.minutes +
                "-minute rest completed. Drink water, stretch gently and resume your " +
                "day. Snooze only if you need a little more rest.",
            startDate = startDate,
            time = endTime(duration),
            zoneId = zone,
            repeat = PersonalRepeat.DAILY,
            alert = PersonalAlert.ALARM,
            snoozeMinutes = 5,
            voiceRepeatCount = 3,
            alarmDurationSeconds = 60,
            autoRetryEnabled = false,
        ).checked()

    /** Accurate outcome: persisted data is different from successful Android scheduling. */
    internal data class InstallResult(
        val changed: Boolean,
        val wasNew: Boolean,
        val androidRechecked: Boolean,
        val widgetsRefreshed: Boolean,
    )

    suspend fun save(context: Context, duration: Duration): InstallResult {
        val app = context.applicationContext
        val zone = ZoneId.systemDefault()
        val startDate = startDateForNewSetup(System.currentTimeMillis(), zone)
        val proposal = template(duration, startDate, zone)
        var changed = false
        var wasNew = false
        PersonalReminderStore(app).mutate { current ->
            val index = current.indexOfFirst { it.id == ID }
            if (index < 0) {
                wasNew = true
                changed = true
                current + proposal
            } else {
                val old = current[index]
                val target = old.copy(
                    startDate = startDate,
                    time = proposal.time,
                    notes = proposal.notes,
                    paused = false,
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
                    lastAcknowledgedOccurrenceMillis = 0L,
                    lastMissedOccurrenceMillis = 0L,
                    lastResponse = PersonalResponse.NONE,
                    lastResponseAtMillis = 0L,
                    updatedAtMillis = System.currentTimeMillis(),
                ).checked()
                // A same-time save is idempotent. Never replace an active,
                // snoozed or customized record without an actual user change.
                if (old.time == proposal.time && !old.paused) current
                else {
                    changed = true
                    current.toMutableList().also { it[index] = target }
                }
            }
        }
        if (changed && !wasNew) {
            val coordinator = PersonalDeliveryCoordinator(app)
            coordinator.cancelPending(ID)
            PersonalPlaybackService.stopForReminder(ID)
            PersonalReminderNotifications.cancel(app, ID)
        }
        val scheduled = runCatching { PersonalDeliveryCoordinator(app).reconcile() }.isSuccess
        val refreshed = runCatching { V224WidgetUpdater.refresh(app) }.isSuccess
        return InstallResult(changed, wasNew, scheduled, refreshed)
    }
}
