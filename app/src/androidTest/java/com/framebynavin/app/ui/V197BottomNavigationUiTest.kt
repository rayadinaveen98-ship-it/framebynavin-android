package com.framebynavin.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class V197BottomNavigationUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun navigationBarIsOnscreenWithLargeAccessibilityFont() {
        rule.setContent {
            val current = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(current.density, 1.8f)) {
                MaterialTheme {
                    Box(Modifier.fillMaxSize()) {
                        PBottomNav(
                            selected = PTab.INSIGHTS,
                            onSelect = {},
                            onCapture = {},
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("backlot-bottom-nav"),
                        )
                    }
                }
            }
        }
        val viewport = rule.onRoot().getUnclippedBoundsInRoot()
        val nav = rule.onNodeWithTag("backlot-bottom-nav").getUnclippedBoundsInRoot()
        assertTrue("Bottom nav must be fully on screen: $nav vs $viewport", nav.bottom <= viewport.bottom)
        assertTrue("Bottom nav left is clipped", nav.left >= viewport.left)
        assertTrue("Bottom nav right is clipped", nav.right <= viewport.right)
        rule.onNodeWithText("Insights", useUnmergedTree = true).assertExists()
    }

    @Test fun bottomNavigationSwitchesTabsWithoutLosingSelection() {
        var selected = PTab.TODAY
        rule.setContent {
            MaterialTheme {
                PBottomNav(
                    selected = selected,
                    onSelect = { selected = it },
                    onCapture = {},
                )
            }
        }
        rule.onNodeWithText("Insights", useUnmergedTree = true).performClick()
        rule.runOnIdle { assertTrue(selected == PTab.INSIGHTS) }
    }
}
