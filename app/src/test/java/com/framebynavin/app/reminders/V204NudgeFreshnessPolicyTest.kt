package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorContextNudge
import com.framebynavin.app.data.CreatorContextNudgeLevel
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V204NudgeFreshnessPolicyTest {
    private val now = 1_800_000_000_000L
    private val hour = 3_600_000L
    private val day = 24 * hour

    private fun task(
        due: Long = now - day,
        reminder: Long = now - hour,
        enabled: Boolean = true,
        status: TaskStatus = TaskStatus.WORKING,
        archived: Long = 0L,
    ) = CreatorTask(
        id = "project-123",
        title = "A real creator project",
        platform = "YouTube",
        contentType = "Video",
        dueLabel = "Tomorrow",
        status = status,
        archivedAtMillis = archived,
        dueAtMillis = due,
        reminderAtMillis = reminder,
        reminderEnabled = enabled,
    )

    private fun nudge(prefix: String, due: Long = now - day, taskId: String = "project-123") =
        CreatorContextNudge(
            key = "$prefix:project-123:edit",
            taskId = taskId,
            title = "A real creator project",
            level = CreatorContextNudgeLevel.NOW,
            message = "Actual workflow status",
            action = "Open project",
            dueAtMillis = due,
        )

    @Test fun recentOverdueIsRelevantButAncientOverdueIsNot() {
        assertTrue(V204NudgeFreshnessPolicy.relevant(nudge("overdue"), task(), now))
        assertTrue(V204NudgeFreshnessPolicy.relevant(nudge("overdue"), task(due = now - 7 * day), now))
        assertFalse(V204NudgeFreshnessPolicy.relevant(nudge("overdue"), task(due = now - 8 * day), now))
        assertFalse(V204NudgeFreshnessPolicy.relevant(nudge("overdue"), task(due = 0L), now))
    }

    @Test fun missedReminderExpiresAndRespectsWhetherReminderWasEnabled() {
        assertTrue(V204NudgeFreshnessPolicy.relevant(nudge("missed-reminder"), task(), now))
        assertFalse(V204NudgeFreshnessPolicy.relevant(nudge("missed-reminder"), task(reminder = now - 25 * hour), now))
        assertFalse(V204NudgeFreshnessPolicy.relevant(nudge("missed-reminder"), task(enabled = false), now))
        assertFalse(V204NudgeFreshnessPolicy.relevant(nudge("missed-reminder"), task(reminder = now + hour), now))
    }

    @Test fun futureRiskMustMatchActualFutureDueDate() {
        val future = now + 4 * hour
        assertTrue(V204NudgeFreshnessPolicy.relevant(nudge("risk-6h", future), task(due = future), now))
        assertTrue(V204NudgeFreshnessPolicy.relevant(nudge("risk-24h", future), task(due = future), now))
        assertTrue(V204NudgeFreshnessPolicy.relevant(nudge("critical-48h", future), task(due = future), now))
        assertFalse(V204NudgeFreshnessPolicy.relevant(nudge("risk-6h", future), task(due = now - day), now))
        assertFalse(V204NudgeFreshnessPolicy.relevant(nudge("risk-24h", future), task(due = now + 6 * hour), now))
    }

    @Test fun inactiveArchivedMissingAndUnknownProjectNotificationsAreRejected() {
        val active = task()
        assertFalse(V204NudgeFreshnessPolicy.relevant(nudge("overdue"), null, now))
        assertFalse(V204NudgeFreshnessPolicy.relevant(nudge("overdue", taskId = "another-id"), active, now))
        assertFalse(V204NudgeFreshnessPolicy.relevant(nudge("overdue"), task(status = TaskStatus.DONE), now))
        assertFalse(V204NudgeFreshnessPolicy.relevant(nudge("overdue"), task(status = TaskStatus.SKIPPED), now))
        assertFalse(V204NudgeFreshnessPolicy.relevant(nudge("overdue"), task(archived = now - hour), now))
        assertFalse(V204NudgeFreshnessPolicy.relevant(nudge("ready"), active, now))
        assertFalse(V204NudgeFreshnessPolicy.relevant(nudge("unknown"), active, now))
    }
}
