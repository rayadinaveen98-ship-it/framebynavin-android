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
            require(LocalDate.ofInstant(at, zone) == day) {
                "Confirmation must belong to its original challenge-local date"
            }
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
        require(!paused) { "Resume the challenge to confirm a day" }
        val today = LocalDate.ofInstant(now, ZoneId.of(zoneId))
        require(!today.isBefore(startDate) && !today.isAfter(endDate)) {
            "Only an active challenge date can be confirmed"
        }
        if (confirmations.containsKey(today)) return this
        return copy(confirmations = confirmations + (today to now)).checked()
    }

    /** Removing Done requires a deliberate owner action; it never runs automatically. */
    fun undoDay(day: LocalDate): Phase1HabitChallenge {
        checked()
        require(!day.isBefore(startDate) && !day.isAfter(endDate))
        return copy(confirmations = confirmations - day).checked()
    }

    fun setPaused(value: Boolean): Phase1HabitChallenge = copy(paused = value).checked()

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
        if (paused) return null
        val zone = ZoneId.of(zoneId)
        return (0L..20L).asSequence()
            .map { startDate.plusDays(it) }
            .filterNot { it in confirmations }
            .map { it.atTime(time).atZone(zone) }
            .firstOrNull { it.toInstant().isAfter(now) }
    }
}

internal enum class Phase1HabitAlertKind { ALARM, VOICE, QUIET }
internal enum class Phase1HabitDayStatus { DONE, MISSED, TODAY, UPCOMING }
