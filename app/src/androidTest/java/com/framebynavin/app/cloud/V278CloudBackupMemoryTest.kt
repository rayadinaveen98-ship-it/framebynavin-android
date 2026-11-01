package com.framebynavin.app.cloud

import android.graphics.Bitmap
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorBackupManager
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorHeroArchive
import com.framebynavin.app.data.HardeningTestEnvironment
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable emulator only: never mutate actual cloud or user phone data. */
@RunWith(AndroidJUnit4::class)
class V278CloudBackupMemoryTest {
    @Test fun cloudSyncAvoidsPhotoBase64AndRestoringPreservesExistingOriginalFrames(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val core = CreatorBackupManager(context)
        val baseline = core.createBackup()
        try {
            val bitmap = Bitmap.createBitmap(3, 4, Bitmap.Config.ARGB_8888)
            val bytes = try {
                bitmap.eraseColor(0xFF1177BB.toInt())
                ByteArrayOutputStream().also {
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }.toByteArray()
            } finally { bitmap.recycle() }
            val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            val frames = JSONArray().put(JSONObject()
                .put("name", "v278-cloud-frame.png")
                .put("sha256", sha)
                .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)))
            CreatorHeroArchive.import(context, frames.toString())
            val originalOrder = CreatorHeroArchive.captureOriginalOrder(context)
            val cloud = core.createCloudBackup()
            val root = JSONObject(cloud)
            assertTrue(root.getString("personalFrames").contains("BacklotCloudOriginalFramesOmitted"))
            assertTrue("Cloud must never include original photo Base64", cloud.length < 2 * 1024 * 1024)
            val preview = core.validateCloud(cloud)
            assertEquals(0, preview.personalFrameCount)
            assertFalse(preview.personalFramesIncluded)
            assertTrue("Portable import must reject a cloud-only missing-photo snapshot",
                runCatching { core.validate(cloud) }.isFailure)
            val sanitized = CreatorCloudPayloadSanitizer.sanitize(cloud, core)
            core.validateCloud(sanitized)
            val restored = core.restoreCloud(sanitized)
            assertEquals(preview.projectCount, restored.projectCount)
            assertEquals("Cloud restore must preserve originals on this device",
                originalOrder, CreatorHeroArchive.captureOriginalOrder(context))
        } finally {
            CreatorHeroArchive.import(context, "[]")
            CreatorDataGate.nonCancellable { core.restore(baseline) }
        }
    }

    @Test fun checksumTamperingStillFailsClosedAndLegacyCloudSchemaRemainsCompatible(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val core = CreatorBackupManager(context)
        val newCloud = core.createCloudBackup()
        assertEquals(CreatorBackupManager.SCHEMA_VERSION, core.validateCloud(newCloud).schemaVersion)
        val changed = JSONObject(newCloud).apply {
            put("createdAtMillis", getLong("createdAtMillis") + 5)
        }.toString()
        assertTrue("Tampered cloud backup must not be accepted",
            runCatching { core.validateCloud(changed) }.isFailure)
        val legacy = core.createBackup()
        assertEquals(CreatorBackupManager.SCHEMA_VERSION, core.validateCloud(legacy).schemaVersion)
    }
}
