package com.framebynavin.app.data

/**
 * Profile editing does not re-run onboarding, replace schedules, or recreate projects.
 * The original profile is the basis for all fields the creator did not change.
 */
data class CreatorProfileEditDraft(
    val primaryMode: String,
    val secondaryModes: Set<String>,
    val platforms: Set<String>,
    val productionStyles: Set<String>,
    val primaryGoal: String,
    val secondaryGoals: Set<String>,
    val weeklyTarget: Int,
)

object CreatorProfileEditV168Policy {
    fun draft(profile: CreatorProfile): CreatorProfileEditDraft = CreatorProfileEditDraft(
        primaryMode = profile.resolvedPrimaryCreatorMode,
        secondaryModes = profile.secondaryCreatorModes,
        platforms = profile.platforms,
        productionStyles = profile.productionStyles,
        primaryGoal = profile.primaryGoal,
        secondaryGoals = profile.secondaryGoals,
        weeklyTarget = profile.weeklyPublishingTarget,
    )

    fun validate(draft: CreatorProfileEditDraft): String? = when {
        draft.primaryMode.isBlank() -> "Choose what kind of content you make."
        draft.platforms.isEmpty() -> "Choose at least one platform."
        draft.primaryGoal.isBlank() -> "Choose your main goal."
        draft.weeklyTarget !in 1..14 -> "Choose between 1 and 14 posts per week."
        draft.secondaryModes.size > CreatorProfile.MAX_SECONDARY_MODES ->
            "Choose up to ${CreatorProfile.MAX_SECONDARY_MODES} additional creator types."
        draft.secondaryGoals.size > CreatorProfile.MAX_ACTIVE_GOALS - 1 ->
            "Choose up to ${CreatorProfile.MAX_ACTIVE_GOALS - 1} additional goals."
        draft.productionStyles.size > CreatorProfile.MAX_PRODUCTION_STYLES ->
            "Choose up to ${CreatorProfile.MAX_PRODUCTION_STYLES} ways to create."
        else -> null
    }

    fun apply(original: CreatorProfile, draft: CreatorProfileEditDraft): CreatorProfile {
        require(validate(draft) == null) { validate(draft) ?: "Invalid creator details" }
        // Preserve display name, account identity, schema, and any fields not in the draft.
        return original.copy(
            category = draft.primaryMode,
            primaryCreatorMode = draft.primaryMode,
            secondaryCreatorModes = draft.secondaryModes - draft.primaryMode,
            platforms = draft.platforms,
            productionStyles = draft.productionStyles,
            primaryGoal = draft.primaryGoal,
            secondaryGoals = draft.secondaryGoals - draft.primaryGoal,
            weeklyPublishingTarget = draft.weeklyTarget,
        ).normalized()
    }
}
