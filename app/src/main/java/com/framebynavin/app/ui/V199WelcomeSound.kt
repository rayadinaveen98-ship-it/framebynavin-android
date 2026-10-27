package com.framebynavin.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.framebynavin.app.ui.theme.VisualExperiencePrefs
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Native, generated 3-second Backlot sonic signature.
 * The visual's monotonic clock is authoritative; the AudioTrack never extends the launch gate.
 * This is intentionally quieter than the old 4.6s cinematic impact.
 */
internal object V199WelcomeSound {
    private const val SAMPLE_RATE = 44_100

    fun prepare(context: Context): AudioTrack? {
        if (!VisualExperiencePrefs.launchSoundEnabled) return null
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return null
        if (manager.getStreamVolume(AudioManager.STREAM_MUSIC) <= 0) return null

        val sampleCount = SAMPLE_RATE * V199WelcomeTimeline.DURATION_MS.toInt() / 1_000
        val output = ShortArray(sampleCount)
        val random = Random(199)
        for (i in 0 until sampleCount) {
            val t = i.toDouble() / SAMPLE_RATE
            val ignite = softWindow(t, 0.04, 0.23, 0.66)
            val swirl = softWindow(t, 0.16, 0.55, 1.98)
            val resolve = softWindow(t, 1.78, 2.13, 2.78)
            val shimmer = if (t >= 1.97) exp(-(t - 1.97) * 5.2) else 0.0
            val air = (random.nextDouble() * 2.0 - 1.0) * ignite * 0.0025
            val gentlePulse = (sin(2.0 * PI * 174.0 * t) + 0.32 * sin(2.0 * PI * 348.0 * t)) * ignite * 0.010
            val floatingNotes = (
                sin(2.0 * PI * (210.0 + 31.0 * t) * t) +
                0.38 * sin(2.0 * PI * (420.0 + 62.0 * t) * t)
            ) * swirl * 0.013
            val tonalLock = (
                sin(2.0 * PI * 523.25 * t) +
                0.64 * sin(2.0 * PI * 659.25 * t) +
                0.38 * sin(2.0 * PI * 783.99 * t)
            ) * resolve * 0.016
            val pinprick = sin(2.0 * PI * 1046.5 * t) * shimmer * 0.009
            val fade = (1.0 - ((t - 2.78) / 0.22).coerceIn(0.0, 1.0))
            val sum = (gentlePulse + floatingNotes + tonalLock + pinprick + air) * fade
            output[i] = (sum.coerceIn(-0.7, 0.7) * Short.MAX_VALUE).toInt().toShort()
        }

        return AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .setBufferSizeInBytes(output.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
            .also { it.write(output, 0, output.size) }
    }

    private fun softWindow(t: Double, start: Double, peak: Double, end: Double): Double {
        val v = when {
            t <= start || t >= end -> 0.0
            t <= peak -> (t - start) / (peak - start)
            else -> (end - t) / (end - peak)
        }.coerceIn(0.0, 1.0)
        return v * v * (3.0 - 2.0 * v)
    }
}
