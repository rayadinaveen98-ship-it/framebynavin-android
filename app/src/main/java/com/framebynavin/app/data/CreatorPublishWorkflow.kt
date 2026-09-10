package com.framebynavin.app.data

import java.util.UUID

object CreatorPublishWorkflow {
    fun defaultGate(platform: String, format: String): List<CreatorPublishGateItem> {
        val items = mutableListOf(
            "Final export reviewed",
            "Final title / caption chosen",
            "Thumbnail / cover reviewed",
            "Rights, music and source check",
            "Platform metadata reviewed",
        )
        val p = platform.trim().lowercase()
        val f = format.trim().lowercase()
        when {
            p == "youtube" && f.contains("long") -> items += "Chapters / end screen / cards checked"
            p == "youtube" && f.contains("short") -> items += "Vertical crop and first-frame check"
            p == "instagram" -> items += "Cover crop and safe-area check"
        }
        return items.map { title ->
            CreatorPublishGateItem(id = UUID.randomUUID().toString(), title = title)
        }
    }

    fun ensureGate(deliverable: CreatorDeliverable): CreatorDeliverable =
        if (deliverable.publishGate.isNotEmpty()) deliverable
        else deliverable.copy(publishGate = defaultGate(deliverable.platform, deliverable.format))

    fun unresolvedRequired(deliverable: CreatorDeliverable): List<CreatorPublishGateItem> =
        ensureGate(deliverable).publishGate.filter {
            it.required && it.status == CreatorPublishGateStatus.TODO
        }

    fun resolvedGateCount(deliverable: CreatorDeliverable): Int =
        ensureGate(deliverable).publishGate.count { it.status != CreatorPublishGateStatus.TODO }

    fun isReadyToPublish(deliverable: CreatorDeliverable): Boolean {
        val normalized = ensureGate(deliverable)
        return normalized.title.isNotBlank() && unresolvedRequired(normalized).isEmpty()
    }

    fun validPublicationUrl(url: String): Boolean =
        url.isBlank() || url.trim().startsWith("https://", ignoreCase = true)

    fun publish(
        deliverable: CreatorDeliverable,
        atMillis: Long,
        url: String,
        note: String = "",
    ): CreatorDeliverable {
        val normalized = ensureGate(deliverable)
        require(isReadyToPublish(normalized)) { "Resolve the required pre-publish checks and choose a final title." }
        require(atMillis > 0L) { "Publication time is required." }
        require(validPublicationUrl(url)) { "Publication links must use HTTPS." }
        val cleanUrl = url.trim()
        val event = snapshot(
            deliverable = normalized,
            kind = CreatorPublicationEventKind.PUBLISHED,
            atMillis = atMillis,
            url = cleanUrl,
            note = note,
        )
        return normalized.copy(
            status = CreatorDeliverableStatus.PUBLISHED,
            publishedAtMillis = atMillis,
            publishedUrl = cleanUrl,
            publicationHistory = normalized.publicationHistory + event,
        )
    }

    fun recordPublishedUpdate(
        deliverable: CreatorDeliverable,
        atMillis: Long,
        url: String = deliverable.publishedUrl,
        note: String = "",
    ): CreatorDeliverable {
        require(deliverable.status == CreatorDeliverableStatus.PUBLISHED) { "Only a published deliverable can record a publication update." }
        require(atMillis > 0L) { "Update time is required." }
        require(validPublicationUrl(url)) { "Publication links must use HTTPS." }
        val cleanUrl = url.trim()
        val event = snapshot(
            deliverable = deliverable,
            kind = CreatorPublicationEventKind.UPDATED,
            atMillis = atMillis,
            url = cleanUrl,
            note = note,
        )
        return deliverable.copy(
            publishedUrl = cleanUrl,
            publicationHistory = deliverable.publicationHistory + event,
        )
    }

    fun reopen(
        deliverable: CreatorDeliverable,
        atMillis: Long,
        note: String = "",
    ): CreatorDeliverable {
        require(deliverable.status == CreatorDeliverableStatus.PUBLISHED) { "Only a published deliverable can be reopened." }
        val event = snapshot(
            deliverable = deliverable,
            kind = CreatorPublicationEventKind.REOPENED,
            atMillis = atMillis,
            url = deliverable.publishedUrl,
            note = note,
        )
        return deliverable.copy(
            status = CreatorDeliverableStatus.READY,
            publishedAtMillis = 0L,
            publishedUrl = "",
            publicationHistory = deliverable.publicationHistory + event,
        )
    }

    fun createDerivative(
        parent: CreatorDeliverable,
        platform: String,
        format: String,
    ): CreatorDeliverable {
        val draft = CreatorDeliverable(
            id = UUID.randomUUID().toString(),
            platform = platform.trim().ifBlank { "Other" },
            format = format.trim().ifBlank { "Derivative" },
            title = parent.title,
            status = CreatorDeliverableStatus.PLANNED,
            tags = parent.tags,
            parentDeliverableId = parent.id,
            titleVariants = parent.titleVariants,
        )
        return ensureGate(draft)
    }

    private fun snapshot(
        deliverable: CreatorDeliverable,
        kind: CreatorPublicationEventKind,
        atMillis: Long,
        url: String,
        note: String,
    ): CreatorPublicationEvent = CreatorPublicationEvent(
        id = UUID.randomUUID().toString(),
        kind = kind,
        atMillis = atMillis,
        titleSnapshot = deliverable.title.trim(),
        thumbnailSnapshot = deliverable.thumbnailConcept.trim(),
        descriptionSnapshot = deliverable.description.trimEnd(),
        tagsSnapshot = deliverable.tags.trim(),
        url = url.trim(),
        note = note.trim(),
    )
}
