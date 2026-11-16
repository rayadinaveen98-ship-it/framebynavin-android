package com.framebynavin.app.ui

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Opt-in, session-only coach narration. No microphone, recording, cloud call,
 * reminder voice mutation, automatic audio playback or new permission.
 * The Android speech engine is owned by the composed guide and disposed safely.
 */
@Composable
internal fun V402TourNarration(enabled: Boolean, spokenCopy: String) {
    // Do not even initialize a speech engine until narration is explicitly enabled.
    if (!enabled) return
    val context = LocalContext.current.applicationContext
    var ready by remember { mutableStateOf(false) }
    val main = remember { Handler(Looper.getMainLooper()) }
    val speech = remember(context) {
        TextToSpeech(context) { status ->
            main.post { ready = status == TextToSpeech.SUCCESS }
        }
    }

    DisposableEffect(speech) {
        onDispose {
            ready = false
            runCatching { speech.stop() }
            runCatching { speech.shutdown() }
        }
    }

    LaunchedEffect(speech, spokenCopy, ready) {
        if (ready) runCatching {
            speech.speak(
                spokenCopy.take(600),
                TextToSpeech.QUEUE_FLUSH,
                null,
                "backlot-ux02-walkthrough",
            )
        }
    }
}
