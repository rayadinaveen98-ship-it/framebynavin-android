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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Disposable emulator ONLY. The CI script runs prepare(), force-stops the
 * target package with adb, then runs recover() in a NEW instrumentation process.
 * Never select the entire class in one instrumentation invocation: process
 * separation and ordered execution are explicit CI acceptance requirements.
 */
@RunWith(AndroidJUnit4::class)
class V210ProcessBoundaryRecoveryTest {
    private val journalFolder = "creator_restore_recovery_v181"
    private val fixtureFolder = "v210_process_boundary_recovery_qa"

    @Test fun preparePendingRecoveryAcrossProcessBoundary(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = File(context.filesDir, fixtureFolder)
        val journalRoot = File(context.filesDir, journalFolder).apply { mkdirs() }
        val pending = File(journalRoot, "pending.json")
        val manager = CreatorBackupManager(context)

        // We must not overwrite evidence from a failed prior phase.
        assertFalse("No previous process-boundary test fixture is allowed", fixture.exists())
        assertFalse("Pending recovery must be clear at test start", pending.exists())
        check(fixture.mkdir()) { "Could not prepare isolated emulator recovery fixture" }
        val baselineFile = File(fixture, "baseline.json")
        val orderFile = File(fixture, "expected_original_frames.json")
        val recoveryNameFile = File(fixture, "journal_file_name.txt")

        // Retain a complete original emulator snapshot for phase-two cleanup.
        writeAtomic(baselineFile, manager.createBackup())
        val bytes = tinyPng()
        val checksum = sha256(bytes)
        val original = JSONArray().put(JSONObject()
            .put("name", "before-process-stop.png")
            .put("sha256", checksum)
            .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)))
        CreatorHeroArchive.import(context, original.toString())
        val order = CreatorHeroArchive.captureOriginalOrder(context)
        assertEquals(1, JSONArray(order).length())
        writeAtomic(orderFile, order)

        // Exactly the small metadata journal + checksummed old-file references
        // produced by V210 restorePortableCompact, not a giant Base64 journal.
        val metadata = manager.rewritePortablePersonalFrames(manager.createBackup(), "[]")
        val recovery = File(journalRoot, "pre-restore-${UUID.randomUUID()}.fbnbackup")
        writeAtomic(recovery, metadata)
        writeAtomic(recoveryNameFile, recovery.name)
        val marker = JSONObject()
            .put("file", recovery.name)
            .put("sha256", sha256(metadata.toByteArray(Charsets.UTF_8)))
            .put("originalFrames", order)
            .put("originalFramesSha256", sha256(order.toByteArray(Charsets.UTF_8)))
        writeAtomic(pending, marker.toString())

        // Simulates state after publishing the new order, before clearing pending.
        // CI force-stops com.framebynavin.app AFTER this test exits.
        CreatorHeroArchive.import(context, "[]")
        assertEquals("[]", CreatorHeroArchive.captureOriginalOrder(context))
        assertTrue("Pending marker must survive next-process restart", pending.isFile)
        assertTrue("Original media must survive order switch", orderFile.isFile)
    }

    @Test fun recoverPendingAfterForceStopInFreshProcess(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = File(context.filesDir, fixtureFolder)
        val root = File(context.filesDir, journalFolder)
        val pending = File(root, "pending.json")
        val manager = CreatorBackupManager(context)
        assertTrue("Phase one fixture missing; CI did not run prepare first", fixture.isDirectory)
        val baseline = File(fixture, "baseline.json")
        val expected = File(fixture, "expected_original_frames.json").readText()
        val recoveryName = File(fixture, "journal_file_name.txt").readText()
        val recovery = File(root, recoveryName)
        try {
            // If startup recovery runs automatically before instrumentation,
            // it may have cleared pending already. In both cases original order
            // MUST be valid on this fresh target-app process.
            if (pending.isFile) {
                assertTrue("Pending journal must survive adb force-stop", recovery.isFile)
                assertTrue(manager.recoverPendingRestore())
            }
            assertEquals(expected, CreatorHeroArchive.captureOriginalOrder(context))
            val checkpoint = JSONArray(CreatorHeroArchive.captureOriginalOrder(context))
            assertEquals(1, checkpoint.length())
            assertFalse("Recovery must clear pending marker", pending.exists())
            assertFalse("Recovery must be idempotent", manager.recoverPendingRestore())
        } finally {
            // This cleanup is confined to an empty disposable CI emulator.
            // Never drop the pending marker on a real user's device.
            pending.delete()
            CreatorDataGate.nonCancellable { manager.restore(baseline.readText()) }
            recovery.delete()
            fixture.deleteRecursively()
        }
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
