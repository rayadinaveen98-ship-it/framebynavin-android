package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

/** Android proof that a Focus receipt survives a real Creator archive restore. */
@RunWith(AndroidJUnit4::class)
class CreatorFocusBackupV10Test {
    @Test fun creatorBackupV10IncludesVerifiedFocusHistoryAndRestoresIt(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = CreatorFocusSessionStore(context)
        val before = store.exportJson()
        try {
            val created = CreatorFocusSessionPolicy.create(
                projectId = "focus-backup-test",
                stageId = "script",
                targetMinutes = 25,
                atWallMillis = 1_700_000_000_000L,
                id = "focus-backup-v10",
            )
            val started = CreatorFocusSessionPolicy.start(created, 1000L, 2)
            val finished = CreatorFocusSessionPolicy.finish(started, 61_000L, 2, 1_700_000_061_000L)
            store.importAfterApprovedRestore(
                CreatorFocusSessionCodec.encode(CreatorFocusLedger(history = listOf(finished)))
            )
            val manager = CreatorBackupManager(context)
            val archived = manager.createBackup()
            val root = JSONObject(archived)
            assertEquals(10, manager.validate(archived).schemaVersion)
            assertTrue(root.has("creatorFocusSessions"))
            assertTrue(root.getJSONObject("manifest").getJSONArray("included")
                .toString().contains("creatorFocusSessions"))
            assertEquals(60_000L,
                CreatorFocusSessionCodec.decode(root.getString("creatorFocusSessions"))
                    .history.single().accruedActiveMillis)

            store.importAfterApprovedRestore(CreatorFocusSessionCodec.encode(CreatorFocusLedger()))
            assertTrue(store.load().history.isEmpty())
            manager.restore(archived)
            assertEquals("focus-backup-test", store.load().history.single().projectId)
            assertEquals(60_000L, store.load().history.single().accruedActiveMillis)

            // Corruption is rejected before any restore changes app data.
            val tampered = JSONObject(archived).put("creatorFocusSessions",
                CreatorFocusSessionCodec.encode(CreatorFocusLedger())).toString()
            assertThrows(IllegalArgumentException::class.java) { manager.validate(tampered) }
            assertEquals(1, store.load().history.size)
        } finally {
            store.importAfterApprovedRestore(before)
        }
    }

    @Test fun oldV9CloudBackupRemainsReadableButCannotSilentlyOmitExistingFocusHistory(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = CreatorFocusSessionStore(context)
        val before = store.exportJson()
        try {
            store.importAfterApprovedRestore(CreatorFocusSessionCodec.encode(CreatorFocusLedger()))
            val manager = CreatorBackupManager(context)
            val current = JSONObject(manager.createBackup())
            current.put("schemaVersion", 9)
            current.remove("creatorFocusSessions")
            current.put("personalFrames",
                """{"format":"BacklotCloudOriginalFramesOmitted","version":1}""")
            val manifest = current.getJSONObject("manifest")
            val included = manifest.getJSONArray("included")
            val olderKeys = org.json.JSONArray()
            for (i in 0 until included.length()) {
                val key = included.getString(i)
                if (key != "creatorFocusSessions") olderKeys.put(key)
            }
            manifest.put("included", olderKeys)
            current.put("payloadSha256", v9Checksum(current))
            val legacy = current.toString()
            assertEquals(9, manager.validateCloud(legacy).schemaVersion)
            assertThrows(IllegalArgumentException::class.java) { manager.validate(legacy) }

            // Existing V322 cloud backups must remain readable without new Focus data.
            manager.restoreCloud(legacy)
            assertTrue(store.load().history.isEmpty())

            val old = CreatorFocusSessionPolicy.create(
                "focus-legacy-loss-guard", "research", 15,
                1_700_000_000_000L, id = "legacy-guard")
            val completed = CreatorFocusSessionPolicy.finish(old, 0L, 0, 1_700_000_010_000L)
            store.importAfterApprovedRestore(CreatorFocusSessionCodec.encode(
                CreatorFocusLedger(history = listOf(completed))))
            try {
                manager.restoreCloud(legacy)
                org.junit.Assert.fail("Older cloud backup silently omitted existing Focus history")
            } catch (_: IllegalStateException) { }
            assertEquals("legacy-guard", store.load().history.single().id)
        } finally {
            store.importAfterApprovedRestore(before)
        }
    }

    private fun v9Checksum(root: JSONObject): String {
        val keys = listOf("format", "schemaVersion", "createdAtMillis", "tasks", "ideas",
            "weeklySchedule", "settings", "smartEscalationConfig", "postPublish", "rewards",
            "personalFrames", "youtubeProjectLinks", "youtubeMilestones",
            "youtubePublishCheckpoints", "projectPulseHistory", "workflowStageTimeline",
            "automationPreferences", "manifest")
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        keys.forEach { key ->
            val value = root.optString(key, "")
            digest.update("${key.length}:$key${value.length}:".toByteArray(Charsets.UTF_8))
            digest.update(value.toByteArray(Charsets.UTF_8))
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Test fun portableFocusArchiveNeverCarriesActiveDeviceClockAcrossBackup(): Unit {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = CreatorFocusSessionStore(context)
        val before = store.exportJson()
        try {
            val created = CreatorFocusSessionPolicy.create(
                projectId = "focus-boot-anchor", stageId = "research",
                targetMinutes = 15, atWallMillis = 1_700_000_000_000L,
                id = "focus-recovery-v10"
            )
            store.importAfterApprovedRestore(CreatorFocusSessionCodec.encode(CreatorFocusLedger()))
            store.create(created)
            store.update(created.id) { CreatorFocusSessionPolicy.start(it, 1_000L, 4) }
            store.update(created.id) { CreatorFocusSessionPolicy.checkpoint(it, 21_000L, 4) }
            val backup = CreatorFocusSessionCodec.decode(store.exportPortableJson())
            assertEquals(CreatorFocusState.PAUSED, backup.active?.state)
            assertEquals(-1L, backup.active?.segmentStartedElapsedMillis)
            assertEquals(20_000L, backup.active?.accruedActiveMillis)
            assertTrue(backup.active?.recoveredAfterReboot == true)

            // A portable export must NEVER pause a running local session.
            assertEquals(CreatorFocusState.RUNNING, store.load().active?.state)
            assertFalse(store.load().active?.recoveredAfterReboot == true)
        } finally {
            store.importAfterApprovedRestore(before)
        }
    }
}
