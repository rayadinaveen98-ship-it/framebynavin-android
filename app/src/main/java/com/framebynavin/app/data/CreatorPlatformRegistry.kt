package com.framebynavin.app.data

data class CreatorPlatformDefinition(
    val name: String,
    val formats: List<String>,
    val defaultFormat: String = formats.first(),
)

/**
 * Single source of truth for creator platforms exposed by setup and creation surfaces.
 * Platform choices now materially drive project defaults, formats and workflow selection.
 */
object CreatorPlatformRegistry {
    val definitions: List<CreatorPlatformDefinition> = listOf(
        CreatorPlatformDefinition("YouTube", listOf("Long-form", "Short", "Cinematic Moment", "Video"), "Long-form"),
        CreatorPlatformDefinition("Instagram", listOf("Reel", "Post", "Story"), "Reel"),
        CreatorPlatformDefinition("X", listOf("Post", "Video", "Update"), "Post"),
        CreatorPlatformDefinition("Facebook", listOf("Post", "Reel", "Video"), "Post"),
        CreatorPlatformDefinition("LinkedIn", listOf("Post", "Article", "Video"), "Post"),
        CreatorPlatformDefinition("Podcast", listOf("Episode", "Clip"), "Episode"),
        CreatorPlatformDefinition("Blog / Newsletter", listOf("Article", "Newsletter"), "Article"),
        CreatorPlatformDefinition("Other", listOf("Content"), "Content"),
    )

    val supportedPlatforms: List<String> get() = definitions.map { it.name }

    fun definition(platform: String): CreatorPlatformDefinition = definitions.firstOrNull {
        it.name.equals(platform.trim(), ignoreCase = true)
    } ?: definitions.last()

    fun formats(platform: String): List<String> = definition(platform).formats

    fun primaryPlatform(profile: CreatorProfile): String {
        val selected = orderedSelected(profile)
        return selected.firstOrNull() ?: "YouTube"
    }

    fun defaultFormat(platform: String): String = definition(platform).defaultFormat

    fun orderedSelected(profile: CreatorProfile, include: String? = null): List<String> {
        val selected = profile.platforms.map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
        include?.trim()?.takeIf { it.isNotBlank() }?.let(selected::add)
        val ordered = definitions.map { it.name }.filter { wanted -> selected.any { it.equals(wanted, ignoreCase = true) } }
        val unknown = selected.filter { value -> definitions.none { it.name.equals(value, ignoreCase = true) } }.sorted()
        return (ordered + unknown).ifEmpty { listOf("YouTube") }
    }
}
