package com.framebynavin.app.youtube

import org.junit.Assert.*
import org.junit.Test

class YouTubePulseComputationV193Test {
    private val hour = 3_600_000L
    private val now = 1000L * hour
    private fun sample(
        hoursAgo: Long,
        lifetimeViews: Long,
        channel: String = "channel-a",
        subscribers: Long = 40,
        videos: List<YouTubePulseVideoCounter> = emptyList(),
    ) = YouTubePulseSample(channel, now - hoursAgo * hour,
        lifetimeViews, subscribers, videos)

    @Test fun equalRatesWithUnequalIntervalsAreSteady() {
        // Current: 900 views/18h; previous: 1200 views/24h. Both 50/hour.
        val report = YouTubePulseComputation.build(
            listOf(sample(42, 1000), sample(18, 2200), sample(0, 3100)), now)!!
        assertEquals(18, report.sampleHours)
        assertEquals(24, report.previousWindowHours)
        assertEquals(900L, report.viewsGained)
        assertEquals(1200L, report.previousViewsGained)
        assertEquals(0, report.viewsChangePercent)
        assertEquals(YouTubePulseMomentum.STEADY, report.momentum)
    }

    @Test fun normalizedRatesDetectRiseDespiteDifferentWindows() {
        // Previous 240/24h = 10/h; current 600/30h = 20/h, a 100% rise.
        val report = YouTubePulseComputation.build(
            listOf(sample(54, 1000), sample(30, 1240), sample(0, 1840)), now)!!
        assertEquals(100, report.viewsChangePercent)
        assertEquals(YouTubePulseMomentum.RISING, report.momentum)
    }

    @Test fun noPreviousBaselineIsNotSteady() {
        val report = YouTubePulseComputation.build(
            listOf(sample(24, 100), sample(0, 150)), now)!!
        assertEquals(YouTubePulseMomentum.INSUFFICIENT, report.momentum)
        assertNull(report.viewsChangePercent)
        assertNull(report.previousViewsGained)
    }

    @Test fun zeroPreviousGrowthIsNotEvidenceOfSteadiness() {
        val report = YouTubePulseComputation.build(
            listOf(sample(48, 100), sample(24, 100), sample(0, 105)), now)!!
        assertEquals(YouTubePulseMomentum.INSUFFICIENT, report.momentum)
        assertNull(report.viewsChangePercent)
    }

    @Test fun negativeChannelCounterIsFlaggedNotPresentedAsGrowth() {
        val report = YouTubePulseComputation.build(
            listOf(sample(48, 1000), sample(24, 1200), sample(0, 1140)), now)!!
        assertEquals(YouTubePulseMomentum.CORRECTION, report.momentum)
        assertEquals(-60L, report.counterAdjustment)
        assertEquals(0L, report.viewsGained)
        assertNull(report.viewsChangePercent)
        assertTrue(report.topMovers.isEmpty())
        assertTrue(YouTubeOpportunityEngine.build(report, emptyList()).isEmpty())
    }

    @Test fun oldLatestSampleNeverMasqueradesAsCurrentPulse() {
        val older = listOf(sample(80, 100), sample(56, 200))
        assertNull(YouTubePulseComputation.build(older, now))
    }

    @Test fun doNotCompareOtherAccountsOrVeryDifferentIntervals() {
        assertNull(YouTubePulseComputation.build(
            listOf(sample(24, 100, "other"), sample(0, 200)), now))
        assertNull(YouTubePulseComputation.build(
            listOf(sample(36, 100), sample(0, 200)), now))
    }

    @Test fun moversOnlyReflectSampledVideosAndRetainActualShare() {
        val old = YouTubePulseVideoCounter("one", "One", now - 100L * hour, 100)
        val newer = old.copy(lifetimeViews = 160)
        val report = YouTubePulseComputation.build(
            listOf(sample(24, 1000, videos = listOf(old)),
                sample(0, 1200, videos = listOf(newer))), now)!!
        assertEquals(60L, report.topMovers.single().viewsGained)
        assertEquals(30, report.topMovers.single().channelGainSharePercent)
    }
}
