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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Backlot founder ident.
 *
 * Original brand-language direction: black -> light -> mark -> name -> app.
 * It intentionally borrows only the pacing discipline of premium studio idents;
 * the geometry, palette and motion language are Backlot's own.
 */
private const val V20_WELCOME_STRIPE_COUNT = 30
private const val V129_THREAD_GAP_SCALE = 1.25f

@Composable
internal fun V174CinematicWelcome() {
    val context = LocalContext.current
    val ignition = remember { Animatable(0f) }
    val strips = remember { Animatable(0f) }
    val impact = remember { Animatable(0f) }
    val mark = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val sweep = remember { Animatable(0f) }
    val settle = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        WelcomeSonicIdent.play(context.applicationContext)
        delay(60)
        ignition.animateTo(1f, tween(220, easing = LinearOutSlowInEasing))
        // Alpha20: the stripe event owns the whole screen first. The brand reveal starts only
        // after the last stripe has crossed its travel window.
        strips.animateTo(1f, tween(2350, easing = FastOutSlowInEasing))
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
            val revealGlow = 0.08f + 0.24f * mark.value + 0.12f * impact.value - 0.035f * settle.value
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
            if (strips.value <= 0f) return@Canvas
            val progress = strips.value.coerceIn(0f, 1f)
            val screenH = size.height
            val overscanH = screenH * 1.18f
            val centerY = screenH * 0.50f
            val fadeOut = (1f - impact.value * 0.92f).coerceIn(0f, 1f)
            val palette = listOf(
                MutedGold, RecRed, ProjectorIvory.copy(alpha = .72f),
                RecRed.copy(alpha = .78f), MutedGold.copy(alpha = .86f), RecRedDeep,
                ProjectorIvory.copy(alpha = .48f), MutedGold.copy(alpha = .62f),
            )

            repeat(V20_WELCOME_STRIPE_COUNT) { index ->
                val stagger = index * 0.014f
                val local = ((progress - stagger) / (1f - stagger)).coerceIn(0f, 1f)
                if (local <= 0f) return@repeat
                val side = if (index % 2 == 0) -1f else 1f
                val lane = (index / 2 + 1).toFloat() / (V20_WELCOME_STRIPE_COUNT / 2f + 1f)
                val startX = size.width * 0.50f + side * size.width * 0.025f * V129_THREAD_GAP_SCALE
                val endX = size.width * 0.50f + side * size.width * (0.50f + lane * 0.10f * V129_THREAD_GAP_SCALE)
                val eased = local * local * (3f - 2f * local)
                val drift = kotlin.math.sin((eased * 3.1415926f) + index * .47f) * size.width * .012f
                val x = startX + (endX - startX) * eased + drift
                val base = size.width * (if (index % 3 == 0) 0.030f else if (index % 3 == 1) 0.018f else 0.010f)
                val width = base * (0.72f + 0.28f * local)
                val color = palette[index % palette.size]
                val alpha = (0.66f + (index % 4) * 0.075f) * fadeOut

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

        BacklotIdentMark(
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
            text = "BACKLOT",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 83.dp)
                .alpha(title.value)
                .graphicsLayer {
                    scaleX = 0.965f + (0.035f * title.value)
                    scaleY = 0.965f + (0.035f * title.value)
                    translationY = 7f * (1f - title.value)
                },
            color = ProjectorIvory,
            fontSize = 29.2.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 5.6.sp,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "CREATE WHAT'S NEXT",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 116.dp)
                .alpha(title.value),
            color = MutedGold.copy(alpha = .92f),
            fontSize = 8.6.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.5.sp,
            textAlign = TextAlign.Center,
        )

        if (sweep.value > 0f) {
            val xFraction = -0.30f + (1.60f * sweep.value)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 133.dp)
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
private fun BacklotIdentMark(
    modifier: Modifier = Modifier,
    reveal: Float,
) {
    Canvas(modifier) {
        val s = size.minDimension
        fun x(v: Float) = (v / 108f) * s
        fun y(v: Float) = (v / 108f) * s

        // Atmospheric blue/gold edge light from the approved Backlot concept.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFAD3D).copy(alpha = .25f * reveal),
                    Color(0xFF234D77).copy(alpha = .10f * reveal),
                    Color.Transparent,
                ),
                center = Offset(x(55f), y(58f)),
                radius = x(55f),
            ),
            radius = x(55f),
            center = Offset(x(55f), y(58f)),
        )

        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color(0xFF08111B), Color(0xFF020304), Color(0xFF061426)),
                start = Offset(x(8f), y(7f)),
                end = Offset(x(101f), y(103f)),
            ),
            topLeft = Offset(x(7f), y(7f)),
            size = Size(x(94f), y(94f)),
            cornerRadius = CornerRadius(x(19f), y(19f)),
        )
        drawRoundRect(
            color = Color(0xFF3FAEFF).copy(alpha = .40f * reveal),
            topLeft = Offset(x(7.5f), y(7.5f)),
            size = Size(x(93f), y(93f)),
            cornerRadius = CornerRadius(x(18.5f), y(18.5f)),
            style = Stroke(width = x(.75f)),
        )

        val beam = Path().apply {
            moveTo(x(62f), y(78f)); lineTo(x(16f), y(100f)); lineTo(x(96f), y(100f)); close()
        }
        drawPath(
            beam,
            Brush.linearGradient(
                listOf(Color(0x00FF9A24), Color(0xAAFFAA32), Color(0xFFFFE0A0), Color(0x2254B8FF)),
                start = Offset(x(18f), y(101f)),
                end = Offset(x(77f), y(79f)),
            ),
        )

        drawRect(
            brush = Brush.linearGradient(
                listOf(Color(0xFFFFF3D0), Color(0xFFF2BE60), Color(0xFFB76625)),
                start = Offset(x(31f), y(19f)),
                end = Offset(x(43f), y(88f)),
            ),
            topLeft = Offset(x(31f), y(19f)),
            size = Size(x(12f), y(69f)),
        )
        repeat(6) { i ->
            drawRoundRect(
                color = Color(0xFF11100E),
                topLeft = Offset(x(34f), y(25f + i * 11f)),
                size = Size(x(6f), y(6f)),
                cornerRadius = CornerRadius(x(1.1f), x(1.1f)),
            )
        }

        val b = Path().apply {
            moveTo(x(45f), y(19f))
            lineTo(x(61f), y(23f))
            cubicTo(x(77f), y(26f), x(84f), y(34f), x(84f), y(45f))
            cubicTo(x(84f), y(54f), x(79f), y(59f), x(72f), y(62f))
            cubicTo(x(82f), y(65f), x(88f), y(73f), x(88f), y(82f))
            cubicTo(x(88f), y(94f), x(79f), y(99f), x(61f), y(99f))
            lineTo(x(45f), y(99f)); lineTo(x(45f), y(86f)); lineTo(x(59f), y(86f))
            cubicTo(x(69f), y(86f), x(74f), y(83f), x(74f), y(77f))
            cubicTo(x(74f), y(70f), x(69f), y(67f), x(59f), y(67f))
            lineTo(x(45f), y(67f)); lineTo(x(45f), y(55f)); lineTo(x(58f), y(55f))
            cubicTo(x(67f), y(55f), x(72f), y(52f), x(72f), y(46f))
            cubicTo(x(72f), y(40f), x(68f), y(37f), x(59f), y(35f))
            lineTo(x(45f), y(32f)); close()
        }
        drawPath(
            b,
            Brush.linearGradient(
                listOf(Color(0xFFFFF5DB), Color(0xFFFFD27A), Color(0xFFE3A04A), Color(0xFF7896B4)),
                start = Offset(x(45f), y(19f)),
                end = Offset(x(88f), y(99f)),
            ),
        )

        val doorway = Path().apply {
            moveTo(x(45f), y(32f)); lineTo(x(61f), y(42f)); lineTo(x(61f), y(78f)); lineTo(x(45f), y(86f)); close()
        }
        drawPath(doorway, Color(0xFF040506))
        val doorLight = Path().apply {
            moveTo(x(61f), y(42f)); lineTo(x(66f), y(46f)); lineTo(x(66f), y(75f)); lineTo(x(61f), y(78f)); close()
        }
        drawPath(doorLight, Color(0xFFFFC45D).copy(alpha = .58f * reveal))
    }
}
