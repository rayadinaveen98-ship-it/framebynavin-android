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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.framebynavin.app.ui.theme.BacklotAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Living reminder scene, not an intro video:
 * 1. Separate particles densely cover the entire safe viewport.
 * 2. Clock/mic dots retain their own CONSTANT color and pulse in brightness.
 * 3. On a tap, icon particles disappear; the ambient bokeh remains behind actions.
 * 4. After reveal, this surface has no click target and cannot steal action taps.
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
    val glyph = if (kind == ReminderCheckInKind.ALARM) V195ReminderGlyph.CLOCK
        else V195ReminderGlyph.MICROPHONE
    val glyphDots = remember(glyph, lowPower) {
        V195ParticleGeometry.build(
            glyph,
            if (lowPower) V195ParticleGeometry.LOW_POWER_DOTS else V195ParticleGeometry.DEFAULT_DOTS,
        )
    }
    val ambient = remember(lowPower) { V197AmbientGeometry.build(lowPower) }
    val ambientPalette = remember(kind) {
        if (kind == ReminderCheckInKind.ALARM) listOf(
            Color(0xFFC86B56), Color(0xFFE1A26D), Color(0xFF87679D), Color(0xFF60759E),
        ) else listOf(
            Color(0xFF647CCA), Color(0xFF9B70D6), Color(0xFF51AFB7), Color(0xFFBD76A3),
        )
    }
    val glyphColor = if (kind == ReminderCheckInKind.ALARM) BacklotAccent
        else Color(0xFFB69CFF)
    val revealProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 750),
        label = "v1952-soft-bokeh-reveal",
    )
    val transition = rememberInfiniteTransition(label = "v1952-ambient-particle-drift")
    val runningPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)),
        label = "v1952-global-phase",
    )
    val t = if (motionEnabled) runningPhase else 0f
    val interactions = remember { MutableInteractionSource() }
    val touch = if (revealed) Modifier else Modifier
        .semantics { contentDescription = "Show reminder controls" }
        .clickable(interactionSource = interactions, indication = null, onClick = onReveal)

    Canvas(modifier = modifier.fillMaxSize().testTag("v1952-ambient-particles").then(touch)) {
        val radius = 2.5.dp.toPx()
        val ambientCoreAlpha = .38f * (1f - revealProgress) + .16f * revealProgress
        val ambientHaloAlpha = .085f * (1f - revealProgress) + .11f * revealProgress
        val ambientHaloRadius = radius * (2.6f + 1.1f * revealProgress)

        // Layer 1: all-over, colorful drifting ambient particles.
        for (dot in ambient) {
            val driftX = if (motionEnabled) sin((t * dot.speed + dot.phase).toDouble()).toFloat() * dot.driftX else 0f
            val driftY = if (motionEnabled) cos((t * (.75f * dot.speed) + dot.phase).toDouble()).toFloat() * dot.driftY else 0f
            val center = Offset(
                (dot.x + driftX).coerceIn(0f, 1f) * size.width,
                (dot.y + driftY).coerceIn(0f, 1f) * size.height,
            )
            val shade = ambientPalette[dot.palette]
            drawCircle(color = shade.copy(alpha = ambientHaloAlpha), radius = ambientHaloRadius, center = center)
            drawCircle(color = shade.copy(alpha = ambientCoreAlpha), radius = radius, center = center)
        }

        // Layer 2: subtle darkening as controls enter; depth without a costly blur filter.
        if (revealProgress > 0f) {
            drawRect(Color.Black.copy(alpha = .22f * revealProgress))
        }

        // Layer 3: always-readable monochromatic glyph, never ghosted behind text.
        if (revealProgress < 1f) {
            val glyphRadius = if (glyph == V195ReminderGlyph.CLOCK)
                min(size.width * .43f, size.height * .31f)
                else min(size.width * .66f, size.height * .34f)
            val center = Offset(size.width * .5f, size.height * .43f)
            val blink = if (motionEnabled) (.78f + .22f * sin((t * 3.1f).toDouble()).toFloat()) else 1f
            val opacity = (1f - revealProgress) * blink
            val symbolDotRadius = 2.65.dp.toPx()
            for (dot in glyphDots) {
                // Only tiny oscillations: randomness must never damage recognition.
                val dx = if (motionEnabled) sin((t * dot.speed + dot.phase).toDouble()).toFloat() * .009f else 0f
                val dy = if (motionEnabled) cos((t * dot.speed * .81f + dot.phase).toDouble()).toFloat() * .009f else 0f
                val position = Offset(
                    center.x + (dot.x + dx) * glyphRadius,
                    center.y + (dot.y + dy) * glyphRadius,
                )
                drawCircle(
                    color = glyphColor.copy(alpha = .16f * opacity),
                    radius = symbolDotRadius * 2.4f,
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
