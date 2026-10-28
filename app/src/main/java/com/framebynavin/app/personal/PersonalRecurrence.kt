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
            PersonalRepeat.WEEKDAYS, PersonalRepeat.WEEKLY -> {
                val days = if (reminder.repeat == PersonalRepeat.WEEKDAYS) {
                    setOf(
                        java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY,
                        java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY,
                        java.time.DayOfWeek.FRIDAY,
                    )
                } else reminder.weeklyDays
                (0L..7L).firstNotNullOfOrNull { offset ->
                    val day = firstDate.plusDays(offset)
                    if (day.dayOfWeek in days) future(day) else null
                }
            }
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
}
