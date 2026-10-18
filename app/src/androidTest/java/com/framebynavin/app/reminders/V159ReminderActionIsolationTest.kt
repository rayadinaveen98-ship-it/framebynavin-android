package com.framebynavin.app.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Reminder actions must fail closed for old, duplicate and unrelated tasks. */
@RunWith(AndroidJUnit4::class)
class V159ReminderActionIsolationTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun task(): CreatorTask {
        val now = System.currentTimeMillis()
        return CreatorTask(
            id = "v159-action-${UUID.randomUUID()}",
            title = "Edit scenes",
            platform = "YouTube",
            contentType = "Long",
            dueLabel = "Tomorrow",
            reminderEnabled = true,
            reminderMode = ReminderMode.SIMPLE,
            reminderAtMillis = now + 3_600_000L,
            dueAtMillis = now + 86_400_000L,
            attentionPlan = ProjectAttentionPlan.CUSTOM,
        )
    }

    @Test fun onlyOneOfTwoConcurrentActionsCanClaimTheSameOccurrence() {
        val task = task()
        val store = ReminderOccurrenceStore(context)
        try {
            val token = store.issue(task)
            assertTrue(store.matches(task, token))
            assertTrue(store.claim(task, token))
            assertFalse("Replay of action must be rejected", store.claim(task, token))
            assertFalse(store.matches(task, token))
        } finally { store.invalidate(task.id) }
    }

    @Test fun changedScheduleAndFinishedProjectRejectOldNotificationActions() {
        val task = task()
        val store = ReminderOccurrenceStore(context)
        try {
            val token = store.issue(task)
            assertFalse(store.matches(task.copy(reminderAtMillis = task.reminderAtMillis + 60_000L), token))
            assertFalse(store.matches(task.copy(status = TaskStatus.DONE), token))
            assertFalse(store.matches(task.copy(archivedAtMillis = System.currentTimeMillis()), token))
            assertTrue(store.matches(task, token))
        } finally { store.invalidate(task.id) }
    }

    @Test fun oneProjectsNotificationTokenCannotActOnAnotherProject() {
        val one = task()
        val two = task()
        val store = ReminderOccurrenceStore(context)
        try {
            val token = store.issue(one)
            store.issue(two)
            assertFalse(store.matches(two, token))
            assertTrue(store.matches(one, token))
        } finally {
            store.invalidate(one.id)
            store.invalidate(two.id)
        }
    }
}
