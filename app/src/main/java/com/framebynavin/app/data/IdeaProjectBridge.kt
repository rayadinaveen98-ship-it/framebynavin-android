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
}
