package com.framebynavin.app.personal

/**
 * Best-effort notification repair after a one-shot optional follow-up races
 * with an acknowledgement, a pause, an edit, or a newer posted lease.
 * This policy does not mutate reminder state or schedule any alarm.
 */
internal enum class PersonalFollowUpHandoffRepair {
    CANCEL_STALE_CARD,
    RESTORE_LATEST_POSTED_CARD,
}

internal object PersonalFollowUpHandoffPolicy {
    fun repair(current: PersonalReminder?): PersonalFollowUpHandoffRepair =
        if (current != null && !current.paused &&
            current.pendingToken.isNotBlank() &&
            current.presentedToken == current.pendingToken
        ) PersonalFollowUpHandoffRepair.RESTORE_LATEST_POSTED_CARD
        else PersonalFollowUpHandoffRepair.CANCEL_STALE_CARD
}
