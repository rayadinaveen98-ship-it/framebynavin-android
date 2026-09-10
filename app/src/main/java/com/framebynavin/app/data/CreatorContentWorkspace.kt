package com.framebynavin.app.data

import java.util.UUID

enum class CreatorDeliverableStatus { PLANNED, READY, PUBLISHED }
enum class CreatorChecklistStatus { TODO, DONE, SKIPPED }
enum class CreatorAssetKind { IMAGE, VIDEO, AUDIO, DOCUMENT, LINK, OTHER }
enum class CreatorPublishGateStatus { TODO, DONE, SKIPPED }
enum class CreatorPublicationEventKind { PUBLISHED, UPDATED, REOPENED }

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

data class CreatorVariantIdea(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
)

data class CreatorPublishGateItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val status: CreatorPublishGateStatus = CreatorPublishGateStatus.TODO,
    val required: Boolean = true,
)

data class CreatorPublicationEvent(
    val id: String = UUID.randomUUID().toString(),
    val kind: CreatorPublicationEventKind = CreatorPublicationEventKind.PUBLISHED,
    val atMillis: Long = 0L,
    val titleSnapshot: String = "",
    val thumbnailSnapshot: String = "",
    val descriptionSnapshot: String = "",
    val tagsSnapshot: String = "",
    val url: String = "",
    val note: String = "",
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
    /** Alpha5 alternatives are retained even after one becomes the final title/cover. */
    val titleVariants: List<CreatorVariantIdea> = emptyList(),
    val thumbnailVariants: List<CreatorVariantIdea> = emptyList(),
    /** Pre-publish checks are deliverable-specific and never complete the parent project. */
    val publishGate: List<CreatorPublishGateItem> = emptyList(),
    /** Append-only publication/reopen/correction history for this output. */
    val publicationHistory: List<CreatorPublicationEvent> = emptyList(),
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
