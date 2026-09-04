package com.framebynavin.app.data

data class QuickCaptureSuggestion(
    val category: IdeaCategory,
    val potential: IdeaPotential,
    val platformHint: String,
    val formatHint: String,
)

object CreatorQuickCaptureEngine {
    fun suggest(text: String): QuickCaptureSuggestion {
        val value = text.trim().lowercase()
        return when {
            "cinematic moment" in value || "cinematic moments" in value || "best moments" in value -> QuickCaptureSuggestion(
                IdeaCategory.EVERY_CINEMATIC_MOMENT,
                IdeaPotential.HIGH,
                "YouTube",
                "Cinematic Moment",
            )
            "frame of today" in value || "frameoftoday" in value -> QuickCaptureSuggestion(
                IdeaCategory.FRAME_OF_TODAY,
                IdeaPotential.MEDIUM,
                "Instagram",
                "Post",
            )
            "why this scene" in value || "scene works" in value || "scene breakdown" in value -> QuickCaptureSuggestion(
                IdeaCategory.WHY_THIS_SCENE_WORKS,
                IdeaPotential.HIGH,
                "YouTube",
                "Short",
            )
            "frame" in value || "shot" in value || "composition" in value || "lighting" in value -> QuickCaptureSuggestion(
                IdeaCategory.FRAME_BREAKDOWN,
                IdeaPotential.MEDIUM,
                "YouTube",
                "Short",
            )
            "review" in value || "recommend" in value || "rating" in value -> QuickCaptureSuggestion(
                IdeaCategory.REVIEW_RECOMMENDATION,
                IdeaPotential.HIGH,
                "YouTube",
                "Short",
            )
            "release" in value || "trailer" in value || "teaser" in value || "poster" in value || "announcement" in value -> QuickCaptureSuggestion(
                IdeaCategory.RELEASE_REACTION,
                IdeaPotential.HIGH,
                "X",
                "Update",
            )
            else -> QuickCaptureSuggestion(
                IdeaCategory.CINEMATIC_ANALYSIS,
                IdeaPotential.MEDIUM,
                "YouTube",
                "Long-form",
            )
        }
    }

    fun toIdea(title: String, notes: String = "", now: Long = System.currentTimeMillis()): CreatorIdea {
        val suggestion = suggest("$title $notes")
        return CreatorIdea(
            id = "",
            title = title.trim(),
            category = suggestion.category,
            status = IdeaStatus.INBOX,
            potential = suggestion.potential,
            platformHint = suggestion.platformHint,
            formatHint = suggestion.formatHint,
            notes = notes.trim(),
            createdAtMillis = now,
            updatedAtMillis = now,
        )
    }
}
