package com.framebynavin.app

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real MainActivity startup pipeline long enough for recovery + the five-second
 * cinematic welcome hand-off to complete. This catches startup/runtime crashes that a pure Compose
 * component test cannot see.
 */
@RunWith(AndroidJUnit4::class)
class V141StartupSmokeInstrumentedTest {

    @Test
    fun mainActivitySurvivesRecoveryAndWelcomeHandoff() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            Thread.sleep(7_000L)

            assertTrue(scenario.state.isAtLeast(Lifecycle.State.RESUMED))
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                assertFalse(activity.isDestroyed)
            }
        }
    }
}
