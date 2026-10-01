package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorIdeaSharedAdapterTest {
    @Test
    fun voiceIdeaCrossPlatformRecordNeverCarriesAndroidLocalPath() {
        val android = CreatorIdea(
            id = "voice-1",
            title = "Voice idea",
            category = IdeaCategory.CONTENT_IDEA,
            status = IdeaStatus.READY_TO_PRODUCE,
            potential = IdeaPotential.HIGH,
            captureType = IdeaCaptureType.VOICE,
            audioLocalPath = "/data/user/0/com.framebynavin.app/files/voice_ideas/voice_idea_1.m4a",
            audioRemoteUrl = "storage://creator-voice-ideas/user/voice-1/media/hash.m4a",
            audioDurationMillis = 42_000L,
            audioMimeType = "audio/mp4",
            audioSyncState = IdeaAudioSyncState.SYNCED,
            transcript = "A creator thought",
            transcriptionState = IdeaTranscriptionState.COMPLETED,
            tags = listOf("film", "analysis"),
        )

        val shared = android.toSharedIdea()
        assertTrue(shared.isVoice)
        assertTrue(shared.hasProtectedRecording)
        assertFalse(shared.toString().contains("/data/user/0"))

        val restored = shared.toAndroidIdea(
            audioLocalPath = "/data/user/0/com.framebynavin.app/files/voice_ideas/restored.m4a"
        )
        assertEquals(android.id, restored.id)
        assertEquals(android.status, restored.status)
        assertEquals(android.potential, restored.potential)
        assertEquals(android.audioRemoteUrl, restored.audioRemoteUrl)
        assertEquals(android.transcript, restored.transcript)
        assertEquals("/data/user/0/com.framebynavin.app/files/voice_ideas/restored.m4a", restored.audioLocalPath)
    }
}
