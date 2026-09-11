package com.framebynavin.app.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeInsightsFoundationV2Test {
    @Test
    fun metricContractFlagsWindowCrossingAugust2026ViewBoundary() {
        val crossing = YouTubeMetricContract.forWindow("2026-08-15", "2026-09-11")
        val after = YouTubeMetricContract.forWindow("2026-09-01", "2026-09-11")

        assertTrue(crossing.crossesPublicViewBoundary)
        assertTrue(crossing.note.contains("engaged views", ignoreCase = true))
        assertFalse(after.crossesPublicViewBoundary)
        assertEquals(YouTubeMetricContract.PUBLIC_VIEWS_ID, after.viewDefinitionId)
        assertEquals(YouTubeMetricContract.ENGAGED_VIEWS_ID, after.engagedViewDefinitionId)
    }

    @Test
    fun breakdownShareIsDeterministicAndClamped() {
        val row = YouTubeBreakdownRow("MOBILE", views = 250, engagedViews = 200, watchMinutes = 800)
        assertEquals(25, row.shareOf(1_000))
        assertEquals(0, row.shareOf(0))
        assertEquals(100, row.copy(views = 2_000).shareOf(1_000))
    }

    @Test
    fun readyDatasetCountDoesNotTreatEmptyOrPendingAsReady() {
        val foundation = YouTubeInsightsFoundationSnapshot(
            channelId = "channel",
            windowDays = 28,
            startDate = "2026-08-15",
            endDate = "2026-09-11",
            fetchedAtMillis = 1L,
            periodViews = 10,
            periodEngagedViews = 8,
            periodWatchMinutes = 20,
            metricContract = YouTubeMetricContract.forWindow("2026-08-15", "2026-09-11"),
            trafficSources = emptyList(),
            subscribedStatus = emptyList(),
            deviceTypes = emptyList(),
            operatingSystems = emptyList(),
            countries = emptyList(),
            retention = emptyList(),
            health = listOf(
                YouTubeDatasetHealth(YouTubeFoundationDataset.SUMMARY, YouTubeDatasetState.READY),
                YouTubeDatasetHealth(YouTubeFoundationDataset.TRAFFIC, YouTubeDatasetState.EMPTY),
                YouTubeDatasetHealth(YouTubeFoundationDataset.REACH, YouTubeDatasetState.NOT_CONFIGURED),
            ),
        )

        assertEquals(1, foundation.readyDatasetCount())
    }
}
