package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import com.framebynavin.app.data.CreatorGuidedTourStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Navigation layout supplies actual spotlight rectangles; no screenshot-size guesses. */
class V207GuidedTourUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun guideNavTargetsAreMeasuredInRootOnCurrentLayout() {
        val measured = mutableStateMapOf<PTab, Rect>()
        rule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    PBottomNav(
                        selected = PTab.TODAY,
                        onSelect = {},
                        onGuideTarget = { tab, bounds -> measured[tab] = bounds },
                    )
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            for (tab in listOf(PTab.TODAY, PTab.IDEAS, PTab.CREATE, PTab.INSIGHTS)) {
                val bounds = measured[tab] ?: error("No measured target for $tab")
                assertTrue("$tab has nonzero width", bounds.width > 28f)
                assertTrue("$tab has nonzero height", bounds.height > 28f)
                assertTrue("$tab starts inside UI", bounds.left >= 0f && bounds.top >= 0f)
            }
            assertTrue(measured[PTab.TODAY]!!.left < measured[PTab.IDEAS]!!.left)
            assertTrue(measured[PTab.CREATE]!!.left < measured[PTab.INSIGHTS]!!.left)
        }
    }

    @Test fun optionalCoachHasFourProgressSegmentsAndExplicitExit() {
        var next = 0
        var exit = 0
        rule.setContent {
            MaterialTheme {
                V20GuidedFirstRunCoach(
                    step = CreatorGuidedTourStep.TODAY,
                    hasProjects = false,
                    onPrimary = { next++ },
                    onSecondary = {},
                    onSkip = { exit++ },
                )
            }
        }
        rule.onNodeWithText("HOME · 1/4").assertExists()
        rule.onNodeWithText("NEXT").performClick()
        rule.onNodeWithText("EXIT GUIDE").performClick()
        rule.runOnIdle {
            assertEquals(1, next)
            assertEquals(1, exit)
        }
    }
}
