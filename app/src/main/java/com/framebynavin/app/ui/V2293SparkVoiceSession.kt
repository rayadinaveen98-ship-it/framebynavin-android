package com.framebynavin.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

/** Android 12+ on-device recognizer only. Never invoke a cloud speech fallback. */
internal object V2293SparkVoicePolicy {
    fun supportsLocalInput(sdk: Int, onDeviceRecognizerAvailable: Boolean): Boolean =
        sdk >= Build.VERSION_CODES.S && onDeviceRecognizerAvailable

    fun transcript(raw: String?): String = raw.orEmpty().trim().take(V2292SparkConversationPolicy.INPUT_MAX)

    fun spokenReply(reply: String): String = reply.trim().take(450)
}

/**
 * Lifecycle-scoped: recognition only after a microphone tap; replies only after
 * a separate read-aloud tap. Never persists recordings, transcripts or TTS data.
 * On devices without on-device recognition, text entry remains the fallback.
 */
internal class V2293SparkVoiceSession(
    private val context: Context,
    private val onTranscript: (String) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    var status by mutableStateOf("")
        private set
    var listening by mutableStateOf(false)
        private set
    var speaking by mutableStateOf(false)
        private set
    var preparingPlayback by mutableStateOf(false)
        private set
    val playbackActive: Boolean
        get() = preparingPlayback || speaking
    private var recognizer: SpeechRecognizer? = null
    private var player: TextToSpeech? = null
    private var playerGeneration = 0
    private var closed = false

    val canUseMic: Boolean
        get() = V2293SparkVoicePolicy.supportsLocalInput(
            Build.VERSION.SDK_INT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }
                    .getOrDefault(false),
        )

    fun permissionDenied() {
        if (!closed) status = "Microphone permission denied. Type your question instead."
    }

    fun startListening() {
        if (closed || listening) return
        // Explicit SDK guard keeps the API 31+ recognizer out of Android 8–11 paths.
        // Runtime availability alone is not a sufficient API guard for Android lint.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            status = "On-device dictation needs Android 12 or newer. Type instead."
            return
        }
        if (!canUseMic) {
            status = "On-device dictation isn't available here. Type your question instead."
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            status = "Microphone permission is required for push-to-talk."
            return
        }
        stopListening()
        // Do not let an ongoing spoken reply feed into the microphone.
        stopPlayback()
        try {
            // API >= 31 and on-device service availability checked above.
            val service = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            recognizer = service
            listening = true
            status = "Listening on-device… tap STOP to cancel."
            service.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (!closed && recognizer === service) status = "Speak now…"
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    if (!closed && recognizer === service) status = "Transcribing on-device…"
                }
                override fun onError(error: Int) {
                    if (recognizer !== service) return
                    stopListening()
                    if (!closed) status = "Didn't catch that. Try again or type your question."
                }
                override fun onResults(results: Bundle?) {
                    if (recognizer !== service || closed) return
                    val text = V2293SparkVoicePolicy.transcript(
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                    )
                    stopListening()
                    if (text.isNotEmpty()) {
                        onTranscript(text) // Review in the input field; never auto-send.
                        status = "Captured locally. Review the text, then tap SEND."
                    } else {
                        status = "No speech was captured. Type or try again."
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            service.startListening(request)
        } catch (_: Exception) {
            stopListening()
            if (!closed) status = "On-device speech couldn't start. Text entry still works."
        }
    }

    fun stopListening() {
        listening = false
        val old = recognizer
        recognizer = null
        runCatching { old?.cancel() }
        runCatching { old?.destroy() }
    }

    fun readAloud(reply: String) {
        val text = V2293SparkVoicePolicy.spokenReply(reply)
        if (closed || text.isBlank()) return
        // Never record locally-played speech as the user's question.
        stopListening()
        stopPlayback()
        preparingPlayback = true
        val generation = ++playerGeneration
        status = "Checking for an installed offline voice…"
        var pendingEngine: TextToSpeech? = null
        try {
            pendingEngine = TextToSpeech(context) { result ->
                main.post {
                    val engine = pendingEngine
                    if (closed || generation != playerGeneration || engine == null ||
                        player !== engine
                    ) return@post
                    if (result != TextToSpeech.SUCCESS) {
                        status = "Local read-aloud unavailable. You can still read the reply."
                        stopPlayback()
                        return@post
                    }
                    val lang = Locale.getDefault().language
                    val voice = runCatching {
                        engine.voices.orEmpty().firstOrNull {
                            !it.isNetworkConnectionRequired &&
                                it.locale?.language == lang &&
                                !it.features.orEmpty()
                                    .contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                        }
                    }.getOrNull()
                    if (voice == null || engine.setVoice(voice) != TextToSpeech.SUCCESS) {
                        status = "No installed offline voice for this language. Reply stays as text."
                        stopPlayback()
                        return@post
                    }
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            main.post {
                                if (!closed && generation == playerGeneration) {
                                    preparingPlayback = false
                                    speaking = true
                                    status = "Reading aloud with an installed offline voice…"
                                }
                            }
                        }
                        override fun onDone(utteranceId: String?) {
                            main.post {
                                if (!closed && generation == playerGeneration) {
                                    preparingPlayback = false
                                    speaking = false
                                    status = "Read-aloud finished."
                                }
                            }
                        }
                        @Deprecated("Android callbacks")
                        override fun onError(utteranceId: String?) {
                            main.post {
                                if (!closed && generation == playerGeneration) {
                                    preparingPlayback = false
                                    speaking = false
                                    status = "Couldn't read aloud. The text is still here."
                                }
                            }
                        }
                    })
                    if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "spark-$generation")
                        == TextToSpeech.ERROR
                    ) {
                        status = "Couldn't start the offline voice. Keep reading as text."
                        stopPlayback()
                    }
                }
            }
            player = pendingEngine
        } catch (_: Exception) {
            stopPlayback()
            if (!closed) status = "Read-aloud is unavailable on this device."
        }
    }

    fun stopPlayback() {
        ++playerGeneration
        preparingPlayback = false
        speaking = false
        val old = player
        player = null
        runCatching { old?.stop() }
        runCatching { old?.shutdown() }
    }

    fun cancelPlayback() {
        stopPlayback()
        if (!closed) status = "Read-aloud stopped."
    }

    fun stopOnBackground() {
        stopListening()
        stopPlayback()
        if (!closed) status = ""
    }

    fun close() {
        closed = true
        stopOnBackground()
    }
}

@Composable
internal fun rememberV2293SparkVoiceSession(
    onTranscript: (String) -> Unit,
): V2293SparkVoiceSession {
    val context = LocalContext.current
    val latestOnTranscript by rememberUpdatedState(onTranscript)
    val session = remember(context) {
        V2293SparkVoiceSession(context.applicationContext) { text -> latestOnTranscript(text) }
    }
    // The LocalContext may be a ContextThemeWrapper, not a ComponentActivity.
    // Use Compose's actual lifecycle owner so ON_STOP always cancels speech.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(session, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_DESTROY) {
                session.stopOnBackground()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            session.close()
        }
    }
    return session
}
