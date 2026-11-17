package com.framebynavin.app.ui

/**
 * UX10E presentation-only decisions. No settings preference, YouTube auth,
 * Activity Center receipt, widget pin or notification is read or mutated here.
 */
internal object UX10EPresentationPolicy {
    const val INSIGHTS_COMPACT_WIDTH_DP = 430
    const val ACCESSIBILITY_FONT_SCALE = 1.35f
    const val INSIGHTS_MIN_TAB_WIDTH_DP = 108
    const val INSIGHTS_TAB_COUNT = 4

    fun scrollInsightsTabs(widthDp: Int, fontScale: Float): Boolean =
        widthDp < INSIGHTS_COMPACT_WIDTH_DP ||
            !fontScale.isFinite() || fontScale <= 0f ||
            fontScale >= ACCESSIBILITY_FONT_SCALE

    /** A genuine empty state requires all read sources and no read failures. */
    fun activityEmptyCopy(
        allSourcesReady: Boolean,
        readFailure: Boolean,
        viewName: String,
    ): String = when {
        readFailure -> "Some activity is unavailable. No saved records were changed."
        !allSourcesReady -> "Loading recorded reminder activity…"
        viewName == "NEEDS_ACTION" -> "Nothing currently needs a response."
        viewName == "HISTORY" -> "No confirmed activity history yet."
        else -> "No recorded reminder activity yet."
    }

    /** Preview presentation only. RemoteViews is never regenerated or pinned. */
    fun widgetPreviewHeightDp(fontScale: Float): Int =
        if (!fontScale.isFinite() || fontScale <= 0f ||
            fontScale >= ACCESSIBILITY_FONT_SCALE
        ) 208 else 174
}
