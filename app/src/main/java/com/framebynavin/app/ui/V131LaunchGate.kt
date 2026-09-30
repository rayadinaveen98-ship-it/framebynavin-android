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

internal object V149LaunchPolicy {
    fun shouldShowCinematicIdent(externalLaunch: Boolean, hasSeenIdent: Boolean): Boolean =
        !externalLaunch && !hasSeenIdent
}

private object V149LaunchIdentPrefs {
    private const val PREFS = "backlot_launch_experience_v149"
    private const val KEY_IDENT_SEEN = "cinematic_ident_seen"

    fun hasSeen(context: Context): Boolean = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_IDENT_SEEN, false)

    fun markSeen(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_IDENT_SEEN, true).apply()
    }
}

/**
 * The cinematic studio ident is a first-use brand moment, not a recurring launch tax.
 * After it completes once, normal cold launches open Backlot immediately. Widget/deep-link launches
 * always stay instant and do not consume the one-time normal-launch ident.
 *
 * Guide identity is intentionally not a launch gate. Backlot already has a safe default guide,
 * and creators can personalize it later in Settings. First run should reach account/creator setup
 * without requiring a cosmetic choice before the product knows anything about the creator.
 */
@Composable
fun V131LaunchGate(externalLaunch: CreatorWidgetLaunch?) {
    val context = LocalContext.current
    val shouldShowIdent = remember(externalLaunch?.nonce) {
        V149LaunchPolicy.shouldShowCinematicIdent(
            externalLaunch = externalLaunch != null,
            hasSeenIdent = V149LaunchIdentPrefs.hasSeen(context),
        )
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
                V140CinematicWelcome(
                    onFinished = {
                        V149LaunchIdentPrefs.markSeen(context)
                        welcomeDone = true
                    },
                )
            }
        }
    }
}
