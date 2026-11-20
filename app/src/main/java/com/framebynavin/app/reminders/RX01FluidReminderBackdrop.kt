package com.framebynavin.app.reminders

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * RX01-A — visual foundation, not a live alert integration.
 *
 * Three distinct fluid-light languages and an intentionally DIFFERENT,
 * quieter MAIN counterpart. Draw-only: no touch handlers, notification
 * launches, scheduling, user content, wake locks, sounds, Done or Snooze.
 *
 * The 21-day CHALLENGE_21 palette is NOT permission to launch a full-screen
 * intent: that OS/delivery decision belongs to a later gated stage.
 *
 * No Android RuntimeShader dependency: this deterministic Compose Canvas
 * fallback supports the app's minSdk 26. An optional API33+ shader can be
 * separately reviewed later without changing the interface.
 */
internal enum class RX01ReminderDomain {
    CREATOR,
    PERSONAL,
    CHALLENGE_21,
}

internal enum class RX01SurfacePhase {
    ENTRANCE,
    MAIN,
}

internal data class RX01FluidPalette(
    val baseTop: Color,
    val baseBottom: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val highlight: Color,
    val iconTint: Color,
    val iconHalo: Color,
    val ink: Color,
)

/**
 * Use fixed, auditable palettes rather than runtime-random colours.
 * Main palettes have their OWN bases, tints and luminance levels;
 * they are not the Entrance scene darkened by a generic overlay.
 */
internal object RX01FluidTheme {
    fun palette(domain: RX01ReminderDomain, phase: RX01SurfacePhase): RX01FluidPalette =
        when (domain to phase) {
            RX01ReminderDomain.CREATOR to RX01SurfacePhase.ENTRANCE -> RX01FluidPalette(
                baseTop = Color(0xFF080B35),
                baseBottom = Color(0xFF180529),
                primary = Color(0xFF2867FF),
                secondary = Color(0xFF932CFF),
                tertiary = Color(0xFFFF37B5),
                highlight = Color(0xFF47E8F6),
                iconTint = Color(0xFFE9F7FF),
                iconHalo = Color(0xFF51B8FF),
                ink = Color.White,
            )
            RX01ReminderDomain.CREATOR to RX01SurfacePhase.MAIN -> RX01FluidPalette(
                baseTop = Color(0xFF090D26),
                baseBottom = Color(0xFF0E0920),
                primary = Color(0xFF28479C),
                secondary = Color(0xFF543D97),
                tertiary = Color(0xFF853F89),
                highlight = Color(0xFF257E97),
                iconTint = Color(0xFFE5F3FF),
                iconHalo = Color(0xFF638CCB),
                ink = Color(0xFFF5F6FF),
            )
            RX01ReminderDomain.PERSONAL to RX01SurfacePhase.ENTRANCE -> RX01FluidPalette(
                baseTop = Color(0xFF432057),
                baseBottom = Color(0xFF251445),
                primary = Color(0xFFFF8D9B),
                secondary = Color(0xFFFFBD74),
                tertiary = Color(0xFFCC7CFF),
                highlight = Color(0xFF7FE2E1),
                iconTint = Color(0xFFFFF4EB),
                iconHalo = Color(0xFFFFD4BE),
                ink = Color.White,
            )
            RX01ReminderDomain.PERSONAL to RX01SurfacePhase.MAIN -> RX01FluidPalette(
                baseTop = Color(0xFF221930),
                baseBottom = Color(0xFF161325),
                primary = Color(0xFF8B577D),
                secondary = Color(0xFF9F7863),
                tertiary = Color(0xFF655587),
                highlight = Color(0xFF487A85),
                iconTint = Color(0xFFFFF0E8),
                iconHalo = Color(0xFFC99DA3),
                ink = Color(0xFFFFF5EF),
            )
            RX01ReminderDomain.CHALLENGE_21 to RX01SurfacePhase.ENTRANCE -> RX01FluidPalette(
                baseTop = Color(0xFF220920),
                baseBottom = Color(0xFF15060A),
                primary = Color(0xFFE63552),
                secondary = Color(0xFFFF812C),
                tertiary = Color(0xFFFFC45D),
                highlight = Color(0xFF943CD2),
                iconTint = Color(0xFFFFF3D7),
                iconHalo = Color(0xFFFFB660),
                ink = Color.White,
            )
            RX01ReminderDomain.CHALLENGE_21 to RX01SurfacePhase.MAIN -> RX01FluidPalette(
                baseTop = Color(0xFF1C0B18),
                baseBottom = Color(0xFF0F090D),
                primary = Color(0xFF823747),
                secondary = Color(0xFF94603C),
                tertiary = Color(0xFF9A7748),
                highlight = Color(0xFF4C2A76),
                iconTint = Color(0xFFFFF0DC),
                iconHalo = Color(0xFFD49B63),
                ink = Color(0xFFFFF7E9),
            )
            else -> error("Unrecognized RX01 palette combination")
        }

    fun motionCycleMillis(domain: RX01ReminderDomain, phase: RX01SurfacePhase): Int =
        when (phase) {
            RX01SurfacePhase.MAIN -> 31_000
            RX01SurfacePhase.ENTRANCE -> when (domain) {
                RX01ReminderDomain.CREATOR -> 16_000
                RX01ReminderDomain.PERSONAL -> 23_000
                RX01ReminderDomain.CHALLENGE_21 -> 18_000
            }
        }
}

/**
 * Draw-only reusable light-field; later stages will supply the independently
 * reviewed icon, double-tap interaction gate and original action-owner screens.
 *
 * motionEnabled must be false for animator-disabled/reduced-motion devices.
 * lowPower must be true when the Android battery saver policy requires static art.
 * No hidden clock runs in those modes.
 */
@Composable
internal fun RX01FluidReminderBackdrop(
    domain: RX01ReminderDomain,
    phase: RX01SurfacePhase,
    motionEnabled: Boolean,
    lowPower: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = RX01FluidTheme.palette(domain, phase)
    val animate = motionEnabled && !lowPower
    val fraction = if (animate) {
        val transition = rememberInfiniteTransition(label = "rx01-fluid-light")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(RX01FluidTheme.motionCycleMillis(domain, phase), easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "rx01-light-cycle",
        )
        progress
    } else 0f

    Canvas(
        modifier = modifier.fillMaxSize()
            .testTag("rx01-fluid-${domain.name.lowercase()}-${phase.name.lowercase()}"),
    ) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val w = size.width
        val h = size.height
        val unit = max(w, h)
        val theta = fraction * (2.0 * PI).toFloat()
        val main = phase == RX01SurfacePhase.MAIN
        val amplitude = if (main) 0.045f else when (domain) {
            RX01ReminderDomain.CREATOR -> 0.13f
            RX01ReminderDomain.PERSONAL -> 0.085f
            RX01ReminderDomain.CHALLENGE_21 -> 0.105f
        }
        val glow = if (main) .29f else .74f

        drawRect(Brush.verticalGradient(listOf(palette.baseTop, palette.baseBottom)))

        // Wide diffuse colour fields: two coupled motions, no random jumps.
        // Radial alpha tails create the frosted-glass softness of the reference.
        rx01Field(
            palette.primary,
            Offset(w * (.19f + amplitude * sin(theta)), h * (.18f + amplitude * cos(theta * .7f))),
            unit * .74f,
            glow,
        )
        rx01Field(
            palette.secondary,
            Offset(w * (.78f + amplitude * cos(theta * .83f + 1.1f)), h * (.31f + amplitude * sin(theta * .63f))),
            unit * .69f,
            glow * .86f,
        )
        rx01Field(
            palette.tertiary,
            Offset(w * (.46f + amplitude * sin(theta * .61f + 2.6f)), h * (.70f + amplitude * cos(theta * .76f + .8f))),
            unit * .63f,
            glow * .80f,
        )
        rx01Field(
            palette.highlight,
            Offset(w * (.84f + amplitude * sin(theta * .72f + 4f)), h * (.09f + amplitude * cos(theta * .77f))),
            unit * .44f,
            glow * .57f,
        )

        // Creator has lateral energy; Personal gently blooms; Challenge rises.
        // Each motion is subtle and reverses continuously around the same loop.
        val accentCenter = when (domain) {
            RX01ReminderDomain.CREATOR ->
                Offset(w * (.50f + amplitude * .65f * sin(theta * .5f)), h * .39f)
            RX01ReminderDomain.PERSONAL ->
                Offset(w * .49f, h * (.37f + amplitude * .65f * sin(theta * .5f)))
            RX01ReminderDomain.CHALLENGE_21 ->
                Offset(w * .5f, h * (.56f - amplitude * .65f * sin(theta * .5f)))
        }
        rx01Field(palette.iconHalo, accentCenter, unit * .36f, if (main) .10f else .29f)

        if (main) {
            // Independent main background protects foreground text and controls.
            drawRect(Brush.verticalGradient(
                0f to Color(0xDD080A15),
                .43f to Color(0x88080812),
                1f to Color(0xF208070E),
            ))
        }
    }
}

private fun DrawScope.rx01Field(color: Color, center: Offset, radius: Float, opacity: Float) {
    if (radius <= 0f) return
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to color.copy(alpha = opacity.coerceIn(0f, 1f)),
                .39f to color.copy(alpha = (opacity * .67f).coerceIn(0f, 1f)),
                1f to color.copy(alpha = 0f),
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
