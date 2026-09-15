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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.FrameTertiary
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class CinePulseState {
    IDLE,
    WALK,
    POINT,
    PRESENT,
    LOOK,
    NOD,
    WAVE,
    THINK,
    LISTEN,
    SUCCESS,
    CELEBRATE,
    REST,
}

/**
 * Live Cine Pulse renderer.
 *
 * The mascot is intentionally isolated behind one state-driven composable so a future authored
 * Rive rig can replace the renderer without changing setup, tour, spotlight or empty-state logic.
 */
@Composable
internal fun CinePulseGuide(
    state: CinePulseState,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    val motion = rememberInfiniteTransition(label = "cinePulseV131")
    val phase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == CinePulseState.WALK) 1040 else 3300,
                easing = LinearEasing,
            ),
        ),
        label = "cinePulsePhase",
    )
    val breathe by motion.animateFloat(
        initialValue = .985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cinePulseBreath",
    )
    val glow by motion.animateFloat(
        initialValue = .55f,
        targetValue = .96f,
        animationSpec = infiniteRepeatable(
            animation = tween(2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cinePulseGlow",
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val u = size.minDimension / 100f
        val cx = w * .50f
        val direction = if (pointRight) 1f else -1f
        val cycle = sin(phase * Math.PI * 2.0).toFloat()
        val opposite = sin(phase * Math.PI * 2.0 + Math.PI).toFloat()
        val ribbonCycle = sin(phase * Math.PI * 2.0).toFloat()

        val coral = RecRed
        val hotPink = Color(0xFFFF4B9A)
        val orange = Color(0xFFFF8A4C)
        val aqua = FrameTertiary
        val gold = MutedGold
        val ivory = ProjectorIvory
        val bodyBlack = Color(0xFF090A10)
        val bodyLift = when (state) {
            CinePulseState.WALK -> abs(cycle) * .35f * u
            CinePulseState.NOD -> 0f
            CinePulseState.CELEBRATE -> abs(cycle) * .25f * u
            CinePulseState.REST -> 0f
            else -> (breathe - 1f) * 6f * u
        }
        val nodShift = if (state == CinePulseState.NOD) max(0f, cycle) * 1.25f * u else 0f
        val faceY = h * .29f + nodShift + bodyLift
        val faceW = 58f * u
        val faceH = 37f * u

        // Soft cinematic atmosphere. The mascot remains readable at tiny setup/tour sizes.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    hotPink.copy(alpha = .13f * glow),
                    aqua.copy(alpha = .11f * glow),
                    orange.copy(alpha = .07f * glow),
                    Color.Transparent,
                ),
                center = Offset(cx, h * .42f),
                radius = w * .60f,
            ),
            radius = w * .60f,
            center = Offset(cx, h * .42f),
        )

        // A detached scarf/ribbon tail gives Cine Pulse the flowing silhouette from the locked board.
        val tail = Path().apply {
            moveTo(cx + 9f * u, h * .49f)
            cubicTo(
                cx + 25f * u,
                h * .48f + ribbonCycle * 1.2f * u,
                cx + 31f * u,
                h * .58f,
                cx + 19f * u,
                h * .65f,
            )
            cubicTo(
                cx + 27f * u,
                h * .59f,
                cx + 20f * u,
                h * .53f,
                cx + 7f * u,
                h * .56f,
            )
            close()
        }
        drawPath(
            path = tail,
            brush = Brush.linearGradient(
                listOf(hotPink.copy(alpha = .82f), orange.copy(alpha = .72f), aqua.copy(alpha = .68f)),
            ),
        )

        // Three broad ribbon bands orbit the head. Their drift is independent from walking,
        // preventing the crown from making the gait feel dance-like.
        val haloSize = Size(faceW * 1.48f, faceH * 1.58f)
        val haloTopLeft = Offset(cx - haloSize.width / 2f, faceY - haloSize.height / 2f)
        drawArc(
            color = hotPink.copy(alpha = .78f),
            startAngle = 194f + ribbonCycle * 5f,
            sweepAngle = 154f,
            useCenter = false,
            topLeft = haloTopLeft,
            size = haloSize,
            style = Stroke(width = 8.2f * u, cap = StrokeCap.Round),
        )
        drawArc(
            color = coral.copy(alpha = .74f),
            startAngle = 230f - ribbonCycle * 4f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = haloTopLeft + Offset(1f * u, 1f * u),
            size = haloSize,
            style = Stroke(width = 5.8f * u, cap = StrokeCap.Round),
        )
        drawArc(
            color = aqua.copy(alpha = .78f),
            startAngle = 8f + ribbonCycle * 4f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = haloTopLeft + Offset(0f, 1.5f * u),
            size = haloSize,
            style = Stroke(width = 6.1f * u, cap = StrokeCap.Round),
        )
        drawArc(
            color = orange.copy(alpha = .58f),
            startAngle = 326f - ribbonCycle * 3f,
            sweepAngle = 76f,
            useCenter = false,
            topLeft = haloTopLeft + Offset(0f, 2f * u),
            size = haloSize,
            style = Stroke(width = 3.2f * u, cap = StrokeCap.Round),
        )

        // Locked oval face: no rectangular robot head.
        drawOval(
            color = Color(0xFF030409),
            topLeft = Offset(cx - faceW / 2f, faceY - faceH / 2f),
            size = Size(faceW, faceH),
        )
        drawOval(
            brush = Brush.linearGradient(
                listOf(
                    hotPink.copy(alpha = .90f),
                    orange.copy(alpha = .88f),
                    aqua.copy(alpha = .92f),
                ),
            ),
            topLeft = Offset(cx - faceW / 2f, faceY - faceH / 2f),
            size = Size(faceW, faceH),
            style = Stroke(width = 1.7f * u),
        )

        // Cute eye movement is deliberately retained and expanded.
        val eyeLook = when (state) {
            CinePulseState.POINT, CinePulseState.PRESENT -> direction * 1.8f * u
            CinePulseState.LOOK -> cycle * 2.6f * u
            CinePulseState.THINK -> direction * 1.1f * u
            CinePulseState.LISTEN -> -direction * .9f * u
            else -> sin(phase * Math.PI).toFloat() * .45f * u
        }
        val eyeYShift = when (state) {
            CinePulseState.THINK -> -1.0f * u
            CinePulseState.NOD -> .7f * u
            else -> 0f
        }
        val blinkWindow = phase > .965f && state !in setOf(CinePulseState.CELEBRATE, CinePulseState.SUCCESS)
        val eyeY = faceY + .6f * u + eyeYShift
        val eyeDX = 11.4f * u
        val eyeW = 5.1f * u
        val normalEyeH = if (state == CinePulseState.REST || blinkWindow) 2.0f * u else 11.8f * u

        if (state == CinePulseState.CELEBRATE || state == CinePulseState.SUCCESS) {
            drawArc(
                color = ivory,
                startAngle = 205f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(cx - eyeDX - 5f * u + eyeLook, eyeY - 2f * u),
                size = Size(10f * u, 8f * u),
                style = Stroke(2.4f * u, cap = StrokeCap.Round),
            )
            drawArc(
                color = ivory,
                startAngle = 205f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(cx + eyeDX - 5f * u + eyeLook, eyeY - 2f * u),
                size = Size(10f * u, 8f * u),
                style = Stroke(2.4f * u, cap = StrokeCap.Round),
            )
        } else {
            drawOval(
                color = ivory.copy(alpha = if (state == CinePulseState.REST) .72f else .98f),
                topLeft = Offset(cx - eyeDX - eyeW / 2f + eyeLook, eyeY - normalEyeH / 2f),
                size = Size(eyeW, normalEyeH),
            )
            drawOval(
                color = ivory.copy(alpha = if (state == CinePulseState.REST) .72f else .98f),
                topLeft = Offset(cx + eyeDX - eyeW / 2f + eyeLook, eyeY - normalEyeH / 2f),
                size = Size(eyeW, normalEyeH),
            )
        }

        // Signature four-point spark.
        val sparkX = cx + 28f * u
        val sparkY = faceY - 24f * u + ribbonCycle * .8f * u
        val spark = Path().apply {
            moveTo(sparkX, sparkY - 5f * u)
            lineTo(sparkX + 2f * u, sparkY - 1.6f * u)
            lineTo(sparkX + 5f * u, sparkY)
            lineTo(sparkX + 2f * u, sparkY + 1.6f * u)
            lineTo(sparkX, sparkY + 5f * u)
            lineTo(sparkX - 2f * u, sparkY + 1.6f * u)
            lineTo(sparkX - 5f * u, sparkY)
            lineTo(sparkX - 2f * u, sparkY - 1.6f * u)
            close()
        }
        drawPath(spark, ivory.copy(alpha = .88f))
        drawPath(spark, gold.copy(alpha = .82f), style = Stroke(1f * u))

        // Wider pear-shaped body removes the old stick-figure read.
        val bodyTop = h * .50f + bodyLift
        val bodyBottom = h * .80f + bodyLift
        val bodyPath = Path().apply {
            moveTo(cx - 11.5f * u, bodyTop)
            cubicTo(cx - 18f * u, bodyTop + 8f * u, cx - 17f * u, bodyBottom - 7f * u, cx - 8f * u, bodyBottom)
            cubicTo(cx - 3f * u, bodyBottom + 2f * u, cx + 3f * u, bodyBottom + 2f * u, cx + 8f * u, bodyBottom)
            cubicTo(cx + 17f * u, bodyBottom - 7f * u, cx + 18f * u, bodyTop + 8f * u, cx + 11.5f * u, bodyTop)
            cubicTo(cx + 5f * u, bodyTop - 2f * u, cx - 5f * u, bodyTop - 2f * u, cx - 11.5f * u, bodyTop)
            close()
        }
        drawPath(
            path = bodyPath,
            brush = Brush.verticalGradient(listOf(Color(0xFF15131D), bodyBlack)),
        )
        drawPath(
            path = bodyPath,
            brush = Brush.linearGradient(
                listOf(hotPink.copy(alpha = .58f), orange.copy(alpha = .36f), aqua.copy(alpha = .54f)),
            ),
            style = Stroke(width = 1.25f * u),
        )

        // Heart core from the locked board.
        val heartX = cx
        val heartY = bodyTop + 12f * u
        val heart = Path().apply {
            moveTo(heartX, heartY + 4.4f * u)
            cubicTo(heartX - 10f * u, heartY - 1.4f * u, heartX - 4.6f * u, heartY - 7f * u, heartX, heartY - 2.2f * u)
            cubicTo(heartX + 4.6f * u, heartY - 7f * u, heartX + 10f * u, heartY - 1.4f * u, heartX, heartY + 4.4f * u)
            close()
        }
        drawCircle(gold.copy(alpha = .11f + .16f * glow), radius = 7.3f * u, center = Offset(heartX, heartY))
        drawPath(heart, ivory.copy(alpha = .96f))

        val shoulderY = bodyTop + 6f * u
        val leftShoulder = Offset(cx - 11.5f * u, shoulderY)
        val rightShoulder = Offset(cx + 11.5f * u, shoulderY)

        fun limb(start: Offset, end: Offset, accent: Color, width: Float = 6.3f * u) {
            drawLine(accent.copy(alpha = .22f), start, end, width + 3f * u, StrokeCap.Round)
            drawLine(bodyBlack, start, end, width, StrokeCap.Round)
            drawLine(accent.copy(alpha = .72f), start, end, 1.0f * u, StrokeCap.Round)
        }

        fun joint(point: Offset, accent: Color, radius: Float = 3.5f * u) {
            drawCircle(accent.copy(alpha = .20f), radius + 1.3f * u, point)
            drawCircle(bodyBlack, radius, point)
            drawCircle(accent.copy(alpha = .78f), .85f * u, point)
        }

        // Pose-specific arms. Each pose has a different silhouette, not just a moved hand.
        when (state) {
            CinePulseState.POINT -> {
                val shoulder = if (pointRight) rightShoulder else leftShoulder
                val elbow = Offset(shoulder.x + direction * 11f * u, shoulder.y + 2f * u)
                val hand = Offset(shoulder.x + direction * 27f * u, shoulder.y - 7f * u)
                limb(shoulder, elbow, if (pointRight) aqua else hotPink)
                limb(elbow, hand, gold, 5.7f * u)
                joint(hand, gold, 2.8f * u)
                val rest = if (pointRight) leftShoulder else rightShoulder
                limb(rest, Offset(rest.x - direction * 5f * u, rest.y + 18f * u), coral.copy(alpha = .75f))
            }

            CinePulseState.PRESENT -> {
                val active = if (pointRight) rightShoulder else leftShoulder
                val palm = Offset(active.x + direction * 24f * u, active.y + 4f * u)
                limb(active, Offset(active.x + direction * 11f * u, active.y + 8f * u), aqua)
                limb(Offset(active.x + direction * 11f * u, active.y + 8f * u), palm, gold, 5.8f * u)
                joint(palm, gold, 3.2f * u)
                val rest = if (pointRight) leftShoulder else rightShoulder
                limb(rest, Offset(rest.x - direction * 4f * u, rest.y + 17f * u), hotPink)
            }

            CinePulseState.WAVE -> {
                val waveAmount = sin(phase * Math.PI * 4.0).toFloat() * 3.5f * u
                val active = if (pointRight) rightShoulder else leftShoulder
                val elbow = Offset(active.x + direction * 10f * u, active.y - 7f * u)
                val hand = Offset(active.x + direction * (16f * u + waveAmount), active.y - 20f * u)
                limb(active, elbow, aqua)
                limb(elbow, hand, hotPink, 5.8f * u)
                joint(hand, gold, 3f * u)
                val rest = if (pointRight) leftShoulder else rightShoulder
                limb(rest, Offset(rest.x - direction * 5f * u, rest.y + 17f * u), orange)
            }

            CinePulseState.THINK -> {
                val chinHand = Offset(cx + direction * 18f * u, faceY + 14f * u)
                limb(rightShoulder, Offset(cx + direction * 15f * u, bodyTop + 11f * u), aqua)
                limb(Offset(cx + direction * 15f * u, bodyTop + 11f * u), chinHand, gold, 5.7f * u)
                joint(chinHand, gold, 2.8f * u)
                limb(leftShoulder, Offset(cx - 13f * u, bodyTop + 20f * u), hotPink)
            }

            CinePulseState.LISTEN -> {
                val earHand = Offset(cx + direction * 23f * u, faceY + 5f * u)
                limb(if (pointRight) rightShoulder else leftShoulder, earHand, aqua, 6f * u)
                joint(earHand, aqua, 3f * u)
                val rest = if (pointRight) leftShoulder else rightShoulder
                limb(rest, Offset(rest.x - direction * 3f * u, rest.y + 18f * u), hotPink)
            }

            CinePulseState.SUCCESS -> {
                val leftHand = Offset(cx - 6f * u, bodyTop + 15f * u)
                val rightHand = Offset(cx + 6f * u, bodyTop + 15f * u)
                limb(leftShoulder, leftHand, hotPink, 5.6f * u)
                limb(rightShoulder, rightHand, aqua, 5.6f * u)
                joint(leftHand, gold, 2.5f * u)
                joint(rightHand, gold, 2.5f * u)
            }

            CinePulseState.CELEBRATE -> {
                val leftHand = Offset(cx - 25f * u, bodyTop - 10f * u)
                val rightHand = Offset(cx + 25f * u, bodyTop - 10f * u)
                limb(leftShoulder, leftHand, hotPink, 6f * u)
                limb(rightShoulder, rightHand, aqua, 6f * u)
                joint(leftHand, hotPink, 3f * u)
                joint(rightHand, aqua, 3f * u)
            }

            CinePulseState.WALK -> {
                // Counter-swing is deliberately tiny; torso/head stay stable.
                val armSwing = cycle * 2.4f * u
                limb(leftShoulder, Offset(cx - 17f * u - armSwing, bodyTop + 18f * u), hotPink)
                limb(rightShoulder, Offset(cx + 17f * u - armSwing, bodyTop + 18f * u), aqua)
            }

            CinePulseState.LOOK -> {
                limb(leftShoulder, Offset(cx - 16f * u, bodyTop + 18f * u), hotPink)
                limb(rightShoulder, Offset(cx + 16f * u, bodyTop + 18f * u), aqua)
            }

            CinePulseState.NOD -> {
                limb(leftShoulder, Offset(cx - 14f * u, bodyTop + 17f * u), hotPink)
                limb(rightShoulder, Offset(cx + 14f * u, bodyTop + 17f * u), aqua)
            }

            CinePulseState.IDLE, CinePulseState.REST -> {
                limb(leftShoulder, Offset(cx - 15f * u, bodyTop + 18f * u), hotPink)
                limb(rightShoulder, Offset(cx + 15f * u, bodyTop + 18f * u), aqua)
            }
        }

        // Thick planted legs. Walking alternates lift and reach without bouncing the whole body.
        val hipY = bodyBottom - 2f * u
        val groundY = h * .94f
        if (state == CinePulseState.WALK) {
            val leftLift = max(0f, cycle) * 2.4f * u
            val rightLift = max(0f, opposite) * 2.4f * u
            val stride = cycle * 4.0f * u
            val leftFoot = Offset(cx - 7.5f * u + stride, groundY - leftLift)
            val rightFoot = Offset(cx + 7.5f * u - stride, groundY - rightLift)
            limb(Offset(cx - 6f * u, hipY), leftFoot, hotPink, 7f * u)
            limb(Offset(cx + 6f * u, hipY), rightFoot, aqua, 7f * u)
            drawOval(bodyBlack, Offset(leftFoot.x - 5.5f * u, leftFoot.y - 2.2f * u), Size(11f * u, 4.4f * u))
            drawOval(bodyBlack, Offset(rightFoot.x - 5.5f * u, rightFoot.y - 2.2f * u), Size(11f * u, 4.4f * u))
        } else {
            val leftFoot = Offset(cx - 8f * u, groundY)
            val rightFoot = Offset(cx + 8f * u, groundY)
            limb(Offset(cx - 6f * u, hipY), leftFoot, hotPink, 7f * u)
            limb(Offset(cx + 6f * u, hipY), rightFoot, aqua, 7f * u)
            drawOval(bodyBlack, Offset(leftFoot.x - 5.5f * u, leftFoot.y - 2.2f * u), Size(11f * u, 4.4f * u))
            drawOval(bodyBlack, Offset(rightFoot.x - 5.5f * u, rightFoot.y - 2.2f * u), Size(11f * u, 4.4f * u))
        }

        if (state == CinePulseState.THINK) {
            repeat(3) { i ->
                drawCircle(
                    color = gold.copy(alpha = .52f + i * .14f),
                    radius = (1f + i * .35f) * u,
                    center = Offset(cx + direction * (27f + i * 4f) * u, faceY - (14f + i * 5f) * u),
                )
            }
        }

        if (state == CinePulseState.CELEBRATE) {
            repeat(6) { i ->
                val angle = (i * 60f + phase * 20f) * 0.017453292f
                val r = 39f * u
                drawCircle(
                    color = when (i % 3) {
                        0 -> hotPink
                        1 -> aqua
                        else -> gold
                    }.copy(alpha = .80f),
                    radius = 1.5f * u,
                    center = Offset(cx + cos(angle) * r, faceY + sin(angle) * r),
                )
            }
        }
    }
}
