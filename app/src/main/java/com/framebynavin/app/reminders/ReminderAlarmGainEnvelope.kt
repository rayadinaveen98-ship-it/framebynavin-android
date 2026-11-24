package com.framebynavin.app.reminders

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

/**
 * Owner-approved app-local alarm gain controller.
 *
 * This ONLY changes the already-playing MediaPlayer's L/R gain. It cannot
 * change system alarm volume, device ringer, notification settings, schedule,
 * persistence, vibration, foreground lifecycle or snooze receipts.
 *
 * The service remains authoritative for duration and immediate cancellations.
 * Never use the fade to delay stopHardware(), Done, Snooze or Dismiss.
 */
internal object ReminderAlarmMixPolicy {
    const val NORMAL_GAIN = 1f
    const val SPEECH_GAIN = 0.42f
    const val SPEECH_RECOVERY_MILLIS = 300L
    const val NATURAL_TIMEOUT_FADE_MILLIS = 350L

    fun interpolate(from: Float, to: Float, fraction: Float): Float =
        (from + (to - from) * fraction.coerceIn(0f, 1f)).coerceIn(0f, 1f)
}

internal class ReminderAlarmGainEnvelope(
    private val player: () -> MediaPlayer?,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var sequence = 0
    private var gain = ReminderAlarmMixPolicy.NORMAL_GAIN
    private var naturallyEnding = false

    fun resetForNewAlarm() {
        cancel()
        naturallyEnding = false
        apply(ReminderAlarmMixPolicy.NORMAL_GAIN)
    }

    /** Duck before TTS begins; alarm stays audible and vibration is unchanged. */
    fun setSpeechActive(active: Boolean) {
        if (naturallyEnding) return
        cancel()
        if (active) apply(ReminderAlarmMixPolicy.SPEECH_GAIN)
        else rampTo(ReminderAlarmMixPolicy.NORMAL_GAIN,
            ReminderAlarmMixPolicy.SPEECH_RECOVERY_MILLIS)
    }

    /** Invoked shortly BEFORE the existing duration watchdog, not after it. */
    fun beginNaturalTimeoutFade() {
        naturallyEnding = true
        cancel()
        rampTo(0f, ReminderAlarmMixPolicy.NATURAL_TIMEOUT_FADE_MILLIS)
    }

    /** Immediate user action, stale occurrence, error, or service teardown. */
    fun stop() {
        cancel()
        naturallyEnding = false
        gain = ReminderAlarmMixPolicy.NORMAL_GAIN
    }

    private fun cancel() {
        ++sequence
        handler.removeCallbacksAndMessages(null)
    }

    private fun rampTo(target: Float, durationMillis: Long) {
        val version = ++sequence
        val initial = gain
        val steps = 10
        for (index in 1..steps) {
            handler.postDelayed({
                if (version == sequence) {
                    apply(ReminderAlarmMixPolicy.interpolate(initial, target,
                        index.toFloat() / steps))
                }
            }, durationMillis * index / steps)
        }
    }

    private fun apply(value: Float) {
        gain = value.coerceIn(0f, 1f)
        runCatching { player()?.setVolume(gain, gain) }
    }
}
