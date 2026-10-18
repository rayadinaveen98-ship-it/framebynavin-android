package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V172ProjectDraftPolicyTest {
    @Test fun untouchedFormDoesNotRequestDiscardConfirmation() {
        val first = V172ProjectDraftPolicy.signature(listOf("Project", "YouTube", "2026-10-18"))
        assertFalse(V172ProjectDraftPolicy.changed(first, first))
    }

    @Test fun changingJustOneFieldTriggersDiscardConfirmation() {
        val baseline = V172ProjectDraftPolicy.signature(listOf("Draft", "notes", "GUIDED"))
        val modified = V172ProjectDraftPolicy.signature(listOf("Draft", "new notes", "GUIDED"))
        assertTrue(V172ProjectDraftPolicy.changed(baseline, modified))
    }

    @Test fun signaturesCannotCollideWhenTextContainsSeparatorsOrLengthTokens() {
        val first = V172ProjectDraftPolicy.signature(listOf("a:1b", "c"))
        val second = V172ProjectDraftPolicy.signature(listOf("a", "1b:c"))
        assertTrue(first != second)
        assertEquals(first, V172ProjectDraftPolicy.signature(listOf("a:1b", "c")))
    }

    @Test fun allUnfinishedFieldsCanBeComparedIncludingStylesAndReminderDelivery() {
        val baseline = listOf("Project", "film", "analysis", "YouTube", "Long-form", "VOICE", "styleA")
        val modified = baseline.toMutableList().also { it[it.lastIndex] = "styleB" }
        assertTrue(V172ProjectDraftPolicy.changed(
            V172ProjectDraftPolicy.signature(baseline),
            V172ProjectDraftPolicy.signature(modified),
        ))
    }

    @Test fun settingsDetourResumesButDiscardDoesNot() {
        assertTrue(V172ProjectDraftPolicy.resumeAfterSettings(true))
        assertFalse(V172ProjectDraftPolicy.resumeAfterSettings(false))
    }
}
