package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V182ReminderLifecycleRegressionTest {
    private val now = 1_800_000_000_000L

    private fun task(mode: ReminderMode = ReminderMode.SIMPLE) = CreatorTask(
        id = "v182-regular", title = "Create a film scene",
        platform = "YouTube", contentType = "Long-form", dueLabel = "Today",
        status = TaskStatus.PLANNED, reminderEnabled = true,
        reminderMode = mode,
        reminderAtMillis = now + 10 * 60_000L,
        dueAtMillis = now + 60 * 60_000L,
        priority = TaskPriority.IMPORTANT,
    )

    @Test fun regularDeliveryRequiresCurrentActiveUnarchivedProject() {
        val active = task()
        assertTrue(ReminderDeliveryPolicy.canDeliverRegular(active, active.reminderAtMillis))
        assertFalse(ReminderDeliveryPolicy.canDeliverRegular(
            active.copy(archivedAtMillis = now), active.reminderAtMillis))
        assertFalse(ReminderDeliveryPolicy.canDeliverRegular(
            active.copy(status = TaskStatus.DONE), active.reminderAtMillis))
        assertFalse(ReminderDeliveryPolicy.canDeliverRegular(
            active.copy(reminderEnabled = false), active.reminderAtMillis))
        assertFalse(ReminderDeliveryPolicy.canDeliverRegular(active, active.reminderAtMillis + 1L))
    }

    @Test fun smartEscalationRejectsArchivedOrCompletedProjectBeforePlaying() {
        val active = task(ReminderMode.SMART).copy(smartEscalationEnabled = true)
        assertEquals(SmartDeliveryDecision.DELIVER, ReminderDeliveryPolicy.smartDecision(
            active, active.reminderAtMillis, SmartEscalationScheduler.Stage.SOFT, now))
        assertEquals(SmartDeliveryDecision.DROP, ReminderDeliveryPolicy.smartDecision(
            active.copy(archivedAtMillis = now), active.reminderAtMillis,
            SmartEscalationScheduler.Stage.SOFT, now))
        assertEquals(SmartDeliveryDecision.DROP, ReminderDeliveryPolicy.smartDecision(
            active.copy(status = TaskStatus.SKIPPED), active.reminderAtMillis,
            SmartEscalationScheduler.Stage.SOFT, now))
    }

    @Test fun scheduledReminderMustNotAcknowledgeOrAdvanceArchivedProject() {
        val archived = task().copy(archivedAtMillis = now, progress = 42,
            workflowStageIndex = 2)
        assertFalse(ReminderActionSafety.isActionable(archived))
        assertEquals(archived, ReminderActionSafety.acknowledge(archived))
        assertEquals(archived, ReminderActionSafety.snooze(archived, now + 15 * 60_000L, now))
        assertEquals(archived, ReminderActionSafety.start(archived, now))
    }

    @Test fun missedReminderIsRelevantOnlyWithinItsOriginalExpiryPolicy() {
        assertTrue(MissedReminderRecovery.shouldCatchUp(
            reminderAtMillis = now - 15 * 60_000L,
            dueAtMillis = now + 40 * 60_000L,
            nowMillis = now))
        assertFalse(MissedReminderRecovery.shouldCatchUp(
            reminderAtMillis = now - 12 * 60 * 60_000L,
            dueAtMillis = now - 9 * 60 * 60_000L,
            nowMillis = now))
    }
}
