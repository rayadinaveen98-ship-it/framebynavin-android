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
    val markScale = .92f + (.13f * p) - (.05f * s)
    Box(
        modifier.graphicsLayer(scaleX = markScale, scaleY = markScale),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size((92 + 38 * glow).dp)
                .alpha(.08f + .18f * glow)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFE83C32), Color(0xFF8F1718), Color.Transparent),
                    ),
                    CircleShape,
                )
        )
        Box(
            Modifier.offset(x = (-50f + 40f * p).dp, y = (22f - 20f * p).dp)
                .size(width = 41.dp, height = 72.dp)
                .graphicsLayer(
                    rotationZ = -18f + 9f * p - 2f * (1f - s),
                    alpha = p,
                    shadowElevation = 9f,
                )
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFF0C9), Color(0xFFF4C46E), Color(0xFF9A5A24)),
                    ),
                    RoundedCornerShape(7.dp),
                )
        )
        Box(
            Modifier.offset(x = (45f - 36f * p).dp, y = (-28f + 24f * p).dp)
                .size(width = 39.dp, height = 68.dp)
                .graphicsLayer(
                    rotationZ = 16f - 13f * p + 2f * (1f - s),
                    alpha = p,
                    shadowElevation = 11f,
                )
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFF5A46), Color(0xFFD72B29), Color(0xFF6E1018)),
                    ),
                    RoundedCornerShape(7.dp),
                )
        )
        Box(
            Modifier.offset(x = (68f - 50f * p).dp, y = (-43f + 35f * p).dp)
                .size(width = 38.dp, height = 63.dp)
                .graphicsLayer(
                    rotationZ = 24f - 15f * p + 3f * (1f - s),
                    alpha = p,
                    shadowElevation = 12f,
                )
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF6A607B), Color(0xFF393848), Color(0xFF15151C)),
                    ),
                    RoundedCornerShape(7.dp),
                )
        )

        Box(
            Modifier.offset(x = (-23).dp, y = (-18).dp)
                .size(width = 4.dp, height = 50.dp)
                .alpha(.26f * p)
                .background(Color(0xFFFFF6DE), RoundedCornerShape(100.dp))
        )
        Box(
            Modifier.offset(x = (2).dp, y = (-17).dp)
                .size(width = 3.dp, height = 45.dp)
                .alpha(.20f * p)
                .background(Color(0xFFFF8D79), RoundedCornerShape(100.dp))
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
        launch { spark.animateTo(1f, tween(170, easing = LinearOutSlowInEasing)) }
        delay(65)
        panels.animateTo(1f, tween(390, easing = FastOutSlowInEasing))
        launch { settle.animateTo(1f, tween(170, easing = FastOutSlowInEasing)) }
        delay(35)
        launch { copy.animateTo(1f, tween(220, easing = LinearOutSlowInEasing)) }
        delay(80)
        subtitle.animateTo(1f, tween(190, easing = LinearOutSlowInEasing))
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFF421612),
                    Color(0xFF211012),
                    Color(0xFF0E0D12),
                    CinemaBlack,
                ),
                radius = 1120f,
            )
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.align(Alignment.Center)
                .size((124 + 70 * spark.value).dp)
                .alpha(.08f * spark.value)
                .background(
                    Brush.radialGradient(listOf(Color(0xFFF23B31), Color.Transparent)),
                    CircleShape,
                )
        )
        Box(
            Modifier.align(Alignment.Center)
                .width((24 + 190 * spark.value).dp)
                .height(1.dp)
                .alpha(.34f * spark.value)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFFFF4A3B), Color.Transparent),
                    )
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            V133LayerMark(
                modifier = Modifier.size(150.dp),
                progress = panels.value,
                glow = spark.value,
                settle = settle.value,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "FRAME BY NAVIN",
                color = ProjectorIvory,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.0.sp,
                modifier = Modifier
                    .alpha(copy.value)
                    .graphicsLayer(translationY = (8f * (1f - copy.value))),
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "CREATOR CONTROL ROOM",
                color = Color(0xFFF3BD66),
                fontSize = 8.7.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
                modifier = Modifier.alpha(subtitle.value),
            )
        }

        Text(
            "PLAN  ·  CREATE  ·  TRACK  ·  GROW",
            color = MutedText.copy(alpha = .78f),
            fontSize = 7.6.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .alpha(subtitle.value),
        )
    }
}
