package com.framebynavin.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.Phase1HabitStore
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Disposable Android 15 CI only. Real store readers, no cloud/session,
 * actual owner phone, alarm delivery, restore or production installation.
 */
@RunWith(AndroidJUnit4::class)
class TrustT1C3LocalEncryptedPreviewDeviceTest {
    @Test fun encryptedPartialReviewAndCrashCleanupDoNotMutateOwnerData() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val preview = TrustT1C3LocalEncryptedPreview(app)
        val passphrase = "ci-disposable-strong-local-preview-only".toCharArray()
        val ownerId = "ci-disposable-owner-not-a-real-account"
        val beforeProjects = TaskStore(app).exportJsonForTrustReadOnly()
        val beforeIdeas = IdeaVaultStore(app).exportJson()
        val beforePersonal = PersonalReminderStore(app).exportPersonalJson()
        val beforeChallenge = Phase1HabitStore(app).exportJson()

        val root = File(app.noBackupFilesDir, "trust-t1c3-local-review-v1")
        val pending = File(root, "pending").apply { mkdirs() }
        val stale = File(pending, "pending-" + UUID.randomUUID() + ".enc.new").apply {
            writeText("abandoned encrypted fixture")
        }
        val tempPlain = File(app.cacheDir,
            "trust-t1c2-owner-bytes-ci-" + UUID.randomUUID()).apply { mkdirs() }
        File(tempPlain, "uncommitted-fixture").writeText("abandoned plaintext")
        val tempVerify = File(app.cacheDir,
            "backlot-trust-check-ci-" + UUID.randomUUID()).apply { mkdirs() }
        File(tempVerify, "part-0001.bin").writeText("abandoned decrypted fixture")

        var receipt: TrustLocalPartialPreviewReceipt? = null
        try {
            receipt = preview.createPartialReview(ownerId, passphrase)
            val result = requireNotNull(receipt)
            val verified = File(root, "verified/verified-" + result.previewId + ".enc")
            assertTrue(verified.isFile && verified.length() > 1000)
            assertEquals(64, result.encryptedSha256.length)
            assertTrue(result.verifiedParts >= 8)
            assertFalse(result.isUniversalBackup)
            assertFalse(result.canUploadToCloud)
            assertFalse(result.canRestoreOwnerData)
            assertFalse(stale.exists())
            assertFalse(tempPlain.exists())
            assertFalse(tempVerify.exists())

            val second = TrustT1C3LocalEncryptedPreview(app).verifyPartialReview(
                result.previewId, ownerId, passphrase,
            )
            assertEquals(result.encryptedSha256, second.encryptedSha256)
            assertEquals(result.verifiedParts, second.verifiedParts)
            assertThrows(Exception::class.java) {
                runBlocking {
                    preview.verifyPartialReview(result.previewId,
                        "different-owner", passphrase)
                }
            }
            assertThrows(Exception::class.java) {
                runBlocking {
                    preview.verifyPartialReview(result.previewId, ownerId,
                        "different-and-strong-password".toCharArray())
                }
            }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    preview.createPartialReview(ownerId, "weak".toCharArray())
                }
            }
            assertTrue(verified.isFile)
            assertEquals(result.encryptedSha256,
                preview.verifyPartialReview(result.previewId, ownerId, passphrase)
                    .encryptedSha256)

            assertEquals(beforeProjects, TaskStore(app).exportJsonForTrustReadOnly())
            assertEquals(beforeIdeas, IdeaVaultStore(app).exportJson())
            assertEquals(beforePersonal, PersonalReminderStore(app).exportPersonalJson())
            assertEquals(beforeChallenge, Phase1HabitStore(app).exportJson())
        } finally {
            passphrase.fill('\u0000')
            receipt?.let { saved ->
                check(File(root, "verified/verified-" + saved.previewId + ".enc").delete())
            }
            stale.delete()
            tempPlain.deleteRecursively()
            tempVerify.deleteRecursively()
        }
    }
}
