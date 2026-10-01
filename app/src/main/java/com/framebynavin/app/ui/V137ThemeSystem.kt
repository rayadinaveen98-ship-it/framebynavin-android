package com.framebynavin.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.framebynavin.app.ui.theme.BacklotSelectableAppearances
import com.framebynavin.app.ui.theme.FrameTheme
import com.framebynavin.app.ui.theme.VisualExperiencePrefs

/**
 * V148 keeps one Backlot visual language and translates it between dark and light appearances.
 * Layout, rhythm and cinematic production marks stay identical; only contrast/intensity changes.
 */
@Composable
internal fun V137ThemeAtmosphere(modifier: Modifier = Modifier) {
    val theme = VisualExperiencePrefs.currentTheme
    val palette = theme.palette
    val alpha = theme.profile.atmosphereAlpha
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val light = theme == FrameTheme.BACKLOT_LIGHT
        val primaryRailAlpha = if (light) .035f else .055f
        val primaryMarkAlpha = if (light) .12f else .16f
        val secondaryMarkAlpha = if (light) .10f else .12f
        val secondaryTickAlpha = if (light) .065f else .08f
        val diagonalAlpha = if (light) .045f else .07f

        // Director's Cut production language: REC-red rail, restrained gold marks and one edit line.
        drawRect(
            palette.primary.copy(alpha = primaryRailAlpha * alpha),
            Offset(w * .045f, 0f),
            Size(w * .010f, h),
        )
        drawLine(
            palette.primary.copy(alpha = primaryMarkAlpha * alpha),
            Offset(w * .045f, h * .11f),
            Offset(w * .045f, h * .34f),
            2.4f,
        )
        drawLine(
            palette.secondary.copy(alpha = secondaryMarkAlpha * alpha),
            Offset(w * .76f, h * .09f),
            Offset(w * .94f, h * .09f),
            1.4f,
        )
        drawLine(
            palette.secondary.copy(alpha = secondaryTickAlpha * alpha),
            Offset(w * .86f, h * .09f),
            Offset(w * .86f, h * .15f),
            1.2f,
        )
        drawLine(
            palette.primary.copy(alpha = diagonalAlpha * alpha),
            Offset(w * .10f, h * .91f),
            Offset(w * .90f, h * .78f),
            1.1f,
        )
    }
}

// Keep the historical symbol name so existing call sites/tests do not need a noisy rename.
internal val V137ThemeOrder = BacklotSelectableAppearances
