package com.framebynavin.app.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Reproduces the v0 seeded CreatorTask constructor from historical source:
 * commit 15aa6faf64525ff043a1b184598580cdde5c360a.
 * Does not open or mutate the user's DataStore.
 */
class V230LegacyStarterProjectPolicyTest {
    private fun original() = CreatorTask(
        id = "starter-frame-breakdown",
        title = "Frame Breakdown",
        platform = "Instagram",
        contentType = "Reel",
        dueLabel = "Today · 7:00 PM",
        status = TaskStatus.WORKING,
        progress = 72,
    )

    @Test fun onlyExactOriginalV0DemoIsEligibleForAutomaticCleanup() {
        val untouched = original()
        assertTrue(V230LegacyStarterProjectPolicy.isUntouchedDemo(untouched))
        assertTrue(V230LegacyStarterProjectPolicy.withoutUntouchedDemos(listOf(untouched)).isEmpty())
    }

    @Test fun changesToTitleNotesOrDeliveryFieldsPreserveSameIdProject() {
        val source = original()
        val edited = listOf(
            source.copy(title = "My film breakdown"),
            source.copy(notes = "I added shot analysis"),
            source.copy(platform = "YouTube"),
            source.copy(contentType = "Video Essay"),
            source.copy(dueLabel = "Next week"),
            source.copy(status = TaskStatus.PLANNED),
            source.copy(progress = 73),
        )
        assertTrue(edited.none(V230LegacyStarterProjectPolicy::isUntouchedDemo))
        assertEquals(edited, V230LegacyStarterProjectPolicy.withoutUntouchedDemos(edited))
    }

    @Test fun creatorWorkflowsRemindersAndAssetsAreNotDestroyedByLegacyId() {
        val source = original()
        val edited = listOf(
            source.copy(reminderEnabled = true, reminderAtMillis = 1_800_000_000_000L),
            source.copy(workflowStageIndex = 2),
            source.copy(workspace = CreatorContentWorkspace(script = "Saved cinema narration")),
            source.copy(workspace = CreatorContentWorkspace(revision = 1L)),
            source.copy(archivedAtMillis = 1_800_000_000_000L),
            source.copy(publishedUrl = "https://example.com/original"),
            source.copy(sourceRefId = "creator:user-work"),
        )
        assertTrue(edited.none(V230LegacyStarterProjectPolicy::isUntouchedDemo))
        assertEquals(edited, V230LegacyStarterProjectPolicy.withoutUntouchedDemos(edited))
    }

    @Test fun cleanupPreservesStableOrderAndNeverRemovesOtherProjects() {
        val untouched = original()
        val first = CreatorTask(
            id = "real-video-one", title = "My First Video", platform = "YouTube",
            contentType = "Long-form", dueLabel = "Friday",
        )
        val edited = original().copy(title = "Personal project from seed")
        val later = CreatorTask(
            id = "real-video-two", title = "Lighting study", platform = "YouTube",
            contentType = "Short", dueLabel = "Tomorrow",
        )
        val mixed = listOf(first, untouched, edited, later)
        val after = V230LegacyStarterProjectPolicy.withoutUntouchedDemos(mixed)
        assertEquals(listOf(first, edited, later), after)
        assertEquals(mixed, listOf(first, untouched, edited, later))
    }

    @Test fun cleanUpIsIdempotentAndCannotAlterAnAlreadyCleanProjectList() {
        val record = original().copy(notes = "Creator notes")
        val originalList = listOf(record)
        val once = V230LegacyStarterProjectPolicy.withoutUntouchedDemos(originalList)
        val twice = V230LegacyStarterProjectPolicy.withoutUntouchedDemos(once)
        assertEquals(originalList, once)
        assertEquals(once, twice)
    }
}
