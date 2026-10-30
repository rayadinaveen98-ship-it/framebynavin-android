package com.framebynavin.app.reminders

/** Only optional context nudges are paused. Real reminders and alarms are unaffected. */
internal object V205NudgePausePolicy {
    const val PREFS = "creator_context_nudge_state"
    const val KEY_PAUSED_UNTIL = "nudge_paused_until_v205"
    const val ACTION_PAUSE_WEEK = "com.framebynavin.app.reminders.PAUSE_CONTEXT_NUDGES_WEEK"
    const val PAUSE_MS = 7L * 24L * 60L * 60L * 1000L
    const val NOTIFICATION_ID = 8401

    fun pauseUntil(nowMillis: Long): Long {
        if (nowMillis <= 0L) return 0L
        return if (nowMillis > Long.MAX_VALUE - PAUSE_MS) Long.MAX_VALUE else nowMillis + PAUSE_MS
    }

    fun isPaused(nowMillis: Long, pausedUntilMillis: Long): Boolean =
        nowMillis > 0L && pausedUntilMillis > nowMillis
}
