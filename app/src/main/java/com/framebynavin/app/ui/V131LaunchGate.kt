package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import com.framebynavin.app.widget.CreatorWidgetLaunch

/**
 * Three-second cinematic welcome on normal cold launches.
 * Widget/deep-link launches stay instant so Quick Project/Release Day never inherit a splash delay.
 */
@Composable
fun V131LaunchGate(externalLaunch: CreatorWidgetLaunch?) {
    var welcomeDone by remember { mutableStateOf(externalLaunch != null) }
    LaunchedEffect(externalLaunch?.nonce) {
        if (externalLaunch != null) {
            welcomeDone = true
        } else if (!welcomeDone) {
            delay(3_000L)
            welcomeDone = true
        }
    }
    AnimatedContent(
        targetState = welcomeDone,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "launchGate",
    ) { ready ->
        if (ready) FrameByNavinV101BApp(externalLaunch = externalLaunch) else V131CinematicWelcome()
    }
}
