package com.framebynavin.app.reminders

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

/**
 * One backing-music layer for Personal and Creator voice reminders.
 * Never requests gallery/storage permissions; CUSTOM only opens an already
 * authorized SAF content URI. No network/remote URLs or raw filesystem paths.
 */
internal class ReminderBackingMusic(private val context: Context) {
    private val piano = ReminderPianoLoop(context)
    private var custom: MediaPlayer? = null
    private var speaking = false
    private var serial = 0

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
            media.setVolume(if (speaking) .035f else .14f,
                if (speaking) .035f else .14f)
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
        piano.setSpeaking(speaking)
    }

    fun setSpeaking(active: Boolean) {
        speaking = active
        piano.setSpeaking(active)
        runCatching {
            val v = if (active) .035f else .14f
            custom?.setVolume(v, v)
        }
    }

    fun stop() {
        ++serial
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
    }
}
