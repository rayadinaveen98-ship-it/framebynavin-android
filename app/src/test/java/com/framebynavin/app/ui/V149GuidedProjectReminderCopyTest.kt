package com.framebynavin.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class V149GuidedProjectReminderCopyTest {
    @Test
    fun firstProjectTourExplainsOptionalRemindersWithoutTechnicalPlanNames() {
        val copy = v149GuidedProjectReminderCopy(hasProjects = false)
        assertTrue(copy.body.contains("reminder", ignoreCase = true))
        assertTrue(copy.body.contains("remind", ignoreCase = true))
        assertTrue(copy.speech.contains("idea", ignoreCase = true))
        assertTrue(copy.speech.contains("project", ignoreCase = true))
        assertTrue(copy.speech.contains("reminder", ignoreCase = true))
    }

    @Test
    fun existingProjectTourStillExplainsNewProjectReminders() {
        val copy = v149GuidedProjectReminderCopy(hasProjects = true)
        assertTrue(copy.body.contains("project", ignoreCase = true))
        assertTrue(copy.body.contains("reminder", ignoreCase = true))
        assertTrue(copy.speech.contains("reminder", ignoreCase = true))
        assertTrue(copy.speech.contains("choose", ignoreCase = true))
    }
}
