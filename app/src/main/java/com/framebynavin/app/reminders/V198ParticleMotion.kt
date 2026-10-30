package com.framebynavin.app.reminders

import kotlin.math.PI
import kotlin.math.sin

/**
 * Pure mathematical motion/color signals: closed integer-harmonic paths make
 * the 3.8-second animation loop seamless instead of jumping at the boundary.
 * No frame allocations and no remapping of app/reminder state.
 */
internal object V198ParticleMotion {
    const val PHASE_END = (2 * PI).toFloat()
    const val CYCLE_MILLIS = 3_800

    fun wave(phase: Float, harmonic: Float, offset: Float): Float =
        sin((phase * harmonic + offset).toDouble()).toFloat()

    fun colorMix(phase: Float, harmonic: Float, offset: Float): Float =
        (.5f + .5f * wave(phase, harmonic, offset)).coerceIn(0f, 1f)

    fun iconPulse(phase: Float): Float = .67f + .33f * colorMix(phase, 2f, 0f)
}
