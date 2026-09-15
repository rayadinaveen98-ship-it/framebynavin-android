package com.framebynavin.app.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.VoicePersona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v141 regression proof for the Settings -> New Project voice contract.
 *
 * These tests intentionally live only on the validation branch. Production code remains identical
 * to the frozen release/backlot-v141-candidate source.
 */
@RunWith(AndroidJUnit4::class)
class V141VoiceInheritanceInstrumentedTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsVoicePersistsAcrossFreshStoreInstances() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = CreatorOsSettingsStore(context)
        val original = store.exportJson()

        try {
            store.setDefaultVoicePersona(VoicePersona.ROBOT)

            val reopenedStore = CreatorOsSettingsStore(context)
            assertEquals(VoicePersona.ROBOT, reopenedStore.snapshot().defaultVoicePersona)
        } finally {
            store.importJson(original)
        }
    }

    @Test
    fun newProjectReadsSettingsVoiceAndSavesInheritedPersona() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = CreatorOsSettingsStore(context)
        val original = store.exportJson()
        var settingsOpened = false
        var savedDraft: PProjectDraft? = null

        try {
            store.setCreatorProfile(
                CreatorProfile(
                    displayName = "V141 Test Creator",
                    primaryCreatorMode = "film_entertainment",
                    platforms = setOf("YouTube"),
                    primaryGoal = "Publish consistently",
                    weeklyPublishingTarget = 2,
                    setupSchemaVersion = CreatorProfile.CURRENT_SCHEMA_VERSION,
                ),
            )
            store.setDefaultVoicePersona(VoicePersona.ROBOT)

            composeRule.setContent {
                PProjectComposer(
                    task = null,
                    reminderSetupReady = true,
                    onDismiss = {},
                    onOpenSettings = { settingsOpened = true },
                    onSave = { savedDraft = it },
                    onRemoveReminder = {},
                )
            }

            composeRule.onNode(hasSetTextAction()).performTextInput("V141 inherited voice project")
            listOf(
                "CHOOSE PROJECT TYPE",
                "CHOOSE PLATFORM",
                "CHOOSE FORMAT",
                "SET DEADLINE",
                "CHOOSE SUPPORT",
            ).forEach { label ->
                composeRule.onNodeWithText(label).performScrollTo().assertIsDisplayed().performClick()
                composeRule.waitForIdle()
            }

            composeRule.onNodeWithText("VOICE · FROM SETTINGS").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("Robot").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("CHANGE IN SETTINGS").performScrollTo().performClick()
            composeRule.runOnIdle { assertTrue(settingsOpened) }

            composeRule.onNodeWithText("REVIEW PROJECT").performScrollTo().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("CREATE & OPEN WORKSPACE").performScrollTo().assertIsDisplayed().performClick()

            composeRule.runOnIdle {
                assertNotNull(savedDraft)
                assertEquals(VoicePersona.ROBOT, savedDraft?.voicePersona)
            }
        } finally {
            store.importJson(original)
        }
    }
}
