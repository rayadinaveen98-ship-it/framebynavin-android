package com.framebynavin.app.data

enum class ContentDnaDimension {
    CREATOR_MODE,
    ARCHETYPE,
    PRODUCTION_STYLE,
    PLATFORM,
    DELIVERY_FORMAT,
}

/**
 * Effective project-level Content DNA.
 *
 * Alpha 1.2H is deliberately read-only/non-destructive: DNA is resolved from existing project
 * fields plus creator-profile defaults instead of rewriting legacy projects. A later milestone can
 * persist explicit project overrides once the model has proved stable.
 */
data class CreatorContentDna(
    val creatorModeId: String = "",
    val archetypeId: String = "",
    val productionStyles: Set<String> = emptySet(),
    val platform: String = "",
    val deliveryFormat: String = "",
    /** Original legacy field retained for explainability while migration stays non-destructive. */
    val legacyContentType: String = "",
    val inferredFromLegacy: Boolean = false,
) {
    val isEmpty: Boolean
        get() = creatorModeId.isBlank() &&
            archetypeId.isBlank() &&
            productionStyles.isEmpty() &&
            platform.isBlank() &&
            deliveryFormat.isBlank()

    fun missingDimensions(): Set<ContentDnaDimension> = buildSet {
        if (creatorModeId.isBlank()) add(ContentDnaDimension.CREATOR_MODE)
        if (archetypeId.isBlank()) add(ContentDnaDimension.ARCHETYPE)
        if (productionStyles.isEmpty()) add(ContentDnaDimension.PRODUCTION_STYLE)
        if (platform.isBlank()) add(ContentDnaDimension.PLATFORM)
        if (deliveryFormat.isBlank()) add(ContentDnaDimension.DELIVERY_FORMAT)
    }

    fun normalized(): CreatorContentDna {
        val modeId = creatorModeId.trim().takeIf { it.isNotBlank() }?.let { value ->
            CreatorModeRegistry.definition(value).id
        }.orEmpty()
        val canonicalArchetype = ContentArchetypeRegistry.definition(archetypeId)?.id.orEmpty()
        val canonicalStyles = productionStyles
            .map(ProductionStyleRegistry::canonicalLabel)
            .filter { it.isNotBlank() }
            .distinct()
            .take(CreatorProfile.MAX_PRODUCTION_STYLES)
            .toSet()
        val canonicalPlatform = CreatorPlatformRegistry.canonicalName(platform)
        val candidateFormat = deliveryFormat.trim()
        val canonicalFormat = when {
            canonicalPlatform.isBlank() || candidateFormat.isBlank() -> ""
            CreatorPlatformRegistry.acceptsFormat(canonicalPlatform, candidateFormat) -> candidateFormat.take(80)
            else -> ""
        }
        return copy(
            creatorModeId = modeId,
            archetypeId = canonicalArchetype,
            productionStyles = canonicalStyles,
            platform = canonicalPlatform,
            deliveryFormat = canonicalFormat,
            legacyContentType = legacyContentType.trim().take(80),
        )
    }
}

/**
 * Deterministic resolver joining the four V2 registries without changing CreatorTask persistence.
 *
 * - Creator Mode comes from account/profile defaults.
 * - Archetype is inferred from the historical contentType only when that meaning is known.
 * - Production Styles come from creator defaults until project-level overrides are introduced.
 * - Platform comes from the project.
 * - Delivery Format is populated only when the old contentType is actually valid for that platform.
 */
object CreatorContentDnaEngine {
    private val legacyArchetypes: Map<String, String> = mapOf(
        "cinematic moment" to "highlights",
        "cinematic moments" to "highlights",
        "long-form" to "video",
        "long video" to "video",
        "video" to "video",
        "short" to "short",
        "reel" to "short",
        "short / explainer" to "explainer",
        "post" to "post",
        "story" to "story",
        "article" to "article",
        "newsletter" to "newsletter",
        "episode" to "podcast",
        "update" to "news_update",
    )

    fun resolve(task: CreatorTask, profile: CreatorProfile = CreatorProfile()): CreatorContentDna {
        val platform = CreatorPlatformRegistry.canonicalName(task.platform)
        val legacyType = task.contentType.trim()
        val modeId = profile.resolvedPrimaryCreatorMode.trim().takeIf { it.isNotBlank() }?.let { value ->
            CreatorModeRegistry.definition(value).id
        }.orEmpty()
        val archetypeId = archetypeForLegacyType(legacyType)
        val styles = profile.productionStyles
            .map(ProductionStyleRegistry::canonicalLabel)
            .filter { it.isNotBlank() }
            .distinct()
            .take(CreatorProfile.MAX_PRODUCTION_STYLES)
            .toSet()
        val format = legacyType.takeIf {
            platform.isNotBlank() && it.isNotBlank() && CreatorPlatformRegistry.acceptsFormat(platform, it)
        }.orEmpty()

        return CreatorContentDna(
            creatorModeId = modeId,
            archetypeId = archetypeId,
            productionStyles = styles,
            platform = platform,
            deliveryFormat = format,
            legacyContentType = legacyType,
            inferredFromLegacy = legacyType.isNotBlank(),
        ).normalized()
    }

    fun forNewProject(
        profile: CreatorProfile,
        platform: String,
        archetypeId: String,
        productionStyles: Set<String> = profile.productionStyles,
        deliveryFormat: String = "",
    ): CreatorContentDna {
        val canonicalPlatform = CreatorPlatformRegistry.canonicalName(platform)
        val format = deliveryFormat.trim().ifBlank {
            canonicalPlatform.takeIf { it.isNotBlank() }?.let(CreatorPlatformRegistry::defaultFormat).orEmpty()
        }
        return CreatorContentDna(
            creatorModeId = profile.resolvedPrimaryCreatorMode,
            archetypeId = archetypeId,
            productionStyles = productionStyles,
            platform = canonicalPlatform,
            deliveryFormat = format,
            inferredFromLegacy = false,
        ).normalized()
    }

    fun archetypeForLegacyType(contentType: String): String {
        val value = contentType.trim()
        if (value.isBlank()) return ""
        ContentArchetypeRegistry.definition(value)?.let { return it.id }
        return legacyArchetypes[value.lowercase()].orEmpty()
    }
}
