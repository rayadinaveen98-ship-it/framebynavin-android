package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.CreatorOsSettings
import com.framebynavin.app.data.VoicePersona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * V207-A/B UI regression: full-bleed Settings surface, compact voice choice,
 * preview must not change selection, and committed selection must reach caller.
 */
class V207FullscreenVoiceUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun settingsBackgroundCoversUnderlyingHomeAtBothSystemBarEdges() {
        rule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize().testTag("v207-activity-root")) {
                    V144SettingsHub(
                        settings = CreatorOsSettings(),
                        tasks = emptyList(),
                        weeklySlots = emptyList(),
                        weeklyAutoPlanEnabled = false,
                        notificationsReady = true,
                        preciseTimingReady = true,
                        fullScreenReady = true,
                        batteryReady = true,
                        onClose = {},
                        onProfile = {},
                        onVoice = {},
                        onAlarmTimeout = {},
                        onSnooze = {},
                        onWeeklyAutoPlan = {},
                        onContextNudges = {},
                        onNotifications = {},
                        onPreciseTiming = {},
                        onFullScreen = {},
                        onBattery = {},
                        onReplayTour = {},
                        onCloudSync = {},
                        onYouTube = {},
                    )
                }
            }
        }
        val root = rule.onNodeWithTag("v207-activity-root").getUnclippedBoundsInRoot()
        val cover = rule.onNodeWithTag("v207-settings-fullscreen").getUnclippedBoundsInRoot()
        assertTrue("Settings should start at the Activity top, not below the status bar", cover.top <= root.top + 1.dp)
        assertTrue("Settings must cover Home at the bottom", cover.bottom >= root.bottom - 1.dp)
        assertTrue("Settings must cover both horizontal edges", cover.left <= root.left + 1.dp && cover.right >= root.right - 1.dp)
        rule.onNodeWithText("Backlot, your way").assertIsDisplayed()
    }

    @Test fun projectVoiceStartsCompactAndPreviewNeverSelects() {
        val picked = mutableStateOf(VoicePersona.WARM)
        val previews = mutableIntStateOf(0)
        rule.setContent {
            MaterialTheme {
                V207ProjectVoiceSelector(
                    selected = picked.value,
                    onSelected = { picked.value = it },
                    onPreview = { previews.intValue++ },
                )
            }
        }
        rule.onNodeWithTag("v207-voice-collapsed").assertIsDisplayed()
        rule.onNodeWithTag("v207-voice-dialog").assertDoesNotExist()
        rule.onNodeWithTag("v207-voice-quick-preview").performClick()
        rule.runOnIdle {
            assertEquals(VoicePersona.WARM, picked.value)
            assertEquals(1, previews.intValue)
        }
        rule.onNodeWithTag("v207-voice-collapsed").performClick()
        rule.onNodeWithTag("v207-voice-dialog").assertIsDisplayed()
        rule.onNodeWithTag("v207-voice-option-preview-WOMAN").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(VoicePersona.WARM, picked.value)
            assertEquals(2, previews.intValue)
        }
        rule.onNodeWithTag("v207-voice-option-WOMAN").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(VoicePersona.WOMAN, picked.value) }
        rule.onNodeWithTag("v207-voice-dialog").assertDoesNotExist()
        rule.onNodeWithText("Clear Human").assertIsDisplayed()
    }

    @Test fun pickerCanCloseWithoutChangingProjectVoice() {
        val picked = mutableStateOf(VoicePersona.MAN)
        rule.setContent {
            MaterialTheme {
                V207ProjectVoiceSelector(
                    selected = picked.value,
                    onSelected = { picked.value = it },
                    onPreview = {},
                )
            }
        }
        rule.onNodeWithTag("v207-voice-collapsed").performClick()
        rule.onNodeWithContentDescription("Close voice picker").performClick()
        rule.runOnIdle { assertEquals(VoicePersona.MAN, picked.value) }
        rule.onNodeWithTag("v207-voice-dialog").assertDoesNotExist()
    }
}
