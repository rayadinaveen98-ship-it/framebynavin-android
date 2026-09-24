package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.youtube.YouTubeRevenuePeriod
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V145RevenueAccessUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun enabledRevenueWithoutCachedPeriod_doesNotAskForConsentAgain() {
        composeRule.setContent {
            V144YouTubeRevenueCard(
                snapshot = null,
                selectedPeriod = YouTubeRevenuePeriod.NINETY_DAYS,
                accessEnabled = true,
                loading = true,
                error = null,
                onPeriod = {},
                onRefresh = {},
            )
        }

        composeRule.onNodeWithText("ENABLE REVENUE").assertDoesNotExist()
        composeRule.onNodeWithText("Loading 90D revenue…").assertIsDisplayed()
    }
}
