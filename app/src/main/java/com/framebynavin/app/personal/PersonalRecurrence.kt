package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Deterministic schedule planner. This is deliberately NOT wired to AlarmManager
 * yet: V223 foundation cannot create personal alarms until a namespaced delivery,
 * acknowledgement and reboot-recovery adapter is tested independently.
 */
internal object PersonalRecurrence {
    /** Strictly after the requested instant. Acknowledged occurrences are not replayed. */
    fun nextAfter(reminder: PersonalReminder, afterMillis: Long): Long? {
        reminder.checked()
        if (reminder.paused) return null
        val cutoff = maxOf(afterMillis, reminder.lastAcknowledgedOccurrenceMillis)
        val date = Instant.ofEpochMilli(cutoff).atZone(reminder.zoneId).toLocalDate()
        val firstDate = maxOf(reminder.startDate, date)

        fun occurrence(on: LocalDate): Long =
            on.atTime(reminder.time).atZone(reminder.zoneId).toInstant().toEpochMilli()

        fun future(on: LocalDate): Long? =
            if (!on.isBefore(reminder.startDate)) occurrence(on).takeIf { it > cutoff } else null

        return when (reminder.repeat) {
            PersonalRepeat.ONCE -> future(reminder.startDate)
            PersonalRepeat.DAILY -> {
                val today = future(firstDate)
                today ?: future(firstDate.plusDays(1))
            }
            PersonalRepeat.WEEKDAYS, PersonalRepeat.WEEKLY, PersonalRepeat.WEEKENDS -> {
                val days = when (reminder.repeat) {
                    PersonalRepeat.WEEKDAYS -> setOf(
                        java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY,
                        java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY,
                        java.time.DayOfWeek.FRIDAY,
                    )
                    PersonalRepeat.WEEKENDS -> setOf(
                        java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY,
                    )
                    else -> reminder.weeklyDays
                }
                (0L..7L).firstNotNullOfOrNull { offset ->
                    val day = firstDate.plusDays(offset)
                    if (day.dayOfWeek in days) future(day) else null
                }
            }
            PersonalRepeat.EVERY_N_HOURS -> nextHourly(reminder, cutoff, firstDate)
            PersonalRepeat.EVERY_N_DAYS -> {
                val daysSince = ChronoUnit.DAYS.between(reminder.startDate, firstDate)
                val n = reminder.everyNDays.toLong()
                val intervals = (daysSince / n).coerceAtLeast(0L)
                val candidate = reminder.startDate.plusDays(intervals * n)
                future(candidate) ?: future(candidate.plusDays(n))
            }
            PersonalRepeat.MONTHLY -> {
                val firstMonth = maxOf(
                    YearMonth.from(reminder.startDate),
                    YearMonth.from(firstDate),
                )
                (0L..2L).firstNotNullOfOrNull { offset ->
                    val month = firstMonth.plusMonths(offset)
                    val day = minOf(reminder.startDate.dayOfMonth, month.lengthOfMonth())
                    future(month.atDay(day))
                }
            }
        }
    }

    /**
     * Hourly reminders are wall-clock slots, not repeated elapsed-time alarms.
     * Selected weekdays and every-N-days must BOTH match. A max of eight
     * eligible date intervals covers weekday alignment for any N-day cadence
     * (weekly weekdays repeat after at most seven distinct residues).
     */
    private fun nextHourly(reminder: PersonalReminder, cutoff: Long,
                           firstDate: LocalDate): Long? {
        val nDays = reminder.everyNDays.toLong()
        val passedDays = ChronoUnit.DAYS.between(reminder.startDate, firstDate)
        val offset = (nDays - passedDays % nDays) % nDays
        var candidate = firstDate.plusDays(offset)
        val startMinute = reminder.time.hour * 60 + reminder.time.minute
        val endMinute = reminder.hourlyUntil.hour * 60 + reminder.hourlyUntil.minute
        val stepMinutes = reminder.everyNHours * 60

        repeat(8) {
            if (reminder.weeklyDays.isEmpty() || candidate.dayOfWeek in reminder.weeklyDays) {
                var minute = startMinute
                while (minute <= endMinute) {
                    val slot = java.time.LocalTime.of(minute / 60, minute % 60)
                    val due = candidate.atTime(slot).atZone(reminder.zoneId)
                        .toInstant().toEpochMilli()
                    if (due > cutoff) return due
                    minute += stepMinutes
                }
            }
            candidate = candidate.plusDays(nDays)
        }
        return null
    }
}
