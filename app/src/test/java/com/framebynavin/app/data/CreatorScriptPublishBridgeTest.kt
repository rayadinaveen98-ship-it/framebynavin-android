package com.framebynavin.app.data

import org.junit.Assert.*
import org.junit.Test

class CreatorScriptPublishBridgeTest {
    private fun target(title: String = "Original title") = CreatorDeliverable(
        id = "output-a", platform = "YouTube", format = "Long video", title = title,
        description = "Original description", tags = "a,b", thumbnailConcept = "Cover",
        publishGate = listOf(CreatorPublishGateItem(id = "custom", title = "Check")),
    )

    @Test fun onlyExplicitlySelectedNonBlankScriptTitleIsEligible() {
        val script = CreatorScriptStudio(
            projectId = "p",
            titles = listOf(
                CreatorTitleIdea(text = "First idea", selected = false),
                CreatorTitleIdea(text = "  Selected title  ", selected = true),
            ),
        )
        assertEquals("Selected title", CreatorScriptPublishBridge.selectedTitle(script))
        assertEquals("", CreatorScriptPublishBridge.selectedTitle(script.copy(titles = script.titles.map { it.copy(selected = false) })))
        assertEquals("", CreatorScriptPublishBridge.selectedTitle(null))
        assertEquals("", CreatorScriptPublishBridge.selectedTitle(script.copy(titles = listOf(CreatorTitleIdea(text = "  ", selected = true)))))
    }

    @Test fun manualTitleUsePreservesAllOtherMetadataAndOriginal() {
        val original = target()
        val updated = CreatorScriptPublishBridge.useTitle(original, "  Selected title  ")
        assertEquals("Selected title", updated.title)
        assertEquals(original.description, updated.description)
        assertEquals(original.tags, updated.tags)
        assertEquals(original.thumbnailConcept, updated.thumbnailConcept)
        assertEquals(original.publishGate, updated.publishGate)
        assertEquals(original.status, updated.status)
        assertEquals(original.publicationHistory, updated.publicationHistory)
        assertEquals("Original title", original.title)
    }

    @Test fun sameOrBlankTitleCannotBeApplied() {
        assertFalse(CreatorScriptPublishBridge.canUseTitle(target(), "  Original title "))
        assertFalse(CreatorScriptPublishBridge.canUseTitle(target(), "  "))
        assertThrows(IllegalArgumentException::class.java) {
            CreatorScriptPublishBridge.useTitle(target(), "")
        }
    }

    @Test fun publishedAndPreviouslyPublishedOutputsCannotReceiveScriptTitle() {
        val released = target().copy(
            status = CreatorDeliverableStatus.PUBLISHED,
            publishedAtMillis = 101L,
            publicationHistory = listOf(CreatorPublicationEvent(atMillis = 101L)),
        )
        val reopened = released.copy(status = CreatorDeliverableStatus.READY, publishedAtMillis = 0L)
        val legacy = target().copy(publishedAtMillis = 101L)
        val urlOnly = target().copy(publishedUrl = "https://example.com")
        for (item in listOf(released, reopened, legacy, urlOnly)) {
            assertFalse(CreatorScriptPublishBridge.canUseTitle(item, "A different title"))
            assertThrows(IllegalArgumentException::class.java) {
                CreatorScriptPublishBridge.useTitle(item, "A different title")
            }
        }
    }

    @Test fun applyingToOneOfMultipleOutputsLeavesOthersUntouched() {
        val first = target()
        val other = target("Second output title").copy(id = "output-b", platform = "Instagram")
        val outputs = listOf(first, other).map {
            if (it.id == first.id) CreatorScriptPublishBridge.useTitle(it, "Selected title") else it
        }
        assertEquals("Selected title", outputs[0].title)
        assertEquals(other, outputs[1])
    }
}
