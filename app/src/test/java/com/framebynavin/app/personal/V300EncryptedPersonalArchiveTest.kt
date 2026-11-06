package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289PersonalSpace
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class V300EncryptedPersonalArchiveTest {
    private val passphrase = "my-long-private-passphrase".toCharArray()
    private val spaceId = "e2e58173-2d17-4fd9-b865-ebc3b8849627"
    private val photoId = "fb509361-2e95-4cc2-b3e0-72f7ac250da1"
    private val photoBytes = ByteArray(48_000) { (it * 7 % 253).toByte() }

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun components(): V300PersonalComponents {
        val personal = PersonalReminder(
            id = "private-archived-reminder",
            title = "My private routine",
            notes = "Do not publish this note",
            startDate = LocalDate.of(2026, 12, 7),
            time = LocalTime.of(8, 30),
            createdAtMillis = 1_000L,
            updatedAtMillis = 1_000L,
        )
        val space = V289PersonalSpace(
            id = spaceId, title = "My space", type = V289SpaceType.CUSTOM,
            createdAtMillis = 1000L,
        )
        return V300PersonalComponents(
            personal = PersonalCompletionBackupCodec.encode(listOf(personal), emptyList()),
            challenges = V297ChallengeCodec.encode(emptyList()),
            groups = V298ReminderGroupCodec.encode(emptyList()),
            spaces = V289SpacesMetadataCodec.encode(V289SpaceState(spaces = listOf(space))),
        )
    }

    private fun photoSource(bytes: ByteArray = photoBytes): V300PhotoSource {
        val photo = V290SpacePhotoEntity(
            id = photoId, spaceId = spaceId, takenAtMillis = 1000L,
            addedAtMillis = 1100L, fileName = "$photoId.jpg",
            sha256Hex = sha256(photoBytes), widthPx = 120, heightPx = 140,
            note = "Confidential photo journal",
        )
        return V300PhotoSource(photo, bytes.size.toLong()) {
            ByteArrayInputStream(bytes)
        }
    }

    private fun createArchive(includePhoto: Boolean = true): ByteArray {
        val out = ByteArrayOutputStream()
        V300EncryptedPersonalArchive.write(
            components(), if (includePhoto) listOf(photoSource()) else emptyList(),
            passphrase, out,
        )
        return out.toByteArray()
    }

    private fun stage(bytes: ByteArray, password: CharArray = passphrase): V300StagedPersonalArchive {
        val root = File(System.getProperty("java.io.tmpdir"), "v300-qa-${System.nanoTime()}")
        check(root.mkdirs())
        return try {
            V300EncryptedPersonalArchive.stageAndVerify(
                ByteArrayInputStream(bytes), password, root,
            )
        } finally {
            // The staged output is a subfolder; the test root cleanup happens in individual tests.
            root.deleteOnExit()
        }
    }

    @Test fun allPersonalDomainsAndPhotoBytesSurviveEncryptedRoundTrip() {
        val bytes = createArchive()
        assertFalse(bytes.toString(Charsets.ISO_8859_1).contains("Confidential photo journal"))
        assertFalse(bytes.toString(Charsets.ISO_8859_1).contains("Do not publish this note"))
        val root = File(System.getProperty("java.io.tmpdir"), "v300-check-${System.nanoTime()}")
        check(root.mkdirs())
        try {
            val verified = V300EncryptedPersonalArchive.stageAndVerify(
                ByteArrayInputStream(bytes), passphrase, root,
            )
            verified.use {
                assertEquals(components(), it.components)
                assertEquals(1, it.photos.size)
                assertEquals(photoId, it.photos.single().photo.id)
                assertArrayEquals(photoBytes, it.photos.single().stagedFile.readBytes())
            }
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun wrongPasswordNeverProducesAnyRestorableData() {
        val root = File(System.getProperty("java.io.tmpdir"), "v300-password-${System.nanoTime()}")
        check(root.mkdirs())
        try {
            assertThrows(Exception::class.java) {
                V300EncryptedPersonalArchive.stageAndVerify(
                    ByteArrayInputStream(createArchive()),
                    "incorrect-passphrase-xxx".toCharArray(), root,
                )
            }
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally { root.deleteRecursively() }
    }

    @Test fun missingRequiredPhotoFailsBeforeAnyPersistentWrite() {
        val original = createArchive()
        val truncated = rewriteZip(original) { name, bytes ->
            if (name.endsWith("part-00001.bin")) null else bytes
        }
        val root = File(System.getProperty("java.io.tmpdir"), "v300-truncated-${System.nanoTime()}")
        check(root.mkdirs())
        try {
            assertThrows(Exception::class.java) {
                V300EncryptedPersonalArchive.stageAndVerify(
                    ByteArrayInputStream(truncated), passphrase, root,
                )
            }
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally { root.deleteRecursively() }
    }

    @Test fun changedEncryptedPhotoFailsAuthenticationAndCleansStage() {
        val changed = rewriteZip(createArchive()) { name, bytes ->
            if (name.endsWith("part-00001.bin")) bytes.clone().also {
                it[it.lastIndex] = (it.last().toInt() xor 0x01).toByte()
            } else bytes
        }
        val root = File(System.getProperty("java.io.tmpdir"), "v300-tampered-${System.nanoTime()}")
        check(root.mkdirs())
        try {
            assertThrows(Exception::class.java) {
                V300EncryptedPersonalArchive.stageAndVerify(
                    ByteArrayInputStream(changed), passphrase, root,
                )
            }
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally { root.deleteRecursively() }
    }

    @Test fun refusingPhotoWhoseBytesChangedDuringExport() {
        val out = ByteArrayOutputStream()
        assertThrows(IllegalArgumentException::class.java) {
            V300EncryptedPersonalArchive.write(
                components(),
                listOf(photoSource(bytes = "Different photo".toByteArray())),
                passphrase, out,
            )
        }
    }

    @Test fun emptyPhotoCollectionIsValidAndNoFakeHistoryIsIntroduced() {
        val root = File(System.getProperty("java.io.tmpdir"), "v300-no-photos-${System.nanoTime()}")
        check(root.mkdirs())
        try {
            val verified = V300EncryptedPersonalArchive.stageAndVerify(
                ByteArrayInputStream(createArchive(includePhoto = false)),
                passphrase, root,
            )
            verified.use {
                assertTrue(it.photos.isEmpty())
                val decoded = PersonalCompletionBackupCodec.decode(it.components.personal)
                assertTrue(decoded.completions.isEmpty())
                assertEquals(1, decoded.reminders.size)
            }
        } finally { root.deleteRecursively() }
    }

    @Test fun tooShortPassphraseCannotCreateEncryptedArchive() {
        assertThrows(IllegalArgumentException::class.java) {
            V300EncryptedPersonalArchive.write(
                components(), emptyList(), "short".toCharArray(), ByteArrayOutputStream(),
            )
        }
    }

    @Test fun anOrphanGroupLinkCannotBeAdvertisedAsRecoverable() {
        val unlinked = V298ReminderGroup(
            id = "group-with-dead-link", name = "My group",
            reminderIds = listOf("deleted-reminder"),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        val bad = components().copy(
            groups = V298ReminderGroupCodec.encode(listOf(unlinked)),
        )
        assertThrows(IllegalArgumentException::class.java) {
            V300EncryptedPersonalArchive.write(
                bad, emptyList(), passphrase, ByteArrayOutputStream(),
            )
        }
    }

    @Test fun unexpectedZipEntryIsRejectedAfterAllDataParts() {
        val original = createArchive(includePhoto = false)
        val out = ByteArrayOutputStream()
        ZipInputStream(ByteArrayInputStream(original)).use { reader ->
            ZipOutputStream(out).use { writer ->
                while (true) {
                    val entry = reader.nextEntry ?: break
                    val bytes = reader.readBytes()
                    writer.putNextEntry(ZipEntry(entry.name))
                    writer.write(bytes)
                    writer.closeEntry()
                    reader.closeEntry()
                }
                writer.putNextEntry(ZipEntry("../../not-allowed.txt"))
                writer.write("malicious extra".toByteArray())
                writer.closeEntry()
            }
        }
        val root = File(System.getProperty("java.io.tmpdir"), "v300-extra-zip-${System.nanoTime()}")
        check(root.mkdirs())
        try {
            assertThrows(IllegalArgumentException::class.java) {
                V300EncryptedPersonalArchive.stageAndVerify(
                    ByteArrayInputStream(out.toByteArray()), passphrase, root,
                )
            }
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally { root.deleteRecursively() }
    }

    private fun rewriteZip(
        zipBytes: ByteArray,
        rewrite: (String, ByteArray) -> ByteArray?,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { source ->
            ZipOutputStream(output).use { target ->
                while (true) {
                    val entry = source.nextEntry ?: break
                    val original = source.readBytes()
                    val newBytes = rewrite(entry.name, original)
                    if (newBytes != null) {
                        target.putNextEntry(ZipEntry(entry.name))
                        target.write(newBytes)
                        target.closeEntry()
                    }
                    source.closeEntry()
                }
            }
        }
        return output.toByteArray()
    }
}
