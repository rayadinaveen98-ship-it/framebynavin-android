package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.IdeaStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/** Stage only: no execution until founder authorizes consolidated QA/APK. */
class P8IdeaVaultPresentationTest {
    private fun idea(id: String, status: IdeaStatus): CreatorIdea =
        CreatorIdea(id = id, title = "Idea " + id, status = status)

    @Test fun summaryCountsOnlyActualSavedStatuses() {
        val ideas = listOf(
            idea("inbox", IdeaStatus.INBOX),
            idea("worth", IdeaStatus.WORTH_EXPLORING),
            idea("research", IdeaStatus.RESEARCHING),
            idea("ready", IdeaStatus.READY_TO_PRODUCE),
            idea("converted", IdeaStatus.CONVERTED),
            idea("archived", IdeaStatus.ARCHIVED),
        )
        val summary = P8IdeaVaultSummary.from(ideas)
        assertEquals(6, summary.total)
        assertEquals(1, summary.inbox)
        assertEquals(2, summary.exploring)
        assertEquals(1, summary.ready)
        assertEquals(1, summary.converted)
        assertEquals(1, summary.archived)
    }

    @Test fun emptyVaultDoesNotInventWorkOrProgress() {
        val summary = P8IdeaVaultSummary.from(emptyList())
        assertEquals(0, summary.total)
        assertEquals(0, summary.inbox)
        assertEquals(0, summary.exploring)
        assertEquals(0, summary.ready)
        assertEquals(0, summary.converted)
        assertEquals(0, summary.archived)
        assertEquals(P8IdeaVaultEmptyMode.EMPTY_VAULT,
            P8IdeaVaultEmptyPolicy.mode(
                showingDeleted = false, allIdeasEmpty = true,
                deletedIdeasEmpty = true))
    }

    @Test fun nonMatchingFiltersNeverClaimVaultWasErased() {
        assertEquals(P8IdeaVaultEmptyMode.NO_RESULTS,
            P8IdeaVaultEmptyPolicy.mode(
                showingDeleted = false, allIdeasEmpty = false,
                deletedIdeasEmpty = true))
        assertEquals(P8IdeaVaultEmptyMode.NO_RESULTS,
            P8IdeaVaultEmptyPolicy.mode(
                showingDeleted = true, allIdeasEmpty = false,
                deletedIdeasEmpty = false))
    }

    @Test fun recentlyDeletedIsDistinctFromArchive() {
        assertEquals(P8IdeaVaultEmptyMode.RECENTLY_DELETED_EMPTY,
            P8IdeaVaultEmptyPolicy.mode(
                showingDeleted = true, allIdeasEmpty = false,
                deletedIdeasEmpty = true))
        val summary = P8IdeaVaultSummary.from(
            listOf(idea("archived", IdeaStatus.ARCHIVED)))
        assertEquals(1, summary.archived)
        assertEquals(0, summary.converted)
    }

    @Test fun fiftyIdeasAreCountedWithoutSamplingOrArtificialLimits() {
        val ideas = (1..57).map { i ->
            idea(i.toString(), if (i <= 50) IdeaStatus.INBOX
                 else IdeaStatus.READY_TO_PRODUCE)
        }
        val summary = P8IdeaVaultSummary.from(ideas)
        assertEquals(57, summary.total)
        assertEquals(50, summary.inbox)
        assertEquals(7, summary.ready)
    }
}
