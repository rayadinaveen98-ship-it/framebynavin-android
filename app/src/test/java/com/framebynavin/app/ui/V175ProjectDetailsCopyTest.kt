package com.framebynavin.app.ui

import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderDeliveryPreference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V175ProjectDetailsCopyTest {
    @Test fun eachPlanHasClearOutcomeAndOffDoesNotPromiseAlerts() {
        for (plan in ProjectAttentionPlan.entries) {
            val text = V175ProjectDetailsCopy.reminderOutcome(plan, ReminderDeliveryPreference.AUTO)
            assertFalse(text.isBlank())
            if (plan == ProjectAttentionPlan.OFF) {
                assertTrue(text.contains("No alerts"))
                assertFalse(text.contains("Backlot chooses"))
            }
        }
    }

    @Test fun explicitDeliveryModesRemainDistinguishable() {
        val plan = ProjectAttentionPlan.CUSTOM
        val notification = V175ProjectDetailsCopy.reminderOutcome(plan, ReminderDeliveryPreference.NOTIFICATION)
        val voice = V175ProjectDetailsCopy.reminderOutcome(plan, ReminderDeliveryPreference.VOICE)
        val alarm = V175ProjectDetailsCopy.reminderOutcome(plan, ReminderDeliveryPreference.ALARM)
        val smart = V175ProjectDetailsCopy.reminderOutcome(plan, ReminderDeliveryPreference.SMART)
        assertTrue(notification.contains("Notifications only"))
        assertTrue(voice.contains("spoken"))
        assertTrue(alarm.contains("alarm"))
        assertTrue(smart.contains("escalate"))
    }

    @Test fun permissionsWarningIsScopedToRelevantMissingSetup() {
        assertNull(V175ProjectDetailsCopy.permissionsWarning(false, false))
        assertNull(V175ProjectDetailsCopy.permissionsWarning(true, true))
        val warning = V175ProjectDetailsCopy.permissionsWarning(true, false)
        assertNotNull(warning)
        assertTrue(warning!!.contains("Settings"))
    }
}
