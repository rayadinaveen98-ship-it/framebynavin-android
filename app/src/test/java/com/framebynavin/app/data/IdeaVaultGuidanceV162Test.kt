package com.framebynavin.app.data

import org.junit.Assert.*
import org.junit.Test

class IdeaVaultGuidanceV162Test {
    private fun idea(id: String, title: String, trashed: Boolean = false) =
        CreatorIdea(id = id, title = title, trashedAtMillis = if (trashed) 1000 else 0)

    @Test fun titleMatchingSuggestsExactAndCloseButDoesNotBlock() {
        val saved = listOf(
            idea("1", "How to stage a cinematic entry"),
            idea("2", "Lighting tricks on a movie set"),
        )
        assertEquals("1", IdeaVaultGuidance.similarTitles("How to stage a cinematic entry", saved).single().id)
        assertEquals("1", IdeaVaultGuidance.similarTitles("Cinematic entry - how to stage a", saved).single().id)
        assertTrue(IdeaVaultGuidance.similarTitles("Different project idea", saved).isEmpty())
        assertTrue(IdeaVaultGuidance.similarTitles("How to stage a cinematic entry", saved, excludingId = "1").isEmpty())
    }

    @Test fun trashedIdeasDoNotCauseFalseWarnings() {
        assertTrue(IdeaVaultGuidance.similarTitles(
            "Telugu action scene lighting",
            listOf(idea("gone", "Telugu action scene lighting", true))
        ).isEmpty())
    }

    @Test fun unicodeTitlesMatchWithoutAsciiAssumptions() {
        val saved = listOf(idea("t", "తెలుగు సినిమా సీన్ విశ్లేషణ"))
        assertEquals("t", IdeaVaultGuidance.similarTitles("తెలుగు సినిమా సీన్ విశ్లేషణ", saved).single().id)
    }

    @Test fun originalAudioAndProjectLinkStayWithReversibleTrash() {
        val voice = CreatorIdea(
            id = "voice", title = "Remember this",
            captureType = IdeaCaptureType.VOICE,
            audioLocalPath = "/private/voice_ideas/voice_idea_1.m4a",
            projectTaskId = "linked-project",
            status = IdeaStatus.CONVERTED,
        )
        val moved = voice.copy(trashedAtMillis = 456L, reminderAtMillis = 0L)
        assertTrue(moved.isTrashed)
        assertTrue(moved.hasOriginalRecording)
        assertEquals("linked-project", moved.projectTaskId)
        val restored = moved.copy(trashedAtMillis = 0L)
        assertFalse(restored.isTrashed)
        assertEquals(voice.audioLocalPath, restored.audioLocalPath)
        assertEquals(voice.status, restored.status)
    }

    @Test fun trashedIdeaRemindersCannotFireOrRecur() {
        val now = System.currentTimeMillis()
        val trashed = CreatorIdea(
            id = "reminder", title = "A scene to analyze",
            reminderAtMillis = now + 60_000,
            reminderCadence = IdeaReminderCadence.DAILY,
            trashedAtMillis = now,
        )
        assertFalse(IdeaReminderPolicy.isActionable(trashed))
        val normalized = IdeaReminderPolicy.normalize(trashed, now)
        assertEquals(0L, normalized.reminderAtMillis)
        assertEquals(IdeaReminderCadence.ONCE, normalized.reminderCadence)
    }
}
