package com.framebynavin.app.data

import com.backlot.shared.workflow.BacklotContentDna
import com.backlot.shared.workflow.BacklotProjectDescriptor
import com.backlot.shared.workflow.BacklotWorkflowResolver

/**
 * Android adapter for the shared Backlot Workflow V2 engine.
 *
 * Android remains responsible for persisted CreatorTask compatibility and registry normalization.
 * The production workflow rules themselves now live in :shared so iOS consumes the same engine.
 */
object CreatorWorkflowV2 {
    fun templateFor(task: CreatorTask): WorkflowTemplate? {
        val dna = task.contentDna.normalized()
        val shared = BacklotWorkflowResolver.templateFor(
            BacklotProjectDescriptor(
                platform = task.platform,
                contentType = task.contentType,
                contentDna = BacklotContentDna(
                    creatorModeId = dna.creatorModeId,
                    archetypeId = dna.archetypeId,
                    archetypeLabel = ContentArchetypeRegistry.definition(dna.archetypeId)?.label.orEmpty(),
                    productionStyles = dna.productionStyles,
                    platform = dna.platform,
                    deliveryFormat = dna.deliveryFormat,
                ),
            )
        ) ?: return null

        return WorkflowTemplate(
            id = shared.id,
            label = shared.label,
            stages = shared.stages.map { stage ->
                WorkflowStage(
                    id = stage.id,
                    label = stage.label,
                    action = stage.action,
                )
            },
        )
    }
}
