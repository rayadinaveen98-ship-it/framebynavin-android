package com.framebynavin.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val V140_IDENT_DURATION_MS = 5_000
private const val V140_AMBIENT_THREADS = 26

private data class V140Segment(val ax: Float, val ay: Float, val bx: Float, val by: Float)

/**
 * Backlot v140 launch ident.
 *
 * One master five-second timeline owns the complete ident. Threads stay alive while a selected
 * set resolves into the BACKLOT wordmark; completion is reported immediately to the launch gate,
 * so there is no independent splash timer and no dead hold after the wordmark settles.
 */
@Composable
internal fun V140CinematicWelcome(onFinished: () -> Unit) {
    val context = LocalContext.current
    val timeline = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        WelcomeSonicIdent.play(context.applicationContext)
        timeline.snapTo(0f)
        timeline.animateTo(1f, animationSpec = tween(V140_IDENT_DURATION_MS, easing = LinearEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020203)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = timeline.value.coerceIn(0f, 1f)
            val formation = v140Smooth(((t - 0.30f) / 0.54f).coerceIn(0f, 1f))
            val settle = v140Smooth(((t - 0.84f) / 0.16f).coerceIn(0f, 1f))
            val ignition = v140Smooth((t / 0.16f).coerceIn(0f, 1f))
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.5f, h * 0.50f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF5A1519).copy(alpha = 0.15f * ignition * (1f - settle * .45f)),
                        Color(0xFF2A2117).copy(alpha = 0.08f * ignition),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = w * 0.70f,
                ),
                radius = w * 0.70f,
                center = center,
            )

            // Ambient threads never freeze. They gather toward the future wordmark, cross it,
            // then clear the stage while the letter threads finish settling.
            repeat(V140_AMBIENT_THREADS) { index ->
                val side = if (index % 2 == 0) -1f else 1f
                val lane = (index + 1f) / (V140_AMBIENT_THREADS + 1f)
                val phase = (t * 1.35f + index * 0.071f) % 1f
                val baseY = h * (0.22f + lane * 0.56f)
                val wave = sin((phase * PI * 2.0 + index * .63).toFloat()) * h * 0.026f
                val startX = if (side < 0f) -w * .18f else w * 1.18f
                val endX = if (side < 0f) w * 1.12f else -w * .12f
                val travel = v140Smooth(phase)
                val headX = v140Lerp(startX, endX, travel)
                val gather = formation * (1f - settle * .75f)
                val targetY = v140Lerp(baseY + wave, center.y + (index % 7 - 3) * h * .008f, gather * .72f)
                val tailX = headX - side * w * (0.18f + (index % 4) * .025f)
                val path = Path().apply {
                    moveTo(tailX, baseY - wave * .30f)
                    cubicTo(
                        v140Lerp(tailX, center.x, .36f),
                        baseY + wave,
                        v140Lerp(headX, center.x, .34f),
                        targetY - wave * .45f,
                        headX,
                        targetY,
                    )
                }
                val palette = when (index % 6) {
                    0 -> RecRed
                    1 -> MutedGold
                    2 -> ProjectorIvory
                    3 -> RecRed.copy(alpha = .84f)
                    4 -> ProjectorIvory.copy(alpha = .72f)
                    else -> MutedGold.copy(alpha = .76f)
                }
                val alpha = (0.22f + (index % 5) * .055f) * ignition * (1f - formation * .62f) * (1f - settle * .70f)
                drawPath(
                    path = path,
                    color = palette.copy(alpha = alpha.coerceIn(0f, .52f)),
                    style = Stroke(
                        width = w * (0.0022f + (index % 3) * .0008f),
                        cap = StrokeCap.Round,
                    ),
                )
            }

            // A quiet horizontal energy trace gives the moving threads a shared focal plane.
            val traceAlpha = (0.11f * ignition * (1f - settle)).coerceAtLeast(0f)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, RecRed.copy(alpha = traceAlpha), MutedGold.copy(alpha = traceAlpha), Color.Transparent),
                ),
                start = Offset(w * .10f, center.y),
                end = Offset(w * .90f, center.y),
                strokeWidth = w * .0015f,
                cap = StrokeCap.Round,
            )

            val specs = listOf(
                v140LetterB(), v140LetterA(), v140LetterC(), v140LetterK(),
                v140LetterL(), v140LetterO(), v140LetterT(),
            )
            val letterW = w * .075f
            val letterH = h * .112f
            val gap = w * .024f
            val totalW = letterW * specs.size + gap * (specs.size - 1)
            val originX = center.x - totalW / 2f
            val originY = center.y - letterH / 2f

            var segmentIndex = 0
            specs.forEachIndexed { letterIndex, segments ->
                val lx = originX + letterIndex * (letterW + gap)
                segments.forEach { seg ->
                    val targetA = Offset(lx + seg.ax * letterW, originY + seg.ay * letterH)
                    val targetB = Offset(lx + seg.bx * letterW, originY + seg.by * letterH)
                    val angle = (segmentIndex * 0.73f + letterIndex * .91f)
                    val radius = w * (0.19f + (segmentIndex % 5) * .018f)
                    val source = Offset(
                        center.x + cos(angle) * radius,
                        center.y + sin(angle * 1.17f) * h * .18f,
                    )
                    val sourceB = Offset(source.x + cos(angle + 1.2f) * w * .035f, source.y + sin(angle + 1.2f) * h * .020f)
                    val currentA = v140Lerp(source, targetA, formation)
                    val currentB = v140Lerp(sourceB, targetB, formation)
                    val color = when {
                        letterIndex == 0 && segmentIndex % 2 == 0 -> RecRed
                        (letterIndex + segmentIndex) % 6 == 0 -> MutedGold
                        else -> ProjectorIvory
                    }
                    val letterAlpha = (0.24f + formation * .74f).coerceIn(0f, .98f)
                    drawLine(
                        color = color.copy(alpha = letterAlpha),
                        start = currentA,
                        end = currentB,
                        strokeWidth = w * (0.0062f - formation * .0012f),
                        cap = StrokeCap.Round,
                    )
                    if (formation < .93f) {
                        drawLine(
                            color = color.copy(alpha = (1f - formation) * .10f),
                            start = source,
                            end = currentA,
                            strokeWidth = w * .0016f,
                            cap = StrokeCap.Round,
                        )
                    }
                    segmentIndex++
                }
            }

            // Final micro-settle: a restrained underline sweeps once beneath the formed name.
            if (t > .79f) {
                val sweep = v140Smooth(((t - .79f) / .17f).coerceIn(0f, 1f))
                val start = originX - w * .012f
                val end = start + totalW * sweep
                drawLine(
                    color = MutedGold.copy(alpha = .42f * (1f - settle * .72f)),
                    start = Offset(start, originY + letterH + h * .028f),
                    end = Offset(end, originY + letterH + h * .028f),
                    strokeWidth = w * .0023f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun v140LetterB() = listOf(
    V140Segment(0f, 0f, 0f, 1f),
    V140Segment(0f, 0f, .58f, 0f),
    V140Segment(.58f, 0f, .78f, .14f),
    V140Segment(.78f, .14f, .78f, .38f),
    V140Segment(.78f, .38f, .58f, .50f),
    V140Segment(0f, .50f, .58f, .50f),
    V140Segment(.58f, .50f, .80f, .62f),
    V140Segment(.80f, .62f, .80f, .86f),
    V140Segment(.80f, .86f, .58f, 1f),
    V140Segment(0f, 1f, .58f, 1f),
)

private fun v140LetterA() = listOf(
    V140Segment(0f, 1f, .42f, 0f),
    V140Segment(.42f, 0f, .84f, 1f),
    V140Segment(.18f, .58f, .66f, .58f),
)

private fun v140LetterC() = listOf(
    V140Segment(.82f, .06f, .24f, .06f),
    V140Segment(.24f, .06f, .04f, .24f),
    V140Segment(.04f, .24f, .04f, .78f),
    V140Segment(.04f, .78f, .24f, .96f),
    V140Segment(.24f, .96f, .82f, .96f),
)

private fun v140LetterK() = listOf(
    V140Segment(0f, 0f, 0f, 1f),
    V140Segment(.02f, .52f, .78f, 0f),
    V140Segment(.02f, .52f, .80f, 1f),
)

private fun v140LetterL() = listOf(
    V140Segment(0f, 0f, 0f, 1f),
    V140Segment(0f, 1f, .82f, 1f),
)

private fun v140LetterO() = listOf(
    V140Segment(.22f, .04f, .66f, .04f),
    V140Segment(.66f, .04f, .84f, .22f),
    V140Segment(.84f, .22f, .84f, .78f),
    V140Segment(.84f, .78f, .66f, .96f),
    V140Segment(.66f, .96f, .22f, .96f),
    V140Segment(.22f, .96f, .04f, .78f),
    V140Segment(.04f, .78f, .04f, .22f),
    V140Segment(.04f, .22f, .22f, .04f),
)

private fun v140LetterT() = listOf(
    V140Segment(0f, .04f, .86f, .04f),
    V140Segment(.43f, .04f, .43f, 1f),
)

private fun v140Smooth(value: Float): Float = value * value * (3f - 2f * value)
private fun v140Lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
private fun v140Lerp(a: Offset, b: Offset, t: Float): Offset = Offset(v140Lerp(a.x, b.x, t), v140Lerp(a.y, b.y, t))
