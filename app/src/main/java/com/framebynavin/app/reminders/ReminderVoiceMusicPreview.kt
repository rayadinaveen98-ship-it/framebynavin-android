package com.framebynavin.app.reminders

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Foreground editor-only sound check, not a scheduled notification or Service.
 *
 * User content is deliberately NEVER spoken in preview. TTS says a fixed
 * phrase only. The sound layer is shared with actual reminder playback; the
 * preview has a strict maximum duration and stops if the editor leaves view.
 */
internal class ReminderVoiceMusicPreview(context: Context) {
    private val app = context.applicationContext
    private val music = ReminderBackingMusic(app)
    private val handler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var generation = 0

    companion object {
        const val MAX_PREVIEW_MILLIS = 30_000L
        const val SAMPLE_SPEECH = "Backlot reminder preview. This is how your voice and music sound together."
    }

    private fun begin(choice: ReminderMusicChoice): Int {
        stop()
        val ticket = ++generation
        music.start(choice)
        handler.postDelayed({
            if (generation == ticket) stop()
        }, MAX_PREVIEW_MILLIS)
        return ticket
    }

    fun playMusic(choice: ReminderMusicChoice) {
        begin(choice)
    }

    /** A single short test utterance; actual reminder repeats are unchanged. */
    fun playVoiceAndMusic(choice: ReminderMusicChoice, onStatus: (String) -> Unit) {
        val ticket = begin(choice)
        // Ensure background stays quieter than speech from the start of TTS setup.
        music.setSpeaking(true)
        runCatching {
            engine = TextToSpeech(app) { status ->
                handler.post {
                    if (generation != ticket) return@post
                    val tts = engine ?: return@post
                    if (status != TextToSpeech.SUCCESS) {
                        stop()
                        onStatus("Voice preview unavailable on this device.")
                        return@post
                    }
                    val available = runCatching {
                        tts.setLanguage(Locale.getDefault())
                    }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
                    if (available == TextToSpeech.LANG_NOT_SUPPORTED ||
                        available == TextToSpeech.LANG_MISSING_DATA) {
                        stop()
                        onStatus("The selected speech language is not installed.")
                        return@post
                    }
                    tts.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    val utterance = "backlot-editor-preview-$ticket"
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit
                        override fun onDone(utteranceId: String?) {
                            handler.post {
                                if (generation == ticket && utteranceId == utterance) {
                                    stop()
                                    onStatus("Voice and music preview complete.")
                                }
                            }
                        }
                        @Deprecated("Required by Android TextToSpeech")
                        override fun onError(utteranceId: String?) {
                            handler.post {
                                if (generation == ticket && utteranceId == utterance) {
                                    stop()
                                    onStatus("Speech preview failed; your reminder was not changed.")
                                }
                            }
                        }
                    })
                    val played = runCatching {
                        tts.speak(SAMPLE_SPEECH, TextToSpeech.QUEUE_FLUSH, null, utterance)
                    }.getOrDefault(TextToSpeech.ERROR)
                    if (played != TextToSpeech.SUCCESS) {
                        stop()
                        onStatus("Speech preview could not start.")
                    }
                }
            }
        }.onFailure {
            stop()
            onStatus("Speech preview unavailable; no reminder was changed.")
        }
    }

    /** Idempotent: safe on Stop button, window background, or editor disposal. */
    fun stop() {
        ++generation
        handler.removeCallbacksAndMessages(null)
        music.stop()
        val previous = engine
        engine = null
        if (previous != null) {
            runCatching { previous.stop() }
            runCatching { previous.shutdown() }
        }
    }
}
