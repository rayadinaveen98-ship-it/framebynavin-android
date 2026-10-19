package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V201ReminderMotionPolicyTest {
    @Test fun pressScaleGivesSubtleImmediateVisualFeedback() {
        val motion = V201ReminderMotionPolicy
        assertEquals(1f, motion.targetScale(false, prominent = false, motionEnabled = true), 0f)
        assertEquals(1f, motion.targetScale(false, prominent = true, motionEnabled = true), 0f)
        assertEquals(.976f, motion.targetScale(true, prominent = true, motionEnabled = true), 0f)
        assertEquals(.985f, motion.targetScale(true, prominent = false, motionEnabled = true), 0f)
    }

    @Test fun reducedMotionLeavesAllButtonGeometryUntouched() {
        val motion = V201ReminderMotionPolicy
        for (prominent in listOf(true, false)) for (pressed in listOf(true, false)) {
            assertEquals(1f, motion.targetScale(pressed, prominent, motionEnabled = false), 0f)
        }
    }

    @Test fun feedbackDurationsRemainBriefAndNonBlocking() {
        val motion = V201ReminderMotionPolicy
        assertTrue(motion.PRESS_MILLIS in 100..180)
        assertTrue(motion.OPTIONS_ENTER_MILLIS in 160..260)
        assertTrue(motion.OPTIONS_EXIT_MILLIS in 100..220)
        assertTrue(motion.FADE_MILLIS <= motion.OPTIONS_ENTER_MILLIS)
    }
}
