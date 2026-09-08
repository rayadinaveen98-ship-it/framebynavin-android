package com.framebynavin.app.data

import android.graphics.Bitmap
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.reminders.AlarmLedger
import com.framebynavin.app.reminders.ReminderOccurrenceStore
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CreatorBackupV183Test {
    private val legacyKeys = listOf("format", "schemaVersion", "createdAtMillis", "tasks", "ideas",
        "weeklySchedule", "settings", "smartEscalationConfig", "postPublish", "rewards", "personalFrames")

    private fun sha(raw: String): String = sha(raw.toByteArray(Charsets.UTF_8))

    private fun sha(raw: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(raw).joinToString("") { "%02x".format(it) }

    private fun checksum(root: JSONObject, schema: Int): String = sha(buildString {
        val keys = if (schema >= 4) legacyKeys + listOf("youtubeProjectLinks", "youtubeMilestones") else legacyKeys
        (if (schema >= 5) keys + "manifest" else keys).forEach { key ->
            val value = root.optString(key, "")
            append(key.length).append(':').append(key).append(value.length).append(':').append(value)
        }
    })

    private fun resign(root: JSONObject): String {
        val schema = root.getInt("schemaVersion")
        root.put("payloadSha256", checksum(root, schema))
        return root.toString()
    }

    @Test fun currentManifestIsCompleteAndTamperingFailsClosed(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = CreatorBackupManager(context)
        val raw = manager.createBackup()
        val root = JSONObject(raw)
        assertEquals(5, manager.validate(raw).schemaVersion)
        val manifest = root.getJSONObject("manifest")
        assertFalse(manifest.getBoolean("authenticated"))
        assertTrue(manifest.getJSONArray("excluded").toString().contains("oauthTokens"))
        assertTrue(manifest.getJSONArray("excluded").toString().contains("reminderOccurrenceTokens"))
        assertTrue(root.has("personalFrames"))
        assertTrue(root.has("youtubeProjectLinks"))
        assertTrue(root.has("youtubeMilestones"))
        assertThrows(IllegalArgumentException::class.java) {
            manager.validate(JSONObject(raw).put("createdAtMillis", root.getLong("createdAtMillis") + 1L).toString())
        }
        assertThrows(IllegalArgumentException::class.java) {
            manager.validate(resign(JSONObject(raw).removeSection("personalFrames")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            manager.validate(resign(JSONObject(raw).put("unknownFutureData", "not safe to discard")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            manager.validate(resign(JSONObject(raw).put("manifest", JSONObject(manifest.toString())
                .put("authenticated", true))))
        }
    }

    private fun JSONObject.removeSection(key: String): JSONObject = apply { remove(key) }

    @Test fun legacyRestorePreservesMissingSectionsAndInvalidatesOrphanedReminders() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = CreatorBackupManager(context)
        val original = manager.createBackup()
        val links = context.getSharedPreferences("youtube_analytics_v11", 0)
        val milestones = context.getSharedPreferences("youtube_milestones_v12", 0)
        val id = "backup-v183-${UUID.randomUUID()}"
        try {
            val image = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            val bytes = ByteArrayOutputStream().use { stream ->
                image.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.toByteArray()
            }
            image.recycle()
            val frame = JSONObject().put("name", "test.png").put("sha256", sha(bytes))
                .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
            CreatorHeroArchive.import(context, JSONArray().put(frame).toString())
            val framesBefore = CreatorHeroArchive.export(context)
            val linksBefore = "{\"$id\":\"original-link\"}"
            val milestonesBefore = "{\"$id\":\"original-milestone\"}"
            assertTrue(links.edit().putString("video_project_links", linksBefore).commit())
            assertTrue(milestones.edit().putString(id, "original-milestone").commit())
            val task = CreatorTask(id=id, title="Restore me", platform="YouTube", contentType="Long-form",
                dueLabel="Today", reminderEnabled=true, reminderMode=ReminderMode.SIMPLE,
                reminderAtMillis=System.currentTimeMillis()+3_600_000L)
            TaskStore(context).mutate { it + task }
            val current = manager.createBackup()
            val root = JSONObject(current).put("schemaVersion", 3).removeSection("manifest")
            listOf("personalFrames", "youtubeProjectLinks", "youtubeMilestones", "postPublish",
                "rewards", "smartEscalationConfig").forEach(root::remove)
            val legacy = resign(root)
            assertEquals(3, manager.validate(legacy).schemaVersion)
            val attached = manager.attachLegacyYoutubeData(legacy, "{\"remote\":\"link\"}", "{}")
            assertEquals(3, JSONObject(attached).getInt("schemaVersion"))
            assertTrue(JSONObject(attached).has("youtubeProjectLinks"))
            assertFalse(JSONObject(attached).has("personalFrames"))
            val occurrences = ReminderOccurrenceStore(context)
            val token = occurrences.issue(task)
            val orphan = "orphan-${UUID.randomUUID()}"
            val orphanTask = task.copy(id=orphan)
            val orphanToken = occurrences.issue(orphanTask)
            val ledger = AlarmLedger(context)
            ledger.markScheduled(orphan, task.reminderAtMillis)
            ledger.markDelivered(orphan, task.reminderAtMillis)
            TaskStore(context).updateTask(id) { it.copy(title="Changed") }
            manager.restore(legacy)
            assertEquals("Restore me", TaskStore(context).load().first { it.id==id }.title)
            assertFalse(occurrences.matches(task, token))
            assertFalse(occurrences.matches(orphanTask, orphanToken))
            assertTrue(occurrences.taskIds().isEmpty())
            assertNull(ledger.scheduledAt(orphan))
            assertFalse(ledger.wasDelivered(orphan, task.reminderAtMillis))
            assertEquals(framesBefore, CreatorHeroArchive.export(context))
            assertEquals(linksBefore, links.getString("video_project_links", ""))
            assertEquals(JSONObject(milestonesBefore).getString(id), milestones.getString(id, ""))
            val after = JSONObject(manager.createBackup())
            assertEquals(JSONObject(current).getString("postPublish"), after.getString("postPublish"))
            assertEquals(JSONObject(current).getString("rewards"), after.getString("rewards"))
            assertEquals(JSONObject(current).getString("smartEscalationConfig"), after.getString("smartEscalationConfig"))
            // Explicit empty fields in a complete snapshot are replacement instructions.
            val emptyLinks = resign(JSONObject(current).put("youtubeProjectLinks", "{}"))
            manager.restore(emptyLinks)
            assertEquals("{}", links.getString("video_project_links", ""))
        } finally {
            CreatorDataGate.nonCancellable { manager.restore(original) }
        }
    }

    @Test fun pendingJournalRecoversOriginalAfterInterruptedRestore() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = CreatorBackupManager(context)
        val original = manager.createBackup()
        val id = "journal-v183-${UUID.randomUUID()}"
        val directory = File(context.filesDir, "creator_restore_recovery_v181").apply { mkdirs() }
        val recovery = File(directory, "pre-restore-${UUID.randomUUID()}.fbnbackup")
        val pending = File(directory, "pending.json")
        try {
            val task = CreatorTask(id=id, title="Before interruption", platform="YouTube",
                contentType="Long-form", dueLabel="Today")
            TaskStore(context).mutate { it + task }
            val before = manager.createBackup()
            val generation = CreatorDataGate.generation(context)
            recovery.writeText(before)
            pending.writeText(JSONObject().put("file", recovery.name).put("sha256", sha(before)).toString())
            TaskStore(context).updateTask(id) { it.copy(title="Partial import") }
            assertTrue(manager.recoverPendingRestore())
            assertEquals("Before interruption", TaskStore(context).load().first { it.id==id }.title)
            assertTrue(CreatorDataGate.generation(context) > generation)
            assertFalse(pending.exists())
        } finally {
            CreatorDataGate.nonCancellable { manager.restore(original) }
            pending.delete()
            recovery.delete()
        }
    }
}
