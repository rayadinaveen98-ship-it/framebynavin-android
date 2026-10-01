package com.backlot.shared.ideas

enum class BacklotIdeaStatus {
    INBOX,
    WORTH_EXPLORING,
    RESEARCHING,
    READY_TO_PRODUCE,
    CONVERTED,
    ARCHIVED,
}

enum class BacklotIdeaPotential {
    LOW,
    MEDIUM,
    HIGH,
}

enum class BacklotIdeaCaptureType {
    TEXT,
    VOICE,
}

enum class BacklotIdeaAudioSyncState {
    NONE,
    LOCAL_ONLY,
    PENDING_UPLOAD,
    SYNCED,
    UPLOAD_FAILED,
}

enum class BacklotIdeaTranscriptionState {
    NOT_REQUESTED,
    PENDING,
    COMPLETED,
    FAILED,
    UNAVAILABLE,
}

/**
 * Platform-neutral Idea Vault record.
 *
 * Device-local audio paths are intentionally absent. Android and iOS keep their own local media
 * handles while this shared model carries the cloud media reference and durable creator metadata.
 */
data class BacklotIdea(
    val id: String,
    val title: String,
    val topic: String = "",
    val categoryId: String = "CONTENT_IDEA",
    val status: BacklotIdeaStatus = BacklotIdeaStatus.INBOX,
    val potential: BacklotIdeaPotential = BacklotIdeaPotential.MEDIUM,
    val platformHint: String = "YouTube",
    val formatHint: String = "Long-form",
    val notes: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val projectTaskId: String = "",
    val sourceRefId: String = "",
    val reminderAtMillis: Long = 0L,
    val reminderCadenceId: String = "ONCE",
    val captureType: BacklotIdeaCaptureType = BacklotIdeaCaptureType.TEXT,
    val audioRemoteRef: String = "",
    val audioDurationMillis: Long = 0L,
    val audioMimeType: String = "",
    val audioSyncState: BacklotIdeaAudioSyncState = BacklotIdeaAudioSyncState.NONE,
    val transcript: String = "",
    val transcriptionState: BacklotIdeaTranscriptionState = BacklotIdeaTranscriptionState.NOT_REQUESTED,
    val transcriptionError: String = "",
    val tags: List<String> = emptyList(),
) {
    val isVoice: Boolean get() = captureType == BacklotIdeaCaptureType.VOICE
    val hasProtectedRecording: Boolean get() = isVoice && audioRemoteRef.isNotBlank()
}
