package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure scheduling checks: actual AlarmManager/FGS needs Android acceptance. */
class V283CreatorAutoRetryPolicyTest {
    private val due = 1_800_000_000_000L
    private fun task(
        enabled: Boolean = true, mode: ReminderMode = ReminderMode.VOICE,
        max: Int = 2, interval: Int = 5,
    ) = CreatorTask(
        id = "test-audible-creator",
        title = "Upload cinematic analysis",
        platform = "YouTube", contentType = "Long form", dueLabel = "Today",
        status = TaskStatus.PLANNED,
        reminderEnabled = true, reminderAtMillis = due,
        dueAtMillis = due + 6 * 60_000L,
        reminderMode = mode, autoRetryEnabled = enabled,
        autoRetryMinutes = interval, autoRetryMaxAttempts = max,
    )

    @Test fun disabledNotificationAndSmartNeverStartAnotherAudibleAlert() {
        val now = due + 60_000L
        assertNull(V283CreatorAutoRetryPolicy.nextAt(task(enabled = false), 0, now))
        assertNull(V283CreatorAutoRetryPolicy.nextAt(task(mode = ReminderMode.SIMPLE), 0, now))
        assertNull(V283CreatorAutoRetryPolicy.nextAt(task(mode = ReminderMode.SMART), 0, now))
        assertNull(V283CreatorAutoRetryPolicy.nextAt(
            task().copy(pulseManagedReminder = true), 0, now))
        assertNull(V283CreatorAutoRetryPolicy.nextAt(
            task().copy(smartEscalationEnabled = true), 0, now))
    }

    @Test fun manualResponseOrArchivedProjectIsNeverEligible() {
        val now = due + 60_000L
        assertFalse(V283CreatorAutoRetryPolicy.eligible(
            task().copy(status = TaskStatus.DONE), now))
        assertFalse(V283CreatorAutoRetryPolicy.eligible(
            task().copy(archivedAtMillis = due), now))
        assertFalse(V283CreatorAutoRetryPolicy.eligible(task(), due - 1))
    }

    @Test fun configuredIntervalAndLimitAreStrictlyBounded() {
        val at = due + 30_000L
        assertEquals(at + 15 * 60_000L,
            V283CreatorAutoRetryPolicy.nextAt(task(interval = 15), 0, at))
        assertEquals(at + 30 * 60_000L,
            V283CreatorAutoRetryPolicy.nextAt(task(interval = 30), 1, at))
        assertNull(V283CreatorAutoRetryPolicy.nextAt(task(max = 2), 2, at))
        assertNull(V283CreatorAutoRetryPolicy.nextAt(task(max = 2), 3, at))
        assertNull(V283CreatorAutoRetryPolicy.nextAt(task(interval = 7), 0, at))
    }

    @Test fun staleOrVeryLateRetryIsNotAudible() {
        val task = task()
        val at = due + 60_000L
        assertFalse(V283CreatorAutoRetryPolicy.mayDeliver(task, 0, at, at - 1))
        assertTrue(V283CreatorAutoRetryPolicy.mayDeliver(task, 0, at, at))
        assertFalse(V283CreatorAutoRetryPolicy.mayDeliver(
            task, 0, at, at + V283CreatorAutoRetryPolicy.MAX_LATE_MILLIS + 1))
        assertFalse(V283CreatorAutoRetryPolicy.mayDeliver(task, 2, at, at))
        assertNull(V283CreatorAutoRetryPolicy.nextAt(task, 0, due + 25L * 60L * 60_000L))
    }
}
