package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderDeliveryPreference
import com.framebynavin.app.data.ReminderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V171ProjectEditSafetyTest {
    private fun task() = CreatorTask(
        id = "existing",
        title = "Creator project",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "Overdue",
        dueAtMillis = 1_000L,
        attentionPlan = ProjectAttentionPlan.GUIDED,
        deliveryPreference = ReminderDeliveryPreference.AUTO,
        reminderMode = ReminderMode.VOICE,
        reminderEnabled = true,
        reminderAtMillis = 900L,
        snoozeCount = 2,
        workingUntilMillis = 1_500L,
        acknowledgedCheckpointStageId = "script",
    )

    @Test fun overdueDeadlineIsPreservedInsteadOfReplacedByFallback() {
        assertEquals(1_000L, V171ProjectEditSafety.initialDeadline(1_000L, 20_000L))
        assertEquals(20_000L, V171ProjectEditSafety.initialDeadline(null, 20_000L))
        assertTrue(V171ProjectEditSafety.deadlineReady(1_000L, 10_000L, 1_000L))
        assertFalse(V171ProjectEditSafety.deadlineReady(2_000L, 10_000L, 1_000L))
        assertTrue(V171ProjectEditSafety.deadlineReady(20_000L, 10_000L, 1_000L))
    }

    @Test fun elapsedCustomReminderMayBeKeptOnlyWhenOriginalScheduleIsUnchanged() {
        val policy = V171ProjectEditSafety
        assertTrue(policy.customReminderReady(
            ProjectAttentionPlan.CUSTOM, 700L, 1_000L, 10_000L,
            ProjectAttentionPlan.CUSTOM, 700L, 1_000L,
        ))
        assertFalse(policy.customReminderReady(
            ProjectAttentionPlan.CUSTOM, 700L, 12_000L, 10_000L,
            ProjectAttentionPlan.CUSTOM, 700L, 1_000L,
        ))
        assertFalse(policy.customReminderReady(
            ProjectAttentionPlan.CUSTOM, 800L, 1_000L, 10_000L,
            ProjectAttentionPlan.CUSTOM, 700L, 1_000L,
        ))
        assertTrue(policy.customReminderReady(
            ProjectAttentionPlan.CUSTOM, 11_000L, 12_000L, 10_000L,
            ProjectAttentionPlan.CUSTOM, 700L, 1_000L,
        ))
    }

    @Test fun editingTitleOrNotesDoesNotRescheduleGuidedVoiceOrClearSnooze() {
        val existing = task()
        // The composer deliberately passes NONE for a managed reminder: runtime VOICE remains authoritative.
        val requested = existing.copy(
            title = "Retitled",
            notes = "New notes",
            reminderMode = ReminderMode.NONE,
            reminderAtMillis = 0L,
        )
        assertFalse(V171ProjectEditSafety.remindersNeedRefresh(existing, requested, formatChanged = false))
    }

    @Test fun changedDeadlinePlanDeliveryOrFormatRecomputesReminders() {
        val existing = task()
        assertTrue(V171ProjectEditSafety.remindersNeedRefresh(
            existing, existing.copy(dueAtMillis = 30_000L), formatChanged = false,
        ))
        assertTrue(V171ProjectEditSafety.remindersNeedRefresh(
            existing, existing.copy(attentionPlan = ProjectAttentionPlan.OFF), formatChanged = false,
        ))
        assertTrue(V171ProjectEditSafety.remindersNeedRefresh(
            existing, existing.copy(deliveryPreference = ReminderDeliveryPreference.ALARM), formatChanged = false,
        ))
        assertTrue(V171ProjectEditSafety.remindersNeedRefresh(
            existing, existing.copy(notes = "Unrelated"), formatChanged = true,
        ))
    }

    @Test fun customReminderOnlyRecomputesWhenItsTimeChanges() {
        val original = task().copy(
            attentionPlan = ProjectAttentionPlan.CUSTOM,
            reminderMode = ReminderMode.VOICE,
            reminderAtMillis = 800L,
        )
        assertFalse(V171ProjectEditSafety.remindersNeedRefresh(
            original, original.copy(notes = "Note", reminderMode = ReminderMode.SIMPLE), formatChanged = false,
        ))
        assertTrue(V171ProjectEditSafety.remindersNeedRefresh(
            original, original.copy(reminderAtMillis = 950L), formatChanged = false,
        ))
    }
}
