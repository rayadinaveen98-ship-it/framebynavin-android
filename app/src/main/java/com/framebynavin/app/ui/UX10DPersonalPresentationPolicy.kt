package com.framebynavin.app.ui

/**
 * UX10D — viewport-only Personal design decisions.
 *
 * Never stores a Space record, advances a Challenge, acknowledges or snoozes a
 * reminder, grants a permission, handles playback or changes the alarm owner.
 */
internal object UX10DPersonalPresentationPolicy {
    private const val SPACE_METRIC_BREAKPOINT_DP = 400
    private const val COMPACT_ACTION_BREAKPOINT_DP = 380
    private const val ACCESSIBLE_TEXT_SCALE = 1.35f

    fun stackSpaceMetrics(screenWidthDp: Int, fontScale: Float): Boolean =
        screenWidthDp < SPACE_METRIC_BREAKPOINT_DP ||
            !fontScale.isFinite() || fontScale <= 0f ||
            fontScale >= ACCESSIBLE_TEXT_SCALE

    fun stackReminderResponses(screenWidthDp: Int, fontScale: Float): Boolean =
        screenWidthDp < COMPACT_ACTION_BREAKPOINT_DP ||
            !fontScale.isFinite() || fontScale <= 0f ||
            fontScale >= ACCESSIBLE_TEXT_SCALE

    fun stackChallengeDayNavigation(screenWidthDp: Int, fontScale: Float): Boolean =
        stackReminderResponses(screenWidthDp, fontScale)

    /**
     * A seven-column grid needs roughly 48dp per date and can exceed the
     * available space once the Personal screen/panel gutters are considered.
     * Reflow to four columns; the canonical 21 dates and callbacks are unchanged.
     */
    fun challengeDayGridColumns(screenWidthDp: Int, fontScale: Float): Int =
        if (screenWidthDp < 480 || !fontScale.isFinite() ||
            fontScale <= 0f || fontScale >= ACCESSIBLE_TEXT_SCALE
        ) 4 else 7

    /** A purpose choice may expand but is never silently truncated or saved. */
    fun purposeCardWidthDp(fontScale: Float): Int =
        if (!fontScale.isFinite() || fontScale <= 0f || fontScale >= ACCESSIBLE_TEXT_SCALE)
            220 else 148

    const val SPACE_METRIC_COUNT = 3
    const val CHALLENGE_DAY_COUNT = 21
}
