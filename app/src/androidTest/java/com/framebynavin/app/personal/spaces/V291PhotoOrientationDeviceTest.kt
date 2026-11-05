package com.framebynavin.app.personal.spaces

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fully synthetic four-color camera fixture: never accesses owner photos.
 *
 * A Bitmap-compressed JPEG has its rotation applied to PIXELS and has no
 * orientation dependency. On Android 15, ExifInterface can report
 * ORIENTATION_UNDEFINED (0) for such freshly re-encoded JPEGs, rather than
 * ORIENTATION_NORMAL (1). Neither is a pending rotation instruction.
 * Test pixel orientation AND metadata stripping, not an optional EXIF tag.
 */
@RunWith(AndroidJUnit4::class)
class V291PhotoOrientationDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    private fun syntheticJpeg(): ByteArray {
        val bitmap = Bitmap.createBitmap(120, 60, Bitmap.Config.ARGB_8888)
        try {
            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    val color = when {
                        x < 60 && y < 30 -> Color.RED
                        x >= 60 && y < 30 -> Color.GREEN
                        x < 60 -> Color.BLUE
                        else -> Color.YELLOW
                    }
                    bitmap.setPixel(x, y, color)
                }
            }
            val data = ByteArrayOutputStream()
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 94, data))
            return data.toByteArray()
        } finally {
            bitmap.recycle()
        }
    }

    private fun assertPixelNear(expected: Int, actual: Int) {
        // Allow JPEG 94 -> 88 lossy re-encoding while checking actual rotation.
        val tolerance = 70
        assertTrue("Rotated pixel's red channel is wrong",
            abs(Color.red(expected) - Color.red(actual)) <= tolerance)
        assertTrue("Rotated pixel's green channel is wrong",
            abs(Color.green(expected) - Color.green(actual)) <= tolerance)
        assertTrue("Rotated pixel's blue channel is wrong",
            abs(Color.blue(expected) - Color.blue(actual)) <= tolerance)
    }

    private fun assertOrientedQuadrants(
        raw: ByteArray, width: Int, height: Int,
        topLeft: Int, topRight: Int, bottomLeft: Int, bottomRight: Int,
    ) {
        val bitmap = requireNotNull(
            BitmapFactory.decodeByteArray(raw, 0, raw.size)
        ) { "Sanitized JPEG could not be decoded" }
        try {
            assertEquals(width, bitmap.width)
            assertEquals(height, bitmap.height)
            assertPixelNear(topLeft, bitmap.getPixel(width / 4, height / 4))
            assertPixelNear(topRight, bitmap.getPixel(width * 3 / 4, height / 4))
            assertPixelNear(bottomLeft, bitmap.getPixel(width / 4, height * 3 / 4))
            assertPixelNear(bottomRight, bitmap.getPixel(width * 3 / 4, height * 3 / 4))
        } finally {
            bitmap.recycle()
        }
    }

    private fun assertNoRotationMetadata(raw: ByteArray) {
        val exif = ExifInterface(ByteArrayInputStream(raw))
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED
        )
        assertTrue(
            "Sanitized JPEG must not require EXIF rotation (found $orientation)",
            orientation == ExifInterface.ORIENTATION_UNDEFINED ||
                orientation == ExifInterface.ORIENTATION_NORMAL,
        )
        assertNull(exif.getAttribute(ExifInterface.TAG_MAKE))
        assertNull(exif.getAttribute(ExifInterface.TAG_MODEL))
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
    }

    @Test fun portraitExifIsAppliedBeforeGpsAndCameraMetadataAreRemoved() {
        HardeningTestEnvironment.requireCiEmulator()
        val file = File(app.cacheDir, "v291-exif-" + UUID.randomUUID() + ".jpg")
        try {
            file.writeBytes(syntheticJpeg())
            ExifInterface(file.absolutePath).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_ROTATE_90.toString(),
                )
                setAttribute(ExifInterface.TAG_MAKE, "EXIF_PRIVATE_MARKER")
                setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
                setAttribute(ExifInterface.TAG_GPS_LATITUDE, "17/1,30/1,0/1")
                saveAttributes()
            }
            assertEquals(
                ExifInterface.ORIENTATION_ROTATE_90,
                ExifInterface(file.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED,
                ),
            )
            val photo = V290PrivatePhotoFiles.sanitizeImage(
                file.readBytes(), UUID.randomUUID().toString(),
            )
            assertEquals(60, photo.widthPx)
            assertEquals(120, photo.heightPx)
            // Clockwise 90: blue/red at the top, yellow/green at the bottom.
            assertOrientedQuadrants(
                photo.bytes, 60, 120,
                Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN,
            )
            assertNoRotationMetadata(photo.bytes)
        } finally {
            file.delete()
        }
    }

    @Test fun landscapeWithRotate180ExifRetainsDimensionsAndStripsMetadata() {
        HardeningTestEnvironment.requireCiEmulator()
        val file = File(app.cacheDir, "v291-exif-" + UUID.randomUUID() + ".jpg")
        try {
            file.writeBytes(syntheticJpeg())
            ExifInterface(file.absolutePath).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_ROTATE_180.toString(),
                )
                setAttribute(ExifInterface.TAG_MODEL, "Synthetic Test Only")
                saveAttributes()
            }
            val photo = V290PrivatePhotoFiles.sanitizeImage(
                file.readBytes(), UUID.randomUUID().toString(),
            )
            assertEquals(120, photo.widthPx)
            assertEquals(60, photo.heightPx)
            // 180: bottom-right moves to top-left and top-left moves to bottom-right.
            assertOrientedQuadrants(
                photo.bytes, 120, 60,
                Color.YELLOW, Color.BLUE, Color.GREEN, Color.RED,
            )
            assertNoRotationMetadata(photo.bytes)
        } finally {
            file.delete()
        }
    }
}
