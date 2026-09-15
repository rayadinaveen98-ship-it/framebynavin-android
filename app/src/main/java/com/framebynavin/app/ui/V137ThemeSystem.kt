package com.framebynavin.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.framebynavin.app.ui.theme.FrameTheme
import com.framebynavin.app.ui.theme.VisualExperiencePrefs

/**
 * App-wide flat 2D atmosphere for v139. Each theme gets its own composition language,
 * not merely a different palette. No fake 3D, bloom or decorative glow.
 */
@Composable
internal fun V137ThemeAtmosphere(modifier: Modifier = Modifier) {
    val theme = VisualExperiencePrefs.currentTheme
    val palette = theme.palette
    val alpha = theme.profile.atmosphereAlpha
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (theme) {
            FrameTheme.DIRECTORS_CUT -> {
                // Classic Backlot: hard REC-red cuts with restrained production-gold marks.
                drawRect(palette.primary.copy(alpha = .055f * alpha), Offset(w * .045f, 0f), Size(w * .010f, h))
                drawLine(palette.primary.copy(alpha = .16f * alpha), Offset(w * .045f, h * .11f), Offset(w * .045f, h * .34f), 2.4f)
                drawLine(palette.secondary.copy(alpha = .12f * alpha), Offset(w * .76f, h * .09f), Offset(w * .94f, h * .09f), 1.4f)
                drawLine(palette.secondary.copy(alpha = .08f * alpha), Offset(w * .86f, h * .09f), Offset(w * .86f, h * .15f), 1.2f)
                drawLine(palette.primary.copy(alpha = .07f * alpha), Offset(w * .10f, h * .91f), Offset(w * .90f, h * .78f), 1.1f)
            }
            FrameTheme.STUDIO_GOLD -> {
                // Luxury editorial: open frame corners and precise hairlines.
                val c = palette.primary.copy(alpha = .14f * alpha)
                drawLine(c, Offset(w * .07f, h * .08f), Offset(w * .30f, h * .08f), 1.5f)
                drawLine(c, Offset(w * .07f, h * .08f), Offset(w * .07f, h * .19f), 1.5f)
                drawLine(c, Offset(w * .70f, h * .88f), Offset(w * .93f, h * .88f), 1.5f)
                drawLine(c, Offset(w * .93f, h * .77f), Offset(w * .93f, h * .88f), 1.5f)
                repeat(3) { i ->
                    val y = h * (.30f + i * .12f)
                    drawLine(palette.secondary.copy(alpha = .045f * alpha), Offset(w * .18f, y), Offset(w * .82f, y), 1f)
                }
            }
            FrameTheme.MIDNIGHT -> {
                // Mono Ink: strict typographic grid and registration marks.
                repeat(4) { i ->
                    val x = w * (.12f + i * .25f)
                    drawLine(palette.foreground.copy(alpha = .030f * alpha), Offset(x, h * .06f), Offset(x, h * .94f), 1f)
                }
                repeat(3) { i ->
                    val y = h * (.22f + i * .25f)
                    drawLine(palette.foreground.copy(alpha = .028f * alpha), Offset(w * .05f, y), Offset(w * .95f, y), 1f)
                }
                drawLine(palette.foreground.copy(alpha = .12f * alpha), Offset(w * .08f, h * .11f), Offset(w * .16f, h * .11f), 1.3f)
                drawLine(palette.foreground.copy(alpha = .12f * alpha), Offset(w * .12f, h * .07f), Offset(w * .12f, h * .15f), 1.3f)
            }
            FrameTheme.LUMEN_FLOW -> {
                // Ivory Atelier: gallery rails and generous negative space.
                drawLine(palette.foreground.copy(alpha = .055f * alpha), Offset(w * .08f, h * .16f), Offset(w * .92f, h * .16f), 1f)
                drawLine(palette.foreground.copy(alpha = .040f * alpha), Offset(w * .18f, h * .16f), Offset(w * .18f, h * .86f), 1f)
                drawLine(palette.secondary.copy(alpha = .18f * alpha), Offset(w * .08f, h * .16f), Offset(w * .08f, h * .32f), 2f)
                drawRect(palette.secondary.copy(alpha = .055f * alpha), Offset(w * .72f, h * .09f), Size(w * .16f, h * .06f))
            }
            FrameTheme.PAPER_QUIET -> {
                // Paper Quiet: ruled page rhythm with a clay margin rule.
                repeat(7) { i ->
                    val y = h * (.16f + i * .105f)
                    drawLine(palette.foreground.copy(alpha = .045f * alpha), Offset(w * .08f, y), Offset(w * .94f, y), 1f)
                }
                drawLine(palette.primary.copy(alpha = .22f * alpha), Offset(w * .15f, h * .09f), Offset(w * .15f, h * .91f), 1.6f)
                drawCircle(palette.secondary.copy(alpha = .16f * alpha), 2.2f, Offset(w * .10f, h * .16f))
                drawCircle(palette.secondary.copy(alpha = .12f * alpha), 2.2f, Offset(w * .10f, h * .37f))
                drawCircle(palette.secondary.copy(alpha = .12f * alpha), 2.2f, Offset(w * .10f, h * .58f))
            }
            FrameTheme.MOSS_STUDIO -> {
                // Moss Studio: calm botanical contour, deliberately sparse.
                val stem = Path().apply {
                    moveTo(w * .88f, h * .08f)
                    cubicTo(w * .78f, h * .23f, w * .90f, h * .40f, w * .73f, h * .61f)
                    cubicTo(w * .67f, h * .69f, w * .72f, h * .78f, w * .62f, h * .88f)
                }
                drawPath(stem, palette.primary.copy(alpha = .14f * alpha), style = Stroke(1.7f, cap = StrokeCap.Round))
                drawOval(palette.primary.copy(alpha = .075f * alpha), Offset(w * .70f, h * .22f), Size(w * .12f, h * .055f))
                drawOval(palette.secondary.copy(alpha = .070f * alpha), Offset(w * .79f, h * .39f), Size(w * .11f, h * .050f))
                drawOval(palette.primary.copy(alpha = .055f * alpha), Offset(w * .61f, h * .66f), Size(w * .13f, h * .052f))
            }
            FrameTheme.EMBER -> {
                // Terracotta Calm: asymmetric poster blocks and one oversized sun form.
                drawRect(palette.primary.copy(alpha = .075f * alpha), Offset(w * .04f, h * .12f), Size(w * .24f, h * .16f))
                drawRect(palette.secondary.copy(alpha = .050f * alpha), Offset(w * .64f, h * .62f), Size(w * .31f, h * .20f))
                drawCircle(palette.primary.copy(alpha = .10f * alpha), w * .19f, Offset(w * .87f, h * .13f))
                drawLine(palette.foreground.copy(alpha = .08f * alpha), Offset(w * .07f, h * .86f), Offset(w * .45f, h * .86f), 2f)
            }
            FrameTheme.VIOLET_NEON -> {
                // Night Bloom: elegant petal linework, not neon.
                val stem = Path().apply {
                    moveTo(w * .90f, h * .08f)
                    cubicTo(w * .81f, h * .25f, w * .91f, h * .44f, w * .75f, h * .66f)
                }
                drawPath(stem, palette.tertiary.copy(alpha = .17f * alpha), style = Stroke(1.5f, cap = StrokeCap.Round))
                drawOval(palette.primary.copy(alpha = .095f * alpha), Offset(w * .70f, h * .18f), Size(w * .16f, h * .075f), style = Stroke(1.4f))
                drawOval(palette.secondary.copy(alpha = .090f * alpha), Offset(w * .78f, h * .37f), Size(w * .14f, h * .065f), style = Stroke(1.3f))
                drawOval(palette.primary.copy(alpha = .060f * alpha), Offset(w * .65f, h * .56f), Size(w * .14f, h * .065f), style = Stroke(1.2f))
            }
            FrameTheme.AURORA_GLASS -> {
                // Blue Hour: wide horizon bands, like a quiet editing room before sunrise.
                drawRect(palette.primary.copy(alpha = .035f * alpha), Offset(0f, h * .20f), Size(w, h * .10f))
                drawRect(palette.secondary.copy(alpha = .025f * alpha), Offset(0f, h * .58f), Size(w, h * .15f))
                drawLine(palette.primary.copy(alpha = .16f * alpha), Offset(w * .04f, h * .31f), Offset(w * .96f, h * .31f), 1.6f)
                drawLine(palette.foreground.copy(alpha = .05f * alpha), Offset(w * .15f, h * .73f), Offset(w * .86f, h * .73f), 1f)
            }
            FrameTheme.STORYBOARD -> {
                // Storyboard: shot-frame grid, center marks and production ticks.
                val left = w * .08f
                val top = h * .12f
                val cellW = w * .38f
                val cellH = h * .16f
                repeat(2) { row ->
                    repeat(2) { col ->
                        val x = left + col * (cellW + w * .06f)
                        val y = top + row * (cellH + h * .055f)
                        drawRect(
                            palette.foreground.copy(alpha = .095f * alpha),
                            Offset(x, y),
                            Size(cellW, cellH),
                            style = Stroke(1.1f),
                        )
                        drawLine(palette.primary.copy(alpha = .13f * alpha), Offset(x + cellW * .44f, y + cellH * .50f), Offset(x + cellW * .56f, y + cellH * .50f), 1f)
                        drawLine(palette.primary.copy(alpha = .13f * alpha), Offset(x + cellW * .50f, y + cellH * .38f), Offset(x + cellW * .50f, y + cellH * .62f), 1f)
                    }
                }
                repeat(4) { i ->
                    drawRect(palette.tertiary.copy(alpha = .12f * alpha), Offset(w * (.09f + i * .07f), h * .58f), Size(w * .035f, h * .008f))
                }
                drawLine(palette.foreground.copy(alpha = .06f * alpha), Offset(w * .08f, h * .64f), Offset(w * .92f, h * .64f), 1f)
                drawLine(palette.foreground.copy(alpha = .06f * alpha), Offset(w * .08f, h * .72f), Offset(w * .74f, h * .72f), 1f)
            }
        }
    }
}

internal val V137ThemeOrder = listOf(
    FrameTheme.DIRECTORS_CUT,
    FrameTheme.STUDIO_GOLD,
    FrameTheme.MIDNIGHT,
    FrameTheme.LUMEN_FLOW,
    FrameTheme.PAPER_QUIET,
    FrameTheme.MOSS_STUDIO,
    FrameTheme.EMBER,
    FrameTheme.VIOLET_NEON,
    FrameTheme.AURORA_GLASS,
    FrameTheme.STORYBOARD,
)
