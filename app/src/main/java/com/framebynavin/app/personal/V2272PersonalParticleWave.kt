package com.framebynavin.app.personal

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.os.PowerManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.framebynavin.app.reminders.V195ParticleGeometry
import com.framebynavin.app.reminders.V195ReminderGlyph
import com.framebynavin.app.reminders.V202InfiniteBounce
import com.framebynavin.app.reminders.V202PremiumParticleGeometry
import com.framebynavin.app.reminders.V218UnifiedWavePolicy
import kotlin.math.min

/**
 * V227.2: Personal particle visuals share the Creator wave's natural, colored
 * particle-to-glyph trajectories. Decorative only: no notification, alarm,
 * speech, acknowledge, dismiss or snooze behavior is affected.
 */
internal object V2272PersonalWavePolicy {
    const val SAMPLE_EVERY = 3
    const val GLYPH_EVERY = 3
    const val FRAME_INTERVAL_NORMAL_NS = 33_333_333L
    const val FRAME_INTERVAL_ECO_NS = 50_000_000L

    fun icon(alert: PersonalAlert): V195ReminderGlyph =
        if (alert == PersonalAlert.VOICE) V195ReminderGlyph.MICROPHONE
        else V195ReminderGlyph.CLOCK

    fun sampledCount(lowPower: Boolean): Int =
        (if (lowPower) V202PremiumParticleGeometry.LOW_POWER_COUNT
        else V202PremiumParticleGeometry.NORMAL_COUNT).let { (it + SAMPLE_EVERY - 1) / SAMPLE_EVERY }

    fun glyphCount(lowPower: Boolean): Int =
        (sampledCount(lowPower) + GLYPH_EVERY - 1) / GLYPH_EVERY

    fun frameIntervalNs(lowPower: Boolean): Long =
        if (lowPower) FRAME_INTERVAL_ECO_NS else FRAME_INTERVAL_NORMAL_NS

    fun elapsedWhenMotionDisabled(): Double = 2.5 // the glyph appears, but does not flicker
}

@Composable
internal fun V2272PersonalParticleWave(
    alert: PersonalAlert,
    modifier: Modifier = Modifier,
    motionEnabledOverride: Boolean? = null,
    lowPowerOverride: Boolean? = null,
) {
    val context = LocalContext.current
    val motionEnabled = motionEnabledOverride ?: remember {
        ValueAnimator.areAnimatorsEnabled()
    }
    val lowPower = lowPowerOverride ?: remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true ||
            context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val glyph = V2272PersonalWavePolicy.icon(alert)
    // Native Creator geometry is generated only once; sample it deterministically
    // rather than rerandomizing dots during each frame. No separate icon overlay.
    val ambient = remember(lowPower) {
        V202PremiumParticleGeometry.build(lowPower)
            .filterIndexed { index, _ -> index % V2272PersonalWavePolicy.SAMPLE_EVERY == 0 }
    }
    val glyphDots = remember(lowPower, glyph) {
        V195ParticleGeometry.build(glyph, V2272PersonalWavePolicy.glyphCount(lowPower))
    }
    val palette = remember(alert) {
        if (alert == PersonalAlert.ALARM) listOf(
            Color(0xFFFFE16B), Color(0xFFFFB437), Color(0xFFFF6B65),
            Color(0xFFFF497B), Color(0xFFFF35BD), Color(0xFFDA52FF),
            Color(0xFF9460FF), Color(0xFFFF8660),
        ) else listOf(
            Color(0xFF4CF5FF), Color(0xFF319EFF), Color(0xFF6475FF),
            Color(0xFFAB69FF), Color(0xFFE65DFF), Color(0xFFFF52C6),
            Color(0xFF28F5D9), Color(0xFF6DC7FF),
        )
    }
    val background = remember(alert) {
        Brush.verticalGradient(
            if (alert == PersonalAlert.ALARM) listOf(
                Color(0xFF211326), Color(0xFF19152D), Color(0xFF0B1930),
            ) else listOf(
                Color(0xFF0C1834), Color(0xFF171439), Color(0xFF092B33),
            ),
        )
    }
    val glow = remember(alert) {
        if (alert == PersonalAlert.ALARM) Color(0xFFFF6D83)
        else Color(0xFF5D8EFF)
    }
    val time by produceState(
        initialValue = if (motionEnabled) 0.0 else V2272PersonalWavePolicy.elapsedWhenMotionDisabled(),
        key1 = motionEnabled, key2 = lowPower,
    ) {
        if (!motionEnabled) {
            value = V2272PersonalWavePolicy.elapsedWhenMotionDisabled()
        } else {
            var started = 0L
            var last = 0L
            while (true) {
                withFrameNanos { frame ->
                    if (started == 0L) started = frame
                    if (last == 0L || frame - last >= V2272PersonalWavePolicy.frameIntervalNs(lowPower)) {
                        value = (frame - started).toDouble() / 1_000_000_000.0
                        last = frame
                    }
                }
            }
        }
    }
    Canvas(modifier.fillMaxWidth().height(252.dp).testTag("v2233-particle-wave")) {
        drawRect(brush = background)
        val center = Offset(size.width * .5f, size.height * .5f)
        val radius = min(size.width * .43f, size.height * .43f)
        if (!lowPower) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(glow.copy(alpha = .22f), Color.Transparent),
                    center = center, radius = radius * 1.9f,
                ), center = center, radius = radius * 1.9f,
            )
        }
        val formation = V218UnifiedWavePolicy.glyphBlend(time, 0f)
        val t = if (motionEnabled) time else 0.0
        val stride = V2272PersonalWavePolicy.GLYPH_EVERY
        val slots = V2272PersonalWavePolicy.glyphCount(lowPower)
        val small = 1.3.dp.toPx()
        val medium = 1.65.dp.toPx()
        val large = 2.0.dp.toPx()
        ambient.forEachIndexed { index, particle ->
            val baseX = V202InfiniteBounce.position(particle.x, particle.velocityX, t)
            val baseY = V202InfiniteBounce.position(particle.y, particle.velocityY, t)
            val waveX = V218UnifiedWavePolicy.wave(t, baseY, particle.colorPhase) * size.width * .014f
            val waveY = V218UnifiedWavePolicy.wave(t, baseX, particle.colorPhase + 1.8f) * size.height * .011f
            val originX = baseX * size.width + waveX
            val originY = baseY * size.height + waveY
            val ordinal = if (index % stride == 0) (index / stride * 137) % slots else -1
            val target = if (ordinal >= 0) glyphDots[ordinal] else null
            val x: Float
            val y: Float
            if (target != null && formation > 0f) {
                val drift = if (motionEnabled) V218UnifiedWavePolicy.iconDrift(t, target)
                    else 0f to 0f
                x = originX + (center.x + (target.x + drift.first) * radius - originX) * formation
                y = originY + (center.y + (target.y + drift.second) * radius - originY) * formation
            } else {
                x = originX
                y = originY
            }
            val blend = if (motionEnabled) V202InfiniteBounce.colorBlend(
                t, particle.colorPhase, particle.colorRate,
            ) else .5f
            val pigment = lerp(
                palette[particle.colorIndex],
                palette[(particle.colorIndex + 1) % palette.size],
                blend,
            )
            val dot = when (particle.depth) {
                0 -> small
                1 -> medium
                else -> large
            }
            val at = Offset(x, y)
            if (!lowPower && index % 23 == 0) {
                drawCircle(pigment.copy(alpha = .16f), radius = dot * 3f, center = at)
            }
            drawCircle(
                pigment.copy(alpha = if (target != null) .97f else .82f),
                radius = dot, center = at,
            )
        }
    }
}
