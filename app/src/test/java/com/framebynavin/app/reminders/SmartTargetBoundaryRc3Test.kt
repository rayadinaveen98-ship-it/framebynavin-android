package com.framebynavin.app.reminders

import com.framebynavin.app.data.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmartTargetBoundaryRc3Test {
    private val target = 1_800_000_000_000L

    @Test
    fun candidateBeforeTargetPreservesPreferredStageAndTime() {
        val bounded = SmartTargetBoundary.bound(
            priority = TaskPriority.CRITICAL,
            preferredStage = SmartEscalationScheduler.Stage.VOICE,
            candidateAtMillis = target - 60_000L,
            finalTargetAtMillis = target,
        )

        assertEquals(SmartEscalationScheduler.Stage.VOICE, bounded?.stage)
        assertEquals(target - 60_000L, bounded?.atMillis)
    }

    @Test
    fun candidateAfterTargetCompressesImportantToAlarmAtTarget() {
        val bounded = SmartTargetBoundary.bound(
            priority = TaskPriority.IMPORTANT,
            preferredStage = SmartEscalationScheduler.Stage.VOICE,
            candidateAtMillis = target + 20 * 60_000L,
            finalTargetAtMillis = target,
        )

        assertEquals(SmartEscalationScheduler.Stage.ALARM, bounded?.stage)
        assertEquals(target, bounded?.atMillis)
    }

    @Test
    fun candidateAfterTargetCompressesCriticalToCriticalAtTarget() {
        val bounded = SmartTargetBoundary.bound(
            priority = TaskPriority.CRITICAL,
            preferredStage = SmartEscalationScheduler.Stage.ALARM,
            candidateAtMillis = target + 5 * 60_000L,
            finalTargetAtMillis = target,
        )

        assertEquals(SmartEscalationScheduler.Stage.CRITICAL, bounded?.stage)
        assertEquals(target, bounded?.atMillis)
    }

    @Test
    fun invalidTargetFailsClosed() {
        assertNull(
            SmartTargetBoundary.bound(
                priority = TaskPriority.NORMAL,
                preferredStage = SmartEscalationScheduler.Stage.SOFT,
                candidateAtMillis = target,
                finalTargetAtMillis = 0L,
            )
        )
    }
}
