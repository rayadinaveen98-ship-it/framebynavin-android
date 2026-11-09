package com.framebynavin.app.reminders

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import com.framebynavin.app.R
import kotlin.math.roundToInt

/**
 * One approved Spark character, five ORIGINAL local expressions, three sizes:
 *
 * Small: Android-safe WHITE silhouette of the approved POINT WebP, in XML.
 * Medium: bounded, cached original color pose only in PRIVATE notification.
 * Full: existing Compose guide art, drawn by FC13SparkPose.
 *
 * Distinguish a quiet/follow-up notification from audio that is actually
 * speaking/ringing. Nothing here creates a receipt, delivery or sound event.
 * Device-only privacy is preserved: never decorate the public lockscreen
 * notification with private large artwork.
 */
internal enum class FC13SparkNotificationMood {
    ALERT,       // Point
    SPEAK,       // Present
    CALM,        // Welcome
    THINK,       // Thinking
    SUCCESS,     // Celebrate
}

internal object FC13SparkNotificationIdentity {
    val STATUS_BAR_ICON: Int = R.drawable.backlot_spark_notification_monochrome

    @DrawableRes
    internal fun approvedArt(mood: FC13SparkNotificationMood): Int = when (mood) {
        FC13SparkNotificationMood.ALERT -> R.drawable.guide_funny_point
        FC13SparkNotificationMood.SPEAK -> R.drawable.guide_funny_present
        FC13SparkNotificationMood.CALM -> R.drawable.guide_funny_welcome
        FC13SparkNotificationMood.THINK -> R.drawable.guide_funny_thinking
        FC13SparkNotificationMood.SUCCESS -> R.drawable.guide_funny_celebrate
    }

    // Five <=192px private bitmaps, regardless of full source WebP dimensions.
    // No disk writes, WebP mutation, network access or new character packs.
    private val lock = Any()
    private val largeIcons = mutableMapOf<Pair<Int, Int>, Bitmap>()

    fun decoratePrivate(
        builder: NotificationCompat.Builder,
        context: Context,
        mood: FC13SparkNotificationMood,
    ): NotificationCompat.Builder {
        val resource = approvedArt(mood)
        val maxEdge = (64f * context.resources.displayMetrics.density)
            .roundToInt().coerceIn(48, 192)
        val key = resource to maxEdge
        val icon = synchronized(lock) {
            largeIcons[key]?.takeUnless { it.isRecycled } ?: runCatching {
                readBounded(context, resource, maxEdge)
            }.getOrNull()?.also { largeIcons[key] = it }
        }
        if (icon != null) builder.setLargeIcon(icon)
        // Missing/malformed WebP affects only optional artwork, not delivery.
        return builder
    }

    private fun readBounded(
        context: Context,
        @DrawableRes resource: Int,
        maxEdge: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resource, bounds)
        val sourceEdge = maxOf(bounds.outWidth, bounds.outHeight)
        if (sourceEdge <= 0) return null
        var sample = 1
        while (sourceEdge / sample > maxEdge * 2 && sample < 128) {
            sample *= 2
        }
        val decoded = BitmapFactory.decodeResource(
            context.resources,
            resource,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ) ?: return null
        if (maxOf(decoded.width, decoded.height) <= maxEdge) return decoded
        val ratio = maxEdge.toFloat() / maxOf(decoded.width, decoded.height)
        val resized = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * ratio).roundToInt().coerceAtLeast(1),
            (decoded.height * ratio).roundToInt().coerceAtLeast(1),
            true,
        )
        if (resized !== decoded) decoded.recycle()
        return resized
    }
}
