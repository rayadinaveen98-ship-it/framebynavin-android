package com.framebynavin.app.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Phase 3D: read-only, evidence-based Home copy for a real CreatorTask.
 *
 * Ranking remains owned by CreatorPriorityEngine. This policy does NOT save projects,
 * start work, infer deadlines from stale human-readable labels or advance stages.
 * Clock/time-zone are injectable so near-midnight and resume behavior is testable.
 */
data class HomeNextMoveSummary(
    val action: String,
    val urgencyLabel: String,
    val dueLabel: String,
    val reason: String,
)

object HomeNextMovePolicy {
    fun summary(
        task: CreatorTask,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): HomeNextMoveSummary {
        val dueMillis = task.dueAtMillis.takeIf { it > 0L }
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val dueDate = dueMillis?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
        val overdue = dueMillis != null && dueMillis < nowMillis
        val dueToday = !overdue && dueDate == today
        val dueTomorrow = !overdue && dueDate == today.plusDays(1)
        val stage = CreatorWorkflowEngine.currentStage(task).label

        val dueLabel = when {
            dueMillis == null -> "NO DEADLINE"
            overdue -> "OVERDUE"
            dueToday -> "DUE TODAY"
            dueTomorrow -> "DUE TOMORROW"
            else -> "DUE " + dueDate!!.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)).uppercase(Locale.ENGLISH)
        }
        val urgency = when {
            overdue -> "OVERDUE"
            dueToday -> "DUE TODAY"
            task.priority == TaskPriority.CRITICAL -> "CRITICAL"
            dueTomorrow -> "DUE TOMORROW"
            task.status == TaskStatus.WORKING -> "CONTINUE"
            else -> "NEXT"
        }
        val reason = when {
            overdue -> "The deadline has passed. ${stage} is the current project step."
            dueToday -> "This project is due today. ${stage} is the current step."
            task.priority == TaskPriority.CRITICAL -> "You marked this project Critical. ${stage} is its current step."
            dueTomorrow -> "This project is due tomorrow. ${stage} is the current step."
            task.status == TaskStatus.WORKING -> "Work is already in progress. Continue ${stage} without switching projects."
            dueMillis != null -> "The saved deadline is ${dueLabel.removePrefix("DUE ")}. ${stage} is the current step."
            task.priority == TaskPriority.IMPORTANT -> "You marked this project Important. ${stage} is the current step; no deadline is set."
            else -> "No deadline is set. ${stage} is the current project step."
        }

        return HomeNextMoveSummary(
            action = CreatorWorkflowEngine.nextAction(task),
            urgencyLabel = urgency,
            dueLabel = dueLabel,
            reason = reason,
        )
    }
}
