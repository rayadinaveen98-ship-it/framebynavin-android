package com.framebynavin.app.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.framebynavin.app.data.VoicePersona
import com.framebynavin.app.reminders.VoicePersonaEngine
import java.util.Locale

/**
 * UX03: local, transient preview; never creates reminder occurrences.
 * Only one Backlot voice sample may own the TTS engine in this process.
 *
 * All state is main-thread confined. An older asynchronous TTS initialization
 * cannot speak after a newer preview was chosen. Normal reminder playback
 * remains independent of this user-initiated preview player.
 */
internal object V403VoicePreviewPlayer {
    private val handler = Handler(Looper.getMainLooper())
    private var generation = 0
    private var active: TextToSpeech? = null
    private var timeout: Runnable? = null
    private const val PREVIEW_TIMEOUT_MS = 12_000L

    fun play(
        context: Context,
        persona: VoicePersona,
        voiceNameToCompare: String? = null,
        compareSameSample: Boolean = false,
        sampleTextOverride: String? = null,
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post {
                play(context.applicationContext, persona, voiceNameToCompare,
                    compareSameSample, sampleTextOverride)
            }
            return
        }

        generation++
        stopActive()
        val currentGeneration = generation
        var candidate: TextToSpeech? = null

        candidate = TextToSpeech(context.applicationContext) { status ->
            handler.post {
                if (currentGeneration != generation) {
                    runCatching { candidate?.shutdown() }
                    return@post
                }
                val engine = candidate ?: return@post
                if (status != TextToSpeech.SUCCESS) {
                    stopActive()
                    return@post
                }
                val language = runCatching { engine.setLanguage(Locale.getDefault()) }
                    .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
                if (language == TextToSpeech.LANG_MISSING_DATA ||
                    language == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    stopActive()
                    return@post
                }

                runCatching {
                    VoicePersonaEngine.apply(
                        engine, persona, context.applicationContext, voiceNameToCompare,
                    )
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit

                        override fun onDone(utteranceId: String?) {
                            handler.post {
                                if (currentGeneration == generation) stopActive()
                            }
                        }

                        @Deprecated("Required by Android TTS")
                        override fun onError(utteranceId: String?) {
                            handler.post {
                                if (currentGeneration == generation) stopActive()
                            }
                        }
                    })
                    engine.speak(
                        if (compareSameSample)
                            VoicePersonaEngine.comparisonPreviewText()
                        else sampleTextOverride ?: VoicePersonaEngine.previewText(persona),
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "backlot-preview-" + persona.name + "-" + currentGeneration,
                    )
                }.onFailure { stopActive() }
            }
        }
        active = candidate
        timeout = Runnable {
            if (currentGeneration == generation) stopActive()
        }.also { handler.postDelayed(it, PREVIEW_TIMEOUT_MS) }
    }

    fun stop() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            generation++
            stopActive()
        } else handler.post { stop() }
    }

    private fun stopActive() {
        timeout?.let { handler.removeCallbacks(it) }
        timeout = null
        active?.let { engine ->
            runCatching { engine.stop() }
            runCatching { engine.shutdown() }
        }
        active = null
    }
}
