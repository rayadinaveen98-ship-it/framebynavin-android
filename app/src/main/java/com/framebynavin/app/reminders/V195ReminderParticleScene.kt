package com.framebynavin.app.reminders

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * V195.3 — layered, richly colored particle scene for alarm and voice alerts.
 * One canvas. Each dot smoothly morphs between two vibrant colors and moves
 * along a repeating closed curve. Glyph pulses without changing its identity.
 * Audio scheduling and reminder actions are intentionally untouched.
 */
@Composable
internal fun V195ReminderParticleScene(
    kind: ReminderCheckInKind,
    revealed: Boolean,
    lowPower: Boolean,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean = true,
) {
    val glyph = if (kind == ReminderCheckInKind.ALARM)
        V195ReminderGlyph.CLOCK else V195ReminderGlyph.MICROPHONE
    val glyphDots = remember(glyph, lowPower) {
        V195ParticleGeometry.build(
            glyph,
            if (lowPower) V195ParticleGeometry.LOW_POWER_DOTS else V195ParticleGeometry.DEFAULT_DOTS,
        )
    }
    val ambient = remember(lowPower) { V197AmbientGeometry.build(lowPower) }
    val ambientPalette = remember(kind) {
        if (kind == ReminderCheckInKind.ALARM) listOf(
            Color(0xFFFF7558), Color(0xFFFFCA5C), Color(0xFFFF943D),
            Color(0xFFFF60C8), Color(0xFFE36AFF), Color(0xFF6C9EFF),
        ) else listOf(
            Color(0xFF4FD9FF), Color(0xFF6196FF), Color(0xFF9973FF),
            Color(0xFFE26CFF), Color(0xFFFF67C7), Color(0xFF4DF3D7),
        )
    }
    val atmosphere = remember(kind) {
        Brush.verticalGradient(
            if (kind == ReminderCheckInKind.ALARM) listOf(
                Color(0xFF241825), Color(0xFF29182A), Color(0xFF111C2C),
            ) else listOf(
                Color(0xFF181A36), Color(0xFF221B3B), Color(0xFF10272D),
            ),
        )
    }
    val glyphColor = if (kind == ReminderCheckInKind.ALARM)
        Color(0xFFFFE6A4) else Color(0xFFDBC9FF)
    val revealProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "v1953-reminder-scene-reveal",
    )
    val transition = rememberInfiniteTransition(label = "v1953-particle-motion")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = V198ParticleMotion.PHASE_END,
        animationSpec = infiniteRepeatable(tween(V198ParticleMotion.CYCLE_MILLIS, easing = LinearEasing)),
        label = "v1953-motion-phase",
    )
    val time = if (motionEnabled) phase else 0f
    val interactions = remember { MutableInteractionSource() }
    val touch = if (revealed) Modifier else Modifier
        .semantics { contentDescription = "Show reminder controls" }
        .clickable(interactionSource = interactions, indication = null, onClick = onReveal)

    Canvas(modifier = modifier.fillMaxSize().testTag("v1952-ambient-particles").then(touch)) {
        drawRect(brush = atmosphere)
        val radii = floatArrayOf(1.5.dp.toPx(), 2.2.dp.toPx(), 3.3.dp.toPx())
        val ambientCoreAlpha = .69f * (1f - revealProgress) + .31f * revealProgress
        val baseHaloAlpha = .15f * (1f - revealProgress) + .10f * revealProgress

        // Full-viewport ambient layer: seamless loops, 3 depth planes, per-dot hue morphing.
        for (dot in ambient) {
            val x = if (motionEnabled) V198ParticleMotion.wave(time, dot.speed, dot.phase) * dot.driftX else 0f
            val y = if (motionEnabled) V198ParticleMotion.wave(
                time, (dot.speed.toInt() % 3 + 1).toFloat(), dot.phase + 1.5708f,
            ) * dot.driftY else 0f
            val center = Offset(
                (dot.x + x).coerceIn(0f, 1f) * size.width,
                (dot.y + y).coerceIn(0f, 1f) * size.height,
            )
            val colorBlend = if (motionEnabled)
                V198ParticleMotion.colorMix(time, dot.speed, dot.phase + 1.3f)
                else .36f
            val shade = lerp(
                ambientPalette[dot.palette],
                ambientPalette[(dot.palette + 1) % ambientPalette.size],
                colorBlend,
            )
            val depthScale = when (dot.depth) { 0 -> .62f; 1 -> .85f; else -> 1f }
            val twinkle = if (motionEnabled)
                .85f + .15f * V198ParticleMotion.colorMix(time, 2f, dot.phase)
                else 1f
            val dotRadius = radii[dot.depth]
            val ambientHaloAlpha = baseHaloAlpha * depthScale * twinkle
            // Rich glow without costly blur: depth-tier concentric circles.
            if (dot.depth == 2 && !lowPower) {
                drawCircle(
                    color = shade.copy(alpha = ambientHaloAlpha * .37f),
                    radius = dotRadius * (4.0f + .7f * revealProgress),
                    center = center,
                )
            }
            drawCircle(color = shade.copy(alpha = ambientHaloAlpha),
                radius = dotRadius * (2.6f + revealProgress), center = center)
            drawCircle(color = shade.copy(alpha = ambientCoreAlpha * depthScale * twinkle),
                radius = dotRadius, center = center)
        }

        // Keep action labels/highlight readable even with richer ambient colors.
        if (revealProgress > 0f) {
            drawRect(color = Color.Black.copy(alpha = .28f * revealProgress))
        }

        // The icon is a separate, color-stable layer; no blurred clock behind controls.
        if (revealProgress < 1f) {
            val glyphRadius = if (glyph == V195ReminderGlyph.CLOCK)
                min(size.width * .43f, size.height * .31f)
                else min(size.width * .66f, size.height * .34f)
            val center = Offset(size.width * .5f, size.height * .43f)
            val fade = 1f - revealProgress
            val blink = if (motionEnabled) V198ParticleMotion.iconPulse(time) else 1f
            val opacity = fade * blink
            // Radial contrast window, fading away completely once controls appear.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF080916).copy(alpha = .70f * fade),
                        Color(0xFF080916).copy(alpha = .32f * fade),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = glyphRadius * 1.30f,
                ),
                center = center,
                radius = glyphRadius * 1.30f,
            )
            val symbolDotRadius = if (lowPower) 2.8.dp.toPx() else 2.7.dp.toPx()
            for (dot in glyphDots) {
                // Integer harmonics avoid time-wrap jumps. Subtle movement retains shape.
                val harmonic = dot.speed.toInt().coerceIn(1, 3).toFloat()
                val dx = if (motionEnabled) V198ParticleMotion.wave(time, harmonic, dot.phase) * .0065f else 0f
                val dy = if (motionEnabled) V198ParticleMotion.wave(time, harmonic, dot.phase + 1.57f) * .0065f else 0f
                val position = Offset(
                    center.x + (dot.x + dx) * glyphRadius,
                    center.y + (dot.y + dy) * glyphRadius,
                )
                drawCircle(
                    color = glyphColor.copy(alpha = .24f * opacity),
                    radius = symbolDotRadius * (2.4f + .6f * blink),
                    center = position,
                )
                drawCircle(
                    color = glyphColor.copy(alpha = opacity.coerceIn(0f, 1f)),
                    radius = symbolDotRadius,
                    center = position,
                )
            }
        }
    }
}
