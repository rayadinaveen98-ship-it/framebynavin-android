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
 * Short cinematic studio-ident on normal cold launches.
 * Widget/deep-link launches stay instant so creator shortcuts never inherit a splash delay.
 */
@Composable
fun V131LaunchGate(externalLaunch: CreatorWidgetLaunch?) {
    var welcomeDone by remember { mutableStateOf(externalLaunch != null) }
    LaunchedEffect(externalLaunch?.nonce) {
        if (externalLaunch != null) {
            welcomeDone = true
        } else if (!welcomeDone) {
            delay(1_550L)
            welcomeDone = true
        }
    }
    AnimatedContent(
        targetState = welcomeDone,
        transitionSpec = {
            fadeIn(tween(170)) togetherWith fadeOut(tween(130))
        },
        label = "launchGate",
    ) { ready ->
        if (ready) FrameByNavinV101BApp(externalLaunch = externalLaunch) else V133CinematicWelcome()
    }
}
