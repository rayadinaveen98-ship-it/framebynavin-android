package com.framebynavin.app.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CreatorCalendarNavigationPolicyV163Test {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val date = LocalDate.of(2026, 10, 16)
    private val now = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private fun at(d: LocalDate, h: Int) = d.atTime(h, 0).atZone(zone).toInstant().toEpochMilli()
    private fun task(id: String, due: Long, state: TaskStatus = TaskStatus.PLANNED, archived: Long = 0L) =
        CreatorTask(id = id, title = id, platform = "YouTube", contentType = "Short", dueLabel = "",
            dueAtMillis = due, status = state, archivedAtMillis = archived)

    @Test fun overdueIncludesOnlyActualActiveRecentProjects() {
        val tasks = listOf(
            task("late", at(date, 9)),
            task("working", at(date.minusDays(1), 18), TaskStatus.WORKING),
            task("old", at(date.minusDays(20), 10)),
            task("done", at(date, 9), TaskStatus.DONE),
            task("skipped", at(date, 9), TaskStatus.SKIPPED),
            task("archived", at(date, 9), archived = 10),
            task("future", at(date.plusDays(1), 9)),
        )
        assertEquals(setOf("late", "working"),
            CreatorCalendarNavigationPolicy.recentOverdue(tasks, now, zone = zone).map { it.id }.toSet())
    }

    @Test fun selectedDayUsesLocalDateNotUtcCalendarDay() {
        val today = CreatorCalendarItem("project:p", "film", "YouTube", "Short",
            at(date, 1), CreatorCalendarSource.PROJECT, taskId = "p")
        val tomorrow = today.copy(id = "weekly:s", source = CreatorCalendarSource.WEEKLY_PLAN,
            atMillis = at(date.plusDays(1), 1), taskId = "")
        assertEquals(listOf(today),
            CreatorCalendarNavigationPolicy.forDay(listOf(today, tomorrow), date, zone))
    }

    @Test fun weeklyPreviewNeverPretendsToBeProjectAndLinksAreExplicit() {
        val project = CreatorCalendarItem("project:p", "Short", "YouTube", "Short", now,
            CreatorCalendarSource.PROJECT, taskId = "p")
        val weekly = CreatorCalendarItem("weekly:key", "Weekly slot", "YouTube", "Short", now,
            CreatorCalendarSource.WEEKLY_PLAN)
        assertTrue(CreatorCalendarNavigationPolicy.isProjectLink(project))
        assertFalse(CreatorCalendarNavigationPolicy.isProjectLink(weekly))
        assertTrue(CreatorCalendarNavigationPolicy.kindLabel(weekly).contains("not a project yet"))
        assertEquals("Completed project", CreatorCalendarNavigationPolicy.kindLabel(project.copy(completed = true)))
    }

    @Test fun upcomingRangeDoesNotShowPastUnfinishedDeadlineAsFutureEvent() {
        val future = task("future", at(date.plusDays(13), 16))
        val tooFar = task("far", at(date.plusDays(14), 16))
        val late = task("late", at(date, 9))
        val items = CreatorContentCalendarEngine.upcoming(
            listOf(future, tooFar, late), emptyList(), nowMillis = now, daysAhead = 14)
        assertEquals(listOf("project:future"), items.map { it.id })
    }
}
