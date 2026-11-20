package com.framebynavin.app.reminders

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * RX01-B: reusable visual ornament for a verified reminder, SOURCE-STAGED ONLY.
 *
 * It DOES NOT read a mic, acquire audio focus, run an alarm, receive taps,
 * navigate, mutate a reminder, show an Android window or acknowledge delivery.
 * RX01-C owns the double-tap interaction; RX01-D/E must supply a verified
 * owner-scoped mode from the real occurrence, never from the presence of music.
 */
internal enum class RX01ReminderIconKind {
    VOICE,
    ALARM,
    VOICE_AND_ALARM,
}

/**
 * This is deliberately strict: the combined symbol is only lawful if the
 * later source owner confirms that BOTH signals belong to this same occurrence.
 */
internal object RX01ReminderIconPolicy {
    fun creator(kind: ReminderCheckInKind): RX01ReminderIconKind = when (kind) {
        ReminderCheckInKind.VOICE -> RX01ReminderIconKind.VOICE
        ReminderCheckInKind.ALARM -> RX01ReminderIconKind.ALARM
    }

    fun verified(
        voice: Boolean,
        alarm: Boolean,
        combinedModeVerified: Boolean = false,
    ): RX01ReminderIconKind {
        require(voice || alarm) { "An actual reminder signal is required" }
        require(!(voice && alarm) || combinedModeVerified) {
            "The source owner has not confirmed a combined Voice/Alarm occurrence"
        }
        return when {
            voice && alarm -> RX01ReminderIconKind.VOICE_AND_ALARM
            voice -> RX01ReminderIconKind.VOICE
            else -> RX01ReminderIconKind.ALARM
        }
    }

    fun accessibilityLabel(mode: RX01ReminderIconKind): String = when (mode) {
        RX01ReminderIconKind.VOICE -> "Voice reminder"
        RX01ReminderIconKind.ALARM -> "Alarm reminder"
        RX01ReminderIconKind.VOICE_AND_ALARM -> "Voice and alarm reminder"
    }

    fun domainLabel(domain: RX01ReminderDomain): String = when (domain) {
        RX01ReminderDomain.CREATOR -> "CREATOR REMINDER"
        RX01ReminderDomain.PERSONAL -> "PERSONAL REMINDER"
        RX01ReminderDomain.CHALLENGE_21 -> "21-DAY CHALLENGE"
    }

    fun detailLabel(domain: RX01ReminderDomain): String = when (domain) {
        RX01ReminderDomain.CREATOR -> "Your next creative moment"
        RX01ReminderDomain.PERSONAL -> "A moment for yourself"
        RX01ReminderDomain.CHALLENGE_21 -> "Your commitment calls"
    }

    fun cycleMillis(domain: RX01ReminderDomain): Int = when (domain) {
        RX01ReminderDomain.CREATOR -> 6_000
        RX01ReminderDomain.PERSONAL -> 8_000
        RX01ReminderDomain.CHALLENGE_21 -> 7_000
    }
}

/**
 * Presentation ONLY. Its words are staged ahead of the RX01-C gesture gate.
 * No clickable or pointer-input modifier is attached, so this cannot change
 * the live reminder state before the later explicit integration stages.
 *
 * The background layer RX01FluidReminderBackdrop(ENTRANCE) will be composed
 * behind this, not inside it. It also does not display title, notes or a
 * companion; this keeps this reusable hero safe for a locked Personal screen.
 */
@Composable
internal fun RX01ReminderEntranceHero(
    domain: RX01ReminderDomain,
    mode: RX01ReminderIconKind,
    motionEnabled: Boolean,
    lowPower: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = RX01FluidTheme.palette(domain, RX01SurfacePhase.ENTRANCE)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .testTag("rx01-entrance-hero-" + domain.name.lowercase()),
        contentAlignment = Alignment.Center,
    ) {
        val iconSize: Dp = when {
            maxHeight < 470.dp -> 130.dp
            maxHeight < 600.dp || maxWidth < 350.dp -> 168.dp
            else -> 218.dp
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.Center).padding(bottom = 54.dp),
        ) {
            Text(
                text = RX01ReminderIconPolicy.domainLabel(domain),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = palette.ink.copy(alpha = .90f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            RX01ReminderOpticIcon(
                domain = domain,
                mode = mode,
                motionEnabled = motionEnabled,
                lowPower = lowPower,
                diameter = iconSize,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = RX01ReminderIconPolicy.detailLabel(domain),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = palette.ink,
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "DOUBLE TAP TO OPEN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
                color = palette.ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Open your reminder",
                fontSize = 12.sp,
                color = palette.ink.copy(alpha = .76f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Original premium glass-metal optic, deliberately not an emoji or stock image.
 *
 * Strictly visual. Optical halos and moving arcs are decorative, NOT a mic
 * recording indicator, sound level, countdown, delivery receipt or 21-day
 * progress ring. The later accessible RX01-C gate will own focus/click.
 *
 * Animator disabled / power-save => stationary artwork with zero animation
 * state. This is a small native Canvas; it draws no off-screen bitmap or blur.
 */
@Composable
internal fun RX01ReminderOpticIcon(
    domain: RX01ReminderDomain,
    mode: RX01ReminderIconKind,
    motionEnabled: Boolean,
    lowPower: Boolean,
    diameter: Dp = 218.dp,
    modifier: Modifier = Modifier,
) {
    val palette = RX01FluidTheme.palette(domain, RX01SurfacePhase.ENTRANCE)
    val animate = motionEnabled && !lowPower
    val motionFraction = if (animate) {
        val infinite = rememberInfiniteTransition(label = "rx01-optical-breath")
        val fraction by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = RX01ReminderIconPolicy.cycleMillis(domain),
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "rx01-optical-phase",
        )
        fraction
    } else 0f

    Canvas(
        modifier = modifier
            .size(diameter)
            .semantics { contentDescription = RX01ReminderIconPolicy.accessibilityLabel(mode) }
            .testTag("rx01-optic-" + domain.name.lowercase() + "-" + mode.name.lowercase()),
    ) {
        val scale = min(size.width, size.height)
        if (scale <= 0f) return@Canvas
        val origin = center
        val orbit = scale * .37f
        val theta = ((2.0 * PI) * motionFraction.toDouble()).toFloat()
        val breath = (1f + .045f * sin(theta)).coerceIn(.95f, 1.05f)
        val haloRadius = orbit * 1.27f

        // A large, diffused light field gives the icon apparent depth.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.iconHalo.copy(alpha = .31f),
                    palette.iconHalo.copy(alpha = .12f),
                    palette.iconHalo.copy(alpha = 0f),
                ),
                center = origin,
                radius = haloRadius * breath,
            ),
            radius = haloRadius * breath,
            center = origin,
        )

        // The central disc is always sufficiently dark to preserve the
        // silhouette against the vivid multicolour entrance backdrop.
        drawCircle(
            color = Color(0xF00C1024),
            radius = orbit * .85f,
            center = origin,
        )
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(
                    palette.iconHalo.copy(alpha = .38f),
                    Color(0xFF17162B).copy(alpha = .98f),
                    Color(0xFF080D1C).copy(alpha = .98f),
                ),
                start = Offset(origin.x - orbit, origin.y - orbit),
                end = Offset(origin.x + orbit, origin.y + orbit),
            ),
            radius = orbit * .80f,
            center = origin,
        )
        drawCircle(
            color = palette.iconTint.copy(alpha = .61f),
            radius = orbit * .80f,
            center = origin,
            style = Stroke(width = scale * .007f),
        )
        drawCircle(
            color = palette.iconHalo.copy(alpha = .22f),
            radius = orbit * .72f,
            center = origin,
            style = Stroke(width = scale * .004f),
        )

        val arcSwing = 8f * sin(theta)
        val brightness = (.66f + .10f * cos(theta)).coerceIn(0f, 1f)
        // Domain silhouettes deliberately differ without implying progress.
        when (domain) {
            RX01ReminderDomain.CREATOR -> {
                rx01OpticalArc(origin, orbit * 1.05f, -118f + arcSwing, 87f,
                    palette.highlight, scale * .016f, brightness)
                rx01OpticalArc(origin, orbit * 1.05f, 44f - arcSwing, 76f,
                    palette.iconHalo, scale * .010f, .48f)
                // Tiny studio optics: fixed perimeter glints, no waveform.
                rx01Glint(origin, orbit, -150f + arcSwing, palette.iconTint, scale)
                rx01Glint(origin, orbit, 35f - arcSwing, palette.highlight, scale)
            }
            RX01ReminderDomain.PERSONAL -> {
                rx01OpticalArc(origin, orbit * 1.05f, -167f + arcSwing * .5f, 126f,
                    palette.iconHalo, scale * .011f, .50f)
                rx01OpticalArc(origin, orbit * .93f, 48f - arcSwing * .5f, 108f,
                    palette.highlight, scale * .009f, .40f)
                rx01Glint(origin, orbit * .98f, -103f, palette.iconTint, scale)
                rx01Glint(origin, orbit * .98f, 87f, palette.iconHalo, scale)
            }
            RX01ReminderDomain.CHALLENGE_21 -> {
                // Three energy arcs, NOT 21 progress segments or daily streaks.
                listOf(-115f, 5f, 125f).forEachIndexed { index, angle ->
                    rx01OpticalArc(
                        origin, orbit * 1.035f, angle + arcSwing * .65f, 53f,
                        if (index == 1) palette.highlight else palette.iconHalo,
                        scale * .014f, .63f,
                    )
                }
                rx01Glint(origin, orbit * 1.07f, -35f - arcSwing, palette.iconTint, scale)
            }
        }

        // Same optical system with three clear, domain-independent mode glyphs.
        when (mode) {
            RX01ReminderIconKind.VOICE ->
                rx01DrawMicrophone(origin, orbit * .97f, palette)
            RX01ReminderIconKind.ALARM ->
                rx01DrawClock(origin, orbit * .92f, palette)
            RX01ReminderIconKind.VOICE_AND_ALARM -> {
                rx01DrawMicrophone(
                    Offset(origin.x - orbit * .36f, origin.y),
                    orbit * .61f,
                    palette,
                )
                drawLine(
                    color = palette.iconTint.copy(alpha = .37f),
                    start = Offset(origin.x, origin.y - orbit * .32f),
                    end = Offset(origin.x, origin.y + orbit * .32f),
                    strokeWidth = scale * .004f,
                )
                rx01DrawClock(
                    Offset(origin.x + orbit * .37f, origin.y),
                    orbit * .61f,
                    palette,
                )
            }
        }

        // Lens reflection is deliberately fixed to keep the emblem readable.
        rx01OpticalArc(
            origin, orbit * .69f, -136f, 49f,
            Color.White, scale * .012f, .20f,
        )
    }
}

private fun DrawScope.rx01DrawMicrophone(
    at: Offset,
    radius: Float,
    palette: RX01FluidPalette,
) {
    val bodyW = radius * .39f
    val bodyH = radius * .62f
    val top = at.y - radius * .45f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.iconTint, palette.iconHalo, palette.iconTint),
            startY = top,
            endY = top + bodyH,
        ),
        topLeft = Offset(at.x - bodyW * .5f, top),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(bodyW * .50f),
    )
    drawRoundRect(
        color = Color(0xFF12223B).copy(alpha = .33f),
        topLeft = Offset(at.x - bodyW * .29f, top + bodyH * .12f),
        size = Size(bodyW * .18f, bodyH * .65f),
        cornerRadius = CornerRadius(bodyW * .1f),
    )
    val upper = at.y - radius * .03f
    val lower = at.y + radius * .38f
    val catch = Path().apply {
        moveTo(at.x - radius * .40f, upper)
        cubicTo(
            at.x - radius * .40f, lower,
            at.x + radius * .40f, lower,
            at.x + radius * .40f, upper,
        )
    }
    drawPath(
        path = catch,
        color = palette.iconTint,
        style = Stroke(width = radius * .075f, cap = StrokeCap.Round),
    )
    drawLine(
        color = palette.iconTint,
        start = Offset(at.x, at.y + radius * .39f),
        end = Offset(at.x, at.y + radius * .65f),
        strokeWidth = radius * .075f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = palette.iconTint,
        start = Offset(at.x - radius * .24f, at.y + radius * .66f),
        end = Offset(at.x + radius * .24f, at.y + radius * .66f),
        strokeWidth = radius * .075f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.rx01DrawClock(
    at: Offset,
    radius: Float,
    palette: RX01FluidPalette,
) {
    val clock = radius * .44f
    val centerY = at.y + radius * .06f
    val clockCenter = Offset(at.x, centerY)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF24324B), Color(0xFF101425)),
            center = clockCenter,
            radius = clock,
        ),
        radius = clock,
        center = clockCenter,
    )
    drawCircle(
        color = palette.iconTint,
        radius = clock,
        center = clockCenter,
        style = Stroke(width = radius * .092f),
    )
    // Clock hands are symbolic and fixed; they are NOT a live countdown.
    drawLine(
        color = palette.iconHalo,
        start = clockCenter,
        end = Offset(at.x, centerY - clock * .60f),
        strokeWidth = radius * .067f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = palette.iconTint,
        start = clockCenter,
        end = Offset(at.x + clock * .55f, centerY + clock * .22f),
        strokeWidth = radius * .063f,
        cap = StrokeCap.Round,
    )
    drawCircle(
        color = palette.iconTint,
        radius = radius * .066f,
        center = clockCenter,
    )
    // Two restrained bell crowns and short foot strokes.
    rx01OpticalArc(
        Offset(at.x - radius * .39f, at.y - radius * .27f),
        radius * .17f, 193f, 133f,
        palette.iconTint, radius * .09f, .92f,
    )
    rx01OpticalArc(
        Offset(at.x + radius * .39f, at.y - radius * .27f),
        radius * .17f, 216f, 125f,
        palette.iconTint, radius * .09f, .92f,
    )
    drawLine(
        color = palette.iconTint,
        start = Offset(at.x - clock * .61f, centerY + clock * .80f),
        end = Offset(at.x - clock * .79f, centerY + clock * 1.06f),
        strokeWidth = radius * .075f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = palette.iconTint,
        start = Offset(at.x + clock * .61f, centerY + clock * .80f),
        end = Offset(at.x + clock * .79f, centerY + clock * 1.06f),
        strokeWidth = radius * .075f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.rx01OpticalArc(
    at: Offset,
    radius: Float,
    start: Float,
    sweep: Float,
    tint: Color,
    stroke: Float,
    alpha: Float,
) {
    drawArc(
        color = tint.copy(alpha = alpha.coerceIn(0f, 1f)),
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(at.x - radius, at.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}

private fun DrawScope.rx01Glint(
    at: Offset,
    orbit: Float,
    degrees: Float,
    color: Color,
    scale: Float,
) {
    val radians = (degrees.toDouble() * (PI / 180.0)).toFloat()
    val position = Offset(
        at.x + orbit * cos(radians),
        at.y + orbit * sin(radians),
    )
    drawCircle(
        color = color.copy(alpha = .76f),
        radius = scale * .015f,
        center = position,
    )
    drawCircle(
        color = Color.White.copy(alpha = .58f),
        radius = scale * .006f,
        center = position,
    )
}
