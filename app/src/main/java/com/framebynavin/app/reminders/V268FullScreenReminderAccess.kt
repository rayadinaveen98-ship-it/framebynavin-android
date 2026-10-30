package com.framebynavin.app.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Only request an interruptive full-screen surface when Android permits it.
 *
 * Creator Voice and Alarm reminders retain their high-priority, tappable
 * notification even if Android 14+ special full-screen access is denied,
 * revoked or cannot be queried. No background Activity launch is attempted
 * here. This is a platform capability check, not a Play eligibility claim.
 */
internal object V268FullScreenReminderAccess {
    fun allowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return runCatching {
            context.getSystemService(NotificationManager::class.java)
                ?.canUseFullScreenIntent() == true
        }.getOrDefault(false)
    }

    /** Pure branch used by Alarm and Voice so a denied permission is testable. */
    fun configure(
        builder: NotificationCompat.Builder,
        surface: PendingIntent,
        fullScreenAllowed: Boolean,
    ): NotificationCompat.Builder {
        return if (fullScreenAllowed) builder.setFullScreenIntent(surface, true) else builder
    }
}
