package com.framebynavin.app.reminders

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * P10B VISUAL SOURCE STAGE, NOT CONNECTED TO REMINDER DELIVERY YET.
 *
 * One living atmosphere reused between entrance and main act. All gestures,
 * alarm/notification ownership, recording, playback and actions live elsewhere.
 * No old V336, V391 or V394 scene dependency and no fake audio amplitude.
 */
internal data class P10ScenePigments(
    val dark: Color,
    val depth: Color,
    val light: Color,
    val secondary: Color,
    val warm: Color,
)

internal fun P10ReminderFamily.pigments(): P10ScenePigments = when (this) {
    P10ReminderFamily.LIVING_BLOOM -> P10ScenePigments(
        dark = Color(0xFF0B0912), depth = Color(0xFF31182F),
        light = Color(0xFFFF93B7), secondary = Color(0xFFCE94FA),
        warm = Color(0xFFFFD0AF),
    )
    P10ReminderFamily.PULSE_STUDIO -> P10ScenePigments(
        dark = Color(0xFF080D16), depth = Color(0xFF14243D),
        light = Color(0xFF6BD0F5), secondary = Color(0xFFB2A4FF),
        warm = Color(0xFFFFCE9B),
    )
    P10ReminderFamily.RISE_CORE -> P10ScenePigments(
        dark = Color(0xFF100D14), depth = Color(0xFF322132),
        light = Color(0xFFFFC48A), secondary = Color(0xFFFF8C99),
        warm = Color(0xFFFFE4BC),
    )
}

/** Bounded frame clock. When animator motion is off, time is exactly zero. */
@Composable
internal fun P10SceneTime(tuning: P10MotionTuning): Float {
    val time by produceState(
        initialValue = 0f,
        key1 = tuning.animated,
        key2 = tuning.frameIntervalNanos,
    ) {
        if (!tuning.animated) {
            value = 0f
        } else {
            var start = 0L
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    if (start == 0L) start = now
                    if (last == 0L || now - last >= tuning.frameIntervalNanos) {
                        value = ((now - start) / 1_000_000_000.0).toFloat()
                        last = now
                    }
                }
            }
        }
    }
    return time
}

/**
 * Decorative full-screen renderer. No own tap handlers, lifecycle services,
 * media decoder or saved-state writes. The main act retains subtle motion.
 */
@Composable
internal fun P10LivingReminderBackdrop(
    family: P10ReminderFamily,
    act: P10ReminderAct,
    animationsAllowed: Boolean,
    batterySaver: Boolean,
    lowRam: Boolean,
    modifier: Modifier = Modifier,
) {
    val tuning = P10ReminderVisualPolicy.tuning(animationsAllowed, batterySaver, lowRam, act)
    val t = P10SceneTime(tuning)
    val pigments = family.pigments()
    Canvas(modifier.fillMaxSize().testTag("p10-living-backdrop-" + family.name.lowercase())) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(pigments.dark, pigments.depth, pigments.dark),
            ),
        )
        val focus = Offset(
            x = size.width * (.49f + .10f * tuning.amplitude * sin(t * .59f)),
            y = size.height * (.42f + .075f * tuning.amplitude * cos(t * .46f)),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(pigments.light.copy(alpha = .27f), pigments.dark.copy(alpha = 0f)),
                center = focus,
                radius = size.width * .76f,
            ),
            center = focus,
            radius = size.width * .76f,
        )
        when (family) {
            P10ReminderFamily.LIVING_BLOOM -> drawP10Bloom(t, tuning, pigments, focus)
            P10ReminderFamily.PULSE_STUDIO -> drawP10Studio(t, tuning, pigments, focus)
            P10ReminderFamily.RISE_CORE -> drawP10Rise(t, tuning, pigments, focus)
        }
        drawP10DepthMotes(t, tuning, pigments)
        if (act == P10ReminderAct.MAIN) {
            // Calm reading zone while leaving peripheral motion visible.
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        pigments.dark.copy(alpha = .27f),
                        pigments.dark.copy(alpha = .36f),
                        pigments.dark.copy(alpha = .70f),
                    ),
                ),
            )
        }
    }
}

/** Organic multiple ribbons: independent curvature/offset rather than one pulse loop. */
private fun DrawScope.drawP10Bloom(
    t: Float, tuning: P10MotionTuning, p: P10ScenePigments, focus: Offset,
) {
    val scale = min(size.width, size.height)
    repeat(tuning.depthLayers + 1) { layer ->
        val phase = t * (.63f + layer * .09f) + layer * 1.41f
        val radial = scale * (.18f + .058f * layer + .025f * tuning.amplitude * sin(phase))
        val sweepX = radial * (.76f + .13f * cos(phase * .72f))
        val sweepY = radial * (.59f + .14f * sin(phase * .81f))
        val ribbon = Path().apply {
            moveTo(focus.x - sweepX, focus.y + sweepY * .35f)
            cubicTo(
                focus.x - sweepX * .46f, focus.y - sweepY * 1.65f,
                focus.x + sweepX * .71f, focus.y - sweepY * 1.24f,
                focus.x + sweepX, focus.y - sweepY * .14f,
            )
            cubicTo(
                focus.x + sweepX * .68f, focus.y + sweepY * 1.53f,
                focus.x - sweepX * .62f, focus.y + sweepY * 1.32f,
                focus.x - sweepX, focus.y + sweepY * .35f,
            )
        }
        val c = if (layer % 2 == 0) p.light else p.secondary
        drawPath(ribbon, c.copy(alpha = .075f), style = Stroke(width = scale * .045f))
        drawPath(ribbon, c.copy(alpha = .38f), style = Stroke(width = scale * .005f, cap = StrokeCap.Round))
    }
    drawCircle(
        brush = Brush.radialGradient(
            listOf(p.warm.copy(alpha = .53f), p.light.copy(alpha = .10f), Color.Transparent),
            center = focus,
            radius = scale * (.16f + .045f * tuning.amplitude * sin(t * 1.33f)),
        ),
        center = focus,
        radius = scale * .22f,
    )
}

/** Asymmetric energized arcs and moving frequency paths. No microphone sampling. */
private fun DrawScope.drawP10Studio(
    t: Float, tuning: P10MotionTuning, p: P10ScenePigments, focus: Offset,
) {
    val scale = min(size.width, size.height)
    repeat(tuning.depthLayers + 2) { layer ->
        val radius = scale * (.12f + .071f * layer)
        val shift = tuning.amplitude * 16f * sin(t * (.62f + layer * .12f) + layer)
        val bounds = Size(radius * 2f, radius * 2f)
        drawArc(
            color = if (layer % 2 == 0) p.light.copy(alpha = .42f)
                else p.secondary.copy(alpha = .37f),
            startAngle = 205f + shift + layer * 13f,
            sweepAngle = 220f - layer * 12f,
            useCenter = false,
            topLeft = Offset(focus.x - radius, focus.y - radius),
            size = bounds,
            style = Stroke(width = (scale * .005f).coerceAtLeast(1.5f), cap = StrokeCap.Round),
        )
    }
    repeat(3) { lane ->
        val y = focus.y + (lane - 1) * scale * .072f
        val amplitude = scale * (.035f + lane * .006f)
        val path = Path().apply {
            moveTo(-size.width * .09f, y)
            cubicTo(
                size.width * .16f, y - amplitude * sin(t * 1.05f + lane),
                size.width * .33f, y + amplitude * cos(t * .82f + lane * 1.8f),
                size.width * .56f, y,
            )
            cubicTo(
                size.width * .74f, y - amplitude,
                size.width * .88f, y + amplitude * sin(t + lane),
                size.width * 1.08f, y,
            )
        }
        drawPath(path, p.warm.copy(alpha = .12f + lane * .03f),
            style = Stroke(width = scale * .004f, cap = StrokeCap.Round))
    }
}

/** Distinct upward ribbons and floating light spine, not historical Sunrise preset. */
private fun DrawScope.drawP10Rise(
    t: Float, tuning: P10MotionTuning, p: P10ScenePigments, focus: Offset,
) {
    val scale = min(size.width, size.height)
    repeat(tuning.depthLayers + 1) { layer ->
        val x = focus.x + (layer - tuning.depthLayers / 2f) * scale * .083f
        val drift = tuning.amplitude * scale * .045f * sin(t * (.56f + layer * .12f) + layer)
        val ribbon = Path().apply {
            moveTo(x - scale * .045f, size.height * 1.06f)
            cubicTo(
                x + drift - scale * .18f, size.height * .77f,
                x - drift + scale * .12f, size.height * .39f,
                x + drift, -size.height * .07f,
            )
        }
        val color = if (layer % 2 == 0) p.light else p.secondary
        drawPath(ribbon, color.copy(alpha = .085f), style = Stroke(width = scale * .06f))
        drawPath(ribbon, color.copy(alpha = .33f), style = Stroke(
            width = scale * .0045f, cap = StrokeCap.Round))
    }
    val coreOffset = focus.copy(y = focus.y + tuning.amplitude * scale * .05f * sin(t * .9f))
    drawCircle(
        Brush.radialGradient(
            listOf(p.warm.copy(alpha = .46f), p.light.copy(alpha = .18f), Color.Transparent),
            coreOffset, scale * .22f,
        ),
        radius = scale * .22f, center = coreOffset,
    )
}

private fun DrawScope.drawP10DepthMotes(t: Float, tuning: P10MotionTuning, p: P10ScenePigments) {
    for (i in 0 until tuning.activeMotes) {
        val seed = i * 1.618f
        val x = ((.17f + i * .131f) % 1f) * size.width +
            sin(t * (.36f + (i % 5) * .08f) + seed) * size.width * .025f * tuning.amplitude
        val y = ((.08f + i * .233f) % 1f) * size.height -
            sin(t * (.31f + (i % 3) * .15f) + seed) * size.height * .063f * tuning.amplitude
        val color = if (i % 3 == 0) p.warm else p.secondary
        val r = (1.4f + (i % 4) * .62f) * density
        drawCircle(color.copy(alpha = .13f), radius = r * 3.6f, center = Offset(x, y))
        drawCircle(color.copy(alpha = .54f), radius = r, center = Offset(x, y))
    }
}

/**
 * Shared live icon entrance point for all three reminder domains.
 * This is presentation only; the selected visual cannot affect delivery.
 * Preview callers may specify a style and disable animation.
 */
@Composable
internal fun P10ReminderIdentityEmblem(
    kind: ReminderCheckInKind,
    family: P10ReminderFamily,
    modifier: Modifier = Modifier,
    style: P10ReminderIconStyle? = null,
    animate: Boolean = true,
) {
    val context = LocalContext.current
    P10PremiumReminderIcon(
        kind = kind,
        family = family,
        style = style ?: P10ReminderIconStyleStore.selected(context),
        modifier = modifier.testTag("p10-identity-" + kind.name.lowercase()),
        animated = animate,
    )
}
