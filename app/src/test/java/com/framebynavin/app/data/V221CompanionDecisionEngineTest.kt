package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V221CompanionDecisionEngineTest {
    private val now = 1_700_000_000_000L
    private val hour = 60L * 60L * 1000L
    private val day = 24L * hour

    private fun task(
        id: String, status: TaskStatus = TaskStatus.PLANNED,
        dueAt: Long = 0, reminderAt: Long = 0,
        reminderEnabled: Boolean = false,
        archiveAt: Long = 0,
        completedAt: Long = 0,
        title: String = "Project $id",
    ) = CreatorTask(
        id = id, title = title, platform = "YouTube", contentType = "Video",
        dueLabel = "Today", status = status,
        dueAtMillis = dueAt,
        reminderEnabled = reminderEnabled, reminderAtMillis = reminderAt,
        archivedAtMillis = archiveAt, completedAtMillis = completedAt,
    )

    private fun pick(
        tasks: List<CreatorTask>,
        ids: Set<String> = emptySet(),
        inbox: Int = 0,
        suppressed: Set<String> = emptySet(),
        at: Long = now,
    ) = V221CompanionDecisionEngine.choose(
        V221CompanionContext(tasks, at, ids, inbox, suppressed),
    )

    @Test fun silentWithNoEvidenceAndInvalidClock() {
        assertNull(pick(emptyList()))
        assertNull(pick(emptyList(), at = 0L, inbox = 3))
    }

    @Test fun actualCurrentUnansweredHasPriorityButDoesNotClaimItWasSeen() {
        val alarm = task("alarm", reminderAt = now - hour, reminderEnabled = true)
        val overdue = task("late", dueAt = now - 2 * hour)
        val result = pick(listOf(overdue, alarm), ids = setOf("alarm"))!!
        assertEquals(V221CompanionMoment.UNANSWERED_REMINDER, result.moment)
        assertEquals(V221CompanionDestination.REMINDER_HUB, result.destination)
        assertEquals("Open Reminders", result.actionLabel)
        assertNull(result.taskId)
        assertTrue(result.reason.contains("does not mean"))
        assertFalse(result.message.contains("heard"))
        assertTrue(result.id.contains("alarm"))
    }

    @Test fun postingOrActiveAlarmNeverAutomaticallyBecomesAnUnansweredSuggestion() {
        val deliveryOnly = task("posted", reminderAt = now - hour, reminderEnabled = true)
        val result = pick(listOf(deliveryOnly))!!
        assertEquals(V221CompanionMoment.REVIEW_PROJECT, result.moment)
        assertFalse(result.message.contains("reminder", ignoreCase = true))
    }

    @Test fun injectedStaleOrDisabledReminderIdsCannotOverrideExistingFacts() {
        val disabled = task("off", reminderAt = now - hour, reminderEnabled = false)
        val result = pick(listOf(disabled), ids = setOf("off"))!!
        assertEquals(V221CompanionMoment.REVIEW_PROJECT, result.moment)
        val gone = task("gone", status = TaskStatus.DONE,
            reminderAt = now - hour, reminderEnabled = true)
        assertNull(pick(listOf(gone), ids = setOf("gone")))
    }

    @Test fun archivedSkippedAndCompletedAreNotActionableProjects() {
        val rows = listOf(
            task("arch", dueAt = now - hour, archiveAt = now - 100),
            task("skip", status = TaskStatus.SKIPPED, dueAt = now - hour),
            task("done", status = TaskStatus.DONE, dueAt = now - hour),
        )
        assertNull(pick(rows))
    }

    @Test fun overdueAndDueSoonDependOnActualMillisNotHumanReadableDueLabel() {
        assertEquals(V221CompanionMoment.OVERDUE_PROJECT,
            pick(listOf(task("late", dueAt = now - 1)))!!.moment)
        assertEquals(V221CompanionMoment.DUE_SOON_PROJECT,
            pick(listOf(task("exact", dueAt = now)))!!.moment)
        assertEquals(V221CompanionMoment.DUE_SOON_PROJECT,
            pick(listOf(task("tomorrow", dueAt = now + day)))!!.moment)
        assertEquals(V221CompanionMoment.REVIEW_PROJECT,
            pick(listOf(task("later", dueAt = now + day + 1)))!!.moment)
        assertEquals(V221CompanionMoment.REVIEW_PROJECT,
            pick(listOf(task("label", dueAt = 0L)))!!.moment)
    }

    @Test fun workingBeatsUnorganizedIdeasAndScheduledProject() {
        val r = pick(listOf(task("planned"), task("work", status = TaskStatus.WORKING)), inbox = 7)!!
        assertEquals(V221CompanionMoment.CONTINUE_PROJECT, r.moment)
        assertEquals("work", r.taskId)
        assertEquals(V221CompanionDestination.PROJECT, r.destination)
    }

    @Test fun actualIdeaInboxCanBeUsefulWithoutAnyProject() {
        val r = pick(emptyList(), inbox = 2)!!
        assertEquals(V221CompanionMoment.ORGANIZE_IDEAS, r.moment)
        assertEquals(V221CompanionDestination.IDEA_VAULT, r.destination)
        assertTrue(r.message.contains("2 captured"))
        assertNull(r.taskId)
        assertNull(pick(emptyList(), inbox = -5))
    }

    @Test fun suppressionUsesStableDecisionIdsAndFallsThroughToNextVerifiedOption() {
        val soon = task("soon", dueAt = now + 5 * hour)
        val late = task("late", dueAt = now - hour)
        val first = pick(listOf(soon, late))!!
        assertEquals("project-overdue:late", first.id)
        val fallback = pick(listOf(late, soon),
            suppressed = setOf("project-overdue:late"))!!
        assertEquals("project-soon:soon", fallback.id)
        assertNull(pick(listOf(late), suppressed =
            setOf("project-overdue:late", "project-review:late")))
    }

    @Test fun deterministicSortingAcrossInputReorderingAndDeadlineTies() {
        val a = task("alpha", dueAt = now - hour)
        val b = task("beta", dueAt = now - hour)
        val c = task("gamma", dueAt = now - 2 * hour)
        assertEquals("project-overdue:gamma", pick(listOf(a,b,c))?.id)
        assertEquals("project-overdue:gamma", pick(listOf(c,b,a))?.id)
        assertEquals("project-overdue:alpha", pick(listOf(b,a))?.id)
    }

    @Test fun shortMessagesNormalizeTitleAndDoNotExposeOtherCreatorFields() {
        val secret = "private draft notes"
        val title = "  Telugu\n Cinema  "
        val r = pick(listOf(task("safe", dueAt = now + hour,
            title = title).copy(notes = secret)))!!
        assertTrue(r.message.contains("Telugu Cinema"))
        assertFalse(r.message.contains("\n"))
        assertFalse(r.message.contains(secret))
        assertEquals("safe", r.taskId)
    }

    @Test fun completionRequiresTimestampAndDoesNotClaimPublication() {
        assertNull(pick(listOf(task("old", status = TaskStatus.DONE))))
        val r = pick(listOf(task("real", status = TaskStatus.DONE,
            completedAt = now - hour)))!!
        assertEquals(V221CompanionMoment.RECENT_COMPLETION, r.moment)
        assertNull(r.actionLabel)
        assertEquals(V221CompanionDestination.NONE, r.destination)
        assertFalse(r.message.contains("published", ignoreCase = true))
        assertNull(pick(listOf(task("future", status = TaskStatus.DONE,
            completedAt = now + hour))))
        assertNull(pick(listOf(task("stale", status = TaskStatus.DONE,
            completedAt = now - day))))
    }

    @Test fun decisionIsReadOnlyAndNeverMutatesInputTaskCollection() {
        val one = task("one", status = TaskStatus.WORKING, dueAt = now + hour)
        val tasks = mutableListOf(one)
        val before = tasks.toList()
        val first = pick(tasks)!!
        assertEquals(first, pick(tasks))
        assertEquals(before, tasks)
        assertEquals(TaskStatus.WORKING, tasks.first().status)
    }
}
