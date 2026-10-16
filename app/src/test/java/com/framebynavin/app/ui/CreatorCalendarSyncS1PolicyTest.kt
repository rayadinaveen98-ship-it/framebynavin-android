package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.*
import org.junit.Test

class CreatorCalendarSyncS1PolicyTest {
    private fun project(id: String = "a", status: TaskStatus = TaskStatus.PLANNED,
                        archived: Long = 0L, due: Long = 123456L) = CreatorTask(
        id = id, title = "My private video", platform = "YouTube",
        contentType = "Long-form", dueLabel = "Tomorrow", dueAtMillis = due,
        status = status, archivedAtMillis = archived,
        notes = "Private idea! DO NOT EXPORT",
        reminderEnabled = true, reminderAtMillis = due - 1000L,
    )

    @Test fun onlyEligibleActiveOrCompletedProjectsAreExported() {
        val active = project()
        val completed = project("finished", TaskStatus.DONE)
        val skipped = project("skip", TaskStatus.SKIPPED)
        val archived = project("archived", archived = 1L)
        val undated = project("undated", due = 0L)
        val included = CreatorCalendarSyncS1Policy.exportable(listOf(
            active, completed, skipped, archived, undated
        ))
        assertEquals(listOf(active, completed), included)
    }

    @Test fun exportedMetadataNeverIncludesPrivateNotesOrAlarm() {
        val text = CreatorCalendarSyncS1Policy.description(project())
        assertEquals("YouTube · Long-form", text)
        assertFalse(text.contains("DO NOT EXPORT"))
        assertFalse(text.contains("123456"))
        assertEquals("Creator project · My private video", CreatorCalendarSyncS1Policy.title(project()))
    }

    @Test fun titleOwnershipMustHaveBacklotPrefix() {
        assertTrue(CreatorCalendarSyncS1Policy.ownedTitle("Creator project · Original title"))
        assertFalse(CreatorCalendarSyncS1Policy.ownedTitle("Personal calendar event"))
        assertFalse(CreatorCalendarSyncS1Policy.ownedTitle(null))
    }

    @Test fun summaryDistinguishesNoChangeFromPartialFailure() {
        assertEquals("Calendar is already up to date.", CreatorCalendarSyncReport().message())
        val partial = CreatorCalendarSyncReport(created = 1, blocked = 2)
        assertEquals(1, partial.changed)
        assertTrue(partial.message().contains("could not be safely updated"))
        assertTrue(CreatorCalendarSyncReport(updated = 2, removed = 1).message().contains("1 old Backlot entries removed"))
    }
}
