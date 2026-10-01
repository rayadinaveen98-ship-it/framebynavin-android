package com.framebynavin.app.data

/** Keeps Idea Vault context intact when an idea becomes a creator project. */
object IdeaProjectBridge {
    fun projectNotes(idea: CreatorIdea): String = buildString {
        append("From Idea Vault")
        if (idea.topic.isNotBlank()) append(" · ${idea.topic.trim()}")
        if (idea.notes.isNotBlank()) append("\n${idea.notes.trim()}")
        if (idea.transcript.isNotBlank()) {
            append("\n\nVoice transcript\n")
            append(idea.transcript.trim())
        }
    }

    /** Resolve the canonical source idea without copying mutable idea state into the project. */
    fun sourceIdea(task: CreatorTask, ideas: List<CreatorIdea>): CreatorIdea? {
        if (task.origin != CreatorTaskOrigin.IDEA_VAULT || task.sourceRefId.isBlank()) return null
        return ideas.firstOrNull { it.id == task.sourceRefId }
    }

    /** A project-side voice surface should only appear while its original local recording exists. */
    fun sourceVoiceIdea(task: CreatorTask, ideas: List<CreatorIdea>): CreatorIdea? =
        sourceIdea(task, ideas)?.takeIf { it.hasOriginalRecording }
}
