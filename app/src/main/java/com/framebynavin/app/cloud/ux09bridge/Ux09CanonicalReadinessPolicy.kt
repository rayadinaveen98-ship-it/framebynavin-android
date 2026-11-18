package com.framebynavin.app.cloud.ux09bridge

import com.framebynavin.app.cloud.ux09.Ux09Contract
import com.framebynavin.app.cloud.ux09.Ux09Domain
import com.framebynavin.app.data.CreatorContentDna
import com.framebynavin.app.data.CreatorContentWorkspace
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.IdeaAudioSyncState
import com.framebynavin.app.data.IdeaCaptureType
import com.framebynavin.app.data.IdeaTranscriptionState
import com.framebynavin.app.data.TaskStatus
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lab-only *readiness inspection* of the real existing Creator serializers.
 *
 * Explicitly refuses rich/unsupported content rather than truncating it.
 * Does NOT save, upload, restore, delete, or mutate canonical objects.
 * A Ready value is a candidate for a future LAB-only sync pilot, not permission
 * for the current disabled RPC feature to send any data.
 */
internal object Ux09CanonicalReadinessPolicy {
    internal enum class Blocker {
        NON_UUID_ID,
        NOT_SIMPLE_PROJECT,
        ACTIVE_REMINDER_OR_SCHEDULE,
        RICH_WORKSPACE_OR_DNA,
        LEGACY_SCRIPT_STUDIO,
        LINKED_OR_LEGACY_PROJECT,
        VOICE_OR_UNPORTABLE_IDEA,
        IDEA_REMINDER_OR_TRASH,
        INCOMPLETE_SOURCE,
        OVER_SERVER_LIMIT,
        UNSAFE_OR_NONCANONICAL_JSON,
        ROUND_TRIP_CHANGED,
    }

    internal sealed interface Result {
        data class Ready(
            val domain: Ux09Domain,
            val entityId: String,
            val payloadJson: String,
            val sha256: String,
            val bytes: Int,
        ) : Result
        data class Blocked(val reason: Blocker) : Result
    }

    // Frozen against 02A field-coverage manifest; CI must check these sets
    // continue to match the exact existing TaskStore / IdeaVaultStore keys.
    internal val projectFieldKeys = setOf(
        "acknowledgedCheckpointDueAtMillis",
        "acknowledgedCheckpointStageId",
        "alarmSoundUri",
        "alarmTimeoutSeconds",
        "alertType",
        "archivedAtMillis",
        "attentionPlan",
        "autoRetryEnabled",
        "autoRetryMaxAttempts",
        "autoRetryMinutes",
        "autoStageReminder",
        "checkpointAtMillis",
        "checkpointStageId",
        "completedAtMillis",
        "contentDna",
        "contentType",
        "deliveryPreference",
        "dueAtMillis",
        "dueLabel",
        "id",
        "notes",
        "origin",
        "platform",
        "priority",
        "progress",
        "publicationIsLegacy",
        "publishedAtMillis",
        "publishedUrl",
        "pulseManagedReminder",
        "reminderAtMillis",
        "reminderEnabled",
        "reminderMode",
        "scheduleOccurrenceKey",
        "scheduleSlotId",
        "smartEscalationEnabled",
        "snoozeCount",
        "snoozeMinutes",
        "sourceRefId",
        "status",
        "title",
        "voiceEnabled",
        "voicePersona",
        "voiceRepeatCount",
        "voiceRepeatIntervalSeconds",
        "workflowStageIndex",
        "workingUntilMillis",
        "workspace",
    )
    internal val ideaFieldKeys = setOf(
        "audioDurationMillis",
        "audioLocalPath",
        "audioMimeType",
        "audioRemoteUrl",
        "audioSyncState",
        "captureType",
        "category",
        "createdAtMillis",
        "formatHint",
        "id",
        "notes",
        "platformHint",
        "potential",
        "projectTaskId",
        "reminderAtMillis",
        "reminderCadence",
        "sourceRefId",
        "status",
        "tags",
        "title",
        "topic",
        "transcript",
        "transcriptionError",
        "transcriptionState",
        "trashedAtMillis",
        "updatedAtMillis",
    )

    fun inspectProject(
        task: CreatorTask,
        existingOneRecordJson: String,
        decodeOneRecord: (String) -> CreatorTask,
    ): Result {
        if (!isUuid(task.id)) return Result.Blocked(Blocker.NON_UUID_ID)
        if (task.title.isBlank() || task.status != TaskStatus.PLANNED ||
            task.progress != 0 || task.workflowStageIndex !in -1..0 ||
            task.completedAtMillis != 0L || task.publishedAtMillis != 0L ||
            task.archivedAtMillis != 0L
        ) return Result.Blocked(Blocker.NOT_SIMPLE_PROJECT)
        if (task.reminderEnabled || task.voiceEnabled || task.smartEscalationEnabled ||
            task.autoStageReminder || task.pulseManagedReminder ||
            task.checkpointAtMillis != 0L || task.reminderAtMillis != 0L ||
            task.scheduleSlotId.isNotBlank() || task.scheduleOccurrenceKey.isNotBlank()
        ) return Result.Blocked(Blocker.ACTIVE_REMINDER_OR_SCHEDULE)
        if (task.workspace != CreatorContentWorkspace() ||
            task.contentDna != CreatorContentDna()
        ) return Result.Blocked(Blocker.RICH_WORKSPACE_OR_DNA)
        if (task.sourceRefId.isNotBlank()) {
            return Result.Blocked(Blocker.LINKED_OR_LEGACY_PROJECT)
        }
        return examine(
            domain = Ux09Domain.PROJECT,
            entityId = task.id,
            raw = existingOneRecordJson,
            requiredFields = projectFieldKeys,
            excludedFields = setOf("alarmSoundUri", "snoozeCount", "workingUntilMillis"),
        ) { payload ->
            val decoded = decodeOneRecord(JSONArray().put(JSONObject(payload)).toString())
            decoded == task.copy(alarmSoundUri = "", snoozeCount = 0, workingUntilMillis = 0L)
        }
    }

    /**
     * V337 can keep old Script Studio data in a separate sidecar even while
     * CreatorTask.workspace looks empty. Treat any nonempty sidecar as unsafe
     * for the simple-record pilot. Never discard it or imply cloud coverage.
     */
    fun vetoLegacyStudio(result: Result, hasUnmigratedScript: Boolean): Result =
        if (hasUnmigratedScript && result is Result.Ready) {
            Result.Blocked(Blocker.LEGACY_SCRIPT_STUDIO)
        } else result

    fun inspectIdea(
        idea: CreatorIdea,
        existingOneRecordJson: String,
        decodeOneRecord: (String) -> CreatorIdea,
    ): Result {
        if (!isUuid(idea.id)) return Result.Blocked(Blocker.NON_UUID_ID)
        if (idea.title.isBlank() || idea.projectTaskId.isNotBlank() ||
            idea.sourceRefId.isNotBlank() || idea.audioRemoteUrl.isNotBlank() ||
            idea.captureType != IdeaCaptureType.TEXT ||
            idea.audioLocalPath.isNotBlank() ||
            idea.audioDurationMillis != 0L || idea.audioMimeType.isNotBlank() ||
            idea.audioSyncState != IdeaAudioSyncState.NONE ||
            idea.transcriptionState != IdeaTranscriptionState.NOT_REQUESTED ||
            idea.transcript.isNotBlank() || idea.transcriptionError.isNotBlank()
        ) return Result.Blocked(Blocker.VOICE_OR_UNPORTABLE_IDEA)
        if (idea.reminderAtMillis != 0L || idea.trashedAtMillis != 0L) {
            return Result.Blocked(Blocker.IDEA_REMINDER_OR_TRASH)
        }
        return examine(
            domain = Ux09Domain.IDEA,
            entityId = idea.id,
            raw = existingOneRecordJson,
            requiredFields = ideaFieldKeys,
            excludedFields = setOf(
                "audioLocalPath", "audioSyncState", "transcriptionError",
                "audioRemoteUrl", "projectTaskId",
            ),
        ) { payload ->
            val decoded = decodeOneRecord(JSONArray().put(JSONObject(payload)).toString())
            decoded == idea.copy(
                audioLocalPath = "",
                audioRemoteUrl = "",
                audioSyncState = IdeaAudioSyncState.NONE,
                transcriptionError = "",
                projectTaskId = "",
            )
        }
    }

    private fun examine(
        domain: Ux09Domain,
        entityId: String,
        raw: String,
        requiredFields: Set<String>,
        excludedFields: Set<String>,
        checkRoundTrip: (String) -> Boolean,
    ): Result {
        val objectBefore = try {
            val array = JSONArray(raw)
            require(array.length() == 1) { "Expected exactly one canonical source record" }
            array.getJSONObject(0)
        } catch (_: Exception) {
            return Result.Blocked(Blocker.INCOMPLETE_SOURCE)
        }
        if (objectBefore.keys().asSequence().toSet() != requiredFields ||
            objectBefore.optString("id") != entityId
        ) return Result.Blocked(Blocker.INCOMPLETE_SOURCE)

        // Device-local fields are intentionally retained on this phone only.
        // No silent removal of user content or advanced structured fields.
        val candidate = JSONObject(objectBefore.toString())
        excludedFields.forEach { candidate.remove(it) }
        val sourceBytes = candidate.toString().toByteArray(Charsets.UTF_8).size
        if (sourceBytes > Ux09Contract.MAX_PAYLOAD_BYTES) {
            return Result.Blocked(Blocker.OVER_SERVER_LIMIT)
        }
        val canonical = try {
            Ux09Contract.canonicalPayload(candidate.toString())
        } catch (_: Exception) {
            return Result.Blocked(Blocker.UNSAFE_OR_NONCANONICAL_JSON)
        }
        val size = canonical.toByteArray(Charsets.UTF_8).size
        if (size > Ux09Contract.MAX_PAYLOAD_BYTES) {
            return Result.Blocked(Blocker.OVER_SERVER_LIMIT)
        }
        val same = try { checkRoundTrip(canonical) } catch (_: Exception) { false }
        if (!same) return Result.Blocked(Blocker.ROUND_TRIP_CHANGED)
        return Result.Ready(domain, entityId, canonical,
            Ux09Contract.sha256(canonical.toByteArray(Charsets.UTF_8)), size)
    }

    private fun isUuid(id: String): Boolean =
        runCatching { Ux09Contract.requireUuid(id) }.isSuccess
}
