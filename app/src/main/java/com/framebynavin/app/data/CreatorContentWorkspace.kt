package com.framebynavin.app.data

import java.util.UUID

enum class CreatorDeliverableStatus { PLANNED, READY, PUBLISHED }

data class CreatorProjectReference(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val url: String = "",
)

data class CreatorDeliverable(
    val id: String = UUID.randomUUID().toString(),
    val platform: String,
    val format: String,
    val title: String = "",
    val status: CreatorDeliverableStatus = CreatorDeliverableStatus.PLANNED,
    val publishedAtMillis: Long = 0L,
    val publishedUrl: String = "",
)

data class CreatorContentWorkspace(
    val revision: Long = 0L,
    val audience: String = "",
    val viewerProblem: String = "",
    val promise: String = "",
    val angle: String = "",
    val hook: String = "",
    val script: String = "",
    val references: List<CreatorProjectReference> = emptyList(),
    val deliverables: List<CreatorDeliverable> = emptyList(),
) {
    fun isEmpty(): Boolean = audience.isBlank() && viewerProblem.isBlank() && promise.isBlank() &&
        angle.isBlank() && hook.isBlank() && script.isBlank() && references.isEmpty() && deliverables.isEmpty()
}
