package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.reminders.ReminderAckState
import com.framebynavin.app.reminders.ReminderAcknowledgementStore

/**
 * Read-only boundary between persistent reminder evidence and V221 decisions.
 * The authoritative occurrence matcher in awaitingResponse rejects stale
 * generations/reschedules. A posted notification or running voice/alarm is
 * not evidence of a creator's response or of its absence.
 */
internal class V221CompanionEvidenceReader(context: Context) {
    private val acknowledgements = ReminderAcknowledgementStore(context.applicationContext)

    fun snapshot(
        tasks: List<CreatorTask>,
        nowMillis: Long,
        unorganizedIdeaCount: Int = 0,
        suppressedDecisionIds: Set<String> = emptySet(),
    ): V221CompanionContext {
        val unanswered = tasks.asSequence()
            .filter { it.id.isNotBlank() && it.reminderEnabled }
            .filter { it.archivedAtMillis <= 0L &&
                (it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING) }
            .filter { task ->
                // Fail closed on an unreadable acknowledgement journal; never
                // infer "unanswered" from a failed read or from Android delivery.
                runCatching { acknowledgements.awaitingResponse(task)?.state ==
                    ReminderAckState.UNANSWERED }.getOrDefault(false)
            }
            .map { it.id }.toSet()
        return V221CompanionContext(
            tasks = tasks,
            nowMillis = nowMillis,
            currentUnansweredReminderIds = unanswered,
            unorganizedIdeaCount = unorganizedIdeaCount.coerceAtLeast(0),
            suppressedDecisionIds = suppressedDecisionIds,
        )
    }
}
