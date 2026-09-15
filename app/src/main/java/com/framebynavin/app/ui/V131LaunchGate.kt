package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import com.framebynavin.app.widget.CreatorWidgetLaunch

/**
 * Five-second cinematic studio-ident on normal cold launches.
 * Widget/deep-link launches stay instant so creator shortcuts never inherit a splash delay.
 */
@Composable
fun V131LaunchGate(externalLaunch: CreatorWidgetLaunch?) {
    var welcomeDone by remember { mutableStateOf(externalLaunch != null) }
    LaunchedEffect(externalLaunch?.nonce) {
        if (externalLaunch != null) {
            welcomeDone = true
        } else if (!welcomeDone) {
            delay(5_000L)
            welcomeDone = true
        }
    }
    AnimatedContent(
        targetState = welcomeDone,
        transitionSpec = {
            fadeIn(tween(220)) togetherWith fadeOut(tween(180))
        },
        label = "launchGate",
    ) { ready ->
        if (ready) FrameByNavinV101BApp(externalLaunch = externalLaunch) else V174CinematicWelcome()
    }
}
