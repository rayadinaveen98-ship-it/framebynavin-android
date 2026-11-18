package com.framebynavin.app.ui

/**
 * UX10F shared read-only layout continuity rule for source-owned analytics
 * metric cards. It never changes a metric value, report evidence, save action,
 * event receipt, account or theme preference.
 */
internal object UX10FContinuityPolicy {
    const val COMPACT_METRIC_WIDTH_DP: Int = 400
    const val LARGE_TEXT_SCALE: Float = 1.35f
    const val SINGLE_CARD_COLUMNS: Int = 1
    const val PAIRED_CARD_COLUMNS: Int = 2

    fun stackMetricCards(screenWidthDp: Int, fontScale: Float): Boolean =
        screenWidthDp < COMPACT_METRIC_WIDTH_DP ||
            !fontScale.isFinite() || fontScale <= 0f ||
            fontScale >= LARGE_TEXT_SCALE

    fun metricColumns(screenWidthDp: Int, fontScale: Float): Int =
        if (stackMetricCards(screenWidthDp, fontScale)) SINGLE_CARD_COLUMNS
        else PAIRED_CARD_COLUMNS
}
