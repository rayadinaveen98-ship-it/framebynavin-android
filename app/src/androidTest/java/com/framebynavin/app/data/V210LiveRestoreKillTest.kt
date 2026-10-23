package com.framebynavin.app.data

import android.graphics.Bitmap
import android.util.AtomicFile
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
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
 * Disposable Android 11 CI-emulator only. These methods MUST run in separate
 * instrumentation processes with adb force-stop in between.
 *
 * Unlike the synthetic P4.1/P4.2 checkpoint tests, phase one invokes the
 * ACTUAL restorePortableCompact transaction and blocks at an internal
 * test-only callback after it has published the new image order.
 */
@RunWith(AndroidJUnit4::class)
class V210LiveRestoreKillTest {
    private val fixtureFolder = "v210_live_restore_kill_qa"
    private val journalFolder = "creator_restore_recovery_v181"

    @Test fun enterRealRestoreAfterPublishingImages(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.filesDir, fixtureFolder)
        val pending = File(context.filesDir, "$journalFolder/pending.json")
        assertFalse("Prior live-stop fixture must not be present", root.exists())
        assertFalse("Pending journal must not be present at start", pending.exists())
        // Progress breadcrumbs are persisted BEFORE accessing CreatorDataGate
        // so a hanging app initialization is distinguishable from a failed
        // restore, even if instrumentation is forcibly stopped by CI.
        check(root.mkdir()) { "Could not create isolated emulator fixture" }
        fun mark(stage: String) = writeAtomic(File(root, "progress.txt"), stage)
        mark("entered_test_before_manager_init")
        val creator = CreatorBackupManager(context)
        mark("manager_constructed_before_baseline_backup")
        val baseline = creator.createBackup()
        mark("baseline_backup_complete")
        writeAtomic(File(root, "baseline.json"), baseline)

        mark("baseline_written_before_first_hero_import")
        CreatorHeroArchive.import(context, frameArray("before-stop.png", 0xFF1166AA.toInt()))
        mark("first_hero_imported")
        val beforeOrder = CreatorHeroArchive.captureOriginalOrder(context)
        assertEquals(1, JSONArray(beforeOrder).length())
        writeAtomic(File(root, "before-order.json"), beforeOrder)
        mark("before_original_creator_backup")
        val beforeCreator = creator.createBackup()
        mark("original_creator_backup_complete")
        writeAtomic(File(root, "before-creator.json"), beforeCreator)

        // Obtain an authentic compact ZIP-style image snapshot from the source
        // under test; then restore the old visible Best Frames references.
        CreatorHeroArchive.import(context, frameArray("after-stop.png", 0xFF22AA55.toInt()))
        mark("before_compact_backup")
        val packed = creator.createPortableBackup()
        mark("compact_backup_complete")
        assertEquals(1, packed.frames.size)
        CreatorHeroArchive.restoreOriginalOrder(context, beforeOrder)
        assertEquals(beforeOrder, CreatorHeroArchive.captureOriginalOrder(context))

        // CI observes this marker with adb run-as and FORCE-STOPS the app.
        // Only a genuine committed image order inside the real restore flow
        // can make this file appear. No normal app caller passes a callback.
        mark("before_real_compact_restore")
        creator.restorePortableCompact(packed.compactJson, packed.frames) {
            mark("inside_callback_after_hero_order_committed")
            assertTrue("Real restore must have published durable recovery journal", pending.isFile)
            assertNotEquals(beforeOrder, CreatorHeroArchive.captureOriginalOrder(context))
            writeAtomic(File(root, "images_committed"), "journal-active")
            Thread.sleep(180_000L) // CI must kill the target before expiry.
            error("CI did not force-stop the active restore after image commit")
        }
        error("The active restore must be terminated by adb force-stop")
    }

    @Test fun recoverRealTransactionInFreshProcess(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.filesDir, fixtureFolder)
        val pending = File(context.filesDir, "$journalFolder/pending.json")
        assertTrue("CI never reached actual image-commit boundary", File(root, "images_committed").isFile)
        val baseline = File(root, "baseline.json").readText()
        val beforeOrder = File(root, "before-order.json").readText()
        val beforeCreator = JSONObject(File(root, "before-creator.json").readText())
        val manager = CreatorBackupManager(context)

        try {
            // Startup might already have performed automatic recovery. Either
            // way the previous order and metadata MUST now be recoverable.
            if (pending.isFile) assertTrue(manager.recoverPendingRestore())
            assertFalse("Real restore recovery must clear pending marker", pending.exists())
            assertEquals(beforeOrder, CreatorHeroArchive.captureOriginalOrder(context))
            assertFalse("Startup recovery may not replay twice", manager.recoverPendingRestore())
            val now = JSONObject(manager.createBackup())
            for (key in listOf("tasks", "ideas", "weeklySchedule", "settings")) {
                assertEquals("Recovered creator section: $key",
                    beforeCreator.getString(key), now.getString(key))
            }
        } finally {
            // This test runs exclusively on an empty, disposable CI emulator.
            // Never delete recovery markers or user files on actual devices.
            pending.delete()
            CreatorDataGate.nonCancellable { manager.restore(baseline) }
            root.deleteRecursively()
        }
    }

    private fun frameArray(name: String, color: Int): String {
        val bitmap = Bitmap.createBitmap(3, 3, Bitmap.Config.ARGB_8888)
        val bytes = try {
            bitmap.eraseColor(color)
            ByteArrayOutputStream().also {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }.toByteArray()
        } finally {
            bitmap.recycle()
        }
        val sha = MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it) }
        return JSONArray().put(JSONObject().put("name", name)
            .put("sha256", sha)
            .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))).toString()
    }

    private fun writeAtomic(file: File, content: String) {
        val atomic = AtomicFile(file)
        val output = atomic.startWrite()
        try {
            output.write(content.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(output)
        } catch (error: Throwable) {
            atomic.failWrite(output)
            throw error
        }
    }
}
