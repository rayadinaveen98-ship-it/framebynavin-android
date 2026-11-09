package com.framebynavin.app.reminders

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.framebynavin.app.R
import kotlin.math.roundToInt

/**
 * FC13: three sizes, one approved Spark identity.
 *
 * Small system/status-bar icon: keep the existing flat monochrome Backlot mark.
 * Medium notification: a bounded, optional ORIGINAL Spark WebP large icon.
 * Full-screen reminder: existing Compose CinePulseGuide and original Spark pose art.
 *
 * This is cosmetic only. It does not change channels, priority, privacy, delivery
 * tokens, read/Done receipts, Android full-screen grants or notification content.
 */
internal enum class FC13SparkNotificationMood { ALERT, SPEAK, CALM }

internal object FC13SparkNotificationIdentity {
    const val STATUS_BAR_ICON = R.drawable.backlot_v198_notification_monochrome

    /** Never replace the mandatory small icon; decoding failure retains it. */
    fun decoratePrivate(
        builder: NotificationCompat.Builder,
        context: Context,
        mood: FC13SparkNotificationMood,
    ): NotificationCompat.Builder {
        val originalResource = when (mood) {
            FC13SparkNotificationMood.ALERT -> R.drawable.guide_funny_point
            FC13SparkNotificationMood.SPEAK -> R.drawable.guide_funny_present
            FC13SparkNotificationMood.CALM -> R.drawable.guide_funny_welcome
        }
        // Constrain decoded artwork before handing it to NotificationManager:
        // original WebPs are for a full-screen character, not a large-icon payload.
        val maxEdge = (64f * context.resources.displayMetrics.density)
            .roundToInt().coerceIn(48, 192)
        val smallArt = runCatching {
            val original = BitmapFactory.decodeResource(context.resources, originalResource)
                ?: return@runCatching null
            val longest = maxOf(original.width, original.height)
            if (longest <= maxEdge) original
            else {
                val scale = maxEdge.toFloat() / longest
                val resized = android.graphics.Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).roundToInt().coerceAtLeast(1),
                    (original.height * scale).roundToInt().coerceAtLeast(1),
                    true,
                )
                if (resized !== original) original.recycle()
                resized
            }
        }.getOrNull()
        if (smallArt != null) builder.setLargeIcon(smallArt)
        return builder
    }
}
