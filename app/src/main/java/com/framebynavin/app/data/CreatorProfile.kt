package com.framebynavin.app.data

/**
 * Small creator identity model used to personalize the local Creator OS without assuming a niche.
 * It deliberately stores only creator-chosen product preferences; account identity stays separate.
 */
data class CreatorProfile(
    val displayName: String = "",
    val category: String = "",
    val platforms: Set<String> = emptySet(),
    val primaryGoal: String = "",
    val weeklyPublishingTarget: Int = 2,
) {
    val isComplete: Boolean
        get() = category.isNotBlank() &&
            platforms.isNotEmpty() &&
            primaryGoal.isNotBlank()

    val safeDisplayName: String
        get() = displayName.trim().ifBlank { "Creator" }

    fun normalized(): CreatorProfile = copy(
        displayName = displayName.trim().take(40),
        category = category.trim().take(60),
        platforms = platforms.map { it.trim() }.filter { it.isNotBlank() }.toSet(),
        primaryGoal = primaryGoal.trim().take(80),
        weeklyPublishingTarget = weeklyPublishingTarget.coerceIn(1, 14),
    )
}
