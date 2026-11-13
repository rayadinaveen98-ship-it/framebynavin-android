package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * A new one-activity challenge, separate from the legacy V297 per-day plan format.
 * Pure domain policy only: this class does NOT persist data, install alarms, send
 * notifications, or migrate/overwrite V297 challenges. Those require separately
 * verified adapters and explicit owner consent before any release.
 */
internal data class Phase1HabitChallenge(
    val id: String,
    val activity: String,
    val startDate: LocalDate,
    val zoneId: String,
    val alertTime: LocalTime? = null,
    val alertKind: Phase1HabitAlertKind? = null,
    val confirmations: Map<LocalDate, Instant> = emptyMap(),
    val paused: Boolean = false,
    /** STRICT permits today's receipt; FLEXIBLE also permits explicit past-day backfill. */
    val completionMode: Phase1HabitCompletionMode = Phase1HabitCompletionMode.STRICT,
    /** Notes are separate from Done receipts: writing a note never completes a day. */
    val dayNotes: Map<LocalDate, String> = emptyMap(),
    /** References only. Challenge creation does not create or mutate a Space/Group. */
    val linkedSpaceId: String? = null,
    val linkedGroupId: String? = null,
    /** Explicit owner snooze for one local 21-day challenge occurrence. */
    val snoozedDay: LocalDate? = null,
    val snoozedUntil: Instant? = null,
    /** Appended for compatibility with existing positional constructors and copies. */
    val archived: Boolean = false,
) {
    fun checked(): Phase1HabitChallenge {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,100}"))) { "Invalid challenge identity" }
        require(activity.isNotBlank() && activity.trim().length <= 200) {
            "Choose one activity (max 200 characters)"
        }
        require(activity == activity.trim()) { "Activity must be trimmed" }
        ZoneId.of(zoneId)
        require((alertTime == null) == (alertKind == null)) {
            "An alert requires both an alert time and a mode"
        }
        val zone = ZoneId.of(zoneId)
        require(confirmations.size <= 21) { "Too many daily confirmations" }
        confirmations.forEach { (day, at) ->
            require(!day.isBefore(startDate) && !day.isAfter(endDate)) {
                "Confirmation is outside the 21-day challenge"
            }
            val confirmedOn = LocalDate.ofInstant(at, zone)
            require(confirmedOn == day || (
                completionMode == Phase1HabitCompletionMode.FLEXIBLE &&
                    confirmedOn.isAfter(day)
            )) {
                "Receipt time cannot be backdated; past-day confirmation needs Flexible mode"
            }
        }
        require(dayNotes.size <= 21 && dayNotes.all { (day, note) ->
            !day.isBefore(startDate) && !day.isAfter(endDate) &&
                note.length <= 1500
        }) { "Daily challenge notes must stay within 21 days and 1500 characters" }
        require(linkedSpaceId == null || linkedSpaceId.matches(
            Regex("[A-Za-z0-9_:.-]{1,128}")
        )) { "Invalid linked Space identity" }
        require(linkedGroupId == null || linkedGroupId.matches(
            Regex("[A-Za-z0-9_:.-]{1,128}")
        )) { "Invalid linked Group identity" }
        require((snoozedDay == null) == (snoozedUntil == null)) {
            "Snooze requires both an occurrence day and scheduled instant"
        }
        if (snoozedDay != null && snoozedUntil != null) {
            require(alertKind != null && alertTime != null &&
                !snoozedDay.isBefore(startDate) && !snoozedDay.isAfter(endDate) &&
                snoozedDay !in confirmations &&
                LocalDate.ofInstant(snoozedUntil, zone) == snoozedDay
            ) { "Snooze must remain on an active, unfinished challenge date" }
        }
        return this
    }

    val endDate: LocalDate get() = startDate.plusDays(20)
    val scheduleKey: String get() = "phase1-habit-$id"

    fun dayNumber(now: Instant): Int? {
        val day = LocalDate.ofInstant(now, ZoneId.of(zoneId))
        val index = java.time.temporal.ChronoUnit.DAYS.between(startDate, day)
        return if (index in 0L..20L) index.toInt() + 1 else null
    }

    fun status(day: LocalDate, now: Instant): Phase1HabitDayStatus {
        require(!day.isBefore(startDate) && !day.isAfter(endDate))
        if (confirmations.containsKey(day)) return Phase1HabitDayStatus.DONE
        val today = LocalDate.ofInstant(now, ZoneId.of(zoneId))
        return when {
            day.isBefore(today) -> Phase1HabitDayStatus.MISSED
            day == today -> Phase1HabitDayStatus.TODAY
            else -> Phase1HabitDayStatus.UPCOMING
        }
    }

    /** Idempotent owner confirmation of TODAY only, never inferred from alert delivery. */
    fun confirmToday(now: Instant): Phase1HabitChallenge {
        checked()
        require(!archived) { "Restore an archived challenge before confirming" }
        require(!paused) { "Resume the challenge to confirm a day" }
        val today = LocalDate.ofInstant(now, ZoneId.of(zoneId))
        require(!today.isBefore(startDate) && !today.isAfter(endDate)) {
            "Only an active challenge date can be confirmed"
        }
        if (confirmations.containsKey(today)) return this
        return copy(
            confirmations = confirmations + (today to now),
            snoozedDay = if (snoozedDay == today) null else snoozedDay,
            snoozedUntil = if (snoozedDay == today) null else snoozedUntil,
        ).checked()
    }

    /**
     * Explicit catch-up records WHEN the owner confirmed an earlier day;
     * it never forges a receipt timestamp on the past date.
     */
    fun confirmDay(day: LocalDate, at: Instant): Phase1HabitChallenge {
        checked()
        require(!archived) { "Restore an archived challenge before confirming" }
        require(!paused) { "Resume the challenge before confirming a day" }
        val today = LocalDate.ofInstant(at, ZoneId.of(zoneId))
        require(!day.isBefore(startDate) && !day.isAfter(endDate) &&
            !day.isAfter(today)) { "Choose a current or past challenge day" }
        require(day == today || completionMode == Phase1HabitCompletionMode.FLEXIBLE) {
            "Strict challenges only allow today; use Flexible for missed-day confirmation"
        }
        if (confirmations.containsKey(day)) return this
        return copy(
            confirmations = confirmations + (day to at),
            snoozedDay = if (snoozedDay == day) null else snoozedDay,
            snoozedUntil = if (snoozedDay == day) null else snoozedUntil,
        ).checked()
    }

    fun noteForDay(day: LocalDate, note: String): Phase1HabitChallenge {
        checked()
        require(!day.isBefore(startDate) && !day.isAfter(endDate))
        require(note.length <= 1500) { "Day note is too long" }
        return copy(dayNotes = if (note.isBlank()) dayNotes - day
            else dayNotes + (day to note)).checked()
    }

    /** Removing Done requires a deliberate owner action; it never runs automatically. */
    fun undoDay(day: LocalDate): Phase1HabitChallenge {
        checked()
        require(!day.isBefore(startDate) && !day.isAfter(endDate))
        return copy(confirmations = confirmations - day).checked()
    }

    fun setPaused(value: Boolean): Phase1HabitChallenge =
        copy(
            paused = value,
            snoozedDay = if (value) null else snoozedDay,
            snoozedUntil = if (value) null else snoozedUntil,
        ).checked()

    /** Archive preserves the entire 21-day history while removing active alerts. */
    fun setArchived(value: Boolean): Phase1HabitChallenge =
        copy(archived = value,
            snoozedDay = if (value) null else snoozedDay,
            snoozedUntil = if (value) null else snoozedUntil,
        ).checked()

    /** Owner-requested delay only after today's scheduled alert time. */
    fun canSnooze(at: Instant): Boolean {
        if (archived || paused || alertTime == null || alertKind == null) return false
        val zone = ZoneId.of(zoneId)
        val day = LocalDate.ofInstant(at, zone)
        if (day.isBefore(startDate) || day.isAfter(endDate) || day in confirmations) return false
        return !at.isBefore(day.atTime(alertTime).atZone(zone).toInstant())
    }

    fun snoozeToday(at: Instant, minutes: Int): Phase1HabitChallenge {
        checked()
        require(minutes in setOf(5, 10, 15, 30)) { "Choose an available snooze interval" }
        require(canSnooze(at)) {
            "This challenge is not awaiting today's alert; snooze was not saved"
        }
        val day = LocalDate.ofInstant(at, ZoneId.of(zoneId))
        val next = at.plusSeconds(minutes.toLong() * 60L)
        require(LocalDate.ofInstant(next, ZoneId.of(zoneId)) == day) {
            "Snooze cannot cross into another challenge day"
        }
        return copy(snoozedDay = day, snoozedUntil = next).checked()
    }

    /** Confirmed consecutive dates, including today if marked; never assumes Done. */
    fun currentStreak(now: Instant): Int {
        checked()
        val today = LocalDate.ofInstant(now, ZoneId.of(zoneId))
        if (today.isBefore(startDate)) return 0
        var cursor = if (today.isAfter(endDate)) endDate else today
        if (!confirmations.containsKey(cursor) && cursor == today) cursor = cursor.minusDays(1)
        var count = 0
        while (!cursor.isBefore(startDate) && confirmations.containsKey(cursor)) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    /**
     * Computes a single next potential alert for an Android scheduling adapter.
     * ZonedDateTime resolves local DST gaps/overlaps according to Java time rules.
     * No alerts after day 21, while paused, or on an already confirmed day.
     */
    fun nextAlertAfter(now: Instant): ZonedDateTime? {
        checked()
        val time = alertTime ?: return null
        if (paused || archived) return null
        val zone = ZoneId.of(zoneId)
        val regular = (0L..20L).asSequence()
            .map { startDate.plusDays(it) }
            .filterNot { it in confirmations }
            .map { it.atTime(time).atZone(zone) }
            .firstOrNull { it.toInstant().isAfter(now) }
        val snoozed = snoozedUntil?.takeIf {
            it.isAfter(now) && snoozedDay != null && snoozedDay !in confirmations
        }?.atZone(zone)
        return listOfNotNull(regular, snoozed).minByOrNull { it.toInstant() }
    }
}

internal enum class Phase1HabitCompletionMode { STRICT, FLEXIBLE }
internal enum class Phase1HabitAlertKind { ALARM, VOICE, QUIET }
internal enum class Phase1HabitDayStatus { DONE, MISSED, TODAY, UPCOMING }
