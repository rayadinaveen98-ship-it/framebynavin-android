package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import com.framebynavin.app.widget.CreatorWidgetLaunch

/**
 * Five-second cinematic studio-ident on normal cold launches.
 * The ident itself owns completion so there is no second independent timer or dead hold.
 * Widget/deep-link launches stay instant so creator shortcuts never inherit a splash delay.
 */
@Composable
fun V131LaunchGate(externalLaunch: CreatorWidgetLaunch?) {
    var welcomeDone by remember { mutableStateOf(externalLaunch != null) }
    LaunchedEffect(externalLaunch?.nonce) {
        if (externalLaunch != null) welcomeDone = true
    }
    AnimatedContent(
        targetState = welcomeDone,
        transitionSpec = {
            fadeIn(tween(180)) togetherWith fadeOut(tween(140))
        },
        label = "launchGate",
    ) { ready ->
        if (ready) {
            V144GuideChoiceGate {
                FrameByNavinV101BApp(externalLaunch = externalLaunch)
            }
        } else {
            V140CinematicWelcome(onFinished = { welcomeDone = true })
        }
    }
}
