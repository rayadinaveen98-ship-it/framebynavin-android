package com.framebynavin.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class V149GuidedProjectReminderCopyTest {
    @Test
    fun firstProjectTourTeachesRealProjectSupportReminderStep() {
        val copy = v149GuidedProjectReminderCopy(hasProjects = false)
        assertTrue(copy.body.contains("Project Support", ignoreCase = true))
        assertTrue(copy.body.contains("remind", ignoreCase = true))
        assertTrue(copy.speech.contains("Light"))
        assertTrue(copy.speech.contains("Guided"))
        assertTrue(copy.speech.contains("Urgent"))
        assertTrue(copy.speech.contains("Custom"))
        assertTrue(copy.speech.contains("reminder", ignoreCase = true))
    }

    @Test
    fun existingProjectTourStillExplainsWhereNewReminderSetupLives() {
        val copy = v149GuidedProjectReminderCopy(hasProjects = true)
        assertTrue(copy.body.contains("Project Support", ignoreCase = true))
        assertTrue(copy.body.contains("reminder", ignoreCase = true))
        assertTrue(copy.speech.contains("right time", ignoreCase = true))
    }
}
