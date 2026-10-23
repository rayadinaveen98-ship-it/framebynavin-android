package com.framebynavin.app.data

import android.graphics.Bitmap
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Disposable emulator compatibility contract: portable original-frame
 * checksums remain EXACTLY compatible with the legacy JSONArray/Base64 bytes.
 */
@RunWith(AndroidJUnit4::class)
class V210StreamedFramesExportTest {
    @Test fun streamedImageArrayHashMatchesLegacyJsonAcrossMultipleFrames(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val core = CreatorBackupManager(context)
        val baseline = core.createBackup()
        try {
            val rows = JSONArray()
            rows.put(image("frame-first.png", 0xFF1199EE.toInt(), 2, 3))
            rows.put(image("frame-second.png", 0xFF55CC22.toInt(), 3, 5))
            rows.put(image("third-frame.jpeg", 0xFFCC2288.toInt(), 5, 7))
            CreatorHeroArchive.import(context, rows.toString())
            val original = CreatorHeroArchive.export(context)
            val prepared = PortableBestFrameMedia.prepareFromFiles(
                context.filesDir, CreatorHeroArchive.orderedSourceNames(context),
            )
            val marker = JSONObject(prepared.personalFramesSection)
            assertEquals(3, marker.getInt("count"))
            assertEquals(3, prepared.frames.size)
            assertEquals(sha256(original.toByteArray(Charsets.UTF_8)),
                marker.getString("originalSha256"))

            val packed = core.createPortableBackup()
            assertEquals(prepared.personalFramesSection,
                JSONObject(packed.compactJson).getString("personalFrames"))
            core.verifyPortableJsonEnvelope(packed.compactJson)
            assertEquals(3, PortableBestFrameMedia.verifyCompactPreview(
                packed.compactJson, packed.frames, true))
        } finally {
            CreatorHeroArchive.import(context, "[]")
            CreatorDataGate.nonCancellable { core.restore(baseline) }
        }
    }

    @Test fun emptyFramesRetainPlainArrayInsteadOfInventingManifest(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val core = CreatorBackupManager(context)
        val baseline = core.createBackup()
        try {
            CreatorHeroArchive.import(context, "[]")
            val prepared = PortableBestFrameMedia.prepareFromFiles(
                context.filesDir, CreatorHeroArchive.orderedSourceNames(context),
            )
            assertEquals("[]", prepared.personalFramesSection)
            assertTrue(prepared.frames.isEmpty())
            val packed = core.createPortableBackup()
            core.verifyPortableJsonEnvelope(packed.compactJson)
            assertEquals("[]", JSONObject(packed.compactJson).getString("personalFrames"))
            assertTrue(packed.frames.isEmpty())
        } finally {
            CreatorDataGate.nonCancellable { core.restore(baseline) }
        }
    }

    private fun image(name: String, color: Int, width: Int, height: Int): JSONObject {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bytes = try {
            bitmap.eraseColor(color)
            ByteArrayOutputStream().also {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }.toByteArray()
        } finally {
            bitmap.recycle()
        }
        return JSONObject()
            .put("name", name)
            .put("sha256", sha256(bytes))
            .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
