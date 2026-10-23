package com.framebynavin.app.data

import android.graphics.Bitmap
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Disposable Android 11 emulator acceptance. Simulate an export destination
 * becoming unwritable; never fill actual device storage or change app code.
 */
@RunWith(AndroidJUnit4::class)
class V210BackupIoFailureTest {
    @Test fun interruptedPortableExportDoesNotAlterCreatorState(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val core = CreatorBackupManager(context)
        val portable = PortableCreatorBackupManager(context)
        val baseline = core.createBackup()
        val pending = File(context.filesDir, "creator_restore_recovery_v181/pending.json")
        assertFalse("Emulator must have no pending restore at start", pending.exists())
        try {
            CreatorHeroArchive.import(context, frame("simulated-storage-full.png"))
            val previousOrder = CreatorHeroArchive.captureOriginalOrder(context)
            val before = JSONObject(core.createBackup())
            val sink = FailAfterBytesOutputStream(64)
            val exportError = runCatching { portable.writeBackup(sink) }.exceptionOrNull()

            assertNotNull("A short-write destination must make ZIP export fail", exportError)
            assertTrue("Failure must be an I/O error, not a silently successful export",
                exportError is IOException)
            assertEquals("No partial backup may be reported as successful", 64, sink.bytesWritten)
            assertEquals(previousOrder, CreatorHeroArchive.captureOriginalOrder(context))
            val after = JSONObject(core.createBackup())
            for (key in listOf("tasks", "ideas", "weeklySchedule", "settings",
                "personalFrames", "postPublish", "rewards")) {
                assertEquals("Failed export modified creator section $key",
                    before.getString(key), after.getString(key))
            }
            assertFalse("Failed export must not publish a restore marker", pending.exists())
        } finally {
            CreatorDataGate.nonCancellable { core.restore(baseline) }
        }
    }

    @Test fun truncatedPortableZipFailsBeforeChangingCreatorData(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val core = CreatorBackupManager(context)
        val portable = PortableCreatorBackupManager(context)
        val baseline = core.createBackup()
        val pending = File(context.filesDir, "creator_restore_recovery_v181/pending.json")
        assertFalse("Emulator must have no pending restore at start", pending.exists())
        try {
            CreatorHeroArchive.import(context, frame("intact-source.png"))
            val previousOrder = CreatorHeroArchive.captureOriginalOrder(context)
            val before = JSONObject(core.createBackup())
            val output = ByteArrayOutputStream()
            portable.writeBackup(output)
            val complete = output.toByteArray()
            assertTrue("Portable archive fixture must include a local ZIP header",
                complete.size > 64 && complete[0] == 0x50.toByte() &&
                    complete[1] == 0x4b.toByte())
            // Deliberately cut through the first local ZIP header. This is a
            // broken .fbnbackup, not a valid archive with media simply omitted.
            val broken = complete.copyOf(16)
            val validationError = runCatching {
                portable.validate(ByteArrayInputStream(broken))
            }.exceptionOrNull()
            assertNotNull("Incomplete ZIP must not receive a usable restore preview",
                validationError)
            val recoveryError = runCatching {
                portable.restore(ByteArrayInputStream(broken))
            }.exceptionOrNull()
            assertNotNull("Incomplete ZIP must never return a successful restore",
                recoveryError)
            assertEquals(previousOrder, CreatorHeroArchive.captureOriginalOrder(context))
            val after = JSONObject(core.createBackup())
            for (key in listOf("tasks", "ideas", "weeklySchedule", "settings",
                "personalFrames", "postPublish", "rewards")) {
                assertEquals("Truncated restore changed creator section $key",
                    before.getString(key), after.getString(key))
            }
            assertFalse("Rejected archive must not publish a recovery marker", pending.exists())
        } finally {
            CreatorDataGate.nonCancellable { core.restore(baseline) }
        }
    }

    private fun frame(name: String): String {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val bytes = try {
            ByteArrayOutputStream().also {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }.toByteArray()
        } finally {
            bitmap.recycle()
        }
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        return JSONArray().put(JSONObject()
            .put("name", name)
            .put("sha256", sha)
            .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))).toString()
    }

    /** Deterministic ENOSPC-style destination, not an actual filesystem fill. */
    private class FailAfterBytesOutputStream(private val limit: Int) : OutputStream() {
        var bytesWritten: Int = 0
            private set
        override fun write(b: Int) {
            if (bytesWritten == limit) throw IOException("Simulated destination storage full")
            bytesWritten++
        }
        override fun write(buffer: ByteArray, off: Int, len: Int) {
            require(off >= 0 && len >= 0 && off + len <= buffer.size)
            for (i in off until off + len) write(buffer[i].toInt())
        }
    }
}
