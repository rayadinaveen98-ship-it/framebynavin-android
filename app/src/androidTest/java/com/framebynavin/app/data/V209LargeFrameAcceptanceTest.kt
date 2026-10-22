package com.framebynavin.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipInputStream
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Disposable CI-emulator-only acceptance: the old ZIP's backlot.json cannot
 * contain >32 MiB of Base64 original Best Frames; P3 must move the image bytes
 * into independently checksum-verified ZIP entries instead.
 *
 * This uses a tiny valid PNG image with a standards-compliant, large tEXt
 * ancillary chunk. It stresses the *byte limits* without decoding huge pixels.
 * It is NOT a low-memory device benchmark or a real-photo decoding benchmark.
 */
@RunWith(AndroidJUnit4::class)
class V209LargeFrameAcceptanceTest {
    @Test fun twoOriginalImagesExceedLegacyJsonCapButPreviewStillValidates(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val core = CreatorBackupManager(context)
        val original = core.createBackup()
        try {
            val image = oversizedButValidPng(13 * 1024 * 1024)
            val sha = MessageDigest.getInstance("SHA-256")
                .digest(image).joinToString("") { "%02x".format(it) }
            val entries = JSONArray()
            for (index in 0 until 2) {
                entries.put(JSONObject()
                    .put("name", "p3-large-frame-$index.png")
                    .put("sha256", sha)
                    .put("data", Base64.encodeToString(image, Base64.NO_WRAP)))
            }
            CreatorHeroArchive.import(context, entries.toString())
            val largeCoreJson = core.createBackup()
            assertTrue(
                "Fixture must cross the old 32 MiB JSON entry cap",
                largeCoreJson.toByteArray(Charsets.UTF_8).size > 32 * 1024 * 1024,
            )
            // Remove large fixture intermediates before exporting.
            val portable = PortableCreatorBackupManager(context)
            val zipBytes = ByteArrayOutputStream().also { portable.writeBackup(it) }.toByteArray()
            var compactJsonSize = -1
            var frameCount = 0
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == "backlot.json") compactJsonSize = zip.readBytes().size
                    if (entry.name.startsWith("best_frames/frame-")) frameCount++
                    zip.closeEntry()
                }
            }
            assertTrue("P3 compact JSON must stay within 32 MiB", compactJsonSize in 1..(32 * 1024 * 1024))
            assertEquals(2, frameCount)
            val preview = portable.validate(ByteArrayInputStream(zipBytes))
            assertTrue(preview.personalFramesIncluded)
            assertEquals(2, preview.personalFrameCount)
        } finally {
            CreatorDataGate.nonCancellable { core.restore(original) }
        }
    }

    private fun oversizedButValidPng(extraTextBytes: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val encoded = ByteArrayOutputStream().also {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }.toByteArray()
        bitmap.recycle()
        check(encoded.size >= 12)
        val type = "tEXt".toByteArray(Charsets.US_ASCII)
        val data = ByteArray(extraTextBytes + 8) { 'p'.code.toByte() }
        val key = "Comment".toByteArray(Charsets.US_ASCII)
        key.copyInto(data, 0)
        data[key.size] = 0
        val crc = CRC32().apply { update(type); update(data) }
        val buffer = ByteArrayOutputStream(encoded.size + data.size + 12)
        buffer.write(encoded, 0, encoded.size - 12) // before PNG IEND
        buffer.write(int32(data.size))
        buffer.write(type)
        buffer.write(data)
        buffer.write(int32(crc.value.toInt()))
        buffer.write(encoded, encoded.size - 12, 12)
        return buffer.toByteArray().also { bytes ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            check(bounds.outWidth == 1 && bounds.outHeight == 1) {
                "Large PNG fixture must remain image-decodable"
            }
        }
    }

    private fun int32(value: Int): ByteArray = byteArrayOf(
        (value ushr 24).toByte(), (value ushr 16).toByte(),
        (value ushr 8).toByte(), value.toByte(),
    )
}
