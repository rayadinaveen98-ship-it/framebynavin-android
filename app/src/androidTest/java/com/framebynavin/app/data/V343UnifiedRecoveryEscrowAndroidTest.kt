package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
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
 * Synthetic-only H3 acceptance. This class NEVER touches a real backup,
 * Creator restore, Personal record, alarm lease, Space or owner photo.
 */
@RunWith(AndroidJUnit4::class)
class V343UnifiedRecoveryEscrowAndroidTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun isolatedRoot(): File = File(app.filesDir, "v343-test-" + UUID.randomUUID())
    private val hash = "a".repeat(64)
    private val otherHash = "b".repeat(64)

    private fun receipt(): V343UnifiedRecoveryRecord = V343UnifiedRecoveryRecord(
        transactionId = UUID.randomUUID().toString(),
        createdAtMillis = 1_800_000_000_000L,
        selectedSourceId = UUID.randomUUID().toString(),
        selectedSourceSha256 = hash,
        rollbackSha256 = otherHash,
        rollbackBytes = 123L,
        creatorBeforeSha256 = hash,
        personalBeforeSha256 = otherHash,
        state = V343UnifiedRecoveryState.ESCROW_VERIFIED_NO_WRITE,
    )

    @Test fun escrowJournalBindsTwoFingerprintsSourceAndEncryptedOriginal() {
        val baseline = receipt()
        val raw = V343UnifiedRecoveryCodec.encode(baseline)
        assertEquals(baseline, V343UnifiedRecoveryCodec.decode(raw))
        assertFalse(String(raw).contains("Personal reminder"))
        val changed = JSONObject(String(raw)).put("selectedSourceSha256", otherHash)
        assertThrows(Exception::class.java) {
            V343UnifiedRecoveryCodec.decode(changed.toString().toByteArray())
        }
        val extra = JSONObject(String(raw)).put("devicePassword", "must not be recorded")
        assertThrows(Exception::class.java) {
            V343UnifiedRecoveryCodec.decode(extra.toString().toByteArray())
        }
    }

    @Test fun driftOrMissingFingerprintCanNeverBeTreatedAsStable() {
        val original = V343UnifiedBaselineFingerprint(hash, otherHash)
        assertTrue(V343UnifiedBaselinePolicy.noDrift(original, original))
        assertFalse(V343UnifiedBaselinePolicy.noDrift(
            original, original.copy(personalSha256 = hash)))
        assertFalse(V343UnifiedBaselinePolicy.noDrift(
            original, original.copy(creatorSha256 = otherHash)))
        assertFalse(V343UnifiedBaselinePolicy.noDrift(
            original, V343UnifiedBaselineFingerprint("invalid", otherHash)))
    }

    @Test fun noEscrowExistsAndConsentIsMandatoryBeforeAnyMutation() = runBlocking {
        val root = isolatedRoot()
        try {
            val manager = V343UnifiedRecoveryEscrow(app, root)
            assertEquals(V343UnifiedRecoveryEscrow.Status.None, manager.status())
            assertFalse(manager.discard(ownerConfirmed = true))
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    manager.retainOriginal(
                        ownerConfirmed = false,
                        passphrase = "strong-private-password".toCharArray())
                }
            }
            assertFalse(root.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun incompleteEscrowIsVisibleAndNeverInventsRecoveryAuthority() = runBlocking {
        val root = isolatedRoot()
        try {
            assertTrue(root.mkdir())
            File(root, "original-owner-encrypted-unified.zip").writeText("truncated")
            val manager = V343UnifiedRecoveryEscrow(app, root)
            assertEquals(V343UnifiedRecoveryEscrow.Status.NeedsOwnerReview,
                manager.status())
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { manager.discard(ownerConfirmed = false) }
            }
            assertTrue(File(root, "original-owner-encrypted-unified.zip").exists())
            assertTrue(manager.discard(ownerConfirmed = true))
            assertFalse(root.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun tamperedJournalCannotBeDeletedEvenWithOwnerConfirmation() = runBlocking {
        val root = isolatedRoot()
        try {
            assertTrue(root.mkdir())
            File(root, "original-owner-encrypted-unified.zip").writeText("synthetic")
            val changed = JSONObject(String(V343UnifiedRecoveryCodec.encode(receipt())))
                .put("rollbackBytes", 999L)
            File(root, "escrow.json").writeText(changed.toString())
            val manager = V343UnifiedRecoveryEscrow(app, root)
            assertEquals(V343UnifiedRecoveryEscrow.Status.NeedsOwnerReview,
                manager.status())
            assertThrows(Exception::class.java) {
                runBlocking { manager.discard(ownerConfirmed = true) }
            }
            assertTrue(File(root, "original-owner-encrypted-unified.zip").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun tornAtomicJournalBackupCanNeverBeDiscardedAsOrdinaryStaging() = runBlocking {
        val root = isolatedRoot()
        try {
            assertTrue(root.mkdir())
            File(root, "original-owner-encrypted-unified.zip").writeText("synthetic")
            File(root, "escrow.json.bak").writeBytes(
                V343UnifiedRecoveryCodec.encode(receipt())
            )
            val manager = V343UnifiedRecoveryEscrow(app, root)
            assertThrows(Exception::class.java) {
                runBlocking { manager.discard(ownerConfirmed = true) }
            }
            assertTrue(File(root, "escrow.json.bak").isFile)
        } finally { root.deleteRecursively() }
    }

    @Test fun escrowReadOnlyInspectionPreservesTornAtomicBackupBytes() = runBlocking {
        val root = isolatedRoot()
        try {
            assertTrue(root.mkdir())
            File(root, "original-owner-encrypted-unified.zip").writeText("synthetic")
            val main = File(root, "escrow.json").apply {
                writeBytes(V343UnifiedRecoveryCodec.encode(receipt()))
            }
            val backup = File(root, "escrow.json.bak").apply {
                writeBytes(main.readBytes())
            }
            val bytes = backup.readBytes()
            val manager = V343UnifiedRecoveryEscrow(app, root)
            assertEquals(V343UnifiedRecoveryEscrow.Status.NeedsOwnerReview,
                manager.status())
            assertTrue(backup.exists())
            assertTrue(bytes.contentEquals(backup.readBytes()))
        } finally { root.deleteRecursively() }
    }

    @Test fun unknownOriginalFileCannotBeRemovedByEscrowCleanup() = runBlocking {
        val root = isolatedRoot()
        try {
            assertTrue(root.mkdir())
            File(root, "unknown-important-owner-photo.jpg").writeBytes(byteArrayOf(1,2,3))
            val manager = V343UnifiedRecoveryEscrow(app, root)
            assertThrows(Exception::class.java) {
                runBlocking { manager.discard(ownerConfirmed = true) }
            }
            assertTrue(File(root, "unknown-important-owner-photo.jpg").isFile)
        } finally { root.deleteRecursively() }
    }
}
