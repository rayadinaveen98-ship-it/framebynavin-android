package com.framebynavin.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source regression fixtures; to run with the consolidated Phase 7 suite. */
class Phase5CalendarAutomationWorkloadPolicyTest {
    private val zone = ZoneId.systemDefault()
    private val monday = LocalDate.of(2026, 11, 9)
    private val now = monday.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val next = monday.plusDays(1).atTime(19, 0).atZone(zone).toInstant().toEpochMilli()
    private val yesterday = monday.minusDays(1).atTime(15, 0).atZone(zone).toInstant().toEpochMilli()

    private fun task(id: String, due: Long = next,
        workspace: CreatorContentWorkspace = CreatorContentWorkspace(),
    ) = CreatorTask(
        id = id, title = "Creator project $id",
        platform = "YouTube", contentType = "Long video",
        dueLabel = "Tuesday", dueAtMillis = due,
        workspace = workspace,
    )

    private fun tuesday(
        id: String = "weekly-test",
        estimate: Int = 0,
    ) = WeeklyScheduleSlot(
        id = id, title = "Weekly planned upload",
        dayOfWeek = DayOfWeek.TUESDAY,
        hour = 19, minute = 0,
        platform = "YouTube", contentType = "Long video",
        estimatedMinutes = estimate,
    )

    @Test fun distinctRealProjectWeeklyPreviewAndRecordedPublication() {
        val posted = CreatorDeliverable(
            id = "out-1", platform = "YouTube",
            format = "Long video", title = "Already posted",
            status = CreatorDeliverableStatus.PUBLISHED,
            publishedAtMillis = yesterday,
        )
        val ready = posted.copy(
            id = "out-2", title = "Not posted",
            status = CreatorDeliverableStatus.READY,
            publishedAtMillis = 0L,
        )
        val project = task("real",
            workspace = CreatorContentWorkspace(deliverables = listOf(posted, ready)))
        val result = CreatorContentCalendarEngine.upcoming(
            tasks = listOf(project), weeklySlots = listOf(tuesday()),
            nowMillis = now, daysAhead = 14,
        )
        assertEquals(1, result.count { it.source == CreatorCalendarSource.PROJECT })
        assertEquals(1, result.count { it.source == CreatorCalendarSource.WEEKLY_PLAN })
        assertEquals(1, result.count { it.source == CreatorCalendarSource.PUBLICATION })
        val publication = result.first { it.source == CreatorCalendarSource.PUBLICATION }
        assertEquals("Already posted", publication.title)
        assertEquals(yesterday, publication.atMillis)
        assertTrue(CreatorCalendarNavigationPolicy.isProjectLink(publication))
        assertFalse(CreatorCalendarNavigationPolicy.isProjectLink(
            result.first { it.source == CreatorCalendarSource.WEEKLY_PLAN }))
    }

    @Test fun archivedPublicationHistorySurvivesAndNeverPollutesDailyBrief() {
        val archived = task("archived", due = 0L).copy(
            archivedAtMillis = now,
            publishedAtMillis = yesterday,
        )
        val calendar = CreatorContentCalendarEngine.upcoming(
            tasks = listOf(archived), weeklySlots = emptyList(), nowMillis = now,
        )
        assertEquals(1, calendar.count {
            it.source == CreatorCalendarSource.PUBLICATION
        })
        val brief = CreatorDailyBriefEngine.build(
            listOf(archived), emptyList(), nowMillis = now,
        )
        assertEquals(0, brief.calendar.count {
            it.source == CreatorCalendarSource.PUBLICATION
        })
    }

    @Test fun generatedSkippedAndArchivedOccurrenceKeysStillBlockPreviewDuplicates() {
        val slot = tuesday()
        val key = WeeklyScheduleEngine.occurrenceKey(slot.id, monday.plusDays(1))
        val preserved = task("generated").copy(
            scheduleOccurrenceKey = key,
            scheduleSlotId = slot.id,
            status = TaskStatus.SKIPPED,
            archivedAtMillis = now,
        )
        val calendar = CreatorContentCalendarEngine.upcoming(
            listOf(preserved), listOf(slot), nowMillis = now,
        )
        assertEquals(0, calendar.count {
            it.source == CreatorCalendarSource.WEEKLY_PLAN
        })
        assertEquals(0, CreatorWeeklyPlanPreviewEngine.build(
            listOf(slot), listOf(preserved), nowMillis = now,
        ).missing.size)
    }

    @Test fun workloadWarnsOnlyFromOwnerProvidedEstimatesAndNeverMovesDates() {
        val estimate = tuesday(estimate = 180)
        val unknown = WeeklyScheduleSlot(
            id = "wed", title = "Another planned post",
            dayOfWeek = DayOfWeek.WEDNESDAY,
            hour = 19, minute = 0,
            platform = "Instagram", contentType = "Reel",
        )
        val overloaded = CreatorWorkloadPlanning.nextSevenDays(
            listOf(estimate, unknown), availableMinutes = 120,
            nowMillis = now,
        )
        assertEquals(2, overloaded.occurrences)
        assertEquals(1, overloaded.estimatedOccurrences)
        assertEquals(1, overloaded.unknownOccurrences)
        assertEquals(180, overloaded.plannedMinutes)
        assertTrue(overloaded.isOverloaded)
        assertTrue(overloaded.hasIncompleteEstimates)
        assertFalse(overloaded.canClaimAllWorkIsCovered)
        val feasible = CreatorWorkloadPlanning.nextSevenDays(
            listOf(estimate, unknown), availableMinutes = 240,
            nowMillis = now,
        )
        assertFalse(feasible.isOverloaded)
        assertEquals(next, WeeklyScheduleEngine.upcomingOccurrences(
            listOf(estimate), fromMillis = now, daysAhead = 7,
        ).first().publishAtMillis)
        assertEquals(0, CreatorWorkloadPlanning.nextSevenDays(
            listOf(unknown), availableMinutes = 0,
            nowMillis = now,
        ).plannedMinutes)
    }

    @Test fun automationStatusNeverConfusesRequestRetryAndConfirmedResult() {
        val pending = CreatorPlannerRunSnapshot(
            200L, 100L, 3, 250L, 2, "RETRY_PENDING",
        )
        assertTrue(pending.hasUnconfirmedAttempt)
        assertEquals(100L, pending.lastConfirmedAtMillis)
        assertEquals(2, pending.retryAttempt)
        val confirmed = pending.copy(
            state = "CONFIRMED", lastConfirmedAtMillis = 300L,
            lastCreatedCount = 0, retryAtMillis = 0L, retryAttempt = 0,
        )
        assertFalse(confirmed.hasUnconfirmedAttempt)
        assertEquals(300L, confirmed.lastConfirmedAtMillis)
    }
}
