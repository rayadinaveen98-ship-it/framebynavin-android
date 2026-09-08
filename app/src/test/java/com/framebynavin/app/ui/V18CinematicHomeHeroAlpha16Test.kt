package com.framebynavin.app.ui

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V18CinematicHomeHeroAlpha16Test {
    @Test
    fun motionPlansStaySubtleAndLoopDeterministically() {
        (0 until 12).forEach { index ->
            val plan = V18CinematicHeroMotion.planFor(index)
            assertTrue(plan.startScale in 1.0f..1.08f)
            assertTrue(plan.endScale in 1.0f..1.08f)
            val movement = abs(plan.endScale - plan.startScale) +
                abs(plan.endX - plan.startX) + abs(plan.endY - plan.startY)
            assertTrue(movement > 0.01f)
        }
        assertEquals(V18CinematicHeroMotion.planFor(0), V18CinematicHeroMotion.planFor(6))
    }
}
