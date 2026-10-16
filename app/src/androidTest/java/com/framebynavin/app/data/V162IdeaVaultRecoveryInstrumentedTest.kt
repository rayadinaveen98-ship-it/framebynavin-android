package com.framebynavin.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Exercise real DataStore encoding and recovery, not just in-memory copies. */
@RunWith(AndroidJUnit4::class)
class V162IdeaVaultRecoveryInstrumentedTest {
    @Test fun oldRecordsDecodeWithActiveDefault() = runBlocking {
        val store = IdeaVaultStore(ApplicationProvider.getApplicationContext<Context>())
        assertEquals(1, store.validateJson("""[{"id":"legacy","title":"Older saved idea"}]"""))
    }

    @Test fun recycleAndRestoreKeepsAudioAndProjectReference() = runBlocking {
        val store = IdeaVaultStore(ApplicationProvider.getApplicationContext<Context>())
        val original = store.load()
        val id = UUID.randomUUID().toString()
        val voice = CreatorIdea(
            id = id,
            title = "Recorded cinematic idea",
            projectTaskId = "project-connected",
            captureType = IdeaCaptureType.VOICE,
            audioLocalPath = "/files/voice_ideas/voice_idea_saved.m4a",
            audioDurationMillis = 3400,
            reminderAtMillis = 0L,
            status = IdeaStatus.CONVERTED,
            notes = "Do not lose this",
        )
        try {
            store.save(listOf(voice) + original)
            val now = System.currentTimeMillis().coerceAtLeast(1L)
            store.mutate { ideas ->
                ideas.map { if (it.id == id) it.copy(trashedAtMillis = now) else it }
            }
            val deleted = store.load().single { it.id == id }
            assertTrue(deleted.isTrashed)
            assertEquals(voice.audioLocalPath, deleted.audioLocalPath)
            assertTrue(deleted.hasOriginalRecording)
            assertEquals(voice.projectTaskId, deleted.projectTaskId)
            store.mutate { ideas ->
                ideas.map { if (it.id == id) it.copy(trashedAtMillis = 0L) else it }
            }
            val restored = store.load().single { it.id == id }
            assertFalse(restored.isTrashed)
            assertEquals(voice.notes, restored.notes)
            assertEquals(voice.projectTaskId, restored.projectTaskId)
            assertEquals(voice.audioLocalPath, restored.audioLocalPath)
        } finally {
            store.save(original)
        }
    }
}
