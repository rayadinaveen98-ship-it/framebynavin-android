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
    @Test fun hugeScriptSignatureStaysBoundedAndNeverContainsDraftText() {
        val privateScript = "Scene in Telugu తెలుగు 🎬 — ".repeat(100_000)
        val token = V172ProjectDraftPolicy.signature(listOf("My Project", privateScript))
        assertEquals("Always a versioned SHA-256 hex fingerprint", 71, token.length)
        assertTrue(token.matches(Regex("sha256:[0-9a-f]{64}")))
        assertFalse(token.contains("My Project"))
        assertFalse(token.contains("Scene in Telugu"))
        assertEquals(token, V172ProjectDraftPolicy.signature(listOf("My Project", privateScript)))
    }

    @Test fun utf8FieldBoundariesAndEmptyFieldsAreUnique() {
        val a = V172ProjectDraftPolicy.signature(listOf("కథ 🎬", "film", ""))
        val b = V172ProjectDraftPolicy.signature(listOf("కథ 🎬film", ""))
        val c = V172ProjectDraftPolicy.signature(listOf("కథ 🎬", "film"))
        assertTrue(a != b)
        assertTrue(a != c)
        assertTrue(b != c)
        assertTrue(a != V172ProjectDraftPolicy.signature(listOf("కథ 🎬", "Film", "")))
    }

    @Test fun longDraftFieldChangesStillPromptForDiscard() {
        val words = "A".repeat(180_000)
        val baseline = V172ProjectDraftPolicy.signature(listOf("Title", words, "OFF"))
        val modified = V172ProjectDraftPolicy.signature(listOf("Title", words.dropLast(1) + "B", "OFF"))
        assertTrue(V172ProjectDraftPolicy.changed(baseline, modified))
        assertFalse(V172ProjectDraftPolicy.changed(baseline, baseline))
    }

}
