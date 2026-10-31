package com.framebynavin.app.personal

/**
 * A full-screen alarm can start BEFORE its delivery receipt has been written.
 * Never treat that transient pre-ack state as a stale occurrence; wait briefly
 * for the exact matching lease instead. Never accept a rotated/stale token.
 */
internal enum class PersonalAlertPresentationState { WAITING, READY, INVALID }

internal object PersonalAlertPresentationPolicy {
    fun state(item: PersonalReminder?, id: String, token: String): PersonalAlertPresentationState {
        if (item == null || item.id != id || item.paused || item.pendingToken != token) {
            return PersonalAlertPresentationState.INVALID
        }
        return if (item.presentedToken == token) {
            PersonalAlertPresentationState.READY
        } else PersonalAlertPresentationState.WAITING
    }

    const val MAX_READ_ATTEMPTS = 10
    const val RETRY_DELAY_MILLIS = 150L
}
