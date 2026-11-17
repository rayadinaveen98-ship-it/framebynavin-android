package com.framebynavin.app.reminders

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * UX06C: five curated, original Canvas scene families.
 *
 * Draw-only presentation: no clickable Canvas, vibration, clock, notification,
 * audio sample, TTS amplitude, completion, gesture or scheduling dependency.
 * Existing Classic/V394 visual preferences and domain stores remain unchanged.
 *
 * Runtime bounds: at most 6 iris blades, 4 beams, 68 motes or 5 ring arcs.
 * Disabled Android animation / battery saver yields a deterministic static frame.
 * The lower half deliberately has much less contrast than the decorative hero,
 * preserving legible text and action affordances on Creator and Personal.
 */
internal object V406CinematicScenes {
    val curated: List<V391ReminderScene> = listOf(
        V391ReminderScene.CINEMATIC_APERTURE,
        V391ReminderScene.PERSONAL_SUNRISE,
        V391ReminderScene.ATMOSPHERIC_PARTICLES,
        V391ReminderScene.PROJECTOR_LIGHT,
        V391ReminderScene.GLOWING_RINGS,
    )

    val classic: List<V391ReminderScene> = listOf(
        V391ReminderScene.DIRECTOR,
        V391ReminderScene.AURORA_SILK,
        V391ReminderScene.LIQUID_CHROME,
        V391ReminderScene.CELESTIAL_DRIFT,
        V391ReminderScene.CRIMSON_PULSE,
        V391ReminderScene.GLASSWAVE,
        V391ReminderScene.EMBER_FIELD,
    )

    fun isCurated(scene: V391ReminderScene): Boolean = scene in curated
}

@Composable
internal fun V406CinematicReminderBackdrop(
    scene: V391ReminderScene,
    motionEnabled: Boolean,
    lowPower: Boolean,
    modifier: Modifier = Modifier,
) {
    require(V406CinematicScenes.isCurated(scene))
    // Unlike Classic's lower-frequency animation, battery saver freezes the
    // new cinematic effects completely. This never delays reminder controls.
    val t = V336MotionTime(motionEnabled && !lowPower, lowPower)
    Canvas(modifier.fillMaxSize().testTag("ux06c-scene-${scene.name}")) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val center = Offset(w * .50f, h * .30f)
        val radius = min(w * .33f, h * .30f)
        val accent = when (scene) {
            V391ReminderScene.CINEMATIC_APERTURE -> Color(0xFFF4C58C)
            V391ReminderScene.PERSONAL_SUNRISE -> Color(0xFFFFC6A2)
            V391ReminderScene.ATMOSPHERIC_PARTICLES -> Color(0xFFB8CADB)
            V391ReminderScene.PROJECTOR_LIGHT -> Color(0xFFE9B27D)
            V391ReminderScene.GLOWING_RINGS -> Color(0xFFFF8292)
            else -> error("Only curated scene families render here")
        }
        val background = when (scene) {
            V391ReminderScene.CINEMATIC_APERTURE ->
                listOf(Color(0xFF070609), Color(0xFF23111A), Color(0xFF11080C), Color(0xFF070609))
            V391ReminderScene.PERSONAL_SUNRISE ->
                listOf(Color(0xFF120E20), Color(0xFF3A2230), Color(0xFF191423), Color(0xFF0D0C15))
            V391ReminderScene.ATMOSPHERIC_PARTICLES ->
                listOf(Color(0xFF08101A), Color(0xFF152338), Color(0xFF101621), Color(0xFF070A11))
            V391ReminderScene.PROJECTOR_LIGHT ->
                listOf(Color(0xFF0B080B), Color(0xFF251720), Color(0xFF130E13), Color(0xFF090709))
            V391ReminderScene.GLOWING_RINGS ->
                listOf(Color(0xFF12080F), Color(0xFF2A0F20), Color(0xFF130911), Color(0xFF09070C))
            else -> error("Only curated scene families render here")
        }
        drawRect(Brush.verticalGradient(background))
        v406Glow(center, radius * 2.15f, accent, .16f)

        when (scene) {
            V391ReminderScene.CINEMATIC_APERTURE -> {
                // Quietly turning iris blades, clearly distinct from orbit rings.
                val spin = sin(t * .55f) * .12f
                val throat = radius * (.33f + .045f * sin(t * .62f))
                drawCircle(Color(0xFF05080E), radius * .95f, center)
                repeat(6) { i ->
                    val a = (i * PI / 3.0).toFloat() + spin
                    val b = a + (PI / 3.0).toFloat()
                    val p = Path().apply {
                        val p0 = v406Polar(center, radius * .94f, a)
                        val p1 = v406Polar(center, radius * .94f, b)
                        val p2 = v406Polar(center, throat, b + .33f)
                        val p3 = v406Polar(center, throat, a + .24f)
                        moveTo(p0.x, p0.y)
                        lineTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        close()
                    }
                    drawPath(
                        p,
                        if (i % 2 == 0) Color(0xFF6D4850).copy(alpha = .68f)
                        else Color(0xFF50343E).copy(alpha = .79f),
                    )
                    drawPath(p, accent.copy(alpha = .22f),
                        style = Stroke(width = 1.dp.toPx()))
                }
                drawCircle(Color(0xFF07090D), throat * .78f, center)
                drawCircle(accent.copy(alpha = .67f), radius, center,
                    style = Stroke(width = 1.7.dp.toPx()))
                drawCircle(accent.copy(alpha = .26f), radius * 1.18f, center,
                    style = Stroke(width = 1.dp.toPx()))
            }

            V391ReminderScene.PERSONAL_SUNRISE -> {
                val horizon = h * .42f
                val sway = sin(t * .30f) * h * .007f
                val sun = Offset(w * .52f, h * .28f + sway)
                v406Glow(sun, radius * 1.68f, Color(0xFFFFB98E), .30f)
                drawCircle(Brush.radialGradient(
                    listOf(Color(0xFFFFE1B9), Color(0xFFF8AE8E)),
                    center = sun, radius = radius * .45f),
                    radius * .45f, sun)
                val haze = Path().apply {
                    moveTo(0f, horizon)
                    cubicTo(w * .24f, horizon - h * .045f,
                        w * .63f, horizon + h * .03f, w, horizon - h * .02f)
                    lineTo(w, h * .57f)
                    lineTo(0f, h * .57f)
                    close()
                }
                drawPath(haze, Brush.verticalGradient(
                    listOf(Color(0xFFF7B29A).copy(alpha = .23f), Color.Transparent),
                    startY = horizon - h * .07f, endY = h * .57f))
                repeat(if (lowPower) 2 else 4) { i ->
                    val yy = horizon + h * (.016f + i * .028f)
                    drawLine(accent.copy(alpha = .21f - i * .037f),
                        Offset(w * (.18f + i * .04f), yy),
                        Offset(w * (.82f - i * .04f), yy),
                        strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
                }
            }

            V391ReminderScene.ATMOSPHERIC_PARTICLES -> {
                val count = if (lowPower) 20 else 68
                repeat(count) { i ->
                    // Deterministic positions: no random object creation per frame.
                    val fx = ((i * 83 + 29) % 257) / 257f
                    val fy = ((i * 131 + 11) % 263) / 263f
                    val drift = sin(t * (.16f + i % 5 * .026f) + i * .63f)
                    val x = (fx + drift * .012f) * w
                    val y = (fy + sin(t * .20f + i * .31f) * .012f) * h
                    val dim = if (fy > .53f) .37f else 1f
                    val shimmer = .48f + .37f * sin(t * .52f + i * .69f)
                    val alpha = ((.08f + shimmer * .22f) * dim).coerceIn(.03f, .36f)
                    val c = if (i % 6 == 0) Color(0xFFFFD6B7) else accent
                    drawCircle(c.copy(alpha = alpha),
                        radius = (if (i % 13 == 0) 1.7f else .85f).dp.toPx(),
                        center = Offset(x, y))
                }
            }

            V391ReminderScene.PROJECTOR_LIGHT -> {
                val origin = Offset(w * .50f, h * .055f)
                repeat(if (lowPower) 2 else 4) { i ->
                    val offset = (i - 1.5f) * .25f
                    val sway = sin(t * (.19f + i * .07f) + i) * .04f
                    val spread = w * (.26f + i * .065f)
                    val bottom = h * (.49f + i * .035f)
                    val endX = w * (.5f + offset + sway)
                    val beam = Path().apply {
                        moveTo(origin.x, origin.y)
                        lineTo(endX - spread, bottom)
                        lineTo(endX + spread, bottom)
                        close()
                    }
                    drawPath(beam, Brush.verticalGradient(
                        listOf(accent.copy(alpha = .13f - i * .018f), Color.Transparent),
                        startY = origin.y, endY = bottom))
                }
                drawCircle(accent.copy(alpha = .79f), 3.dp.toPx(), origin)
                // Low-contrast film frame hints at the edges, not below the buttons.
                for (sx in listOf(.055f, .945f)) {
                    drawLine(accent.copy(alpha = .20f),
                        Offset(w * sx, h * .09f), Offset(w * sx, h * .47f),
                        strokeWidth = .8.dp.toPx())
                }
            }

            V391ReminderScene.GLOWING_RINGS -> {
                repeat(if (lowPower) 3 else 5) { i ->
                    val scale = .68f + i * .20f
                    val r = radius * scale
                    val rotation = t * (7f + i * 3f) * (if (i % 2 == 0) 1f else -1f)
                    val alpha = (.42f - i * .056f).coerceAtLeast(.12f)
                    drawArc(
                        color = accent.copy(alpha = alpha),
                        startAngle = -65f + rotation + i * 58f,
                        sweepAngle = 92f + i * 8f,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = (2.3f - i * .24f).dp.toPx(),
                            cap = StrokeCap.Round),
                    )
                }
                drawCircle(accent.copy(alpha = .16f), radius * .50f, center,
                    style = Stroke(width = 1.0.dp.toPx()))
                val core = Offset(center.x, center.y)
                drawCircle(Brush.radialGradient(
                    listOf(accent.copy(alpha = .16f), Color.Transparent),
                    center = core, radius = radius * .72f),
                    radius = radius * .72f, center = core)
            }

            else -> error("Only curated scene families render here")
        }

        // The lower control area is covered by a stable tint, not a pulsating
        // overlay. The Canvas cannot intercept any button or TalkBack action.
        drawRect(Brush.verticalGradient(
            0f to Color.Transparent,
            .46f to Color.Transparent,
            .74f to Color(0xBB09080E),
            1f to Color(0xE609070C),
        ))
    }
}

private fun DrawScope.v406Glow(
    center: Offset, radius: Float, color: Color, alpha: Float,
) {
    drawCircle(
        Brush.radialGradient(
            listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center, radius = radius),
        radius = radius, center = center,
    )
}

private fun v406Polar(center: Offset, radius: Float, radians: Float): Offset =
    Offset(center.x + radius * cos(radians), center.y + radius * sin(radians))
