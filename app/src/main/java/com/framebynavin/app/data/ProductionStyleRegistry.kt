package com.framebynavin.app.data

enum class CreatorProductionKind {
    NARRATION,
    PRESENTER,
    GAMEPLAY,
    SCREEN,
    CAMERA,
    LIVE,
    AUDIO,
    MOTION,
    WRITING,
    HYBRID,
}

data class CreatorProductionStyleDefinition(
    val id: String,
    val label: String,
    val kind: CreatorProductionKind,
    val description: String,
    val aliases: Set<String> = emptySet(),
)

/**
 * Canonical description of HOW creator content is produced.
 *
 * Production style is intentionally separate from creator mode, content archetype and platform.
 * Modes may recommend styles, but never restrict them. Unknown legacy/custom labels are preserved
 * during normalization instead of being silently rewritten to another production method.
 */
object ProductionStyleRegistry {
    val definitions: List<CreatorProductionStyleDefinition> = listOf(
        style(
            id = "voiceover",
            label = "Voiceover",
            kind = CreatorProductionKind.NARRATION,
            description = "Recorded narration over footage, frames, graphics or screen content.",
            aliases = arrayOf("Voice-over", "VO"),
        ),
        style(
            id = "talking_head",
            label = "Talking Head",
            kind = CreatorProductionKind.PRESENTER,
            description = "Creator speaks directly to camera as the primary presentation layer.",
            aliases = arrayOf("On Camera", "Presenter"),
        ),
        style(
            id = "gameplay_capture",
            label = "Gameplay Capture",
            kind = CreatorProductionKind.GAMEPLAY,
            description = "Game footage is captured as the primary source material.",
            aliases = arrayOf("Gameplay", "Game Capture"),
        ),
        style(
            id = "screen_recording",
            label = "Screen Recording",
            kind = CreatorProductionKind.SCREEN,
            description = "Desktop, mobile or application screen capture drives the content.",
            aliases = arrayOf("Screen Capture", "Screencast"),
        ),
        style(
            id = "camera_broll",
            label = "Camera / B-roll",
            kind = CreatorProductionKind.CAMERA,
            description = "Original camera footage or B-roll is a major production source.",
            aliases = arrayOf("Camera", "B-roll", "B Roll", "Camera/B-roll"),
        ),
        style(
            id = "livestream",
            label = "Livestream",
            kind = CreatorProductionKind.LIVE,
            description = "Content is produced live or primarily around a live broadcast.",
            aliases = arrayOf("Live Stream", "Live"),
        ),
        style(
            id = "audio_only",
            label = "Audio-only",
            kind = CreatorProductionKind.AUDIO,
            description = "Audio is the primary finished medium, such as podcasts or music-first work.",
            aliases = arrayOf("Audio Only", "Audio"),
        ),
        style(
            id = "animation_motion",
            label = "Animation / Motion",
            kind = CreatorProductionKind.MOTION,
            description = "Animation, motion graphics or generated visual movement is central to production.",
            aliases = arrayOf("Animation", "Motion Graphics", "Animation/Motion"),
        ),
        style(
            id = "writing",
            label = "Writing",
            kind = CreatorProductionKind.WRITING,
            description = "The finished work is primarily written or text-led.",
            aliases = arrayOf("Written", "Text"),
        ),
        style(
            id = "mixed_hybrid",
            label = "Mixed / Hybrid",
            kind = CreatorProductionKind.HYBRID,
            description = "Two or more production methods are intentionally combined without one dominant method.",
            aliases = arrayOf("Hybrid", "Mixed", "Mixed/Hybrid"),
        ),
    )

    private val byId = definitions.associateBy { it.id }

    val labels: List<String> get() = definitions.map { it.label }
    val ids: Set<String> get() = byId.keys

    fun definition(value: String): CreatorProductionStyleDefinition? {
        val normalized = value.trim()
        if (normalized.isBlank()) return null
        return byId[normalized.lowercase()] ?: definitions.firstOrNull { style ->
            style.label.equals(normalized, ignoreCase = true) ||
                style.aliases.any { it.equals(normalized, ignoreCase = true) }
        }
    }

    /** Preserve an unknown legacy/custom value rather than silently changing its meaning. */
    fun canonicalLabel(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return ""
        return definition(trimmed)?.label ?: trimmed.take(60)
    }

    fun orderedForMode(creatorMode: String): List<String> {
        val recommended = CreatorModeRegistry.suggestedProductionStyles(creatorMode)
            .map(::canonicalLabel)
            .filter { it.isNotBlank() }
        return (recommended + labels).distinct()
    }

    fun validateModeRegistry(): List<String> = buildList {
        CreatorModeRegistry.definitions.forEach { mode ->
            mode.suggestedProductionStyles.forEach { suggestion ->
                if (definition(suggestion) == null) add("${mode.label}: $suggestion")
            }
        }
    }

    private fun style(
        id: String,
        label: String,
        kind: CreatorProductionKind,
        description: String,
        aliases: Array<String> = emptyArray(),
    ) = CreatorProductionStyleDefinition(id, label, kind, description, aliases.toSet())
}
