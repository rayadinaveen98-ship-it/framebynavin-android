package com.backlot.shared

import com.backlot.shared.workflow.BacklotContentDna
import com.backlot.shared.workflow.BacklotProjectDescriptor
import com.backlot.shared.workflow.BacklotWorkflowResolver
import com.backlot.shared.workflow.BacklotWorkflowTemplate

/**
 * Stable, Swift-friendly entry point into Backlot shared product logic.
 *
 * Keep platform UIs behind this facade instead of teaching Swift/Android screens the internal
 * resolver graph. More shared capabilities can be added here as the migration proceeds.
 */
object BacklotSharedApi {
    const val CONTRACT_VERSION: Int = 1

    fun resolveWorkflow(
        creatorModeId: String,
        archetypeId: String,
        archetypeLabel: String,
        platform: String,
        deliveryFormat: String,
        legacyContentType: String,
    ): BacklotWorkflowTemplate? = BacklotWorkflowResolver.templateFor(
        BacklotProjectDescriptor(
            platform = platform,
            contentType = legacyContentType,
            contentDna = BacklotContentDna(
                creatorModeId = creatorModeId,
                archetypeId = archetypeId,
                archetypeLabel = archetypeLabel,
                platform = platform,
                deliveryFormat = deliveryFormat,
            ),
        )
    )
}
