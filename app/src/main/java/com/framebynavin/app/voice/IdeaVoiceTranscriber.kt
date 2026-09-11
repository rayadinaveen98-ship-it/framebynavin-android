package com.framebynavin.app.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

enum class IdeaVoiceLanguage(val displayLabel: String) {
    AUTO("AUTO"),
    TELUGU("తెలుగు"),
    ENGLISH("ENGLISH"),
}

interface IdeaVoiceTranscriberListener {
    fun onListeningChanged(listening: Boolean)
    fun onPartialTranscript(text: String)
    fun onFinalTranscript(text: String, confidence: Float?)
    fun onDetectedLanguage(languageTag: String?)
    fun onVoiceError(message: String)
}

/**
 * Small SpeechRecognizer wrapper for Quick Idea capture.
 *
 * Audio is never persisted by FrameByNavin. On devices that expose Android's on-device
 * recognizer we prefer it, then transparently fall back to the system recognizer when a local
 * language model is unavailable. AUTO enables Telugu/English detection and switching on API 34+.
 */
class IdeaVoiceTranscriber(
    context: Context,
    private val listener: IdeaVoiceTranscriberListener,
) : RecognitionListener {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var language = IdeaVoiceLanguage.AUTO
    private var keepListening = false
    private var usingOnDevice = false
    private var destroyed = false
    private var restartScheduled = false

    fun start(selectedLanguage: IdeaVoiceLanguage) {
        if (destroyed) return
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            listener.onVoiceError("Voice typing is not available on this phone.")
            return
        }

        language = selectedLanguage
        keepListening = true
        listener.onDetectedLanguage(null)
        listener.onPartialTranscript("")

        val preferOnDevice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
        recreateRecognizer(preferOnDevice)
        startListeningInternal()
    }

    fun stop() {
        keepListening = false
        restartScheduled = false
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { recognizer?.stopListening() }
    }

    fun cancel() {
        keepListening = false
        restartScheduled = false
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { recognizer?.cancel() }
        listener.onPartialTranscript("")
        listener.onListeningChanged(false)
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        keepListening = false
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun recreateRecognizer(onDevice: Boolean) {
        runCatching { recognizer?.destroy() }
        recognizer = null
        usingOnDevice = false

        recognizer = runCatching {
            if (onDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                usingOnDevice = true
                SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
            } else {
                SpeechRecognizer.createSpeechRecognizer(appContext)
            }
        }.getOrElse {
            usingOnDevice = false
            runCatching { SpeechRecognizer.createSpeechRecognizer(appContext) }.getOrNull()
        }
        recognizer?.setRecognitionListener(this)
    }

    private fun startListeningInternal() {
        if (!keepListening || destroyed) return
        val current = recognizer ?: run {
            listener.onVoiceError("Voice typing could not start. Try again.")
            listener.onListeningChanged(false)
            keepListening = false
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLanguageTag())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putExtra(RecognizerIntent.EXTRA_ENABLE_FORMATTING, RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY)
                putExtra(RecognizerIntent.EXTRA_HIDE_PARTIAL_TRAILING_PUNCTUATION, true)
                putExtra(RecognizerIntent.EXTRA_ENABLE_BIASING_DEVICE_CONTEXT, true)
                putExtra(RecognizerIntent.EXTRA_MASK_OFFENSIVE_WORDS, false)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && language == IdeaVoiceLanguage.AUTO) {
                val allowed = arrayListOf("te-IN", "en-IN")
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
                putStringArrayListExtra(RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES, allowed)
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH, RecognizerIntent.LANGUAGE_SWITCH_BALANCED)
                putStringArrayListExtra(RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES, allowed)
            }
        }

        runCatching {
            current.startListening(intent)
            listener.onListeningChanged(true)
        }.onFailure {
            keepListening = false
            listener.onListeningChanged(false)
            listener.onVoiceError("Voice typing could not start. Try again.")
        }
    }

    private fun primaryLanguageTag(): String = when (language) {
        IdeaVoiceLanguage.TELUGU -> "te-IN"
        IdeaVoiceLanguage.ENGLISH -> "en-IN"
        IdeaVoiceLanguage.AUTO -> if (Locale.getDefault().language == "te") "te-IN" else "en-IN"
    }

    private fun scheduleRestart(delayMillis: Long = 260L) {
        if (!keepListening || destroyed || restartScheduled) return
        restartScheduled = true
        mainHandler.postDelayed({
            restartScheduled = false
            if (keepListening && !destroyed) startListeningInternal()
        }, delayMillis)
    }

    private fun bestText(results: Bundle?): String =
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

    private fun bestConfidence(results: Bundle?): Float? =
        results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            ?.firstOrNull()
            ?.takeIf { it >= 0f }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit

    override fun onError(error: Int) {
        listener.onPartialTranscript("")

        if (!keepListening) {
            listener.onListeningChanged(false)
            return
        }

        if (usingOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            error in setOf(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
        ) {
            recreateRecognizer(onDevice = false)
            scheduleRestart(120L)
            return
        }

        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> scheduleRestart()

            else -> {
                keepListening = false
                listener.onListeningChanged(false)
                listener.onVoiceError(friendlyError(error))
            }
        }
    }

    override fun onResults(results: Bundle?) {
        val text = bestText(results)
        if (text.isNotBlank()) listener.onFinalTranscript(text, bestConfidence(results))
        listener.onPartialTranscript("")

        if (keepListening) {
            scheduleRestart(180L)
        } else {
            listener.onListeningChanged(false)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        listener.onPartialTranscript(bestText(partialResults))
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    override fun onLanguageDetection(results: Bundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            listener.onDetectedLanguage(results.getString(SpeechRecognizer.DETECTED_LANGUAGE))
        }
    }

    private fun friendlyError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed for voice ideas."
        SpeechRecognizer.ERROR_AUDIO -> "The microphone could not be used. Try again."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice typing needs a connection on this phone right now."
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Voice typing is busy. Wait a moment and try again."
        SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Voice typing is temporarily unavailable. Try again."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "That speech language is not available on this phone yet."
        else -> "I couldn't transcribe that clearly. Try again."
    }
}

object VoiceIdeaText {
    fun append(existing: String, recognized: String): String {
        val segment = recognized.trim().replace(Regex("\\s+"), " ")
        if (segment.isBlank()) return existing
        if (existing.isBlank()) return segment
        return existing.trimEnd() + " " + segment
    }
}
