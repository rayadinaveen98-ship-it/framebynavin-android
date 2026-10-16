package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V160QuickActionsSafetyInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun intentionallyEmptyWeeklyScheduleSurvivesReloadAndExport(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = WeeklyScheduleStore(context)
        val before = store.exportJson()
        try {
            store.save(emptyList())
            assertTrue(store.loadOrSeed().isEmpty())
            assertEquals("[]", store.exportJson())
            store.importJson("[]")
            assertTrue(store.loadOrSeed().isEmpty())
        } finally {
            store.importJson(before)
        }
    }

    @Test fun automationRoutineSettingsRoundTripThroughSignedBackup(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val manager = CreatorBackupManager(context)
        val prefs = CreatorAutomationPreferencesStore(context)
        val original = manager.createBackup()
        try {
            prefs.setDailyBriefRoutineEnabled(true)
            prefs.setIdeaReviewRoutineEnabled(true)
            prefs.setWeeklyReviewRoutineEnabled(false)
            val saved = manager.createBackup()
            val root = JSONObject(saved)
            assertEquals(9, root.getInt("schemaVersion"))
            assertTrue(root.getJSONObject("manifest").getJSONArray("included").toString().contains("automationPreferences"))
            val embedded = prefs.validateJson(root.getString("automationPreferences"))
            assertTrue(embedded.dailyBriefRoutineEnabled)
            assertTrue(embedded.ideaReviewRoutineEnabled)
            assertFalse(embedded.weeklyReviewRoutineEnabled)

            prefs.setDailyBriefRoutineEnabled(false)
            prefs.setIdeaReviewRoutineEnabled(false)
            prefs.setWeeklyReviewRoutineEnabled(true)
            manager.restore(saved)
            assertEquals(embedded, prefs.snapshot())
        } finally {
            CreatorDataGate.nonCancellable { manager.restore(original) }
        }
    }

    @Test fun schemaEightBackupStillValidAndDoesNotEraseNewerRoutineChoices(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val manager = CreatorBackupManager(context)
        val prefs = CreatorAutomationPreferencesStore(context)
        val original = manager.createBackup()
        try {
            val old = legacySchemaEight(original)
            assertEquals(8, manager.validate(old).schemaVersion)
            prefs.setDailyBriefRoutineEnabled(true)
            prefs.setWeeklyReviewRoutineEnabled(true)
            val expected = prefs.snapshot()
            manager.restore(old)
            assertEquals(expected, prefs.snapshot())
        } finally {
            CreatorDataGate.nonCancellable { manager.restore(original) }
        }
    }

    @Test fun portableVoiceBackupRestoresOriginalBytesAndSignedLegacyV8Archive(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val manager = CreatorBackupManager(context)
        val portable = PortableCreatorBackupManager(context)
        val original = manager.createBackup()
        val voiceFolder = File(context.filesDir, VoiceIdeaStorage.DIRECTORY_NAME).apply { mkdirs() }
        val voiceFile = File(voiceFolder, "voice_idea_v160-${UUID.randomUUID()}.m4a")
        val ideaId = "v160-voice-${UUID.randomUUID()}"
        val bytes = byteArrayOf(4, 0, 2, 6, 10, 16, 23, 32)
        var installed: File? = null
        try {
            voiceFile.writeBytes(bytes)
            val idea = CreatorIdea(
                id = ideaId, title = "Portable original voice thought",
                captureType = IdeaCaptureType.VOICE, audioLocalPath = voiceFile.absolutePath,
                audioMimeType = "audio/mp4",
            )
            IdeaVaultStore(context).mutate { listOf(idea) + it }
            val export = ByteArrayOutputStream()
            portable.writeBackup(export)
            assertTrue(export.size() > bytes.size)
            val staged = PortableBackupArchive.stage(
                ByteArrayInputStream(export.toByteArray()), context.cacheDir,
            )
            try {
                assertTrue(staged.isArchive)
                assertArrayEquals(bytes, staged.mediaByIdeaId.getValue(ideaId).readBytes())
            } finally {
                staged.cleanup()
            }

            // Re-sign an actual v8 portable archive, preserving voice bytes, to
            // prove schema v9 does not block old user's media restores.
            val savedJson = manager.createBackup()
            val oldArchive = ByteArrayOutputStream()
            PortableBackupArchive.write(
                legacySchemaEight(savedJson),
                listOf(PortableBackupMedia(ideaId, voiceFile)), oldArchive,
            )
            voiceFile.delete()
            portable.restore(ByteArrayInputStream(oldArchive.toByteArray()))
            val restored = IdeaVaultStore(context).load().first { it.id == ideaId }
            installed = File(restored.audioLocalPath)
            assertTrue(installed.isFile)
            assertArrayEquals(bytes, installed.readBytes())
            assertTrue(installed.absolutePath != voiceFile.absolutePath)
        } finally {
            CreatorDataGate.nonCancellable { manager.restore(original) }
            voiceFile.delete()
            installed?.delete()
        }
    }

    @Test fun missingVoiceFileBlocksSuccessfulExportInsteadOfOmittingRecording(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val manager = CreatorBackupManager(context)
        val original = manager.createBackup()
        val portable = PortableCreatorBackupManager(context)
        try {
            val id = "v160-missing-${UUID.randomUUID()}"
            val missing = File(File(context.filesDir, "voice_ideas"), "voice_idea_missing-${UUID.randomUUID()}.m4a")
            val idea = CreatorIdea(
                id = id, title = "Never silently omit voice bytes",
                captureType = IdeaCaptureType.VOICE, audioLocalPath = missing.absolutePath,
            )
            IdeaVaultStore(context).mutate { listOf(idea) + it }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { portable.writeBackup(ByteArrayOutputStream()) }
            }
        } finally {
            CreatorDataGate.nonCancellable { manager.restore(original) }
        }
    }

    private fun legacySchemaEight(raw: String): String {
        val root = JSONObject(raw).put("schemaVersion", 8).apply { remove("automationPreferences") }
        val manifest = root.getJSONObject("manifest")
        val current = manifest.getJSONArray("included")
        val old = JSONArray()
        for (i in 0 until current.length()) {
            val key = current.getString(i)
            if (key != "automationPreferences") old.put(key)
        }
        manifest.put("included", old)
        root.put("payloadSha256", sha256(buildString {
            val keys = listOf(
                "format", "schemaVersion", "createdAtMillis", "tasks", "ideas",
                "weeklySchedule", "settings", "smartEscalationConfig", "postPublish", "rewards",
                "personalFrames", "youtubeProjectLinks", "youtubeMilestones",
                "youtubePublishCheckpoints", "projectPulseHistory", "workflowStageTimeline", "manifest",
            )
            keys.forEach { key ->
                val value = root.optString(key, "")
                append(key.length).append(':').append(key).append(value.length).append(':').append(value)
            }
        }))
        return root.toString()
    }

    private fun sha256(raw: String): String = MessageDigest.getInstance("SHA-256")
        .digest(raw.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
