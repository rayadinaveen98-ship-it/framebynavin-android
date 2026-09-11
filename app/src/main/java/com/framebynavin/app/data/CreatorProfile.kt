package com.framebynavin.app.data

/**
 * Creator-selected defaults for the local-first Creator OS.
 *
 * `category` is retained as a compatibility field for older app code/backups. New code should use
 * [primaryCreatorMode]. [normalized] mirrors the resolved primary mode into both fields so existing
 * projects/UI continue to behave while the Creator Modes architecture is introduced incrementally.
 * Account identity remains separate from this model.
 */
data class CreatorProfile(
    val displayName: String = "",
    val category: String = "",
    val primaryCreatorMode: String = "",
    val secondaryCreatorModes: Set<String> = emptySet(),
    val platforms: Set<String> = emptySet(),
    val productionStyles: Set<String> = emptySet(),
    val primaryGoal: String = "",
    val secondaryGoals: Set<String> = emptySet(),
    val weeklyPublishingTarget: Int = 2,
    val setupSchemaVersion: Int = 1,
) {
    val resolvedPrimaryCreatorMode: String
        get() = primaryCreatorMode.trim().ifBlank { category.trim() }

    val activeGoals: List<String>
        get() = buildList {
            primaryGoal.trim().takeIf { it.isNotBlank() }?.let(::add)
            secondaryGoals.map(String::trim).filter { it.isNotBlank() && it != primaryGoal.trim() }.sorted().forEach(::add)
        }.take(MAX_ACTIVE_GOALS)

    /** Legacy-complete profiles remain usable; V2 enrichment is never forced on upgrade. */
    val isComplete: Boolean
        get() = resolvedPrimaryCreatorMode.isNotBlank() &&
            platforms.isNotEmpty() &&
            primaryGoal.isNotBlank()

    val isV2Configured: Boolean
        get() = setupSchemaVersion >= CURRENT_SCHEMA_VERSION && isComplete && productionStyles.isNotEmpty()

    val safeDisplayName: String
        get() = displayName.trim().ifBlank { "Creator" }

    fun normalized(): CreatorProfile {
        val rawPrimaryMode = resolvedPrimaryCreatorMode.take(60)
        val primaryMode = CreatorModeRegistry.canonicalLabel(rawPrimaryMode)
        val normalizedPrimaryGoal = primaryGoal.trim().take(80)
        val normalizedSecondaryModes = secondaryCreatorModes
            .map { CreatorModeRegistry.canonicalLabel(it.trim()) }
            .filter { it.isNotBlank() && it != primaryMode }
            .distinct()
            .take(MAX_SECONDARY_MODES)
            .toSet()
        val normalizedSecondaryGoals = secondaryGoals
            .map(String::trim)
            .filter { it.isNotBlank() && it != normalizedPrimaryGoal }
            .distinct()
            .take(MAX_ACTIVE_GOALS - 1)
            .toSet()

        return copy(
            displayName = displayName.trim().take(40),
            category = primaryMode,
            primaryCreatorMode = primaryMode,
            secondaryCreatorModes = normalizedSecondaryModes,
            platforms = platforms.map { it.trim() }.filter { it.isNotBlank() }.toSet(),
            productionStyles = productionStyles
                .map(ProductionStyleRegistry::canonicalLabel)
                .filter { it.isNotBlank() }
                .distinct()
                .take(MAX_PRODUCTION_STYLES)
                .toSet(),
            primaryGoal = normalizedPrimaryGoal,
            secondaryGoals = normalizedSecondaryGoals,
            weeklyPublishingTarget = weeklyPublishingTarget.coerceIn(1, 14),
            setupSchemaVersion = setupSchemaVersion.coerceIn(1, CURRENT_SCHEMA_VERSION),
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
        const val MAX_ACTIVE_GOALS = 3
        const val MAX_SECONDARY_MODES = 3
        const val MAX_PRODUCTION_STYLES = 6
    }
}
