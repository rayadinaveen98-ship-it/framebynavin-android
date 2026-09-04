package com.framebynavin.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun V133LayerMark(
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    glow: Float = 0f,
    settle: Float = 1f,
) {
    val p = progress.coerceIn(0f, 1f)
    val s = settle.coerceIn(0f, 1f)
    val markScale = .88f + (.18f * p) - (.06f * s)
    Box(
        modifier.graphicsLayer(scaleX = markScale, scaleY = markScale),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size((150 + 72 * glow).dp)
                .alpha(.16f + .24f * glow)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFF493A), Color(0xFFB31324), Color(0xFF4C1023), Color.Transparent),
                    ),
                    CircleShape,
                )
        )
        Box(
            Modifier.size((108 + 52 * glow).dp)
                .alpha(.13f + .18f * glow)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFBE55), Color(0xFFEF4C2E), Color.Transparent),
                    ),
                    CircleShape,
                )
        )

        // Front champagne card — always fully opaque. Motion comes from travel/rotation, never transparency.
        Box(
            Modifier.offset(x = (-82f + 65f * p).dp, y = (34f - 31f * p).dp)
                .size(width = 56.dp, height = 96.dp)
                .graphicsLayer(
                    rotationZ = -23f + 13f * p - 2.5f * (1f - s),
                    shadowElevation = 18f,
                )
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFF7D7), Color(0xFFFFD26B), Color(0xFFE88B2B), Color(0xFF9A461C)),
                    ),
                    RoundedCornerShape(10.dp),
                )
        )
        Box(
            Modifier.offset(x = (-37).dp, y = (-31).dp)
                .size(width = 5.dp, height = 68.dp)
                .background(Color(0xFFFFFCE9), RoundedCornerShape(100.dp))
        )

        // Middle scarlet card — solid, saturated, and visually dominant.
        Box(
            Modifier.offset(x = (73f - 58f * p).dp, y = (-42f + 36f * p).dp)
                .size(width = 54.dp, height = 92.dp)
                .graphicsLayer(
                    rotationZ = 21f - 17f * p + 2.5f * (1f - s),
                    shadowElevation = 20f,
                )
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFF7860), Color(0xFFFF332E), Color(0xFFD20E2A), Color(0xFF6A0921)),
                    ),
                    RoundedCornerShape(10.dp),
                )
        )
        Box(
            Modifier.offset(x = (2).dp, y = (-29).dp)
                .size(width = 4.dp, height = 64.dp)
                .background(Color(0xFFFFB09C), RoundedCornerShape(100.dp))
        )

        // Rear graphite/violet card — still dark, but rich enough to read as a third layer.
        Box(
            Modifier.offset(x = (104f - 77f * p).dp, y = (-63f + 52f * p).dp)
                .size(width = 52.dp, height = 86.dp)
                .graphicsLayer(
                    rotationZ = 30f - 20f * p + 3f * (1f - s),
                    shadowElevation = 22f,
                )
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF9D8BC8), Color(0xFF65558C), Color(0xFF332D50), Color(0xFF12131D)),
                    ),
                    RoundedCornerShape(10.dp),
                )
        )
        Box(
            Modifier.offset(x = (30).dp, y = (-34).dp)
                .size(width = 3.dp, height = 57.dp)
                .background(Color(0xFFC6B7EE), RoundedCornerShape(100.dp))
        )
    }
}

@Composable
internal fun V133CinematicWelcome() {
    val spark = remember { Animatable(0f) }
    val panels = remember { Animatable(0f) }
    val settle = remember { Animatable(0f) }
    val copy = remember { Animatable(0f) }
    val subtitle = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { spark.animateTo(1f, tween(150, easing = LinearOutSlowInEasing)) }
        delay(45)
        panels.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        launch { settle.animateTo(1f, tween(160, easing = FastOutSlowInEasing)) }
        delay(30)
        launch { copy.animateTo(1f, tween(210, easing = LinearOutSlowInEasing)) }
        delay(70)
        subtitle.animateTo(1f, tween(180, easing = LinearOutSlowInEasing))
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFF5A1119),
                    Color(0xFF35101B),
                    Color(0xFF18101A),
                    Color(0xFF090A0F),
                    CinemaBlack,
                ),
                radius = 1150f,
            )
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.align(Alignment.Center)
                .size((172 + 98 * spark.value).dp)
                .alpha(.16f * spark.value)
                .background(
                    Brush.radialGradient(listOf(Color(0xFFFF402E), Color(0xFFB20C32), Color.Transparent)),
                    CircleShape,
                )
        )
        Box(
            Modifier.align(Alignment.Center)
                .width((36 + 250 * spark.value).dp)
                .height(2.dp)
                .alpha(.46f * spark.value)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFFFFC15A), Color(0xFFFF3E37), Color.Transparent),
                    )
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            V133LayerMark(
                modifier = Modifier.size(194.dp),
                progress = panels.value,
                glow = spark.value,
                settle = settle.value,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "FRAME BY NAVIN",
                color = ProjectorIvory,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.1.sp,
                modifier = Modifier
                    .alpha(copy.value)
                    .graphicsLayer(translationY = (7f * (1f - copy.value))),
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "CREATOR CONTROL ROOM",
                color = Color(0xFFFFC35E),
                fontSize = 8.9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.6.sp,
                modifier = Modifier.alpha(subtitle.value),
            )
        }

        Text(
            "PLAN  ·  CREATE  ·  TRACK  ·  GROW",
            color = MutedText.copy(alpha = .82f),
            fontSize = 7.7.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .alpha(subtitle.value),
        )
    }
}
