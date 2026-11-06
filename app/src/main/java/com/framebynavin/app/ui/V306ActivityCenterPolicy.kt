package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.personal.PersonalCompletionEvent
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.V288PersonalTodayPolicy
import com.framebynavin.app.reminders.ReminderAckRecord
import com.framebynavin.app.reminders.ReminderAckState

/**
 * Read-only V306 activity projection. The two reminder systems remain authoritative.
 * In particular: posted != read, playback != heard, missed != done.
 * This model does not store new reminders or create acknowledgement tokens.
 */
internal enum class V306ActivityDomain { CREATOR, PERSONAL }
internal enum class V306ActivityBucket { NEEDS_ACTION, HISTORY }
internal enum class V306ActivityFilter { NEEDS_ACTION, ALL, HISTORY }

internal data class V306ActivityItem(
    val id: String,
    val domain: V306ActivityDomain,
    val bucket: V306ActivityBucket,
    val title: String,
    val detail: String,
    val atMillis: Long,
    val targetId: String,
    val deliveryProblem: Boolean = false,
)

internal data class V306CreatorEvidence(
    val task: CreatorTask,
    val history: List<ReminderAckRecord>,
    /** Verified against ReminderOccurrenceStore using the exact current CreatorTask. */
    val latestTokenCurrent: Boolean,
)

/**
 * Builds at most 80 recent history items plus all currently actionable items.
 * No event is synthesized from a scheduled reminder or from a task's status.
 */
internal object V306ActivityCenterPolicy {
    private const val MAX_HISTORY = 80

    fun build(
        creators: List<V306CreatorEvidence>,
        personal: List<PersonalReminder>,
        confirmedDone: List<PersonalCompletionEvent>,
    ): List<V306ActivityItem> {
        val actions = ArrayList<V306ActivityItem>()
        val history = ArrayList<V306ActivityItem>()

        creators.distinctBy { it.task.id }.forEach { snapshot ->
            val task = snapshot.task
            val records = snapshot.history.filter { it.taskId == task.id }
                .distinctBy { it.occurrenceId }
            val current = records.lastOrNull()
            val isPending = V181ReminderInboxPolicy.actionable(
                task, current, snapshot.latestTokenCurrent,
            )
            val isFailure = V181ReminderInboxPolicy.failedDelivery(
                task, current, snapshot.latestTokenCurrent,
            )
            if (current != null && (isPending || isFailure)) {
                actions.add(
                    V306ActivityItem(
                        id = "creator:action:${task.id}:${current.occurrenceId}",
                        domain = V306ActivityDomain.CREATOR,
                        bucket = V306ActivityBucket.NEEDS_ACTION,
                        title = task.title,
                        detail = if (isFailure) "Reminder delivery needs attention"
                            else "Reminder awaiting your response; delivery is not a read receipt",
                        atMillis = current.changedAtMillis,
                        targetId = task.id,
                        deliveryProblem = isFailure,
                    )
                )
            }
            records.forEach { record ->
                // An actionable occurrence appears once in All, not again in History.
                if ((isPending || isFailure) && record.occurrenceId == current?.occurrenceId) {
                    return@forEach
                }
                if (record.state !in setOf(
                        ReminderAckState.RESPONDED,
                        ReminderAckState.UNANSWERED,
                        ReminderAckState.DELIVERY_FAILED,
                    )) return@forEach
                history.add(
                    V306ActivityItem(
                        id = "creator:history:${task.id}:${record.occurrenceId}",
                        domain = V306ActivityDomain.CREATOR,
                        bucket = V306ActivityBucket.HISTORY,
                        title = task.title,
                        detail = V181ReminderInboxPolicy.recentLabel(record),
                        atMillis = record.changedAtMillis,
                        targetId = task.id,
                        deliveryProblem = record.state == ReminderAckState.DELIVERY_FAILED,
                    )
                )
            }
        }

        val personalById = personal.associateBy { it.id }
        personal.forEach { reminder ->
            if (V288PersonalTodayPolicy.canRespond(reminder)) {
                actions.add(
                    V306ActivityItem(
                        id = "personal:action:${reminder.id}:${reminder.pendingOccurrenceMillis}",
                        domain = V306ActivityDomain.PERSONAL,
                        bucket = V306ActivityBucket.NEEDS_ACTION,
                        title = reminder.title,
                        detail = "Personal reminder presented; no Done response is recorded",
                        atMillis = reminder.presentedAtMillis,
                        targetId = reminder.id,
                    )
                )
            }
        }
        confirmedDone.distinctBy { it.stableKey }.forEach { event ->
            val reminder = personalById[event.reminderId] ?: return@forEach
            if (event.occurrenceMillis <= 0L || event.doneAtMillis <= 0L) return@forEach
            history.add(
                V306ActivityItem(
                    id = "personal:done:${event.stableKey}",
                    domain = V306ActivityDomain.PERSONAL,
                    bucket = V306ActivityBucket.HISTORY,
                    title = reminder.title,
                    detail = "Marked Done by you",
                    atMillis = event.doneAtMillis,
                    targetId = reminder.id,
                )
            )
        }

        val activeSorted = actions.distinctBy { it.id }
            .sortedWith(compareByDescending<V306ActivityItem> { it.deliveryProblem }
                .thenByDescending { it.atMillis }.thenBy { it.id })
        val historySorted = history.distinctBy { it.id }
            .sortedWith(compareByDescending<V306ActivityItem> { it.atMillis }
                .thenBy { it.id })
            .take(MAX_HISTORY)
        return activeSorted + historySorted
    }

    fun filter(items: List<V306ActivityItem>, filter: V306ActivityFilter): List<V306ActivityItem> =
        when (filter) {
            V306ActivityFilter.NEEDS_ACTION -> items.filter { it.bucket == V306ActivityBucket.NEEDS_ACTION }
            V306ActivityFilter.ALL -> items
            V306ActivityFilter.HISTORY -> items.filter { it.bucket == V306ActivityBucket.HISTORY }
        }
}
