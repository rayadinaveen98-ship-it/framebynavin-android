package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderCenterGroup
import java.text.Normalizer
import java.util.Locale

/**
 * Offline, read-only search over the SAME tasks the Reminders Center has already
 * classified. No network, persistence or reminder engine side effects.
 *
 * Unicode NFC preserves Telugu/Indic dependent vowel signs; stripping all
 * combining marks would silently corrupt searches in these languages.
 */
internal object V219ReminderSearchPolicy {
    private val spaces = Regex("\\s+")

    fun terms(query: String): List<String> =
        Normalizer.normalize(query.trim(), Normalizer.Form.NFC)
            .lowercase(Locale.ROOT)
            .split(spaces)
            .filter { it.isNotBlank() }

    fun matches(task: CreatorTask, queryTerms: List<String>): Boolean {
        if (queryTerms.isEmpty()) return true
        val searchable = Normalizer.normalize(
            listOf(task.title, task.platform, task.contentType, task.notes)
                .joinToString(" "),
            Normalizer.Form.NFC,
        ).lowercase(Locale.ROOT)
        return queryTerms.all(searchable::contains)
    }

    /**
     * Preserve ReminderCenterPolicy's canonical category, order and task
     * identity. Every action still receives the original CreatorTask and the
     * ViewModel validates its occurrence fingerprint before any mutation.
     */
    fun filter(
        grouped: Map<ReminderCenterGroup, List<CreatorTask>>,
        query: String,
    ): Map<ReminderCenterGroup, List<CreatorTask>> {
        val words = terms(query)
        if (words.isEmpty()) return grouped
        return grouped.mapValues { (_, tasks) -> tasks.filter { matches(it, words) } }
    }

    fun visibleIds(
        grouped: Map<ReminderCenterGroup, List<CreatorTask>>,
        active: V219ReminderHubFilter,
    ): Set<String> = grouped.entries
        .filter { active.includes(it.key) }
        .flatMap { it.value }
        .mapTo(linkedSetOf()) { it.id }
}
