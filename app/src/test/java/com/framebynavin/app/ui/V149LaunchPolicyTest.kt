package com.framebynavin.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V151LaunchPolicyTest {
    @Test
    fun normalFirstLaunchShowsIdent() {
        assertTrue(V151LaunchPolicy.shouldShowCinematicIdent(externalLaunch = false, ))
    }

    @Test
    fun returningNormalLaunchSkipsIdent() {
        assertFalse(V151LaunchPolicy.shouldShowCinematicIdent(externalLaunch = false, ))
    }

    @Test
    fun widgetOrDeepLinkLaunchAlwaysSkipsIdent() {
        assertFalse(V151LaunchPolicy.shouldShowCinematicIdent(externalLaunch = true, ))
        assertFalse(V151LaunchPolicy.shouldShowCinematicIdent(externalLaunch = true, ))
    }
}
