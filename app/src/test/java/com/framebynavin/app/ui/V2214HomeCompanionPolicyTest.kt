package com.framebynavin.app.ui

import com.framebynavin.app.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** V221.4 must not invent ideas or navigate to missing creator records. */
class V2214HomeCompanionPolicyTest {
    private fun task(id: String, status: TaskStatus = TaskStatus.PLANNED,
        archived: Long = 0L) = CreatorTask(
        id = id, title = "Short film", platform = "YouTube",
        contentType = "Video", dueLabel = "Tomorrow",
        status = status, archivedAtMillis = archived,
    )

    private fun decision(id: String = "a", destination: V221CompanionDestination =
        V221CompanionDestination.PROJECT) = V221CompanionDecision(
        id = "project-soon:$id",
        moment = V221CompanionMoment.DUE_SOON_PROJECT,
        expression = V221CompanionExpression.FOCUS,
        message = "A due project", reason = "Saved deadline",
        actionLabel = "Open Project",
        destination = destination, taskId = id,
    )

    @Test fun inboxExcludesLinkedArchivedConvertedAndTrashedIdeas() {
        val list = listOf(
            CreatorIdea(id = "a", title = "Unsorted"),
            CreatorIdea(id = "b", title = "Linked", projectTaskId = "project"),
            CreatorIdea(id = "c", title = "Research", status = IdeaStatus.RESEARCHING),
            CreatorIdea(id = "d", title = "Archived", status = IdeaStatus.ARCHIVED),
            CreatorIdea(id = "e", title = "Deleted", trashedAtMillis = 10L),
            CreatorIdea(id = "f", title = "Another inbox"),
        )
        assertEquals(2, V2214HomeCompanionPolicy.unorganizedCount(list))
        assertEquals(0, V2214HomeCompanionPolicy.unorganizedCount(emptyList()))
    }

    @Test fun activeProjectActionTargetsExactlyCurrentRecord() {
        assertEquals("a", V2214HomeCompanionPolicy.openableProjectId(
            decision(), listOf(task("a"), task("b"))))
        assertEquals("a", V2214HomeCompanionPolicy.openableProjectId(
            decision(), listOf(task("a", TaskStatus.WORKING))))
    }

    @Test fun staleProjectsAndNonProjectDestinationsNeverOpenStudio() {
        assertNull(V2214HomeCompanionPolicy.openableProjectId(decision(), emptyList()))
        assertNull(V2214HomeCompanionPolicy.openableProjectId(
            decision(), listOf(task("b"))))
        assertNull(V2214HomeCompanionPolicy.openableProjectId(
            decision(), listOf(task("a", TaskStatus.DONE))))
        assertNull(V2214HomeCompanionPolicy.openableProjectId(
            decision(), listOf(task("a", TaskStatus.SKIPPED))))
        assertNull(V2214HomeCompanionPolicy.openableProjectId(
            decision(), listOf(task("a", archived = 300L))))
        assertNull(V2214HomeCompanionPolicy.openableProjectId(
            decision(destination = V221CompanionDestination.REMINDER_HUB),
            listOf(task("a"))))
    }
}
