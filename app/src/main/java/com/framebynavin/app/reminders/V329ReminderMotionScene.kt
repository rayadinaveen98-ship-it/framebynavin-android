package com.framebynavin.app.reminders

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * V329 presentation-only Canvas, inspired by the five approved Motion Lab V2
 * directions. This NEVER owns the reminder buttons or reminder service lifecycle.
 *
 * A standard tap energizes the current world; a double-tap reveals the existing
 * action screen. TalkBack/keyboard activation is an accessible single action.
 */
@Composable
internal fun V329ReminderMotionScene(
    channel: V329ReminderChannel,
    appearance: V329ReminderAppearance,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    motionOverride: Boolean? = null,
    lowPowerOverride: Boolean? = null,
) {
    val context = LocalContext.current
    val motion = motionOverride ?: remember { ValueAnimator.areAnimatorsEnabled() }
    val lowPower = lowPowerOverride ?: remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true ||
            context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val safe = appearance.normalized()
    val glyph = if (channel == V329ReminderChannel.ALARM)
        V195ReminderGlyph.CLOCK else V195ReminderGlyph.MICROPHONE
    val particleCount = (if (lowPower) 110 else 190) +
        (safe.density * (if (lowPower) 80 else 190)).roundToInt()
    val dots = remember(glyph, particleCount) {
        V195ParticleGeometry.build(glyph, particleCount)
    }
    val t by produceState(
        initialValue = if (motion) 0.0 else 3.5,
        key1 = motion, key2 = lowPower, key3 = safe.speed,
    ) {
        if (!motion) {
            value = 3.5
        } else {
            var start = 0L
            var last = 0L
            val interval = if (lowPower) 50_000_000L else 33_333_333L
            while (true) {
                withFrameNanos { frame ->
                    if (start == 0L) start = frame
                    if (last == 0L || frame - last >= interval) {
                        value = (frame - start).toDouble() / 1_000_000_000.0 *
                            (.35 + safe.speed * 1.25)
                        last = frame
                    }
                }
            }
        }
    }
    var taps by remember { mutableIntStateOf(0) }
    val impulse = remember { Animatable(0f) }
    LaunchedEffect(taps) {
        if (taps > 0 && motion) {
            impulse.snapTo(1f)
            impulse.animateTo(0f, tween(900))
        }
    }
    val touch = if (interactive) {
        Modifier.pointerInput(onReveal) {
            detectTapGestures(
                onTap = { taps += 1 },
                onDoubleTap = { onReveal() },
            )
        }.semantics {
            onClick(label = "Open reminder controls") {
                onReveal()
                true
            }
        }
    } else Modifier
    val palette = remember(safe.world) { V329Palette.forWorld(safe.world) }
    val elapsed = t.toFloat()
    val tapLight = impulse.value

    Box(modifier.fillMaxSize().testTag("v329-motion-entry")) {
        Canvas(Modifier.fillMaxSize().then(touch).testTag("v329-motion-canvas")) {
            val w = size.width
            val h = size.height
            val short = min(w, h)
            val core = Offset(w * .5f, h * .43f)
            val radius = min(w * .36f, h * .24f)
            val strength = safe.intensity

            drawRect(brush = Brush.verticalGradient(
                listOf(palette.top, palette.middle, palette.bottom),
            ))
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(palette.glow.copy(alpha = (.27f + .23f * strength)),
                        palette.glow.copy(alpha = .06f), Color.Transparent),
                    center = core,
                    radius = short * .9f,
                ),
                radius = short * .9f, center = core,
            )

            // Distinct scene geometry; all lighting stays behind the real
            // particle-built mic / alarm silhouette, never a flat icon overlay.
            when (safe.world) {
                V329ReminderWorld.CRIMSON -> {
                    repeat(if (lowPower) 2 else 5) { i ->
                        val r = radius * (1.19f + i * .25f)
                        val phase = (elapsed * (9f + i * 2f) + i * 46f)
                        drawArc(
                            palette.line.copy(alpha = .12f + .08f * strength),
                            startAngle = phase, sweepAngle = 90f + i * 14f,
                            useCenter = false,
                            topLeft = Offset(core.x - r, core.y - r),
                            size = Size(r * 2f, r * 2f),
                            style = Stroke(width = (1.2f + i * .55f) * density),
                        )
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(palette.bright.copy(alpha = .20f * strength),
                                Color.Transparent),
                            center = core,
                            radius = radius * .66f,
                        ),
                        radius = radius * .66f, center = core,
                    )
                }
                V329ReminderWorld.LIQUID -> {
                    repeat(if (lowPower) 2 else 5) { i ->
                        val sway = sin(elapsed * .45f + i * 1.3f) * short * .095f
                        val y = h * (.24f + i * .13f) + sway
                        val curve = Path().apply {
                            moveTo(-w * .2f, y + h * .055f)
                            cubicTo(w * .20f, y - h * .16f,
                                w * .66f, y + h * .18f,
                                w * 1.20f, y - h * .10f)
                        }
                        drawPath(
                            curve,
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent,
                                    palette.glow.copy(alpha = .08f * strength),
                                    palette.line.copy(alpha = .30f * strength),
                                    Color.Transparent),
                            ),
                            style = Stroke(width = short * (.035f + i * .012f),
                                cap = StrokeCap.Round),
                        )
                        drawPath(
                            curve, palette.bright.copy(alpha = .20f * strength),
                            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
                V329ReminderWorld.OBSIDIAN -> {
                    repeat(if (lowPower) 10 else 25) { i ->
                        val angle = i * 2.39996f + elapsed * (.021f + (i % 3) * .012f)
                        val reach = radius * (1.38f + (i % 7) * .13f)
                        val x = core.x + cos(angle) * reach
                        val y = core.y + sin(angle) * reach * 1.30f
                        val s = short * (.013f + (i % 5) * .006f)
                        val shard = Path().apply {
                            moveTo(x - s, y + s)
                            lineTo(x + s * .17f, y - s * 1.95f)
                            lineTo(x + s * 1.25f, y + s * .45f)
                            close()
                        }
                        drawPath(shard, palette.glow.copy(alpha = .18f + .22f * strength))
                        drawLine(palette.bright.copy(alpha = .42f * strength),
                            Offset(x + s * .17f, y - s * 1.95f),
                            Offset(x + s * 1.25f, y + s * .45f),
                            strokeWidth = .8.dp.toPx())
                    }
                }
                V329ReminderWorld.SIGNAL -> {
                    val unit = short / 10f
                    repeat(13) { i ->
                        val x = (i - 2) * unit + (elapsed * 2.7f) % unit
                        drawLine(palette.line.copy(alpha = .09f * strength),
                            Offset(x, 0f), Offset(x, h), strokeWidth = .8.dp.toPx())
                    }
                    repeat(23) { i ->
                        val y = i * unit
                        drawLine(palette.line.copy(alpha = .08f * strength),
                            Offset(0f, y), Offset(w, y), strokeWidth = .7.dp.toPx())
                    }
                    val scanY = (elapsed * 24f) % (h + short * .3f) - short * .15f
                    drawLine(palette.bright.copy(alpha = .22f * strength),
                        Offset(0f, scanY), Offset(w, scanY),
                        strokeWidth = 1.dp.toPx())
                    repeat(3) { i ->
                        val r = radius * (1.16f + i * .28f)
                        drawCircle(palette.line.copy(alpha = .20f * strength),
                            radius = r, center = core, style = Stroke(1.dp.toPx()))
                    }
                }
                V329ReminderWorld.NOCTURNE -> {
                    repeat(if (lowPower) 20 else 56) { i ->
                        val px = ((i * 137) % 97) / 97f * w
                        val py = ((i * 59 + 31) % 101) / 101f * h
                        val drift = if (motion) sin(elapsed * .19f + i) * 4.dp.toPx() else 0f
                        val point = Offset(px + drift, py)
                        val tint = if (i % 4 == 0) palette.bright else palette.line
                        drawCircle(tint.copy(alpha = .23f + .23f * strength),
                            radius = (if (i % 9 == 0) 1.65f else .8f).dp.toPx(),
                            center = point)
                        if (i % 7 == 0) {
                            val nx = (((i + 3) * 137) % 97) / 97f * w
                            val ny = (((i + 3) * 59 + 31) % 101) / 101f * h
                            drawLine(palette.line.copy(alpha = .11f * strength),
                                point, Offset(nx, ny), strokeWidth = .65.dp.toPx())
                        }
                    }
                }
            }

            // The icon is *formed by the moving particles themselves*.
            dots.forEachIndexed { i, dot ->
                val phase = i * .618f
                val swirl = elapsed * (.50f + safe.speed * .26f)
                val originX = w * (.5f + dot.scatterX * .34f)
                val originY = h * (.43f + dot.scatterY * .30f)
                val formation = ((elapsed - .45f - (i % 9) * .067f) / 2.35f)
                    .coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
                val ambientDx: Float
                val ambientDy: Float
                when (safe.world) {
                    V329ReminderWorld.CRIMSON -> {
                        ambientDx = cos(swirl + phase) * radius * .16f
                        ambientDy = sin(swirl + phase) * radius * .16f
                    }
                    V329ReminderWorld.LIQUID -> {
                        ambientDx = sin(swirl * .65f + phase) * radius * .22f
                        ambientDy = sin(swirl * .48f + phase * .6f) * radius * .13f
                    }
                    V329ReminderWorld.OBSIDIAN -> {
                        ambientDx = sin(swirl * .35f + phase) * radius * .07f
                        ambientDy = cos(swirl * .32f + phase) * radius * .07f
                    }
                    V329ReminderWorld.SIGNAL -> {
                        ambientDx = sin(swirl * .85f + phase) * radius * .048f
                        ambientDy = cos(swirl * .85f + phase) * radius * .048f
                    }
                    V329ReminderWorld.NOCTURNE -> {
                        ambientDx = sin(swirl * .22f + phase) * radius * .10f
                        ambientDy = cos(swirl * .24f + phase) * radius * .10f
                    }
                }
                val x = originX + ambientDx +
                    (core.x + dot.x * radius - originX - ambientDx) * formation
                val y = originY + ambientDy +
                    (core.y + dot.y * radius - originY - ambientDy) * formation
                val color = when (i % 6) {
                    0 -> palette.bright
                    1, 4 -> palette.line
                    2 -> palette.glow
                    else -> palette.particle
                }
                val scale = if (i % 13 == 0) 1.65f else if (i % 3 == 0) 1.1f else .80f
                val particleSize = scale.dp.toPx()
                if (!lowPower && i % 20 == 0) {
                    drawCircle(color.copy(alpha = .11f * strength),
                        radius = particleSize * 5f, center = Offset(x, y))
                }
                drawCircle(color.copy(alpha = (.43f + formation * .50f) * strength),
                    radius = particleSize, center = Offset(x, y))
            }
            if (tapLight > .01f) {
                val r = radius * (1.05f + (1f - tapLight) * .95f)
                drawCircle(
                    palette.bright.copy(alpha = tapLight * .62f * strength),
                    radius = r, center = core,
                    style = Stroke(width = (1f + tapLight * 4f).dp.toPx()),
                )
            }
        }
        if (interactive) {
            Text(
                text = "DOUBLE TAP TO OPEN   ·   TAP TO EXPLORE",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 32.dp),
                color = palette.particle.copy(alpha = .70f),
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.15.sp, textAlign = TextAlign.Center,
            )
        }
    }
}

private data class V329Palette(
    val top: Color, val middle: Color, val bottom: Color,
    val glow: Color, val line: Color, val bright: Color, val particle: Color,
) {
    companion object {
        fun forWorld(world: V329ReminderWorld) = when (world) {
            V329ReminderWorld.CRIMSON -> V329Palette(
                Color(0xFF070407), Color(0xFF1D0712), Color(0xFF070409),
                Color(0xFF9D0E35), Color(0xFFE72D4C),
                Color(0xFFFFB9A2), Color(0xFFFF5868))
            V329ReminderWorld.LIQUID -> V329Palette(
                Color(0xFF09060A), Color(0xFF24101D), Color(0xFF0C0810),
                Color(0xFF96294F), Color(0xFFE084A5),
                Color(0xFFFFE7DA), Color(0xFFF9A3C0))
            V329ReminderWorld.OBSIDIAN -> V329Palette(
                Color(0xFF050506), Color(0xFF1B070E), Color(0xFF060409),
                Color(0xFF91112A), Color(0xFFBE3650),
                Color(0xFFFFA6B3), Color(0xFFE74B66))
            V329ReminderWorld.SIGNAL -> V329Palette(
                Color(0xFF050A0D), Color(0xFF101620), Color(0xFF06080F),
                Color(0xFF7E1833), Color(0xFFC23D5D),
                Color(0xFFFF919E), Color(0xFFF17B89))
            V329ReminderWorld.NOCTURNE -> V329Palette(
                Color(0xFF090610), Color(0xFF190C21), Color(0xFF070912),
                Color(0xFF603354), Color(0xFFB96D94),
                Color(0xFFF6CEDA), Color(0xFFE3A1C0))
        }
    }
}
