package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/**
 * Explicit user-tapped action from an existing opt-in context notification.
 * This is not exported and cannot be triggered by other applications.
 */
class V205NudgePauseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != V205NudgePausePolicy.ACTION_PAUSE_WEEK) return
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val until = V205NudgePausePolicy.pauseUntil(now)
        if (until <= 0L) return
        val saved = app.getSharedPreferences(V205NudgePausePolicy.PREFS, Context.MODE_PRIVATE)
            .edit().putLong(V205NudgePausePolicy.KEY_PAUSED_UNTIL, until).commit()
        if (saved) {
            NotificationManagerCompat.from(app).cancel(V205NudgePausePolicy.NOTIFICATION_ID)
        }
    }
}
