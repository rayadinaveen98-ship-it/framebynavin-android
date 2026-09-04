package com.framebynavin.app.data

data class CreatorProjectPreset(
    val id: String,
    val label: String,
    val platform: String,
    val contentType: String,
    val priority: TaskPriority = TaskPriority.IMPORTANT,
    val suggestedNotes: String = "",
)

object CreatorProjectTemplates {
    val presets: List<CreatorProjectPreset> = listOf(
        CreatorProjectPreset(
            id = "youtube_analysis",
            label = "YouTube Analysis",
            platform = "YouTube",
            contentType = "Long-form",
            priority = TaskPriority.IMPORTANT,
            suggestedNotes = "Angle • Research • Script • Record • Edit • Thumbnail • Upload • Publish • Promote",
        ),
        CreatorProjectPreset(
            id = "movie_review_short",
            label = "Movie Review Short",
            platform = "YouTube",
            contentType = "Short",
            priority = TaskPriority.IMPORTANT,
            suggestedNotes = "Hook • Core opinion • 60-sec script • Voice • Edit • Cover • Upload • Promote",
        ),
        CreatorProjectPreset(
            id = "cinematic_compilation",
            label = "Cinematic Compilation",
            platform = "YouTube",
            contentType = "Cinematic Moment",
            priority = TaskPriority.IMPORTANT,
            suggestedNotes = "Select moments • Build sequence • Sound + grade • Thumbnail • Upload • Publish • Promote",
        ),
        CreatorProjectPreset(
            id = "instagram_reel",
            label = "Instagram Reel",
            platform = "Instagram",
            contentType = "Reel",
            priority = TaskPriority.NORMAL,
            suggestedNotes = "Hook • Script/beats • Voice • Vertical edit • Cover/caption • Upload • Promote",
        ),
        CreatorProjectPreset(
            id = "x_post",
            label = "X Post",
            platform = "X",
            contentType = "Post",
            priority = TaskPriority.NORMAL,
            suggestedNotes = "Draft • Verify wording/context • Publish • Follow up if needed",
        ),
    )

    fun find(id: String): CreatorProjectPreset? = presets.firstOrNull { it.id == id }
}
