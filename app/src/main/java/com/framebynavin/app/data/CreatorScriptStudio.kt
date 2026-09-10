package com.framebynavin.app.data

import java.util.UUID

enum class CreatorScriptStatus { IDEA, DRAFTING, REVIEW, READY_TO_RECORD, RECORDED }
enum class CreatorScriptBeatStatus { DRAFT, READY, LOCKED }

data class CreatorHookIdea(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val selected: Boolean = false,
)

data class CreatorTitleIdea(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val selected: Boolean = false,
)

data class CreatorScriptBeat(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val purpose: String = "",
    val narration: String = "",
    val visualNotes: String = "",
    val bRollNotes: String = "",
    val onScreenText: String = "",
    val status: CreatorScriptBeatStatus = CreatorScriptBeatStatus.DRAFT,
)

data class CreatorScriptStudio(
    val projectId: String,
    val revision: Long = 0L,
    val status: CreatorScriptStatus = CreatorScriptStatus.DRAFTING,
    val hooks: List<CreatorHookIdea> = emptyList(),
    val titles: List<CreatorTitleIdea> = emptyList(),
    val beats: List<CreatorScriptBeat> = emptyList(),
    val creatorNotes: String = "",
) {
    fun selectedHook(): String = hooks.firstOrNull { it.selected }?.text?.trim().orEmpty()
        .ifBlank { hooks.firstOrNull { it.text.isNotBlank() }?.text?.trim().orEmpty() }

    fun selectedTitle(): String = titles.firstOrNull { it.selected }?.text?.trim().orEmpty()
        .ifBlank { titles.firstOrNull { it.text.isNotBlank() }?.text?.trim().orEmpty() }

    fun compiledNarration(): String = beats
        .map { it.narration.trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n\n")

    fun wordCount(): Int = compiledNarration()
        .split(Regex("\\s+"))
        .count { it.isNotBlank() }

    fun readyBeatCount(): Int = beats.count { it.status != CreatorScriptBeatStatus.DRAFT }

    fun progressPercent(): Int {
        if (beats.isEmpty()) return 0
        return ((readyBeatCount() * 100f) / beats.size).toInt().coerceIn(0, 100)
    }

    fun isEmpty(): Boolean = hooks.none { it.text.isNotBlank() } && titles.none { it.text.isNotBlank() } &&
        beats.none { beat -> beat.label.isNotBlank() || beat.purpose.isNotBlank() || beat.narration.isNotBlank() ||
            beat.visualNotes.isNotBlank() || beat.bRollNotes.isNotBlank() || beat.onScreenText.isNotBlank() } &&
        creatorNotes.isBlank()

    /** Canonical normalization shared by the embedded project copy and the Alpha4/5 migration store. */
    fun normalized(projectId: String = this.projectId, revision: Long = this.revision): CreatorScriptStudio {
        val cleanHooks = hooks
            .map { it.copy(text = it.text.trim()) }
            .filter { it.text.isNotBlank() }
            .distinctBy { it.id }
            .let { list ->
                val selectedId = list.firstOrNull { it.selected }?.id
                if (selectedId == null) list else list.map { it.copy(selected = it.id == selectedId) }
            }
        val cleanTitles = titles
            .map { it.copy(text = it.text.trim()) }
            .filter { it.text.isNotBlank() }
            .distinctBy { it.id }
            .let { list ->
                val selectedId = list.firstOrNull { it.selected }?.id
                if (selectedId == null) list else list.map { it.copy(selected = it.id == selectedId) }
            }
        val cleanBeats = beats
            .map {
                it.copy(
                    label = it.label.trim(),
                    purpose = it.purpose.trim(),
                    narration = it.narration.trimEnd(),
                    visualNotes = it.visualNotes.trimEnd(),
                    bRollNotes = it.bRollNotes.trimEnd(),
                    onScreenText = it.onScreenText.trimEnd(),
                )
            }
            .filter { beat ->
                beat.label.isNotBlank() || beat.purpose.isNotBlank() || beat.narration.isNotBlank() ||
                    beat.visualNotes.isNotBlank() || beat.bRollNotes.isNotBlank() || beat.onScreenText.isNotBlank()
            }
            .distinctBy { it.id }
        return copy(
            projectId = projectId,
            revision = revision.coerceAtLeast(0L),
            hooks = cleanHooks,
            titles = cleanTitles,
            beats = cleanBeats,
            creatorNotes = creatorNotes.trimEnd(),
        )
    }

    companion object {
        fun fromLegacy(projectId: String, hook: String, script: String): CreatorScriptStudio {
            val hooks = hook.trim().takeIf { it.isNotBlank() }?.let {
                listOf(CreatorHookIdea(text = it, selected = true))
            }.orEmpty()
            val beats = script.trim().takeIf { it.isNotBlank() }?.let {
                listOf(
                    CreatorScriptBeat(
                        label = "Legacy script",
                        purpose = "Imported from the original Content Project script field",
                        narration = it,
                    )
                )
            }.orEmpty()
            return CreatorScriptStudio(projectId = projectId, hooks = hooks, beats = beats)
        }
    }
}
