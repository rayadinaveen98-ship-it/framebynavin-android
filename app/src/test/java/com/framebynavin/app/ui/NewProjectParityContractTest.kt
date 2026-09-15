package com.framebynavin.app.ui

import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderDeliveryPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewProjectParityContractTest {
    @Test
    fun projectSupportKeepsAllFivePlans() {
        assertEquals(
            listOf(
                ProjectAttentionPlan.OFF,
                ProjectAttentionPlan.LIGHT,
                ProjectAttentionPlan.GUIDED,
                ProjectAttentionPlan.URGENT,
                ProjectAttentionPlan.CUSTOM,
            ),
            V20NewProjectSupportPlans,
        )
    }

    @Test
    fun reminderStyleKeepsEveryDeliveryChoice() {
        assertEquals(ReminderDeliveryPreference.entries, V20NewProjectReminderStyles)
    }

    @Test
    fun onlyCustomRequiresAValidManualReminderTime() {
        val now = 1_000L
        val due = 10_000L
        assertTrue(v20NewProjectSupportReady(ProjectAttentionPlan.GUIDED, 0L, due, now))
        assertFalse(v20NewProjectSupportReady(ProjectAttentionPlan.CUSTOM, 0L, due, now))
        assertFalse(v20NewProjectSupportReady(ProjectAttentionPlan.CUSTOM, 11_000L, due, now))
        assertTrue(v20NewProjectSupportReady(ProjectAttentionPlan.CUSTOM, 5_000L, due, now))
    }
}
