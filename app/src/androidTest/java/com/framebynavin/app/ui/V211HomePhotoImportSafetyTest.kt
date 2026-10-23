package com.framebynavin.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorHeroArchive
import com.framebynavin.app.data.HardeningTestEnvironment
import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * All persistence tests run only in a disposable CI emulator. The user's
 * installed creator library must never be used to exercise destructive cases.
 */
@RunWith(AndroidJUnit4::class)
class V211HomePhotoImportSafetyTest {
    private fun withIsolatedSelection(test: suspend (Context, File) -> Unit) = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("best_frames_original_prefs", Context.MODE_PRIVATE)
        val oldOrder = prefs.getString("frame_order", null)
        val dir = File(context.filesDir, "best_frames_original").apply { mkdirs() }
        val prior = dir.listFiles()?.map { it.name }?.toSet().orEmpty()
        try {
            // Expose no original data during fixture verification.
            check(prefs.edit().putString("frame_order", "").commit())
            test(context, dir)
        } finally {
            if (oldOrder == null) check(prefs.edit().remove("frame_order").commit())
            else check(prefs.edit().putString("frame_order", oldOrder).commit())
            dir.listFiles()?.filterNot { it.name in prior }?.forEach { it.delete() }
        }
    }

    private fun png(context: Context, label: String): File {
        val file = File(context.cacheDir, "home_import_${label}_${System.nanoTime()}.png")
        val image = Bitmap.createBitmap(36, 24, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { check(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            image.recycle()
        }
        return file
    }

    @Test
    fun validPhotoImportProducesBackupCompatibleOriginal() = withIsolatedSelection { context, _ ->
        val source = png(context, "valid")
        try {
            val imported = V18HomeHeroStore.replace(context, listOf(Uri.fromFile(source)))
            assertEquals(1, imported.size)
            assertTrue(imported.single().isFile)
            assertEquals(listOf(imported.single().name), CreatorHeroArchive.orderedSourceNames(context))
            assertTrue(CreatorHeroArchive.captureOriginalOrder(context).contains("sha256"))
        } finally {
            source.delete()
        }
    }

    @Test
    fun invalidSecondPhotoRejectsWholeReplacementAndPreservesPreviousOrder() =
        withIsolatedSelection { context, _ ->
            val first = png(context, "original")
            val next = png(context, "candidate")
            val broken = File(context.cacheDir, "home_import_broken_${System.nanoTime()}.png")
            broken.writeText("This is not an image")
            try {
                val installed = V18HomeHeroStore.replace(context, listOf(Uri.fromFile(first)))
                val saved = CreatorHeroArchive.orderedSourceNames(context)
                val attempt = runCatching {
                    V18HomeHeroStore.replace(context, listOf(Uri.fromFile(next), Uri.fromFile(broken)))
                }
                assertTrue("Invalid second image must reject whole selection", attempt.isFailure)
                assertEquals(saved, CreatorHeroArchive.orderedSourceNames(context))
                assertTrue(installed.single().isFile)
            } finally {
                first.delete()
                next.delete()
                broken.delete()
            }
        }

    @Test
    fun oversizedPhotoFailsBeforeVisibleOrderCanChange() =
        withIsolatedSelection { context, _ ->
            val original = png(context, "keep")
            val oversized = File(context.cacheDir, "home_import_oversized_${System.nanoTime()}.png")
            try {
                val imported = V18HomeHeroStore.replace(context, listOf(Uri.fromFile(original)))
                val saved = CreatorHeroArchive.orderedSourceNames(context)
                RandomAccessFile(oversized, "rw").use {
                    it.setLength(20L * 1024 * 1024 + 1)
                }
                val attempt = runCatching {
                    V18HomeHeroStore.replace(context, listOf(Uri.fromFile(oversized)))
                }
                assertTrue("Over-budget photo must be rejected", attempt.isFailure)
                assertEquals(saved, CreatorHeroArchive.orderedSourceNames(context))
                assertTrue(imported.single().isFile)
            } finally {
                original.delete()
                oversized.delete()
            }
        }
}
