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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.ProjectorIvory
import kotlin.math.PI
import kotlin.math.abs
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
 * Backlot Frame guide.
 *
 * Intentionally built only from Compose Canvas geometry: no generated bitmap, glow, texture,
 * external rig or 3D asset. This keeps the mascot deterministic, crisp at every size and safe to
 * theme/animate inside the app. The legacy CinePulseState API is preserved so existing tour,
 * onboarding and empty-state behavior keeps working without product-logic changes.
 */
@Composable
internal fun CinePulseGuide(
    state: CinePulseState,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "backlotFrameGuide")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == CinePulseState.WALK) 950 else 3200,
                easing = LinearEasing,
            ),
        ),
        label = "framePhase",
    )
    val breathe by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "frameBreath",
    )

    Canvas(modifier = modifier) {
        val u = size.minDimension / 100f
        val cx = size.width / 2f
        val direction = if (pointRight) 1f else -1f
        val wave = sin(phase * PI * 2.0).toFloat()
        val walk = sin(phase * PI * 2.0).toFloat()
        val tinyLift = when (state) {
            CinePulseState.WALK -> abs(walk) * 1.2f * u
            CinePulseState.CELEBRATE, CinePulseState.SUCCESS -> abs(wave) * .8f * u
            else -> breathe * .35f * u
        }

        val gold = MutedGold
        val ivory = ProjectorIvory
        val face = Color(0xFF090A0C)
        val body = Color(0xFF111216)
        val outline = Color(0xFF25272D)
        val shoe = Color(0xFF08090B)
        val accent = gold

        val headCenter = Offset(cx, size.height * .30f + tinyLift)
        val headW = 60f * u
        val headH = 39f * u
        val headTilt = when (state) {
            CinePulseState.THINK -> -5f * direction
            CinePulseState.LISTEN -> 4f * direction
            CinePulseState.LOOK -> wave * 3f
            CinePulseState.NOD -> 0f
            else -> 0f
        }
        val nodShift = if (state == CinePulseState.NOD) ((wave + 1f) * .5f) * 2.2f * u else 0f
        val headY = headCenter.y + nodShift

        // Flat, deterministic frame head. The outer gold slab is the character's identity.
        rotate(degrees = headTilt, pivot = Offset(cx, headY)) {
            drawRoundRect(
                color = accent,
                topLeft = Offset(cx - headW / 2f, headY - headH / 2f),
                size = Size(headW, headH),
                cornerRadius = CornerRadius(10f * u, 10f * u),
            )
            val inset = 3.2f * u
            drawRoundRect(
                color = face,
                topLeft = Offset(cx - headW / 2f + inset, headY - headH / 2f + inset),
                size = Size(headW - inset * 2f, headH - inset * 2f),
                cornerRadius = CornerRadius(8f * u, 8f * u),
            )
            drawRoundRect(
                color = outline,
                topLeft = Offset(cx - headW / 2f + inset, headY - headH / 2f + inset),
                size = Size(headW - inset * 2f, headH - inset * 2f),
                cornerRadius = CornerRadius(8f * u, 8f * u),
                style = Stroke(width = 1f * u),
            )

            val eyeBaseY = headY + .7f * u
            val eyeDx = 11.2f * u
            val lookX = when (state) {
                CinePulseState.POINT, CinePulseState.PRESENT -> direction * 2.3f * u
                CinePulseState.LOOK -> wave * 3.0f * u
                CinePulseState.THINK -> direction * 1.0f * u
                CinePulseState.LISTEN -> -direction * .8f * u
                else -> 0f
            }
            val blink = phase > .945f && phase < .985f && state !in setOf(CinePulseState.SUCCESS, CinePulseState.CELEBRATE)

            fun neutralEye(x: Float) {
                if (blink || state == CinePulseState.REST) {
                    drawLine(
                        color = ivory.copy(alpha = if (state == CinePulseState.REST) .72f else 1f),
                        start = Offset(x - 2.4f * u, eyeBaseY),
                        end = Offset(x + 2.4f * u, eyeBaseY),
                        strokeWidth = 2.2f * u,
                        cap = StrokeCap.Round,
                    )
                } else if (state == CinePulseState.THINK) {
                    drawLine(
                        color = ivory,
                        start = Offset(x - 2.8f * u, eyeBaseY + .8f * u),
                        end = Offset(x + 2.8f * u, eyeBaseY - .2f * u),
                        strokeWidth = 2.2f * u,
                        cap = StrokeCap.Round,
                    )
                } else {
                    val eyeH = if (state == CinePulseState.PRESENT) 12.3f * u else 11.0f * u
                    val eyeW = if (state == CinePulseState.PRESENT) 5.5f * u else 4.8f * u
                    drawOval(
                        color = ivory,
                        topLeft = Offset(x - eyeW / 2f, eyeBaseY - eyeH / 2f),
                        size = Size(eyeW, eyeH),
                    )
                }
            }

            if (state == CinePulseState.SUCCESS || state == CinePulseState.CELEBRATE) {
                val arcSize = Size(10f * u, 8f * u)
                drawArc(
                    color = ivory,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(cx - eyeDx - 5f * u + lookX, eyeBaseY - 2f * u),
                    size = arcSize,
                    style = Stroke(width = 2.4f * u, cap = StrokeCap.Round),
                )
                drawArc(
                    color = ivory,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(cx + eyeDx - 5f * u + lookX, eyeBaseY - 2f * u),
                    size = arcSize,
                    style = Stroke(width = 2.4f * u, cap = StrokeCap.Round),
                )
            } else {
                neutralEye(cx - eyeDx + lookX)
                neutralEye(cx + eyeDx + lookX)
            }
        }

        // Neck is deliberately tiny so the head remains the graphic focus.
        drawRoundRect(
            color = body,
            topLeft = Offset(cx - 4.2f * u, size.height * .485f + tinyLift),
            size = Size(8.4f * u, 8f * u),
            cornerRadius = CornerRadius(3f * u, 3f * u),
        )

        val torsoTop = size.height * .51f + tinyLift
        val torsoW = 28f * u
        val torsoH = 28f * u
        drawRoundRect(
            color = body,
            topLeft = Offset(cx - torsoW / 2f, torsoTop),
            size = Size(torsoW, torsoH),
            cornerRadius = CornerRadius(8f * u, 8f * u),
        )
        drawRoundRect(
            color = outline,
            topLeft = Offset(cx - torsoW / 2f, torsoTop),
            size = Size(torsoW, torsoH),
            cornerRadius = CornerRadius(8f * u, 8f * u),
            style = Stroke(width = 1.1f * u),
        )

        // Small Backlot chest mark: a miniature frame rather than text, so it stays vector-clean.
        drawRoundRect(
            color = accent,
            topLeft = Offset(cx - 4.4f * u, torsoTop + 8.2f * u),
            size = Size(8.8f * u, 7.2f * u),
            cornerRadius = CornerRadius(2f * u, 2f * u),
        )
        drawRoundRect(
            color = body,
            topLeft = Offset(cx - 2.6f * u, torsoTop + 10f * u),
            size = Size(5.2f * u, 3.6f * u),
            cornerRadius = CornerRadius(1f * u, 1f * u),
        )

        val leftShoulder = Offset(cx - 12.5f * u, torsoTop + 7f * u)
        val rightShoulder = Offset(cx + 12.5f * u, torsoTop + 7f * u)

        fun arm(start: Offset, elbow: Offset, hand: Offset) {
            drawLine(body, start, elbow, 6.0f * u, StrokeCap.Round)
            drawLine(body, elbow, hand, 5.4f * u, StrokeCap.Round)
            drawLine(outline, start, elbow, 1.0f * u, StrokeCap.Round)
            drawLine(outline, elbow, hand, 1.0f * u, StrokeCap.Round)
            drawCircle(accent, radius = 3.1f * u, center = hand)
        }

        val idleSwing = if (state == CinePulseState.WALK) walk * 3f * u else 0f
        when (state) {
            CinePulseState.WAVE -> {
                arm(leftShoulder, Offset(cx - 16f * u, torsoTop + 17f * u), Offset(cx - 16f * u, torsoTop + 24f * u))
                arm(
                    rightShoulder,
                    Offset(cx + 18f * u, torsoTop + 2f * u),
                    Offset(cx + 23f * u + wave * 1.5f * u, torsoTop - 8f * u),
                )
            }
            CinePulseState.POINT -> {
                val pointingShoulder = if (pointRight) rightShoulder else leftShoulder
                val restingShoulder = if (pointRight) leftShoulder else rightShoulder
                arm(
                    pointingShoulder,
                    Offset(cx + direction * 19f * u, torsoTop + 8f * u),
                    Offset(cx + direction * 30f * u, torsoTop + 5f * u),
                )
                arm(
                    restingShoulder,
                    Offset(cx - direction * 16f * u, torsoTop + 17f * u),
                    Offset(cx - direction * 14f * u, torsoTop + 24f * u),
                )
                drawLine(
                    color = accent,
                    start = Offset(cx + direction * 30f * u, torsoTop + 5f * u),
                    end = Offset(cx + direction * 35f * u, torsoTop + 4f * u),
                    strokeWidth = 1.8f * u,
                    cap = StrokeCap.Round,
                )
            }
            CinePulseState.PRESENT -> {
                arm(leftShoulder, Offset(cx - 20f * u, torsoTop + 11f * u), Offset(cx - 25f * u, torsoTop + 8f * u))
                arm(rightShoulder, Offset(cx + 20f * u, torsoTop + 11f * u), Offset(cx + 25f * u, torsoTop + 8f * u))
            }
            CinePulseState.THINK -> {
                arm(leftShoulder, Offset(cx - 16f * u, torsoTop + 17f * u), Offset(cx - 14f * u, torsoTop + 24f * u))
                arm(rightShoulder, Offset(cx + 17f * u, torsoTop + 7f * u), Offset(cx + 15f * u, torsoTop - 1f * u))
            }
            CinePulseState.SUCCESS, CinePulseState.CELEBRATE -> {
                arm(leftShoulder, Offset(cx - 18f * u, torsoTop + 2f * u), Offset(cx - 22f * u, torsoTop - 8f * u))
                arm(rightShoulder, Offset(cx + 18f * u, torsoTop + 2f * u), Offset(cx + 22f * u, torsoTop - 8f * u))
            }
            else -> {
                arm(
                    leftShoulder,
                    Offset(cx - 16f * u + idleSwing, torsoTop + 16f * u),
                    Offset(cx - 14f * u + idleSwing, torsoTop + 24f * u),
                )
                arm(
                    rightShoulder,
                    Offset(cx + 16f * u - idleSwing, torsoTop + 16f * u),
                    Offset(cx + 14f * u - idleSwing, torsoTop + 24f * u),
                )
            }
        }

        // Compact legs and shoes keep the character friendly without becoming a cartoon body rig.
        val hipY = torsoTop + torsoH - 1f * u
        val legSwing = if (state == CinePulseState.WALK) walk * 4.8f * u else 0f
        val leftFoot = Offset(cx - 8f * u + legSwing, size.height * .92f)
        val rightFoot = Offset(cx + 8f * u - legSwing, size.height * .92f)
        drawLine(
            color = body,
            start = Offset(cx - 6f * u, hipY),
            end = Offset(leftFoot.x, leftFoot.y - 4f * u),
            strokeWidth = 6.5f * u,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = body,
            start = Offset(cx + 6f * u, hipY),
            end = Offset(rightFoot.x, rightFoot.y - 4f * u),
            strokeWidth = 6.5f * u,
            cap = StrokeCap.Round,
        )
        drawOval(
            color = shoe,
            topLeft = Offset(leftFoot.x - 6.2f * u, leftFoot.y - 4f * u),
            size = Size(12.4f * u, 5.2f * u),
        )
        drawOval(
            color = shoe,
            topLeft = Offset(rightFoot.x - 6.2f * u, rightFoot.y - 4f * u),
            size = Size(12.4f * u, 5.2f * u),
        )
        drawLine(
            color = accent.copy(alpha = .85f),
            start = Offset(leftFoot.x - 4.5f * u, leftFoot.y - 1.5f * u),
            end = Offset(leftFoot.x + 3.8f * u, leftFoot.y - 1.5f * u),
            strokeWidth = .8f * u,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = accent.copy(alpha = .85f),
            start = Offset(rightFoot.x - 4.5f * u, rightFoot.y - 1.5f * u),
            end = Offset(rightFoot.x + 3.8f * u, rightFoot.y - 1.5f * u),
            strokeWidth = .8f * u,
            cap = StrokeCap.Round,
        )
    }
}
