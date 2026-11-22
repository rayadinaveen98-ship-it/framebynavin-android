package com.framebynavin.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.os.PowerManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * UX12 D3: present only after the setup write succeeds, never for widget,
 * notification or deep-link launches. No new account, reminder, or profile state.
 */
internal enum class UX12D3ArrivalDestination { CREATOR, PERSONAL }

internal object UX12D3ArrivalPolicy {
    const val MOTION_DURATION_MS = 2_250L
    const val REDUCED_MOTION_DURATION_MS = 180L

    fun shouldPresent(setupCommitted: Boolean, externalLaunch: Boolean): Boolean =
        setupCommitted && !externalLaunch
}

private val UX12D3Ink = Color(0xFF110E16)
private val UX12D3Ivory = Color(0xFFF5F0E9)
private val UX12D3Muted = Color(0xFFD2BDB0)

@Composable
private fun ux12D3MotionAllowed(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        ValueAnimator.areAnimatorsEnabled() && powerManager?.isPowerSaveMode != true
    }
}

/** Single lightweight Compose Canvas, no bitmap blur, shaders or generated art. */
@Composable
internal fun UX12D3WarmCompanionBackdrop(modifier: Modifier = Modifier) {
    val motionAllowed = ux12D3MotionAllowed()
    val drift = if (motionAllowed) {
        val movement = rememberInfiniteTransition(label = "ux12-d3-stage-motion")
        val phase by movement.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(6_800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "ux12-d3-light-drift",
        )
        phase
    } else 0f

    Canvas(modifier.fillMaxSize().testTag("ux12-d3-companion-atmosphere")) {
        if (size.minDimension <= 0f) return@Canvas
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF100E17), Color(0xFF1B1119), Color(0xFF110F16)),
            ),
        )
        val radius = size.minDimension * .88f
        val warmCenter = Offset(
            size.width * (.22f + drift * .045f),
            size.height * (.71f - drift * .035f),
        )
        val coolCenter = Offset(
            size.width * (.84f - drift * .045f),
            size.height * (.23f + drift * .025f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFDB9771).copy(alpha = .18f), Color.Transparent),
                center = warmCenter,
                radius = radius,
            ),
            center = warmCenter,
            radius = radius,
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF9A7CCC).copy(alpha = .11f), Color.Transparent),
                center = coolCenter,
                radius = radius * .92f,
            ),
            center = coolCenter,
            radius = radius * .92f,
        )
    }
}

/**
 * One-time transition AFTER a successful Creator or Personal guide completion.
 * The existing workspace is already composed underneath this blocking visual
 * overlay, so all original navigation and guided-tour state is preserved.
 */
@Composable
internal fun UX12D3WorkspaceArrival(
    destination: UX12D3ArrivalDestination,
    onFinished: () -> Unit,
) {
    val motionAllowed = ux12D3MotionAllowed()
    val finish by rememberUpdatedState(onFinished)
    val entrance = remember(destination) { Animatable(0f) }
    var pose by remember(destination) { mutableStateOf(CinePulseState.WAVE) }

    LaunchedEffect(destination, motionAllowed) {
        if (motionAllowed) {
            entrance.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
            delay(560L)
            pose = CinePulseState.PRESENT
            delay(1_080L)
            entrance.animateTo(0f, tween(250))
        } else {
            entrance.snapTo(1f)
            delay(UX12D3ArrivalPolicy.REDUCED_MOTION_DURATION_MS)
        }
        finish()
    }

    // Do not force a setup loop: Back simply completes the decorative entrance.
    BackHandler { finish() }
    Box(
        Modifier.fillMaxSize()
            .background(UX12D3Ink)
            .pointerInput(Unit) {
                // Block taps on the already-composed workspace while arriving.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .testTag("ux12-d3-arrival-" + destination.name.lowercase()),
    ) {
        UX12D3WarmCompanionBackdrop()
        Column(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp)
                .graphicsLayer { alpha = entrance.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            V144GuideCharacter(
                guide = V144GuidePrefs.currentGuide,
                state = pose,
                modifier = Modifier.size(168.dp),
            )
            Spacer(Modifier.height(17.dp))
            Text(
                "ENTERING BACKLOT",
                color = UX12D3Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                maxLines = 1,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (destination == UX12D3ArrivalDestination.CREATOR)
                    "Your creative space is ready."
                else "Your personal space is ready.",
                color = UX12D3Ivory,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (destination == UX12D3ArrivalDestination.CREATOR)
                    "Make something meaningful."
                else "A little room for what matters.",
                color = UX12D3Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
