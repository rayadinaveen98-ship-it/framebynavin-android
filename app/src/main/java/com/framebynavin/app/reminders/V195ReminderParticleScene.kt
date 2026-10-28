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
 * Backlot V218 — one continuous premium particle field for alarm AND voice.
 *
 * Previous animation rendered independent ambient particles, a dark hole and
 * an unrelated mono-color dotted glyph on top. This replaces all three with
 * ONE colorful wave: a sampled subset of ambient particles gently migrates
 * into clock or microphone trajectories without changing colors or radius.
 * Other particles keep crossing the entire viewport. No black icon sticker.
 *
 * Manual tap-to-reveal, reduced motion, battery saver, content overlays and
 * every reminder acknowledgement / service / watchdog are unchanged.
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
    val ambient = remember(lowPower) { V202PremiumParticleGeometry.build(lowPower) }
    val glyphDots = remember(glyph, lowPower) {
        V195ParticleGeometry.build(
            glyph, if (lowPower) V218UnifiedWavePolicy.LOW_POWER_GLYPH_DOTS
                else V218UnifiedWavePolicy.NORMAL_GLYPH_DOTS,
        )
    }
    // V222: higher-chroma pigment while preserving the original unified particle
    // trajectories, clock/mic shape, reveal mechanics and accessible actions.
    val palette = remember(kind) {
        if (kind == ReminderCheckInKind.ALARM) listOf(
            Color(0xFFFFE16B), Color(0xFFFFB437), Color(0xFFFF6B3E), Color(0xFFFF497B),
            Color(0xFFFF35BD), Color(0xFFDA52FF), Color(0xFF9460FF), Color(0xFFFF8660),
        ) else listOf(
            Color(0xFF4CF5FF), Color(0xFF319EFF), Color(0xFF6475FF), Color(0xFFAB69FF),
            Color(0xFFE65DFF), Color(0xFFFF52C6), Color(0xFF28F5D9), Color(0xFF6DC7FF),
        )
    }
    val atmosphere = remember(kind) {
        Brush.verticalGradient(
            if (kind == ReminderCheckInKind.ALARM) listOf(
                Color(0xFF221027), Color(0xFF23132D), Color(0xFF101A32),
            ) else listOf(
                Color(0xFF0C1835), Color(0xFF1C1642), Color(0xFF092B36),
            )
        )
    }
    val centralGlow = remember(kind) {
        if (kind == ReminderCheckInKind.ALARM)
            Color(0xFFFF5988) else Color(0xFF4D8BFF)
    }
    val rimGlow = remember(kind) {
        if (kind == ReminderCheckInKind.ALARM)
            Color(0xFFFFB43B) else Color(0xFF38EBE4)
    }
    val revealProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = if (motionEnabled) 560 else 0),
        label = "v218-wave-controls-reveal",
    )

    // One lifecycle-bound compositor. Cap work at about 30fps, or 20fps under
    // power saver. No timer on the reminder engine, no frame allocations.
    val elapsedSeconds by produceState(
        initialValue = if (motionEnabled) 0.0 else 2.5,
        key1 = motionEnabled,
        key2 = lowPower,
    ) {
        if (motionEnabled) {
            var startedAt = 0L
            var lastPresented = 0L
            val intervalNs = if (lowPower) 50_000_000L else 33_333_333L
            while (true) {
                withFrameNanos { frame ->
                    if (startedAt == 0L) startedAt = frame
                    if (lastPresented == 0L || frame - lastPresented >= intervalNs) {
                        value = (frame - startedAt).toDouble() / 1_000_000_000.0
                        lastPresented = frame
                    }
                }
            }
        } else {
            value = 2.5
        }
    }
    val interactions = remember { MutableInteractionSource() }
    val touch = if (revealed) Modifier else Modifier
        .semantics { contentDescription = "Show reminder controls" }
        .clickable(interactionSource = interactions, indication = null, onClick = onReveal)

    Canvas(modifier = modifier.fillMaxSize().testTag("v1952-ambient-particles").then(touch)) {
        drawRect(brush = atmosphere)

        val center = Offset(size.width * .5f, size.height * .42f)
        // Two fixed-cost radial blooms behind the wave; never blur every dot.
        // Lower-power devices skip these extra draws entirely.
        if (!lowPower) {
            val bloomRadius = min(size.width * .67f, size.height * .43f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(centralGlow.copy(alpha = .24f), centralGlow.copy(alpha = 0f)),
                    center = center,
                    radius = bloomRadius,
                ),
                center = center,
                radius = bloomRadius,
            )
            val rimCenter = Offset(size.width * .69f, size.height * .29f)
            val rimRadius = min(size.width * .43f, size.height * .29f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(rimGlow.copy(alpha = .16f), rimGlow.copy(alpha = 0f)),
                    center = rimCenter,
                    radius = rimRadius,
                ),
                center = rimCenter,
                radius = rimRadius,
            )
        }
        val glyphRadius = when (glyph) {
            V195ReminderGlyph.CLOCK -> min(size.width * .395f, size.height * .30f)
            V195ReminderGlyph.MICROPHONE -> min(size.width * .48f, size.height * .31f)
        }
        val attraction = V218UnifiedWavePolicy.glyphBlend(elapsedSeconds, revealProgress)
        // V218 dimmed particles to 40%, then darkened the entire scene again
        // by 26% after reveal. That made the rich pigment look nearly black.
        // Keep about 82% of particle brightness when controls are present.
        val scrim = 1f - .18f * revealProgress
        val small = 1.32.dp.toPx()
        val mid = 1.68.dp.toPx()
        val large = 2.12.dp.toPx()
        val slowTime = if (motionEnabled) elapsedSeconds else 0.0

        // The ambient particles that form the icon are spread deterministically
        // across the ENTIRE screen rather than pulled from a single row.
        val stride = if (lowPower) 2 else 3
        val slots = (ambient.size + stride - 1) / stride

        ambient.forEachIndexed { index, particle ->
            val baseX = V202InfiniteBounce.position(particle.x, particle.velocityX, slowTime)
            val baseY = V202InfiniteBounce.position(particle.y, particle.velocityY, slowTime)
            val wavePhase = particle.colorPhase
            val flowX = V218UnifiedWavePolicy.wave(slowTime, baseY, wavePhase) * size.width * .017f
            val flowY = V218UnifiedWavePolicy.wave(slowTime, baseX, wavePhase + 1.8f) * size.height * .012f
            val ambientX = baseX * size.width + flowX
            val ambientY = baseY * size.height + flowY

            val ordinal = if (index % stride == 0) (index / stride * 137) % slots else -1
            val assigned = ordinal in glyphDots.indices
            val x: Float
            val y: Float
            if (assigned && attraction > 0f) {
                val target = glyphDots[ordinal]
                val (driftX, driftY) = if (motionEnabled)
                    V218UnifiedWavePolicy.iconDrift(slowTime, target) else 0f to 0f
                val targetX = center.x + (target.x + driftX) * glyphRadius
                val targetY = center.y + (target.y + driftY) * glyphRadius
                x = ambientX + (targetX - ambientX) * attraction
                y = ambientY + (targetY - ambientY) * attraction
            } else {
                x = ambientX
                y = ambientY
            }

            val blend = if (motionEnabled)
                V202InfiniteBounce.colorBlend(slowTime, particle.colorPhase, particle.colorRate)
                else .5f
            val pigment = lerp(
                palette[particle.colorIndex], palette[(particle.colorIndex + 1) % palette.size], blend,
            )
            val radius = when (particle.depth) {
                0 -> small
                1 -> mid
                else -> large
            }
            val intensity = if (assigned) .89f + .11f * attraction else .87f
            val position = Offset(x, y)

            // Selectively halo ~1/17 particles; preserve low-power draw budget.
            // The pigment itself still forms the silhouette, never a separate icon.
            if (!lowPower && index % 17 == 0) {
                drawCircle(
                    pigment.copy(alpha = .17f * scrim),
                    radius = radius * 3.4f, center = position,
                )
            }
            drawCircle(
                pigment.copy(alpha = (intensity * scrim).coerceIn(0f, 1f)),
                radius = radius, center = position,
            )
        }

        // Text and action buttons maintain contrast after manual reveal. Icon
        // attraction releases automatically so the action labels aren't covered.
        if (revealProgress > 0f) {
            drawRect(Color.Black.copy(alpha = .12f * revealProgress))
        }
    }
}
