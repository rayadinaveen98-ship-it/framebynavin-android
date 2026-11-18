package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorCalendarSource
import com.framebynavin.app.data.CreatorPriorityEngine
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ScheduleCadence
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.WeeklyScheduleEngine
import com.framebynavin.app.data.WeeklyScheduleSlot
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** UX08D source tests, NOT executed until the owner-approved post-UX10 campaign. */
class UX08DDailyBriefPolicyTest {
    private val zone = ZoneId.systemDefault()
    private val day = LocalDate.of(2026, 11, 17)
    private val now = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun at(date: LocalDate, hour: Int): Long =
        date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun project(
        id: String,
        due: Long,
        status: TaskStatus = TaskStatus.PLANNED,
        priority: TaskPriority = TaskPriority.IMPORTANT,
        scheduledKey: String = "",
        archived: Boolean = false,
    ) = CreatorTask(
        id = id,
        title = "Project $id",
        platform = "YouTube",
        contentType = "Short",
        dueLabel = "Saved date",
        dueAtMillis = due,
        status = status,
        priority = priority,
        scheduleOccurrenceKey = scheduledKey,
        archivedAtMillis = if (archived) now else 0L,
    )

    @Test
    fun `overdue and later-today are separate real active project sources`() {
        val tasks = listOf(
            project("past", at(day.minusDays(2), 15)),
            project("later", at(day, 18)),
            project("done", at(day.minusDays(1), 9), TaskStatus.DONE),
            project("skipped", at(day, 19), TaskStatus.SKIPPED),
            project("archived", at(day.minusDays(1), 20), archived = true),
        )
        val sections = UX08DDailyBriefPolicy.build(tasks, emptyList(), now, zone)
        assertEquals(2, sections.activeTotal)
        assertEquals(1, sections.overdueTotal)
        assertEquals(1, sections.dueLaterTodayTotal)
        val focusId = CreatorPriorityEngine.rankActive(tasks, now).first().id
        assertFalse(
            (sections.overdue + sections.dueLaterToday).any { it.id == focusId }
        )
    }

    @Test
    fun `focus project appears only in primary action not duplicate secondary cards`() {
        val tasks = listOf(
            project("overdue1", at(day.minusDays(3), 10)),
            project("overdue2", at(day.minusDays(2), 10)),
            project("today", at(day, 16)),
        )
        val focusId = CreatorPriorityEngine.rankActive(tasks, now).first().id
        val snapshot = UX08DDailyBriefPolicy.build(tasks, emptyList(), now, zone)
        assertEquals(2, snapshot.overdueTotal)
        assertTrue(snapshot.focusIsOverdue)
        assertTrue(snapshot.overdue.all { it.id != focusId })
        assertEquals(1, snapshot.dueLaterTodayTotal)
        assertEquals(listOf("today"), snapshot.dueLaterToday.map { it.id })
        assertTrue(snapshot.otherAttention.none { it.taskId == focusId })
    }

    @Test
    fun `upcoming plan remains weekly preview when no project owns occurrence`() {
        val tomorrow = day.plusDays(1)
        val slot = WeeklyScheduleSlot(
            id = "weekly1",
            title = "My weekly video",
            dayOfWeek = tomorrow.dayOfWeek,
            hour = 15,
            minute = 0,
            platform = "YouTube",
            contentType = "Long-form",
            cadence = ScheduleCadence.EVERY_WEEK,
        )
        val sections = UX08DDailyBriefPolicy.build(
            tasks = emptyList(), weeklySlots = listOf(slot),
            nowMillis = now, zone = zone,
        )
        assertEquals(1, sections.upcomingTotal)
        assertEquals(CreatorCalendarSource.WEEKLY_PLAN, sections.upcoming.single().source)
        assertFalse(sections.isEmpty)
        // Viewing a preview never creates a task.
        assertEquals(0, sections.activeTotal)
    }

    @Test
    fun `saved project suppresses duplicate weekly occurrence identity`() {
        val tomorrow = day.plusDays(1)
        val key = WeeklyScheduleEngine.occurrenceKey("weekly1", tomorrow)
        val slot = WeeklyScheduleSlot(
            id = "weekly1", title = "Weekly master",
            dayOfWeek = tomorrow.dayOfWeek, hour = 15, minute = 0,
            platform = "YouTube", contentType = "Short",
        )
        val original = project(
            "owned", at(tomorrow, 15),
            scheduledKey = key,
        )
        val sections = UX08DDailyBriefPolicy.build(
            listOf(original), listOf(slot), now, zone,
        )
        assertEquals(1, sections.upcomingTotal)
        assertEquals(CreatorCalendarSource.PROJECT, sections.upcoming.single().source)
        assertEquals("owned", sections.upcoming.single().taskId)
    }

    @Test
    fun `honestly empty only when no active tasks and no upcoming occurrence`() {
        val onlyCompleted = listOf(
            project("done", at(day.minusDays(2), 12), TaskStatus.DONE),
        )
        val empty = UX08DDailyBriefPolicy.build(
            onlyCompleted, emptyList(), now, zone,
        )
        assertTrue(empty.isEmpty)
        assertEquals(0, empty.overdueTotal)
        assertEquals(0, empty.dueLaterTodayTotal)
        assertEquals(0, empty.upcomingTotal)
    }

    @Test
    fun `bounded displays do not erase additional owner projects or plan records`() {
        val many = (1..12).map { n ->
            project("due$n", at(day.minusDays(n.toLong()), 9))
        }
        val sections = UX08DDailyBriefPolicy.build(many, emptyList(), now, zone)
        assertEquals(12, sections.overdueTotal)
        assertTrue(sections.overdue.size <= UX08DDailyBriefPolicy.MAX_OVERDUE)
        assertEquals(12, many.size)
        assertTrue(many.all { it.status == TaskStatus.PLANNED })
    }
}
