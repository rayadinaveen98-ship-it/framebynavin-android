package com.framebynavin.app.ui

/**
 * Viewport-to-navigation clearance in dp. Uses the measured top of Backlot's floating
 * bottom bar, rather than assuming the bar/system-navigation height is always 124dp.
 *
 * The 124dp minimum preserves the legacy screen spacing before measurement and on
 * standard devices; the extra 16dp prevents a last-row tap target touching the bar.
 * Both positions must be in the same root coordinate space.
 */
internal object V197BottomNavigationClearance {
    const val MINIMUM_DP = 124f
    private const val BUFFER_DP = 16f

    fun calculate(
        rootHeightPx: Int,
        navigationTopPx: Float,
        density: Float,
    ): Float {
        if (rootHeightPx <= 0 || !navigationTopPx.isFinite() ||
            !density.isFinite() || density <= 0f ||
            navigationTopPx < 0f || navigationTopPx > rootHeightPx.toFloat()
        ) return MINIMUM_DP

        val occupiedDp = (rootHeightPx - navigationTopPx) / density
        return maxOf(MINIMUM_DP, occupiedDp + BUFFER_DP)
    }
}
