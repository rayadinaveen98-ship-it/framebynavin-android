package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorCalendarItem
import com.framebynavin.app.data.CreatorCalendarSource
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Read-only planning window on top of CreatorContentCalendarEngine.upcoming().
 * This is NOT an all-time publication archive: the original engine deliberately
 * projects only upcoming and eligible completed entries in the future window.
 * Nothing here materializes a weekly occurrence as a CreatorTask.
 */
internal enum class UX08ECalendarView { WEEK, MONTH, AGENDA }

internal data class UX08ECalendarWindow(
    val start: LocalDate,
    val endInclusive: LocalDate,
)

internal data class UX08ECalendarCounts(
    val savedProjects: Int,
    val weeklyPreviews: Int,
    val completedProjects: Int,
) {
    val total: Int get() = savedProjects + weeklyPreviews
}

internal object UX08ECalendarPolicy {
    // Modest, finite planning horizon. Existing storage and occurrence keys
    // stay intact; this is an on-demand projection, not 90 created projects.
    const val PLANNING_DAYS = 90L
    const val AGENDA_DAYS = 14L

    fun horizonEnd(today: LocalDate): LocalDate =
        today.plusDays(PLANNING_DAYS - 1)

    fun constrained(date: LocalDate, today: LocalDate): LocalDate =
        date.coerceIn(today, horizonEnd(today))

    fun window(
        view: UX08ECalendarView,
        selected: LocalDate,
        today: LocalDate,
    ): UX08ECalendarWindow {
        val actual = constrained(selected, today)
        return when (view) {
            UX08ECalendarView.WEEK -> {
                val monday = actual.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                UX08ECalendarWindow(monday, monday.plusDays(6))
            }
            UX08ECalendarView.MONTH -> {
                val month = YearMonth.from(actual)
                UX08ECalendarWindow(month.atDay(1), month.atEndOfMonth())
            }
            UX08ECalendarView.AGENDA ->
                UX08ECalendarWindow(actual, actual.plusDays(AGENDA_DAYS - 1))
        }
    }

    /** A disabled arrow never jumps into an unsupported or fully past period. */
    fun shift(
        view: UX08ECalendarView,
        selected: LocalDate,
        today: LocalDate,
        direction: Long,
    ): LocalDate? {
        require(direction == -1L || direction == 1L)
        val base = constrained(selected, today)
        val proposed = when (view) {
            UX08ECalendarView.WEEK -> base.plusWeeks(direction)
            UX08ECalendarView.MONTH -> base.plusMonths(direction)
            UX08ECalendarView.AGENDA -> base.plusDays(direction * AGENDA_DAYS)
        }
        if (view == UX08ECalendarView.AGENDA) {
            return proposed.takeIf { !it.isBefore(today) && !it.isAfter(horizonEnd(today)) }
        }
        // Check real period BEFORE any in-range clamping, otherwise a previous
        // arrow on today's first week would appear enabled but do nothing.
        val realStart = when (view) {
            UX08ECalendarView.WEEK -> proposed.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            )
            UX08ECalendarView.MONTH -> YearMonth.from(proposed).atDay(1)
            UX08ECalendarView.AGENDA -> proposed
        }
        val realEnd = when (view) {
            UX08ECalendarView.WEEK -> realStart.plusDays(6)
            UX08ECalendarView.MONTH -> YearMonth.from(proposed).atEndOfMonth()
            UX08ECalendarView.AGENDA -> proposed.plusDays(AGENDA_DAYS - 1)
        }
        return proposed.takeIf {
            !realEnd.isBefore(today) && !realStart.isAfter(horizonEnd(today))
        }?.let { constrained(it, today) }
    }

    fun isSelectable(date: LocalDate, today: LocalDate): Boolean =
        !date.isBefore(today) && !date.isAfter(horizonEnd(today))

    fun monthGrid(month: YearMonth): List<LocalDate> {
        val firstMonday = month.atDay(1)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0L..41L).map(firstMonday::plusDays)
    }

    fun count(items: List<CreatorCalendarItem>): UX08ECalendarCounts =
        UX08ECalendarCounts(
            savedProjects = items.count { it.source == CreatorCalendarSource.PROJECT },
            weeklyPreviews = items.count { it.source == CreatorCalendarSource.WEEKLY_PLAN },
            completedProjects = items.count {
                it.source == CreatorCalendarSource.PROJECT && it.completed
            },
        )

    fun visibleItems(
        items: List<CreatorCalendarItem>,
        view: UX08ECalendarView,
        selected: LocalDate,
        today: LocalDate,
        zone: ZoneId,
        selectedDayOnly: Boolean = false,
    ): List<CreatorCalendarItem> {
        val period = window(view, selected, today)
        val end = horizonEnd(today)
        val constrainedDay = constrained(selected, today)
        return items.filter { item ->
            val date = Instant.ofEpochMilli(item.atMillis).atZone(zone).toLocalDate()
            !date.isBefore(today) && !date.isAfter(end) &&
                !date.isBefore(period.start) && !date.isAfter(period.endInclusive) &&
                (!selectedDayOnly || date == constrainedDay)
        }
    }

    fun countOnDate(
        items: List<CreatorCalendarItem>, date: LocalDate, zone: ZoneId,
    ): Int = items.count {
        Instant.ofEpochMilli(it.atMillis).atZone(zone).toLocalDate() == date
    }
}
