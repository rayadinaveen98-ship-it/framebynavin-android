package com.framebynavin.app.data

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.zip.CRC32
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Builds a valid >8 MiB PNG without retaining its media bytes in memory.
 * Exercises the SAME compact export and verification used by BackupActivity,
 * including Android JSON escaping and SHA-256 integrity checks.
 * CI disposable emulator only. Never modify a real user's photos.
 */
@RunWith(AndroidJUnit4::class)
class V277MemoryBoundedBackupPreviewTest {
    @Test fun largeOriginalFrameIsVerifiedByStreamingWithoutFullImageBase64Allocation() {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir, "best_frames_original").apply { mkdirs() }
        val filename = "v277-memory-regression.png"
        val file = File(directory, filename)
        try {
            writeLargeValidPng(file, 8 * 1024 * 1024)
            assertTrue("Fixture must contain multi-megabyte original media", file.length() > 8L * 1024 * 1024)
            val prepared = PortableBestFrameMedia.prepareFromFiles(context.filesDir, listOf(filename))
            val compact = JSONObject()
                .put("schemaVersion", CreatorBackupManager.SCHEMA_VERSION)
                .put("personalFrames", prepared.personalFramesSection)
                .toString()
            assertEquals(1, PortableBestFrameMedia.verifyCompactPreview(compact, prepared.frames, true))
            // The verifier must still reject a changed image, rather than
            // skipping media integrity to avoid allocation.
            RandomAccessFile(file, "rw").use { random ->
                random.seek(file.length() - 32)
                val byte = random.read()
                random.seek(file.length() - 32)
                random.write(byte xor 1)
            }
            val rejected = runCatching {
                PortableBestFrameMedia.verifyCompactPreview(compact, prepared.frames, true)
            }.isFailure
            assertTrue("Tampered original frame must fail closed", rejected)
        } finally {
            file.delete()
        }
    }

    @Test fun emptyOriginalFramesRetainLegacyArrayCompatibility() {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val empty = PortableBestFrameMedia.prepareFromFiles(context.filesDir, emptyList())
        assertEquals("[]", empty.personalFramesSection)
        assertTrue(empty.frames.isEmpty())
        val compact = JSONObject()
            .put("schemaVersion", CreatorBackupManager.SCHEMA_VERSION)
            .put("personalFrames", "[]")
            .toString()
        assertEquals(null, PortableBestFrameMedia.verifyCompactPreview(compact, emptyList(), false))
    }

    private fun writeLargeValidPng(file: File, fillerBytes: Int) {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val original = try {
            bitmap.eraseColor(0xFF186599.toInt())
            ByteArrayOutputStream().also { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }.toByteArray()
        } finally {
            bitmap.recycle()
        }
        require(original.size >= 12)
        val type = "tEXt".toByteArray(Charsets.US_ASCII)
        val key = "Comment\u0000".toByteArray(Charsets.US_ASCII)
        val bytes = ByteArray(8192) { 'p'.code.toByte() }
        val crc = CRC32().apply { update(type); update(key) }
        FileOutputStream(file).use { output ->
            output.write(original, 0, original.size - 12)
            output.write(int32(key.size + fillerBytes))
            output.write(type)
            output.write(key)
            var remaining = fillerBytes
            while (remaining > 0) {
                val n = minOf(remaining, bytes.size)
                output.write(bytes, 0, n)
                crc.update(bytes, 0, n)
                remaining -= n
            }
            output.write(int32(crc.value.toInt()))
            output.write(original, original.size - 12, 12)
        }
    }

    private fun int32(value: Int): ByteArray = byteArrayOf(
        (value ushr 24).toByte(), (value ushr 16).toByte(),
        (value ushr 8).toByte(), value.toByte()
    )
}
