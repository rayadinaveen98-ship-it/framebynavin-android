package com.framebynavin.app.reminders

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.framebynavin.app.ui.CinePulseGuide
import com.framebynavin.app.ui.CinePulseState
import kotlin.math.min

/**
 * Spark's original, approved transparent character artwork is kept intact.
 * The alarm pointing pose gains restrained alert rings; voice's presenting
 * pose gains gentle visual speaking bars. Neither is an audio meter.
 *
 * Decoration is purely Canvas, with no touch target, accessibility node,
 * system notification changes or per-particle Compose allocations.
 */
@Composable
internal fun V199ReminderSpark(
    kind: ReminderCheckInKind,
    motionEnabled: Boolean,
    lowPower: Boolean,
    modifier: Modifier = Modifier,
) {
    val expression = V199SparkMotionPolicy.expression(kind)
    val animate = motionEnabled && !lowPower
    val transition = rememberInfiniteTransition(label = "v199-spark-scene")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = V199SparkMotionPolicy.FULL_TURN,
        animationSpec = infiniteRepeatable(
            tween(V199SparkMotionPolicy.CYCLE_MILLIS, easing = LinearEasing),
        ),
        label = "v199-spark-pose-cycle",
    )
    val t = if (animate) phase else 0f
    val glow = if (animate) V199SparkMotionPolicy.glow(t) else .83f
    val accent = if (expression == V199SparkExpression.ALERT)
        Color(0xFFFFD18B) else Color(0xFFBEB0FF)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().testTag(
            if (expression == V199SparkExpression.ALERT) "v199-spark-alarm-fx" else "v199-spark-voice-fx",
        )) {
            val middle = Offset(size.width * .5f, size.height * .52f)
            val radius = min(size.width, size.height) * .45f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = .21f * glow),
                        accent.copy(alpha = .065f * glow),
                        Color.Transparent,
                    ),
                    center = middle,
                    radius = radius,
                ),
                center = middle,
                radius = radius,
            )
            if (expression == V199SparkExpression.ALERT) {
                // Partial arcs sit outside Spark's face. They never obscure the eyes.
                val arcRadius = radius * (.90f + .055f * glow)
                val box = Size(arcRadius * 2f, arcRadius * 2f)
                val topLeft = Offset(middle.x - arcRadius, middle.y - arcRadius)
                drawArc(
                    color = accent.copy(alpha = .48f * glow),
                    startAngle = 208f, sweepAngle = 78f, useCenter = false,
                    topLeft = topLeft, size = box,
                    style = Stroke(width = 1.7.dp.toPx()),
                )
                drawArc(
                    color = accent.copy(alpha = .52f * glow),
                    startAngle = 324f, sweepAngle = 54f, useCenter = false,
                    topLeft = topLeft, size = box,
                    style = Stroke(width = 1.7.dp.toPx()),
                )
                drawCircle(
                    color = accent.copy(alpha = .74f * glow),
                    radius = 2.2.dp.toPx(),
                    center = Offset(size.width * .84f, size.height * .23f),
                )
            } else {
                // Visual speech energy, intentionally not tied to TTS amplitude.
                val barWidth = 2.6.dp.toPx()
                val baseX = size.width * .835f
                val baseY = size.height * .52f
                repeat(4) { index ->
                    val level = if (animate) V199SparkMotionPolicy.voiceBar(index, t) else .58f
                    val height = (5f + 14f * level).dp.toPx()
                    drawRoundRect(
                        color = accent.copy(alpha = .54f + .35f * glow),
                        topLeft = Offset(baseX + index * (barWidth + 2.4.dp.toPx()),
                            baseY - height * .5f),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(barWidth * .5f),
                    )
                }
            }
        }
        // Keep original Spark proportions/identity: no redrawing or distorting.
        CinePulseGuide(
            state = if (expression == V199SparkExpression.ALERT)
                CinePulseState.POINT else CinePulseState.PRESENT,
            modifier = Modifier
                .size(width = 92.dp, height = 103.dp)
                .graphicsLayer {
                    translationY = if (animate) V199SparkMotionPolicy.liftDp(expression, t).dp.toPx() else 0f
                    rotationZ = if (animate) V199SparkMotionPolicy.tiltDegrees(expression, t) else 0f
                    val pulse = if (animate) V199SparkMotionPolicy.scale(t) else 1f
                    scaleX = pulse
                    scaleY = pulse
                },
        )
    }
}
