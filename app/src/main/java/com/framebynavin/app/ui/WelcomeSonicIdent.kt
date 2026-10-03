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
    // One authoritative clock shared with V174CinematicWelcome.
    const val TOTAL_DURATION_MS = 4_600L
    const val IGNITION_END_MS = 800L
    const val IMPACT_END_MS = 2_650L
    const val MARK_END_MS = 3_050L
    const val TITLE_END_MS = 3_700L
    const val UNDERLINE_START_MS = 3_500L
    const val UNDERLINE_END_MS = 4_050L
    const val SETTLE_START_MS = 4_000L

    fun prepare(context: Context): AudioTrack? {
        if (!VisualExperiencePrefs.launchSoundEnabled) return null
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return null
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) <= 0) return null

        val sampleRate = 44_100
        val samples = ShortArray((sampleRate * TOTAL_DURATION_MS / 1_000L).toInt())
        val random = Random(151)

        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate

            val ignition = triangularWindow(t, 0.02, 0.46, IGNITION_END_MS / 1000.0)
            val air = (random.nextDouble() * 2.0 - 1.0) * ignition * 0.018
            val ignitionTone = sin(2.0 * PI * (176.0 + 26.0 * t) * t) * ignition * 0.018

            val impactStart = (IMPACT_END_MS - 200L) / 1000.0
            val impactPeak = (IMPACT_END_MS - 70L) / 1000.0
            val impact = triangularWindow(t, impactStart, impactPeak, IMPACT_END_MS / 1000.0)
            val impactTone = (
                sin(2.0 * PI * 92.0 * t) +
                    0.34 * sin(2.0 * PI * 184.0 * t)
            ) * impact * 0.10

            val bed = broadWindow(t, 0.65, SETTLE_START_MS / 1000.0)
            val movingFrequency = 228.0 + t * 34.0
            val bedTone = (
                sin(2.0 * PI * movingFrequency * t) +
                    0.42 * sin(2.0 * PI * movingFrequency * 1.5 * t)
            ) * bed * 0.024
            val dust = (random.nextDouble() * 2.0 - 1.0) * bed * 0.005

            val sweep = triangularWindow(
                t,
                UNDERLINE_START_MS / 1000.0,
                (UNDERLINE_START_MS + 260L) / 1000.0,
                UNDERLINE_END_MS / 1000.0,
            )
            val sweepTone = (
                sin(2.0 * PI * 760.0 * t) +
                    0.45 * sin(2.0 * PI * 1140.0 * t)
            ) * sweep * 0.040
            val sweepAir = (random.nextDouble() * 2.0 - 1.0) * sweep * 0.012

            val settleT = (t - SETTLE_START_MS / 1000.0).coerceAtLeast(0.0)
            val settleEnvelope = if (t >= SETTLE_START_MS / 1000.0) exp(-settleT * 7.0) else 0.0
            val settleHit = (
                sin(2.0 * PI * 74.0 * settleT) +
                    0.30 * sin(2.0 * PI * 222.0 * settleT)
            ) * settleEnvelope * 0.14

            val resolve = triangularWindow(
                t,
                (SETTLE_START_MS + 220L) / 1000.0,
                (SETTLE_START_MS + 420L) / 1000.0,
                (TOTAL_DURATION_MS - 120L) / 1000.0,
            )
            val resolveChord = (
                sin(2.0 * PI * 220.0 * t) +
                    0.58 * sin(2.0 * PI * 330.0 * t) +
                    0.34 * sin(2.0 * PI * 440.0 * t)
            ) * resolve * 0.028

            val mix = air + ignitionTone + impactTone + bedTone + dust +
                sweepTone + sweepAir + settleHit + resolveChord
            samples[i] = (mix.coerceIn(-0.82, 0.82) * Short.MAX_VALUE).toInt().toShort()
        }

        return AudioTrack.Builder()
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
            .also { track ->
                track.write(samples, 0, samples.size)
            }
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
