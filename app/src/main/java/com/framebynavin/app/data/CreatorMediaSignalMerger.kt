package com.framebynavin.app.data

/**
 * Collapses source-specific observations into one canonical media signal before ranking/rendering.
 * Exact normalized title + kind + release date + platform form the conservative story key; we do
 * not fuzzy-merge different titles because false merges are worse than a duplicate card.
 */
object CreatorMediaSignalMerger {
    fun merge(signals: List<CreatorMediaSignal>): List<CreatorMediaSignal> = signals
        .asSequence()
        .filter { it.id.isNotBlank() && it.title.isNotBlank() && it.evidence.isNotEmpty() }
        .groupBy(::canonicalKey)
        .values
        .map(::mergeGroup)
        .sortedByDescending { it.publishedAtMillis }

    internal fun canonicalKey(signal: CreatorMediaSignal): String = listOf(
        signal.kind.name,
        normalizeText(signal.title),
        signal.releaseDate?.toString().orEmpty(),
        normalizeText(signal.platform),
    ).joinToString("|")

    private fun mergeGroup(group: List<CreatorMediaSignal>): CreatorMediaSignal {
        val newest = group.maxBy { it.publishedAtMillis }
        val evidence = group
            .flatMap { it.evidence }
            .distinctBy { item ->
                item.url.trim().lowercase().ifBlank {
                    item.id.trim().lowercase().ifBlank { item.label.trim().lowercase() }
                }
            }
        val languages = group.flatMap { it.languages }.map(String::trim).filter(String::isNotBlank).toSet()
        val summary = group
            .sortedByDescending { it.publishedAtMillis }
            .firstNotNullOfOrNull { it.summary.trim().takeIf(String::isNotBlank) }
            .orEmpty()
        val stableId = group.map { it.id.trim() }.filter(String::isNotBlank).sorted().first()

        return CreatorMediaVerificationPolicy.normalize(
            newest.copy(
                id = stableId,
                summary = summary,
                evidence = evidence,
                languages = languages,
                relevanceScore = group.maxOf { it.relevanceScore },
                audienceImpactScore = group.maxOf { it.audienceImpactScore },
            )
        )
    }

    private fun normalizeText(raw: String): String = raw
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")
}
