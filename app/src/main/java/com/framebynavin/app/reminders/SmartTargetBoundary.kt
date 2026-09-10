package com.framebynavin.app.reminders

import com.framebynavin.app.data.TaskPriority

data class BoundedSmartStage(
    val stage: SmartEscalationScheduler.Stage,
    val atMillis: Long,
)

/** Pure RC3 rule: no Smart stage may be planned beyond the creator's final target. */
object SmartTargetBoundary {
    fun bound(
        priority: TaskPriority,
        preferredStage: SmartEscalationScheduler.Stage,
        candidateAtMillis: Long,
        finalTargetAtMillis: Long,
    ): BoundedSmartStage? {
        if (finalTargetAtMillis <= 0L || candidateAtMillis <= 0L) return null
        if (candidateAtMillis <= finalTargetAtMillis) {
            return BoundedSmartStage(preferredStage, candidateAtMillis)
        }
        return BoundedSmartStage(finalStage(priority), finalTargetAtMillis)
    }

    fun finalStage(priority: TaskPriority): SmartEscalationScheduler.Stage = when (priority) {
        TaskPriority.NORMAL -> SmartEscalationScheduler.Stage.SOFT
        TaskPriority.IMPORTANT -> SmartEscalationScheduler.Stage.ALARM
        TaskPriority.CRITICAL -> SmartEscalationScheduler.Stage.CRITICAL
    }
}
