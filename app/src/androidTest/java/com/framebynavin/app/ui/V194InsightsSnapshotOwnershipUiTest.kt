package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import com.framebynavin.app.youtube.*
import org.junit.Rule
import org.junit.Test

class V194InsightsSnapshotOwnershipUiTest {
    @get:Rule val rule = createComposeRule()

    private fun snapshot(channelId: String, fetchedAtMillis: Long) = YouTubeAnalyticsSnapshot(
        channel = YouTubeChannelSnapshot(channelId, "Channel", 22, 1000, 4, "uploads"),
        windowDays = 28, startDate = "2026-09-01", endDate = "2026-09-28",
        views = 100, watchMinutes = 40, averageViewDurationSeconds = 24,
        subscribersGained = 2, subscribersLost = 0, likes = 4, comments = 1,
        topVideos = emptyList(), recentVideos = emptyList(), trend = emptyList(),
        fetchedAtMillis = fetchedAtMillis,
        reportEvidence = YouTubeReportEvidence(
            true, true, setOf("views", "estimatedMinutesWatched",
                "subscribersGained", "subscribersLost", "averageViewDuration"),
            setOf("views", "estimatedMinutesWatched", "subscribersGained",
                "subscribersLost", "averageViewDuration"), emptyList()),
    )

    private fun openSignal() {
        rule.onNodeWithText("Unavailable").performClick()
        rule.onNodeWithText("24H PULSE").assertExists()
    }

    @Test fun switchingChannelClosesPreviousChannelSignalDetail() {
        HardeningTestEnvironment.requireCiEmulator()
        var channel by mutableStateOf("v194-owner-a")
        rule.setContent {
            FrameByNavinTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    V172InsightsBody(snapshot(channel, 100L), emptyList(), emptyList(),
                        emptyMap(), onLinkVideo = {})
                }
            }
        }
        openSignal()
        rule.runOnIdle { channel = "v194-owner-b" }
        rule.waitForIdle()
        rule.onNodeWithText("24H PULSE").assertDoesNotExist()
    }

    @Test fun refreshedSnapshotClosesOldSignalDetail() {
        HardeningTestEnvironment.requireCiEmulator()
        var fetchedAt by mutableLongStateOf(100L)
        rule.setContent {
            FrameByNavinTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    V172InsightsBody(snapshot("v194-owner-a", fetchedAt), emptyList(),
                        emptyList(), emptyMap(), onLinkVideo = {})
                }
            }
        }
        openSignal()
        rule.runOnIdle { fetchedAt = 200L }
        rule.waitForIdle()
        rule.onNodeWithText("24H PULSE").assertDoesNotExist()
    }
}
