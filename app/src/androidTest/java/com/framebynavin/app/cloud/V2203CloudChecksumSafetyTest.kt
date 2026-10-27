package com.framebynavin.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorBackupManager
import com.framebynavin.app.data.HardeningTestEnvironment
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real current-schema backups on isolated CI emulator. No cloud calls,
 * no production account, and no user data mutation or restore.
 */
@RunWith(AndroidJUnit4::class)
class V2203CloudChecksumSafetyTest {
    @Test fun actualCurrentBackupSurvivesCloudSanitizationAndCanonicalVerification() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val manager = CreatorBackupManager(InstrumentationRegistry.getInstrumentation().targetContext)
        val raw = manager.createBackup()
        val root = JSONObject(raw)
        assertTrue(root.has("automationPreferences"))
        val cloud = CreatorCloudPayloadSanitizer.sanitize(raw, manager)
        assertEquals(CreatorBackupManager.SCHEMA_VERSION, manager.validate(cloud).schemaVersion)
        assertEquals(root.getString("automationPreferences"),
            JSONObject(cloud).getString("automationPreferences"))
    }

    @Test fun voiceIdeaPathIsRemovedWithoutInvalidatingBackupOrPublishingLocalPath() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val manager = CreatorBackupManager(InstrumentationRegistry.getInstrumentation().targetContext)
        val root = JSONObject(manager.createBackup())
        val ideas = JSONArray(root.getString("ideas"))
        ideas.put(JSONObject()
            .put("id", "v2203-voice-synthetic")
            .put("title", "Recorded cinematic idea")
            .put("captureType", "VOICE")
            .put("audioLocalPath", "/data/user/0/private/voice_idea_secret.m4a"))
        root.put("ideas", ideas.toString())
        // Create a fully signed fixture with the actual backup codec; do not
        // touch the owner's Idea Vault or trust fabricated checksum bytes.
        val fixture = manager.resealCloudSanitizedPayload(root.toString())
        manager.validate(fixture)
        val cloud = CreatorCloudPayloadSanitizer.sanitize(fixture, manager)
        val safeIdeas = JSONArray(JSONObject(cloud).getString("ideas"))
        val synthetic = (0 until safeIdeas.length())
            .map { safeIdeas.getJSONObject(it) }
            .first { it.getString("id") == "v2203-voice-synthetic" }
        assertEquals("", synthetic.getString("audioLocalPath"))
        assertFalse(cloud.contains("/data/user/0/private/voice_idea_secret.m4a"))
        assertEquals(manager.validate(fixture).ideaCount, manager.validate(cloud).ideaCount)
    }

    @Test fun tamperedOriginalAndTamperedSanitizedSnapshotFailClosed() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val manager = CreatorBackupManager(InstrumentationRegistry.getInstrumentation().targetContext)
        val valid = manager.createBackup()
        val changed = JSONObject(valid)
            .put("createdAtMillis", JSONObject(valid).getLong("createdAtMillis") + 1L).toString()
        assertThrows(IllegalArgumentException::class.java) {
            CreatorCloudPayloadSanitizer.sanitize(changed, manager)
        }
        val safe = CreatorCloudPayloadSanitizer.sanitize(valid, manager)
        val broken = JSONObject(safe)
            .put("automationPreferences", """{"unexpected":"tamper"}""").toString()
        assertThrows(IllegalArgumentException::class.java) { manager.validate(broken) }
    }
}
