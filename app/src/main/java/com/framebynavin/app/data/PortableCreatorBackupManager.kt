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
        // The core snapshot remains schema v9; the PORTABLE representation
        // is built compactly while holding the same creator-data transaction.
        // Never reparse the massive Base64 canonical JSON just to split images.
        val packed = creatorBackup.createPortableBackup()
        PortableBackupArchive.write(
            packed.compactJson,
            exportableVoiceMedia(packed.compactJson),
            output,
            packed.frames,
        )
    }

    /**
     * A restore preview must verify the ENTIRE portable archive, including
     * voice-media manifest, sizes and SHA-256 checksums. The older preview
     * stopped at the first ZIP entry (backlot.json), falsely showing a safe
     * restore even when later voice entries were truncated or corrupted.
     */
    fun validate(input: InputStream): CreatorBackupManager.BackupPreview {
        val staged = PortableBackupArchive.stage(input, appContext.cacheDir)
        try {
            // Preview does not need to materialize the full Base64 canonical
            // JSON. Validate the original compact envelope, staged media, exact
            // original-frame hash, and every regular creator-data section.
            creatorBackup.verifyPortableJsonEnvelope(staged.backupJson)
            val verifiedFrames = PortableBestFrameMedia.verifyCompactPreview(
                staged.backupJson, staged.bestFrames, staged.hasFrameManifest,
            )
            val preview = if (verifiedFrames == null) {
                creatorBackup.validate(staged.backupJson) // legacy raw JSON / ZIP
            } else {
                creatorBackup.validatePortableCompact(staged.backupJson, verifiedFrames)
            }
            verifyArchiveVoiceReferences(staged)
            return preview
        } finally {
            staged.cleanup()
        }
    }

    suspend fun restore(input: InputStream): CreatorBackupManager.BackupPreview {
        val staged = PortableBackupArchive.stage(input, appContext.cacheDir)
        try {
            val original = fullyHydratedBackup(staged)
            creatorBackup.validate(original)
            verifyArchiveVoiceReferences(staged)
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

    private fun fullyHydratedBackup(staged: StagedPortableBackup): String {
        // Verify the ORIGINAL portable envelope before any frame hydration
        // recomputes its checksum. Corrupted project/idea JSON must fail closed.
        creatorBackup.verifyPortableJsonEnvelope(staged.backupJson)
        return PortableBestFrameMedia.expand(
            staged.backupJson,
            staged.bestFrames,
            staged.hasFrameManifest,
            creatorBackup::rewritePortablePersonalFrames,
        )
    }

    /**
     * A portable ZIP must contain exactly one verified media item per idea
     * that references an owned local Voice Idea recording. Do not erase valid
     * local recording references merely because an archive silently omitted
     * them. Legacy raw-JSON backups retain their existing compatibility path.
     */
    private fun verifyArchiveVoiceReferences(staged: StagedPortableBackup) {
        if (!staged.isArchive) return
        val ideas = JSONArray(JSONObject(staged.backupJson).getString("ideas"))
        val expectedIds = linkedSetOf<String>()
        for (index in 0 until ideas.length()) {
            val idea = ideas.getJSONObject(index)
            if (idea.optString("audioLocalPath", "").isBlank()) continue
            val id = idea.optString("id", "").trim()
            require(id.isNotBlank() && expectedIds.add(id)) {
                "Backup has an invalid or repeated Voice Idea reference"
            }
        }
        require(expectedIds == staged.mediaByIdeaId.keys) {
            "Backup voice recordings are missing or do not match saved ideas. No data was restored."
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
            // Refuse an apparently successful backup if it would silently omit a
            // referenced, but missing or corrupt, original voice recording.
            val file = PortableVoiceExportPolicy.requireReadableOwnedRecording(
                File(appContext.filesDir, VOICE_IDEA_DIRECTORY), path,
            )
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
        require(root.optInt("schemaVersion", -1) in 8..CreatorBackupManager.SCHEMA_VERSION) {
            "Portable voice media requires a supported Backlot backup schema"
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
        root.put("payloadSha256", sha256(fingerprintSupportedSchema(root)))
        return root.toString().also(creatorBackup::validate)
    }

    private fun uniqueVoiceDestination(root: File): File {
        repeat(20) {
            val candidate = File(root, "$VOICE_IDEA_FILE_PREFIX${UUID.randomUUID()}$VOICE_IDEA_FINAL_SUFFIX")
            if (!candidate.exists()) return candidate
        }
        error("Could not allocate Voice Idea recording path")
    }

    /** Re-sign the imported schema *without* changing its version or legacy fields. */
    private fun fingerprintSupportedSchema(root: JSONObject): String = buildString {
        val schema = root.getInt("schemaVersion")
        val keys = if (schema >= 9) CURRENT_FINGERPRINT_KEYS
            else CURRENT_FINGERPRINT_KEYS.filterNot { it == "automationPreferences" }
        keys.forEach { key ->
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
            "automationPreferences",
            "manifest",
        )
    }
}

/**
 * Fail closed during export: neither foreign paths nor missing/empty original
 * recordings may be silently omitted from a successful portable backup.
 */
internal object PortableVoiceExportPolicy {
    fun requireReadableOwnedRecording(root: File, rawPath: String): File {
        val target = runCatching { File(rawPath).canonicalFile }.getOrNull()
        val safeRoot = runCatching { root.canonicalFile }.getOrNull()
        require(
            target != null &&
                safeRoot != null &&
                target.parentFile == safeRoot &&
                target.isFile &&
                target.canRead() &&
                target.length() > 0L &&
                target.name.startsWith("voice_idea_") &&
                target.name.endsWith(".m4a", ignoreCase = true) &&
                !target.name.endsWith(".recording.m4a", ignoreCase = true)
        ) {
            "A saved Voice Idea recording is missing or unavailable. The backup was not marked complete."
        }
        return target
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
