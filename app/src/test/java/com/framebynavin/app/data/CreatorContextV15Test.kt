package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class CreatorContextV15Test {
    private val now = 1_800_000_000_000L

    @Test
    fun contextNudgeFlagsLowProgressProjectDueSoon() {
        val task = task(
            id = "soon",
            title = "Soon project",
            due = now + 2 * 60 * 60_000L,
            priority = TaskPriority.IMPORTANT,
            progress = 20,
        )

        val nudge = CreatorContextNudgeEngine.topNudge(listOf(task), now)

        assertEquals("soon", nudge?.taskId)
        assertEquals(CreatorContextNudgeLevel.NOW, nudge?.level)
        assertTrue(nudge?.message?.contains("Due in about") == true)
    }

    @Test
    fun dailyBriefUsesSmartPriorityForFocus() {
        val normal = task(
            id = "normal",
            title = "Normal",
            due = now + 2 * 24 * 60 * 60_000L,
            priority = TaskPriority.NORMAL,
            progress = 60,
        )
        val urgent = task(
            id = "urgent",
            title = "Urgent",
            due = now + 60 * 60_000L,
            priority = TaskPriority.CRITICAL,
            progress = 10,
        )

        val brief = CreatorDailyBriefEngine.build(listOf(normal, urgent), emptyList(), now)

        assertEquals("urgent", brief.focusTask?.id)
        assertEquals(2, brief.activeCount)
        assertTrue(brief.focusAction.isNotBlank())
    }

    @Test
    fun contentCalendarCombinesProjectsAndFutureWeeklySlots() {
        val zone = ZoneId.systemDefault()
        val tomorrow = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1)
        val project = task(
            id = "project",
            title = "Project deadline",
            due = tomorrow.atTime(10, 0).atZone(zone).toInstant().toEpochMilli(),
            priority = TaskPriority.IMPORTANT,
            progress = 30,
        )
        val slot = WeeklyScheduleSlot(
            id = "weekly-test",
            title = "Weekly post",
            dayOfWeek = tomorrow.dayOfWeek,
            hour = 12,
            minute = 0,
            platform = "Instagram",
            contentType = "Post",
            enabled = true,
            reminderMode = ReminderMode.NONE,
            priority = TaskPriority.NORMAL,
        )

        val items = CreatorContentCalendarEngine.upcoming(listOf(project), listOf(slot), now, 3)

        assertTrue(items.any { it.source == CreatorCalendarSource.PROJECT && it.taskId == "project" })
        assertTrue(items.any { it.source == CreatorCalendarSource.WEEKLY_PLAN && it.title == "Weekly post" })
    }

    private fun task(
        id: String,
        title: String,
        due: Long,
        priority: TaskPriority,
        progress: Int,
    ) = CreatorTask(
        id = id,
        title = title,
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "Soon",
        dueAtMillis = due,
        status = TaskStatus.PLANNED,
        progress = progress,
        workflowStageIndex = CreatorWorkflowEngine.stageIndexFromProgress(progress, CreatorWorkflowEngine.templateFor("YouTube", "Long-form").stages.size),
        priority = priority,
    )
}
