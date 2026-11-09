package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.ui.theme.FrameTheme

/** Read persisted owner theme and launch-sound choice WITHOUT initialize(). */
internal object TrustAppearancePreferencesInventory {
    private const val SOURCE = "framebynavin_visual_experience"
    private val accepted = setOf("theme", "launch_sound")

    fun inspect(context: Context): TrustDomainEvidence? {
        val saved = context.applicationContext.getSharedPreferences(
            SOURCE, Context.MODE_PRIVATE,
        ).all
        if (saved.isEmpty()) return null
        require(saved.keys.all { it in accepted }) {
            "Unknown saved appearance choice requires backup schema review"
        }
        val canonical = saved.keys.sorted().joinToString(separator = "\u0000") { key ->
            val value: String = when (key) {
                "theme" -> {
                    val theme = saved.getValue(key) as? String
                        ?: throw IllegalStateException("Invalid saved theme type")
                    require(FrameTheme.entries.any { it.name == theme }) {
                        "Unrecognized saved visual theme"
                    }
                    theme
                }
                "launch_sound" -> {
                    val enabled = saved.getValue(key) as? Boolean
                        ?: throw IllegalStateException("Invalid saved launch sound preference")
                    enabled.toString()
                }
                else -> error("Unsupported appearance preference")
            }
            "$key=$value"
        }
        return TrustBackupInventoryPolicy.evidence(
            TrustBackupDomain.APP_APPEARANCE_PREFERENCES,
            canonical, saved.size,
        )
    }
}
