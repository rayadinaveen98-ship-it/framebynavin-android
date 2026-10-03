package com.framebynavin.app.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.framebynavin.app.ui.theme.BacklotBackground
import com.framebynavin.app.widget.CreatorWidgetLaunch

internal object V151LaunchPolicy {
    /** Normal app launches always show the cinematic ident; widget/deep-link launches stay instant. */
    fun shouldShowCinematicIdent(externalLaunch: Boolean): Boolean = !externalLaunch
}

/**
 * The cinematic studio ident is part of every normal Backlot launch. Widget/deep-link launches
 * stay instant so a targeted action is never blocked by branding.
 *
 * Guide identity is intentionally handled separately by V144GuideChoiceGate.
 */
@Composable
fun V131LaunchGate(externalLaunch: CreatorWidgetLaunch?) {
    val context = LocalContext.current
    val shouldShowIdent = remember(externalLaunch?.nonce) {
        V151LaunchPolicy.shouldShowCinematicIdent(externalLaunch = externalLaunch != null)
    }
    var welcomeDone by remember(shouldShowIdent) { mutableStateOf(!shouldShowIdent) }

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
                V174CinematicWelcome(
                    onFinished = { welcomeDone = true },
                )
            }
        }
    }
}
