package com.framebynavin.app.reminders

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.os.Handler
import android.os.Looper

/**
 * One backing-music layer for Personal and Creator voice reminders.
 * Never requests gallery/storage permissions; CUSTOM only opens an already
 * authorized SAF content URI. No network/remote URLs or raw filesystem paths.
 */
internal class ReminderBackingMusic(private val context: Context) {
    private val piano = ReminderPianoLoop(context)
    private var custom: MediaPlayer? = null
    private var speaking = false
    private var hasSpoken = false
    private var serial = 0
    private val volumeHandler = Handler(Looper.getMainLooper())
    private var fadeGeneration = 0
    private var currentGain = ReminderMusicMixPolicy.BEFORE_VOICE

    fun start(choice: ReminderMusicChoice) {
        stop()
        val approved = runCatching { choice.checked() }.getOrElse {
            Log.w("BacklotMusic", "Invalid music selection; using original piano", it)
            piano.start()
            return
        }
        when (approved.mode) {
            ReminderMusicMode.OFF -> Unit
            ReminderMusicMode.PIANO -> piano.start()
            ReminderMusicMode.CUSTOM -> startCustom(approved.uri)
        }
    }

    private fun startCustom(source: String) {
        val generation = ++serial
        val media = MediaPlayer()
        custom = media
        runCatching {
            media.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            media.setDataSource(context, Uri.parse(source))
            media.isLooping = true
            media.setVolume(currentGain, currentGain)
            media.setOnPreparedListener { prepared ->
                if (serial == generation && custom === prepared) {
                    runCatching { prepared.start() }.onFailure { unavailable(generation, prepared) }
                }
            }
            media.setOnErrorListener { instance, _, _ ->
                unavailable(generation, instance)
                true
            }
            media.prepareAsync()
        }.onFailure {
            unavailable(generation, media)
        }
    }

    private fun unavailable(generation: Int, failed: MediaPlayer) {
        if (generation != serial || custom !== failed) return
        Log.w("BacklotMusic", "Selected audio unavailable; using original piano")
        custom = null
        runCatching { failed.reset() }
        runCatching { failed.release() }
        // Revoked/moved SAF documents must not silence the reminder or crash it.
        piano.start()
        piano.setGain(currentGain)
    }

    fun setSpeaking(active: Boolean) {
        speaking = active
        if (active) hasSpoken = true
        // Speech may start as soon as speak() returns, so duck synchronously
        // BEFORE calling TTS. Recovery is gradual to avoid a sudden music jump.
        cancelGainEvents()
        val target = ReminderMusicMixPolicy.level(active, hasSpoken)
        if (active) applyGain(target)
        else rampTo(target, ReminderMusicMixPolicy.FADE_MILLIS)
    }

    /**
     * End naturally INSIDE an existing service-owned music tail. No lease,
     * foreground service, user action or completion receipt is extended.
     * Repeated TTS/new reminders cancel this via setSpeaking/start/stop.
     */
    fun beginNaturalEnding(existingTailMillis: Long) {
        if (speaking) return
        // Do NOT cancel the already scheduled post-speech recovery. It must
        // rise to the normal music gain first, then fade near the tail's end.
        // A new utterance, occurrence, stop or error invalidates this token.
        val generation = fadeGeneration
        volumeHandler.postDelayed({
            if (generation == fadeGeneration && !speaking) {
                rampTo(0f, minOf(ReminderMusicMixPolicy.FINAL_FADE_MILLIS,
                    existingTailMillis.coerceAtLeast(0L)))
            }
        }, ReminderMusicMixPolicy.tailFadeDelay(existingTailMillis))
    }

    private fun cancelGainEvents() {
        ++fadeGeneration
        volumeHandler.removeCallbacksAndMessages(null)
    }

    private fun rampTo(target: Float, durationMillis: Long) {
        val generation = ++fadeGeneration
        volumeHandler.removeCallbacksAndMessages(null)
        val from = currentGain
        val to = target.coerceIn(0f, 1f)
        if (durationMillis <= 0L) {
            applyGain(to)
            return
        }
        val steps = 10
        for (step in 1..steps) {
            volumeHandler.postDelayed({
                if (generation == fadeGeneration) {
                    applyGain(ReminderMusicMixPolicy.interpolatedGain(
                        from, to, step.toFloat() / steps))
                }
            }, durationMillis * step / steps)
        }
    }

    private fun applyGain(level: Float) {
        currentGain = level.coerceIn(0f, 1f)
        piano.setGain(currentGain)
        runCatching { custom?.setVolume(currentGain, currentGain) }
    }

    fun stop() {
        ++serial
        cancelGainEvents()
        val old = custom
        custom = null
        if (old != null) {
            runCatching { old.setOnPreparedListener(null) }
            runCatching { old.setOnErrorListener(null) }
            runCatching { old.stop() }
            runCatching { old.release() }
        }
        piano.stop()
        speaking = false
        hasSpoken = false
        currentGain = ReminderMusicMixPolicy.BEFORE_VOICE
    }
}
