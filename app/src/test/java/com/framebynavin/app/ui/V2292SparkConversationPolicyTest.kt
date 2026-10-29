package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.IdeaStatus
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.*
import org.junit.Test

class V2292SparkConversationPolicyTest {
    private fun project(id: String, title: String, due: Long = 0L,
        status: TaskStatus = TaskStatus.WORKING, archived: Long = 0L) =
        CreatorTask(id = id, title = title, platform = "YouTube",
            contentType = "Long-form", dueLabel = "This week",
            dueAtMillis = due, status = status, archivedAtMillis = archived)
    private fun idea(id: String, status: IdeaStatus = IdeaStatus.INBOX,
        linked: String = "") = CreatorIdea(id = id, title = "An idea",
        status = status, projectTaskId = linked)

    @Test fun nextStepUsesOnlyActiveUnarchivedProjects() {
        val response = V2292SparkConversationPolicy.respond("What should I do next?",
            listOf(project("finished", "Finished", 1L, TaskStatus.DONE),
                project("archived", "Archived", 2L, archived = 42L),
                project("active", "Cinema essay", 3L)), emptyList())
        assertEquals(V2292SparkDestination.PROJECT, response.destination)
        assertEquals("active", response.targetProjectId)
        assertTrue(response.message.contains("Cinema essay"))
        assertTrue(response.message.contains("workflow suggestion"))
    }

    @Test fun neverInventProjectWhenNothingActive() {
        val r = V2292SparkConversationPolicy.respond("Next step",
            listOf(project("old", "Old project", status = TaskStatus.DONE)), emptyList())
        assertEquals(V2292SparkDestination.IDEAS, r.destination)
        assertFalse(r.message.contains("Old project"))
    }

    @Test fun ideaCountsExcludeTrashedConvertedAndLinked() {
        val r = V2292SparkConversationPolicy.respond("My ideas", emptyList(),
            listOf(idea("active"), idea("converted", IdeaStatus.CONVERTED),
                idea("linked", linked = "project-1"),
                idea("archived", IdeaStatus.ARCHIVED)))
        assertEquals(V2292SparkDestination.IDEAS, r.destination)
        assertTrue(r.message.contains("1 unlinked active idea"))
    }

    @Test fun reminderReplyNeverClaimsAndroidDelivery() {
        val r = V2292SparkConversationPolicy.respond("Reminders",
            emptyList(), emptyList())
        assertEquals(V2292SparkDestination.REMINDERS, r.destination)
        assertTrue(r.message.contains("can't verify"))
    }

    @Test fun unknownAndEmptyInputsAreExplicitlyLimited() {
        val a = V2292SparkConversationPolicy.respond("write me a novel", emptyList(), emptyList())
        assertEquals(V2292SparkDestination.NONE, a.destination)
        assertTrue(a.message.contains("not a full AI"))
        val b = V2292SparkConversationPolicy.respond("  ", emptyList(), emptyList())
        assertEquals(V2292SparkDestination.NONE, b.destination)
    }
}
