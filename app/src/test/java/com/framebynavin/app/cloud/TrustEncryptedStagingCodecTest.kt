package com.framebynavin.app.cloud

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** No Android/emulator/cloud account needed. Never touches user-owned data. */
class TrustEncryptedStagingCodecTest {
    @get:Rule val folder = TemporaryFolder()
    private val ownerId = "owner-accounts/ci-fixture-not-real"
    private val passphrase = "unique-private-CI-passphrase-only".toCharArray()

    private fun sha(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun source(id: String, bytes: ByteArray, records: Int = 1) =
        TrustStagingSource(id, records, bytes.size.toLong(), sha(bytes)) {
            ByteArrayInputStream(bytes)
        }

    private fun archive(vararg pieces: TrustStagingSource): ByteArray {
        val output = ByteArrayOutputStream()
        TrustEncryptedStagingCodec.write(ownerId, passphrase, pieces.toList(), output)
        return output.toByteArray()
    }

    private fun verify(bytes: ByteArray, owner: String = ownerId,
        password: CharArray = passphrase): TrustVerifiedStagedBundle =
        TrustEncryptedStagingCodec.stageAndVerify(
            owner, password, ByteArrayInputStream(bytes), folder.root,
        )

    @Test fun encryptedMixedTeluguAndBinaryOriginalsRoundTripToDisposableStaging() {
        val notes = "తెలుగు కథ • mixed EN notes".toByteArray(Charsets.UTF_8)
        val photo = ByteArray(200_123) { (it % 251).toByte() }
        val bytes = archive(
            source("creator/ideas", notes, 2),
            source("personal/original-jpeg", photo, 1),
        )
        val plaintext = bytes.toString(Charsets.ISO_8859_1)
        assertFalse(plaintext.contains(ownerId))
        assertFalse(plaintext.contains("creator/ideas"))
        assertFalse(plaintext.contains("తెలుగు కథ"))

        val staging = verify(bytes)
        val saved = staging.parts.associateBy { it.id }
        val file = requireNotNull(saved["creator/ideas"]?.file)
        assertArrayEquals(notes, file.readBytes())
        assertArrayEquals(photo,
            requireNotNull(saved["personal/original-jpeg"]?.file).readBytes())
        assertEquals(2, saved["creator/ideas"]?.records)
        assertFalse(staging.canClaimCompleteBackup)
        assertFalse(staging.canUploadAsUniversalBackup)
        staging.close()
        assertFalse(file.exists())
    }

    @Test fun wrongPassphraseAndWrongOwnerAreBothRejectedWithoutSavingRecoveredBytes() {
        val bytes = archive(source("creator/tasks", "private script".toByteArray()))
        assertThrows(Exception::class.java) {
            verify(bytes, password = "a-different-strong-passphrase".toCharArray())
        }
        assertThrows(IllegalArgumentException::class.java) {
            verify(bytes, owner = "another-signed-in-account")
        }
        assertTrue(folder.root.listFiles().orEmpty().none { it.name.startsWith("backlot-trust-check-") })
    }

    @Test fun alteredEncryptedChunkFailsAuthenticatedDecryption() {
        val bytes = archive(source("creator/tasks", "private text".toByteArray()))
        val tampered = repack(bytes) { name, data ->
            if (name == "part-0001.bin") {
                data.copyOf().also { it[20] = (it[20].toInt() xor 0x40).toByte() }
            } else data
        }
        assertThrows(Exception::class.java) { verify(tampered) }
        assertTrue(folder.root.listFiles().orEmpty().none { it.name.startsWith("backlot-trust-check-") })
    }

    @Test fun missingOrExtraZipEntryAndTruncatedFileFailClosed() {
        val bytes = archive(source("personal/reminder-history", "[]".toByteArray()))
        val missing = repack(bytes) { name, data ->
            if (name == "part-0001.bin") null else data
        }
        assertThrows(Exception::class.java) { verify(missing) }
        val extra = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                ZipInputStream(ByteArrayInputStream(bytes)).use { input ->
                    while (true) {
                        val entry = input.nextEntry ?: break
                        zip.putNextEntry(ZipEntry(entry.name))
                        zip.write(input.readBytes())
                        zip.closeEntry()
                        input.closeEntry()
                    }
                }
                zip.putNextEntry(ZipEntry("part-9999.bin"))
                zip.write(byteArrayOf(1, 2, 3))
                zip.closeEntry()
            }
        }.toByteArray()
        assertThrows(Exception::class.java) { verify(extra) }
        assertThrows(Exception::class.java) {
            verify(bytes.copyOf(bytes.size / 2))
        }
    }

    @Test fun sourceChangingAfterInventoryRejectsIncompleteStaging() {
        val actual = "changed after inventory".toByteArray()
        val oldSha = sha("original".toByteArray())
        val source = TrustStagingSource(
            "creator/ideas", 1, actual.size.toLong(), oldSha,
        ) { ByteArrayInputStream(actual) }
        assertThrows(IllegalArgumentException::class.java) {
            archive(source)
        }
        val short = source.copy(expectedSha256 = sha(actual),
            expectedBytes = actual.size.toLong() + 1)
        assertThrows(IllegalArgumentException::class.java) {
            archive(short)
        }
    }

    @Test fun neverAcceptDuplicateOrUnsafeIdentifiersOrMissingPassphrase() {
        val a = source("creator/tasks", "one".toByteArray())
        assertThrows(IllegalArgumentException::class.java) { archive(a, a) }
        assertThrows(IllegalArgumentException::class.java) {
            archive(source("../../secrets", "x".toByteArray()))
        }
        assertThrows(IllegalArgumentException::class.java) {
            archive(source("creator/../tasks", "x".toByteArray()))
        }
        assertThrows(IllegalArgumentException::class.java) {
            TrustEncryptedStagingCodec.write(
                ownerId, "short".toCharArray(), listOf(a), ByteArrayOutputStream(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            TrustEncryptedStagingCodec.write(
                "", passphrase, listOf(a), ByteArrayOutputStream(),
            )
        }
    }

    @Test fun sameSourceUsesRandomFreshSaltAndNonceEachTime() {
        val content = "same owner information".toByteArray()
        val one = archive(source("creator/ideas", content))
        val two = archive(source("creator/ideas", content))
        assertFalse(one.contentEquals(two))
        verify(one).use { first ->
            verify(two).use { second ->
                assertEquals(first.parts.single().sha256, second.parts.single().sha256)
                assertNotEquals(first.parts.single().file.path, second.parts.single().file.path)
            }
        }
    }

    @Test fun emptyOriginalIsValidButStillNotAUniversalBackup() {
        val bytes = archive(source("creator/empty", byteArrayOf(), 0))
        verify(bytes).use {
            assertEquals(0L, it.parts.single().sizeBytes)
            assertEquals(sha(byteArrayOf()), it.parts.single().sha256)
            assertFalse(it.canUploadAsUniversalBackup)
        }
    }

    private fun repack(bytes: ByteArray,
        mutation: (String, ByteArray) -> ByteArray?): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            ZipInputStream(ByteArrayInputStream(bytes)).use { input ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    val original = input.readBytes()
                    val next = mutation(entry.name, original)
                    if (next != null) {
                        zip.putNextEntry(ZipEntry(entry.name))
                        zip.write(next)
                        zip.closeEntry()
                    }
                    input.closeEntry()
                }
            }
        }
        return output.toByteArray()
    }
}
