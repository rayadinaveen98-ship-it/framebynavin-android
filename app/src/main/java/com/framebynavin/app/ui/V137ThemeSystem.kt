package com.framebynavin.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.framebynavin.app.ui.theme.FrameTheme
import com.framebynavin.app.ui.theme.VisualExperiencePrefs

/**
 * Small flat 2D atmosphere layer for the six v137 themes.
 * No 3D, bloom or animated glow: only low-contrast lines/shapes that keep each theme distinct.
 */
@Composable
internal fun V137ThemeAtmosphere(modifier: Modifier = Modifier) {
    val theme = VisualExperiencePrefs.currentTheme
    val palette = theme.palette
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (theme) {
            FrameTheme.DIRECTORS_CUT -> {
                drawLine(palette.secondary.copy(alpha = .055f), Offset(0f, h * .17f), Offset(w, h * .07f), 1f)
                drawLine(palette.primary.copy(alpha = .045f), Offset(w * .10f, h), Offset(w * .92f, h * .12f), 1f)
            }
            FrameTheme.MIDNIGHT -> {
                repeat(3) { i ->
                    val y = h * (.22f + i * .21f)
                    drawLine(palette.primary.copy(alpha = .045f - i * .007f), Offset(w * .05f, y), Offset(w * .95f, y + h * .035f), 1.2f)
                }
            }
            FrameTheme.EMBER -> {
                val path = Path().apply {
                    moveTo(-w * .08f, h * .80f)
                    cubicTo(w * .22f, h * .57f, w * .56f, h * .98f, w * 1.08f, h * .66f)
                }
                drawPath(path, palette.primary.copy(alpha = .065f), style = Stroke(width = 2.2f, cap = StrokeCap.Round))
                drawCircle(palette.secondary.copy(alpha = .045f), w * .18f, Offset(w * .84f, h * .18f))
            }
            FrameTheme.VIOLET_NEON -> {
                drawCircle(palette.primary.copy(alpha = .045f), w * .25f, Offset(w * .12f, h * .22f))
                drawCircle(palette.secondary.copy(alpha = .035f), w * .18f, Offset(w * .92f, h * .70f))
                drawLine(palette.tertiary.copy(alpha = .045f), Offset(w * .08f, h * .58f), Offset(w * .76f, h * .30f), 1.4f)
            }
            FrameTheme.LUMEN_FLOW -> {
                repeat(4) { i ->
                    val y = h * (.18f + i * .20f)
                    drawLine(palette.foreground.copy(alpha = .027f), Offset(0f, y), Offset(w, y), 1f)
                }
                repeat(3) { i ->
                    val x = w * (.22f + i * .28f)
                    drawLine(palette.foreground.copy(alpha = .018f), Offset(x, 0f), Offset(x, h), 1f)
                }
            }
            FrameTheme.AURORA_GLASS -> {
                drawCircle(palette.secondary.copy(alpha = .055f), w * .14f, Offset(w * .87f, h * .12f))
                val path = Path().apply {
                    moveTo(w * .08f, h * .82f)
                    cubicTo(w * .30f, h * .72f, w * .54f, h * .90f, w * .94f, h * .75f)
                }
                drawPath(path, palette.primary.copy(alpha = .050f), style = Stroke(width = 1.5f, cap = StrokeCap.Round))
            }
        }
    }
}

internal val V137ThemeOrder = listOf(
    FrameTheme.DIRECTORS_CUT,
    FrameTheme.AURORA_GLASS,
    FrameTheme.MIDNIGHT,
    FrameTheme.EMBER,
    FrameTheme.VIOLET_NEON,
    FrameTheme.LUMEN_FLOW,
)
