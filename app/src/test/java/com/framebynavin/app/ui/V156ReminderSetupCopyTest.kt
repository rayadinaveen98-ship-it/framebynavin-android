package com.framebynavin.app.ui

import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderDeliveryPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Product language is presentation-only: it must never rename persisted enum values. */
class V156ReminderSetupCopyTest {
    @Test fun allFiveLegacyPlansHaveDistinctPlainLanguageLabelsAndExplanations() {
        val plans = ProjectAttentionPlan.entries
        assertEquals(5, plans.size)
        assertEquals(5, plans.map(V156ReminderSetupCopy::title).distinct().size)
        assertEquals(5, plans.map(V156ReminderSetupCopy::shortTitle).distinct().size)
        plans.forEach { plan ->
            assertTrue(V156ReminderSetupCopy.title(plan).isNotBlank())
            assertTrue(V156ReminderSetupCopy.description(plan).length > 25)
        }
    }

    @Test fun suggestedGuidedPlanExplainsThatVoiceOrAlarmMayBeUsed() {
        val hint = V156ReminderSetupCopy.description(ProjectAttentionPlan.GUIDED)
        assertTrue(hint.contains("voice", ignoreCase = true))
        assertTrue(hint.contains("alarm", ignoreCase = true))
        assertTrue(V156ReminderSetupCopy.title(ProjectAttentionPlan.GUIDED).contains("track"))
    }

    @Test fun urgentPlanDoesNotHidePossibleRingingAlarm() {
        val hint = V156ReminderSetupCopy.description(ProjectAttentionPlan.URGENT)
        assertTrue(hint.contains("alarm", ignoreCase = true))
        assertTrue(hint.contains("voice", ignoreCase = true))
    }

    @Test fun offNeverShowsHiddenDeliveryPreferenceInSummary() {
        assertEquals(
            "No reminders",
            V156ReminderSetupCopy.summary(ProjectAttentionPlan.OFF, ReminderDeliveryPreference.ALARM),
        )
    }

    @Test fun existingExplicitDeliveryPreferencesRemainDiscoverableInSummary() {
        assertTrue(
            V156ReminderSetupCopy.summary(ProjectAttentionPlan.CUSTOM, ReminderDeliveryPreference.VOICE)
                .contains("Voice alert"),
        )
        assertTrue(
            V156ReminderSetupCopy.summary(ProjectAttentionPlan.GUIDED, ReminderDeliveryPreference.NOTIFICATION)
                .contains("Notification only"),
        )
        assertFalse(
            V156ReminderSetupCopy.alertExplanation(ReminderDeliveryPreference.NOTIFICATION)
                .contains("automatic voice", ignoreCase = true) &&
            V156ReminderSetupCopy.alertExplanation(ReminderDeliveryPreference.NOTIFICATION)
                .startsWith("Always use a voice"),
        )
        assertEquals(ReminderDeliveryPreference.entries.size, 5)
    }

    @Test fun manualTimeValidationStillOnlyAppliesToCustomPlan() {
        val now = 10_000L
        val due = 30_000L
        for (plan in ProjectAttentionPlan.entries.filter { it != ProjectAttentionPlan.CUSTOM }) {
            assertTrue(v20NewProjectSupportReady(plan, 0L, due, now))
        }
        assertFalse(v20NewProjectSupportReady(ProjectAttentionPlan.CUSTOM, 0L, due, now))
        assertFalse(v20NewProjectSupportReady(ProjectAttentionPlan.CUSTOM, due + 1, due, now))
        assertTrue(v20NewProjectSupportReady(ProjectAttentionPlan.CUSTOM, now + 1, due, now))
    }
}
