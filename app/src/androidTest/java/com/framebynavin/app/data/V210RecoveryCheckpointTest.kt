package com.framebynavin.app.data

import android.graphics.Bitmap
import android.util.AtomicFile
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android 11 disposable-emulator acceptance of the V210 pending journal path.
 * Models the on-disk state left after the new image order was published but
 * before the durable restore marker could be cleared. No real process kill is
 * performed: reboot/death/low-storage need separate acceptance.
 */
@RunWith(AndroidJUnit4::class)
class V210RecoveryCheckpointTest {
    @Test fun incompleteCompactRestoreRecoversPreviousOriginalsAndPreservesJournalOnCorruption(): Unit =
        runBlocking {
            HardeningTestEnvironment.requireCiEmulator()
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val creator = CreatorBackupManager(context)
            val baseline = creator.createBackup() // also resolves any older pending recovery
            val root = File(context.filesDir, "creator_restore_recovery_v181").apply { mkdirs() }
            val pending = File(root, "pending.json")
            assertFalse("Disposable emulator must begin with no pending journal", pending.exists())
            val recovery = File(root, "pre-restore-${UUID.randomUUID()}.fbnbackup")
            try {
                val bytes = tinyPng()
                val sum = sha256(bytes)
                val frame = JSONArray().put(
                    JSONObject().put("name", "original.png")
                        .put("sha256", sum)
                        .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)),
                )
                CreatorHeroArchive.import(context, frame.toString())
                val oldOrder = CreatorHeroArchive.captureOriginalOrder(context)
                assertEquals(1, JSONArray(oldOrder).length())

                // Same metadata journal format as restorePortableCompact():
                // empty Base64 image array + SHA-bound original image references.
                val compactRecovery = creator.rewritePortablePersonalFrames(
                    creator.createBackup(), "[]",
                )
                writeAtomic(recovery, compactRecovery)
                val marker = JSONObject()
                    .put("file", recovery.name)
                    .put("sha256", sha256(compactRecovery.toByteArray(Charsets.UTF_8)))
                    .put("originalFrames", oldOrder)
                    .put("originalFramesSha256", sha256(oldOrder.toByteArray(Charsets.UTF_8)))

                // Imitate an interrupted restore after changing visible order.
                // The original image file stays on disk and can be reinstated.
                CreatorHeroArchive.import(context, "[]")
                assertNotEquals(oldOrder, CreatorHeroArchive.captureOriginalOrder(context))

                // Corrupted journal must fail CLOSED and retain the marker for
                // explicit recovery, rather than erasing the last good order.
                val tampered = JSONObject(marker.toString())
                    .put("originalFramesSha256", "0".repeat(64))
                writeAtomic(pending, tampered.toString())
                val error = runCatching { creator.recoverPendingRestore() }.exceptionOrNull()
                assertTrue("Tampered recovery checkpoint must be rejected", error is IllegalArgumentException)
                assertTrue("Failed recovery must preserve pending journal", pending.isFile)
                assertEquals("[]", CreatorHeroArchive.captureOriginalOrder(context))

                // Simulates restart reading the intact durable marker and
                // recovering the metadata + previous original image order.
                writeAtomic(pending, marker.toString())
                assertTrue(creator.recoverPendingRestore())
                assertEquals(oldOrder, CreatorHeroArchive.captureOriginalOrder(context))
                assertFalse("Successful recovery must clear pending marker", pending.exists())
                assertFalse("Recovered transaction must not replay twice", creator.recoverPendingRestore())
            } finally {
                // All mutations above occur only on the disposable CI emulator.
                // Remove only this test's pending fixture before restoring the
                // snapshot captured before the test.
                pending.delete()
                CreatorDataGate.nonCancellable { creator.restore(baseline) }
                recovery.delete()
            }
        }

    private fun writeAtomic(target: File, text: String) {
        val atomic = AtomicFile(target)
        val stream = atomic.startWrite()
        try {
            stream.write(text.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
    }

    private fun tinyPng(): ByteArray {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        return try {
            ByteArrayOutputStream().also {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }.toByteArray()
        } finally {
            bitmap.recycle()
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
