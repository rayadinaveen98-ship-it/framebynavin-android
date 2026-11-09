package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lightweight model fixtures; retained for consolidated Phase 7 QA. */
class Phase4CreatorWorkspacePolicyTest {
    private fun task(workspace: CreatorContentWorkspace = CreatorContentWorkspace(),
        publishedAt: Long = 0L) = CreatorTask(
        id = "p4-project-1", title = "Creative analysis",
        platform = "YouTube", contentType = "Long-form", dueLabel = "Tomorrow",
        publishedAtMillis = publishedAt, workspace = workspace,
    )

    @Test fun localSaveNeverPretendsCloudBackupOrCommittedDraft() {
        val ws = CreatorContentWorkspace(revision = 8)
        val s = Phase4CreatorWorkspacePolicy.summarize(
            task(ws), listOf("Plan", "Script"), listOf("publish"),
        )
        assertTrue(s.revisionLabel.contains("revision 8"))
        assertTrue(s.pendingDraftLabel!!.contains("not saved to project"))
        assertTrue(s.heldDraftLabel!!.contains("review before applying"))
        assertTrue(s.backupLabel.contains("not verified"))
        assertFalse(s.backupLabel.contains("backed up"))
    }

    @Test fun readyIsNotPublishedAndChecklistsDoNotCompleteProject() {
        val ws = CreatorContentWorkspace(
            revision = 3,
            checklist = listOf(
                CreatorChecklistItem("c1", "Research", CreatorChecklistStatus.DONE),
                CreatorChecklistItem("c2", "Record", CreatorChecklistStatus.TODO),
            ),
            deliverables = listOf(
                CreatorDeliverable("d1", "YouTube", "Long video", "Video",
                    status = CreatorDeliverableStatus.READY),
                CreatorDeliverable("d2", "Instagram", "Reel", "Clip",
                    status = CreatorDeliverableStatus.PLANNED),
            ),
        )
        val result = Phase4CreatorWorkspacePolicy.summarize(
            task(ws), emptyList(), emptyList(),
        )
        assertEquals("1 checklist done · 1 remaining", result.productionLabel)
        assertEquals("1 planned · 1 ready · 0 published", result.publishingLabel)
        assertEquals(null, result.pendingDraftLabel)
    }

    @Test fun olderProjectPublicationIsNotMisrepresentedAsPublishedOutput() {
        val result = Phase4CreatorWorkspacePolicy.summarize(
            task(publishedAt = 1_790_000_000_000L),
            emptyList(), emptyList(),
        )
        assertTrue(result.publishingLabel.contains("Earlier project publication recorded"))
    }

    @Test fun additiveUniversalTemplatesNeverEraseSavedChecklistOrPublication() {
        val required = setOf("creator_analysis", "commentary_education",
            "short_reel_launch")
        assertTrue(CreatorProjectTemplates.contentTemplates.map { it.id }
            .toSet().containsAll(required))
        val published = CreatorDeliverable(
            id = "published-1", platform = "YouTube", format = "Long video",
            title = "Original", status = CreatorDeliverableStatus.PUBLISHED,
        )
        val existing = CreatorContentWorkspace(deliverables = listOf(published))
        val template = CreatorProjectTemplates.contentTemplates.first {
            it.id == "creator_analysis"
        }
        val withTemplate = CreatorProjectTemplates.applyContentTemplate(
            existing, template, "My analysis",
        )
        assertEquals(listOf(published), withTemplate.deliverables)
        val twice = CreatorProjectTemplates.applyContentTemplate(
            withTemplate, template, "My analysis",
        )
        assertEquals(withTemplate.checklist, twice.checklist)
        assertEquals(withTemplate.deliverables, twice.deliverables)
    }
}
