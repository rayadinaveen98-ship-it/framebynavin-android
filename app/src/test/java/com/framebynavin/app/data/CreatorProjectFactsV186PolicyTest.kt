package com.framebynavin.app.data

import org.junit.Assert.*
import org.junit.Test

class CreatorProjectFactsV186PolicyTest {
    private fun task() = CreatorTask(id = "v186", title = "Film", platform = "YouTube",
        contentType = "Video", dueLabel = "Tomorrow")

    @Test fun plannedStagesAndChecklistRemainIndependent() {
        val task = task().copy(workflowStageIndex = 2, workspace = CreatorContentWorkspace(checklist = listOf(
            CreatorChecklistItem(id = "a", title = "Script", status = CreatorChecklistStatus.DONE),
            CreatorChecklistItem(id = "b", title = "Edit"),
            CreatorChecklistItem(id = "c", title = "Optional", status = CreatorChecklistStatus.SKIPPED))))
        val facts = CreatorProjectFactsV186Policy.facts(task)
        assertEquals(2, facts.workflowCompletedStages)
        assertTrue(facts.workflowTotalStages > 2)
        assertEquals("Checklist 1/3 done · 1 skipped", facts.checklistLabel)
        assertEquals("Not published", facts.publicationLabel)
        assertEquals("Planned", facts.projectStatusLabel)
        assertEquals(0L, task.publishedAtMillis)
    }

    @Test fun completedWorkflowNeverInventsPublishing() {
        val facts = CreatorProjectFactsV186Policy.facts(task().copy(status = TaskStatus.DONE))
        assertEquals(facts.workflowTotalStages, facts.workflowCompletedStages)
        assertEquals(1f, facts.workflowFraction, 0.0001f)
        assertEquals("No checklist", facts.checklistLabel)
        assertEquals("Not published", facts.publicationLabel)
        assertEquals("Finished", facts.projectStatusLabel)
    }

    @Test fun publishedProjectMayHaveUnfinishedWorkflow() {
        val facts = CreatorProjectFactsV186Policy.facts(task().copy(status = TaskStatus.WORKING, publishedAtMillis = 100L))
        assertEquals("Project publication recorded", facts.publicationLabel)
        assertTrue(facts.workflowCompletedStages < facts.workflowTotalStages)
        assertEquals("In progress", facts.projectStatusLabel)
    }

    @Test fun aPublishedOutputDoesNotPublishItsParent() {
        val facts = CreatorProjectFactsV186Policy.facts(task().copy(workspace = CreatorContentWorkspace(deliverables = listOf(
            CreatorDeliverable(platform = "YouTube", format = "Short", status = CreatorDeliverableStatus.PUBLISHED, publishedAtMillis = 20L),
            CreatorDeliverable(platform = "Instagram", format = "Reel")))))
        assertEquals("1/2 outputs published", facts.publicationLabel)
        assertEquals("Planned", facts.projectStatusLabel)
    }

    @Test fun skippingAndArchivingNeverImplyCompletion() {
        val skipped = CreatorProjectFactsV186Policy.facts(task().copy(status = TaskStatus.SKIPPED))
        val archived = CreatorProjectFactsV186Policy.facts(task().copy(archivedAtMillis = 99L))
        assertEquals("Skipped", skipped.projectStatusLabel)
        assertTrue(skipped.workflowLabel.startsWith("Workflow skipped"))
        assertEquals("Archived", archived.projectStatusLabel)
        assertEquals("Not published", archived.publicationLabel)
    }
}
