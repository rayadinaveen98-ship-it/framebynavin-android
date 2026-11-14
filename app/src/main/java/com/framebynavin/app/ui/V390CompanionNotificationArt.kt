package com.framebynavin.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.framebynavin.app.R

/**
 * Companion artwork for PRIVATE expanded Android notifications.
 *
 * A foreground receiver can run before MainActivity initializes Compose state,
 * so always read the historical persisted guide key directly. The status-bar
 * small icon remains Android's required monochrome icon.
 *
 * This is decorative only: never changes notification permissions, timings,
 * Snooze, Done, or activity routing. Fail safely if resources are unavailable.
 */
internal object V390CompanionNotificationArt {
    fun selectedIcon(context: Context): Bitmap? = runCatching {
        val saved = context.applicationContext.getSharedPreferences(
            "backlot_character_identity_v151", Context.MODE_PRIVATE,
        ).getString("selected_character", null)
        val drawable = if (saved == V144GuideIdentity.MIRA.name) {
            R.drawable.guide_mira_welcome
        } else {
            R.drawable.guide_funny_welcome
        }
        BitmapFactory.decodeResource(context.resources, drawable)
    }.getOrNull()
}
