package com.framebynavin.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V151LaunchGateTest {

    @Test
    fun normalLaunchAlwaysShowsIdent() {
        assertTrue(V151LaunchPolicy.shouldShowCinematicIdent(externalLaunch = false))
    }

    @Test
    fun widgetAndDeepLinkLaunchesStayInstant() {
        assertFalse(V151LaunchPolicy.shouldShowCinematicIdent(externalLaunch = true))
    }

    @Test
    fun audioAndVisualTimelineShareOneDuration() {
        assertTrue(WelcomeSonicIdent.TOTAL_DURATION_MS > 0L)
        assertTrue(WelcomeSonicIdent.SETTLE_START_MS < WelcomeSonicIdent.TOTAL_DURATION_MS)
        assertTrue(WelcomeSonicIdent.UNDERLINE_START_MS < WelcomeSonicIdent.UNDERLINE_END_MS)
    }
}
