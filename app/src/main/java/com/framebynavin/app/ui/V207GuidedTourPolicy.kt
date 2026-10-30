package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorGuidedTourStep

/**
 * Four focused optional lessons. Keep legacy enum values and the previously
 * shipped tour policy intact for old saved states and external API contracts.
 */
internal object V207GuidedTourPolicy {
    val lessons = listOf(
        CreatorGuidedTourStep.TODAY,
        CreatorGuidedTourStep.IDEAS,
        CreatorGuidedTourStep.PROJECT,
        CreatorGuidedTourStep.INSIGHTS,
    )

    fun indexOf(step: CreatorGuidedTourStep): Int = when (step) {
        CreatorGuidedTourStep.WORKSPACE -> 2
        CreatorGuidedTourStep.CONTROL -> 3
        else -> lessons.indexOf(step).coerceAtLeast(0)
    }

    fun next(step: CreatorGuidedTourStep): CreatorGuidedTourStep? =
        lessons.getOrNull(indexOf(step) + 1)

    fun previous(step: CreatorGuidedTourStep): CreatorGuidedTourStep? =
        lessons.getOrNull(indexOf(step) - 1)

    fun isLast(step: CreatorGuidedTourStep) = indexOf(step) == lessons.lastIndex
}
