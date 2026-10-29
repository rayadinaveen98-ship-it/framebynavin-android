package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorDeliverable
import com.framebynavin.app.data.CreatorDeliverableStatus
import com.framebynavin.app.data.CreatorPublishGateItem
import com.framebynavin.app.data.CreatorPublishGateStatus
import com.framebynavin.app.data.CreatorContentWorkspace
import com.framebynavin.app.data.CreatorTask
import org.junit.Assert.*
import org.junit.Test

class V2295PublishReadinessPolicyTest {
    private fun output(
        platform: String = "YouTube",
        format: String = "Long-form",
        title: String = "Why silence works",
        description: String = "A cinema analysis",
        status: CreatorDeliverableStatus = CreatorDeliverableStatus.PLANNED,
        thumbnailConcept: String = "A silent wide shot",
        gates: List<CreatorPublishGateItem> = emptyList(),
    ) = CreatorDeliverable(
        platform = platform,
        format = format,
        title = title,
        description = description,
        status = status,
        thumbnailConcept = thumbnailConcept,
        publishGate = gates,
    )

    @Test fun missingTitleIsTheFirstSpecificGap() {
        val saved = output(title = "", description = "", thumbnailConcept = "")
        val hint = V2295PublishReadinessPolicy.hint(listOf(saved))
        assertEquals("output", hint?.key)
        assertEquals(V2285WorkspaceDestination.PUBLISH, hint?.destination)
        assertEquals("Give your output a working title", hint?.title)
        assertEquals("", saved.title)
        assertEquals(CreatorDeliverableStatus.PLANNED, saved.status)
    }

    @Test fun requiredTodoGateBeatsOptionalMetadataAndCannotMarkReady() {
        val gate = CreatorPublishGateItem(
            title = "Review copyright",
            status = CreatorPublishGateStatus.TODO,
            required = true,
        )
        val saved = output(description = "", gates = listOf(gate))
        assertEquals("Review unfinished publishing checks",
            V2295PublishReadinessPolicy.hint(listOf(saved))?.title)
        assertEquals(CreatorPublishGateStatus.TODO, saved.publishGate.single().status)
    }

    @Test fun descriptionAndYoutubeThumbnailAreHonestSuggestions() {
        assertEquals("Draft the publishing description",
            V2295PublishReadinessPolicy.hint(listOf(output(description = "")))?.title)
        assertEquals("Develop a thumbnail idea",
            V2295PublishReadinessPolicy.hint(listOf(output(thumbnailConcept = "")))?.title)
        assertEquals("Prepare your planned output",
            V2295PublishReadinessPolicy.hint(
                listOf(output(platform = "Podcast", thumbnailConcept = "")))?.title)
    }

    @Test fun publishedOutputsAreNeverReframedAsUnpublished() {
        assertNull(V2295PublishReadinessPolicy.hint(
            listOf(output(status = CreatorDeliverableStatus.PUBLISHED))))
        assertNull(V2295PublishReadinessPolicy.hint(
            listOf(output(status = CreatorDeliverableStatus.READY))))
    }

    @Test fun readyWithRequiredTodoIsFlaggedButNotAutoChanged() {
        val saved = output(
            status = CreatorDeliverableStatus.READY,
            gates = listOf(CreatorPublishGateItem(
                title = "Final clearance", required = true,
                status = CreatorPublishGateStatus.TODO,
            )),
        )
        assertEquals("Review unfinished publishing checks",
            V2295PublishReadinessPolicy.hint(listOf(saved))?.title)
        assertEquals(CreatorDeliverableStatus.READY, saved.status)
    }

    @Test fun existingWorkspaceFocusStillOpensPublishWithoutChangingData() {
        val workspace = CreatorContentWorkspace(
            audience = "Learners",
            promise = "Learn why quiet frames work",
            script = "Saved narrative",
            deliverables = listOf(output(title = "")),
        )
        val task = CreatorTask(
            id = "v2295", title = "Why silence works", platform = "YouTube",
            contentType = "Long-form", dueLabel = "This week", workspace = workspace,
        )
        val hints = V2285WorkspaceFocusPolicy.hints(task)
        assertEquals(listOf("output"), hints.map { it.key })
        assertEquals(V2285WorkspaceDestination.PUBLISH, hints.single().destination)
        assertEquals(0L, task.workspace.revision)
        assertEquals(CreatorDeliverableStatus.PLANNED, task.workspace.deliverables.single().status)
    }
}
