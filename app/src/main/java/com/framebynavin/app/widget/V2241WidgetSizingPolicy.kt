package com.framebynavin.app.widget

/**
 * Android launcher size hints are in dp and vary by launcher/OS. Use compact
 * mode before fixed action rows collide with typography or large font settings.
 * Unknown size hints use the normal layout, matching preview defaults.
 */
internal object V2241WidgetSizingPolicy {
    fun compact(minWidthDp: Int, minHeightDp: Int, fontScale: Float = 1f): Boolean =
        (minWidthDp in 1..224) || (minHeightDp in 1..174) || fontScale >= 1.3f
}
