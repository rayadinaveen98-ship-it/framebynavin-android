package com.framebynavin.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun V127StageCompletionPulse(eventKey: Long, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val opacity = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(eventKey) {
        if (eventKey <= 0L) return@LaunchedEffect
        progress.snapTo(0f)
        opacity.snapTo(1f)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        launch { progress.animateTo(1f, tween(760, easing = FastOutSlowInEasing)) }
        delay(590L)
        opacity.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
    }

    if (eventKey > 0L && opacity.value > 0f) {
        Box(modifier.fillMaxSize().alpha(opacity.value), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(150.dp)) {
                val p = progress.value
                val radius = size.minDimension * (.18f + .25f * p)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(RecRed.copy(alpha = .22f * (1f - p)), MutedGold.copy(alpha = .08f * (1f - p)), Color.Transparent),
                        center = center,
                        radius = radius * 1.8f,
                    ),
                    radius = radius * 1.8f,
                )
                drawCircle(
                    color = RecRed.copy(alpha = .88f * (1f - p * .55f)),
                    radius = radius,
                    style = Stroke(width = 2.3f + 2f * (1f - p)),
                )
            }
            Surface(shape = CircleShape, color = CinemaSurfaceRaised, shadowElevation = 12.dp, modifier = Modifier.size(54.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Check, null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
                }
            }
            Text(
                "STAGE COMPLETE",
                modifier = Modifier.offset(y = 52.dp),
                color = ProjectorIvory,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            )
        }
    }
}

@Composable
internal fun V127RewardAura(eventKey: String, modifier: Modifier = Modifier) {
    val pulse = remember(eventKey) { Animatable(0f) }
    LaunchedEffect(eventKey) {
        pulse.snapTo(0f)
        pulse.animateTo(1f, tween(1040, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        val p = pulse.value
        val radius = size.minDimension * (.16f + p * .50f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(MutedGold.copy(alpha = .24f * (1f - p)), RecRed.copy(alpha = .07f * (1f - p)), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
        )
    }
}
