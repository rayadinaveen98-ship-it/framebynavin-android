package com.framebynavin.app.reminders

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.RecRed
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Premium, deterministic alarm hero. No bitmap/3D asset: every element is native Compose. */
@Composable
internal fun V140AlarmPulse(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "v140Alarm")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing)),
        label = "v140AlarmPhase",
    )

    Canvas(modifier.size(116.dp)) {
        val c = center
        val r = size.minDimension * .36f
        val ring = size.minDimension * .020f

        // Controlled wake-up pulse. It expands without scaling the actual icon.
        val shock = r * (.92f + phase * .34f)
        drawCircle(
            color = RecRed.copy(alpha = (1f - phase) * .18f),
            radius = shock,
            center = c,
            style = Stroke(width = ring * .72f),
        )
        drawCircle(Color(0xFF190B0D), radius = r * 1.12f, center = c)
        drawCircle(
            color = RecRed.copy(alpha = .15f),
            radius = r * 1.12f,
            center = c,
            style = Stroke(width = ring),
        )
        drawCircle(Color(0xFF0B090A), radius = r * .82f, center = c)
        drawCircle(
            color = RecRed.copy(alpha = .72f),
            radius = r * .82f,
            center = c,
            style = Stroke(width = ring * .72f),
        )

        // Precision clock ticks.
        repeat(12) { index ->
            val angle = ((index * 30f - 90f) * PI / 180f).toFloat()
            val outer = Offset(c.x + cos(angle) * r * .70f, c.y + sin(angle) * r * .70f)
            val innerRadius = if (index % 3 == 0) r * .56f else r * .61f
            val inner = Offset(c.x + cos(angle) * innerRadius, c.y + sin(angle) * innerRadius)
            drawLine(
                color = RecRed.copy(alpha = if (index % 3 == 0) .80f else .36f),
                start = inner,
                end = outer,
                strokeWidth = if (index % 3 == 0) ring * .72f else ring * .40f,
                cap = StrokeCap.Round,
            )
        }

        // Hour + minute hands stay composed while a fine attention hand keeps moving.
        fun hand(angleDeg: Float, length: Float, width: Float, alpha: Float) {
            val a = ((angleDeg - 90f) * PI / 180f).toFloat()
            drawLine(
                color = RecRed.copy(alpha = alpha),
                start = c,
                end = Offset(c.x + cos(a) * length, c.y + sin(a) * length),
                strokeWidth = width,
                cap = StrokeCap.Round,
            )
        }
        hand(42f, r * .40f, ring * 1.25f, .96f)
        hand(188f, r * .58f, ring * .84f, .86f)
        hand(phase * 360f, r * .66f, ring * .42f, .74f)
        drawCircle(RecRed, radius = ring * 1.15f, center = c)

        // Alarm bell arcs, kept small and architectural instead of cartoon-bouncing.
        val bellTop = c.y - r * .98f
        val bellSize = Size(r * .58f, r * .38f)
        val bellAlpha = .34f + .24f * (0.5f + 0.5f * sin(phase * PI.toFloat() * 4f))
        drawArc(
            color = RecRed.copy(alpha = bellAlpha),
            startAngle = 205f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = Offset(c.x - r * .83f, bellTop),
            size = bellSize,
            style = Stroke(width = ring * .72f, cap = StrokeCap.Round),
        )
        drawArc(
            color = RecRed.copy(alpha = bellAlpha),
            startAngle = 240f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = Offset(c.x + r * .25f, bellTop),
            size = bellSize,
            style = Stroke(width = ring * .72f, cap = StrokeCap.Round),
        )

        // Moving rim trace adds motion even between the larger pulse beats.
        drawArc(
            color = RecRed.copy(alpha = .88f),
            startAngle = -90f + phase * 360f,
            sweepAngle = 58f,
            useCenter = false,
            topLeft = Offset(c.x - r * 1.12f, c.y - r * 1.12f),
            size = Size(r * 2.24f, r * 2.24f),
            style = Stroke(width = ring * .82f, cap = StrokeCap.Round),
        )
    }
}

/** Voice hero with independent waveform motion rather than one pulsing container. */
@Composable
internal fun V140VoicePulse(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "v140Voice")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_650, easing = LinearEasing)),
        label = "v140VoicePhase",
    )

    Canvas(modifier.size(120.dp)) {
        val c = center
        val r = size.minDimension * .38f
        val ring = size.minDimension * .018f
        val breath = .5f + .5f * sin(phase * PI.toFloat() * 2f)

        drawCircle(Color(0xFF17140E), radius = r * 1.12f, center = c)
        drawCircle(
            color = MutedGold.copy(alpha = .13f + .06f * breath),
            radius = r * (1.06f + .025f * breath),
            center = c,
            style = Stroke(width = ring),
        )
        drawCircle(Color(0xFF0C0B09), radius = r * .80f, center = c)

        val bars = 9
        val barW = r * .105f
        val gap = r * .095f
        val total = bars * barW + (bars - 1) * gap
        val left = c.x - total / 2f
        repeat(bars) { index ->
            val local = abs(
                sin(
                    phase * PI.toFloat() * (2.0f + (index % 3) * .22f) +
                        index * .67f,
                ),
            )
            val centerWeight = 1f - abs(index - (bars - 1) / 2f) / 7.5f
            val barH = r * (.24f + local * (.68f * centerWeight))
            val x = left + index * (barW + gap)
            drawRoundRect(
                color = MutedGold.copy(alpha = .72f + local * .25f),
                topLeft = Offset(x, c.y - barH / 2f),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW, barW),
            )
        }

        // Two thin orbit traces move at different speeds so the visual never feels frozen.
        drawArc(
            color = MutedGold.copy(alpha = .72f),
            startAngle = -100f + phase * 360f,
            sweepAngle = 74f,
            useCenter = false,
            topLeft = Offset(c.x - r * 1.12f, c.y - r * 1.12f),
            size = Size(r * 2.24f, r * 2.24f),
            style = Stroke(width = ring * .76f, cap = StrokeCap.Round),
        )
        drawArc(
            color = MutedGold.copy(alpha = .24f),
            startAngle = 110f - phase * 220f,
            sweepAngle = 38f,
            useCenter = false,
            topLeft = Offset(c.x - r * .94f, c.y - r * .94f),
            size = Size(r * 1.88f, r * 1.88f),
            style = Stroke(width = ring * .46f, cap = StrokeCap.Round),
        )
    }
}
