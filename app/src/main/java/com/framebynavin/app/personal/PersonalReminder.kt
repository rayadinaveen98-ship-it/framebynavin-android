package com.framebynavin.app.personal

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * Personal data is NOT a CreatorTask and must never be inserted into task flows,
 * Project Pulse, creator insights, connected YouTube data or creator cloud sync.
 * Local-first foundation for Backlot Personal Reminders.
 *
 * Occurrence scheduling uses local wall clock + an explicit zone so recurring
 * reminders survive DST/time-zone transitions without drifting in elapsed time.
 */
internal enum class PersonalRepeat {
    ONCE, DAILY, WEEKDAYS, WEEKLY, MONTHLY, EVERY_N_DAYS, WEEKENDS, EVERY_N_HOURS,
}

internal enum class PersonalAlert {
    NOTIFICATION, VOICE, ALARM,
}

/** A record of actual user response, never inferred from display or timeout. */
internal enum class PersonalResponse {
    NONE, DONE, DISMISSED, SNOOZED, RESCHEDULED,
}

internal data class PersonalReminder(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val startDate: LocalDate,
    val time: LocalTime,
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val repeat: PersonalRepeat = PersonalRepeat.ONCE,
    val weeklyDays: Set<DayOfWeek> = emptySet(),
    val everyNDays: Int = 1,
    val alert: PersonalAlert = PersonalAlert.NOTIFICATION,
    val paused: Boolean = false,
    /** Highest *scheduled* occurrence explicitly acknowledged; never infer Done from no response. */
    val lastAcknowledgedOccurrenceMillis: Long = 0L,
    /** Old unanswered delivery retired after the recovery grace window; NOT a user response. */
    val lastMissedOccurrenceMillis: Long = 0L,
    /** Optional one-off user snooze for the currently due occurrence. */
    val snoozedUntilMillis: Long = 0L,
    /** Original scheduled occurrence, distinct from later snooze/fire times. */
    val pendingOccurrenceMillis: Long = 0L,
    val pendingFireAtMillis: Long = 0L,
    /** Rotated for every re-armed occurrence, prevents replay of old alarm/actions. */
    val pendingToken: String = "",
    /** Posted notification handoff token; prevents repeat delivery on reconciliation. */
    val presentedToken: String = "",
    /** Actual original posting time, not the scheduled Android alarm time. */
    val presentedAtMillis: Long = 0L,
    /** Durable one-shot receipt for the exact opt-in follow-up lease. */
    val followUpSentToken: String = "",
    /** Original scheduled occurrence with a follow-up already claimed; survives Snooze. */
    val followUpSentOccurrenceMillis: Long = 0L,
    val lastResponse: PersonalResponse = PersonalResponse.NONE,
    val lastResponseAtMillis: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = createdAtMillis,
    /** Hourly schedule cadence; ignored for existing non-hourly reminder types. */
    val everyNHours: Int = 3,
    /** Last daily eligible wall-clock time, inclusive; start is reminder.time. */
    val hourlyUntil: LocalTime = LocalTime.of(23, 59),
) {
    fun checked(): PersonalReminder {
        require(id.matches(Regex("[a-zA-Z0-9_:.-]{1,128}"))) { "Invalid personal reminder id" }
        require(title.isNotBlank() && title.trim() == title && title.length <= 180) {
            "Personal reminder title must contain 1–180 characters"
        }
        require(notes.length <= 2_000) { "Personal reminder notes are too long" }
        require(everyNDays in 1..365) { "Custom repeat interval must be 1–365 days" }
        require(everyNHours in 1..24) { "Hourly interval must be 1–24 hours" }
        require(repeat != PersonalRepeat.EVERY_N_HOURS || !hourlyUntil.isBefore(time)) {
            "Hourly window must end at or after its starting time"
        }
        require(weeklyDays.size <= 7) { "Invalid weekly recurrence days" }
        require(repeat != PersonalRepeat.WEEKLY || weeklyDays.isNotEmpty()) {
            "Weekly recurrence must select at least one day"
        }
        require(lastAcknowledgedOccurrenceMillis >= 0L && lastMissedOccurrenceMillis >= 0L &&
            snoozedUntilMillis >= 0L &&
            pendingOccurrenceMillis >= 0L && pendingFireAtMillis >= 0L &&
            presentedAtMillis >= 0L && followUpSentOccurrenceMillis >= 0L &&
            lastResponseAtMillis >= 0L) {
            "Invalid personal occurrence state"
        }
        require(presentedToken.isEmpty() || presentedToken == pendingToken) {
            "Stale presented occurrence token"
        }
        require(followUpSentToken.isEmpty() || followUpSentToken.matches(Regex("[a-f0-9-]{36}"))) {
            "Invalid personal follow-up token"
        }
        require(pendingToken.isEmpty() || pendingToken.matches(Regex("[a-f0-9-]{36}"))) {
            "Invalid personal occurrence token"
        }
        require((pendingOccurrenceMillis == 0L && pendingFireAtMillis == 0L && pendingToken.isEmpty()) ||
            (pendingOccurrenceMillis > 0L && pendingFireAtMillis > 0L && pendingToken.isNotEmpty())) {
            "Incomplete personal occurrence journal"
        }
        require(createdAtMillis >= 0L && updatedAtMillis >= 0L) { "Invalid timestamps" }
        return this
    }
}
