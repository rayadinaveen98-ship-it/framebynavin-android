package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorProfile

/**
 * V207-C: only ask what is needed to establish a local-first creator workspace.
 * Goals, extra modes, production styles and Android notification permissions
 * remain editable later and MUST NOT block a creator's first project.
 */
internal object V207FirstRunPolicy {
    const val PAGE_COUNT = 3
    const val LAST_PAGE = PAGE_COUNT - 1
    const val DEFAULT_GOAL = "Stay organized"

    fun canContinue(page: Int, primaryMode: String, platforms: Set<String>): Boolean =
        when (page) {
            0 -> primaryMode.isNotBlank()
            1 -> platforms.isNotEmpty()
            LAST_PAGE -> primaryMode.isNotBlank() && platforms.isNotEmpty()
            else -> false
        }

    fun resolvedGoal(profile: CreatorProfile): String =
        profile.primaryGoal.trim().ifBlank { DEFAULT_GOAL }

    fun completeProfile(profile: CreatorProfile, primaryMode: String, platforms: Set<String>): CreatorProfile {
        require(primaryMode.isNotBlank() && platforms.isNotEmpty()) { "Creator mode and publishing platform required" }
        val completed = profile.copy(
            category = primaryMode,
            primaryCreatorMode = primaryMode,
            platforms = platforms,
            primaryGoal = resolvedGoal(profile),
            setupSchemaVersion = CreatorProfile.CURRENT_SCHEMA_VERSION,
        ).normalized()
        require(completed.isComplete) { "Creator setup cannot complete with invalid profile" }
        return completed
    }
}
