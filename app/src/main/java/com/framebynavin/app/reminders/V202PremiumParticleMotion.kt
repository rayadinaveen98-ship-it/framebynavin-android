package com.framebynavin.app.reminders

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Independent deterministic particles built only once; 2x the approved V195.3
 * ambient geometry in BOTH performance modes. Speeds are normalized screens/sec.
 */
internal data class V202PremiumParticle(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val colorIndex: Int,
    val colorPhase: Float,
    val colorRate: Float,
    val depth: Int,
)

internal object V202PremiumParticleGeometry {
    const val NORMAL_COUNT = V197AmbientGeometry.NORMAL_COUNT * 2 // 2,184
    const val LOW_POWER_COUNT = V197AmbientGeometry.LOW_POWER_COUNT * 2 // 986

    fun build(lowPower: Boolean): List<V202PremiumParticle> {
        val originals = V197AmbientGeometry.build(lowPower)
        return buildList(originals.size * 2) {
            originals.forEachIndexed { i, p ->
                for (copy in 0..1) {
                    val index = i * 2 + copy
                    val rotation = (2.0 * PI * noise(index, 61)).toFloat()
                    val speed = (.085f + .075f * noise(index, 103)) * (0.75f + p.depth * .16f)
                    // Second copy starts nearby but never on top of its parent.
                    val shiftX = if (copy == 0) 0f else (noise(index, 127) - .5f) * .020f
                    val shiftY = if (copy == 0) 0f else (noise(index, 149) - .5f) * .020f
                    add(V202PremiumParticle(
                        x = (p.x + shiftX).coerceIn(0f, 1f),
                        y = (p.y + shiftY).coerceIn(0f, 1f),
                        velocityX = speed * cos(rotation),
                        velocityY = speed * sin(rotation),
                        colorIndex = (noise(index, 173) * 8f).toInt().coerceIn(0, 7),
                        colorPhase = (2.0 * PI * noise(index, 197)).toFloat(),
                        colorRate = .42f + .6f * noise(index, 233),
                        depth = p.depth,
                    ))
                }
            }
        }
    }

    private fun noise(index: Int, salt: Int): Float {
        var n = index * 1664525 + salt * 1013904223
        n = n xor (n ushr 16)
        n *= 0x7feb352d
        n = n xor (n ushr 15)
        return (n ushr 1).toDouble().div(Int.MAX_VALUE).toFloat()
    }
}

/**
 * Infinite, independent edge-to-edge translation in normalized coordinates.
 * Analytic reflection never teleports particles; easing only within 6% of
 * each edge smoothly reduces velocity to ZERO before reversing.
 *
 * Time is monotonic Double seconds, not a 3.8s repeating animation phase.
 */
internal object V202InfiniteBounce {
    private const val EDGE = .06

    fun position(initial: Float, velocity: Float, elapsedSeconds: Double): Float {
        val travel = initial.toDouble() + velocity.toDouble() * elapsedSeconds
        // Positive modulo even when travelling backwards and after many hours.
        val w = ((travel % 2.0) + 2.0) % 2.0
        val reflected = if (w <= 1.0) w else 2.0 - w
        val eased = when {
            reflected < EDGE -> {
                val t = reflected / EDGE
                EDGE * (2.0 * t * t - t * t * t)
            }
            reflected > 1.0 - EDGE -> {
                val t = (1.0 - reflected) / EDGE
                1.0 - EDGE * (2.0 * t * t - t * t * t)
            }
            else -> reflected
        }
        return eased.coerceIn(0.0, 1.0).toFloat()
    }

    fun colorBlend(timeSeconds: Double, phase: Float, rate: Float): Float =
        (.5f + .5f * sin(timeSeconds * rate + phase).toFloat()).coerceIn(0f, 1f)
}
