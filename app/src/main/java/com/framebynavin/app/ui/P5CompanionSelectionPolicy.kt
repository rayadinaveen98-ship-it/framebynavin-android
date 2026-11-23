package com.framebynavin.app.ui

/** Point 5 uses ONLY the two already-approved Spark and Mira sprite packs. */
internal enum class P5CompanionPose(val label: String) {
    WELCOME("Hello"),
    THINKING("Thinking"),
    CELEBRATE("Celebrate"),
}

internal object P5CompanionSelectionPolicy {
    fun previewable(name: String?): V144GuideIdentity =
        V145SelectableGuides.firstOrNull { it.name == name } ?: V144GuideIdentity.FUNNY

    fun characterState(pose: P5CompanionPose): CinePulseState = when (pose) {
        P5CompanionPose.WELCOME -> CinePulseState.WAVE
        P5CompanionPose.THINKING -> CinePulseState.THINK
        P5CompanionPose.CELEBRATE -> CinePulseState.CELEBRATE
    }

    fun canCommit(preview: V144GuideIdentity): Boolean =
        preview in V145SelectableGuides
}
