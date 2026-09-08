package com.framebynavin.app.ui

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
private const val V20_WELCOME_STRIPE_COUNT = 12

@Composable
internal fun V174CinematicWelcome() {
    val ignition = remember { Animatable(0f) }
    val strips = remember { Animatable(0f) }
    val impact = remember { Animatable(0f) }
    val mark = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val sweep = remember { Animatable(0f) }
    val settle = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(60)
        ignition.animateTo(1f, tween(220, easing = LinearOutSlowInEasing))
        // Alpha20: the stripe event owns the whole screen first. The brand reveal starts only
        // after the last stripe has crossed its travel window.
        strips.animateTo(1f, tween(980, easing = FastOutSlowInEasing))
        impact.animateTo(1f, tween(170, easing = LinearOutSlowInEasing))
        launch { mark.animateTo(1f, tween(470, easing = FastOutSlowInEasing)) }
        delay(270)
        launch { title.animateTo(1f, tween(390, easing = LinearOutSlowInEasing)) }
        delay(160)
        sweep.animateTo(1f, tween(560, easing = LinearEasing))
        settle.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020203)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val revealGlow = 0.05f + 0.16f * mark.value + 0.08f * impact.value - 0.035f * settle.value
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD72B29).copy(alpha = revealGlow),
                        Color(0xFF521015).copy(alpha = revealGlow * 0.52f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, size.height * 0.47f),
                    radius = size.width * 0.62f,
                ),
                radius = size.width * 0.62f,
                center = Offset(size.width / 2f, size.height * 0.47f),
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height * 0.47f
            val lineHeight = size.height * (0.10f + 0.36f * ignition.value)
            val lineWidth = 1.4f + 3.0f * ignition.value
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFF3B33).copy(alpha = 0.86f * ignition.value),
                        Color(0xFFFFC27A).copy(alpha = ignition.value),
                        Color(0xFFFF3B33).copy(alpha = 0.86f * ignition.value),
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
            val progress = strips.value.coerceIn(0f, 1f)
            val screenH = size.height
            val overscanH = screenH * 1.18f
            val centerY = screenH * 0.50f
            val fadeOut = (1f - impact.value * 0.92f).coerceIn(0f, 1f)
            val palette = listOf(
                Color(0xFFF4C06B), Color(0xFFD72B29), Color(0xFF6A607B),
                Color(0xFFE55A3C), Color(0xFFFFD39A), Color(0xFF8B3040),
            )

            repeat(V20_WELCOME_STRIPE_COUNT) { index ->
                val stagger = index * 0.035f
                val local = ((progress - stagger) / (1f - stagger)).coerceIn(0f, 1f)
                if (local <= 0f) return@repeat
                val side = if (index % 2 == 0) -1f else 1f
                val lane = (index / 2 + 1).toFloat() / (V20_WELCOME_STRIPE_COUNT / 2f + 1f)
                val startX = size.width * 0.50f + side * size.width * 0.025f
                val endX = size.width * 0.50f + side * size.width * (0.50f + lane * 0.10f)
                val x = startX + (endX - startX) * local
                val base = size.width * (if (index % 3 == 0) 0.030f else if (index % 3 == 1) 0.018f else 0.010f)
                val width = base * (0.72f + 0.28f * local)
                val color = palette[index % palette.size]
                val alpha = (0.54f + (index % 4) * 0.10f) * fadeOut

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            color.copy(alpha = alpha * 0.72f),
                            color.copy(alpha = alpha),
                            color.copy(alpha = alpha * 0.90f),
                            color.copy(alpha = alpha * 0.62f),
                            Color.Transparent,
                        ),
                        startY = centerY - overscanH / 2f,
                        endY = centerY + overscanH / 2f,
                    ),
                    topLeft = Offset(x - width / 2f, centerY - overscanH / 2f),
                    size = Size(width, overscanH),
                )
            }

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD39A).copy(alpha = 0.17f * fadeOut),
                        Color(0xFFD72B29).copy(alpha = 0.08f * fadeOut),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, centerY),
                    radius = size.width * 0.52f,
                ),
                topLeft = Offset.Zero,
                size = size,
            )
        }

        FrameByNavinIdentMark(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-36).dp)
                .size(148.dp)
                .graphicsLayer {
                    alpha = mark.value
                    scaleX = 0.84f + (0.16f * mark.value)
                    scaleY = 0.84f + (0.16f * mark.value)
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
                    .fillMaxWidth(0.72f)
                    .height(2.dp)
                    .alpha((1f - settle.value) * 0.80f)
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
