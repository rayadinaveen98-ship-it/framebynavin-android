package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorDeliverable
import com.framebynavin.app.data.CreatorDeliverableStatus
import com.framebynavin.app.data.CreatorPublishGateStatus
import java.util.Locale

/**
 * Advisory-only publish preparation derived from saved deliverables. Never
 * edits their recorded status or treats "READY" as evidence of publication.
 */
internal object V2295PublishReadinessPolicy {
    fun hint(deliverables: List<CreatorDeliverable>): V2285WorkspaceFocusHint? {
        // A planned output is actionable before a ready one. PUBLISHED records
        // are historical evidence and should not be nagged to prepare again.
        val output = deliverables.firstOrNull { it.status == CreatorDeliverableStatus.PLANNED }
            ?: deliverables.firstOrNull {
                it.status == CreatorDeliverableStatus.READY &&
                    it.publishGate.any { gate ->
                        gate.required && gate.status == CreatorPublishGateStatus.TODO
                    }
            } ?: return null

        val (title, description) = when {
            output.platform.isBlank() || output.format.isBlank() ->
                "Choose an output destination" to
                    "Set the target platform and format for this deliverable."
            output.title.isBlank() ->
                "Give your output a working title" to
                    "Write or select a title in Publish before preparing the release."
            output.publishGate.any {
                it.required && it.status == CreatorPublishGateStatus.TODO
            } ->
                "Review unfinished publishing checks" to
                    "Required publish-gate items are still marked TODO. Review them in Publish."
            output.description.isBlank() && output.status == CreatorDeliverableStatus.PLANNED ->
                "Draft the publishing description" to
                    "Your planned output has no saved description yet. Add one in Publish."
            output.status == CreatorDeliverableStatus.PLANNED &&
                output.platform.lowercase(Locale.ROOT).contains("youtube") &&
                output.thumbnailConcept.isBlank() &&
                output.thumbnailVariants.none { it.text.isNotBlank() } ->
                "Develop a thumbnail idea" to
                    "Save a thumbnail concept or candidate for this YouTube output."
            else ->
                "Prepare your planned output" to
                    "Review the saved packaging and publishing details before changing its status."
        }
        return V2285WorkspaceFocusHint(
            key = "output",
            title = title,
            description = description,
            destination = V2285WorkspaceDestination.PUBLISH,
        )
    }
}
