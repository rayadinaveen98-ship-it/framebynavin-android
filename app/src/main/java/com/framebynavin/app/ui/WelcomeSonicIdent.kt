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

/** Original, synthesized FrameByNavin launch sound. No bundled copyrighted audio asset. */
internal object WelcomeSonicIdent {
    fun play(context: Context) {
        if (!VisualExperiencePrefs.launchSoundEnabled) return
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) <= 0) return

        Thread({
            runCatching {
                val sampleRate = 44_100
                val duration = 1.05
                val samples = ShortArray((sampleRate * duration).toInt())
                val random = Random(127)
                for (i in samples.indices) {
                    val t = i.toDouble() / sampleRate
                    val whooshWindow = when {
                        t < .04 || t > .48 -> 0.0
                        t < .24 -> (t - .04) / .20
                        else -> (.48 - t) / .24
                    }.coerceIn(0.0, 1.0)
                    val whoosh = (random.nextDouble() * 2.0 - 1.0) * whooshWindow * .09

                    val impactT = (t - .31).coerceAtLeast(0.0)
                    val impact = if (t >= .31) sin(2.0 * PI * 84.0 * impactT) * exp(-impactT * 8.5) * .19 else 0.0

                    val chimeT = (t - .57).coerceAtLeast(0.0)
                    val chime = if (t >= .57) {
                        (sin(2.0 * PI * 720.0 * chimeT) + .52 * sin(2.0 * PI * 1080.0 * chimeT)) * exp(-chimeT * 5.4) * .10
                    } else 0.0

                    val sample = ((whoosh + impact + chime).coerceIn(-.82, .82) * Short.MAX_VALUE).toInt()
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
                Handler(Looper.getMainLooper()).postDelayed({ runCatching { track.stop() }; track.release() }, 1_250L)
            }
        }, "frame-sonic-ident").start()
    }
}
