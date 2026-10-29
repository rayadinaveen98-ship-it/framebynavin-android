package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorChecklistItem
import com.framebynavin.app.data.CreatorChecklistStatus
import com.framebynavin.app.data.CreatorContentWorkspace
import com.framebynavin.app.data.CreatorDeliverable
import com.framebynavin.app.data.CreatorDeliverableStatus
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2285WorkspaceFocusPolicyTest {
    private fun task(ws: CreatorContentWorkspace = CreatorContentWorkspace()) =
        CreatorTask(
            id = "project-qa", title = "Cinema essay", platform = "YouTube",
            contentType = "Long-form", dueLabel = "This week",
            workspace = ws,
        )

    @Test fun emptyProjectSuggestsActualSavedGapsWithoutChangingProgress() {
        val source = task()
        val result = V2285WorkspaceFocusPolicy.hints(source)
        assertEquals(listOf("promise", "script"), result.map { it.key })
        assertEquals(0, source.progress)
        assertEquals(0L, source.workspace.revision)
        assertEquals(2, result.size)
    }

    @Test fun writtenProjectSurfacesPublishSetupAndUnfinishedPlan() {
        val ws = CreatorContentWorkspace(
            audience = "Film students", promise = "Understand why silence matters",
            hook = "Why does this shot stay quiet?",
            checklist = listOf(CreatorChecklistItem(title = "Record", status = CreatorChecklistStatus.TODO)),
        )
        val result = V2285WorkspaceFocusPolicy.hints(task(ws))
        assertEquals(listOf("checklist", "output"), result.map { it.key })
    }

    @Test fun readyOrPublishedDeliverableIsNotFlaggedAsUnplanned() {
        val ws = CreatorContentWorkspace(
            audience = "New directors", promise = "Learn", script = "Draft",
            deliverables = listOf(CreatorDeliverable(platform = "YouTube",
                format = "Long-form", status = CreatorDeliverableStatus.READY)),
        )
        assertTrue(V2285WorkspaceFocusPolicy.hints(task(ws)).isEmpty())
        assertTrue(V2285WorkspaceFocusPolicy.hints(task(ws.copy(
            deliverables = ws.deliverables.map { it.copy(status = CreatorDeliverableStatus.PUBLISHED) },
        ))).isEmpty())
    }

    @Test fun completedAndSkippedProjectsReceiveNoRequestsToWork() {
        val empty = task()
        assertTrue(V2285WorkspaceFocusPolicy.hints(empty.copy(status = TaskStatus.DONE)).isEmpty())
        assertTrue(V2285WorkspaceFocusPolicy.hints(empty.copy(status = TaskStatus.SKIPPED)).isEmpty())
        assertFalse(V2285WorkspaceFocusPolicy.hints(empty).isEmpty())
    }

    @Test fun scriptStagePrioritizesScriptButNeverMoreThanTwoEditors() {
        val t = task().copy(workflowStageIndex = 2)
        val result = V2285WorkspaceFocusPolicy.hints(t)
        assertEquals(V2285WorkspaceDestination.SCRIPT, result.first().destination)
        assertTrue(result.size <= 2)
    }
}
