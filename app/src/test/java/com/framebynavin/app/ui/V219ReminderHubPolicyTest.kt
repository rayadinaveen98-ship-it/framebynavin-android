package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderCenterGroup
import com.framebynavin.app.data.ReminderCenterPolicy
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V219ReminderHubPolicyTest {
    private val now = 1_800_000_000_000L
    private fun reminder(id: String, next: Long, snooze: Int = 0) =
        CreatorTask(
            id = id, title = "Film work $id", platform = "YouTube",
            contentType = "Video", dueLabel = "Soon",
            dueAtMillis = now + 14L * 24 * 60 * 60_000L,
            reminderEnabled = true, reminderAtMillis = next,
            reminderMode = ReminderMode.SIMPLE,
            attentionPlan = ProjectAttentionPlan.CUSTOM, snoozeCount = snooze,
        )

    @Test fun homeCountsMatchCanonicalCenterGroupsWithoutDuplicatingStatuses() {
        val tasks = listOf(
            reminder("urgent", now - 10L * 60_000L),
            reminder("today", now + 2L * 60 * 60_000L),
            reminder("postponed", now + 3L * 60 * 60_000L, snooze = 1),
            reminder("upcoming", now + 3L * 24 * 60 * 60_000L),
            reminder("finished", now + 60_000L).copy(status = TaskStatus.DONE),
        )
        val grouped = ReminderCenterPolicy.grouped(tasks, now)
        val s = V219ReminderHomeSnapshot.from(tasks, now)
        assertEquals(grouped[ReminderCenterGroup.NEEDS_ATTENTION].orEmpty().size, s.attention)
        assertEquals(grouped[ReminderCenterGroup.TODAY].orEmpty().size, s.today)
        assertEquals(grouped[ReminderCenterGroup.SNOOZED_PAUSED].orEmpty().size, s.postponed)
        assertEquals(grouped[ReminderCenterGroup.UPCOMING].orEmpty().size, s.upcoming)
        assertEquals(4, s.total)
        assertEquals(4, V219ReminderHubFilter.ALL.count(grouped))
        assertEquals(1, V219ReminderHubFilter.POSTPONED.count(grouped))
        assertEquals(1, V219ReminderHubFilter.UPCOMING.count(grouped))
    }

    @Test fun filtersReadOnlyAndEveryCategoryHasExactlyOneCorrespondingFilter() {
        val filters = V219ReminderHubFilter.entries
        assertEquals(5, filters.size)
        assertTrue(ReminderCenterGroup.entries.all { group ->
            filters.count { it.category == group } == 1
        })
        for (group in ReminderCenterGroup.entries) {
            assertTrue(V219ReminderHubFilter.ALL.includes(group))
            assertEquals(1, filters.count { it != V219ReminderHubFilter.ALL && it.includes(group) })
        }
        assertFalse(V219ReminderHubFilter.TODAY.includes(ReminderCenterGroup.UPCOMING))
        assertTrue(V219ReminderHubFilter.UPCOMING.includes(ReminderCenterGroup.UPCOMING))
    }

    @Test fun emptyAndDoneTasksNeverShowAsActiveReminder() {
        assertEquals(0, V219ReminderHomeSnapshot.from(emptyList(), now).total)
        val done = reminder("done", now + 100_000L).copy(status = TaskStatus.DONE)
        assertEquals(0, V219ReminderHomeSnapshot.from(listOf(done), now).total)
    }
}
