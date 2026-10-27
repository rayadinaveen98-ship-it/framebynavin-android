package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus

/**
 * Selects ONE truthful next-alert preview for the already-canonical V219
 * Reminder Hub. This does not replace ReminderCenterPolicy's group counts.
 * A stale past due time is not presented as an upcoming alert.
 */
internal data class V220HomeReminderPreview(
    val task: CreatorTask,
    val remindingNow: Boolean,
)

internal object V220HomeReminderPreviewPolicy {
    fun pick(
        tasks: List<CreatorTask>,
        nowMillis: Long,
        activeSessionTaskIds: Set<String> = emptySet(),
    ): V220HomeReminderPreview? {
        val candidate = tasks.asSequence()
            .filter {
                (it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING) &&
                    it.reminderEnabled && it.reminderMode != ReminderMode.NONE
            }
            .filter { it.id in activeSessionTaskIds || it.reminderAtMillis >= nowMillis }
            .sortedWith(
                compareByDescending<CreatorTask> { it.id in activeSessionTaskIds }
                    .thenBy { it.reminderAtMillis.takeIf { at -> at > 0L } ?: Long.MAX_VALUE }
                    .thenBy { it.id }
            )
            .firstOrNull() ?: return null

        return V220HomeReminderPreview(
            task = candidate,
            remindingNow = candidate.id in activeSessionTaskIds,
        )
    }
}
