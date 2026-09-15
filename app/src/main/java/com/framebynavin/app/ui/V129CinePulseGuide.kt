package com.framebynavin.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.FrameTertiary
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import kotlin.math.cos
import kotlin.math.sin

enum class CinePulseState { IDLE, WALK, POINT, THINK, LISTEN, SUCCESS, CELEBRATE, REST }

/** Live, density-independent Cine Pulse mascot. No GIF/video loop: app state drives the pose. */
@Composable
internal fun CinePulseGuide(
    state: CinePulseState,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    val motion = rememberInfiniteTransition(label = "cinePulse")
    val phase by motion.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(if (state == CinePulseState.WALK) 760 else 3200, easing = LinearEasing)),
        label = "cinePulsePhase",
    )
    val breathe by motion.animateFloat(
        .96f, 1.035f,
        infiniteRepeatable(tween(1850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cinePulseBreathe",
    )
    val glow by motion.animateFloat(
        .48f, .92f,
        infiniteRepeatable(tween(2100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cinePulseGlow",
    )
    val bob = when (state) {
        CinePulseState.WALK -> sin(phase * Math.PI * 4).toFloat() * 2.2f
        CinePulseState.CELEBRATE -> -4.2f * sin(phase * Math.PI * 2).toFloat().coerceAtLeast(0f)
        CinePulseState.REST -> 0f
        else -> (breathe - 1f) * 18f
    }

    Canvas(modifier.graphicsLayer { translationY = bob }) {
        val w = size.width
        val h = size.height
        val u = size.minDimension / 100f
        val cx = w * .50f
        val faceY = h * .31f
        val faceW = 50f * u
        val faceH = 36f * u
        val coral = RecRed
        val aqua = FrameTertiary
        val gold = MutedGold
        val ivory = ProjectorIvory
        val shell = CinemaSurfaceRaised
        val direction = if (pointRight) 1f else -1f

        // Atmosphere: the glow follows the current app theme while the silhouette stays Cine Pulse.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(coral.copy(alpha = .22f * glow), aqua.copy(alpha = .14f * glow), gold.copy(alpha = .06f), Color.Transparent),
                center = Offset(cx, h * .43f),
                radius = w * .62f,
            ),
            radius = w * .62f,
            center = Offset(cx, h * .43f),
        )

        // Signature translucent ribbon halo. Separate arcs drift at different rates.
        val ribbonBox = Size(faceW * 1.55f, faceH * 1.65f)
        val ribbonTopLeft = Offset(cx - ribbonBox.width / 2f, faceY - ribbonBox.height / 2f)
        drawArc(
            coral.copy(alpha = .72f),
            startAngle = 198f + phase * 30f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = ribbonTopLeft,
            size = ribbonBox,
            style = Stroke(width = 6.4f * u, cap = StrokeCap.Round),
        )
        drawArc(
            aqua.copy(alpha = .72f),
            startAngle = 14f - phase * 24f,
            sweepAngle = 137f,
            useCenter = false,
            topLeft = ribbonTopLeft + Offset(0f, 1.5f * u),
            size = ribbonBox,
            style = Stroke(width = 4.6f * u, cap = StrokeCap.Round),
        )
        drawArc(
            gold.copy(alpha = .52f),
            startAngle = 322f + phase * 18f,
            sweepAngle = 72f,
            useCenter = false,
            topLeft = ribbonTopLeft + Offset(0f, 2f * u),
            size = ribbonBox,
            style = Stroke(width = 2.3f * u, cap = StrokeCap.Round),
        )

        // Head shell + premium rim.
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(shell, CinemaBlack)),
            topLeft = Offset(cx - faceW / 2f, faceY - faceH / 2f),
            size = Size(faceW, faceH),
            cornerRadius = CornerRadius(16f * u),
        )
        drawRoundRect(
            brush = Brush.linearGradient(listOf(gold.copy(alpha = .90f), coral.copy(alpha = .72f), aqua.copy(alpha = .88f))),
            topLeft = Offset(cx - faceW / 2f, faceY - faceH / 2f),
            size = Size(faceW, faceH),
            cornerRadius = CornerRadius(16f * u),
            style = Stroke(width = 2.25f * u),
        )

        val eyeY = faceY + 1f * u
        val eyeDX = 10.5f * u
        val eyeH = if (state == CinePulseState.REST) 2.2f * u else 10.5f * u
        val eyeW = 4.8f * u
        if (state == CinePulseState.CELEBRATE || state == CinePulseState.SUCCESS) {
            drawArc(ivory, 205f, 130f, false, Offset(cx - eyeDX - 5f*u, eyeY - 2f*u), Size(10f*u, 8f*u), style = Stroke(2.2f*u, cap = StrokeCap.Round))
            drawArc(ivory, 205f, 130f, false, Offset(cx + eyeDX - 5f*u, eyeY - 2f*u), Size(10f*u, 8f*u), style = Stroke(2.2f*u, cap = StrokeCap.Round))
        } else {
            drawRoundRect(ivory.copy(alpha = if (state == CinePulseState.REST) .68f else .96f), Offset(cx - eyeDX - eyeW/2, eyeY - eyeH/2), Size(eyeW, eyeH), CornerRadius(eyeW))
            drawRoundRect(ivory.copy(alpha = if (state == CinePulseState.REST) .68f else .96f), Offset(cx + eyeDX - eyeW/2, eyeY - eyeH/2), Size(eyeW, eyeH), CornerRadius(eyeW))
        }

        // Tiny signature spark above the halo.
        val sparkX = cx + 26f*u
        val sparkY = faceY - 25f*u + sin(phase * Math.PI * 2).toFloat() * 1.4f*u
        val spark = Path().apply {
            moveTo(sparkX, sparkY - 5f*u); lineTo(sparkX + 2f*u, sparkY - 1.5f*u)
            lineTo(sparkX + 5f*u, sparkY); lineTo(sparkX + 2f*u, sparkY + 1.5f*u)
            lineTo(sparkX, sparkY + 5f*u); lineTo(sparkX - 2f*u, sparkY + 1.5f*u)
            lineTo(sparkX - 5f*u, sparkY); lineTo(sparkX - 2f*u, sparkY - 1.5f*u); close()
        }
        drawPath(spark, gold.copy(alpha = .70f + .25f * glow))

        // Compact black body; the luminous core is the stable identity mark.
        val bodyTop = h * .51f
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF151722), Color(0xFF08090E))),
            topLeft = Offset(cx - 15.5f*u, bodyTop),
            size = Size(31f*u, 28f*u),
            cornerRadius = CornerRadius(12f*u),
        )
        drawCircle(coral.copy(alpha = .22f * glow), 7.5f*u, Offset(cx, bodyTop + 12.5f*u))
        drawCircle(gold.copy(alpha = .94f), 3.2f*u, Offset(cx, bodyTop + 12.5f*u))
        drawCircle(ivory.copy(alpha = .92f), 1.2f*u, Offset(cx - .7f*u, bodyTop + 11.5f*u))

        val shoulderY = bodyTop + 7f*u
        val leftShoulder = Offset(cx - 13f*u, shoulderY)
        val rightShoulder = Offset(cx + 13f*u, shoulderY)
        val arm = ivory.copy(alpha = .78f)
        val walkSwing = if (state == CinePulseState.WALK) sin(phase * Math.PI * 4).toFloat() * 7f*u else 0f

        when (state) {
            CinePulseState.POINT -> {
                val s = if (pointRight) rightShoulder else leftShoulder
                val elbow = Offset(s.x + direction * 13f*u, s.y - 2f*u)
                val hand = Offset(s.x + direction * 28f*u, s.y - 11f*u)
                drawLine(arm, s, elbow, 4f*u, StrokeCap.Round)
                drawLine(arm, elbow, hand, 4f*u, StrokeCap.Round)
                drawCircle(gold, 2.5f*u, hand)
                val rest = if (pointRight) leftShoulder else rightShoulder
                drawLine(arm.copy(alpha=.62f), rest, Offset(rest.x - direction*4f*u, rest.y + 17f*u), 3.5f*u, StrokeCap.Round)
            }
            CinePulseState.CELEBRATE -> {
                drawLine(arm, leftShoulder, Offset(cx - 28f*u, bodyTop - 9f*u), 4f*u, StrokeCap.Round)
                drawLine(arm, rightShoulder, Offset(cx + 28f*u, bodyTop - 9f*u), 4f*u, StrokeCap.Round)
                drawCircle(aqua, 2.8f*u, Offset(cx - 28f*u, bodyTop - 9f*u))
                drawCircle(coral, 2.8f*u, Offset(cx + 28f*u, bodyTop - 9f*u))
            }
            CinePulseState.THINK -> {
                drawLine(arm, leftShoulder, Offset(cx - 17f*u, bodyTop + 20f*u), 3.5f*u, StrokeCap.Round)
                drawLine(arm, rightShoulder, Offset(cx + 20f*u, faceY + 15f*u), 3.5f*u, StrokeCap.Round)
                drawCircle(gold, 2.4f*u, Offset(cx + 20f*u, faceY + 15f*u))
            }
            CinePulseState.LISTEN -> {
                drawLine(aqua.copy(alpha=.90f), leftShoulder, Offset(cx - 22f*u, bodyTop + 3f*u), 3.7f*u, StrokeCap.Round)
                drawLine(aqua.copy(alpha=.90f), rightShoulder, Offset(cx + 22f*u, bodyTop + 3f*u), 3.7f*u, StrokeCap.Round)
            }
            CinePulseState.SUCCESS -> {
                drawLine(arm, leftShoulder, Offset(cx - 17f*u, bodyTop + 18f*u), 3.5f*u, StrokeCap.Round)
                drawLine(arm, rightShoulder, Offset(cx + 18f*u, bodyTop + 4f*u), 3.5f*u, StrokeCap.Round)
                drawCircle(gold, 2.5f*u, Offset(cx + 18f*u, bodyTop + 4f*u))
            }
            else -> {
                drawLine(arm.copy(alpha=.64f), leftShoulder, Offset(cx - 18f*u - walkSwing, bodyTop + 21f*u), 3.5f*u, StrokeCap.Round)
                drawLine(arm.copy(alpha=.64f), rightShoulder, Offset(cx + 18f*u + walkSwing, bodyTop + 21f*u), 3.5f*u, StrokeCap.Round)
            }
        }

        val hipY = h * .79f
        val footY = h * .92f
        drawLine(arm.copy(alpha=.70f), Offset(cx - 7f*u, hipY), Offset(cx - 10f*u + walkSwing, footY), 4f*u, StrokeCap.Round)
        drawLine(arm.copy(alpha=.70f), Offset(cx + 7f*u, hipY), Offset(cx + 10f*u - walkSwing, footY), 4f*u, StrokeCap.Round)
        drawLine(coral.copy(alpha=.86f), Offset(cx - 14f*u + walkSwing, footY), Offset(cx - 6f*u + walkSwing, footY), 3.2f*u, StrokeCap.Round)
        drawLine(aqua.copy(alpha=.82f), Offset(cx + 6f*u - walkSwing, footY), Offset(cx + 14f*u - walkSwing, footY), 3.2f*u, StrokeCap.Round)

        if (state == CinePulseState.CELEBRATE) {
            repeat(5) { i ->
                val angle = (i * 72f + phase * 35f) * 0.017453292f
                val r = 39f*u
                drawCircle(
                    if (i % 2 == 0) coral else aqua,
                    1.4f*u,
                    Offset(cx + cos(angle)*r, faceY + sin(angle)*r),
                )
            }
        }
    }
}
