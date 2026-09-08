package com.framebynavin.app.data

data class QuickCaptureSuggestion(
    val category: IdeaCategory,
    val potential: IdeaPotential,
    val platformHint: String,
    val formatHint: String,
)

object CreatorQuickCaptureEngine {
    fun suggest(text: String, profile: CreatorProfile? = null): QuickCaptureSuggestion {
        val value = text.trim().lowercase()
        val raw = when {
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
        val normalized = profile?.normalized() ?: return raw
        val categoryAdjusted = if (normalized.category.equals("Film & Entertainment", ignoreCase = true)) {
            raw
        } else {
            val genericCategory = when {
                "how to" in value || "tutorial" in value || "guide" in value || "explain" in value -> IdeaCategory.HOW_TO_EXPLAINER
                "series" in value || "episode" in value || "weekly" in value || "daily" in value -> IdeaCategory.SERIES
                "behind" in value || "process" in value || "making" in value || "workflow" in value -> IdeaCategory.BEHIND_THE_SCENES
                "community" in value || "question" in value || "poll" in value || "q&a" in value -> IdeaCategory.COMMUNITY
                "opinion" in value || "thought" in value || "take" in value || "commentary" in value -> IdeaCategory.OPINION_COMMENTARY
                "review" in value || "recommend" in value || "rating" in value -> IdeaCategory.REVIEW_RECOMMENDATION
                "release" in value || "launch" in value || "announcement" in value -> IdeaCategory.RELEASE_REACTION
                else -> IdeaCategory.CONTENT_IDEA
            }
            raw.copy(category = genericCategory)
        }
        if (normalized.platforms.isEmpty() || normalized.platforms.any { it.equals(categoryAdjusted.platformHint, ignoreCase = true) }) return categoryAdjusted
        val platform = CreatorPlatformRegistry.primaryPlatform(normalized)
        return categoryAdjusted.copy(
            platformHint = platform,
            formatHint = CreatorPlatformRegistry.defaultFormat(platform),
        )
    }

    fun toIdea(
        title: String,
        notes: String = "",
        now: Long = System.currentTimeMillis(),
        profile: CreatorProfile? = null,
    ): CreatorIdea {
        val suggestion = suggest("$title $notes", profile)
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
