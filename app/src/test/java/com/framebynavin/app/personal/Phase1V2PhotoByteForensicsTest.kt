package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

/** Synthetic files only: no owner data, Room, cloud or alarm operations. */
class Phase1V2PhotoByteForensicsTest {
    private val space = "12537c2d-521d-40db-bf25-2d79bf0f1c2a"
    private val originalId = "ecb00ac2-134d-4195-b80b-7a41111a3021"
    private val incomingId = "fcb00ac2-134d-4195-b80b-7a41111a3021"
    private val a = "owner-original".toByteArray()
    private val b = "verified-restore".toByteArray()
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
    private fun photo(id: String, bytes: ByteArray) = V290SpacePhotoEntity(
        id = id, spaceId = space, takenAtMillis = 100L, addedAtMillis = 120L,
        fileName = "$id.jpg", sha256Hex = sha(bytes), widthPx = 500,
        heightPx = 700, note = "",
    )
    private val old = photo(originalId, a)
    private val addition = photo(incomingId, b)
    private fun fixture(block: (File, File) -> Unit) {
        val sandbox = Files.createTempDirectory("backlot-v2-photos-").toFile()
        val root = File(sandbox, "private-photos")
        try { block(sandbox, root) } finally { sandbox.deleteRecursively() }
    }
    private fun install(root: File, p: V290SpacePhotoEntity, bytes: ByteArray) {
        val dir = File(root, p.spaceId)
        assertTrue(dir.isDirectory || dir.mkdirs())
        File(dir, p.fileName).writeBytes(bytes)
    }
    private fun inspect(
        stage: Phase1V2RestoreStep, root: File,
        rows: List<V290SpacePhotoEntity>,
        before: List<V290SpacePhotoEntity> = listOf(old),
        after: List<V290SpacePhotoEntity> = listOf(old, addition),
    ) = Phase1V2PhotoByteForensics.inspect(stage, root, before, after, rows)

    @Test fun intactOriginalBeforePhotoStageIsByteVerifiedNotAuthorized() = fixture { _, root ->
        install(root, old, a)
        val proof = inspect(Phase1V2RestoreStep.SPACES_WRITTEN, root, listOf(old))
        assertEquals(1, proof.verifiedFinalFiles)
        assertEquals(0, proof.verifiedNewFiles)
        assertFalse(proof.requiresOwnerReview)
        assertFalse(proof.mayWriteLiveData)
        assertFalse(proof.mayAutoResume)
        assertFalse(proof.mayReleaseJournal)
        assertFalse(proof.mayDeleteAnyFile)
    }

    @Test fun activeBeforePhotosAcceptsAnUnfinishedPendingCopyOnlyAsEvidence() =
        fixture { _, root ->
            install(root, old, a)
            val pending = File(File(root, space), ".$incomingId.restore.pending")
            pending.writeBytes(byteArrayOf(1, 2, 3))
            val proof = inspect(
                Phase1V2RestoreStep.BEFORE_PHOTOS, root, listOf(old))
            assertEquals(1, proof.pendingCopies)
            assertEquals(0, proof.verifiedNewFiles)
            assertTrue(proof.requiresOwnerReview)
            assertFalse(proof.mayAutoResume)
            assertTrue(pending.exists())
        }

    @Test fun validNewPhotoInstalledBeforeRoomRowIsForensicNotAutomaticRecovery() =
        fixture { _, root ->
            install(root, old, a)
            install(root, addition, b)
            val evidence = inspect(
                Phase1V2RestoreStep.BEFORE_PHOTOS, root, listOf(old))
            assertEquals(1, evidence.verifiedNewFiles)
            assertEquals(1, evidence.observedRecords)
            assertTrue(evidence.requiresOwnerReview)
            assertFalse(evidence.mayWriteLiveData)
        }

    @Test fun completedPhotoPhaseNeedsAllRoomRowsAndAllExactBytes() = fixture { _, root ->
        install(root, old, a)
        install(root, addition, b)
        val evidence = inspect(
            Phase1V2RestoreStep.PHOTOS_WRITTEN, root, listOf(old, addition))
        assertEquals(2, evidence.verifiedFinalFiles)
        assertEquals(1, evidence.verifiedNewFiles)
        assertEquals(0, evidence.pendingCopies)
        assertFalse(evidence.mayReleaseJournal)
    }

    @Test fun missingRestoredPhotoAtCompletedCheckpointFailsClosed() = fixture { _, root ->
        install(root, old, a)
        assertThrows(IllegalArgumentException::class.java) {
            inspect(Phase1V2RestoreStep.PHOTOS_WRITTEN, root, listOf(old, addition))
        }
    }

    @Test fun changedPhotoBytesCannotPassEvenWhenRoomMetadataMatches() = fixture { _, root ->
        install(root, old, a)
        install(root, addition, "tampered".toByteArray())
        assertThrows(IllegalArgumentException::class.java) {
            inspect(Phase1V2RestoreStep.PHOTOS_WRITTEN, root, listOf(old, addition))
        }
    }

    @Test fun unknownNewPhotoAppearingBeforeCheckpointIsRejected() = fixture { _, root ->
        install(root, old, a)
        install(root, addition, b)
        assertThrows(IllegalArgumentException::class.java) {
            inspect(Phase1V2RestoreStep.SPACES_WRITTEN, root, listOf(old))
        }
    }

    @Test fun interruptedPendingCopyCannotBeCalledCompleted() = fixture { _, root ->
        install(root, old, a)
        install(root, addition, b)
        File(File(root, space), ".$incomingId.restore.pending").writeText("partial")
        assertThrows(IllegalArgumentException::class.java) {
            inspect(Phase1V2RestoreStep.VERIFYING, root, listOf(old, addition))
        }
    }

    @Test fun unknownFilesAndUnrelatedSpaceFoldersAreRejected() = fixture { _, root ->
        install(root, old, a)
        File(File(root, space), "random.txt").writeText("unexpected")
        assertThrows(IllegalArgumentException::class.java) {
            inspect(Phase1V2RestoreStep.BEFORE_PHOTOS, root, listOf(old))
        }
    }

    @Test fun originalPhotoMetadataCannotBeReplacedOrRemovedFromTarget() =
        fixture { _, root ->
            install(root, old, a)
            assertThrows(IllegalArgumentException::class.java) {
                inspect(Phase1V2RestoreStep.BEFORE_PHOTOS, root, listOf(old),
                    after = listOf(addition))
            }
        }

    @Test fun symlinkCannotMasqueradeAsOwnerJpeg() = fixture { sandbox, root ->
        install(root, old, a)
        val file = File(File(root, space), old.fileName)
        assertTrue(file.delete())
        val external = File(sandbox, "outside.jpg").apply { writeBytes(a) }
        Files.createSymbolicLink(file.toPath(), external.toPath())
        assertThrows(IllegalArgumentException::class.java) {
            inspect(Phase1V2RestoreStep.SPACES_WRITTEN, root, listOf(old))
        }
        assertEquals(a.toList(), external.readBytes().toList())
    }

    @Test fun emptyPhotoInventoryDoesNotCreateDirectories() = fixture { _, root ->
        val proof = inspect(
            Phase1V2RestoreStep.VERIFYING, root, emptyList(),
            before = emptyList(), after = emptyList())
        assertEquals(0, proof.verifiedFinalFiles)
        assertFalse(root.exists())
    }
}
