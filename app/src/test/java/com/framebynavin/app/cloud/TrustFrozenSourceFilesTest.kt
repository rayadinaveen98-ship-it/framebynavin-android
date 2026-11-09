package com.framebynavin.app.cloud

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Source-only JVM tests: never read the user's app database or cloud. */
class TrustFrozenSourceFilesTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun prepared(): Pair<File, File> =
        tmp.newFolder("private-" + System.nanoTime()) to
            tmp.newFolder("staging-" + System.nanoTime())

    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    @Test fun frozenJsonAndOriginalMediaRemainImmutableWhenSourceChanges() {
        val (source, staging) = prepared()
        val original = File(source, "owner/photo.jpg")
        original.parentFile!!.mkdirs()
        val originalBytes = ByteArray(100_023) { (it % 239).toByte() }
        original.writeBytes(originalBytes)
        val freezer = TrustSourceFreezer(source, staging)
        freezer.addJson("personal/receipts",
            """{"date":"2026-11-09","note":"తెలుగు"}""", 1)
        freezer.addOwnedFile("personal/photo-0000", original, 1, sha(originalBytes))
        val bundle = freezer.sealPartial()
        val parts = bundle.parts.associateBy { it.id }
        assertEquals(2, parts.size)
        assertFalse(bundle.canClaimCompleteBackup)
        assertFalse(bundle.canUploadAsUniversalBackup)
        assertEquals(64, parts["personal/photo-0000"]?.sha256?.length)
        original.writeText("replaced on live source")
        assertArrayEquals(originalBytes, parts["personal/photo-0000"]!!.file.readBytes())
        val streamBytes = parts["personal/photo-0000"]!!.source().open().use { it.readBytes() }
        assertArrayEquals(originalBytes, streamBytes)
        val frozenFile = parts["personal/photo-0000"]!!.file
        bundle.close()
        assertFalse(frozenFile.exists())
    }

    @Test fun incompleteFreezeNeverSucceedsAndCleansCopiedPlaintext() {
        val (source, staging) = prepared()
        val freezer = TrustSourceFreezer(source, staging)
        freezer.addJson("creator/projects", "[]", 0)
        assertThrows(IllegalStateException::class.java) {
            freezer.addStream("personal/photo-0000", 1,
                ByteArrayInputStream("tampered".toByteArray()), sha("original".toByteArray()))
        }
        freezer.close()
        assertTrue(staging.walkTopDown().count { it.isFile } == 0)
    }

    @Test fun missingExternalOrSymlinkedSourceIsRejectedWithoutCopy() {
        val (source, staging) = prepared()
        val original = File(source, "owned.jpg").apply { writeText("pixels") }
        val outside = File(tmp.root, "external.jpg").apply { writeText("private") }
        val freezer = TrustSourceFreezer(source, staging)
        assertThrows(IllegalArgumentException::class.java) {
            freezer.addOwnedFile("personal/outside", outside, 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            freezer.addOwnedFile("personal/traversal", File(source, "../external.jpg"), 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            freezer.addOwnedFile("personal/missing", File(source, "missing.jpg"), 1)
        }
        val linked = File(source, "linked.jpg")
        Files.createSymbolicLink(linked.toPath(), original.toPath())
        assertThrows(IllegalArgumentException::class.java) {
            freezer.addOwnedFile("personal/symlink", linked, 1)
        }
        freezer.close()
        assertFalse(outside.readText().isBlank())
    }

    @Test fun unsafeDuplicateAndUnboundedPartsNeverCreateValidSnapshot() {
        val (source, staging) = prepared()
        val freezer = TrustSourceFreezer(source, staging)
        assertThrows(IllegalArgumentException::class.java) {
            freezer.addJson("../creds", "hello", 1)
        }
        freezer.addJson("creator/ideas", "[]", 0)
        assertThrows(IllegalArgumentException::class.java) {
            freezer.addJson("creator/ideas", "[]", 0)
        }
        val zero = freezer.sealPartial()
        assertEquals(1, zero.parts.size)
        assertEquals(0, zero.parts.single().records)
        zero.close()
        assertThrows(IllegalStateException::class.java) {
            freezer.addJson("creator/another", "[]", 0)
        }
    }

    @Test fun failedSourceReadLeavesNoVerifiedFrozenFile() {
        val (source, staging) = prepared()
        val freezer = TrustSourceFreezer(source, staging)
        val broken = object : ByteArrayInputStream(byteArrayOf(1, 2, 3)) {
            override fun read(b: ByteArray, off: Int, len: Int): Int =
                throw IOException("source is unreadable")
        }
        assertThrows(IOException::class.java) {
            freezer.addStream("creator/broken", 1, broken)
        }
        freezer.close()
        assertFalse(staging.walkTopDown().any { it.isFile })
    }
}
