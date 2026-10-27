package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import android.graphics.Bitmap
import java.io.File

class V185IdeaConversionUiTest {
    @get:Rule val rule = createComposeRule()
    @Before fun reset() {
        HardeningTestEnvironment.requireCiEmulator()
        InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("idea_conversion_v185", 0).edit().clear().commit()
    }
    @Test fun saveWaitsForAcceptanceAndFailedRetryKeepsReminderSelection() {
        val release = CompletableDeferred<String?>()
        var attempts = 0
        var accepted = 0
        var request: IdeaProjectConversionRequest? = null
        rule.setContent {
            FrameByNavinTheme {
            V09ConvertIdeaDialog(CreatorIdea(id = "idea", title = "తెలుగు camera idea"), onDismiss = {}, conversionFailed = false,
                onConvert = { _, _, _ -> error("Must use durable writer") },
                onConvertDurably = { request = it; attempts++; if (attempts == 1) release.await() else "project" },
                onConverted = { accepted++ })
            }
        }
        rule.onNodeWithTag("conversion-plan-OFF").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("conversion-plan-LIGHT").performScrollTo().performClick()
        rule.onNodeWithTag("conversion-plan-LIGHT").assertIsSelected()
        rule.onNodeWithTag("conversion-create").performClick()
        rule.waitUntil(30_000) { attempts == 1 }
        rule.onNodeWithTag("conversion-create").assertIsNotEnabled()
        rule.onNodeWithTag("conversion-cancel").assertIsNotEnabled()
        rule.runOnIdle { assertEquals(0, accepted); assertEquals(ProjectAttentionPlan.LIGHT, request!!.attentionPlan); release.complete(null) }
        rule.onNodeWithTag("conversion-error").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("conversion-plan-LIGHT").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("conversion-create").performClick()
        rule.waitUntil(30_000) { accepted == 1 }
        assertEquals(2, attempts)
    }

    @Test fun thrownSaveFailureDoesNotAnnounceSuccessAndLeavesDialogUsable() {
        var accepted = false
        rule.setContent {
            FrameByNavinTheme {
            V09ConvertIdeaDialog(CreatorIdea(id = "idea", title = "Camera movement idea"), onDismiss = {}, conversionFailed = false,
                onConvert = { _, _, _ -> null }, onConvertDurably = { error("Could not save. Your idea was kept.") },
                onConverted = { accepted = true })
            }
        }
        rule.onNodeWithTag("conversion-plan-description").performScrollTo().assertTextContains("No automatic reminders", substring = true)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.filesDir, "v185-conversion-dialog.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
        rule.onNodeWithTag("conversion-create").performClick()
        rule.onNodeWithTag("conversion-error").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("conversion-create").assertIsEnabled()
        rule.onNodeWithTag("conversion-cancel").assertIsEnabled()
        assertFalse(accepted)
    }
}
