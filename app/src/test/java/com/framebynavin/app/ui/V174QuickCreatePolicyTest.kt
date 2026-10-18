package com.framebynavin.app.ui

import com.framebynavin.app.data.ProjectAttentionPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V174QuickCreatePolicyTest {
    @Test fun onlyNewQuickCreateUsesFullDayDefault() {
        val now = 1_000_000L
        assertEquals(now + 86_400_000L, V174QuickCreatePolicy.defaultDueAt(now))
    }

    @Test fun allFiveReminderPlansRemainRepresentable() {
        assertEquals(5, ProjectAttentionPlan.entries.size)
        for (plan in ProjectAttentionPlan.entries) {
            assertEquals(plan == ProjectAttentionPlan.CUSTOM,
                V174QuickCreatePolicy.needsCustomReminderTime(plan))
        }
    }

    @Test fun quickAndFullCreationUseSameRequiredFields() {
        val now = 100_000L
        for (plan in ProjectAttentionPlan.entries) {
            val due = now + 86_400_000L
            val reminder = now + 3_600_000L
            val support = v20NewProjectSupportReady(plan, reminder, due, now)
            val fullReady = V153QuickCreatePolicy.canCreate(
                "A creator project", "analysis", "YouTube", "Long-form", due, now, support,
            )
            val quickReady = V174QuickCreatePolicy.valid(
                "A creator project", "analysis", "YouTube", "Long-form", due, reminder, plan, now,
            )
            assertEquals("Reminder plan: $plan", fullReady, quickReady)
            assertTrue(quickReady)
        }
    }

    @Test fun fastPathCannotSaveEmptyTitlesPastDatesOrInvalidCustomReminders() {
        val now = 100_000L
        val due = now + 86_400_000L
        val ready = { title: String, at: Long, reminder: Long, plan: ProjectAttentionPlan ->
            V174QuickCreatePolicy.valid(title, "analysis", "YouTube", "Long-form", at, reminder, plan, now)
        }
        assertFalse(ready(" ", due, now + 60_000L, ProjectAttentionPlan.GUIDED))
        assertFalse(ready("Project", now, now + 60_000L, ProjectAttentionPlan.GUIDED))
        assertFalse(ready("Project", due, now - 1L, ProjectAttentionPlan.CUSTOM))
        assertFalse(ready("Project", due, due + 1L, ProjectAttentionPlan.CUSTOM))
        assertTrue(ready("Project", due, due - 5L, ProjectAttentionPlan.CUSTOM))
        assertTrue(ready("Project", due, 0L, ProjectAttentionPlan.OFF))
    }

    @Test fun quickCanCreateWithoutConfiguringRemindersManually() {
        assertTrue(V174QuickCreatePolicy.valid(
            "Project", "analysis", "YouTube", "Short",
            nowMillis = 1000L,
            dueAtMillis = 50_000L,
            reminderAtMillis = 0L,
            attentionPlan = ProjectAttentionPlan.GUIDED,
        ))
    }
}
