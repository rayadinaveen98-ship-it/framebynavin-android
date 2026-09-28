package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceIdeaModelTest {
    @Test
    fun `existing ideas default to text capture`() {
        val idea = CreatorIdea(id = "idea-1", title = "Text idea")

        assertEquals(IdeaCaptureType.TEXT, idea.captureType)
        assertEquals(IdeaAudioSyncState.NONE, idea.audioSyncState)
        assertFalse(idea.hasOriginalRecording)
    }

    @Test
    fun `voice idea reports original recording only when local audio exists`() {
        val withRecording = CreatorIdea(
            id = "voice-1",
            title = "Voice idea",
            captureType = IdeaCaptureType.VOICE,
            audioLocalPath = "/data/user/0/com.framebynavin.app/files/voice_ideas/voice-1.m4a",
            audioDurationMillis = 42_000L,
            audioMimeType = "audio/mp4",
            audioSyncState = IdeaAudioSyncState.LOCAL_ONLY,
        )
        val withoutRecording = withRecording.copy(audioLocalPath = "")

        assertTrue(withRecording.hasOriginalRecording)
        assertFalse(withoutRecording.hasOriginalRecording)
    }

    @Test
    fun `voice metadata keeps transcript separate from original recording`() {
        val idea = CreatorIdea(
            id = "voice-2",
            title = "Scene breakdown thought",
            captureType = IdeaCaptureType.VOICE,
            audioLocalPath = "/tmp/voice-2.m4a",
            transcript = "This is a derived transcript.",
            tags = listOf("scene", "breakdown"),
        )

        assertTrue(idea.hasOriginalRecording)
        assertEquals("This is a derived transcript.", idea.transcript)
        assertEquals(listOf("scene", "breakdown"), idea.tags)
        assertEquals("/tmp/voice-2.m4a", idea.audioLocalPath)
    }

    @Test
    fun `project conversion notes preserve voice transcript after creator notes`() {
        val idea = CreatorIdea(
            id = "voice-3",
            title = "Rajamouli hero intro",
            topic = "Hero introductions",
            notes = "Compare Telugu and Hollywood staging.",
            captureType = IdeaCaptureType.VOICE,
            transcript = "Start with the emotional promise before the reveal.",
            transcriptionState = IdeaTranscriptionState.COMPLETED,
        )

        assertEquals(
            "From Idea Vault · Hero introductions\n" +
                "Compare Telugu and Hollywood staging.\n\n" +
                "Voice transcript\n" +
                "Start with the emotional promise before the reveal.",
            IdeaProjectBridge.projectNotes(idea),
        )
    }

    @Test
    fun `project conversion notes do not invent transcript section when transcript is absent`() {
        val idea = CreatorIdea(
            id = "voice-4",
            title = "Lighting idea",
            topic = "Lighting",
            captureType = IdeaCaptureType.VOICE,
            transcriptionState = IdeaTranscriptionState.FAILED,
            transcriptionError = "Recognizer unavailable",
        )

        assertEquals("From Idea Vault · Lighting", IdeaProjectBridge.projectNotes(idea))
    }
}
