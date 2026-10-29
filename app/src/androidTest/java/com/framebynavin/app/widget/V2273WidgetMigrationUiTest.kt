package com.framebynavin.app.widget

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.ui.V2273WidgetMigrationScreen
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2273WidgetMigrationUiTest {
    @get:Rule val rule = createComposeRule()

    private fun inventory(old: Int) = V2273WidgetInventory(
        newPinned = mapOf(
            V2273WidgetKind.QUICK_CAPTURE to 1,
            V2273WidgetKind.TODAY_FOCUS to 0,
            V2273WidgetKind.MY_DAY to 0,
        ),
        legacyPinned = mapOf("Compact" to old, "Creator Desk" to 0),
    )

    @Test fun previewKeepsAllThreePinOptionsAndLegacyInventoryReadOnly() {
        val requests = mutableListOf<V2273WidgetKind>()
        rule.setContent {
            FrameByNavinTheme {
                V2273WidgetMigrationScreen(
                    inventory = inventory(old = 2),
                    feedback = null,
                    canRequestPin = true,
                    onClose = {},
                    onRefresh = {},
                    onRequestPin = { requests.add(it) },
                )
            }
        }
        rule.onNodeWithTag("v2273-widget-counts").assertTextContains("OLDER: 2 PINNED", substring = true)
        for (kind in V2273WidgetKind.entries) {
            rule.onNodeWithTag("v2273-pin-${kind.name.lowercase()}")
                .performScrollTo().assertIsDisplayed()
                .performClick()
        }
        rule.runOnIdle { assertEquals(V2273WidgetKind.entries, requests) }
    }

    @Test fun unsupportedLauncherDisablesPinRequestsButExplainsManualOption() {
        rule.setContent {
            FrameByNavinTheme {
                V2273WidgetMigrationScreen(
                    inventory = inventory(old = 0),
                    feedback = null,
                    canRequestPin = false,
                    onClose = {},
                    onRefresh = {},
                    onRequestPin = { error("Pin requests should be disabled") },
                )
            }
        }
        rule.onNodeWithTag("v2273-pin-quick_capture").assertIsNotEnabled()
        rule.onNodeWithText("Your launcher doesn't support direct pin requests.", substring = true)
            .assertExists()
    }
}
