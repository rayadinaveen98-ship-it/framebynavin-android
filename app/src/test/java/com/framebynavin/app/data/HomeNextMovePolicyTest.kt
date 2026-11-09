package com.framebynavin.app.data

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeNextMovePolicyTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 11, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun project(
        due: Long = 0L,
        label: String = "Tomorrow",
        priority: TaskPriority = TaskPriority.NORMAL,
        status: TaskStatus = TaskStatus.PLANNED,
    ) = CreatorTask(
        id = "home-policy-test",
        title = "Scene analysis",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = label,
        dueAtMillis = due,
        priority = priority,
        status = status,
    )

    @Test fun staleLegacyLabelCannotInventDeadline() {
        val task = project(due = 0L, label = "Today")
        val actual = HomeNextMovePolicy.summary(task, at(9, 10), zone)
        assertEquals("NO DEADLINE", actual.dueLabel)
        assertEquals("NEXT", actual.urgencyLabel)
        assertTrue(actual.reason.contains("No deadline"))
        assertFalse(actual.reason.contains("today"))
    }

    @Test fun overdueWinsEvenWhenSavedLabelClaimsTomorrow() {
        val task = project(due = at(9, 8), label = "Tomorrow", priority = TaskPriority.CRITICAL)
        val actual = HomeNextMovePolicy.summary(task, at(9, 10), zone)
        assertEquals("OVERDUE", actual.dueLabel)
        assertEquals("OVERDUE", actual.urgencyLabel)
        assertTrue(actual.reason.contains("deadline has passed"))
    }

    @Test fun dueTodayUsesLocalCalendarDate() {
        val actual = HomeNextMovePolicy.summary(project(due = at(9, 21)), at(9, 10), zone)
        assertEquals("DUE TODAY", actual.dueLabel)
        assertEquals("DUE TODAY", actual.urgencyLabel)
        assertTrue(actual.reason.contains("due today"))
    }

    @Test fun nearMidnightTomorrowIsNotFalselyDueToday() {
        val actual = HomeNextMovePolicy.summary(
            project(due = at(10, 0, 10), label = "Today"),
            at(9, 23, 50),
            zone,
        )
        assertEquals("DUE TOMORROW", actual.dueLabel)
        assertEquals("DUE TOMORROW", actual.urgencyLabel)
        assertFalse(actual.reason.contains("due today"))
    }

    @Test fun criticalWithoutDeadlineNeverInventsDueDate() {
        val actual = HomeNextMovePolicy.summary(
            project(priority = TaskPriority.CRITICAL),
            at(9, 12), zone,
        )
        assertEquals("NO DEADLINE", actual.dueLabel)
        assertEquals("CRITICAL", actual.urgencyLabel)
        assertTrue(actual.reason.contains("marked this project Critical"))
    }

    @Test fun workingWithoutDeadlineRecommendsContinue() {
        val task = project(priority = TaskPriority.NORMAL, status = TaskStatus.WORKING)
        val actual = HomeNextMovePolicy.summary(task, at(9, 12), zone)
        assertEquals("CONTINUE", actual.urgencyLabel)
        assertEquals(CreatorWorkflowEngine.nextAction(task), actual.action)
        assertTrue(actual.reason.contains("already in progress"))
    }

    @Test fun farFutureDeadlineUsesSavedTimestampNotStaleText() {
        val actual = HomeNextMovePolicy.summary(
            project(due = at(18, 19), label = "Today"),
            at(9, 12), zone,
        )
        assertEquals("DUE 18 NOV", actual.dueLabel)
        assertFalse(actual.reason.contains("due today"))
    }
}
