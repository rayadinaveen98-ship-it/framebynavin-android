package com.framebynavin.app.ui

/**
 * UX10B — viewport-only presentation policy.
 *
 * Unlike a data/profile policy, these decisions must never advance onboarding,
 * save preferences, request permissions, alter project stages or route elsewhere.
 */
internal object UX10BPresentationPolicy {
    const val NARROW_PHONE_DP = 360
    const val LARGE_TEXT_SCALE = 1.35f

    fun stackedFirstRunChoices(screenWidthDp: Int, fontScale: Float): Boolean =
        screenWidthDp < NARROW_PHONE_DP || fontScale >= LARGE_TEXT_SCALE

    fun stackedHomeActions(screenWidthDp: Int, fontScale: Float): Boolean =
        screenWidthDp < NARROW_PHONE_DP || fontScale >= LARGE_TEXT_SCALE

    /**
     * Use readable >=10sp labels when they fit. An icon-only compact tab
     * retains the full original accessibility label and click destination,
     * never shrinking visible words to 6.5sp.
     */
    fun showBottomNavigationText(
        itemWidthDp: Float,
        fontScale: Float,
        label: String,
    ): Boolean {
        if (!itemWidthDp.isFinite() || !fontScale.isFinite() ||
            itemWidthDp <= 0f || fontScale <= 0f
        ) return false
        val minWidth = when (label) {
            "Projects", "Insights" -> 58f
            else -> 48f
        }
        return itemWidthDp >= minWidth * fontScale.coerceAtLeast(1f)
    }
}
