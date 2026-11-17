package com.framebynavin.app.ui

/**
 * Creator UI-only layout policy. It does not inspect or mutate the project,
 * saved draft revision, stage advancement, output or idea records.
 */
internal object UX10CWorkspacePresentationPolicy {
    const val NARROW_DEVICE_DP = 380
    const val LARGE_FONT_SCALE = 1.3f

    fun stackWorkspaceTools(
        screenWidthDp: Int,
        fontScale: Float,
    ): Boolean = screenWidthDp < NARROW_DEVICE_DP ||
        !fontScale.isFinite() || fontScale >= LARGE_FONT_SCALE

    /**
     * Presentation-only: four original tools are always present; reflow
     * changes how many cards share a row, never their route or order.
     */
    fun columnsPerRow(screenWidthDp: Int, fontScale: Float): Int =
        if (stackWorkspaceTools(screenWidthDp, fontScale)) 1 else 2

    fun pageSizeForToolCards(): Int = 4
}
