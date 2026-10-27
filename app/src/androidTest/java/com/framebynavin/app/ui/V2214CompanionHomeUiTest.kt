package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Home integration: original character, user decision and real routes. */
@RunWith(AndroidJUnit4::class)
class V2214CompanionHomeUiTest {
    @get:Rule val rule = createComposeRule()

    private fun prefs() = InstrumentationRegistry.getInstrumentation().targetContext
        .getSharedPreferences("backlot_companion_suggestions_v2212", 0)

    private fun task(): CreatorTask = CreatorTask(
        id = "companion-home-test-project",
        title = "An unfinished short film",
        platform = "YouTube",
        contentType = "Short",
        dueLabel = "Tomorrow",
        status = TaskStatus.WORKING,
        dueAtMillis = System.currentTimeMillis() + 2 * 3_600_000L,
    )

    private fun screenshot(label: String) {
        val i = InstrumentationRegistry.getInstrumentation()
        val profile = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
        require(profile == "default" || profile == "compact")
        rule.waitForIdle()
        i.waitForIdleSync()
        SystemClock.sleep(650)
        val fileDir = File(i.targetContext.filesDir, "v2214-home")
        assertTrue(fileDir.isDirectory || fileDir.mkdirs())
        val file = File(fileDir, "v2214-$profile-$label.png")
        val shot = requireNotNull(i.uiAutomation.takeScreenshot())
        try { file.outputStream().use { assertTrue(shot.compress(Bitmap.CompressFormat.PNG,100,it)) } }
        finally { shot.recycle() }
        assertTrue("Blank Home integration preview: $file", file.length() > 24_000)
    }

    @Test fun realHomeCardOpensExactProjectAndNotNowSilencesTheVisit() {
        HardeningTestEnvironment.requireCiEmulator()
        prefs().edit().clear().commit()
        var opened: String? = null
        try {
            rule.setContent {
                FrameByNavinTheme {
                    Column {
                        V2214HomeCompanion(
                            tasks = listOf(task()), ideas = emptyList(),
                            onOpenReminders = {},
                            onOpenProject = { opened = it },
                            onOpenIdeas = {},
                        )
                    }
                }
            }
            rule.onNodeWithTag("v2212-companion-card").assertIsDisplayed()
            rule.onNodeWithText("Open Project").assertIsDisplayed()
            screenshot("project")
            rule.onNodeWithTag("v2212-companion-action").performClick()
            rule.runOnIdle { assertEquals("companion-home-test-project", opened) }
            rule.onNodeWithTag("v2212-companion-dismiss").performClick()
            rule.onNodeWithTag("v2212-companion-card").assertDoesNotExist()
            rule.runOnIdle {
                assertTrue(V221CompanionDismissalStore(
                    InstrumentationRegistry.getInstrumentation().targetContext
                ).suppressed(System.currentTimeMillis()).isNotEmpty())
            }
        } finally { prefs().edit().clear().commit() }
    }

    @Test fun disabledCompanionIsTrulyQuietAndNeverChangesProjects() {
        HardeningTestEnvironment.requireCiEmulator()
        prefs().edit().clear().commit()
        try {
            assertTrue(V221CompanionDismissalStore(
                InstrumentationRegistry.getInstrumentation().targetContext
            ).setEnabled(false))
            rule.setContent {
                FrameByNavinTheme {
                    V2214HomeCompanion(
                        tasks = listOf(task()), ideas = emptyList(),
                        onOpenReminders = {},
                        onOpenProject = {},
                        onOpenIdeas = {},
                    )
                }
            }
            rule.onNodeWithTag("v2212-companion-card").assertDoesNotExist()
        } finally { prefs().edit().clear().commit() }
    }

    @Test fun actualInboxIdeaGoesToIdeaVaultWithoutInventingProjects() {
        HardeningTestEnvironment.requireCiEmulator()
        prefs().edit().clear().commit()
        var ideaOpens = 0
        try {
            rule.setContent {
                FrameByNavinTheme {
                    V2214HomeCompanion(
                        tasks = emptyList(),
                        ideas = listOf(CreatorIdea(id = "idea-one", title = "Intro")),
                        onOpenReminders = {},
                        onOpenProject = { throw AssertionError("No project exists") },
                        onOpenIdeas = { ideaOpens++ },
                    )
                }
            }
            rule.onNodeWithTag("v2212-companion-card").assertIsDisplayed()
            rule.onNodeWithText("Open Ideas").assertIsDisplayed()
            screenshot("idea")
            rule.onNodeWithTag("v2212-companion-action").performClick()
            rule.runOnIdle { assertEquals(1, ideaOpens) }
        } finally { prefs().edit().clear().commit() }
    }
}
