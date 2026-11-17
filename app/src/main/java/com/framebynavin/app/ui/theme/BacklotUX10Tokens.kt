package com.framebynavin.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * UX10A — existing Director's Cut / Light visual identity, one consistent
 * density and typography language for new or progressively migrated screens.
 *
 * These are PRESENTATION tokens: no user preference migration, persistent
 * values, theme IDs, notification semantics or Android permission changes.
 *
 * Older feature-specific UI is migrated by owner under separate scoped PRs,
 * not through a risky global search-and-replace of all BorderStroke calls.
 */
object BacklotUX10Tokens {
    // True minimum touch target for interactive shortcuts and controls.
    val minimumTouchTarget: Dp = 48.dp
    val rowTarget: Dp = 56.dp
    val pageGutter: Dp = 18.dp
    val compactPageGutter: Dp = 16.dp
    val compactSectionGap: Dp = 16.dp
    val rowGap: Dp = 6.dp

    val surfaceRadius: Dp = 14.dp
    val cardRadius: Dp = 16.dp
    val compactRadius: Dp = 12.dp

    // Labels below 10sp were inconsistently legible on tablets, large
    // text settings and small handsets. Compose font scaling remains enabled.
    val eyebrow: TextUnit = 10.sp
    val supporting: TextUnit = 11.sp
    val body: TextUnit = 13.sp
    val sectionTitle: TextUnit = 14.sp
    val pageTitle: TextUnit = 23.sp

    val eyebrowWeight: FontWeight = FontWeight.Bold
    val sectionWeight: FontWeight = FontWeight.SemiBold

    /**
     * Low-contrast outlines only for surfaces that require separation.
     * Original alarm/error/selected controls should keep a higher-contrast
     * affordance rather than being quietly stripped of their boundaries.
     */
    fun quietBorder(
        color: Color = BacklotBorder,
        alpha: Float = 0.32f,
    ): BorderStroke = BorderStroke(0.75.dp, color.copy(alpha = alpha.coerceIn(0f, 1f)))

    fun activeBorder(
        color: Color = BacklotSecondaryAccent,
    ): BorderStroke = BorderStroke(1.dp, color.copy(alpha = 0.65f))
}
