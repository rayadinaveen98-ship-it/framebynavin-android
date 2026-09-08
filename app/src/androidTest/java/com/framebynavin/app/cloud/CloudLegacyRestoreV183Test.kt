package com.framebynavin.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorBackupManager
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.HardeningTestEnvironment
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CloudLegacyRestoreV183Test {
    private val legacyKeys = listOf("format", "schemaVersion", "createdAtMillis", "tasks", "ideas",
        "weeklySchedule", "settings", "smartEscalationConfig", "postPublish", "rewards", "personalFrames")

    private fun resign(root: JSONObject): String {
        val schema = root.getInt("schemaVersion")
        val keys = if (schema >= 4) legacyKeys + listOf("youtubeProjectLinks", "youtubeMilestones") else legacyKeys
        val fingerprint = buildString {
            keys.forEach { key ->
                val value = root.optString(key, "")
                append(key.length).append(':').append(key).append(value.length).append(':').append(value)
            }
        }
        val sha = MessageDigest.getInstance("SHA-256").digest(fingerprint.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return root.put("payloadSha256", sha).toString()
    }

    private fun envelope(localBackup: String, links: String? = null, milestones: String? = null): String =
        JSONObject().put("format", CloudConfig.CLOUD_FORMAT)
            .put("schemaVersion", CloudConfig.CLOUD_SCHEMA_VERSION)
            .put("localBackup", localBackup)
            .apply {
                if (links != null) put("youtubeProjectLinks", links)
                if (milestones != null) put("youtubeMilestones", milestones)
            }.toString()

    private suspend fun restoreEnvelope(manager: CreatorBackupManager, raw: String) {
        val parsed = CloudBackupEnvelope.parse(raw)
        manager.restore(manager.attachLegacyYoutubeData(parsed.localBackup,
            parsed.youtubeProjectLinks, parsed.youtubeMilestones))
    }

    @Test fun omittedLegacyMetadataPreservesLocalValues(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = CreatorBackupManager(context)
        val original = manager.createBackup()
        val links = context.getSharedPreferences("youtube_analytics_v11", 0)
        val milestones = context.getSharedPreferences("youtube_milestones_v12", 0)
        val id = "legacy-cloud-${UUID.randomUUID()}"
        try {
            val localLinks = JSONObject().put(id, "local-link").toString()
            assertTrue(links.edit().putString("video_project_links", localLinks).commit())
            assertTrue(milestones.edit().putString(id, "local-milestone").commit())
            val current = manager.createBackup()
            val root = JSONObject(current).put("schemaVersion", 3)
            root.remove("manifest")
            listOf("personalFrames", "youtubeProjectLinks", "youtubeMilestones", "postPublish",
                "rewards", "smartEscalationConfig").forEach(root::remove)
            val legacy = resign(root)
            val parsed = CloudBackupEnvelope.parse(envelope(legacy))
            assertNull(parsed.youtubeProjectLinks)
            assertNull(parsed.youtubeMilestones)
            restoreEnvelope(manager, envelope(legacy))
            assertEquals(localLinks, links.getString("video_project_links", ""))
            assertEquals("local-milestone", milestones.getString(id, ""))
            // An explicitly present empty object is a deliberate replacement, not an omission.
            restoreEnvelope(manager, envelope(legacy, "{}", "{}"))
            assertEquals("{}", links.getString("video_project_links", ""))
            assertFalse(milestones.contains(id))
        } finally {
            CreatorDataGate.nonCancellable { manager.restore(original) }
        }
    }

    @Test fun currentSchemaMetadataIsAuthoritative(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = CreatorBackupManager(context)
        val original = manager.createBackup()
        val links = context.getSharedPreferences("youtube_analytics_v11", 0)
        try {
            val inner = JSONObject(original).put("schemaVersion", 4)
            inner.remove("manifest")
            inner.put("youtubeProjectLinks", "{\"inner\":\"kept\"}")
            val legacy4 = resign(inner)
            val parsed = CloudBackupEnvelope.parse(envelope(legacy4, "{}", "{}"))
            val attached = manager.attachLegacyYoutubeData(parsed.localBackup,
                parsed.youtubeProjectLinks, parsed.youtubeMilestones)
            assertEquals(legacy4, attached)
            manager.restore(attached)
            assertEquals("kept", JSONObject(links.getString("video_project_links", "{}")!!).getString("inner"))
        } finally {
            CreatorDataGate.nonCancellable { manager.restore(original) }
        }
    }

    @Test fun malformedIncludedMetadataAndTamperingFailBeforeMutation(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = CreatorBackupManager(context)
        val original = manager.createBackup()
        val links = context.getSharedPreferences("youtube_analytics_v11", 0)
        val before = links.getString("video_project_links", "{}")
        try {
            assertThrows(Exception::class.java) {
                CloudBackupEnvelope.parse(envelope(original, "not-json"))
            }
            assertThrows(Exception::class.java) {
                CloudBackupEnvelope.parse(JSONObject(envelope(original))
                    .put("youtubeProjectLinks", JSONObject.NULL).toString())
            }
            assertThrows(Exception::class.java) {
                CloudBackupEnvelope.parse(envelope(original, "[]"))
            }
            val tampered = JSONObject(original).put("createdAtMillis", -1L).toString()
            val parsed = CloudBackupEnvelope.parse(envelope(tampered))
            assertThrows(IllegalArgumentException::class.java) {
                manager.attachLegacyYoutubeData(parsed.localBackup,
                    parsed.youtubeProjectLinks, parsed.youtubeMilestones)
            }
            assertEquals(before, links.getString("video_project_links", "{}"))
        } finally {
            CreatorDataGate.nonCancellable { manager.restore(original) }
        }
    }
}
