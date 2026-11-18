package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorRewardEventType
import com.framebynavin.app.data.CreatorRewardLedgerEntry

/**
 * UX08C presentation gate for already-newly-credited Creator feedback.
 *
 * The caller MUST supply the live CreatorViewModel.rewardFeedbackQueue item,
 * which is enqueued only after the original reward store's successful new
 * commit / owner reconciliation. A saved ledger list or imported history is
 * never valid input for an on-screen celebration.
 *
 * This gate only validates event shape and near-action freshness. It does not
 * grant XP, persist receipts, trigger a reminder, or replay historical rewards.
 */
internal object UX08CCelebrationPolicy {
    const val VISIBLE_HOLD_MS = 2_650L
    const val EXIT_MS = 350L
    const val FEEDBACK_QUEUE_SLOT_MS = 3_300L
    const val MAX_RECENT_KEYS = 32
    const val MAX_EVENT_AGE_MS = 10 * 60_000L

    fun verifiedFeedback(entry: CreatorRewardLedgerEntry?, nowMillis: Long): Boolean {
        if (entry == null || entry.eventKey.isBlank() ||
            entry.subjectId.isBlank() || entry.xp <= 0 ||
            entry.occurredAtMillis <= 0L || nowMillis < entry.occurredAtMillis ||
            nowMillis - entry.occurredAtMillis > MAX_EVENT_AGE_MS
        ) return false

        return when (entry.type) {
            CreatorRewardEventType.IDEA_DAILY_CAPTURE ->
                Regex("idea-day:[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(entry.eventKey)
            CreatorRewardEventType.IDEA_CONVERTED ->
                entry.projectId.isNotBlank() &&
                    entry.eventKey == "idea-converted:${entry.subjectId}"
            CreatorRewardEventType.PROJECT_STAGE_COMPLETED ->
                entry.projectId.isNotBlank() &&
                    entry.eventKey == "stage:${entry.projectId}:${entry.subjectId}"
            CreatorRewardEventType.PROJECT_PUBLISHED ->
                entry.projectId.isNotBlank() && entry.projectId == entry.subjectId &&
                    entry.eventKey == "published:${entry.projectId}"
            CreatorRewardEventType.POST_PUBLISH_COMPLETED ->
                entry.projectId.isNotBlank() &&
                    entry.eventKey == "post-publish:${entry.subjectId}"
        }
    }

    /** Never trigger twice while the same app screen is reconstructed. */
    fun addSeenKey(keys: List<String>, key: String): List<String> =
        if (key.isBlank() || key in keys) keys
        else (keys + key).takeLast(MAX_RECENT_KEYS)
}
