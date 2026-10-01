package com.framebynavin.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.framebynavin.app.ui.theme.VisualExperiencePrefs
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Original, synthesized Backlot launch sound. No bundled copyrighted audio asset.
 *
 * v141 deliberately follows the existing five-second visual ident instead of firing a short
 * one-second logo sting at launch. The sound landmarks mirror the visual timeline:
 * ignition -> thread formation -> underline sweep -> final lock.
 */
internal object WelcomeSonicIdent {
    private const val V141_IDENT_DURATION_SECONDS = 5.0
    private const val V141_IGNITION_END_SECONDS = 0.80
    private const val V141_FORMATION_START_SECONDS = 1.50
    private const val V141_UNDERLINE_START_SECONDS = 3.95
    private const val V141_SETTLE_START_SECONDS = 4.20

    fun play(context: Context) {
        if (!VisualExperiencePrefs.launchSoundEnabled) return
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) <= 0) return

        Thread({
            runCatching {
                val sampleRate = 44_100
                val samples = ShortArray((sampleRate * V141_IDENT_DURATION_SECONDS).toInt())
                val random = Random(141)

                for (i in samples.indices) {
                    val t = i.toDouble() / sampleRate

                    // 0.00-0.80s: threads wake up with a restrained air/shimmer layer.
                    val ignition = triangularWindow(t, 0.02, 0.42, V141_IGNITION_END_SECONDS)
                    val air = (random.nextDouble() * 2.0 - 1.0) * ignition * 0.018
                    val ignitionTone = sin(2.0 * PI * (176.0 + 26.0 * t) * t) * ignition * 0.018

                    // 1.50s: the first visible thread segments commit to the wordmark.
                    val formationHitT = (t - V141_FORMATION_START_SECONDS).coerceAtLeast(0.0)
                    val formationHit = if (t >= V141_FORMATION_START_SECONDS) {
                        (
                            sin(2.0 * PI * 92.0 * formationHitT) +
                                0.34 * sin(2.0 * PI * 184.0 * formationHitT)
                            ) * exp(-formationHitT * 8.2) * 0.11
                    } else 0.0

                    // 1.50-4.20s: a quiet evolving harmonic bed travels with the forming letters.
                    val formation = broadWindow(t, V141_FORMATION_START_SECONDS, V141_SETTLE_START_SECONDS)
                    val movingFrequency = 228.0 + (t - V141_FORMATION_START_SECONDS).coerceAtLeast(0.0) * 42.0
                    val threadTone = (
                        sin(2.0 * PI * movingFrequency * t) +
                            0.42 * sin(2.0 * PI * movingFrequency * 1.5 * t)
                        ) * formation * 0.024
                    val threadDust = (random.nextDouble() * 2.0 - 1.0) * formation * 0.006

                    // ~3.95s: underline sweep gets one clean high-frequency pass.
                    val sweep = triangularWindow(t, V141_UNDERLINE_START_SECONDS, 4.08, 4.24)
                    val sweepTone = (
                        sin(2.0 * PI * 760.0 * t) +
                            0.45 * sin(2.0 * PI * 1140.0 * t)
                        ) * sweep * 0.040
                    val sweepAir = (random.nextDouble() * 2.0 - 1.0) * sweep * 0.015

                    // 4.20s: the completed BACKLOT mark locks into place with a low tactile hit.
                    val settleT = (t - V141_SETTLE_START_SECONDS).coerceAtLeast(0.0)
                    val settleHit = if (t >= V141_SETTLE_START_SECONDS) {
                        (
                            sin(2.0 * PI * 74.0 * settleT) +
                                0.30 * sin(2.0 * PI * 222.0 * settleT)
                            ) * exp(-settleT * 7.0) * 0.14
                    } else 0.0

                    // Final resolving chord fades before the five-second visual hand-off.
                    val resolve = triangularWindow(t, 4.22, 4.42, 4.92)
                    val resolveChord = (
                        sin(2.0 * PI * 220.0 * t) +
                            0.58 * sin(2.0 * PI * 330.0 * t) +
                            0.34 * sin(2.0 * PI * 440.0 * t)
                        ) * resolve * 0.030

                    val mix = air + ignitionTone + formationHit + threadTone + threadDust +
                        sweepTone + sweepAir + settleHit + resolveChord
                    val sample = (mix.coerceIn(-0.82, 0.82) * Short.MAX_VALUE).toInt()
                    samples[i] = sample.toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        runCatching { track.stop() }
                        track.release()
                    },
                    5_250L,
                )
            }
        }, "backlot-sonic-ident").start()
    }

    private fun triangularWindow(t: Double, start: Double, peak: Double, end: Double): Double = when {
        t <= start || t >= end -> 0.0
        t <= peak -> smooth01((t - start) / (peak - start))
        else -> smooth01((end - t) / (end - peak))
    }

    private fun broadWindow(t: Double, start: Double, end: Double): Double {
        if (t <= start || t >= end) return 0.0
        val attackEnd = start + 0.34
        val releaseStart = end - 0.42
        return when {
            t < attackEnd -> smooth01((t - start) / (attackEnd - start))
            t > releaseStart -> smooth01((end - t) / (end - releaseStart))
            else -> 1.0
        }
    }

    private fun smooth01(value: Double): Double {
        val x = value.coerceIn(0.0, 1.0)
        return x * x * (3.0 - 2.0 * x)
    }
}
