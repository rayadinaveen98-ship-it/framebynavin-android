package com.backlot.shared.workflow

/** Platform-neutral Content DNA used by Android and iOS. */
data class BacklotContentDna(
    val creatorModeId: String = "",
    val archetypeId: String = "",
    val archetypeLabel: String = "",
    val productionStyles: Set<String> = emptySet(),
    val platform: String = "",
    val deliveryFormat: String = "",
) {
    val isEmpty: Boolean
        get() = creatorModeId.isBlank() &&
            archetypeId.isBlank() &&
            productionStyles.isEmpty() &&
            platform.isBlank() &&
            deliveryFormat.isBlank()

    fun normalized(): BacklotContentDna = copy(
        creatorModeId = creatorModeId.trim(),
        archetypeId = archetypeId.trim(),
        archetypeLabel = archetypeLabel.trim(),
        productionStyles = productionStyles.map(String::trim).filter(String::isNotBlank).toSet(),
        platform = platform.trim(),
        deliveryFormat = deliveryFormat.trim(),
    )
}

/** Only product-level fields needed to resolve a workflow. No Android types are allowed here. */
data class BacklotProjectDescriptor(
    val platform: String = "",
    val contentType: String = "",
    val contentDna: BacklotContentDna = BacklotContentDna(),
)

data class BacklotWorkflowStage(
    val id: String,
    val label: String,
    val action: String,
)

data class BacklotWorkflowTemplate(
    val id: String,
    val label: String,
    val stages: List<BacklotWorkflowStage>,
)
