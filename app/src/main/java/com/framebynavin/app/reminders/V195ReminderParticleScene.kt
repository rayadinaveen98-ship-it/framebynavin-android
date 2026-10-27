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
            )
        )
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
        val glyphRadius = when (glyph) {
            V195ReminderGlyph.CLOCK -> min(size.width * .395f, size.height * .30f)
            V195ReminderGlyph.MICROPHONE -> min(size.width * .48f, size.height * .31f)
        }
        val attraction = V218UnifiedWavePolicy.glyphBlend(elapsedSeconds, revealProgress)
        val scrim = 1f - .60f * revealProgress
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
            val intensity = if (assigned) .80f + .18f * attraction else .77f
            val position = Offset(x, y)

            // Only occasional glow; many full-radius blurs at 2,184 particles
            // are expensive on entry-level Android hardware.
            if (!lowPower && index % 13 == 0) {
                drawCircle(
                    pigment.copy(alpha = .070f * scrim),
                    radius = radius * 2.9f, center = position,
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
            drawRect(Color.Black.copy(alpha = .26f * revealProgress))
        }
    }
}
