package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine

internal enum class V18CreatorLoopAction {
    CAPTURE_NEXT_IDEA,
    REVIEW_INSIGHTS,
    START_NEXT_PROJECT,
}

/**
 * Alpha 13 closes the creator loop after publishing instead of ending the journey.
 * This policy stays deterministic/offline-first and can later be upgraded with richer signals.
 */
internal object V18CreatorLoop {
    fun afterPublished(task: CreatorTask, hasIdeas: Boolean): V18CreatorLoopAction {
        val template = CreatorWorkflowEngine.templateFor(task)
        val stage = CreatorWorkflowEngine.stageIndex(task)
        require(CreatorWorkflowEngine.isPublicationStage(template.stages[stage]) || stage >= template.stages.lastIndex) {
            "Creator loop should only run when a project reaches its final workflow stage"
        }
        return if (hasIdeas) V18CreatorLoopAction.REVIEW_INSIGHTS else V18CreatorLoopAction.CAPTURE_NEXT_IDEA
    }

    fun afterInsightsReviewed(hasReadyIdea: Boolean): V18CreatorLoopAction =
        if (hasReadyIdea) V18CreatorLoopAction.START_NEXT_PROJECT else V18CreatorLoopAction.CAPTURE_NEXT_IDEA
}
