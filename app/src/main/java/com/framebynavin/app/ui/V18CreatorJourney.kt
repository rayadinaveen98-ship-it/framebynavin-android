package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine

internal enum class V18JourneyDestination { IDEAS, CREATE, INSIGHTS }

/**
 * Small deterministic routing policy for the v1.8 creator journey.
 * Keeps post-action navigation intentional without coupling the data layer to Compose tabs.
 */
internal object V18CreatorJourney {
    fun afterCapture(): V18JourneyDestination = V18JourneyDestination.IDEAS

    fun afterProjectCreated(projectId: String?): V18JourneyDestination? =
        projectId?.takeIf { it.isNotBlank() }?.let { V18JourneyDestination.CREATE }

    fun afterWorkflowAdvance(taskBeforeAdvance: CreatorTask): V18JourneyDestination? {
        val template = CreatorWorkflowEngine.templateFor(taskBeforeAdvance)
        val stage = CreatorWorkflowEngine.stageIndex(taskBeforeAdvance)
        return if (CreatorWorkflowEngine.isPublicationStage(template.stages[stage]) || stage >= template.stages.lastIndex) V18JourneyDestination.INSIGHTS else null
    }
}
