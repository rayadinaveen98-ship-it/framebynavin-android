package com.framebynavin.app.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VoiceIdeaStorageTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `snapshot separates referenced old orphan and recent unreferenced recordings`() {
        val root = temporaryFolder.newFolder("voice_ideas")
        val now = 1_000_000L
        val referenced = voiceFile(root, "voice_idea_referenced.m4a", 10, now - 20_000L)
        val orphan = voiceFile(root, "voice_idea_orphan.m4a", 20, now - 20_000L)
        val recent = voiceFile(root, "voice_idea_recent.m4a", 30, now - 1_000L)
        voiceFile(root, "voice_idea_working.recording.m4a", 40, now - 20_000L)
        voiceFile(root, "foreign.m4a", 50, now - 20_000L)
        val storage = VoiceIdeaStorage(root, nowMillis = { now }, orphanGraceMillis = 5_000L)
        val ideas = listOf(
            CreatorIdea(
                id = "voice-1",
                title = "Referenced",
                captureType = IdeaCaptureType.VOICE,
                audioLocalPath = referenced.absolutePath,
            ),
        )

        val snapshot = storage.snapshot(ideas)

        assertEquals(1, snapshot.referencedCount)
        assertEquals(10L, snapshot.referencedBytes)
        assertEquals(1, snapshot.orphanCount)
        assertEquals(20L, snapshot.orphanBytes)
        assertEquals(1, snapshot.recentUnreferencedCount)
        assertEquals(30L, snapshot.recentUnreferencedBytes)
        assertEquals(3, snapshot.totalOwnedCount)
        assertEquals(60L, snapshot.totalOwnedBytes)
        assertEquals(10L, storage.recordingSizeBytes(ideas.single()))
        assertTrue(orphan.exists())
        assertTrue(recent.exists())
    }

    @Test
    fun `cleanup deletes only aged unreferenced Backlot finals`() {
        val root = temporaryFolder.newFolder("voice_ideas-cleanup")
        val now = 2_000_000L
        val referenced = voiceFile(root, "voice_idea_keep.m4a", 11, now - 50_000L)
        val orphan = voiceFile(root, "voice_idea_delete.m4a", 22, now - 50_000L)
        val recent = voiceFile(root, "voice_idea_recent.m4a", 33, now - 500L)
        val working = voiceFile(root, "voice_idea_capture.recording.m4a", 44, now - 50_000L)
        val storage = VoiceIdeaStorage(root, nowMillis = { now }, orphanGraceMillis = 5_000L)
        val ideas = listOf(
            CreatorIdea(id = "keep", title = "Keep", audioLocalPath = referenced.absolutePath),
        )

        val result = storage.cleanupOrphans(ideas)

        assertEquals(1, result.deletedCount)
        assertEquals(22L, result.deletedBytes)
        assertEquals(0, result.failedCount)
        assertTrue(referenced.exists())
        assertFalse(orphan.exists())
        assertTrue(recent.exists())
        assertTrue(working.exists())
    }

    @Test
    fun `recording size rejects paths outside owned directory`() {
        val root = temporaryFolder.newFolder("voice_ideas-owned")
        val outside = temporaryFolder.newFile("voice_idea_outside.m4a").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val storage = VoiceIdeaStorage(root)

        assertNull(storage.recordingSizeBytes(CreatorIdea(id = "outside", title = "Outside", audioLocalPath = outside.absolutePath)))
    }

    @Test
    fun `byte formatter stays readable across units`() {
        assertEquals("0 B", formatVoiceStorageBytes(0))
        assertEquals("1.0 KB", formatVoiceStorageBytes(1_024))
        assertEquals("1.0 MB", formatVoiceStorageBytes(1_024L * 1_024L))
    }

    private fun voiceFile(root: File, name: String, size: Int, modifiedAt: Long): File =
        File(root, name).apply {
            writeBytes(ByteArray(size) { it.toByte() })
            assertTrue(setLastModified(modifiedAt))
        }
}
