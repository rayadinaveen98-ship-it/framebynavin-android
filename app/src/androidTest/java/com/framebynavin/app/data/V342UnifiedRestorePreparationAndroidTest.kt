package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * H2 synthetic-only tests. They never call a Creator restore, Personal restore,
 * OS scheduler, live photo writer or the user's real backup documents.
 */
@RunWith(AndroidJUnit4::class)
class V342UnifiedRestorePreparationAndroidTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun newRoot(): File = File(app.filesDir, "v342-test-" + UUID.randomUUID())
    private fun record(bytes: ByteArray) = V342UnifiedPreparationRecord(
        UUID.randomUUID().toString(),
        1_800_000_000_000L,
        bytes.size.toLong(),
        sha(bytes),
    )
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    @Test fun recordRoundTripAndChecksumBoundMetadata() {
        val r = record("synthetic encrypted bytes".toByteArray())
        assertEquals(r, V342UnifiedPreparationCodec.decode(
            V342UnifiedPreparationCodec.encode(r)
        ))
        val changed = JSONObject(String(V342UnifiedPreparationCodec.encode(r)))
            .put("sizeBytes", r.sizeBytes + 1)
        assertThrows(Exception::class.java) {
            V342UnifiedPreparationCodec.decode(changed.toString().toByteArray())
        }
        val extra = JSONObject(String(V342UnifiedPreparationCodec.encode(r)))
            .put("unexpected", "field")
        assertThrows(Exception::class.java) {
            V342UnifiedPreparationCodec.decode(extra.toString().toByteArray())
        }
    }

    @Test fun noPreparedSourceMeansNoWorkAndNoImplicitDatabaseChange() = runBlocking {
        val root = newRoot()
        try {
            val store = V342UnifiedRestorePreparationStore(app, root)
            assertEquals(V342UnifiedRestorePreparationStore.Status.None, store.status())
            assertFalse(store.discard(ownerConfirmed = true))
            assertFalse(root.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun interruptedPreparationRequiresOwnerReviewAndConsent() = runBlocking {
        val root = newRoot()
        try {
            assertTrue(root.mkdir())
            File(root, "incoming-unified-encrypted.zip").writeText("partial")
            val store = V342UnifiedRestorePreparationStore(app, root)
            assertEquals(V342UnifiedRestorePreparationStore.Status.NeedsOwnerReview,
                store.status())
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { store.discard(ownerConfirmed = false) }
            }
            assertTrue(root.exists())
            assertTrue(store.discard(ownerConfirmed = true))
            assertEquals(V342UnifiedRestorePreparationStore.Status.None, store.status())
        } finally { root.deleteRecursively() }
    }

    @Test fun checksumMatchDoesNotGrantRestoreOrCryptographicAuthentication() = runBlocking {
        val root = newRoot()
        try {
            assertTrue(root.mkdir())
            val fake = "not a valid encrypted unified archive".toByteArray()
            File(root, "incoming-unified-encrypted.zip").writeBytes(fake)
            File(root, "prepared.json").writeBytes(
                V342UnifiedPreparationCodec.encode(record(fake))
            )
            val store = V342UnifiedRestorePreparationStore(app, root)
            assertTrue(store.status() is V342UnifiedRestorePreparationStore.Status.ChecksumMatched)
            assertThrows(Exception::class.java) {
                runBlocking {
                    store.inspectPrepared("owner-password-12345".toCharArray())
                }
            }
            assertTrue(File(root, "incoming-unified-encrypted.zip").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun tamperedEncryptedSourceIsQuarantinedWithoutDeletingIt() = runBlocking {
        val root = newRoot()
        try {
            assertTrue(root.mkdir())
            val original = "encrypted owner bytes".toByteArray()
            val owned = File(root, "incoming-unified-encrypted.zip")
            owned.writeBytes(original)
            File(root, "prepared.json").writeBytes(
                V342UnifiedPreparationCodec.encode(record(original))
            )
            owned.appendText("tampered")
            val store = V342UnifiedRestorePreparationStore(app, root)
            assertEquals(V342UnifiedRestorePreparationStore.Status.NeedsOwnerReview,
                store.status())
            assertTrue(owned.exists())
            assertThrows(IllegalStateException::class.java) {
                runBlocking { store.inspectPrepared("owner-password-12345".toCharArray()) }
            }
            assertTrue(owned.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun noSelectionConsentMeansAbsolutelyNoStaging() = runBlocking {
        val root = newRoot()
        try {
            val store = V342UnifiedRestorePreparationStore(app, root)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    store.prepare(ByteArrayInputStream("synthetic".toByteArray()),
                        "private-passphrase-123".toCharArray(), ownerRequested = false)
                }
            }
            assertFalse(root.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun malformedSourceIsQuarantinedUntilOwnerExplicitlyDiscards() = runBlocking {
        val root = newRoot()
        try {
            val store = V342UnifiedRestorePreparationStore(app, root)
            assertThrows(Exception::class.java) {
                runBlocking {
                    store.prepare(ByteArrayInputStream("not a ZIP".toByteArray()),
                        "private-passphrase-123".toCharArray(), ownerRequested = true)
                }
            }
            assertEquals(V342UnifiedRestorePreparationStore.Status.NeedsOwnerReview,
                store.status())
            assertTrue(File(root, "incoming-unified-encrypted.zip").isFile)
            assertTrue(store.discard(ownerConfirmed = true))
            assertFalse(root.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun tornAtomicPreparationJournalRemainsUntouchedOnInspection() = runBlocking {
        val root = newRoot()
        try {
            assertTrue(root.mkdir())
            val fake = "synthetic encrypted file".toByteArray()
            File(root, "incoming-unified-encrypted.zip").writeBytes(fake)
            File(root, "prepared.json").writeBytes(
                V342UnifiedPreparationCodec.encode(record(fake))
            )
            val backup = File(root, "prepared.json.bak").apply {
                writeBytes(V342UnifiedPreparationCodec.encode(record(fake)))
            }
            val originalBackup = backup.readBytes()
            val store = V342UnifiedRestorePreparationStore(app, root)
            assertEquals(V342UnifiedRestorePreparationStore.Status.NeedsOwnerReview,
                store.status())
            assertTrue(backup.isFile)
            assertTrue(originalBackup.contentEquals(backup.readBytes()))
        } finally { root.deleteRecursively() }
    }

    @Test fun unexpectedFileBlocksEvenExplicitDestructiveCleanup() = runBlocking {
        val root = newRoot()
        try {
            assertTrue(root.mkdir())
            File(root, "unexpected-owner-photo.jpg").writeBytes(byteArrayOf(1,2,3))
            val store = V342UnifiedRestorePreparationStore(app, root)
            assertEquals(V342UnifiedRestorePreparationStore.Status.NeedsOwnerReview,
                store.status())
            assertThrows(Exception::class.java) {
                runBlocking { store.discard(ownerConfirmed = true) }
            }
            assertTrue(File(root, "unexpected-owner-photo.jpg").exists())
        } finally { root.deleteRecursively() }
    }
}
