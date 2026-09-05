from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "app/src/main/java/com/framebynavin/app/ui/V174CinematicWelcome.kt"

SOURCE = r'''package com.framebynavin.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * FrameByNavin founder ident.
 *
 * Original brand-language direction: black -> light -> mark -> name -> app.
 * It intentionally borrows only the pacing discipline of premium studio idents;
 * the geometry, palette and motion language are FrameByNavin's own.
 */
@Composable
internal fun V174CinematicWelcome() {
    val ignition = remember { Animatable(0f) }
    val strips = remember { Animatable(0f) }
    val mark = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val sweep = remember { Animatable(0f) }
    val settle = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(70)
        ignition.animateTo(1f, tween(250, easing = LinearOutSlowInEasing))

        launch { strips.animateTo(1f, tween(680, easing = FastOutSlowInEasing)) }
        delay(390)
        launch { mark.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
        delay(350)
        launch { title.animateTo(1f, tween(420, easing = LinearOutSlowInEasing)) }
        delay(190)
        sweep.animateTo(1f, tween(620, easing = LinearEasing))
        settle.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020203)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val glowAlpha = 0.08f + (0.16f * mark.value) - (0.035f * settle.value)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD72B29).copy(alpha = glowAlpha),
                        Color(0xFF521015).copy(alpha = glowAlpha * 0.55f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, size.height * 0.455f),
                    radius = size.width * 0.52f,
                ),
                radius = size.width * 0.52f,
                center = Offset(size.width / 2f, size.height * 0.455f),
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height * 0.46f
            val lineHeight = size.height * (0.08f + 0.22f * ignition.value)
            val lineWidth = 1.2f + (2.8f * ignition.value)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFF3B33).copy(alpha = 0.85f * ignition.value),
                        Color(0xFFFF8A52).copy(alpha = ignition.value),
                        Color(0xFFFF3B33).copy(alpha = 0.85f * ignition.value),
                        Color.Transparent,
                    ),
                    startY = centerY - lineHeight / 2f,
                    endY = centerY + lineHeight / 2f,
                ),
                topLeft = Offset(centerX - lineWidth / 2f, centerY - lineHeight / 2f),
                size = Size(lineWidth, lineHeight),
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            if (strips.value <= 0f) return@Canvas

            val centerY = size.height * 0.46f
            val maxHeight = size.height * 0.37f
            val h = maxHeight * (0.55f + 0.45f * strips.value)
            val travel = size.width * 0.115f * strips.value
            val fade = (1f - mark.value * 0.88f).coerceIn(0f, 1f)
            val baseWidth = size.width * (0.013f + 0.012f * strips.value)
            val cx = size.width / 2f

            fun lightStrip(x: Float, width: Float, color: Color, alpha: Float) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            color.copy(alpha = alpha * 0.82f),
                            color.copy(alpha = alpha),
                            color.copy(alpha = alpha * 0.82f),
                            Color.Transparent,
                        ),
                        startY = centerY - h / 2f,
                        endY = centerY + h / 2f,
                    ),
                    topLeft = Offset(x - width / 2f, centerY - h / 2f),
                    size = Size(width, h),
                )
            }

            lightStrip(cx - travel, baseWidth * 1.05f, Color(0xFFF1C06B), 0.88f * fade)
            lightStrip(cx, baseWidth * 1.22f, Color(0xFFD72B29), 1.0f * fade)
            lightStrip(cx + travel, baseWidth, Color(0xFF6A607B), 0.80f * fade)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFB15F).copy(alpha = 0.22f * fade),
                        Color(0xFFD72B29).copy(alpha = 0.09f * fade),
                        Color.Transparent,
                    ),
                    center = Offset(cx, centerY),
                    radius = size.width * 0.25f,
                ),
                radius = size.width * 0.25f,
                center = Offset(cx, centerY),
            )
        }

        FrameByNavinIdentMark(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-36).dp)
                .size(148.dp)
                .graphicsLayer {
                    alpha = mark.value
                    scaleX = 0.86f + (0.14f * mark.value)
                    scaleY = 0.86f + (0.14f * mark.value)
                },
            reveal = mark.value,
        )

        Text(
            text = "FRAME BY NAVIN",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 82.dp)
                .alpha(title.value)
                .graphicsLayer {
                    scaleX = 0.965f + (0.035f * title.value)
                    scaleY = 0.965f + (0.035f * title.value)
                    translationY = 7f * (1f - title.value)
                },
            color = Color(0xFFF7F1E8),
            fontSize = 24.sp,
            lineHeight = 29.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.2.sp,
            textAlign = TextAlign.Center,
        )

        if (sweep.value > 0f) {
            val xFraction = -0.30f + (1.60f * sweep.value)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 111.dp)
                    .fillMaxWidth(0.68f)
                    .height(2.dp)
                    .alpha((1f - settle.value) * 0.78f)
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                (xFraction - 0.09f).coerceIn(0f, 1f) to Color.Transparent,
                                xFraction.coerceIn(0f, 1f) to Color(0xFFFFD39A),
                                (xFraction + 0.09f).coerceIn(0f, 1f) to Color.Transparent,
                                1.00f to Color.Transparent,
                            )
                        )
                    ),
            )
        }
    }
}

@Composable
private fun FrameByNavinIdentMark(
    modifier: Modifier = Modifier,
    reveal: Float,
) {
    Canvas(modifier) {
        val s = size.minDimension
        fun x(v: Float) = (v / 108f) * s
        fun y(v: Float) = (v / 108f) * s

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFD72B29).copy(alpha = 0.25f * reveal),
                    Color(0xFF521015).copy(alpha = 0.12f * reveal),
                    Color.Transparent,
                ),
                center = Offset(s * 0.52f, s * 0.54f),
                radius = s * 0.58f,
            ),
            radius = s * 0.58f,
            center = Offset(s * 0.52f, s * 0.54f),
        )

        val rear = Path().apply {
            moveTo(x(58f), y(15f)); lineTo(x(89f), y(31f)); lineTo(x(89f), y(76f)); lineTo(x(58f), y(62f)); close()
        }
        drawPath(rear, Color(0xFF393544))
        val rearEdge = Path().apply {
            moveTo(x(58f), y(15f)); lineTo(x(89f), y(31f)); lineTo(x(89f), y(36f)); lineTo(x(63f), y(23f)); lineTo(x(63f), y(64f)); lineTo(x(58f), y(62f)); close()
        }
        drawPath(rearEdge, Color(0xFF6A607B))

        val red = Path().apply {
            moveTo(x(39f), y(23f)); lineTo(x(72f), y(36f)); lineTo(x(72f), y(84f)); lineTo(x(39f), y(73f)); close()
        }
        drawPath(red, Color(0xFFD72B29))
        val redEdge = Path().apply {
            moveTo(x(39f), y(23f)); lineTo(x(72f), y(36f)); lineTo(x(72f), y(41f)); lineTo(x(45f), y(31f)); lineTo(x(45f), y(75f)); lineTo(x(39f), y(73f)); close()
        }
        drawPath(redEdge, Color(0xFFFF5A46))
        val redShade = Path().apply {
            moveTo(x(66f), y(39f)); lineTo(x(72f), y(41f)); lineTo(x(72f), y(84f)); lineTo(x(66f), y(82f)); close()
        }
        drawPath(redShade, Color(0xFF771219))

        val gold = Path().apply {
            moveTo(x(18f), y(34f)); lineTo(x(50f), y(19f)); lineTo(x(50f), y(77f)); lineTo(x(18f), y(91f)); close()
        }
        drawPath(gold, Color(0xFFF1C06B))
        val goldEdge = Path().apply {
            moveTo(x(18f), y(34f)); lineTo(x(50f), y(19f)); lineTo(x(50f), y(25f)); lineTo(x(24f), y(38f)); lineTo(x(24f), y(88f)); lineTo(x(18f), y(91f)); close()
        }
        drawPath(goldEdge, Color(0xFFFFF0C9))
        val goldShade = Path().apply {
            moveTo(x(44f), y(22f)); lineTo(x(50f), y(19f)); lineTo(x(50f), y(77f)); lineTo(x(44f), y(80f)); close()
        }
        drawPath(goldShade, Color(0xFFA35C25))

        drawCircle(
            color = Color(0xFFFF493D),
            radius = x(4.2f),
            center = Offset(x(78f), y(84.2f)),
        )
        drawCircle(
            color = Color(0xFFFFC15C).copy(alpha = 0.82f),
            radius = x(1.3f),
            center = Offset(x(79.4f), y(83.5f)),
        )
    }
}
'''

TARGET.write_text(SOURCE, encoding="utf-8")
print("v1.7.5 RC4 original FrameByNavin studio ident applied")
