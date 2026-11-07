package com.framebynavin.app.phase1

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * ONE color contract for all Phase 1 Option A debug-only review screens.
 *
 * Baseline: Phase1VisualReviewActivity (Cloud / Challenge / Alarm / Account).
 * The Reminder Hubs must reference this contract rather than duplicate hex values.
 * Applies to preview screens only; shipping UI is not modified by this file.
 */
internal object Phase1OptionAReviewTokens {
    val Ink = Color(0xFF0C1016)
    val Panel = Color(0xFF191E26)
    val Raised = Color(0xFF222833)
    val Border = Color(0xFF353D4B)

    // Locked foreground hierarchy (all UI text roles map to these).
    val Ivory = Color(0xFFF8F3EB) // headings, primary content, selected chip text
    val Muted = Color(0xFFA5ADBA) // supporting copy, metadata, neutral statuses
    val Gold = Color(0xFFF3B060) // eyebrow, link, selected accents only
    val Peach = Color(0xFFF3B374)
    val Coral = Color(0xFFE68A7C)

    val ActiveChip = Color(0xFF2D231E)
    val WarmPanel = Brush.horizontalGradient(
        listOf(Color(0xFF1C1F25), Color(0xFF28211E))
    )
    val WarmAction = Brush.horizontalGradient(listOf(Peach, Coral))
}
