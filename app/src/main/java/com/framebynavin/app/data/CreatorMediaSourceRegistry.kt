package com.framebynavin.app.data

/**
 * Curated starter catalog for V147 onboarding.
 *
 * Keep this list conservative: entries are added only after the channel/source is identified as an
 * active first-party or established industry source. The recommendation engine is catalog-size
 * agnostic, so this can grow to the full maintained source set without changing onboarding UX.
 */
object CreatorMediaSourceRegistry {
    val sources: List<CreatorMediaSource> = listOf(
        CreatorMediaSource(
            id = "youtube-mythri-movie-makers",
            label = "Mythri Movie Makers",
            languages = setOf("Telugu"),
            authority = CreatorMediaSourceAuthority.OFFICIAL,
            recommendationWeight = 18,
        ),
        CreatorMediaSource(
            id = "youtube-haarika-hassine",
            label = "Haarika & Hassine Creations",
            languages = setOf("Telugu"),
            authority = CreatorMediaSourceAuthority.OFFICIAL,
            recommendationWeight = 16,
        ),
        CreatorMediaSource(
            id = "youtube-sithara-entertainments",
            label = "Sithara Entertainments",
            languages = setOf("Telugu", "Tamil"),
            authority = CreatorMediaSourceAuthority.OFFICIAL,
            recommendationWeight = 17,
        ),
        CreatorMediaSource(
            id = "youtube-vyjayanthi-network",
            label = "Vyjayanthi Network",
            languages = setOf("Telugu"),
            authority = CreatorMediaSourceAuthority.OFFICIAL,
            recommendationWeight = 15,
        ),
        CreatorMediaSource(
            id = "youtube-telugu-filmnagar",
            label = "Telugu Filmnagar",
            languages = setOf("Telugu"),
            authority = CreatorMediaSourceAuthority.REPUTABLE_NEWS,
            recommendationWeight = 8,
        ),
        CreatorMediaSource(
            id = "youtube-netflix-india",
            label = "Netflix India",
            languages = emptySet(),
            authority = CreatorMediaSourceAuthority.PLATFORM,
            recommendationWeight = 14,
        ),
        CreatorMediaSource(
            id = "youtube-prime-video-india",
            label = "Prime Video India",
            languages = emptySet(),
            authority = CreatorMediaSourceAuthority.PLATFORM,
            recommendationWeight = 14,
        ),
        CreatorMediaSource(
            id = "youtube-sun-nxt",
            label = "Sun NXT",
            languages = setOf("Tamil", "Telugu", "Malayalam", "Kannada"),
            authority = CreatorMediaSourceAuthority.PLATFORM,
            recommendationWeight = 13,
        ),
    )

    fun byId(id: String): CreatorMediaSource? =
        sources.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }
}
