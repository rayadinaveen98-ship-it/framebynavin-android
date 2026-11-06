package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

/** Synthetic temp files only: never run against active app directories. */
class StabilizationVerifiedPhotoInstallerTest {
    private val id = "2e01c0b5-ec32-4f4c-8ea9-5ca7b4100cd1"
    private val space = "81ed09c3-5713-4bca-a40c-fbb909a55f06"
    private val bytes = ByteArray(2200) { ((it * 17 + 23) % 251).toByte() }
    private fun sha(data: ByteArray) = MessageDigest.getInstance("SHA-256")
        .digest(data).joinToString("") { "%02x".format(it) }
    private fun row() = V290SpacePhotoEntity(
        id, space, 100L, 110L, "$id.jpg", sha(bytes), 24, 24, "",
    )
    private fun withFiles(run: (File, File, File, V290SpacePhotoEntity) -> Unit) {
        val sandbox = Files.createTempDirectory("backlot-photo-qa-").toFile()
        try {
            val stage = File(sandbox, "authenticated-stage.jpg").apply { writeBytes(bytes) }
            run(sandbox, File(sandbox, "private-photos"), stage, row())
        } finally { sandbox.deleteRecursively() }
    }
    private fun destination(root: File) = File(File(root, space), "$id.jpg")
    private fun install(root: File, stage: File, photo: V290SpacePhotoEntity) =
        StabilizationVerifiedPhotoInstaller(root).installVerified(photo, stage, bytes.size.toLong())

    @Test fun verifiedBytesInstallWithoutReplacingAnyone() = withFiles { _, root, stage, photo ->
        assertEquals(StabilizationPhotoInstallResult.INSTALLED, install(root, stage, photo))
        assertArrayEquals(bytes, destination(root).readBytes())
        assertFalse(File(destination(root).parentFile, ".$id.restore.pending").exists())
    }
    @Test fun replayWithIdenticalBytesIsSafelyIdempotent() = withFiles { _, root, stage, photo ->
        install(root, stage, photo)
        assertEquals(StabilizationPhotoInstallResult.ALREADY_VERIFIED, install(root, stage, photo))
        assertArrayEquals(bytes, destination(root).readBytes())
    }
    @Test fun differentExistingPhotoIsNeverReplaced() = withFiles { _, root, stage, photo ->
        val target = destination(root)
        target.parentFile.mkdirs(); target.writeBytes(byteArrayOf(5, 4, 3))
        assertThrows(IllegalStateException::class.java) { install(root, stage, photo) }
        assertArrayEquals(byteArrayOf(5, 4, 3), target.readBytes())
    }
    @Test fun corruptedSourceSizeIsRejectedBeforeCreatingDestination() = withFiles { _, root, stage, photo ->
        stage.writeBytes(byteArrayOf(1, 2))
        assertThrows(IllegalArgumentException::class.java) { install(root, stage, photo) }
        assertFalse(root.exists())
    }
    @Test fun corruptedSourceChecksumIsRejectedBeforeCreatingDestination() = withFiles { _, root, stage, photo ->
        stage.writeBytes(ByteArray(bytes.size) { 0 })
        assertThrows(IllegalArgumentException::class.java) { install(root, stage, photo) }
        assertFalse(root.exists())
    }
    @Test fun photoFilenameMustMatchCanonicalIdentity() = withFiles { _, root, stage, photo ->
        assertThrows(IllegalArgumentException::class.java) {
            install(root, stage, photo.copy(fileName = "11111111-1111-4111-8111-111111111111.jpg"))
        }
        assertFalse(root.exists())
    }
    @Test fun unfinishedPendingPhotoBlocksInstallAndIsPreserved() = withFiles { _, root, stage, photo ->
        val final = destination(root)
        final.parentFile.mkdirs()
        val partial = File(final.parentFile, ".$id.restore.pending")
        partial.writeText("unfinished")
        assertThrows(IllegalArgumentException::class.java) { install(root, stage, photo) }
        assertTrue(partial.exists()); assertFalse(final.exists())
    }
    @Test fun unrelatedOwnerPhotoIsNotDeleted() = withFiles { _, root, stage, photo ->
        val unrelated = File(File(root, space), "other.jpg")
        unrelated.parentFile.mkdirs(); unrelated.writeText("owner")
        install(root, stage, photo)
        assertEquals("owner", unrelated.readText())
    }
    @Test fun destinationSymlinkCannotOverwriteOutsideFile() = withFiles { sandbox, root, stage, photo ->
        val outside = File(sandbox, "owner.txt").apply { writeText("original") }
        val final = destination(root)
        final.parentFile.mkdirs()
        Files.createSymbolicLink(final.toPath(), outside.toPath())
        assertThrows(IllegalStateException::class.java) { install(root, stage, photo) }
        assertEquals("original", outside.readText())
    }
    @Test fun sourceSymlinkIsNotTrusted() = withFiles { sandbox, root, stage, photo ->
        val alias = File(sandbox, "alias.jpg")
        Files.createSymbolicLink(alias.toPath(), stage.toPath())
        assertThrows(IllegalArgumentException::class.java) { install(root, alias, photo) }
        assertFalse(root.exists())
    }
    @Test fun stageInsideLivePhotoRootIsRejected() = withFiles { _, root, _, photo ->
        root.mkdirs()
        val stage = File(root, "stage.jpg").apply { writeBytes(bytes) }
        assertThrows(IllegalArgumentException::class.java) { install(root, stage, photo) }
        assertFalse(destination(root).exists())
    }
    @Test fun zeroByteExpectationIsRejected() = withFiles { _, root, stage, photo ->
        assertThrows(IllegalArgumentException::class.java) {
            StabilizationVerifiedPhotoInstaller(root).installVerified(photo, stage, 0L)
        }
        assertFalse(root.exists())
    }
}
