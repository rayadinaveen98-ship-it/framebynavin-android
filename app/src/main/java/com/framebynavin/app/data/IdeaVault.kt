package com.framebynavin.app.data

enum class IdeaCategory {
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
)

object IdeaVaultLabels {
    fun category(category: IdeaCategory): String = when (category) {
        IdeaCategory.CINEMATIC_ANALYSIS -> "Cinematic Analysis"
        IdeaCategory.EVERY_CINEMATIC_MOMENT -> "Every Cinematic Moment"
        IdeaCategory.FRAME_OF_TODAY -> "#TheFrameOfToday"
        IdeaCategory.FRAME_BREAKDOWN -> "Frame Breakdown"
        IdeaCategory.WHY_THIS_SCENE_WORKS -> "Why This Scene Works"
        IdeaCategory.REVIEW_RECOMMENDATION -> "Review / Recommendation"
        IdeaCategory.RELEASE_REACTION -> "Release Reaction"
        IdeaCategory.EXPERIMENT -> "Experiment"
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
