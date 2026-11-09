package com.framebynavin.app.cloud

import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.IdeaCaptureType
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** T1B3: local original recordings only; no network calls or owner audio. */
class TrustVoiceIdeaInventoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun sha(value: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(value)
            .joinToString("") { "%02x".format(it) }

    private fun local(root: File, id: String = "voice-1", bytes: ByteArray = "recording".toByteArray()):
        Pair<CreatorIdea, File> {
        val dir = File(root, "voice_ideas").apply { mkdirs() }
        val file = File(dir, "voice_idea_$id.m4a").apply { writeBytes(bytes) }
        val idea = CreatorIdea(
            id = id, title = "A bilingual idea",
            captureType = IdeaCaptureType.VOICE,
            audioLocalPath = file.absolutePath,
        )
        return idea to file
    }

    @Test fun ownedFinalRecordingProvesRealBytesAndDoesNotLeakFilePath() {
        val root = tmp.newFolder("owner-original")
        val (idea, file) = local(root)
        val first = requireNotNull(TrustVoiceIdeaInventory.inspect(root, listOf(idea)))
        assertEquals(1, first.records)
        assertEquals(file.length().toInt(), first.bytes)
        assertFalse(first.toString().contains(file.absolutePath))
        val changed = "different recording".toByteArray()
        file.writeBytes(changed)
        val second = requireNotNull(TrustVoiceIdeaInventory.inspect(root, listOf(idea)))
        assertNotEquals(first.sha256, second.sha256)
        assertEquals(changed.size, second.bytes)
    }

    @Test fun syncedRemoteDigestMustMatchActualLocalOriginal() {
        val root = tmp.newFolder("recording-synced")
        val bytes = "original m4a bytes".toByteArray()
        val (idea, file) = local(root, bytes = bytes)
        val remote = "storage://creator-voice-ideas/test-user/${idea.id}/media/${sha(bytes)}.m4a"
        assertEquals(1, TrustVoiceIdeaInventory.inspect(
            root, listOf(idea.copy(audioRemoteUrl = remote)),
        )?.records)
        file.writeText("different bytes")
        assertNull(TrustVoiceIdeaInventory.inspect(
            root, listOf(idea.copy(audioRemoteUrl = remote)),
        ))
    }

    @Test fun unfinishedAndUnreferencedTakesPreventFalseCompleteInventory() {
        val root = tmp.newFolder("recording-unfinished")
        val (idea, _) = local(root)
        val dir = File(root, "voice_ideas")
        File(dir, "voice_idea_new.recording.m4a").writeText("not yet saved")
        assertNull(TrustVoiceIdeaInventory.inspect(root, listOf(idea)))
        File(dir, "voice_idea_new.recording.m4a").delete()
        File(dir, "voice_idea_orphan.m4a").writeText("do not delete me")
        assertNull(TrustVoiceIdeaInventory.inspect(root, listOf(idea)))
        assertTrue(File(dir, "voice_idea_orphan.m4a").isFile)
    }

    @Test fun missingOrExternalOriginalNeverInventsLocalBackup() {
        val root = tmp.newFolder("no-recording")
        val (idea, file) = local(root)
        file.delete()
        assertNull(TrustVoiceIdeaInventory.inspect(root, listOf(idea)))
        assertNull(TrustVoiceIdeaInventory.inspect(root,
            listOf(idea.copy(audioLocalPath = "content://picked/document"))))
        assertNull(TrustVoiceIdeaInventory.inspect(root,
            listOf(idea.copy(audioLocalPath = ""))))
        val outside = File(tmp.root, "outside.m4a").apply { writeText("private") }
        assertNull(TrustVoiceIdeaInventory.inspect(root,
            listOf(idea.copy(audioLocalPath = outside.absolutePath))))
    }

    @Test fun fileSymlinksAndDuplicateOriginalsDoNotCount() {
        val root = tmp.newFolder("recording-symlink")
        val (idea, file) = local(root)
        val second = idea.copy(id = "voice-2")
        assertNull(TrustVoiceIdeaInventory.inspect(root, listOf(idea, second)))
        val raw = File(root, "voice_ideas/voice_idea_link.m4a")
        Files.createSymbolicLink(raw.toPath(), file.toPath())
        assertNull(TrustVoiceIdeaInventory.inspect(root, listOf(
            second.copy(audioLocalPath = raw.absolutePath),
        )))
    }

    @Test fun genuinelyEmptyVoiceFolderHasZeroBytesButUniversalBackupStaysBlocked() {
        val root = tmp.newFolder("unused-voice")
        val actual = requireNotNull(TrustVoiceIdeaInventory.inspect(root, emptyList()))
        assertEquals(0, actual.bytes)
        assertEquals(0, actual.records)
        val combined = TrustBackupInventoryPolicy.assemble(listOf(actual))
        assertTrue(TrustBackupDomain.CREATOR_MEDIA in combined.pendingDomains)
        assertFalse(combined.canClaimCompleteBackup)
        assertFalse(combined.canUploadAsUniversalBackup)
    }
}
