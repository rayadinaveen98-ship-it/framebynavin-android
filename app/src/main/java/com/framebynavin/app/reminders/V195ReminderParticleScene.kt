package com.framebynavin.app.reminders

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
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
 * V195.5 – visible circular cores, soft supporting bokeh, true independent
 * full-screen travel, smooth reflected motion at every viewport boundary.
 *
 * All geometry and palettes are remembered; no per-particle Composables.
 * One Canvas, no GPU blur/shaders, no navigation/service/audio changes.
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
    val ambient = remember(lowPower) { V202PremiumParticleGeometry.build(lowPower) }
    val palette = remember(kind) {
        if (kind == ReminderCheckInKind.ALARM) listOf(
            Color(0xFFFFCF52), Color(0xFFFFA724), Color(0xFFFF793B), Color(0xFFFF527D),
            Color(0xFFFF4BB9), Color(0xFFE065FF), Color(0xFFA56DFF), Color(0xFFFF9473),
        ) else listOf(
            Color(0xFF3EEDFF), Color(0xFF48B6FF), Color(0xFF677FFF), Color(0xFFA376FF),
            Color(0xFFDB69FF), Color(0xFFFF69D4), Color(0xFF45F3C9), Color(0xFF73C7FF),
        )
    }
    val atmosphere = remember(kind) {
        Brush.verticalGradient(
            if (kind == ReminderCheckInKind.ALARM) listOf(
                Color(0xFF1B1021), Color(0xFF211328), Color(0xFF0A1729),
            ) else listOf(
                Color(0xFF100F2D), Color(0xFF1A1435), Color(0xFF0B202D),
            ),
        )
    }
    val glyphColor = if (kind == ReminderCheckInKind.ALARM)
        Color(0xFFFFE7A0) else Color(0xFFE2D3FF)
    val revealProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = if (motionEnabled) 650 else 0),
        label = "v1955-premium-reveal",
    )

    // Monotonic frame time never restarts at an arbitrary 3.8-second boundary.
    // Producing state is lifecycle-bound; when animations are disabled, no loop runs.
    val elapsedSeconds by produceState(0.0, motionEnabled) {
        if (motionEnabled) {
            var startedAt = 0L
            while (true) {
                withFrameNanos { frame ->
                    if (startedAt == 0L) startedAt = frame
                    value = (frame - startedAt).toDouble() / 1_000_000_000.0
                }
            }
        }
    }
    val iconPhase = ((elapsedSeconds * V198ParticleMotion.PHASE_END / 3.8)
        % V198ParticleMotion.PHASE_END).toFloat()
    val interactions = remember { MutableInteractionSource() }
    val touch = if (revealed) Modifier else Modifier
        .semantics { contentDescription = "Show reminder controls" }
        .clickable(interactionSource = interactions, indication = null, onClick = onReveal)

    Canvas(modifier = modifier.fillMaxSize().testTag("v1952-ambient-particles").then(touch)) {
        drawRect(brush = atmosphere)
        val farRadius = 1.45.dp.toPx()
        val midRadius = 2.15.dp.toPx()
        val nearRadius = 2.90.dp.toPx()
        val scrimmed = 1f - .58f * revealProgress
        // Every particle moves across the *entire* screen at a distinct angle and
        // pace. Positions reflect with zero velocity at the edge, then reverse.
        for (dot in ambient) {
            val normalizedX = if (motionEnabled)
                V202InfiniteBounce.position(dot.x, dot.velocityX, elapsedSeconds) else dot.x
            val normalizedY = if (motionEnabled)
                V202InfiniteBounce.position(dot.y, dot.velocityY, elapsedSeconds) else dot.y
            val center = Offset(normalizedX * size.width, normalizedY * size.height)
            val blend = if (motionEnabled)
                V202InfiniteBounce.colorBlend(elapsedSeconds, dot.colorPhase, dot.colorRate) else .5f
            val pigment = lerp(palette[dot.colorIndex], palette[(dot.colorIndex + 1) % 8], blend)
            val depth = when (dot.depth) { 0 -> .68f; 1 -> .88f; else -> 1f }
            val radius = when (dot.depth) { 0 -> farRadius; 1 -> midRadius; else -> nearRadius }
            // Background glow only. Crisp anti-aliased circular core is always drawn last.
            if (!lowPower && dot.depth == 2) {
                drawCircle(pigment.copy(alpha = .045f * scrimmed * depth),
                    radius = radius * 3.9f, center = center)
            }
            drawCircle(pigment.copy(alpha = .10f * scrimmed * depth),
                radius = radius * 2.2f, center = center)
            drawCircle(pigment.copy(alpha = .88f * scrimmed * depth),
                radius = radius, center = center)
        }

        // Clear content labels and buttons against a much brighter ambient field.
        if (revealProgress > 0f) drawRect(Color.Black.copy(alpha = .32f * revealProgress))

        // Keep the icon sharply legible. Its dots move locally, never scatter.
        if (revealProgress < 1f) {
            val glyphRadius = if (glyph == V195ReminderGlyph.CLOCK)
                min(size.width * .43f, size.height * .31f)
                else min(size.width * .66f, size.height * .34f)
            val center = Offset(size.width * .5f, size.height * .43f)
            val fade = 1f - revealProgress
            val pulse = if (motionEnabled) V198ParticleMotion.iconPulse(iconPhase) else 1f
            val opacity = fade * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0xFF060717).copy(alpha = .85f * fade),
                        Color(0xFF060717).copy(alpha = .35f * fade),
                        Color.Transparent,
                    ), center = center, radius = glyphRadius * 1.35f,
                ),
                center = center, radius = glyphRadius * 1.35f,
            )
            val dotRadius = if (lowPower) 2.15.dp.toPx() else 1.95.dp.toPx()
            for (dot in glyphDots) {
                val harmonic = dot.speed.toInt().coerceIn(1, 3).toFloat()
                val dx = if (motionEnabled)
                    V198ParticleMotion.wave(iconPhase, harmonic, dot.phase) * .003f else 0f
                val dy = if (motionEnabled)
                    V198ParticleMotion.wave(iconPhase, harmonic, dot.phase + 1.57f) * .003f else 0f
                val position = Offset(
                    center.x + (dot.x + dx) * glyphRadius,
                    center.y + (dot.y + dy) * glyphRadius,
                )
                val shine = if (motionEnabled)
                    V198ParticleMotion.colorMix(iconPhase, 2f, dot.phase) else .5f
                drawCircle(glyphColor.copy(alpha = (.13f + .10f * shine) * opacity),
                    radius = dotRadius * 2.25f, center = position)
                drawCircle(glyphColor.copy(alpha = opacity.coerceIn(0f, 1f)),
                    radius = dotRadius, center = position)
            }
        }
    }
}
