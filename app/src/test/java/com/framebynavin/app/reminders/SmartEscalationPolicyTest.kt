package com.framebynavin.app.reminders

import com.framebynavin.app.data.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartEscalationPolicyTest {

    private val defaultConfig = SmartEscalationConfig()

    @Test
    fun defaultWindows_matchLockedProductRules() {
        assertEquals(0, SmartEscalationPolicy.requiredWindowMinutes(TaskPriority.NORMAL, defaultConfig))
        assertEquals(30, SmartEscalationPolicy.requiredWindowMinutes(TaskPriority.IMPORTANT, defaultConfig))
        assertEquals(45, SmartEscalationPolicy.requiredWindowMinutes(TaskPriority.CRITICAL, defaultConfig))
    }

    @Test
    fun customCriticalWindow_isSumOfEditableWaits() {
        val custom = SmartEscalationConfig(
            notificationToVoiceMinutes = 5,
            voiceToAlarmMinutes = 10,
            alarmToCriticalMinutes = 20,
        )
        assertEquals(35, SmartEscalationPolicy.requiredWindowMinutes(TaskPriority.CRITICAL, custom))
    }

    @Test
    fun gaps_areClampedToSupportedFiveToThirtyMinuteRange() {
        val normalized = SmartEscalationConfig(1, 99, 0).normalized()
        assertEquals(5, normalized.notificationToVoiceMinutes)
        assertEquals(30, normalized.voiceToAlarmMinutes)
        assertEquals(5, normalized.alarmToCriticalMinutes)
    }

    @Test
    fun importantWindow_rejectsTooLateReminder_andAcceptsExactWindow() {
        val reminderAt = 1_000_000L
        val twentyNineMinutesLater = reminderAt + 29 * 60_000L
        val thirtyMinutesLater = reminderAt + 30 * 60_000L

        assertFalse(
            SmartEscalationPolicy.isWindowValid(
                TaskPriority.IMPORTANT,
                reminderAt,
                twentyNineMinutesLater,
                defaultConfig,
            )
        )
        assertTrue(
            SmartEscalationPolicy.isWindowValid(
                TaskPriority.IMPORTANT,
                reminderAt,
                thirtyMinutesLater,
                defaultConfig,
            )
        )
    }

    @Test
    fun criticalCustomWindow_rejectsOneMinuteShort() {
        val config = SmartEscalationConfig(5, 10, 20)
        val reminderAt = 2_000_000L
        assertFalse(
            SmartEscalationPolicy.isWindowValid(
                TaskPriority.CRITICAL,
                reminderAt,
                reminderAt + 34 * 60_000L,
                config,
            )
        )
        assertTrue(
            SmartEscalationPolicy.isWindowValid(
                TaskPriority.CRITICAL,
                reminderAt,
                reminderAt + 35 * 60_000L,
                config,
            )
        )
    }

    @Test
    fun normalSmart_hasNoEscalationAfterSoftNotification() {
        assertNull(
            SmartEscalationPolicy.nextStage(
                TaskPriority.NORMAL,
                SmartEscalationScheduler.Stage.SOFT,
            )
        )
    }

    @Test
    fun importantSequence_isSoftVoiceAlarmThenStop() {
        assertEquals(
            SmartEscalationScheduler.Stage.VOICE,
            SmartEscalationPolicy.nextStage(TaskPriority.IMPORTANT, SmartEscalationScheduler.Stage.SOFT),
        )
        assertEquals(
            SmartEscalationScheduler.Stage.ALARM,
            SmartEscalationPolicy.nextStage(TaskPriority.IMPORTANT, SmartEscalationScheduler.Stage.VOICE),
        )
        assertNull(
            SmartEscalationPolicy.nextStage(TaskPriority.IMPORTANT, SmartEscalationScheduler.Stage.ALARM)
        )
    }

    @Test
    fun criticalSequence_isSoftVoiceAlarmCriticalThenStop() {
        assertEquals(
            SmartEscalationScheduler.Stage.VOICE,
            SmartEscalationPolicy.nextStage(TaskPriority.CRITICAL, SmartEscalationScheduler.Stage.SOFT),
        )
        assertEquals(
            SmartEscalationScheduler.Stage.ALARM,
            SmartEscalationPolicy.nextStage(TaskPriority.CRITICAL, SmartEscalationScheduler.Stage.VOICE),
        )
        assertEquals(
            SmartEscalationScheduler.Stage.CRITICAL,
            SmartEscalationPolicy.nextStage(TaskPriority.CRITICAL, SmartEscalationScheduler.Stage.ALARM),
        )
        assertNull(
            SmartEscalationPolicy.nextStage(TaskPriority.CRITICAL, SmartEscalationScheduler.Stage.CRITICAL)
        )
    }

    @Test
    fun availableWindow_neverBecomesNegative() {
        assertEquals(0, SmartEscalationPolicy.availableWindowMinutes(2_000L, 1_000L))
        assertEquals(0, SmartEscalationPolicy.availableWindowMinutes(0L, 99_000L))
    }
}
