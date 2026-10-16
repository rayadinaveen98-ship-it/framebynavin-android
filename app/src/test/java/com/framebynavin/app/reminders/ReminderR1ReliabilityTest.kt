package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderR1ReliabilityTest {
    private val target = 10_000_000L

    private fun smartTask(
        dueAt: Long = target + 30 * 60_000L,
        status: TaskStatus = TaskStatus.PLANNED,
    ): CreatorTask = CreatorTask(
        id = "r1-smart", title = "Important video", platform = "YouTube",
        contentType = "Long", dueLabel = "Soon", dueAtMillis = dueAt,
        reminderEnabled = true, reminderAtMillis = target,
        reminderMode = ReminderMode.SMART, smartEscalationEnabled = true,
        priority = TaskPriority.CRITICAL, status = status,
    )

    @Test fun androidDelayAfterSmartTargetTriggersBoundedNotificationInsteadOfSilentDrop() {
        val task = smartTask()
        assertEquals(
            SmartDeliveryDecision.RECOVER_LATE,
            ReminderDeliveryPolicy.smartDecision(
                task, target, SmartEscalationScheduler.Stage.CRITICAL, target + 20_000L,
            ),
        )
    }

    @Test fun lateSoftSmartStageAlsoRecoversWithoutRestartingEscalation() {
        val task = smartTask()
        assertEquals(
            SmartDeliveryDecision.RECOVER_LATE,
            ReminderDeliveryPolicy.smartDecision(
                task, target, SmartEscalationScheduler.Stage.SOFT, target + 60_000L,
            ),
        )
    }

    @Test fun staleSmartReminderAfterRecoveryGraceDoesNotInterruptCreator() {
        val task = smartTask(dueAt = target)
        assertEquals(
            SmartDeliveryDecision.DROP,
            ReminderDeliveryPolicy.smartDecision(
                task, target, SmartEscalationScheduler.Stage.CRITICAL,
                target + 6 * 60 * 60_000L + 1L,
            ),
        )
    }

    @Test fun creatorWorkingQuietWindowBlocksLateCatchup() {
        val now = target + 60_000L
        val task = smartTask().copy(workingUntilMillis = now + 60_000L)
        assertEquals(
            SmartDeliveryDecision.DROP,
            ReminderDeliveryPolicy.smartDecision(
                task, target, SmartEscalationScheduler.Stage.SOFT, now,
            ),
        )
    }

    @Test fun completedOrRescheduledSmartReminderDoesNotRecoverOutdatedOccurrence() {
        val completed = smartTask(status = TaskStatus.DONE)
        assertEquals(
            SmartDeliveryDecision.DROP,
            ReminderDeliveryPolicy.smartDecision(
                completed, target, SmartEscalationScheduler.Stage.SOFT, target + 60_000L,
            ),
        )
        val rescheduled = smartTask().copy(reminderAtMillis = target + 60 * 60_000L)
        assertEquals(
            SmartDeliveryDecision.DROP,
            ReminderDeliveryPolicy.smartDecision(
                rescheduled, target, SmartEscalationScheduler.Stage.SOFT, target + 60_000L,
            ),
        )
    }

    @Test fun onTimeSmartDeliveryRemainsTheExistingDeliveryPath() {
        val task = smartTask()
        assertEquals(
            SmartDeliveryDecision.DELIVER,
            ReminderDeliveryPolicy.smartDecision(
                task, target, SmartEscalationScheduler.Stage.SOFT, target - 60_000L,
            ),
        )
    }
}
