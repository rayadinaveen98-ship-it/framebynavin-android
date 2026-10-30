package com.framebynavin.app.reminders

import kotlin.math.PI

/**
 * Independently distributed full-screen particles. Unlike the V195 glyph
 * targets, these cover the viewport even while the icon is visible.
 *
 * Stratified jitter avoids an accidental empty screen or an obvious grid.
 * The engine is deterministic and allocated only once for a reminder scene.
 */
internal data class V197AmbientParticle(
    val x: Float,
    val y: Float,
    val phase: Float,
    val speed: Float,
    val driftX: Float,
    val driftY: Float,
    val palette: Int,
    /** 0 far, 1 mid, 2 near: depth affects brightness, glow and drift. */
    val depth: Int,
)

internal object V197AmbientGeometry {
    private const val NORMAL_COLS = 26
    private const val NORMAL_ROWS = 42
    private const val LOW_POWER_COLS = 17
    private const val LOW_POWER_ROWS = 29
    const val NORMAL_COUNT = NORMAL_COLS * NORMAL_ROWS
    const val LOW_POWER_COUNT = LOW_POWER_COLS * LOW_POWER_ROWS

    fun build(lowPower: Boolean = false): List<V197AmbientParticle> {
        val cols = if (lowPower) LOW_POWER_COLS else NORMAL_COLS
        val rows = if (lowPower) LOW_POWER_ROWS else NORMAL_ROWS
        return buildList(cols * rows) {
            for (row in 0 until rows) for (col in 0 until cols) {
                val index = row * cols + col
                // Slight per-cell randomness keeps full-screen coverage predictable.
                val x = (col + .14f + .72f * noise(index, 11)) / cols
                val y = (row + .14f + .72f * noise(index, 29)) / rows
                val depth = (row + 2 * col) % 3
                add(
                    V197AmbientParticle(
                        x = x.coerceIn(0f, 1f),
                        y = y.coerceIn(0f, 1f),
                        phase = (2 * PI * noise(index, 47)).toFloat(),
                        // Whole-number harmonics ensure no jump on animation repeat.
                        speed = (1 + (noise(index, 83) * 3f).toInt().coerceIn(0, 2)).toFloat(),
                        driftX = (noise(index, 113) * 2f - 1f) * (.015f + depth * .010f),
                        driftY = (noise(index, 131) * 2f - 1f) * (.024f + depth * .013f),
                        palette = (noise(index, 173) * 6f).toInt().coerceIn(0, 5),
                        depth = depth,
                    ),
                )
            }
        }
    }

    private fun noise(index: Int, salt: Int): Float {
        var value = index * 1664525 + salt * 1013904223
        value = value xor (value ushr 16)
        value *= 0x7feb352d
        value = value xor (value ushr 15)
        return (value ushr 1).toDouble().div(Int.MAX_VALUE.toDouble()).toFloat()
    }
}

/** Explicit policy: no ticking timer may force the reveal for a normal user. */
internal object V197ReminderRevealPolicy {
    fun startRevealed(motionEnabled: Boolean): Boolean = !motionEnabled
}
