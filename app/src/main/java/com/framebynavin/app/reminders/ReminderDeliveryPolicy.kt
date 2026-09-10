package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus

enum class SmartDeliveryDecision { DELIVER, DEFER, DROP }

/** Pure reliability rules shared by AlarmManager delivery and recovery paths. */
object ReminderDeliveryPolicy {
    fun canDeliverRegular(task: CreatorTask?, scheduledAtMillis: Long): Boolean {
        if (task == null || scheduledAtMillis <= 0L) return false
        return isActive(task) &&
            task.reminderMode != ReminderMode.SMART &&
            !task.smartEscalationEnabled &&
            task.reminderAtMillis == scheduledAtMillis
    }

    fun smartDecision(
        task: CreatorTask?,
        targetAtMillis: Long,
        stage: SmartEscalationScheduler.Stage,
        nowMillis: Long,
    ): SmartDeliveryDecision {
        if (task == null || targetAtMillis <= 0L) return SmartDeliveryDecision.DROP
        if (!isActive(task)) return SmartDeliveryDecision.DROP
        if (task.reminderMode != ReminderMode.SMART && !task.smartEscalationEnabled) return SmartDeliveryDecision.DROP
        if (task.reminderAtMillis != targetAtMillis) return SmartDeliveryDecision.DROP
        if (task.dueAtMillis > 0L && task.reminderAtMillis > task.dueAtMillis) return SmartDeliveryDecision.DROP
        if (!stageAllowed(task.priority, stage)) return SmartDeliveryDecision.DROP
        // reminderAtMillis is the creator's hard FINAL Smart target, not a soft anchor.
        if (nowMillis > targetAtMillis) return SmartDeliveryDecision.DROP
        if (task.workingUntilMillis > nowMillis) {
            return if (task.workingUntilMillis <= targetAtMillis) SmartDeliveryDecision.DEFER else SmartDeliveryDecision.DROP
        }
        return SmartDeliveryDecision.DELIVER
    }

    fun smartResumeAt(
        plannedAtMillis: Long,
        workingUntilMillis: Long,
        finalTargetAtMillis: Long,
        nowMillis: Long,
    ): Long? {
        val candidate = maxOf(plannedAtMillis, workingUntilMillis)
        return candidate.takeIf { it > nowMillis && it <= finalTargetAtMillis }
    }

    private fun isActive(task: CreatorTask): Boolean =
        task.reminderEnabled &&
            task.reminderMode != ReminderMode.NONE &&
            task.status != TaskStatus.DONE &&
            task.status != TaskStatus.SKIPPED

    private fun stageAllowed(priority: TaskPriority, stage: SmartEscalationScheduler.Stage): Boolean = when (priority) {
        TaskPriority.NORMAL -> stage == SmartEscalationScheduler.Stage.SOFT
        TaskPriority.IMPORTANT -> stage != SmartEscalationScheduler.Stage.CRITICAL
        TaskPriority.CRITICAL -> true
    }
}
