package com.framebynavin.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.UUID

/**
 * Portable file facade around CreatorBackupManager.
 *
 * CreatorBackupManager remains responsible for schema validation, restore journaling and rollback.
 * This layer only packages/restores app-owned Voice Idea recordings and rewrites their device-local
 * paths after the media has been verified on the destination device.
 */
class PortableCreatorBackupManager(context: Context) {
    private val appContext = context.applicationContext
    private val creatorBackup = CreatorBackupManager(appContext)

    suspend fun writeBackup(output: OutputStream) {
        val raw = creatorBackup.createBackup()
        PortableBackupArchive.write(raw, exportableVoiceMedia(raw), output)
    }

    fun validate(input: InputStream): CreatorBackupManager.BackupPreview =
        creatorBackup.validate(PortableBackupArchive.readBackupJson(input))

    suspend fun restore(input: InputStream): CreatorBackupManager.BackupPreview {
        val staged = PortableBackupArchive.stage(input, appContext.cacheDir)
        try {
            val original = staged.backupJson
            creatorBackup.validate(original)
            if (!staged.isArchive) return creatorBackup.restore(original)

            val installed = installVoiceMedia(original, staged.mediaByIdeaId)
            return try {
                creatorBackup.restore(rewritePortableVoicePaths(original, installed))
            } catch (error: Throwable) {
                installed.values.forEach { runCatching { it.delete() } }
                throw error
            }
        } finally {
            staged.cleanup()
        }
    }

    private fun exportableVoiceMedia(raw: String): List<PortableBackupMedia> {
        val ideas = JSONArray(JSONObject(raw).getString("ideas"))
        val result = mutableListOf<PortableBackupMedia>()
        val seenIdeaIds = mutableSetOf<String>()
        for (index in 0 until ideas.length()) {
            val idea = ideas.getJSONObject(index)
            val ideaId = idea.optString("id", "").trim()
            val path = idea.optString("audioLocalPath", "").trim()
            if (ideaId.isBlank() || path.isBlank()) continue
            val file = ownedFinalVoiceRecording(path) ?: continue
            require(seenIdeaIds.add(ideaId)) { "Duplicate Voice Idea id in backup" }
            result += PortableBackupMedia(ideaId, file)
        }
        return result
    }

    private fun installVoiceMedia(raw: String, stagedByIdeaId: Map<String, File>): Map<String, File> {
        val ideas = JSONArray(JSONObject(raw).getString("ideas"))
        val expectedIds = buildSet {
            for (index in 0 until ideas.length()) {
                val idea = ideas.getJSONObject(index)
                if (idea.optString("audioLocalPath", "").isNotBlank()) {
                    idea.optString("id", "").trim().takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }
        require(stagedByIdeaId.keys.all { it in expectedIds }) { "Voice media does not match backup ideas" }

        val voiceDir = File(appContext.filesDir, VOICE_IDEA_DIRECTORY).apply { mkdirs() }.canonicalFile
        val installed = linkedMapOf<String, File>()
        try {
            stagedByIdeaId.forEach { (ideaId, staged) ->
                val destination = uniqueVoiceDestination(voiceDir)
                // Track the destination before any bytes are written so a partial write or a
                // post-copy integrity failure is removed by the common rollback path below.
                installed[ideaId] = destination
                staged.inputStream().buffered().use { input ->
                    destination.outputStream().buffered().use(input::copyTo)
                }
                PortableVoiceMediaIntegrity.verifyInstalledCopy(staged, destination)
            }
            return installed
        } catch (error: Throwable) {
            installed.values.forEach { runCatching { it.delete() } }
            throw error
        }
    }

    private fun rewritePortableVoicePaths(raw: String, installed: Map<String, File>): String {
        val root = JSONObject(raw)
        require(root.optInt("schemaVersion", -1) == CreatorBackupManager.SCHEMA_VERSION) {
            "Portable voice media requires the current Backlot backup schema"
        }
        val ideas = JSONArray(root.getString("ideas"))
        val consumed = mutableSetOf<String>()
        for (index in 0 until ideas.length()) {
            val idea = ideas.getJSONObject(index)
            val oldPath = idea.optString("audioLocalPath", "")
            if (oldPath.isBlank()) continue
            val ideaId = idea.optString("id", "").trim()
            val restored = installed[ideaId]
            if (restored != null) {
                idea.put("audioLocalPath", restored.absolutePath)
                consumed += ideaId
            } else {
                // The exporting device did not have a readable app-owned recording. Never retain
                // its absolute path on another device because it would become a dead reference.
                idea.put("audioLocalPath", "")
                if (idea.optString("audioRemoteUrl", "").isBlank()) {
                    idea.put("audioSyncState", IdeaAudioSyncState.NONE.name)
                }
            }
        }
        require(consumed == installed.keys) { "Not all restored Voice Idea media was referenced" }
        root.put("ideas", ideas.toString())
        root.put("payloadSha256", sha256(fingerprintCurrentSchema(root)))
        return root.toString().also(creatorBackup::validate)
    }

    private fun ownedFinalVoiceRecording(path: String): File? = runCatching {
        val root = File(appContext.filesDir, VOICE_IDEA_DIRECTORY).canonicalFile
        val target = File(path).canonicalFile
        val owned = target.parentFile == root &&
            target.isFile &&
            target.name.startsWith(VOICE_IDEA_FILE_PREFIX) &&
            target.name.endsWith(VOICE_IDEA_FINAL_SUFFIX, ignoreCase = true) &&
            !target.name.endsWith(VOICE_IDEA_WORKING_SUFFIX, ignoreCase = true)
        target.takeIf { owned }
    }.getOrNull()

    private fun uniqueVoiceDestination(root: File): File {
        repeat(20) {
            val candidate = File(root, "$VOICE_IDEA_FILE_PREFIX${UUID.randomUUID()}$VOICE_IDEA_FINAL_SUFFIX")
            if (!candidate.exists()) return candidate
        }
        error("Could not allocate Voice Idea recording path")
    }

    /** Must stay byte-for-byte equivalent to CreatorBackupManager's schema-8 fingerprint order. */
    private fun fingerprintCurrentSchema(root: JSONObject): String = buildString {
        CURRENT_FINGERPRINT_KEYS.forEach { key ->
            val value = root.optString(key, "")
            append(key.length).append(':').append(key).append(value.length).append(':').append(value)
        }
    }

    private fun sha256(raw: String): String = MessageDigest.getInstance("SHA-256")
        .digest(raw.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    companion object {
        private const val VOICE_IDEA_DIRECTORY = "voice_ideas"
        private const val VOICE_IDEA_FILE_PREFIX = "voice_idea_"
        private const val VOICE_IDEA_WORKING_SUFFIX = ".recording.m4a"
        private const val VOICE_IDEA_FINAL_SUFFIX = ".m4a"
        private val CURRENT_FINGERPRINT_KEYS = listOf(
            "format",
            "schemaVersion",
            "createdAtMillis",
            "tasks",
            "ideas",
            "weeklySchedule",
            "settings",
            "smartEscalationConfig",
            "postPublish",
            "rewards",
            "personalFrames",
            "youtubeProjectLinks",
            "youtubeMilestones",
            "youtubePublishCheckpoints",
            "projectPulseHistory",
            "workflowStageTimeline",
            "manifest",
        )
    }
}

/**
 * Verifies the final app-owned copy, not only the staged archive entry. A filesystem or write-path
 * fault can preserve byte length while changing content, so length alone is not an integrity check.
 */
internal object PortableVoiceMediaIntegrity {
    fun verifyInstalledCopy(staged: File, installed: File) {
        require(staged.isFile && installed.isFile && installed.length() == staged.length()) {
            "Could not restore Voice Idea recording"
        }
        require(sha256(installed) == sha256(staged)) {
            "Restored Voice Idea recording failed integrity check"
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
