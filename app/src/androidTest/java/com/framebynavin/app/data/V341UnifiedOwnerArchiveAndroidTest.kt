package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Cryptographic codec tests with synthetic records and no real owner content. */
@RunWith(AndroidJUnit4::class)
class V341UnifiedOwnerArchiveAndroidTest {
    private val cache get() = InstrumentationRegistry.getInstrumentation()
        .targetContext.cacheDir
    private val password = "strong-owner-passphrase".toCharArray()

    private fun archive(
        creator: ByteArray,
        personal: ByteArray,
    ): ByteArray {
        val dir = java.nio.file.Files.createTempDirectory(cache.toPath(),
            "v341-test-export-").toFile()
        try {
            val creatorFile = File(dir, "c.zip").apply { writeBytes(creator) }
            val personalFile = File(dir, "p.zip").apply { writeBytes(personal) }
            return ByteArrayOutputStream().use { output ->
                V341UnifiedOwnerArchive.write(
                    creatorFile, personalFile, password, output)
                output.toByteArray()
            }
        } finally { dir.deleteRecursively() }
    }

    @Test fun bothCanonicalPayloadsSurviveIndependentEncryptionAndOneMiBChunkBoundary() {
        val creator = ByteArray(1024 * 1024 + 17) { i -> (i % 251).toByte() }
        val personal = ByteArray(1024 * 1024 + 3) { i -> (i % 193).toByte() }
        val bytes = archive(creator, personal)
        V341UnifiedOwnerArchive.stageAndVerify(
            ByteArrayInputStream(bytes), password, cache,
        ).use { staged ->
            assertArrayEquals(creator, staged.creator.readBytes())
            assertArrayEquals(personal, staged.personal.readBytes())
            assertEquals(creator.size.toLong(), staged.creatorBytes)
            assertEquals(personal.size.toLong(), staged.personalBytes)
        }
    }

    @Test fun wrongPassphraseNeverProvidesStagedOwnerFiles() {
        val bytes = archive("creator data".toByteArray(), "personal data".toByteArray())
        assertThrows(Exception::class.java) {
            V341UnifiedOwnerArchive.stageAndVerify(
                ByteArrayInputStream(bytes), "wrong-password-1234".toCharArray(), cache)
        }
    }

    @Test fun truncatedEncryptedArchiveFailsClosed() {
        val bytes = archive("creator bytes".toByteArray(), "private journal".toByteArray())
        // Cut into the authenticated body, not merely ZIP's optional central
        // directory, which ZipInputStream deliberately does not consume.
        val truncated = bytes.copyOf(bytes.size / 2)
        assertThrows(Exception::class.java) {
            V341UnifiedOwnerArchive.stageAndVerify(
                ByteArrayInputStream(truncated), password, cache)
        }
    }

    @Test fun noInjectedOrUnexpectedZipEntryIsAccepted() {
        val bytes = archive("creator".toByteArray(), "personal".toByteArray())
        val altered = ByteArrayOutputStream()
        ZipInputStream(ByteArrayInputStream(bytes)).use { input ->
            ZipOutputStream(altered).use { output ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    output.putNextEntry(ZipEntry(entry.name))
                    input.copyTo(output)
                    output.closeEntry()
                    input.closeEntry()
                }
                output.putNextEntry(ZipEntry("unexpected-extra.dat"))
                output.write(byteArrayOf(1, 2, 3))
                output.closeEntry()
            }
        }
        assertThrows(Exception::class.java) {
            V341UnifiedOwnerArchive.stageAndVerify(
                ByteArrayInputStream(altered.toByteArray()), password, cache)
        }
    }

    @Test fun singleCiphertextByteChangeIsDetectedBeforeReturning() {
        val bytes = archive("creator".toByteArray(), "personal".toByteArray())
        val modified = ByteArrayOutputStream()
        ZipInputStream(ByteArrayInputStream(bytes)).use { input ->
            ZipOutputStream(modified).use { output ->
                while (true) {
                    val e = input.nextEntry ?: break
                    val data = input.readBytes()
                    if (e.name == "personal.enc") {
                        assertTrue(data.isNotEmpty())
                        data[0] = (data[0].toInt() xor 1).toByte()
                    }
                    output.putNextEntry(ZipEntry(e.name))
                    output.write(data)
                    output.closeEntry()
                    input.closeEntry()
                }
            }
        }
        assertThrows(Exception::class.java) {
            V341UnifiedOwnerArchive.stageAndVerify(
                ByteArrayInputStream(modified.toByteArray()), password, cache)
        }
    }
}
