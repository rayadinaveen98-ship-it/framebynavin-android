package com.framebynavin.app.data

import java.util.Locale

/**
 * Local, non-blocking hint; not AI scoring and never merges or deletes a creator's work.
 * Handles Unicode word boundaries so Indian-language titles remain searchable.
 */
object IdeaVaultGuidance {
    private fun words(text: String): List<String> = text.lowercase(Locale.ROOT)
        .split(Regex("[^\\p{L}\\p{Nd}]+"))
        .filter { it.isNotBlank() }

    fun similarTitles(
        draftTitle: String,
        ideas: List<CreatorIdea>,
        excludingId: String = "",
    ): List<CreatorIdea> {
        val draft = words(draftTitle)
        if (draft.joinToString(" ").length < 5) return emptyList()
        val draftSet = draft.toSet()
        return ideas.filter { idea ->
            if (idea.isTrashed || idea.id == excludingId) return@filter false
            val candidate = words(idea.title)
            if (candidate.isEmpty()) return@filter false
            if (candidate == draft) return@filter true
            val candidateSet = candidate.toSet()
            val intersection = draftSet.intersect(candidateSet).size.toDouble()
            val union = draftSet.union(candidateSet).size.toDouble()
            draftSet.size >= 3 && candidateSet.size >= 3 &&
                union > 0 && intersection / union >= 0.8
        }.take(3)
    }
}
