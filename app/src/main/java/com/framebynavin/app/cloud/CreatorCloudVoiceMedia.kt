package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.data.CreatorBackupManager
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.IdeaAudioSyncState
import com.framebynavin.app.data.IdeaCaptureType
import com.framebynavin.app.data.IdeaVaultStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

internal data class CreatorVoiceMediaSummary(
    val uploaded: Int = 0,
    val downloaded: Int = 0,
    val failed: Int = 0,
)

/** Private Supabase Storage transport for Voice Idea recordings. */
internal class CreatorVoiceMediaApi {
    suspend fun upload(
        session: CloudSession,
        objectPath: String,
        file: File,
        mimeType: String,
    ) = withContext(Dispatchers.IO) {
        require(file.isFile) { "Voice Idea recording is missing" }
        validateObjectPath(session.userId, objectPath)
        val connection = connection(
            method = "POST",
            session = session,
            objectPath = objectPath,
            contentType = mimeType.ifBlank { "audio/mp4" },
        ).apply {
            doOutput = true
            setRequestProperty("x-upsert", "true")
            setFixedLengthStreamingMode(file.length())
        }
        try {
            connection.outputStream.buffered().use { output ->
                file.inputStream().buffered().use { input -> input.copyTo(output) }
            }
            ensureSuccess(connection)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun download(
        session: CloudSession,
        objectPath: String,
        destination: File,
    ) = withContext(Dispatchers.IO) {
        validateObjectPath(session.userId, objectPath)
        val connection = connection("GET", session, objectPath, null)
        try {
            ensureSuccess(connection, consumeSuccessBody = false)
            destination.parentFile?.mkdirs()
            connection.inputStream.buffered().use { input ->
                destination.outputStream().buffered().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun connection(
        method: String,
        session: CloudSession,
        objectPath: String,
        contentType: String?,
    ): HttpURLConnection = (URL(
        CloudConfig.SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + objectPath
    ).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        useCaches = false
        connectTimeout = 20_000
        readTimeout = 60_000
        setRequestProperty("Cache-Control", "no-store")
        setRequestProperty("apikey", CloudConfig.SUPABASE_PUBLISHABLE_KEY)
        setRequestProperty("Authorization", "Bearer ${session.accessToken}")
        setRequestProperty("Accept", "application/json")
        contentType?.let { setRequestProperty("Content-Type", it) }
    }

    private fun ensureSuccess(connection: HttpURLConnection, consumeSuccessBody: Boolean = true) {
        val status = connection.responseCode
        if (status in 200..299) {
            if (consumeSuccessBody) runCatching { connection.inputStream?.close() }
            return
        }
        val text = connection.errorStream?.let {
            BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use(BufferedReader::readText)
        }.orEmpty()
        val message = runCatching {
            val o = JSONObject(text)
            o.optString("message").ifBlank { o.optString("error") }
        }.getOrDefault("").ifBlank { "Voice media request failed ($status)" }
        throw CloudHttpException(status, message)
    }

    private fun validateObjectPath(userId: String, objectPath: String) {
        require(SAFE_USER.matches(userId)) { "Creator identity is invalid" }
        require(objectPath.startsWith("$userId/")) { "Voice media path does not belong to this creator" }
        require(!objectPath.contains("..") && !objectPath.contains('\\')) { "Voice media path is invalid" }
        objectPath.split('/').forEach {
            require(SAFE_SEGMENT.matches(it)) { "Voice media path is invalid" }
        }
    }

    companion object {
        const val BUCKET = "creator-voice-ideas"
        private val SAFE_USER = Regex("[A-Za-z0-9-]{8,80}")
        private val SAFE_SEGMENT = Regex("[A-Za-z0-9._-]{1,160}")
    }
}

/**
 * Uploads app-owned recordings before snapshot sync and reconstructs local media after restore.
 * The SHA-256 hash is part of the object path so restored bytes can be verified.
 */
internal class CreatorCloudVoiceMediaManager(context: Context) {
    private val app = context.applicationContext
    private val store = IdeaVaultStore(app)
    private val api = CreatorVoiceMediaApi()

    suspend fun protectLocal(session: CloudSession): CreatorVoiceMediaSummary {
        val initial = store.load()
        val updates = linkedMapOf<String, CreatorIdea>()
        var uploaded = 0
        initial.forEach { idea ->
            if (idea.captureType != IdeaCaptureType.VOICE) return@forEach
            val file = ownedRecording(idea.audioLocalPath) ?: return@forEach
            val hash = sha256(file)
            val objectPath = objectPath(session.userId, idea.id, hash)
            val remote = remoteUri(objectPath)
            if (idea.audioRemoteUrl == remote && idea.audioSyncState == IdeaAudioSyncState.SYNCED) {
                return@forEach
            }
            api.upload(session, objectPath, file, idea.audioMimeType)
            updates[idea.id] = idea.copy(
                audioRemoteUrl = remote,
                audioSyncState = IdeaAudioSyncState.SYNCED,
                updatedAtMillis = maxOf(idea.updatedAtMillis, System.currentTimeMillis()),
            )
            uploaded++
        }
        if (updates.isNotEmpty()) {
            store.mutate { current -> current.map { updates[it.id] ?: it } }
        }
        return CreatorVoiceMediaSummary(uploaded = uploaded)
    }

    suspend fun restoreMissing(session: CloudSession): CreatorVoiceMediaSummary {
        val initial = store.load()
        val updates = linkedMapOf<String, CreatorIdea>()
        var downloaded = 0
        var failed = 0
        initial.forEach { idea ->
            if (idea.captureType != IdeaCaptureType.VOICE || idea.audioRemoteUrl.isBlank()) return@forEach
            val remote = parseRemote(session.userId, idea.id, idea.audioRemoteUrl) ?: run {
                failed++
                return@forEach
            }
            val current = ownedRecording(idea.audioLocalPath)
            if (current != null && sha256(current) == remote.sha256) return@forEach

            val root = File(app.filesDir, VOICE_DIRECTORY).apply { mkdirs() }
            val temp = File(app.cacheDir, "voice-restore-${UUID.randomUUID()}.m4a")
            try {
                api.download(session, remote.objectPath, temp)
                require(sha256(temp) == remote.sha256) { "Restored Voice Idea failed integrity check" }
                val destination = File(root, "voice_idea_${UUID.randomUUID()}.m4a")
                temp.inputStream().buffered().use { input ->
                    destination.outputStream().buffered().use { output -> input.copyTo(output) }
                }
                require(destination.isFile && sha256(destination) == remote.sha256) {
                    "Could not install restored Voice Idea recording"
                }
                updates[idea.id] = idea.copy(
                    audioLocalPath = destination.absolutePath,
                    audioSyncState = IdeaAudioSyncState.SYNCED,
                )
                downloaded++
            } catch (_: Throwable) {
                failed++
            } finally {
                runCatching { temp.delete() }
            }
        }
        if (updates.isNotEmpty()) {
            store.mutate { current -> current.map { updates[it.id] ?: it } }
        }
        return CreatorVoiceMediaSummary(downloaded = downloaded, failed = failed)
    }

    private data class RemoteVoiceObject(val objectPath: String, val sha256: String)

    private fun parseRemote(userId: String, ideaId: String, value: String): RemoteVoiceObject? = runCatching {
        require(value.startsWith(REMOTE_PREFIX))
        val path = value.removePrefix(REMOTE_PREFIX)
        val parts = path.split('/')
        require(parts.size == 4)
        require(parts[0] == userId)
        require(parts[1] == safeSegment(ideaId))
        require(parts[2] == "media")
        val filename = parts[3]
        require(filename.endsWith(".m4a"))
        val hash = filename.removeSuffix(".m4a")
        require(SHA256.matches(hash))
        RemoteVoiceObject(path, hash)
    }.getOrNull()

    private fun objectPath(userId: String, ideaId: String, hash: String): String =
        "$userId/${safeSegment(ideaId)}/media/$hash.m4a"

    private fun remoteUri(objectPath: String): String = REMOTE_PREFIX + objectPath

    private fun safeSegment(value: String): String = value.trim()
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .take(120)
        .ifBlank { "idea" }

    private fun ownedRecording(path: String): File? = runCatching {
        if (path.isBlank()) return@runCatching null
        val root = File(app.filesDir, VOICE_DIRECTORY).canonicalFile
        val target = File(path).canonicalFile
        val owned = target.parentFile == root && target.isFile &&
            target.name.startsWith("voice_idea_") &&
            target.name.endsWith(".m4a", ignoreCase = true) &&
            !target.name.endsWith(".recording.m4a", ignoreCase = true)
        target.takeIf { owned }
    }.getOrNull()

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

    companion object {
        private const val VOICE_DIRECTORY = "voice_ideas"
        private const val REMOTE_PREFIX = "storage://${CreatorVoiceMediaApi.BUCKET}/"
        private val SHA256 = Regex("[0-9a-f]{64}")
    }
}

/**
 * Cloud snapshots never persist device-local absolute media paths. Manual portable backups still
 * retain those paths because PortableCreatorBackupManager packages their media bytes.
 */
internal object CreatorCloudPayloadSanitizer {
    fun sanitize(raw: String): String {
        val root = JSONObject(raw)
        val schema = root.optInt("schemaVersion", -1)
        require(schema == CreatorBackupManager.SCHEMA_VERSION) { "Cloud backup schema is out of date" }
        val ideas = JSONArray(root.getString("ideas"))
        for (index in 0 until ideas.length()) {
            val idea = ideas.getJSONObject(index)
            if (idea.optString("captureType") == IdeaCaptureType.VOICE.name) {
                idea.put("audioLocalPath", "")
            }
        }
        root.put("ideas", ideas.toString())
        root.put("payloadSha256", sha256(fingerprint(root)))
        return root.toString()
    }

    private fun fingerprint(root: JSONObject): String = buildString {
        CURRENT_KEYS.forEach { key ->
            val value = root.optString(key, "")
            append(key.length).append(':').append(key).append(value.length).append(':').append(value)
        }
    }

    private fun sha256(raw: String): String = MessageDigest.getInstance("SHA-256")
        .digest(raw.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private val CURRENT_KEYS = listOf(
        "format", "schemaVersion", "createdAtMillis", "tasks", "ideas", "weeklySchedule",
        "settings", "smartEscalationConfig", "postPublish", "rewards", "personalFrames",
        "youtubeProjectLinks", "youtubeMilestones", "youtubePublishCheckpoints",
        "projectPulseHistory", "workflowStageTimeline", "manifest",
    )
}
