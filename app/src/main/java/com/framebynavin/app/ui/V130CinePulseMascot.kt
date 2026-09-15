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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val CinePink = Color(0xFFFF3D88)
private val CineCoral = Color(0xFFFF6A54)
private val CineOrange = Color(0xFFFF9A4D)
private val CineAqua = Color(0xFF4FE4E7)
private val CineGold = Color(0xFFFFD39A)
private val CineIvory = Color(0xFFFFF1D8)
private val CineInk = Color(0xFF05060A)
private val CineInkSoft = Color(0xFF12131A)

/**
 * v130 production mascot renderer.
 *
 * The locked Cine Pulse board is the source of truth: oval black face, luminous vertical eyes,
 * glowing heart, multicolour translucent ribbon crown/tail, soft tapered body and rounded limbs.
 * App state drives pose; no GIF/video loop is used.
 */
@Composable
internal fun CinePulseMascotV130(
    state: CinePulseState,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    val motion = rememberInfiniteTransition(label = "cinePulseV130")
    val phase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == CinePulseState.WALK) 840 else 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cinePulsePhaseV130",
    )
    val ribbonPhase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "cinePulseRibbonV130",
    )
    val breathe by motion.animateFloat(
        initialValue = .985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cinePulseBreatheV130",
    )
    val glow by motion.animateFloat(
        initialValue = .64f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cinePulseGlowV130",
    )

    val walkWave = sin(phase * PI * 2.0).toFloat()
    val bob = when (state) {
        CinePulseState.WALK -> kotlin.math.abs(walkWave) * -2.2f
        CinePulseState.CELEBRATE -> -3.5f * kotlin.math.max(0f, sin(phase * PI * 2.0).toFloat())
        CinePulseState.REST -> 0f
        else -> (breathe - 1f) * 16f
    }
    val tilt = when (state) {
        CinePulseState.POINT -> if (pointRight) -2.2f else 2.2f
        CinePulseState.THINK -> if (pointRight) 3f else -3f
        CinePulseState.WALK -> walkWave * 1.4f
        else -> 0f
    }

    Canvas(
        modifier.graphicsLayer {
            translationY = bob
            rotationZ = tilt
            scaleX = if (state == CinePulseState.REST) .985f else 1f
            scaleY = if (state == CinePulseState.REST) .985f else 1f
        },
    ) {
        val u = size.minDimension / 100f
        val cx = size.width * .5f
        val faceY = size.height * .30f
        val faceW = 58f * u
        val faceH = 38f * u
        val bodyTop = size.height * .52f
        val bodyBottom = size.height * .84f
        val direction = if (pointRight) 1f else -1f
        val palette = Brush.linearGradient(
            colors = listOf(CinePink, CineCoral, CineOrange, CineAqua, CinePink),
            start = Offset(cx - 38f * u, faceY - 20f * u),
            end = Offset(cx + 40f * u, faceY + 24f * u),
        )

        // Quiet atmosphere behind the mascot.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CinePink.copy(alpha = .12f * glow),
                    CineAqua.copy(alpha = .09f * glow),
                    CineOrange.copy(alpha = .05f * glow),
                    Color.Transparent,
                ),
                center = Offset(cx, faceY + 16f * u),
                radius = 54f * u,
            ),
            radius = 54f * u,
            center = Offset(cx, faceY + 16f * u),
        )

        // Rear ribbon tail — the board's scarf-like light trail, not a ring.
        val tail = Path().apply {
            moveTo(cx - 17f * u, faceY + 10f * u)
            cubicTo(
                cx - 40f * u,
                faceY + 22f * u,
                cx - 34f * u,
                bodyTop + 28f * u,
                cx - 16f * u,
                bodyTop + 23f * u,
            )
            cubicTo(
                cx + 2f * u,
                bodyTop + 19f * u,
                cx + 19f * u,
                bodyTop + 35f * u,
                cx + 29f * u,
                bodyBottom - 4f * u,
            )
        }
        drawPath(tail, CinePink.copy(alpha = .11f * glow), style = Stroke(14f * u, cap = StrokeCap.Round))
        drawPath(tail, palette, style = Stroke(6.6f * u, cap = StrokeCap.Round))
        drawPath(tail, CineIvory.copy(alpha = .22f), style = Stroke(1.1f * u, cap = StrokeCap.Round))

        // Three independent crown ribbons. Their control points drift subtly instead of rotating as a rigid ring.
        val driftA = sin(ribbonPhase * PI * 2.0).toFloat() * 2.1f * u
        val driftB = cos(ribbonPhase * PI * 2.0).toFloat() * 1.8f * u

        val backRibbon = Path().apply {
            moveTo(cx - 34f * u, faceY - 4f * u)
            cubicTo(cx - 23f * u, faceY - 27f * u + driftA, cx + 20f * u, faceY - 27f * u - driftB, cx + 34f * u, faceY - 9f * u)
            cubicTo(cx + 43f * u, faceY + 2f * u, cx + 31f * u, faceY + 10f * u, cx + 21f * u, faceY + 8f * u)
        }
        drawPath(backRibbon, CineAqua.copy(alpha = .12f), style = Stroke(14f * u, cap = StrokeCap.Round))
        drawPath(backRibbon, palette, style = Stroke(7.4f * u, cap = StrokeCap.Round))
        drawPath(backRibbon, CineIvory.copy(alpha = .25f), style = Stroke(1.1f * u, cap = StrokeCap.Round))

        val frontRibbon = Path().apply {
            moveTo(cx + 30f * u, faceY - 13f * u)
            cubicTo(cx + 40f * u, faceY + 1f * u, cx + 25f * u, faceY + 21f * u, cx + 1f * u, faceY + 21f * u)
            cubicTo(cx - 26f * u, faceY + 22f * u, cx - 42f * u, faceY + 9f * u, cx - 32f * u, faceY - 5f * u)
        }
        drawPath(frontRibbon, CineCoral.copy(alpha = .12f), style = Stroke(13f * u, cap = StrokeCap.Round))
        drawPath(frontRibbon, palette, style = Stroke(6.4f * u, cap = StrokeCap.Round))
        drawPath(frontRibbon, CineIvory.copy(alpha = .18f), style = Stroke(.9f * u, cap = StrokeCap.Round))

        val accentRibbon = Path().apply {
            moveTo(cx - 28f * u, faceY + 7f * u)
            cubicTo(cx - 8f * u, faceY - 4f * u + driftB, cx + 20f * u, faceY - 2f * u - driftA, cx + 31f * u, faceY + 7f * u)
        }
        drawPath(accentRibbon, CinePink.copy(alpha = .18f), style = Stroke(8f * u, cap = StrokeCap.Round))
        drawPath(accentRibbon, Brush.linearGradient(listOf(CinePink, CineOrange, CineAqua)), style = Stroke(3.4f * u, cap = StrokeCap.Round))

        // Face: horizontal soft black oval with a luminous gradient rim.
        val faceTopLeft = Offset(cx - faceW / 2f, faceY - faceH / 2f)
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF181820), CineInkSoft, CineInk),
                center = Offset(cx - 4f * u, faceY - 5f * u),
                radius = 38f * u,
            ),
            topLeft = faceTopLeft,
            size = Size(faceW, faceH),
        )
        drawOval(
            brush = palette,
            topLeft = faceTopLeft,
            size = Size(faceW, faceH),
            style = Stroke(width = 1.75f * u),
        )
        drawArc(
            color = CineIvory.copy(alpha = .30f * glow),
            startAngle = 205f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = faceTopLeft + Offset(1.8f * u, 1.8f * u),
            size = Size(faceW - 3.6f * u, faceH - 3.6f * u),
            style = Stroke(.8f * u, cap = StrokeCap.Round),
        )

        // Eyes: warm vertical capsules from the locked board; celebrate state turns them into smiling arcs.
        val eyeY = faceY + .8f * u
        val eyeDx = 10.8f * u
        val blink = state != CinePulseState.CELEBRATE && state != CinePulseState.SUCCESS && phase > .93f && phase < .975f
        if (state == CinePulseState.CELEBRATE || state == CinePulseState.SUCCESS) {
            drawSmileEye(Offset(cx - eyeDx, eyeY), u)
            drawSmileEye(Offset(cx + eyeDx, eyeY), u)
        } else {
            val eyeH = if (state == CinePulseState.REST || blink) 2.4f * u else 11.4f * u
            val eyeW = 5.4f * u
            listOf(cx - eyeDx, cx + eyeDx).forEach { x ->
                drawOval(CineGold.copy(alpha = .16f * glow), Offset(x - 5f * u, eyeY - 9f * u), Size(10f * u, 18f * u))
                drawOval(
                    brush = Brush.verticalGradient(listOf(Color.White, CineIvory, CineGold)),
                    topLeft = Offset(x - eyeW / 2f, eyeY - eyeH / 2f),
                    size = Size(eyeW, eyeH),
                )
            }
        }

        // Signature four-point spark anchored to the crown.
        val sparkX = cx + 31f * u
        val sparkY = faceY - 22f * u + sin(ribbonPhase * PI * 2.0).toFloat() * 1.4f * u
        drawCircle(CineOrange.copy(alpha = .12f * glow), 9f * u, Offset(sparkX, sparkY))
        drawPath(starPath(sparkX, sparkY, 6.5f * u, 2.4f * u), CineGold)
        drawPath(starPath(sparkX, sparkY, 6.5f * u, 2.4f * u), CineCoral, style = Stroke(1.1f * u))

        // Body silhouette: tapered and soft, matching the board instead of a rectangular torso.
        val body = Path().apply {
            moveTo(cx - 11f * u, bodyTop)
            cubicTo(cx - 17f * u, bodyTop + 8f * u, cx - 15f * u, bodyBottom - 7f * u, cx - 7f * u, bodyBottom)
            cubicTo(cx - 2f * u, bodyBottom + 4f * u, cx + 2f * u, bodyBottom + 4f * u, cx + 7f * u, bodyBottom)
            cubicTo(cx + 15f * u, bodyBottom - 7f * u, cx + 17f * u, bodyTop + 8f * u, cx + 11f * u, bodyTop)
            cubicTo(cx + 6f * u, bodyTop - 4f * u, cx - 6f * u, bodyTop - 4f * u, cx - 11f * u, bodyTop)
            close()
        }
        drawPath(body, Brush.verticalGradient(listOf(Color(0xFF171820), CineInk)))
        drawPath(body, Brush.linearGradient(listOf(CineAqua, CinePink, CineOrange)), style = Stroke(1.35f * u))

        // Heart core.
        val heartCenter = Offset(cx, bodyTop + 13.2f * u)
        drawCircle(CineOrange.copy(alpha = .12f * glow), 8.5f * u, heartCenter)
        drawPath(heartPath(heartCenter.x, heartCenter.y, 4.5f * u), CineGold)
        drawPath(heartPath(heartCenter.x, heartCenter.y, 4.5f * u), CineIvory.copy(alpha = .72f), style = Stroke(.6f * u))

        // Pose-specific rounded limbs. Each limb is black with a luminous edge, never a stick line.
        val leftShoulder = Offset(cx - 10f * u, bodyTop + 6f * u)
        val rightShoulder = Offset(cx + 10f * u, bodyTop + 6f * u)
        val limbEdge = Brush.linearGradient(listOf(CinePink, CineOrange, CineAqua))
        val swing = if (state == CinePulseState.WALK) walkWave * 8f * u else 0f

        when (state) {
            CinePulseState.POINT -> {
                val pointShoulder = if (pointRight) rightShoulder else leftShoulder
                val restShoulder = if (pointRight) leftShoulder else rightShoulder
                limb(
                    path = Path().apply {
                        moveTo(pointShoulder.x, pointShoulder.y)
                        cubicTo(pointShoulder.x + direction * 8f * u, pointShoulder.y - 2f * u, pointShoulder.x + direction * 16f * u, pointShoulder.y - 8f * u, pointShoulder.x + direction * 27f * u, pointShoulder.y - 11f * u)
                    },
                    u = u,
                    edge = limbEdge,
                )
                drawCircle(CineGold, 2.5f * u, Offset(pointShoulder.x + direction * 28.2f * u, pointShoulder.y - 11.3f * u))
                limb(restingArm(restShoulder, -direction, u), u, limbEdge, alpha = .78f)
            }
            CinePulseState.CELEBRATE -> {
                limb(raisedArm(leftShoulder, -1f, u), u, limbEdge)
                limb(raisedArm(rightShoulder, 1f, u), u, limbEdge)
                drawCircle(CineAqua, 2.2f * u, Offset(cx - 26f * u, bodyTop - 12f * u))
                drawCircle(CineOrange, 2.2f * u, Offset(cx + 26f * u, bodyTop - 12f * u))
            }
            CinePulseState.THINK -> {
                limb(restingArm(leftShoulder, -1f, u), u, limbEdge, alpha = .78f)
                limb(
                    Path().apply {
                        moveTo(rightShoulder.x, rightShoulder.y)
                        cubicTo(rightShoulder.x + 7f * u, rightShoulder.y - 3f * u, cx + 17f * u, faceY + 13f * u, cx + 20f * u, faceY + 11f * u)
                    },
                    u,
                    limbEdge,
                )
                drawCircle(CineGold, 2.1f * u, Offset(cx + 20f * u, faceY + 11f * u))
            }
            CinePulseState.LISTEN -> {
                limb(openArm(leftShoulder, -1f, u), u, limbEdge)
                limb(openArm(rightShoulder, 1f, u), u, limbEdge)
            }
            CinePulseState.SUCCESS -> {
                limb(restingArm(leftShoulder, -1f, u), u, limbEdge, alpha = .78f)
                limb(openArm(rightShoulder, 1f, u), u, limbEdge)
            }
            else -> {
                limb(restingArm(leftShoulder, -1f, u, swing), u, limbEdge, alpha = .80f)
                limb(restingArm(rightShoulder, 1f, u, -swing), u, limbEdge, alpha = .80f)
            }
        }

        // Legs. Walking uses bent curves and opposing stride instead of sliding straight lines.
        val hipLeft = Offset(cx - 5f * u, bodyBottom - 1f * u)
        val hipRight = Offset(cx + 5f * u, bodyBottom - 1f * u)
        if (state == CinePulseState.WALK) {
            val stride = walkWave * 9f * u
            val leftLeg = Path().apply {
                moveTo(hipLeft.x, hipLeft.y)
                cubicTo(hipLeft.x - 2f * u, hipLeft.y + 7f * u, cx - 8f * u + stride * .35f, hipLeft.y + 12f * u, cx - 11f * u + stride, hipLeft.y + 17f * u)
            }
            val rightLeg = Path().apply {
                moveTo(hipRight.x, hipRight.y)
                cubicTo(hipRight.x + 2f * u, hipRight.y + 7f * u, cx + 8f * u - stride * .35f, hipRight.y + 12f * u, cx + 11f * u - stride, hipRight.y + 17f * u)
            }
            limb(leftLeg, u, limbEdge)
            limb(rightLeg, u, limbEdge)
        } else {
            limb(
                Path().apply {
                    moveTo(hipLeft.x, hipLeft.y)
                    cubicTo(hipLeft.x - 2f * u, hipLeft.y + 7f * u, cx - 7f * u, hipLeft.y + 13f * u, cx - 7.5f * u, hipLeft.y + 17f * u)
                },
                u,
                limbEdge,
                width = 4.8f,
            )
            limb(
                Path().apply {
                    moveTo(hipRight.x, hipRight.y)
                    cubicTo(hipRight.x + 2f * u, hipRight.y + 7f * u, cx + 7f * u, hipRight.y + 13f * u, cx + 7.5f * u, hipRight.y + 17f * u)
                },
                u,
                limbEdge,
                width = 4.8f,
            )
        }

        // Celebration accents from the concept board.
        if (state == CinePulseState.CELEBRATE) {
            repeat(5) { i ->
                val angle = (i * 72f - 95f + ribbonPhase * 18f) * (PI / 180.0)
                val radius = (39f + (i % 2) * 5f) * u
                val center = Offset(
                    cx + cos(angle).toFloat() * radius,
                    faceY + sin(angle).toFloat() * radius,
                )
                if (i == 1) {
                    drawPath(starPath(center.x, center.y, 4.2f * u, 1.5f * u), CineGold)
                } else {
                    drawCircle(if (i % 2 == 0) CinePink else CineAqua, 1.45f * u, center)
                }
            }
        }
    }
}

private fun DrawScope.drawSmileEye(center: Offset, u: Float) {
    drawCircle(CineGold.copy(alpha = .11f), 7f * u, center)
    drawArc(
        color = CineIvory,
        startAngle = 205f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(center.x - 5f * u, center.y - 3.5f * u),
        size = Size(10f * u, 7f * u),
        style = Stroke(2.1f * u, cap = StrokeCap.Round),
    )
}

private fun DrawScope.limb(
    path: Path,
    u: Float,
    edge: Brush,
    alpha: Float = 1f,
    width: Float = 6.4f,
) {
    drawPath(path, edge, alpha = .72f * alpha, style = Stroke((width + 2.2f) * u, cap = StrokeCap.Round))
    drawPath(path, CineInk, alpha = alpha, style = Stroke(width * u, cap = StrokeCap.Round))
    drawPath(path, CineIvory.copy(alpha = .08f * alpha), style = Stroke(.7f * u, cap = StrokeCap.Round))
}

private fun restingArm(shoulder: Offset, side: Float, u: Float, swing: Float = 0f): Path = Path().apply {
    moveTo(shoulder.x, shoulder.y)
    cubicTo(
        shoulder.x + side * 5f * u,
        shoulder.y + 6f * u,
        shoulder.x + side * 8f * u + swing * .35f,
        shoulder.y + 14f * u,
        shoulder.x + side * 7f * u + swing,
        shoulder.y + 19f * u,
    )
}

private fun openArm(shoulder: Offset, side: Float, u: Float): Path = Path().apply {
    moveTo(shoulder.x, shoulder.y)
    cubicTo(
        shoulder.x + side * 7f * u,
        shoulder.y + 1f * u,
        shoulder.x + side * 14f * u,
        shoulder.y - 2f * u,
        shoulder.x + side * 20f * u,
        shoulder.y - 5f * u,
    )
}

private fun raisedArm(shoulder: Offset, side: Float, u: Float): Path = Path().apply {
    moveTo(shoulder.x, shoulder.y)
    cubicTo(
        shoulder.x + side * 7f * u,
        shoulder.y - 3f * u,
        shoulder.x + side * 15f * u,
        shoulder.y - 9f * u,
        shoulder.x + side * 22f * u,
        shoulder.y - 18f * u,
    )
}

private fun heartPath(cx: Float, cy: Float, s: Float): Path = Path().apply {
    moveTo(cx, cy + s * .9f)
    cubicTo(cx - s * 1.45f, cy + s * .05f, cx - s * 1.1f, cy - s * 1.05f, cx - s * .45f, cy - s * .72f)
    cubicTo(cx - s * .12f, cy - s * .55f, cx, cy - s * .22f, cx, cy - s * .08f)
    cubicTo(cx, cy - s * .22f, cx + s * .12f, cy - s * .55f, cx + s * .45f, cy - s * .72f)
    cubicTo(cx + s * 1.1f, cy - s * 1.05f, cx + s * 1.45f, cy + s * .05f, cx, cy + s * .9f)
    close()
}

private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float): Path = Path().apply {
    moveTo(cx, cy - outer)
    lineTo(cx + inner, cy - inner)
    lineTo(cx + outer, cy)
    lineTo(cx + inner, cy + inner)
    lineTo(cx, cy + outer)
    lineTo(cx - inner, cy + inner)
    lineTo(cx - outer, cy)
    lineTo(cx - inner, cy - inner)
    close()
}
