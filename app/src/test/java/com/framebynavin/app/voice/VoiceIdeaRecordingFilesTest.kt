package com.framebynavin.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceIdeaRecordingFilesTest {
    @Test
    fun `successful replacement removes previous take only`() {
        val previous = VoiceIdeaRecording("/old.m4a", 1_000L)
        val replacement = VoiceIdeaRecording("/new.m4a", 2_000L)
        val deleted = mutableListOf<String>()

        val accepted = VoiceIdeaRecordingFiles.replace(previous, replacement) { path ->
            deleted += path
            true
        }

        assertTrue(accepted)
        assertEquals(listOf("/old.m4a"), deleted)
    }

    @Test
    fun `failed replacement keeps previous take and discards replacement`() {
        val previous = VoiceIdeaRecording("/old.m4a", 1_000L)
        val replacement = VoiceIdeaRecording("/new.m4a", 2_000L)
        val deleted = mutableListOf<String>()

        val accepted = VoiceIdeaRecordingFiles.replace(previous, replacement) { path ->
            deleted += path
            path != "/old.m4a"
        }

        assertFalse(accepted)
        assertEquals(listOf("/old.m4a", "/new.m4a"), deleted)
    }

    @Test
    fun `first recording needs no ownership cleanup`() {
        val replacement = VoiceIdeaRecording("/new.m4a", 2_000L)
        val deleted = mutableListOf<String>()

        val accepted = VoiceIdeaRecordingFiles.replace(null, replacement) { path ->
            deleted += path
            true
        }

        assertTrue(accepted)
        assertTrue(deleted.isEmpty())
    }

    @Test
    fun `remove keeps reference when file deletion fails`() {
        val recording = VoiceIdeaRecording("/keep.m4a", 1_000L)

        assertFalse(VoiceIdeaRecordingFiles.remove(recording) { false })
        assertTrue(VoiceIdeaRecordingFiles.remove(recording) { true })
    }
}
