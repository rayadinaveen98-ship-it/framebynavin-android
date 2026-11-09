package com.framebynavin.app.reminders

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import com.framebynavin.app.ui.theme.RecRed
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Only the *revealed action surface* uses this blurred, cinematic color field.
 * The untouched pre-tap mic / clock silhouette remains in V195ReminderParticleScene.
 * No particle geometry, notification/scheduling logic, microphone or alarm coupling.
 */
internal object V231CinematicWaveMotion {
    internal data class Travel(val position: Float, val edgeCount: Int, val colorProgress: Float)

    /** Continuous ping-pong motion; reverses rather than teleporting at viewport edges. */
    fun travel(phase: Double): Travel {
        val safe = phase.coerceAtLeast(0.0)
        val whole = floor(safe)
        val fraction = (safe - whole).toFloat()
        val reflected = if (whole.toLong() % 2L == 0L) fraction else 1f - fraction
        // Ease color toward the NEXT hue as a moving wave reaches an edge.
        val t = ((fraction - .76f) / .24f).coerceIn(0f, 1f)
        val fade = t * t * (3f - 2f * t)
        return Travel(reflected, whole.toInt(), fade)
    }

    fun palette(kind: ReminderCheckInKind): List<Color> =
        if (kind == ReminderCheckInKind.ALARM) listOf(
            Color(0xFFDC5269), Color(0xFFEC9861), Color(0xFF8C64C8),
            Color(0xFFD67EAE), Color(0xFFE9B979), Color(0xFF876AB6),
        ) else listOf(
            Color(0xFF3787CB), Color(0xFF4DB8BE), Color(0xFF8067C9),
            Color(0xFF6A9BE6), Color(0xFF5DB9A3), Color(0xFFA084D9),
        )

    fun hue(palette: List<Color>, lane: Int, travel: Travel): Color {
        val start = palette[(lane + travel.edgeCount) % palette.size]
        val end = palette[(lane + travel.edgeCount + 1) % palette.size]
        return lerp(start, end, travel.colorProgress)
    }
}

@Composable
internal fun V231CinematicWaveBackdrop(
    kind: ReminderCheckInKind,
    lowPower: Boolean,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
    playbackActive: Boolean = false,
) {
    // A playback callback can change while this compositor is running.
    // Integrate velocity rather than multiplying absolute time on stage change:
    // the waves accelerate naturally without jumping across the viewport.
    val playbackNow by rememberUpdatedState(playbackActive)
    val elapsedSeconds by produceState(
        initialValue = 0.0,
        key1 = motionEnabled,
        key2 = lowPower,
    ) {
        if (motionEnabled) {
            var lastFrame = 0L
            var travelSeconds = 0.0
            val minFrameNs = if (lowPower) 50_000_000L else 33_333_333L
            while (true) {
                withFrameNanos { frame ->
                    if (lastFrame == 0L || frame - lastFrame >= minFrameNs) {
                        if (lastFrame != 0L) {
                            // A backgrounded Activity may miss many frames.
                            // Clamp the resumed delta to avoid giant time jumps.
                            val delta = ((frame - lastFrame).toDouble() /
                                1_000_000_000.0).coerceIn(0.0, .075)
                            val momentum = if (playbackNow && !lowPower) 1.32 else 1.0
                            travelSeconds += delta * momentum
                        }
                        value = travelSeconds
                        lastFrame = frame
                    }
                }
            }
        }
    }
    val palette = V231CinematicWaveMotion.palette(kind)
    Canvas(modifier.fillMaxSize().testTag("v231-cinematic-wave-backdrop")) {
        val darkBase = if (kind == ReminderCheckInKind.ALARM) Color(0xFF100C15) else Color(0xFF080F1D)
        drawRect(darkBase)
        val t = if (motionEnabled) elapsedSeconds else 0.0
        val extent = max(size.width, size.height)
        // Stage response from a real MediaPlayer or TTS callback, not output RMS.
        // Lower power and disabled motion retain the original calm backdrop.
        val soundInProgress = playbackActive && motionEnabled && !lowPower
        val accent = if (soundInProgress) 1.22f else 1f
        if (soundInProgress) {
            // Live playback STAGE only; smooth decorative bloom, not audio RMS.
            val breathe = (.5f + .5f * sin(t * 4.1)).toFloat()
            val audioCenter = Offset(size.width * .50f, size.height * .38f)
            val bloomRadius = extent * (.43f + .045f * breathe)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        RecRed.copy(alpha = .105f + .05f * breathe),
                        RecRed.copy(alpha = .03f),
                        Color.Transparent,
                    ),
                    center = audioCenter,
                    radius = bloomRadius,
                ),
                center = audioCenter,
                radius = bloomRadius,
            )
        }
        val count = if (lowPower) 3 else 5
        for (i in 0 until count) {
            val horizontal = V231CinematicWaveMotion.travel(t * (.095 + i * .021) + .19 * i + .12)
            val vertical = V231CinematicWaveMotion.travel(t * (.071 + i * .018) + .27 * i + .34)
            // Independent axes and rates produce graceful turns after each edge bounce.
            val center = Offset(horizontal.position * size.width, vertical.position * size.height)
            val chroma = V231CinematicWaveMotion.hue(palette, i, horizontal)
            val radius = extent * (if (lowPower) .74f else .63f)
            // Continuous falloff is the blur: no sharp blobs, bokeh dots or GPU blur allocation.
            drawCircle(
                brush = Brush.radialGradient(
                    0f to chroma.copy(alpha = .23f * accent),
                    .32f to chroma.copy(alpha = .13f * accent),
                    1f to chroma.copy(alpha = 0f),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
            // A soft, curved band across each diffuse bloom reads as flowing
            // color rather than a point light. Broad stroke + translucent brush.
            if (!lowPower || i == 0) {
                val sway = sin((t * .68 + i * 1.31)).toFloat() * size.height * .10f
                val y = center.y + sway
                val path = Path().apply {
                    moveTo(-size.width * .26f, y + size.height * .13f)
                    cubicTo(
                        size.width * .20f, y - size.height * .21f,
                        size.width * .68f, y + size.height * .23f,
                        size.width * 1.23f, y - size.height * .10f,
                    )
                }
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        0f to chroma.copy(alpha = 0f),
                        .40f to chroma.copy(alpha = .10f),
                        .68f to chroma.copy(alpha = .12f),
                        1f to chroma.copy(alpha = 0f),
                    ),
                    style = Stroke(width = min(size.width, size.height) * .20f, cap = StrokeCap.Round),
                )
            }
        }
        // Keep foreground title, timer and action labels crisp over animated color.
        drawRect(Color.Black.copy(alpha = .17f))
    }
}
