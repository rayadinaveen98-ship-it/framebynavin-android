package com.framebynavin.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UX12 D3 source-only acceptance tests; intentionally NOT EXECUTED until
 * founder authorizes the cumulative final UX12 QA and Lab APK stage.
 */
class UX12D3ArrivalPolicyTest {

    @Test
    fun arrivalRequiresCommittedSetupAndNormalAppEntry() {
        assertTrue(
            UX12D3ArrivalPolicy.shouldPresent(
                setupCommitted = true,
                externalLaunch = false,
            )
        )
        assertFalse(
            UX12D3ArrivalPolicy.shouldPresent(
                setupCommitted = false,
                externalLaunch = false,
            )
        )
    }

    @Test
    fun notificationWidgetAndDeepLinkEntryNeverShowArrival() {
        assertFalse(
            UX12D3ArrivalPolicy.shouldPresent(
                setupCommitted = true,
                externalLaunch = true,
            )
        )
        assertFalse(
            UX12D3ArrivalPolicy.shouldPresent(
                setupCommitted = false,
                externalLaunch = true,
            )
        )
    }

    @Test
    fun decorativeArrivalHasShortBoundedMotionAndReducedMotionPath() {
        assertTrue(UX12D3ArrivalPolicy.MOTION_DURATION_MS in 2_000L..3_000L)
        assertTrue(UX12D3ArrivalPolicy.REDUCED_MOTION_DURATION_MS in 0L..400L)
        assertTrue(
            UX12D3ArrivalPolicy.REDUCED_MOTION_DURATION_MS <
                UX12D3ArrivalPolicy.MOTION_DURATION_MS
        )
    }
}
