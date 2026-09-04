package com.framebynavin.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import kotlinx.coroutines.delay

@Composable
internal fun V133LayerMark(
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    glow: Float = 0f,
) {
    val p = progress.coerceIn(0f, 1f)
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier.size((54 + 18 * glow).dp)
                .alpha(.05f + .12f * glow)
                .background(RecRed, CircleShape)
        )
        Box(
            Modifier.offset(x = (-12f + 12f * p).dp, y = (5f - 5f * p).dp)
                .size(width = 25.dp, height = 43.dp)
                .graphicsLayer(rotationZ = -9f, alpha = p)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFF1D9AE), Color(0xFFA47C50))),
                    RoundedCornerShape(4.dp),
                )
        )
        Box(
            Modifier.offset(x = (11f - 5f * p).dp, y = (-7f + 7f * p).dp)
                .size(width = 23.dp, height = 39.dp)
                .graphicsLayer(rotationZ = 2f, alpha = p)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFCC3A31), Color(0xFF661713))),
                    RoundedCornerShape(4.dp),
                )
        )
        Box(
            Modifier.offset(x = (24f - 11f * p).dp, y = (-15f + 9f * p).dp)
                .size(width = 22.dp, height = 35.dp)
                .graphicsLayer(rotationZ = 8f, alpha = p)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF46413A), Color(0xFF18191C))),
                    RoundedCornerShape(4.dp),
                )
        )
    }
}

@Composable
internal fun V133CinematicWelcome() {
    val spark = remember { Animatable(0f) }
    val panels = remember { Animatable(0f) }
    val copy = remember { Animatable(0f) }
    val subtitle = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        spark.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        delay(90)
        panels.animateTo(1f, tween(720, easing = FastOutSlowInEasing))
        delay(150)
        copy.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
        delay(130)
        subtitle.animateTo(1f, tween(380))
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0xFF2A1714), Color(0xFF111113), CinemaBlack),
                radius = 1100f,
            )
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.align(Alignment.Center)
                .size((5 + 4 * spark.value).dp)
                .background(RecRed.copy(alpha = spark.value), CircleShape)
        )
        Box(
            Modifier.align(Alignment.Center)
                .width((18 + 118 * spark.value).dp)
                .height(1.dp)
                .alpha(.18f * spark.value)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, RecRed, Color.Transparent)))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            V133LayerMark(
                modifier = Modifier.size(104.dp),
                progress = panels.value,
                glow = spark.value,
            )
            Spacer(Modifier.height(26.dp))
            Text(
                "FRAME BY NAVIN",
                color = ProjectorIvory,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 4.2.sp,
                modifier = Modifier.alpha(copy.value),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "CREATOR CONTROL ROOM",
                color = MutedGold,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.4.sp,
                modifier = Modifier.alpha(subtitle.value),
            )
        }

        Text(
            "PLAN  ·  CREATE  ·  TRACK  ·  GROW",
            color = MutedText.copy(alpha = .72f),
            fontSize = 7.5.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 30.dp)
                .alpha(subtitle.value),
        )
    }
}
