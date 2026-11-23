package com.framebynavin.app.ui

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.os.PowerManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.RecRedDeep
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Presentation-only math for the P8B voice core.
 * Android SpeechRecognizer reports a non-calibrated dB-like level, commonly
 * 0..10. This is an intensity cue, not measured audio quality or a waveform.
 */
internal object P8BVoiceOrbPolicy {
    fun intensity(listening: Boolean, rmsDb: Float): Float =
        if (!listening || !rmsDb.isFinite()) 0f
        else ((rmsDb - 0.6f) / 8.4f).coerceIn(0f, 1f)
}

/**
 * The Voice Idea Living Orb. Only V117 dictate surfaces use this orb.
 * The previous FC13ReactiveDotField (used by Spark elsewhere) is preserved.
 *
 * At rest the core breathes noticeably, even before microphone permission;
 * listening adds a clear, actual RMS-linked response. Animator-off, power
 * saver and low-RAM devices use a non-animated presentation.
 */
@Composable
internal fun P8BLivingVoiceOrb(
    listening: Boolean,
    rmsDb: Float,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val motionAllowed = remember { ValueAnimator.areAnimatorsEnabled() }
    val lowPower = remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true ||
            context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val animate = motionAllowed && !lowPower
    val energy by animateFloatAsState(
        targetValue = P8BVoiceOrbPolicy.intensity(listening, rmsDb),
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 220f),
        label = "p8bVoiceEnergy",
    )
    // This transition is intentionally visible even at rest. The looping
    // phase is seamless and the amplitude is explicitly zero with Reduce Motion.
    val transition = rememberInfiniteTransition(label = "p8bBreathingLoop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
        ),
        label = "p8bBreathingPhase",
    )
    val t = if (animate) phase else 0f
    val pulse = if (animate) (sin(t - (PI / 2).toFloat()) + 1f) * 0.5f else 0.48f
    val effectiveEnergy = if (animate) energy else 0f

    Canvas(
        modifier = modifier
            .semantics {
                contentDescription = if (listening) "Stop voice dictation" else "Start voice dictation"
            }
            .clickable(role = Role.Button, onClick = onTap),
    ) {
        val center = this.center
        val unit = size.minDimension
        val radius = unit * (0.235f + 0.046f * pulse + 0.09f * effectiveEnergy)
        val bloom = unit * (0.41f + 0.07f * pulse + 0.055f * effectiveEnergy)
        val crimson = RecRed.copy(alpha = 0.16f + pulse * 0.15f + effectiveEnergy * 0.21f)

        // Soft luminance travels with the core rather than flashing the screen.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    RecRedDeep.copy(alpha = 0.25f + 0.11f * pulse),
                    crimson,
                    Color.Transparent,
                ),
                center = center,
                radius = bloom,
            ),
            radius = bloom,
            center = center,
        )

        // A gently irregular, fluid silhouette, with deliberately stronger
        // amplitude than the previous fixed grid of dots.
        val contour = Path()
        val steps = 72
        for (index in 0..steps) {
            val theta = (index * 2.0 * PI / steps).toFloat()
            val wobble = if (animate) {
                1f + 0.044f * sin(3f * theta - 0.8f * t) +
                    0.029f * sin(5f * theta + 1.25f * t)
            } else 1f
            val r = radius * wobble
            val x = center.x + r * cos(theta)
            val y = center.y + r * sin(theta)
            if (index == 0) contour.moveTo(x, y) else contour.lineTo(x, y)
        }
        contour.close()
        drawPath(
            path = contour,
            brush = Brush.radialGradient(
                colors = listOf(
                    ProjectorIvory.copy(alpha = if (listening) .19f else .10f),
                    RecRed.copy(alpha = if (listening) .58f else .43f),
                    RecRedDeep.copy(alpha = .83f),
                ),
                center = Offset(center.x - radius * .14f, center.y - radius * .20f),
                radius = radius * 1.45f,
            ),
        )

        // Two subtle expanding halos respond to actual microphone energy.
        // No strobing, particle explosion or unrelated fake 'speech' activity.
        for (ring in 0..1) {
            val advance = if (animate) (t / (2f * PI.toFloat()) + ring * .5f) % 1f
                else ring * .5f
            val ringRadius = radius * (1.12f + advance * .75f)
            val alpha = (1f - advance) *
                (if (listening) .35f + effectiveEnergy * .27f else .15f)
            drawCircle(
                color = RecRed.copy(alpha = alpha.coerceIn(0f, .72f)),
                radius = ringRadius,
                center = center,
                style = Stroke(width = min(unit * .009f, 2.8f)),
            )
        }

        // A halo of gently drifting satellites stays well inside Canvas bounds.
        val satelliteCount = if (lowPower) 0 else 16
        for (index in 0 until satelliteCount) {
            val angle = (index * 2.0 * PI / satelliteCount).toFloat() +
                t * (if (listening) .28f else .10f)
            val orbit = unit * (.34f + .020f * sin(t + index * .89f))
            drawCircle(
                color = if (index % 4 == 0) ProjectorIvory.copy(alpha = .58f)
                    else RecRed.copy(alpha = .46f),
                radius = unit * (if (index % 5 == 0) .008f else .0048f) *
                    (1f + .25f * pulse + .35f * effectiveEnergy),
                center = Offset(center.x + orbit * cos(angle),
                    center.y + orbit * sin(angle)),
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ProjectorIvory.copy(alpha = .91f),
                    ProjectorIvory.copy(alpha = .28f),
                    Color.Transparent,
                ),
                center = center,
                radius = unit * (.076f + effectiveEnergy * .022f),
            ),
            radius = unit * (.076f + effectiveEnergy * .022f),
            center = center,
        )
    }
}
