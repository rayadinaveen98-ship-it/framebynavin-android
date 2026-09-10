package com.framebynavin.app.data

import java.util.UUID

enum class CreatorDeliverableStatus { PLANNED, READY, PUBLISHED }
enum class CreatorChecklistStatus { TODO, DONE, SKIPPED }
enum class CreatorAssetKind { IMAGE, VIDEO, AUDIO, DOCUMENT, LINK, OTHER }

data class CreatorProjectReference(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val url: String = "",
)

data class CreatorChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val status: CreatorChecklistStatus = CreatorChecklistStatus.TODO,
)

data class CreatorProjectAsset(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    /** A content URI, path or web reference. Media bytes are never stored in the project JSON. */
    val location: String = "",
    val kind: CreatorAssetKind = CreatorAssetKind.OTHER,
    val notes: String = "",
)

data class CreatorDeliverable(
    val id: String = UUID.randomUUID().toString(),
    val platform: String,
    val format: String,
    val title: String = "",
    val status: CreatorDeliverableStatus = CreatorDeliverableStatus.PLANNED,
    val deadlineLabel: String = "",
    val description: String = "",
    val tags: String = "",
    val thumbnailConcept: String = "",
    /** Optional relationship for a Short/Reel/other derivative created from another deliverable. */
    val parentDeliverableId: String = "",
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
    val checklist: List<CreatorChecklistItem> = emptyList(),
    val assets: List<CreatorProjectAsset> = emptyList(),
    val deliverables: List<CreatorDeliverable> = emptyList(),
    val learnings: String = "",
) {
    fun isEmpty(): Boolean = audience.isBlank() && viewerProblem.isBlank() && promise.isBlank() &&
        angle.isBlank() && hook.isBlank() && script.isBlank() && references.isEmpty() && checklist.isEmpty() &&
        assets.isEmpty() && deliverables.isEmpty() && learnings.isBlank()
}
