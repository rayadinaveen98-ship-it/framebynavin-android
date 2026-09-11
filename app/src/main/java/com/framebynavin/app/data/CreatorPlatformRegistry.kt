package com.framebynavin.app.data

data class CreatorPlatformCapabilities(
    val supportsVideo: Boolean = false,
    val supportsShortVideo: Boolean = false,
    val supportsImages: Boolean = false,
    val supportsText: Boolean = false,
    val supportsAudio: Boolean = false,
    val supportsLive: Boolean = false,
    val supportsArticles: Boolean = false,
    val supportsStories: Boolean = false,
)

data class CreatorPlatformDefinition(
    val name: String,
    /** Current delivery formats shown for new work. */
    val formats: List<String>,
    val defaultFormat: String = formats.first(),
    /** Historical values remain readable/editable but are not suggested for new work. */
    val legacyFormats: Set<String> = emptySet(),
    val capabilities: CreatorPlatformCapabilities = CreatorPlatformCapabilities(),
    val aliases: Set<String> = emptySet(),
) {
    fun acceptsFormat(format: String): Boolean {
        val value = format.trim()
        if (value.isBlank()) return false
        return formats.any { it.equals(value, ignoreCase = true) } ||
            legacyFormats.any { it.equals(value, ignoreCase = true) }
    }

    /** Keeps an existing legacy value visible while editing without reintroducing it for new work. */
    fun formatOptions(currentFormat: String? = null): List<String> {
        val current = currentFormat?.trim().orEmpty()
        if (current.isBlank() || formats.any { it.equals(current, ignoreCase = true) }) return formats
        return if (acceptsFormat(current)) formats + current else formats
    }
}

/**
 * Canonical description of WHERE creator content is delivered.
 *
 * Platforms describe delivery capabilities and packaging only. They do not define creator niche,
 * content archetype or production method. Historical format strings remain accepted so old projects
 * and Drive snapshots do not lose meaning when the current creation UI becomes cleaner.
 */
object CreatorPlatformRegistry {
    val definitions: List<CreatorPlatformDefinition> = listOf(
        CreatorPlatformDefinition(
            name = "YouTube",
            formats = listOf("Long-form", "Short", "Video"),
            defaultFormat = "Long-form",
            legacyFormats = setOf("Cinematic Moment", "Long video", "Short / explainer"),
            capabilities = CreatorPlatformCapabilities(
                supportsVideo = true,
                supportsShortVideo = true,
                supportsImages = true,
                supportsText = true,
                supportsLive = true,
            ),
        ),
        CreatorPlatformDefinition(
            name = "Instagram",
            formats = listOf("Reel", "Post", "Story"),
            defaultFormat = "Reel",
            capabilities = CreatorPlatformCapabilities(
                supportsVideo = true,
                supportsShortVideo = true,
                supportsImages = true,
                supportsText = true,
                supportsLive = true,
                supportsStories = true,
            ),
        ),
        CreatorPlatformDefinition(
            name = "X",
            formats = listOf("Post", "Video", "Update"),
            defaultFormat = "Post",
            capabilities = CreatorPlatformCapabilities(
                supportsVideo = true,
                supportsImages = true,
                supportsText = true,
                supportsLive = true,
            ),
            aliases = setOf("Twitter"),
        ),
        CreatorPlatformDefinition(
            name = "Facebook",
            formats = listOf("Post", "Reel", "Video"),
            defaultFormat = "Post",
            capabilities = CreatorPlatformCapabilities(
                supportsVideo = true,
                supportsShortVideo = true,
                supportsImages = true,
                supportsText = true,
                supportsLive = true,
                supportsStories = true,
            ),
        ),
        CreatorPlatformDefinition(
            name = "LinkedIn",
            formats = listOf("Post", "Article", "Video"),
            defaultFormat = "Post",
            capabilities = CreatorPlatformCapabilities(
                supportsVideo = true,
                supportsImages = true,
                supportsText = true,
                supportsArticles = true,
            ),
        ),
        CreatorPlatformDefinition(
            name = "Podcast",
            formats = listOf("Episode", "Clip"),
            defaultFormat = "Episode",
            capabilities = CreatorPlatformCapabilities(
                supportsVideo = true,
                supportsShortVideo = true,
                supportsAudio = true,
            ),
        ),
        CreatorPlatformDefinition(
            name = "Blog / Newsletter",
            formats = listOf("Article", "Newsletter"),
            defaultFormat = "Article",
            capabilities = CreatorPlatformCapabilities(
                supportsImages = true,
                supportsText = true,
                supportsArticles = true,
            ),
            aliases = setOf("Blog", "Newsletter"),
        ),
        CreatorPlatformDefinition(
            name = "Other",
            formats = listOf("Content"),
            defaultFormat = "Content",
            capabilities = CreatorPlatformCapabilities(
                supportsVideo = true,
                supportsShortVideo = true,
                supportsImages = true,
                supportsText = true,
                supportsAudio = true,
                supportsLive = true,
                supportsArticles = true,
                supportsStories = true,
            ),
        ),
    )

    val supportedPlatforms: List<String> get() = definitions.map { it.name }

    fun findDefinition(platform: String): CreatorPlatformDefinition? {
        val value = platform.trim()
        if (value.isBlank()) return null
        return definitions.firstOrNull { definition ->
            definition.name.equals(value, ignoreCase = true) ||
                definition.aliases.any { it.equals(value, ignoreCase = true) }
        }
    }

    /** Compatibility API: unknown/custom platforms still resolve to the broad Other definition. */
    fun definition(platform: String): CreatorPlatformDefinition =
        findDefinition(platform) ?: definitions.last()

    fun canonicalName(platform: String): String {
        val value = platform.trim()
        if (value.isBlank()) return ""
        return findDefinition(value)?.name ?: value.take(60)
    }

    /** Current formats only; legacy values are intentionally not reintroduced into new-work menus. */
    fun formats(platform: String): List<String> = definition(platform).formats

    fun formatOptions(platform: String, currentFormat: String? = null): List<String> =
        definition(platform).formatOptions(currentFormat)

    fun acceptsFormat(platform: String, format: String): Boolean {
        val known = findDefinition(platform)
        // Custom platforms remain permissive so existing creator-defined workflows are not broken.
        return known?.acceptsFormat(format) ?: format.trim().isNotBlank()
    }

    fun capabilities(platform: String): CreatorPlatformCapabilities = definition(platform).capabilities

    fun primaryPlatform(profile: CreatorProfile): String {
        val selected = orderedSelected(profile)
        return selected.firstOrNull() ?: "YouTube"
    }

    fun defaultFormat(platform: String): String = definition(platform).defaultFormat

    fun orderedSelected(profile: CreatorProfile, include: String? = null): List<String> {
        val selected = profile.platforms
            .map(::canonicalName)
            .filter { it.isNotBlank() }
            .toMutableSet()
        include?.let(::canonicalName)?.takeIf { it.isNotBlank() }?.let(selected::add)
        val ordered = definitions.map { it.name }.filter { wanted -> selected.any { it.equals(wanted, ignoreCase = true) } }
        val unknown = selected.filter { value -> definitions.none { it.name.equals(value, ignoreCase = true) } }.sorted()
        return (ordered + unknown).ifEmpty { listOf("YouTube") }
    }
}
