package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorGuidedTourStep

/**
 * Six guided lessons using the existing stable enum IDs and real app surfaces.
 * Completed legacy tours are not restarted by this change.
 */
internal object V207GuidedTourPolicy {
    val lessons = listOf(
        CreatorGuidedTourStep.TODAY,
        CreatorGuidedTourStep.IDEAS,
        CreatorGuidedTourStep.PROJECT,
        CreatorGuidedTourStep.WORKSPACE,
        CreatorGuidedTourStep.INSIGHTS,
        CreatorGuidedTourStep.CONTROL,
    )

    fun indexOf(step: CreatorGuidedTourStep): Int = lessons.indexOf(step).coerceAtLeast(0)

    fun next(step: CreatorGuidedTourStep): CreatorGuidedTourStep? =
        lessons.getOrNull(indexOf(step) + 1)

    fun previous(step: CreatorGuidedTourStep): CreatorGuidedTourStep? =
        lessons.getOrNull(indexOf(step) - 1)

    fun isLast(step: CreatorGuidedTourStep) = indexOf(step) == lessons.lastIndex
}
