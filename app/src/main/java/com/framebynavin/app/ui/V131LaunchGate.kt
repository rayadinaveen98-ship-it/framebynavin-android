package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.framebynavin.app.ui.theme.BacklotBackground
import com.framebynavin.app.widget.CreatorWidgetLaunch

/**
 * Three-second cinematic studio-ident on normal cold launches.
 * The ident itself owns completion so there is no second independent timer or dead hold.
 * Widget/deep-link launches stay instant so creator shortcuts never inherit a splash delay.
 *
 * Guide identity is intentionally not a launch gate. Backlot already has a safe default guide,
 * and creators can personalize it later in Settings. First run should reach account/creator setup
 * without requiring a cosmetic choice before the product knows anything about the creator.
 */
@Composable
fun V131LaunchGate(externalLaunch: CreatorWidgetLaunch?) {
    var welcomeDone by remember { mutableStateOf(externalLaunch != null) }
    LaunchedEffect(externalLaunch?.nonce) {
        if (externalLaunch != null) welcomeDone = true
    }
    Surface(modifier = Modifier.fillMaxSize(), color = BacklotBackground) {
        AnimatedContent(
            targetState = welcomeDone,
            transitionSpec = {
                fadeIn(tween(140)) togetherWith fadeOut(tween(110))
            },
            label = "launchGate",
        ) { ready ->
            if (ready) {
                FrameByNavinV101BApp(externalLaunch = externalLaunch)
            } else {
                V140CinematicWelcome(onFinished = { welcomeDone = true })
            }
        }
    }
}
