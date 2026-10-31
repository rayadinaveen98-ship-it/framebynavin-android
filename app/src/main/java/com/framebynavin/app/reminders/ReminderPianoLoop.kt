package com.framebynavin.app.reminders

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Original fully synthesized piano-like melody: no third-party recording,
 * download, storage permission, or streaming. Never shares source files.
 */
internal object ReminderPianoWaveform {
    const val SAMPLE_RATE = 16_000
    private const val STEP_SECONDS = 0.68
    private val notes = doubleArrayOf(261.63, 329.63, 392.00, 329.63,
        220.00, 261.63, 329.63, 293.66)

    fun render(): ShortArray {
        val stepSamples = (SAMPLE_RATE * STEP_SECONDS).roundToInt()
        val count = stepSamples * notes.size
        return ShortArray(count) { index ->
            val step = index / stepSamples
            val inNote = (index % stepSamples).toDouble() / SAMPLE_RATE
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

    fun waveHeader(dataBytes: Int): ByteArray {
        require(dataBytes in 2..1_000_000 && dataBytes % 2 == 0)
        return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray(Charsets.US_ASCII))
            .putInt(dataBytes + 36)
            .put("WAVE".toByteArray(Charsets.US_ASCII))
            .put("fmt ".toByteArray(Charsets.US_ASCII))
            .putInt(16)
            .putShort(1.toShort())
            .putShort(1.toShort())
            .putInt(SAMPLE_RATE)
            .putInt(SAMPLE_RATE * 2)
            .putShort(2.toShort())
            .putShort(16.toShort())
            .put("data".toByteArray(Charsets.US_ASCII))
            .putInt(dataBytes).array()
    }
}

/**
 * Android11 emulator and some vendor audio backends reject MODE_STATIC loop
 * points for generated PCM. Use the platform decoder on a tiny temporary WAV
 * instead. Each active service owns its own cache file; never overwrites
 * another reminder and always deletes temporary audio on stop.
 */
internal class ReminderPianoLoop(private val context: Context) {
    private var player: MediaPlayer? = null
    private var backingFile: File? = null
    private var generation = 0
    private var requestedVolume = .14f

    fun start(): Boolean {
        stop()
        val currentGeneration = ++generation
        return runCatching {
            val samples = ReminderPianoWaveform.render()
            val file = File.createTempFile(
                "backlot-piano-", ".wav", context.applicationContext.cacheDir,
            )
            backingFile = file
            FileOutputStream(file).use { out ->
                out.write(ReminderPianoWaveform.waveHeader(samples.size * 2))
                val data = ByteArray(samples.size * 2)
                for (i in samples.indices) {
                    val sample = samples[i].toInt()
                    data[2 * i] = (sample and 0xff).toByte()
                    data[2 * i + 1] = (sample ushr 8).toByte()
                }
                out.write(data)
            }
            val media = MediaPlayer()
            player = media
            media.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            media.setDataSource(file.absolutePath)
            media.isLooping = true
            media.setVolume(requestedVolume, requestedVolume)
            media.setOnPreparedListener { prepared ->
                if (generation == currentGeneration && player === prepared) {
                    runCatching { prepared.start() }
                        .onFailure { failure -> fail(currentGeneration, failure) }
                }
            }
            media.setOnErrorListener { _, what, extra ->
                fail(currentGeneration, IllegalStateException("Music decode error $what/$extra"))
                true
            }
            media.prepareAsync()
            true
        }.onFailure { fail(currentGeneration, it) }.getOrDefault(false)
    }

    private fun fail(expected: Int, error: Throwable) {
        if (expected != generation) return
        Log.w("BacklotPiano", "Offline piano playback unavailable; speech continues", error)
        stop()
    }

    fun setSpeaking(speaking: Boolean) {
        requestedVolume = if (speaking) .035f else .14f
        runCatching { player?.setVolume(requestedVolume, requestedVolume) }
    }

    fun stop() {
        ++generation
        val old = player
        player = null
        if (old != null) {
            runCatching { old.setOnPreparedListener(null) }
            runCatching { old.setOnErrorListener(null) }
            runCatching { old.reset() }
            runCatching { old.release() }
        }
        val file = backingFile
        backingFile = null
        if (file != null) runCatching { file.delete() }
        requestedVolume = .14f
    }
}
