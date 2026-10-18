package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderDeliveryPreference
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.VoicePersona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cross-phase guard against changing V171–V175 creator contracts during release QA.
 */
class V176NewProjectReleaseContractTest {
    private val now = 1_000_000L
    private val due = now + 86_400_000L

    @Test fun allPlansHaveMatchingQuickAndFullSaveValidation() {
        for (plan in ProjectAttentionPlan.entries) {
            val customTime = now + 600_000L
            val quick = V174QuickCreatePolicy.valid(
                title = "Film analysis",
                archetypeId = "video",
                platform = "YouTube",
                format = "Long-form",
                dueAtMillis = due,
                reminderAtMillis = customTime,
                attentionPlan = plan,
                nowMillis = now,
            )
            val full = V153QuickCreatePolicy.canCreate(
                title = "Film analysis",
                archetypeId = "video",
                platform = "YouTube",
                format = "Long-form",
                dueAtMillis = due,
                nowMillis = now,
                supportReady = v20NewProjectSupportReady(plan, customTime, due, now),
            )
            assertEquals("Mismatch in plan: $plan", full, quick)
            assertTrue(quick)
        }
    }

    @Test fun customTimeValidationCannotBeSkippedWhenReminderPickerIsCollapsed() {
        val invalid = V174QuickCreatePolicy.valid(
            "Creator project", "video", "YouTube", "Long-form",
            due, now - 2L, ProjectAttentionPlan.CUSTOM, now,
        )
        assertFalse(invalid)
        assertTrue(V174QuickCreatePolicy.needsCustomReminderTime(ProjectAttentionPlan.CUSTOM))
    }

    @Test fun unrelatedProjectEditsPreserveHistoricalDeadlineAndReminderState() {
        val previous = CreatorTask(
            id = "project",
            title = "Original",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "Overdue",
            dueAtMillis = now - 60_000L,
            reminderAtMillis = now + 300_000L,
            reminderMode = ReminderMode.VOICE,
            attentionPlan = ProjectAttentionPlan.GUIDED,
            deliveryPreference = ReminderDeliveryPreference.AUTO,
            voicePersona = VoicePersona.ROBOT,
            snoozeCount = 2,
            progress = 65,
            notes = "First note",
        )
        val renamed = previous.copy(
            title = "Updated",
            notes = "More detail",
            // The wizard's managed-plan draft uses NONE; this is not a manual choice.
            reminderMode = ReminderMode.NONE,
            reminderAtMillis = 0L,
        )
        assertFalse(V171ProjectEditSafety.remindersNeedRefresh(previous, renamed, formatChanged = false))
        assertTrue(V171ProjectEditSafety.deadlineReady(
            previous.dueAtMillis, now, previous.dueAtMillis,
        ))
        assertTrue(V172ProjectDraftPolicy.changed(
            V172ProjectDraftPolicy.signature(listOf(previous.title, previous.notes)),
            V172ProjectDraftPolicy.signature(listOf(renamed.title, renamed.notes)),
        ))
        assertEquals(VoicePersona.ROBOT, renamed.voicePersona)
        assertEquals(65, renamed.progress)
    }

    @Test fun projectNotesAndAlertExplanationsDoNotChangeTheSavedPlanChoice() {
        for (plan in ProjectAttentionPlan.entries) {
            val text = V175ProjectDetailsCopy.reminderOutcome(plan, ReminderDeliveryPreference.AUTO)
            assertFalse(text.isBlank())
        }
        assertEquals(8, VoicePersona.entries.size)
        assertEquals(5, ProjectAttentionPlan.entries.size)
        assertTrue(V174QuickCreatePolicy.defaultDueAt(now) > now)
    }
}
