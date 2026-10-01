package com.framebynavin.app.cloud

import com.framebynavin.app.data.CreatorBackupManager
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorCloudPayloadSanitizerTest {
    @Test
    fun voiceIdeaCloudSnapshotDropsDevicePathAndKeepsRemoteReference() {
        val remote = "storage://creator-voice-ideas/12345678-1234-1234-1234-123456789012/idea-1/media/" + "a".repeat(64) + ".m4a"
        val idea = JSONObject()
            .put("id", "idea-1")
            .put("title", "Voice thought")
            .put("captureType", "VOICE")
            .put("audioLocalPath", "/data/user/0/com.framebynavin.app/files/voice_ideas/voice_idea_1.m4a")
            .put("audioRemoteUrl", remote)

        val root = JSONObject()
            .put("format", "FrameByNavinBackup")
            .put("schemaVersion", CreatorBackupManager.SCHEMA_VERSION)
            .put("createdAtMillis", 1L)
            .put("tasks", "[]")
            .put("ideas", JSONArray().put(idea).toString())
            .put("weeklySchedule", "[]")
            .put("settings", "{}")
            .put("smartEscalationConfig", "{}")
            .put("postPublish", "[]")
            .put("rewards", "[]")
            .put("personalFrames", "{}")
            .put("youtubeProjectLinks", "{}")
            .put("youtubeMilestones", "{}")
            .put("youtubePublishCheckpoints", "[]")
            .put("projectPulseHistory", "[]")
            .put("workflowStageTimeline", "[]")
            .put("manifest", "{}")
            .put("payloadSha256", "0".repeat(64))

        val sanitized = JSONObject(CreatorCloudPayloadSanitizer.sanitize(root.toString()))
        val restoredIdea = JSONArray(sanitized.getString("ideas")).getJSONObject(0)

        assertEquals("", restoredIdea.getString("audioLocalPath"))
        assertEquals(remote, restoredIdea.getString("audioRemoteUrl"))
        assertTrue(sanitized.getString("payloadSha256").matches(Regex("[0-9a-f]{64}")))
    }
}
