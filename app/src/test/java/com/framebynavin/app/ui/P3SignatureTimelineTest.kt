package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-staged only. Execution belongs to the founder-approved consolidated
 * QA campaign after all design points have finished staging.
 */
class P3SignatureTimelineTest {
    @Test fun progressIsBoundedAndMonotonic() {
        assertEquals(0f, P3SignatureTimeline.smooth(-500L, 100L, 600L), 0f)
        assertEquals(0f, P3SignatureTimeline.smooth(100L, 100L, 600L), 0f)
        assertEquals(1f, P3SignatureTimeline.smooth(600L, 100L, 600L), 0f)
        assertEquals(1f, P3SignatureTimeline.smooth(5_000L, 100L, 600L), 0f)
        var previous = 0f
        for (time in 0L..3_000L step 100) {
            val next = P3SignatureTimeline.smooth(time, 100L, 600L)
            assertTrue(next >= previous)
            assertTrue(next in 0f..1f)
            previous = next
        }
    }

    @Test fun threePanelsAppearInFounderApprovedOrder() {
        assertTrue(P3SignatureTimeline.top(450L) >
            P3SignatureTimeline.middle(450L))
        assertTrue(P3SignatureTimeline.middle(700L) >
            P3SignatureTimeline.lower(700L))
        assertEquals(1f, P3SignatureTimeline.top(1_500L), 0f)
        assertEquals(1f, P3SignatureTimeline.middle(1_500L), 0f)
        assertEquals(1f, P3SignatureTimeline.lower(1_500L), 0f)
    }

    @Test fun assembledLogoAndTaglineReachCompleteStillFrame() {
        assertEquals(1f, P3SignatureTimeline.fullLogo(2_500L), 0f)
        assertEquals(0f, P3SignatureTimeline.splitVisibility(2_500L), 0f)
        assertEquals(1f, P3SignatureTimeline.wordmark(2_500L), 0f)
        assertEquals(1f, P3SignatureTimeline.tagline(2_500L), 0f)
        assertEquals(3_000L, P3SignatureTimeline.DURATION_MS)
        assertTrue(P3SignatureTimeline.REDUCED_DURATION_MS < 500L)
    }
}
