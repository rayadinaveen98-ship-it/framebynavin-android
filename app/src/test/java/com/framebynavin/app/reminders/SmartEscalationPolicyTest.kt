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
        val custom = SmartEscalationConfig(5, 10, 20)
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
    fun availableWindow_isNowToFinalReminderTarget_notReminderToPublish() {
        val now = 1_000_000L
        val reminder = now + 10 * 60_000L
        val publish = reminder + 390 * 60_000L

        assertEquals(10, SmartEscalationPolicy.availableWindowMinutes(now, reminder))
        // Publish time is deliberately irrelevant to Smart window calculation.
        assertEquals(390, (publish - reminder) / 60_000L)
    }

    @Test
    fun screenshotCase_criticalFiveFiveFive_isInvalidWithOnlyTenMinutesToReminder() {
        val config = SmartEscalationConfig(5, 5, 5)
        val now = 9 * 60 * 60_000L + 20 * 60_000L // 09:20
        val reminder = 9 * 60 * 60_000L + 30 * 60_000L // 09:30

        assertEquals(15, SmartEscalationPolicy.requiredWindowMinutes(TaskPriority.CRITICAL, config))
        assertFalse(SmartEscalationPolicy.isWindowValid(TaskPriority.CRITICAL, now, reminder, config))
    }

    @Test
    fun screenshotCase_importantFiveFive_isValidAtExactTenMinutes_andInvalidOneSecondLater() {
        val config = SmartEscalationConfig(5, 5, 15)
        val reminder = 9 * 60 * 60_000L + 30 * 60_000L
        val exactTenMinutesBefore = reminder - 10 * 60_000L
        val oneSecondInsideWindow = exactTenMinutesBefore + 1_000L

        assertTrue(SmartEscalationPolicy.isWindowValid(TaskPriority.IMPORTANT, exactTenMinutesBefore, reminder, config))
        assertFalse(SmartEscalationPolicy.isWindowValid(TaskPriority.IMPORTANT, oneSecondInsideWindow, reminder, config))
    }

    @Test
    fun firstStage_isScheduledBackwardSoFinalStageLandsOnReminderTarget() {
        val target = 10_000_000L
        val important = SmartEscalationConfig(5, 10, 15)
        val critical = SmartEscalationConfig(5, 10, 20)

        assertEquals(target - 15 * 60_000L, SmartEscalationPolicy.firstStageAtMillis(TaskPriority.IMPORTANT, target, important))
        assertEquals(target - 35 * 60_000L, SmartEscalationPolicy.firstStageAtMillis(TaskPriority.CRITICAL, target, critical))
        assertEquals(target, SmartEscalationPolicy.firstStageAtMillis(TaskPriority.NORMAL, target, defaultConfig))
    }

    @Test
    fun normalSmart_hasNoEscalationAfterSoftNotification() {
        assertNull(SmartEscalationPolicy.nextStage(TaskPriority.NORMAL, SmartEscalationScheduler.Stage.SOFT))
    }

    @Test
    fun importantSequence_isSoftVoiceAlarmThenStop() {
        assertEquals(SmartEscalationScheduler.Stage.VOICE, SmartEscalationPolicy.nextStage(TaskPriority.IMPORTANT, SmartEscalationScheduler.Stage.SOFT))
        assertEquals(SmartEscalationScheduler.Stage.ALARM, SmartEscalationPolicy.nextStage(TaskPriority.IMPORTANT, SmartEscalationScheduler.Stage.VOICE))
        assertNull(SmartEscalationPolicy.nextStage(TaskPriority.IMPORTANT, SmartEscalationScheduler.Stage.ALARM))
    }

    @Test
    fun criticalSequence_isSoftVoiceAlarmCriticalThenStop() {
        assertEquals(SmartEscalationScheduler.Stage.VOICE, SmartEscalationPolicy.nextStage(TaskPriority.CRITICAL, SmartEscalationScheduler.Stage.SOFT))
        assertEquals(SmartEscalationScheduler.Stage.ALARM, SmartEscalationPolicy.nextStage(TaskPriority.CRITICAL, SmartEscalationScheduler.Stage.VOICE))
        assertEquals(SmartEscalationScheduler.Stage.CRITICAL, SmartEscalationPolicy.nextStage(TaskPriority.CRITICAL, SmartEscalationScheduler.Stage.ALARM))
        assertNull(SmartEscalationPolicy.nextStage(TaskPriority.CRITICAL, SmartEscalationScheduler.Stage.CRITICAL))
    }

    @Test
    fun availableWindow_neverBecomesNegative() {
        assertEquals(0, SmartEscalationPolicy.availableWindowMinutes(2_000L, 1_000L))
        assertEquals(0, SmartEscalationPolicy.availableWindowMinutes(99_000L, 99_000L))
    }
}
