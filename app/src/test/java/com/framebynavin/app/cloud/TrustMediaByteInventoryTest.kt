package com.framebynavin.app.cloud

import java.io.File
import java.security.MessageDigest
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Pure JVM tests; actual user file contents and cloud accounts are never used. */
class TrustMediaByteInventoryTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun sha(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun verify(
        root: File,
        sources: List<TrustMediaSource>,
        exact: Boolean = true,
    ): TrustDomainEvidence = TrustMediaByteInventory.verify(
        domain = TrustBackupDomain.PERSONAL_PHOTO_BYTES,
        root = root,
        sources = sources,
        maxEachBytes = 1024L,
        maxTotalBytes = 4096L,
        requireExactFileInventory = exact,
    )

    @Test fun originalBytesNotJustPhotoRowCountAreHashed() {
        val root = temporary.newFolder("photos")
        val dir = File(root, "space-1").apply { mkdirs() }
        val content = "private-photo-binary-1".toByteArray()
        File(dir, "photo.jpg").writeBytes(content)
        val source = TrustMediaSource("space-1/photo.jpg", sha(content))
        val original = verify(root, listOf(source))
        assertEquals(1, original.records)
        assertEquals(content.size, original.bytes)
        assertEquals(64, original.sha256.length)
        assertFalse(original.toString().contains("space-1"))
        // Changing just the original file bytes changes the source checksum.
        File(dir, "photo.jpg").writeBytes("private-photo-binary-2".toByteArray())
        assertThrows(IllegalArgumentException::class.java) {
            verify(root, listOf(source))
        }
        val fresh = "private-photo-binary-2".toByteArray()
        val updated = verify(root, listOf(source.copy(expectedSha256 = sha(fresh))))
        assertNotEquals(original.sha256, updated.sha256)
    }

    @Test fun missingFileAndUntrackedRecoveryArtifactBlockCompletePhotoInventory() {
        val root = temporary.newFolder("missing-original")
        val missing = TrustMediaSource("space-2/lost.jpg", sha("missing".toByteArray()))
        assertThrows(IllegalArgumentException::class.java) {
            verify(root, listOf(missing))
        }
        File(root, "orphan.restore.pending").writeText("owner bytes at risk")
        assertThrows(IllegalArgumentException::class.java) {
            verify(root, emptyList())
        }
    }

    @Test fun emptyPrivateDirectoryIsValidZeroButNotAUniversalBackup() {
        val root = File(temporary.root, "not-present")
        val proof = verify(root, emptyList())
        assertEquals(0, proof.records)
        assertEquals(0, proof.bytes)
        val inventory = TrustBackupInventoryPolicy.assemble(listOf(proof))
        assertTrue(TrustBackupDomain.HOME_SLIDESHOW_MEDIA in inventory.pendingDomains)
        assertFalse(inventory.canClaimCompleteBackup)
        assertFalse(inventory.canUploadAsUniversalBackup)
    }

    @Test fun traversalAndDuplicateNamesAreRejected() {
        val root = temporary.newFolder("safe-root")
        val dummy = sha("file".toByteArray())
        for (name in listOf("../outside", "one/../two", "/absolute", "one//two", "one\\file")) {
            assertThrows(IllegalArgumentException::class.java) {
                verify(root, listOf(TrustMediaSource(name, dummy)))
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            verify(root, listOf(
                TrustMediaSource("one.jpg", dummy),
                TrustMediaSource("one.jpg", dummy),
            ))
        }
    }

    @Test fun fileSymlinkCannotSmuggleAnotherFileIntoPrivateBackup() {
        val root = temporary.newFolder("no-symlinks")
        val real = File(temporary.root, "outside-secret.jpg").apply { writeText("outside") }
        Files.createSymbolicLink(File(root, "stolen.jpg").toPath(), real.toPath())
        assertThrows(IllegalArgumentException::class.java) {
            verify(root, listOf(
                TrustMediaSource("stolen.jpg", sha(real.readBytes())),
            ))
        }
    }

    @Test fun activeCreatorFrameCanBeProvenButNotBroadCreatorRecordings() {
        val root = temporary.newFolder("best_frames_original")
        val bytes = "best frame".toByteArray()
        File(root, "current.img").writeBytes(bytes)
        File(root, "legacy_recovery.img").writeText("older recovered frame")
        val current = TrustMediaByteInventory.verify(
            domain = TrustBackupDomain.CREATOR_HERO_ORIGINAL_BYTES,
            root = root,
            sources = listOf(TrustMediaSource("current.img", sha(bytes))),
            maxEachBytes = 1024L,
            maxTotalBytes = 4096L,
            requireExactFileInventory = false,
        )
        assertEquals(1, current.records)
        val inventory = TrustBackupInventoryPolicy.assemble(listOf(current))
        assertTrue(TrustBackupDomain.CREATOR_MEDIA in inventory.pendingDomains)
        assertTrue(TrustBackupDomain.HOME_SLIDESHOW_MEDIA in inventory.pendingDomains)
    }

    @Test fun sizeAndIdentityTamperingCannotClaimVerifiedBytes() {
        val root = temporary.newFolder("size-limits")
        val big = ByteArray(1500) { it.toByte() }
        File(root, "oversize.jpg").writeBytes(big)
        assertThrows(IllegalArgumentException::class.java) {
            verify(root, listOf(TrustMediaSource("oversize.jpg", sha(big))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            TrustBackupInventoryPolicy.verifiedByteEvidence(
                TrustBackupDomain.ANDROID_PERMISSIONS, 1, 5L, sha(big),
            )
        }
    }
}
