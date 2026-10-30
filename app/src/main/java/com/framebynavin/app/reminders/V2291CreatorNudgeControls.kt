package com.framebynavin.app.reminders

import android.content.Context

/**
 * Reuses V205's existing on-device pause state. Applies exclusively to
 * optional context nudges, never to Creator or Personal scheduled reminders.
 */
internal enum class V2291NudgeControlState { OFF, PAUSED, BLOCKED, ACTIVE }

internal object V2291NudgeControlPolicy {
    fun status(
        optedIn: Boolean,
        notificationsAvailable: Boolean,
        pausedUntilMillis: Long,
        nowMillis: Long,
    ): V2291NudgeControlState = when {
        !optedIn -> V2291NudgeControlState.OFF
        V205NudgePausePolicy.isPaused(nowMillis, pausedUntilMillis) ->
            V2291NudgeControlState.PAUSED
        !notificationsAvailable -> V2291NudgeControlState.BLOCKED
        else -> V2291NudgeControlState.ACTIVE
    }
}

internal class V2291CreatorNudgeControls(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        V205NudgePausePolicy.PREFS, Context.MODE_PRIVATE,
    )

    fun pausedUntil(): Long = prefs.getLong(V205NudgePausePolicy.KEY_PAUSED_UNTIL, 0L)

    /** A failed durable write never becomes a claimed successful pause. */
    fun pauseForWeek(nowMillis: Long): Boolean {
        val until = V205NudgePausePolicy.pauseUntil(nowMillis)
        if (until <= 0L) return false
        return prefs.edit()
            .putLong(V205NudgePausePolicy.KEY_PAUSED_UNTIL, until)
            .commit()
    }

    /** Resuming cannot force a nudge or bypass its original 24h/72h history. */
    fun resume(): Boolean = prefs.edit()
        .remove(V205NudgePausePolicy.KEY_PAUSED_UNTIL)
        .commit()
}
