package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Motion formulas must not require Android/notification permissions. */
class V199SparkMotionPolicyTest {
    @Test fun distinctAlertAndSpeakingPoses() {
        assertEquals(V199SparkExpression.ALERT,
            V199SparkMotionPolicy.expression(ReminderCheckInKind.ALARM))
        assertEquals(V199SparkExpression.SPEAK,
            V199SparkMotionPolicy.expression(ReminderCheckInKind.VOICE))
    }

    @Test fun calmMotionIsBoundedAndLoopsWithoutJumps() {
        val p = V199SparkMotionPolicy
        for (expression in V199SparkExpression.entries) {
            for (i in 0..100) {
                val t = p.FULL_TURN * i / 100f
                assertTrue(p.liftDp(expression, t) in -2.5f..0.01f)
                assertTrue(p.tiltDegrees(expression, t) in -1.2f..1.2f)
                assertTrue(p.scale(t) in 1f..1.013f)
                assertTrue(p.glow(t) in .64f..1.01f)
            }
            assertEquals(p.liftDp(expression, 0f),
                p.liftDp(expression, p.FULL_TURN), .00001f)
            assertEquals(p.tiltDegrees(expression, 0f),
                p.tiltDegrees(expression, p.FULL_TURN), .00001f)
        }
        assertEquals(3_200, p.CYCLE_MILLIS)
    }

    @Test fun voiceCueMovesIndependentlyButNeverDisappears() {
        val p = V199SparkMotionPolicy
        val levels = (0..3).map { p.voiceBar(it, 1.1f) }
        assertEquals(4, levels.distinct().size)
        assertTrue(levels.all { it in .33f..1.001f })
        for (index in 0..3) assertEquals(
            p.voiceBar(index, 0f), p.voiceBar(index, p.FULL_TURN), .00001f)
        try {
            p.voiceBar(4, 0f)
            fail("Unexpected fifth voice bar")
        } catch (_: IllegalArgumentException) {}
    }
}
