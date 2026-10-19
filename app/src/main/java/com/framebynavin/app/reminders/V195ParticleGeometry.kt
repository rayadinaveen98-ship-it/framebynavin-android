package com.framebynavin.app.reminders

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Pure, deterministic coordinates. No view instances, timers, Android API, or saved state. */
internal enum class V195ReminderGlyph { CLOCK, MICROPHONE }

internal data class V195Dot(
    val x: Float,
    val y: Float,
    val scatterX: Float,
    val scatterY: Float,
    val phase: Float,
    val speed: Float,
)

/**
 * All dots are drawn at the SAME radius. Shape strokes consist only of dot targets.
 * Discrete positions and scatter origins are created once per scene, never per frame.
 */
internal object V195ParticleGeometry {
    const val DEFAULT_DOTS = 420
    const val LOW_POWER_DOTS = 220

    fun build(glyph: V195ReminderGlyph, count: Int = DEFAULT_DOTS): List<V195Dot> {
        require(count in 24..500)
        val targets = when (glyph) {
            V195ReminderGlyph.CLOCK -> clock(count)
            V195ReminderGlyph.MICROPHONE -> microphone(count)
        }
        return targets.mapIndexed { i, point ->
            V195Dot(
                x = point.first,
                y = point.second,
                scatterX = unitHash(i, 11) * 2.8f - 1.4f,
                scatterY = unitHash(i, 29) * 2.8f - 1.4f,
                phase = unitHash(i, 47) * (2.0 * PI).toFloat(),
                speed = 0.8f + 2.2f * unitHash(i, 71),
            )
        }
    }

    private fun clock(count: Int): List<Pair<Float, Float>> {
        val ringCount = (count * 0.69f).toInt()
        val minuteCount = (count - ringCount) / 2
        val hourCount = count - ringCount - minuteCount
        return buildList(count) {
            repeat(ringCount) { i ->
                val angle = 2.0 * PI * i / ringCount - PI / 2.0
                add(0.9f * cos(angle).toFloat() to 0.9f * sin(angle).toFloat())
            }
            repeat(minuteCount) { i ->
                val t = (i + 1f) / minuteCount
                add(0.02f to -0.68f * t)
            }
            repeat(hourCount) { i ->
                val t = (i + 1f) / hourCount
                add(0.52f * t to 0.28f * t)
            }
        }
    }

    private fun microphone(count: Int): List<Pair<Float, Float>> {
        val capsuleCount = (count * 0.61f).toInt()
        val archCount = (count * 0.22f).toInt()
        val stemCount = (count * 0.075f).toInt()
        val baseCount = count - capsuleCount - archCount - stemCount
        val r = 0.29f
        // Mic capsule: top half-circle, right side, bottom half-circle, left side.
        return buildList(count) {
            repeat(capsuleCount) { i ->
                val t = i.toFloat() / capsuleCount
                val (x, y) = when {
                    t < .25f -> {
                        val angle = PI + (t / .25f) * PI
                        r * cos(angle).toFloat() to (-.51f + r * sin(angle).toFloat())
                    }
                    t < .50f -> r to (-.51f + ((t - .25f) / .25f) * .65f)
                    t < .75f -> {
                        val angle = ((t - .50f) / .25f) * PI
                        r * cos(angle).toFloat() to (.14f + r * sin(angle).toFloat())
                    }
                    else -> -r to (.14f - ((t - .75f) / .25f) * .65f)
                }
                add(x to y)
            }
            repeat(archCount) { i ->
                val angle = PI * i / (archCount - 1).coerceAtLeast(1)
                add(.52f * cos(angle).toFloat() to (.12f + .55f * sin(angle).toFloat()))
            }
            repeat(stemCount) { i ->
                val t = (i + 1f) / stemCount
                add(0f to (.67f + .22f * t))
            }
            repeat(baseCount) { i ->
                val t = i.toFloat() / (baseCount - 1).coerceAtLeast(1)
                add((t - .5f) * .62f to .92f)
            }
        }
    }

    private fun unitHash(index: Int, salt: Int): Float {
        var value = index * 1664525 + salt * 1013904223
        value = value xor (value ushr 16)
        value *= 0x7feb352d
        value = value xor (value ushr 15)
        return (value ushr 1).toDouble().div(Int.MAX_VALUE.toDouble()).toFloat()
    }
}
