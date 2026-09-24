package com.framebynavin.app.data

enum class CreatorMediaSourceAuthority {
    OFFICIAL,
    PLATFORM,
    TRADE,
    REPUTABLE_NEWS,
}

data class CreatorMediaSource(
    val id: String,
    val label: String,
    val languages: Set<String> = emptySet(),
    val authority: CreatorMediaSourceAuthority,
    val recommendationWeight: Int = 0,
    val active: Boolean = true,
)

data class CreatorMediaSourceSelection(
    val recommended: List<CreatorMediaSource>,
    val allEligible: List<CreatorMediaSource>,
) {
    fun visible(expanded: Boolean): List<CreatorMediaSource> =
        if (expanded) allEligible else recommended

    val hasMore: Boolean
        get() = allEligible.size > recommended.size
}

/**
 * Ranks an externally maintained source catalog for onboarding and Settings.
 *
 * The engine deliberately does not manufacture a fallback list. A source must exist in the verified
 * catalog before it can be shown. Language-specific official/platform sources rank first, then trade
 * and reputable-news sources. Empty-language sources are treated as cross-language sources.
 */
object CreatorMediaSourceRecommendationEngine {
    const val DEFAULT_RECOMMENDED_COUNT = 5

    fun build(
        catalog: List<CreatorMediaSource>,
        preferredLanguages: Set<String>,
        recommendedCount: Int = DEFAULT_RECOMMENDED_COUNT,
    ): CreatorMediaSourceSelection {
        val preferred = preferredLanguages
            .map(CreatorMediaLanguageRegistry::canonicalLabel)
            .filter(String::isNotBlank)
            .map(String::lowercase)
            .toSet()
        val safeCount = recommendedCount.coerceIn(1, 20)

        val eligible = catalog
            .asSequence()
            .filter { it.active && it.id.isNotBlank() && it.label.isNotBlank() }
            .distinctBy { it.id.trim().lowercase() }
            .filter { source ->
                val sourceLanguages = source.languages
                    .map(CreatorMediaLanguageRegistry::canonicalLabel)
                    .filter(String::isNotBlank)
                    .map(String::lowercase)
                    .toSet()
                preferred.isEmpty() || sourceLanguages.isEmpty() || sourceLanguages.any(preferred::contains)
            }
            .sortedWith(
                compareByDescending<CreatorMediaSource> { score(it, preferred) }
                    .thenBy { it.label.lowercase() }
            )
            .toList()

        return CreatorMediaSourceSelection(
            recommended = eligible.take(safeCount),
            allEligible = eligible,
        )
    }

    private fun score(source: CreatorMediaSource, preferred: Set<String>): Int {
        val sourceLanguages = source.languages
            .map(CreatorMediaLanguageRegistry::canonicalLabel)
            .filter(String::isNotBlank)
            .map(String::lowercase)
            .toSet()
        val languageScore = when {
            preferred.isEmpty() -> 20
            sourceLanguages.any(preferred::contains) -> 100
            sourceLanguages.isEmpty() -> 35
            else -> 0
        }
        val authorityScore = when (source.authority) {
            CreatorMediaSourceAuthority.OFFICIAL -> 55
            CreatorMediaSourceAuthority.PLATFORM -> 50
            CreatorMediaSourceAuthority.TRADE -> 40
            CreatorMediaSourceAuthority.REPUTABLE_NEWS -> 30
        }
        return languageScore + authorityScore + source.recommendationWeight.coerceIn(-20, 20)
    }
}
