package com.framebynavin.app.reminders

import kotlin.math.sin

/**
 * V218 one-field particle language. The clock/mic is NOT drawn separately.
 * A deterministic subset of the same colorful ambient particles is attracted
 * toward a glyph trajectory, while all remaining particles continue flowing.
 *
 * This pure policy has no Android dependency, no timers, no audio access, and
 * never participates in reminder acknowledgement or scheduler state.
 */
internal object V218UnifiedWavePolicy {
    const val NORMAL_GLYPH_DOTS = 650
    const val LOW_POWER_GLYPH_DOTS = 420
    const val FORM_SECONDS = 1.8
    const val PEAK_GLYPH_STRENGTH = 1f
    // Fluidity is intentionally low; excessive drift erases recognizability.
    const val GLYPH_FLOW_AMPLITUDE = .009f

    /** Clamped smoothstep: both ends have zero velocity (no hard snap). */
    fun entrance(seconds: Double): Float {
        val t = ((seconds - .12) / FORM_SECONDS).coerceIn(0.0, 1.0).toFloat()
        return t * t * (3f - 2f * t)
    }

    fun glyphBlend(seconds: Double, revealProgress: Float): Float =
        (entrance(seconds) * PEAK_GLYPH_STRENGTH *
            (1f - revealProgress.coerceIn(0f, 1f))).coerceIn(0f, 1f)

    /**
     * The same harmonic displaces surrounding waves and nearby glyph dots,
     * so the icon is formed by the waves rather than pasted over them.
     * All trajectories are analytical: no per-frame randomized positions.
     */
    fun wave(seconds: Double, position: Float, phase: Float): Float =
        sin(seconds * 1.16 + position * 8.2 + phase).toFloat()

    fun iconDrift(seconds: Double, glyph: V195Dot): Pair<Float, Float> =
        (GLYPH_FLOW_AMPLITUDE * wave(seconds, glyph.y, glyph.phase)) to
            (GLYPH_FLOW_AMPLITUDE * wave(seconds, glyph.x, glyph.phase + 1.4f))
}
