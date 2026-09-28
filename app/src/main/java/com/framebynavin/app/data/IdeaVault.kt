package com.framebynavin.app.data

enum class IdeaCategory {
    CONTENT_IDEA,
    HOW_TO_EXPLAINER,
    OPINION_COMMENTARY,
    SERIES,
    BEHIND_THE_SCENES,
    COMMUNITY,
    CINEMATIC_ANALYSIS,
    EVERY_CINEMATIC_MOMENT,
    FRAME_OF_TODAY,
    FRAME_BREAKDOWN,
    WHY_THIS_SCENE_WORKS,
    REVIEW_RECOMMENDATION,
    RELEASE_REACTION,
    EXPERIMENT,
}

enum class IdeaStatus {
    INBOX,
    WORTH_EXPLORING,
    RESEARCHING,
    READY_TO_PRODUCE,
    CONVERTED,
    ARCHIVED,
}

enum class IdeaPotential {
    LOW,
    MEDIUM,
    HIGH,
}

/** How the original idea was captured. Existing ideas decode as TEXT. */
enum class IdeaCaptureType {
    TEXT,
    VOICE,
}

/**
 * Upload state for the original voice recording.
 *
 * The local audio file remains the source of truth. Sync/transcription layers may fail or retry
 * independently without replacing the recording.
 */
enum class IdeaAudioSyncState {
    NONE,
    LOCAL_ONLY,
    PENDING_UPLOAD,
    SYNCED,
    UPLOAD_FAILED,
}

/**
 * State of the optional transcript derived from the original Voice Idea recording.
 *
 * This state never controls whether the original audio can be saved or played.
 */
enum class IdeaTranscriptionState {
    NOT_REQUESTED,
    PENDING,
    COMPLETED,
    FAILED,
    UNAVAILABLE,
}

data class CreatorIdea(
    val id: String,
    val title: String,
    val topic: String = "",
    val category: IdeaCategory = IdeaCategory.CINEMATIC_ANALYSIS,
    val status: IdeaStatus = IdeaStatus.INBOX,
    val potential: IdeaPotential = IdeaPotential.MEDIUM,
    val platformHint: String = "YouTube",
    val formatHint: String = "Long-form",
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val projectTaskId: String = "",
    val sourceRefId: String = "",
    val reminderAtMillis: Long = 0L,
    val reminderCadence: IdeaReminderCadence = IdeaReminderCadence.ONCE,
    val captureType: IdeaCaptureType = IdeaCaptureType.TEXT,
    val audioLocalPath: String = "",
    val audioRemoteUrl: String = "",
    val audioDurationMillis: Long = 0L,
    val audioMimeType: String = "",
    val audioSyncState: IdeaAudioSyncState = IdeaAudioSyncState.NONE,
    val transcript: String = "",
    val transcriptionState: IdeaTranscriptionState = IdeaTranscriptionState.NOT_REQUESTED,
    val transcriptionError: String = "",
    val tags: List<String> = emptyList(),
) {
    val hasOriginalRecording: Boolean
        get() = captureType == IdeaCaptureType.VOICE && audioLocalPath.isNotBlank()
}

object IdeaVaultLabels {
    fun category(category: IdeaCategory): String = when (category) {
        IdeaCategory.CONTENT_IDEA -> "Content Idea"
        IdeaCategory.HOW_TO_EXPLAINER -> "How-to / Explainer"
        IdeaCategory.OPINION_COMMENTARY -> "Opinion / Commentary"
        IdeaCategory.SERIES -> "Series / Recurring"
        IdeaCategory.BEHIND_THE_SCENES -> "Behind the Scenes"
        IdeaCategory.COMMUNITY -> "Community"
        IdeaCategory.CINEMATIC_ANALYSIS -> "Deep Dive"
        IdeaCategory.EVERY_CINEMATIC_MOMENT -> "Compilation / List"
        IdeaCategory.FRAME_OF_TODAY -> "Daily Series"
        IdeaCategory.FRAME_BREAKDOWN -> "Breakdown"
        IdeaCategory.WHY_THIS_SCENE_WORKS -> "Explainer"
        IdeaCategory.REVIEW_RECOMMENDATION -> "Review / Recommendation"
        IdeaCategory.RELEASE_REACTION -> "Reaction / Update"
        IdeaCategory.EXPERIMENT -> "Experiment"
    }

    fun categoriesFor(profile: CreatorProfile): List<IdeaCategory> {
        val filmCreator = profile.category.equals("Film & Entertainment", ignoreCase = true)
        return if (filmCreator) {
            listOf(
                IdeaCategory.CINEMATIC_ANALYSIS,
                IdeaCategory.EVERY_CINEMATIC_MOMENT,
                IdeaCategory.FRAME_OF_TODAY,
                IdeaCategory.FRAME_BREAKDOWN,
                IdeaCategory.WHY_THIS_SCENE_WORKS,
                IdeaCategory.REVIEW_RECOMMENDATION,
                IdeaCategory.RELEASE_REACTION,
                IdeaCategory.CONTENT_IDEA,
                IdeaCategory.SERIES,
                IdeaCategory.EXPERIMENT,
            )
        } else {
            listOf(
                IdeaCategory.CONTENT_IDEA,
                IdeaCategory.HOW_TO_EXPLAINER,
                IdeaCategory.OPINION_COMMENTARY,
                IdeaCategory.SERIES,
                IdeaCategory.BEHIND_THE_SCENES,
                IdeaCategory.COMMUNITY,
                IdeaCategory.REVIEW_RECOMMENDATION,
                IdeaCategory.RELEASE_REACTION,
                IdeaCategory.EXPERIMENT,
            )
        }
    }

    fun status(status: IdeaStatus): String = when (status) {
        IdeaStatus.INBOX -> "Inbox"
        IdeaStatus.WORTH_EXPLORING -> "Worth Exploring"
        IdeaStatus.RESEARCHING -> "Researching"
        IdeaStatus.READY_TO_PRODUCE -> "Ready to Produce"
        IdeaStatus.CONVERTED -> "Converted"
        IdeaStatus.ARCHIVED -> "Archived"
    }
}
