package com.framebynavin.app.reminders

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * V195.4 Phase 4A: distinct visual cues for approved Spark artwork.
 * This policy deliberately owns no reminder/service/action state.
 */
internal enum class V199SparkExpression { ALERT, SPEAK }

internal object V199SparkMotionPolicy {
    const val CYCLE_MILLIS = 3_200
    val FULL_TURN: Float = (2.0 * PI).toFloat()

    fun expression(kind: ReminderCheckInKind): V199SparkExpression =
        if (kind == ReminderCheckInKind.ALARM) V199SparkExpression.ALERT
        else V199SparkExpression.SPEAK

    /** Gentle character motion; keep arms/face legible, never tumble the raster. */
    fun liftDp(expression: V199SparkExpression, phase: Float): Float {
        val amplitude = if (expression == V199SparkExpression.ALERT) 2.4f else 1.5f
        return -amplitude * (.5f + .5f * sin(phase.toDouble()).toFloat())
    }

    fun tiltDegrees(expression: V199SparkExpression, phase: Float): Float {
        val amplitude = if (expression == V199SparkExpression.ALERT) 1.15f else .65f
        return amplitude * sin(phase.toDouble()).toFloat()
    }

    fun scale(phase: Float): Float = 1f + .012f * (.5f + .5f * sin(phase.toDouble()).toFloat())

    /** A living light cue, not an alarm sound or notification. */
    fun glow(phase: Float): Float =
        (.65f + .35f * (.5f + .5f * cos((2f * phase).toDouble()).toFloat()))
            .coerceIn(0f, 1f)

    /** Three phase-offset speaking bars, not fake TTS/audio data. */
    fun voiceBar(index: Int, phase: Float): Float {
        require(index in 0..3)
        return (.34f + .66f * (.5f + .5f *
            sin((phase * 2f + index * 1.15f).toDouble()).toFloat()))
            .coerceIn(0f, 1f)
    }
}
