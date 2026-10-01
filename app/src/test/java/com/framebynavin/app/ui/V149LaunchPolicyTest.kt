package com.framebynavin.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V149LaunchPolicyTest {
    @Test
    fun normalFirstLaunchShowsIdent() {
        assertTrue(V149LaunchPolicy.shouldShowCinematicIdent(externalLaunch = false, hasSeenIdent = false))
    }

    @Test
    fun returningNormalLaunchSkipsIdent() {
        assertFalse(V149LaunchPolicy.shouldShowCinematicIdent(externalLaunch = false, hasSeenIdent = true))
    }

    @Test
    fun widgetOrDeepLinkLaunchAlwaysSkipsIdent() {
        assertFalse(V149LaunchPolicy.shouldShowCinematicIdent(externalLaunch = true, hasSeenIdent = false))
        assertFalse(V149LaunchPolicy.shouldShowCinematicIdent(externalLaunch = true, hasSeenIdent = true))
    }
}
