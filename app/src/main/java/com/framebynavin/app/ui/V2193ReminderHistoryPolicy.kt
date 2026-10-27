package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.reminders.ReminderAckRecord
import com.framebynavin.app.reminders.ReminderAckState

/**
 * V219.3 HISTORY IS EVIDENCE, NOT AN ACTIVITY ESTIMATE.
 *
 * Reminder responses are only explicit, committed RESPONDED journal entries.
 * A notification posted, a service running, a timeout, or a delivered sound
 * never counts as a completed/acknowledged reminder.
 *
 * Separately, DONE projects are shown as project completions, not reminder
 * completions. Legacy DONE projects may have no reliable completion timestamp.
 */
internal data class V2193ConfirmedReminderResponse(
    val taskId: String,
    val projectTitle: String,
    val action: String,
    val atMillis: Long,
    val occurrenceId: String,
)

internal data class V2193CompletedProject(
    val taskId: String,
    val title: String,
    val completedAtMillis: Long,
) {
    val timeVerified: Boolean get() = completedAtMillis > 0L
}

internal object V2193ReminderHistoryPolicy {
    fun confirmedResponses(
        tasks: List<CreatorTask>,
        recordsByTask: Map<String, List<ReminderAckRecord>>,
        maxEntries: Int = 60,
    ): List<V2193ConfirmedReminderResponse> {
        val byId = tasks.associateBy { it.id }
        return recordsByTask.entries.flatMap { (taskId, records) ->
            val task = byId[taskId] ?: return@flatMap emptyList()
            records.asSequence()
                .filter { record ->
                    record.taskId == taskId &&
                        record.state == ReminderAckState.RESPONDED &&
                        record.changedAtMillis > 0L &&
                        record.occurrenceId.isNotBlank()
                }
                .map { record ->
                    V2193ConfirmedReminderResponse(
                        taskId = taskId,
                        projectTitle = task.title,
                        action = V181ReminderInboxPolicy.recentLabel(record),
                        atMillis = record.changedAtMillis,
                        occurrenceId = record.occurrenceId,
                    )
                }.toList()
        }.distinctBy { it.taskId to it.occurrenceId }
            .sortedWith(
                compareByDescending<V2193ConfirmedReminderResponse> { it.atMillis }
                    .thenBy { it.taskId }
            )
            .take(maxEntries.coerceIn(0, 100))
    }

    fun completedProjects(tasks: List<CreatorTask>): List<V2193CompletedProject> =
        tasks.asSequence()
            .filter { it.status == TaskStatus.DONE && it.archivedAtMillis <= 0L }
            .map { V2193CompletedProject(it.id, it.title, it.completedAtMillis) }
            .sortedWith(
                compareByDescending<V2193CompletedProject> { it.timeVerified }
                    .thenByDescending { it.completedAtMillis }
                    .thenBy { it.title }
            )
            .toList()
}
