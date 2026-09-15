package com.framebynavin.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.framebynavin.app.ui.theme.FrameTheme
import com.framebynavin.app.ui.theme.VisualExperiencePrefs

/**
 * Flat 2D atmosphere for Backlot's eight v138 visual worlds.
 * Intentionally restrained: no 3D, bloom, neon haze or animated glow.
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
                drawLine(palette.secondary.copy(alpha = .075f), Offset(0f, h * .16f), Offset(w, h * .06f), 1.2f)
                drawLine(palette.primary.copy(alpha = .055f), Offset(w * .08f, h), Offset(w * .92f, h * .13f), 1f)
            }
            FrameTheme.AURORA_GLASS -> {
                val path = Path().apply {
                    moveTo(w * .02f, h * .78f)
                    cubicTo(w * .28f, h * .62f, w * .60f, h * .88f, w * 1.04f, h * .68f)
                }
                drawPath(path, palette.primary.copy(alpha = .085f), style = Stroke(1.6f, cap = StrokeCap.Round))
                drawCircle(palette.secondary.copy(alpha = .06f), w * .13f, Offset(w * .88f, h * .14f))
            }
            FrameTheme.LUMEN_FLOW -> {
                repeat(4) { i ->
                    val y = h * (.20f + i * .18f)
                    drawLine(palette.foreground.copy(alpha = .035f), Offset(w * .05f, y), Offset(w * .95f, y), 1f)
                }
                drawLine(palette.tertiary.copy(alpha = .16f), Offset(w * .08f, h * .10f), Offset(w * .33f, h * .10f), 2f)
            }
            FrameTheme.PAPER_QUIET -> {
                repeat(5) { i ->
                    val y = h * (.18f + i * .14f)
                    drawLine(palette.foreground.copy(alpha = .045f), Offset(w * .08f, y), Offset(w * .92f, y), 1f)
                }
                val corner = Path().apply {
                    moveTo(w * .76f, h * .10f)
                    cubicTo(w * .84f, h * .06f, w * .89f, h * .14f, w * .94f, h * .09f)
                }
                drawPath(corner, palette.primary.copy(alpha = .18f), style = Stroke(2f, cap = StrokeCap.Round))
            }
            FrameTheme.MOSS_STUDIO -> {
                val stem = Path().apply {
                    moveTo(w * .86f, h * .10f)
                    cubicTo(w * .78f, h * .28f, w * .84f, h * .42f, w * .75f, h * .60f)
                }
                drawPath(stem, palette.primary.copy(alpha = .12f), style = Stroke(1.7f, cap = StrokeCap.Round))
                drawOval(palette.primary.copy(alpha = .065f), topLeft = Offset(w * .70f, h * .25f), size = androidx.compose.ui.geometry.Size(w * .10f, h * .055f))
                drawOval(palette.secondary.copy(alpha = .055f), topLeft = Offset(w * .79f, h * .39f), size = androidx.compose.ui.geometry.Size(w * .10f, h * .05f))
            }
            FrameTheme.MIDNIGHT -> {
                repeat(3) { i ->
                    val x = w * (.12f + i * .30f)
                    drawLine(palette.foreground.copy(alpha = .035f), Offset(x, h * .08f), Offset(x + w * .24f, h * .92f), 1f)
                }
                drawCircle(palette.secondary.copy(alpha = .08f), 3f, Offset(w * .88f, h * .18f))
            }
            FrameTheme.EMBER -> {
                drawCircle(palette.primary.copy(alpha = .11f), w * .24f, Offset(w * .88f, h * .17f))
                drawCircle(palette.secondary.copy(alpha = .065f), w * .18f, Offset(w * .96f, h * .20f))
                val arc = Path().apply {
                    moveTo(-w * .06f, h * .78f)
                    cubicTo(w * .22f, h * .62f, w * .48f, h * .94f, w * .78f, h * .75f)
                }
                drawPath(arc, palette.primary.copy(alpha = .09f), style = Stroke(2f, cap = StrokeCap.Round))
            }
            FrameTheme.VIOLET_NEON -> {
                val stem = Path().apply {
                    moveTo(w * .90f, h * .10f)
                    cubicTo(w * .84f, h * .26f, w * .88f, h * .45f, w * .77f, h * .63f)
                }
                drawPath(stem, palette.tertiary.copy(alpha = .15f), style = Stroke(1.6f, cap = StrokeCap.Round))
                drawOval(palette.primary.copy(alpha = .09f), Offset(w * .73f, h * .21f), androidx.compose.ui.geometry.Size(w * .13f, h * .06f))
                drawOval(palette.secondary.copy(alpha = .10f), Offset(w * .80f, h * .38f), androidx.compose.ui.geometry.Size(w * .12f, h * .055f))
            }
        }
    }
}

internal val V137ThemeOrder = listOf(
    FrameTheme.DIRECTORS_CUT,
    FrameTheme.AURORA_GLASS,
    FrameTheme.LUMEN_FLOW,
    FrameTheme.PAPER_QUIET,
    FrameTheme.MOSS_STUDIO,
    FrameTheme.MIDNIGHT,
    FrameTheme.EMBER,
    FrameTheme.VIOLET_NEON,
)
