package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*
import kotlin.math.sin

enum class FrameGuidePose { IDLE, WALK, POINT, CELEBRATE }

/**
 * Original minimal 2D FrameByNavin guide. It is intentionally drawn with Compose primitives so
 * it stays tiny, theme-aware and crisp at every density without shipping a heavy animation SDK.
 */
@Composable
internal fun FrameGuideCompanion(
    pose: FrameGuidePose,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    val motion = rememberInfiniteTransition(label = "frameGuide")
    val phase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (pose == FrameGuidePose.WALK) 560 else 1800, easing = LinearEasing)),
        label = "phase",
    )
    val breathe by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val bob = when (pose) {
        FrameGuidePose.WALK -> sin(phase * Math.PI * 4).toFloat() * 2.3f
        FrameGuidePose.CELEBRATE -> -3.5f * sin(phase * Math.PI).toFloat().coerceAtLeast(0f)
        else -> (breathe - .5f) * 2f
    }

    Canvas(
        modifier.graphicsLayer { translationY = bob }
    ) {
        val w = size.width
        val h = size.height
        val cx = w * .50f
        val headY = h * .28f
        val unit = size.minDimension / 100f
        val primary = RecRed
        val secondary = MutedGold
        val foreground = ProjectorIvory
        val body = CinemaSurfaceRaised

        drawCircle(
            brush = Brush.radialGradient(
                listOf(primary.copy(alpha = .22f), secondary.copy(alpha = .07f), Color.Transparent),
                center = Offset(cx, h * .45f),
                radius = w * .58f,
            ),
            radius = w * .58f,
            center = Offset(cx, h * .45f),
        )

        // Frame-shaped head / viewfinder.
        drawRoundRect(
            color = body,
            topLeft = Offset(cx - 21 * unit, headY - 17 * unit),
            size = Size(42 * unit, 34 * unit),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(9 * unit),
        )
        drawRoundRect(
            color = primary.copy(alpha = .92f),
            topLeft = Offset(cx - 21 * unit, headY - 17 * unit),
            size = Size(42 * unit, 34 * unit),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(9 * unit),
            style = Stroke(width = 2.2f * unit),
        )
        drawLine(
            secondary.copy(alpha = .85f),
            Offset(cx - 13 * unit, headY - 8 * unit),
            Offset(cx + 11 * unit, headY - 8 * unit),
            strokeWidth = 2 * unit,
            cap = StrokeCap.Round,
        )
        drawCircle(primary, 4.2f * unit, Offset(cx + 10 * unit, headY + 3 * unit))
        drawCircle(foreground.copy(alpha = .92f), 1.25f * unit, Offset(cx + 11.2f * unit, headY + 1.8f * unit))

        // Neck + compact jacket body.
        drawLine(foreground.copy(alpha = .45f), Offset(cx, headY + 17 * unit), Offset(cx, h * .52f), 3 * unit, StrokeCap.Round)
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(primary.copy(alpha = .30f), body)),
            topLeft = Offset(cx - 17 * unit, h * .48f),
            size = Size(34 * unit, 29 * unit),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10 * unit),
        )
        drawLine(secondary.copy(alpha = .75f), Offset(cx - 7 * unit, h * .53f), Offset(cx + 8 * unit, h * .53f), 1.7f * unit, StrokeCap.Round)

        val walkSwing = if (pose == FrameGuidePose.WALK) sin(phase * Math.PI * 4).toFloat() * 7f * unit else 0f
        val leftShoulder = Offset(cx - 14 * unit, h * .53f)
        val rightShoulder = Offset(cx + 14 * unit, h * .53f)
        val point = if (pointRight) 1f else -1f

        if (pose == FrameGuidePose.POINT) {
            val shoulder = if (pointRight) rightShoulder else leftShoulder
            val elbow = Offset(shoulder.x + point * 13 * unit, shoulder.y - 4 * unit)
            val hand = Offset(shoulder.x + point * 29 * unit, shoulder.y - 12 * unit)
            drawLine(foreground.copy(alpha = .82f), shoulder, elbow, 3.2f * unit, StrokeCap.Round)
            drawLine(foreground.copy(alpha = .82f), elbow, hand, 3.2f * unit, StrokeCap.Round)
            drawCircle(secondary, 2.7f * unit, hand)
            val resting = if (pointRight) leftShoulder else rightShoulder
            drawLine(foreground.copy(alpha = .58f), resting, Offset(resting.x - point * 5 * unit, resting.y + 18 * unit), 3 * unit, StrokeCap.Round)
        } else if (pose == FrameGuidePose.CELEBRATE) {
            drawLine(foreground.copy(alpha = .88f), leftShoulder, Offset(cx - 28 * unit, h * .36f), 3.2f * unit, StrokeCap.Round)
            drawLine(foreground.copy(alpha = .88f), rightShoulder, Offset(cx + 28 * unit, h * .36f), 3.2f * unit, StrokeCap.Round)
            drawCircle(secondary, 2.6f * unit, Offset(cx - 28 * unit, h * .36f))
            drawCircle(primary, 2.6f * unit, Offset(cx + 28 * unit, h * .36f))
        } else {
            drawLine(foreground.copy(alpha = .65f), leftShoulder, Offset(cx - 18 * unit - walkSwing, h * .69f), 3 * unit, StrokeCap.Round)
            drawLine(foreground.copy(alpha = .65f), rightShoulder, Offset(cx + 18 * unit + walkSwing, h * .69f), 3 * unit, StrokeCap.Round)
        }

        val hipY = h * .75f
        val footY = h * .91f
        drawLine(foreground.copy(alpha = .72f), Offset(cx - 8 * unit, hipY), Offset(cx - 10 * unit + walkSwing, footY), 3.2f * unit, StrokeCap.Round)
        drawLine(foreground.copy(alpha = .72f), Offset(cx + 8 * unit, hipY), Offset(cx + 10 * unit - walkSwing, footY), 3.2f * unit, StrokeCap.Round)
        drawLine(primary.copy(alpha = .85f), Offset(cx - 14 * unit + walkSwing, footY), Offset(cx - 6 * unit + walkSwing, footY), 3 * unit, StrokeCap.Round)
        drawLine(primary.copy(alpha = .85f), Offset(cx + 6 * unit - walkSwing, footY), Offset(cx + 14 * unit - walkSwing, footY), 3 * unit, StrokeCap.Round)
    }
}

@Composable
internal fun V127SetupGuideStrip(page: Int) {
    val tips = listOf(
        "Start with what you create most. You can change this later.",
        "Pick every place you actually publish — no need to overthink it.",
        "Choose how you usually make things, not the genre you make.",
        "Tell me what matters now so the app can prioritize around you.",
        "Last step. These permissions only power reminders and alerts.",
    )
    val poses = listOf(FrameGuidePose.POINT, FrameGuidePose.WALK, FrameGuidePose.POINT, FrameGuidePose.IDLE, FrameGuidePose.CELEBRATE)

    AnimatedContent(
        targetState = page.coerceIn(0, tips.lastIndex),
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
        label = "setupGuide",
    ) { index ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = CinemaSurface,
            border = BorderStroke(1.dp, RecRed.copy(alpha = .22f)),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                FrameGuideCompanion(poses[index], Modifier.size(width = 54.dp, height = 64.dp), pointRight = true)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("FRAME GUIDE", color = MutedGold, fontSize = 7.5.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(tips[index], color = ProjectorIvory, fontSize = 9.6.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
