package com.framebynavin.app.reminders

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Local original piano-like soundtrack for Backlot voice reminders.
 *
 * No third-party recording, network, media URI or special storage permission.
 * A bounded PCM loop is generated on demand and released when speech finishes
 * or the reminder is Done/Snoozed/stopped. Native platform AudioTrack controls
 * playback; this is not a separate foreground/background service.
 */
internal object ReminderPianoWaveform {
    const val SAMPLE_RATE = 16_000
    private const val STEP_SECONDS = 0.68
    // A gentle, original eight-note arpeggio; nothing copyrighted is sampled.
    private val notes = doubleArrayOf(261.63, 329.63, 392.00, 329.63,
        220.00, 261.63, 329.63, 293.66)

    fun render(): ShortArray {
        val stepSamples = (SAMPLE_RATE * STEP_SECONDS).roundToInt()
        val count = stepSamples * notes.size
        return ShortArray(count) { index ->
            val step = index / stepSamples
            val inNote = (index % stepSamples).toDouble() / SAMPLE_RATE
            // Fast attack, exponentially decaying harmonics plus mellow release.
            val attack = (inNote / 0.018).coerceIn(0.0, 1.0)
            val envelope = attack * exp(-3.9 * inNote)
            val wave = (
                sin(2.0 * PI * notes[step] * inNote) * 0.68 +
                sin(4.0 * PI * notes[step] * inNote) * 0.23 +
                sin(6.0 * PI * notes[step] * inNote) * 0.09
            ) * envelope
            (wave * 13_000.0).roundToInt().coerceIn(Short.MIN_VALUE.toInt(),
                Short.MAX_VALUE.toInt()).toShort()
        }
    }
}

/** AudioTrack is created only after the existing current-occurrence admission check. */
internal class ReminderPianoLoop {
    private var track: AudioTrack? = null

    fun start(): Boolean {
        stop()
        return runCatching {
            val samples = ReminderPianoWaveform.render()
            val bytes = ByteArray(samples.size * 2)
            for (i in samples.indices) {
                val sample = samples[i].toInt()
                bytes[2 * i] = (sample and 0xff).toByte()
                bytes[2 * i + 1] = (sample shr 8).toByte()
            }
            val audio = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(ReminderPianoWaveform.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bytes.size)
                .build()
            try {
                check(audio.state == AudioTrack.STATE_INITIALIZED) { "Piano track unavailable" }
                check(audio.write(bytes, 0, bytes.size, AudioTrack.WRITE_BLOCKING) == bytes.size) {
                    "Piano loop write incomplete"
                }
                check(audio.setLoopPoints(0, samples.size, -1) == AudioTrack.SUCCESS) {
                    "Piano loop points unavailable"
                }
                audio.setVolume(0.14f)
                audio.play()
                track = audio
            } finally {
                if (track !== audio) audio.release()
            }
            true
        }.onFailure {
            Log.w("BacklotPiano", "Built-in piano unavailable; speech will continue", it)
        }.getOrDefault(false)
    }

    /** Spoken words stay intelligible while the background piano continues. */
    fun setSpeaking(speaking: Boolean) {
        runCatching { track?.setVolume(if (speaking) 0.035f else 0.14f) }
    }

    fun stop() {
        val old = track
        track = null
        if (old != null) {
            runCatching { old.pause() }
            runCatching { old.flush() }
            runCatching { old.release() }
        }
    }
}
