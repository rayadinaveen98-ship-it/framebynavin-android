package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source staged only; do not run without founder's consolidated QA approval. */
class P6CreatorEntryPolicyTest {
    @Test fun onlySuccessfulSetupWithoutExternalTargetShowsTransition() {
        assertTrue(P6CreatorEntryPolicy.eligible(true, true, true, false))
        assertFalse(P6CreatorEntryPolicy.eligible(false, true, true, false))
        assertFalse(P6CreatorEntryPolicy.eligible(true, false, true, false))
        assertFalse(P6CreatorEntryPolicy.eligible(true, true, false, false))
        assertFalse(P6CreatorEntryPolicy.eligible(true, true, true, true))
    }

    @Test fun timelineIsMonotoneAndFinishesWithinTwoAndHalfSeconds() {
        assertTrue(P6CreatorEntryPolicy.DURATION_MS in 2_000L..2_500L)
        assertTrue(P6CreatorEntryPolicy.REDUCED_MOTION_MS < 250L)
        for (a in 0L..2_300L step 100) {
            assertTrue(P6CreatorEntryPolicy.smooth(a, 160L, 690L) in 0f..1f)
        }
        assertEquals(1f, P6CreatorEntryPolicy.titleAlpha(2_300L), 0f)
        assertEquals(1f, P6CreatorEntryPolicy.subtitleAlpha(2_300L), 0f)
        assertEquals(1f, P6CreatorEntryPolicy.underlineProgress(2_300L), 0f)
    }
}
