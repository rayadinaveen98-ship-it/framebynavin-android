package com.framebynavin.app.reminders

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * V336, Director's Cut reminder art direction.
 *
 * All pixels are drawn by Compose Canvas: no WebView, rasterized button,
 * screenshot overlay, extra controls or touch interceptors. Both Creator
 * and Personal alert screens use the same palette, icon geometry and rings.
 * Existing reminder action/notification/privacy code is not referenced.
 *
 * This is decorative; never reports microphone input or actual TTS amplitude.
 */
internal object V336DirectorPalette {
    val black = Color(0xFF080408)
    val wine = Color(0xFF25070F)
    val ruby = Color(0xFFE94A45)
    val hot = Color(0xFFFF647F)
    val pearl = Color(0xFFFFE2D4)
    val gold = Color(0xFFD2AE67)
    val ivory = Color(0xFFF4F0E8)
    val muted = Color(0xFFB9A49D)
    val panel = Color(0xDD260C14)
    val line = Color(0x668E3948)
}

@Composable
internal fun V336MotionTime(
    motionEnabled: Boolean,
    lowPower: Boolean,
): Float {
    val elapsed by produceState(0f, motionEnabled, lowPower) {
        if (!motionEnabled) {
            value = 0f
        } else {
            var started = 0L
            var last = 0L
            val frameInterval = if (lowPower) 50_000_000L else 33_333_333L
            while (true) {
                withFrameNanos { now ->
                    if (started == 0L) started = now
                    if (last == 0L || now - last >= frameInterval) {
                        value = ((now - started) / 1_000_000_000.0).toFloat()
                        last = now
                    }
                }
            }
        }
    }
    return elapsed
}

private fun DrawScope.v336FlowLine(
    y: Float,
    amplitude: Float,
    t: Float,
    delay: Float,
    color: Color,
    thickness: Float,
) {
    val wave = Path().apply {
        moveTo(-size.width * .09f, y)
        cubicTo(
            size.width * .18f, y - amplitude * (1f + sin(t + delay) * .14f),
            size.width * .30f, y + amplitude,
            size.width * .52f, y - amplitude * .35f,
        )
        cubicTo(
            size.width * .78f, y - amplitude * 1.2f,
            size.width * .82f, y + amplitude * .95f,
            size.width * 1.10f, y - amplitude * .20f,
        )
    }
    drawPath(wave, color.copy(alpha = .10f), style = Stroke(width = thickness * 7f, cap = StrokeCap.Round))
    drawPath(wave, color.copy(alpha = .19f), style = Stroke(width = thickness * 3f, cap = StrokeCap.Round))
    drawPath(wave, color.copy(alpha = .65f), style = Stroke(width = thickness, cap = StrokeCap.Round))
}

/** Diffuse full-screen moving ribbon light: text and buttons remain crisp. */
@Composable
internal fun V336DirectorBackdrop(
    motionEnabled: Boolean,
    lowPower: Boolean,
    modifier: Modifier = Modifier,
) {
    val t = V336MotionTime(motionEnabled, lowPower)
    Canvas(modifier.fillMaxSize().testTag("v336-director-backdrop")) {
        drawRect(
            Brush.verticalGradient(
                listOf(V336DirectorPalette.black, V336DirectorPalette.wine,
                    Color(0xFF14060C), V336DirectorPalette.black),
            ),
        )
        val glow = Offset(size.width * .52f, size.height * .31f)
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFFBD2448).copy(alpha = .36f), Color.Transparent),
                center = glow, radius = size.width * .88f,
            ),
            radius = size.width * .88f, center = glow,
        )
        if (!lowPower) {
            val glow2 = Offset(size.width * .16f, size.height * .78f)
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF75142A).copy(alpha = .20f), Color.Transparent),
                    center = glow2, radius = size.width * .72f,
                ),
                radius = size.width * .72f, center = glow2,
            )
        }
        for (index in 0 until if (lowPower) 2 else 4) {
            val height = size.height * (.23f + index * .045f)
            val amplitude = size.height * (.035f + index * .008f)
            v336FlowLine(height + sin(t * .38f + index).toFloat() * size.height * .014f,
                amplitude, t * .24f, index * .71f,
                if (index % 2 == 0) V336DirectorPalette.hot else V336DirectorPalette.gold,
                .9.dp.toPx())
        }
        for (i in 0 until if (lowPower) 9 else 28) {
            // Deterministic stars; no randomized allocations per frame.
            val x = ((i * 127 + 43) % 251) / 251f * size.width
            val y = ((i * 83 + 17) % 269) / 269f * size.height
            val pulse = .52f + .32f * sin(t * .62f + i * .87f).toFloat()
            drawCircle(V336DirectorPalette.hot.copy(alpha = .17f * pulse),
                3.7.dp.toPx(), Offset(x, y))
            drawCircle(V336DirectorPalette.pearl.copy(alpha = .75f * pulse),
                .75.dp.toPx(), Offset(x, y))
        }
    }
}

/** Icon particles and orbital rings, in one bounded, responsive hero canvas. */
@Composable
internal fun V336DirectorSignalArt(
    kind: ReminderCheckInKind,
    motionEnabled: Boolean,
    lowPower: Boolean,
    modifier: Modifier = Modifier,
) {
    val glyph = if (kind == ReminderCheckInKind.ALARM)
        V195ReminderGlyph.CLOCK else V195ReminderGlyph.MICROPHONE
    val dots = remember(glyph, lowPower) {
        V195ParticleGeometry.build(glyph, if (lowPower) 310 else 580)
    }
    val t = V336MotionTime(motionEnabled, lowPower)
    Canvas(modifier.testTag("v336-director-signal-art")) {
        val mid = Offset(size.width * .5f, size.height * .51f)
        val base = min(size.width, size.height)
        val radius = base * .315f
        val bloom = radius * 1.75f
        drawCircle(
            Brush.radialGradient(
                0f to V336DirectorPalette.hot.copy(alpha = .28f),
                .33f to V336DirectorPalette.hot.copy(alpha = .08f),
                1f to Color.Transparent,
                center = mid, radius = bloom,
            ),
            center = mid, radius = bloom,
        )
        for (ring in 0..3) {
            val r = radius * (1.16f + ring * .13f)
            drawCircle(
                color = V336DirectorPalette.hot.copy(alpha = if (ring == 0) .27f else .11f),
                radius = r, center = mid,
                style = Stroke(width = .8.dp.toPx()),
            )
        }
        for (ring in 0..2) {
            val r = radius * (1.21f + ring * .12f)
            drawArc(
                color = if (ring == 1) V336DirectorPalette.gold.copy(alpha = .68f)
                    else V336DirectorPalette.hot.copy(alpha = .38f),
                startAngle = (t * (if (ring == 1) -5f else 4f) + ring * 92f) % 360f,
                sweepAngle = 42f + ring * 14f,
                useCenter = false,
                topLeft = Offset(mid.x - r, mid.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = (if (ring == 1) 1.5f else 1f).dp.toPx(),
                    cap = StrokeCap.Round),
            )
        }
        // Full-width ribbons appear behind, never covering the glyph strokes.
        for (lane in 0 until if (lowPower) 2 else 4) {
            val lineY = mid.y + base * (-.18f + lane * .105f) +
                sin(t * .50f + lane * 1.1f).toFloat() * base * .028f
            v336FlowLine(
                y = lineY, amplitude = base * (.17f + lane * .017f),
                t = t, delay = lane.toFloat(),
                color = if (lane % 2 == 0) V336DirectorPalette.pearl else V336DirectorPalette.hot,
                thickness = if (lane == 0) 1.5.dp.toPx() else .8.dp.toPx(),
            )
        }
        // Keep the crisp particle signal in FRONT of the ribbons.
        dots.forEachIndexed { index, dot ->
            val drift = if (motionEnabled) sin(t * .88f + dot.phase).toFloat() * base * .004f else 0f
            val x = mid.x + dot.x * radius + drift
            val y = mid.y + dot.y * radius + drift * .6f
            val at = Offset(x, y)
            val color = if (index % 9 == 0) V336DirectorPalette.gold
                else if (index % 4 == 0) V336DirectorPalette.pearl
                else V336DirectorPalette.hot
            val dotRadius = if (index % 11 == 0) 1.9.dp.toPx() else 1.4.dp.toPx()
            if (!lowPower && index % 16 == 0) {
                drawCircle(color.copy(alpha = .20f), dotRadius * 3.1f, at)
            }
            drawCircle(color.copy(alpha = .94f), dotRadius, at)
        }
        // Distinctive upper alarm bells; never synthetic task/status UI.
        if (kind == ReminderCheckInKind.ALARM) {
            for (side in listOf(-1f, 1f)) {
                val cx = mid.x + side * radius * .88f
                val cy = mid.y - radius * .86f
                drawArc(
                    color = V336DirectorPalette.hot.copy(alpha = .84f),
                    startAngle = if (side < 0) 190f else 270f,
                    sweepAngle = 93f,
                    useCenter = false,
                    topLeft = Offset(cx - radius * .25f, cy - radius * .18f),
                    size = Size(radius * .5f, radius * .35f),
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            drawLine(
                V336DirectorPalette.hot.copy(alpha = .85f),
                Offset(mid.x - radius * .53f, mid.y + radius * .72f),
                Offset(mid.x - radius * .70f, mid.y + radius * 1.01f),
                strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round,
            )
            drawLine(
                V336DirectorPalette.hot.copy(alpha = .85f),
                Offset(mid.x + radius * .53f, mid.y + radius * .72f),
                Offset(mid.x + radius * .70f, mid.y + radius * 1.01f),
                strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round,
            )
        }
    }
}
