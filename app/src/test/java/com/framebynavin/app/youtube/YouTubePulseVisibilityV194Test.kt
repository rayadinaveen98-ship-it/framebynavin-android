package com.framebynavin.app.youtube

import org.junit.Assert.*
import org.junit.Test

class YouTubePulseVisibilityV194Test {
    private val hour = 3_600_000L
    private val now = 1_000L * hour

    private fun report(
        channel: String = "channel-a",
        current: Long = now - hour,
        baseline: Long = current - 24L * hour,
    ) = YouTube24HourReport(
        sampleHours = 24, viewsGained = 500, subscribersDelta = 2,
        previousViewsGained = 400, viewsChangePercent = 25,
        momentum = YouTubePulseMomentum.RISING, topMovers = emptyList(),
        currentCapturedAtMillis = current, baselineCapturedAtMillis = baseline,
        channelId = channel,
    )

    @Test fun validCurrentOwnerAndSnapshotAreVisible() {
        val source = report()
        assertSame(source, YouTubePulseVisibilityPolicy.visible(
            source, "channel-a", now - hour, now))
    }

    @Test fun switchedAccountCannotSeePreviousAccountPulse() {
        assertNull(YouTubePulseVisibilityPolicy.visible(
            report(channel = "channel-a"), "channel-b", now, now))
    }

    @Test fun legacyUnownedPulseDoesNotLeakToAnyAccount() {
        assertNull(YouTubePulseVisibilityPolicy.visible(
            report(channel = ""), "channel-a", now, now))
    }

    @Test fun reportOlderThanThirtySixHoursIsUnavailable() {
        val source = report(current = now - 37L * hour)
        assertNull(YouTubePulseVisibilityPolicy.visible(
            source, "channel-a", now, now))
    }

    @Test fun oneOlderThanSnapshotCannotShowAFuturePulse() {
        val source = report(current = now - hour)
        assertNull(YouTubePulseVisibilityPolicy.visible(
            source, "channel-a", now - 2L * hour, now))
    }

    @Test fun rejectMissingSelectedReportAndInvalidChronology() {
        assertNull(YouTubePulseVisibilityPolicy.visible(null, "channel-a", now, now))
        assertNull(YouTubePulseVisibilityPolicy.visible(report(), "", now, now))
        assertNull(YouTubePulseVisibilityPolicy.visible(report(), "channel-a", 0L, now))
        val invalid = report(baseline = now)
        assertNull(YouTubePulseVisibilityPolicy.visible(invalid, "channel-a", now, now))
    }

    @Test fun tolerateMinorClockSkewButNotFutureDates() {
        val near = report(current = now + 4L * 60_000L)
        assertSame(near, YouTubePulseVisibilityPolicy.visible(
            near, "channel-a", now + 5L * 60_000L, now))
        val future = report(current = now + 7L * 60_000L)
        assertNull(YouTubePulseVisibilityPolicy.visible(
            future, "channel-a", now + 8L * 60_000L, now))
    }
}
