package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorCalendarItem
import com.framebynavin.app.data.CreatorCalendarSource
import com.framebynavin.app.data.CreatorCalendarNavigationPolicy
import com.framebynavin.app.data.CreatorContentCalendarEngine
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.WeeklyScheduleEngine
import com.framebynavin.app.data.WeeklyScheduleSlot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * UX08E authored source tests only.
 * Per founder approval, DO NOT EXECUTE Gradle/JVM/instrumentation/physical QA
 * before the single cumulative UX01–UX10 certification gate.
 */
class UX08ECalendarPolicyTest {
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 11, 17)

    private fun at(day: LocalDate): Long =
        day.atTime(15, 0).atZone(zone).toInstant().toEpochMilli()

    private fun item(
        date: LocalDate,
        source: CreatorCalendarSource,
        key: String,
        completed: Boolean = false,
    ) = CreatorCalendarItem(
        id = key, title = "Saved source",
        platform = "YouTube", contentType = "Short",
        atMillis = at(date), source = source,
        taskId = if (source == CreatorCalendarSource.PROJECT) key else "",
        completed = completed,
    )

    @Test
    fun `planning window has exactly ninety future inclusive local dates`() {
        val end = today.plusDays(89)
        assertEquals(end, UX08ECalendarPolicy.horizonEnd(today))
        assertTrue(UX08ECalendarPolicy.isSelectable(today, today))
        assertTrue(UX08ECalendarPolicy.isSelectable(end, today))
        assertFalse(UX08ECalendarPolicy.isSelectable(today.minusDays(1), today))
        assertFalse(UX08ECalendarPolicy.isSelectable(end.plusDays(1), today))
        assertEquals(
            today, UX08ECalendarPolicy.constrained(today.minusYears(1), today),
        )
        assertEquals(
            end, UX08ECalendarPolicy.constrained(today.plusYears(1), today),
        )
    }

    @Test
    fun `week mode navigates seven dates without allowing a fully past week`() {
        val mode = UX08ECalendarView.WEEK
        val period = UX08ECalendarPolicy.window(mode, today, today)
        assertEquals(LocalDate.of(2026, 11, 16), period.start)
        assertEquals(LocalDate.of(2026, 11, 22), period.endInclusive)
        assertNull(UX08ECalendarPolicy.shift(mode, today, today, -1L))
        assertEquals(today.plusWeeks(1), UX08ECalendarPolicy.shift(
            mode, today, today, 1L,
        ))
    }

    @Test
    fun `month grid starts on Monday and covers leap February`() {
        val feb = YearMonth.of(2028, 2)
        val dates = UX08ECalendarPolicy.monthGrid(feb)
        assertEquals(42, dates.size)
        assertEquals(DayOfWeek.MONDAY, dates.first().dayOfWeek)
        assertTrue(LocalDate.of(2028, 2, 29) in dates)
        assertTrue(dates.zipWithNext().all { (one, two) ->
            two == one.plusDays(1)
        })
        assertEquals(
            YearMonth.of(2026, 12),
            YearMonth.from(
                UX08ECalendarPolicy.shift(
                    UX08ECalendarView.MONTH, today, today, 1L,
                )!!
            ),
        )
        assertNull(UX08ECalendarPolicy.shift(
            UX08ECalendarView.MONTH, today, today, -1L,
        ))
    }

    @Test
    fun `agenda begins at selected date and advances by fourteen days`() {
        val win = UX08ECalendarPolicy.window(
            UX08ECalendarView.AGENDA, today, today,
        )
        assertEquals(today, win.start)
        assertEquals(today.plusDays(13), win.endInclusive)
        assertEquals(today.plusDays(14), UX08ECalendarPolicy.shift(
            UX08ECalendarView.AGENDA, today, today, 1L,
        ))
        assertNull(UX08ECalendarPolicy.shift(
            UX08ECalendarView.AGENDA, today, today, -1L,
        ))
    }

    @Test
    fun `visible groups are real items in the chosen week, month and day`() {
        val data = listOf(
            item(today, CreatorCalendarSource.PROJECT, "today"),
            item(today.plusDays(2), CreatorCalendarSource.WEEKLY_PLAN, "this-week"),
            item(today.plusDays(9), CreatorCalendarSource.PROJECT, "next-week"),
            item(today.minusDays(1), CreatorCalendarSource.PROJECT, "past"),
            item(today.plusDays(91), CreatorCalendarSource.WEEKLY_PLAN, "beyond"),
        )
        val week = UX08ECalendarPolicy.visibleItems(
            data, UX08ECalendarView.WEEK, today, today, zone,
        )
        assertEquals(listOf("today", "this-week"), week.map { it.id })
        val exactDay = UX08ECalendarPolicy.visibleItems(
            data, UX08ECalendarView.WEEK, today, today, zone,
            selectedDayOnly = true,
        )
        assertEquals(listOf("today"), exactDay.map { it.id })
        val month = UX08ECalendarPolicy.visibleItems(
            data, UX08ECalendarView.MONTH, today, today, zone,
        )
        assertEquals(3, month.size)
        assertFalse(month.any { it.id == "past" || it.id == "beyond" })
        assertEquals(
            1, UX08ECalendarPolicy.countOnDate(data, today.plusDays(9), zone),
        )
    }

    @Test
    fun `inspecting an agenda date does not shift its fourteen-day period`() {
        val twoDaysAhead = today.plusDays(2)
        val events = listOf(
            item(today, CreatorCalendarSource.PROJECT, "start"),
            item(twoDaysAhead, CreatorCalendarSource.WEEKLY_PLAN, "chosen"),
            item(today.plusDays(8), CreatorCalendarSource.PROJECT, "later"),
        )
        val filtered = UX08ECalendarPolicy.visibleItems(
            events, UX08ECalendarView.AGENDA, today, today, zone,
            selectedDayOnly = true, focusedDay = twoDaysAhead,
        )
        assertEquals(listOf("chosen"), filtered.map { it.id })
        val unchanged = UX08ECalendarPolicy.window(
            UX08ECalendarView.AGENDA, today, today,
        )
        assertEquals(today, unchanged.start)
        assertEquals(today.plusDays(13), unchanged.endInclusive)
    }

    @Test
    fun `project and weekly previews stay distinct with separate completion counts`() {
        val data = listOf(
            item(today, CreatorCalendarSource.PROJECT, "active"),
            item(today.plusDays(1), CreatorCalendarSource.PROJECT, "done", completed = true),
            item(today.plusDays(1), CreatorCalendarSource.WEEKLY_PLAN, "template"),
        )
        assertEquals(
            UX08ECalendarCounts(
                savedProjects = 2, weeklyPreviews = 1, completedProjects = 1,
            ),
            UX08ECalendarPolicy.count(data),
        )
        assertTrue(CreatorCalendarNavigationPolicy.isProjectLink(data.first()))
        assertFalse(CreatorCalendarNavigationPolicy.isProjectLink(data.last()))
        assertTrue(CreatorCalendarNavigationPolicy.kindLabel(data.last())
            .contains("not a project"))
    }

    @Test
    fun `large source stays fully reachable via bounded calendar pages`() {
        val source = (1..173).map { n ->
            item(today, CreatorCalendarSource.PROJECT, "project$n")
        }
        val first = UX08ECalendarPolicy.page(source, 0)
        assertEquals(80, first.items.size)
        assertEquals(0, first.index)
        assertEquals(2, first.maxIndex)
        val second = UX08ECalendarPolicy.page(source, 1)
        assertEquals(80, second.items.size)
        assertEquals("project81", second.items.first().id)
        val last = UX08ECalendarPolicy.page(source, 200)
        assertEquals(2, last.index)
        assertEquals(160, last.offset)
        assertEquals(13, last.items.size)
        assertEquals("project173", last.items.last().id)
        assertEquals(173, source.size)
        val empty = UX08ECalendarPolicy.page(emptyList(), 1)
        assertEquals(0, empty.index)
        assertTrue(empty.items.isEmpty())
    }

    @Test
    fun `canonical occurrence key is still deduped by existing calendar engine`() {
        val tomorrow = today.plusDays(1)
        val slot = WeeklyScheduleSlot(
            id = "publish-weekly", title = "Weekly video",
            dayOfWeek = tomorrow.dayOfWeek,
            hour = 15, minute = 0,
            platform = "YouTube", contentType = "Short",
        )
        val original = CreatorTask(
            id = "real-project", title = "Real project",
            platform = "YouTube", contentType = "Short",
            dueLabel = "Scheduled", dueAtMillis = at(tomorrow),
            scheduleOccurrenceKey = WeeklyScheduleEngine.occurrenceKey(
                slot.id, tomorrow,
            ),
        )
        val items = CreatorContentCalendarEngine.upcoming(
            tasks = listOf(original), weeklySlots = listOf(slot),
            nowMillis = today.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
            daysAhead = UX08ECalendarPolicy.PLANNING_DAYS,
        )
        assertEquals(
            1, items.count { it.taskId == original.id },
        )
        assertFalse(items.any {
            it.source == CreatorCalendarSource.WEEKLY_PLAN &&
                it.id == "weekly:" + original.scheduleOccurrenceKey
        })
        assertEquals(original.scheduleOccurrenceKey,
            WeeklyScheduleEngine.occurrenceKey(slot.id, tomorrow))
    }
}
