package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ProgressJourneyEngineTest {
    private val base = 1_780_000_000_000L
    private fun task(id: String = "p1", status: TaskStatus = TaskStatus.PLANNED, completedAt: Long = 0L) =
        CreatorTask(id = id, title = "Cinema lighting", platform = "YouTube",
            contentType = "Analysis", dueLabel = "", status = status,
            completedAtMillis = completedAt)

    private fun entry(
        id: String, source: CreatorWorkflowTimelineSource, type: CreatorWorkflowTimelineType,
        at: Long = base,
    ) = CreatorWorkflowTimelineEvent(
        id = id, taskId = "p1", stageId = "script",
        stageLabel = "Writing", type = type, source = source, atMillis = at,
    )

    @Test fun importedCurrentStageIsNeverClaimedAsCreationOrCompletion() {
        val events = ProgressJourneyEngine.build(
            listOf(task()), emptyList(),
            listOf(entry("observed", CreatorWorkflowTimelineSource.OBSERVED_CURRENT,
                CreatorWorkflowTimelineType.ENTERED)), base + 2000,
        )
        assertTrue(events.isEmpty())
    }

    @Test fun creatorEventsRepresentTheirActualKindWithoutDuplication() {
        val events = ProgressJourneyEngine.build(
            listOf(task(status = TaskStatus.DONE, completedAt = base + 300)),
            listOf(CreatorIdea(id = "i1", title = "Scene", createdAtMillis = base + 400)),
            listOf(
                entry("create", CreatorWorkflowTimelineSource.PROJECT_CREATED,
                    CreatorWorkflowTimelineType.ENTERED, base),
                entry("move", CreatorWorkflowTimelineSource.STAGE_TRANSITION,
                    CreatorWorkflowTimelineType.ENTERED, base + 100),
                entry("finish", CreatorWorkflowTimelineSource.PROJECT_FINISHED,
                    CreatorWorkflowTimelineType.EXITED, base + 300),
            ), base + 1000,
        )
        assertEquals(4, events.size)
        assertEquals(1, events.count { it.kind == ProgressJourneyKind.PROJECT_COMPLETED })
        assertEquals(1, events.count { it.kind == ProgressJourneyKind.PROJECT_CREATED })
        assertEquals("Moved to Writing", events.first { it.kind == ProgressJourneyKind.STAGE_MOVED }.description)
    }

    @Test fun neverCountsFutureOrTrashedOrLegacyPublishedEvents() {
        val future = task().copy(publishedAtMillis = base + 9999, publicationIsLegacy = true)
        val events = ProgressJourneyEngine.build(
            listOf(future),
            listOf(CreatorIdea(id = "deleted", title = "Private", createdAtMillis = base, trashedAtMillis = base+1),
                CreatorIdea(id = "future", title = "Future", createdAtMillis = base + 99_999)),
            emptyList(), base + 100,
        )
        assertTrue(events.isEmpty())
    }

    @Test fun datesRespectRequestedZoneInsteadOfUtcOrSystemDefault() {
        val nearMidnight = 1_780_000_000_000L
        val events = listOf(ProgressJourneyEvent("e", ProgressJourneyKind.IDEA_CAPTURED,
            "Idea", "Idea saved", nearMidnight, "i1"))
        val zone = ZoneId.of("Asia/Kolkata")
        val day = events.single().localDate(zone)
        assertEquals(1, ProgressJourneyEngine.onDay(events, day, zone).size)
        assertEquals(setOf(day), ProgressJourneyEngine.activeDays(events, zone))
        assertFalse(ProgressJourneyEngine.onDay(events, LocalDate.of(2020, 1, 1), zone).isNotEmpty())
    }
}
