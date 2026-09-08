package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderReliabilityAlpha18Test {
    private val now = 1_000_000L

    @Test
    fun regularDelivery_requiresCurrentActiveReminderSnapshot() {
        val task = regularTask(reminderAt = now + 60_000L)
        assertTrue(ReminderDeliveryPolicy.canDeliverRegular(task, now + 60_000L))
        assertFalse(ReminderDeliveryPolicy.canDeliverRegular(task, now + 61_000L))
        assertFalse(ReminderDeliveryPolicy.canDeliverRegular(task.copy(status = TaskStatus.DONE), now + 60_000L))
        assertFalse(ReminderDeliveryPolicy.canDeliverRegular(task.copy(reminderEnabled = false), now + 60_000L))
    }

    @Test
    fun regularDelivery_rejectsSmartPendingIntentPath() {
        val task = regularTask(reminderAt = now + 60_000L).copy(
            reminderMode = ReminderMode.SMART,
            smartEscalationEnabled = true,
        )
        assertFalse(ReminderDeliveryPolicy.canDeliverRegular(task, now + 60_000L))
    }

    @Test
    fun smartDelivery_defersDuringWorkingQuietWindow() {
        val task = smartTask(targetAt = now + 60 * 60_000L).copy(workingUntilMillis = now + 15 * 60_000L)
        assertEquals(
            SmartDeliveryDecision.DEFER,
            ReminderDeliveryPolicy.smartDecision(task, task.reminderAtMillis, SmartEscalationScheduler.Stage.SOFT, now),
        )
    }

    @Test
    fun smartDelivery_rejectsStaleTargetAndImpossibleStage() {
        val task = smartTask(targetAt = now + 60 * 60_000L).copy(priority = TaskPriority.IMPORTANT)
        assertEquals(
            SmartDeliveryDecision.DROP,
            ReminderDeliveryPolicy.smartDecision(task, task.reminderAtMillis + 1L, SmartEscalationScheduler.Stage.SOFT, now),
        )
        assertEquals(
            SmartDeliveryDecision.DROP,
            ReminderDeliveryPolicy.smartDecision(task, task.reminderAtMillis, SmartEscalationScheduler.Stage.CRITICAL, now),
        )
    }

    @Test
    fun smartResume_neverBreaksQuietWindowOrFinalTarget() {
        val target = now + 60 * 60_000L
        val planned = now + 30 * 60_000L
        val quietUntil = now + 40 * 60_000L
        assertEquals(
            quietUntil,
            ReminderDeliveryPolicy.smartResumeAt(planned, quietUntil, target, now),
        )
        assertNull(
            ReminderDeliveryPolicy.smartResumeAt(planned, target + 1L, target, now),
        )
    }

    private fun regularTask(reminderAt: Long) = CreatorTask(
        id = "regular",
        title = "Regular",
        platform = "YouTube",
        contentType = "Video",
        dueLabel = "Today",
        dueAtMillis = reminderAt + 60_000L,
        status = TaskStatus.PLANNED,
        reminderEnabled = true,
        reminderAtMillis = reminderAt,
        reminderMode = ReminderMode.SIMPLE,
    )

    private fun smartTask(targetAt: Long) = CreatorTask(
        id = "smart",
        title = "Smart",
        platform = "YouTube",
        contentType = "Video",
        dueLabel = "Today",
        dueAtMillis = targetAt + 60_000L,
        status = TaskStatus.PLANNED,
        reminderEnabled = true,
        reminderAtMillis = targetAt,
        reminderMode = ReminderMode.SMART,
        smartEscalationEnabled = true,
        priority = TaskPriority.CRITICAL,
    )
}
