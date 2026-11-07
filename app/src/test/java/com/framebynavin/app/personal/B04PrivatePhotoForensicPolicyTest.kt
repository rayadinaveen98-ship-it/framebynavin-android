package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class B04PrivatePhotoForensicPolicyTest {
    private val space = "afcd17ed-5fd6-4a60-b8cb-31484ee653e7"
    private val oldId = "729d2bfa-af37-4836-af9e-c14c3d836421"
    private val newId = "7fa4f5ce-2a88-4df0-a199-cc246d4937a2"
    private val first = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1, 2, 0xd9.toByte())
    private val second = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 3, 4, 0xd9.toByte())
    private fun photo(id: String, bytes: ByteArray): V290SpacePhotoEntity =
        V290SpacePhotoEntity(
            id = id, spaceId = space, takenAtMillis = 100L,
            addedAtMillis = 200L, fileName = "$id.jpg",
            sha256Hex = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) },
            widthPx = 16, heightPx = 16, note = "",
        )
    private val old = photo(oldId, first)
    private val fresh = photo(newId, second)
    private fun sandbox(test: (File) -> Unit) {
        val directory = Files.createTempDirectory("backlot-b4-photo-").toFile()
        try { test(File(directory, "photos")) }
        finally { directory.deleteRecursively() }
    }
    private fun save(root: File, photo: V290SpacePhotoEntity, bytes: ByteArray) {
        val parent = File(root, photo.spaceId)
        check(parent.isDirectory || parent.mkdirs())
        File(parent, photo.fileName).writeBytes(bytes)
    }
    private fun inspect(root: File, step: StabilizationRestoreWriteStep,
        originals: List<V290SpacePhotoEntity> = listOf(old),
        target: List<V290SpacePhotoEntity> = listOf(old, fresh),
    ) = B04PrivatePhotoForensicPolicy.inspect(root, originals, target, step)

    @Test fun untouchedBaselineFilesAreNotWrittenByForensics() = sandbox { root ->
        save(root, old, first)
        val r = inspect(root, StabilizationRestoreWriteStep.PREPARED)
        assertEquals(1, r.originalVerified)
        assertEquals(1, r.incomingMissing)
        assertEquals(0, r.untrackedEntries)
        assertFalse(r.demandsOwnerReview)
        assertFalse(r.mayDeletePendingFiles)
        assertFalse(r.grantsMutationAuthority)
    }
    @Test fun missingBaselineIsAlwaysBlocked() = sandbox { root ->
        assertEquals(1, inspect(root, StabilizationRestoreWriteStep.PREPARED)
            .originalMissingOrCorrupt)
    }
    @Test fun corruptBaselineCannotBeReplacedWithoutReview() = sandbox { root ->
        save(root, old, second)
        assertTrue(inspect(root, StabilizationRestoreWriteStep.PREPARED).demandsOwnerReview)
    }
    @Test fun completeTargetBytesMayBeConfirmedAfterPhotoCheckpoint() = sandbox { root ->
        save(root, old, first);save(root, fresh, second)
        val r = inspect(root, StabilizationRestoreWriteStep.PHOTOS_WRITTEN)
        assertEquals(1, r.incomingVerified)
        assertTrue(r.allTargetBytesPresentAndVerified)
        assertFalse(r.mayAutoResume)
    }
    @Test fun missingIncomingAfterPhotosWrittenRequiresReview() = sandbox { root ->
        save(root, old, first)
        val r = inspect(root, StabilizationRestoreWriteStep.PHOTOS_WRITTEN)
        assertTrue(r.demandsOwnerReview)
        assertFalse(r.allTargetBytesPresentAndVerified)
    }
    @Test fun earlyImportedBytesCannotBeAcceptedAsBaseline() = sandbox { root ->
        save(root, old, first);save(root, fresh, second)
        val r = inspect(root, StabilizationRestoreWriteStep.PERSONAL_WRITTEN)
        assertEquals(1, r.prematureIncomingFiles)
        assertTrue(r.demandsOwnerReview)
    }
    @Test fun incompleteCopyKeepsPendingFileForOwnerReview() = sandbox { root ->
        save(root, old, first)
        val staged = File(File(root, space), ".$newId.restore.pending")
        staged.writeBytes(second)
        val r = inspect(root, StabilizationRestoreWriteStep.BEFORE_PHOTOS)
        assertEquals(1, r.interruptedPendingFiles)
        assertTrue(staged.exists())
        assertTrue(r.demandsOwnerReview)
    }
    @Test fun pendingFileStillBlocksAfterCompletePhoto() = sandbox { root ->
        save(root, old, first);save(root, fresh, second)
        File(File(root, space), ".$newId.restore.pending").writeBytes(second)
        val r = inspect(root, StabilizationRestoreWriteStep.VERIFYING)
        assertFalse(r.allTargetBytesPresentAndVerified)
        assertTrue(r.demandsOwnerReview)
    }
    @Test fun corruptedNewJpegCannotMasqueradeAsTarget() = sandbox { root ->
        save(root, old, first);save(root, fresh, first)
        val r = inspect(root, StabilizationRestoreWriteStep.BEFORE_PHOTOS)
        assertEquals(1, r.incomingCorrupt)
        assertTrue(r.demandsOwnerReview)
    }
    @Test fun untrackedOwnerFileIsLeftUntouched() = sandbox { root ->
        save(root, old, first)
        val unknown = File(File(root, space), "extra.jpg")
        unknown.writeBytes(second)
        assertEquals(1, inspect(root, StabilizationRestoreWriteStep.BEFORE_PHOTOS).untrackedEntries)
        assertArrayEquals(second, unknown.readBytes())
    }
    @Test fun symbolicLinkPhotoIsNotFollowed() = sandbox { root ->
        save(root, old, first)
        val other = File(root.parentFile, "elsewhere.jpg")
        other.writeBytes(second)
        val link = File(File(root, space), fresh.fileName)
        Files.createSymbolicLink(link.toPath(), other.toPath())
        val r = inspect(root, StabilizationRestoreWriteStep.BEFORE_PHOTOS)
        assertEquals(1, r.untrackedEntries)
        assertEquals(1, r.incomingMissing)
        assertArrayEquals(second, other.readBytes())
    }
    @Test fun symbolicLinkRootIsRejected() = sandbox { root ->
        val other = File(root.parentFile, "other")
        check(other.mkdir())
        Files.createSymbolicLink(root.toPath(), other.toPath())
        assertThrows(IllegalStateException::class.java) {
            inspect(root, StabilizationRestoreWriteStep.PREPARED)
        }
    }
    @Test fun targetCannotChangeOriginalOwnerPhoto() = sandbox { root ->
        assertThrows(IllegalArgumentException::class.java) {
            inspect(root, StabilizationRestoreWriteStep.PREPARED,
                listOf(old), listOf(old.copy(note = "changed"), fresh))
        }
    }
    @Test fun duplicatePhotoIdentityIsRejected() = sandbox { root ->
        assertThrows(IllegalArgumentException::class.java) {
            inspect(root, StabilizationRestoreWriteStep.PREPARED,
                listOf(old, old), listOf(old, fresh))
        }
    }
    @Test fun snapshotWithNoPhotosDoesNotWriteFolders() = sandbox { root ->
        val r = inspect(root, StabilizationRestoreWriteStep.VERIFYING,
            emptyList(), emptyList())
        assertTrue(r.allTargetBytesPresentAndVerified)
        assertFalse(root.exists())
    }
}
