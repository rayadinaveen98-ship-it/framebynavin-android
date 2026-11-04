package com.framebynavin.app.personal.spaces

import android.graphics.Bitmap
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic camera images. Never use owner photos or phone media. */
@RunWith(AndroidJUnit4::class)
class V291PhotoOrientationDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    private fun syntheticJpeg(): ByteArray {
        val bitmap = Bitmap.createBitmap(120, 60, Bitmap.Config.ARGB_8888)
        try {
            val data = ByteArrayOutputStream()
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 94, data))
            return data.toByteArray()
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun portraitExifIsAppliedBeforeGpsAndCameraMetadataAreRemoved() {
        HardeningTestEnvironment.requireCiEmulator()
        val file = File(app.cacheDir, "v291-exif-" + UUID.randomUUID() + ".jpg")
        try {
            file.writeBytes(syntheticJpeg())
            val original = ExifInterface(file.absolutePath)
            original.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_ROTATE_90.toString(),
            )
            original.setAttribute(ExifInterface.TAG_MAKE, "EXIF_PRIVATE_MARKER")
            original.saveAttributes()
            assertEquals(
                ExifInterface.ORIENTATION_ROTATE_90,
                ExifInterface(file.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
                ),
            )

            val photo = V290PrivatePhotoFiles.sanitizeImage(
                file.readBytes(), UUID.randomUUID().toString(),
            )
            assertEquals(60, photo.widthPx)
            assertEquals(120, photo.heightPx)
            val clean = ExifInterface(ByteArrayInputStream(photo.bytes))
            assertEquals(
                ExifInterface.ORIENTATION_NORMAL,
                clean.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
                ),
            )
            assertNull("Sanitized private photo must not retain camera identity",
                clean.getAttribute(ExifInterface.TAG_MAKE))
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
            assertNull(
                ExifInterface(ByteArrayInputStream(photo.bytes))
                    .getAttribute(ExifInterface.TAG_MODEL),
            )
        } finally {
            file.delete()
        }
    }
}
