package com.framebynavin.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.R
import com.framebynavin.app.data.CreatorModeRegistry
import com.framebynavin.app.data.CreatorPlatformRegistry
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.reminders.ReminderCheckInKind
import com.framebynavin.app.reminders.V391ReminderScene
import com.framebynavin.app.reminders.V391ReminderScenePrefs
import com.framebynavin.app.reminders.V391ReminderSceneSettings
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Stage 3/4/6 acceptance SMOKE on disposable CI-emulator only.
 * Test-only UI composition + synthetic preferences; no network, owner data,
 * direct alarm broadcasts, production source or destructive real-device action.
 * This is not full end-to-end navigation, visual quality, or OEM certification.
 */
@RunWith(AndroidJUnit4::class)
class P1SixStageQuickstartSceneDeviceTest {
    @get:Rule val rule = createComposeRule()

    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun firstRunActuallyOffersCreatorPersonalAndBoth() {
        HardeningTestEnvironment.requireCiEmulator()
        var chosen: V393FirstRunFocus? = null
        rule.setContent {
            FrameByNavinTheme {
                V393FirstRunFocusScreen(onChoose = { chosen = it })
            }
        }
        rule.onNodeWithTag("v393-first-run-focus").assertExists()
        V393FirstRunFocus.entries.forEach { focus ->
            rule.onNodeWithTag("v393-focus-${focus.name}").assertExists()
        }
        rule.onNodeWithTag("v393-focus-PERSONAL").performClick()
        rule.runOnIdle { assertEquals(V393FirstRunFocus.PERSONAL, chosen) }
    }

    @Test fun focusSelectionCommitsAndSurvivesStoreReload() {
        HardeningTestEnvironment.requireCiEmulator()
        val store = app.getSharedPreferences("backlot_first_run_intent_v393", 0)
        // The harness clears this disposable emulator before running the class.
        check(store.all.isEmpty()) { "Refuse to mutate nonempty first-run preference state" }
        try {
            val first = V393FirstRunPrefs(app)
            assertEquals(null, first.selectedFocus())
            first.saveFocus(V393FirstRunFocus.PERSONAL)
            assertEquals(V393FirstRunFocus.PERSONAL, V393FirstRunPrefs(app).selectedFocus())
            first.markPersonalFirstEntryShown()
            assertTrue(V393FirstRunPrefs(app).personalFirstEntryShown())
        } finally {
            store.edit().clear().commit()
        }
    }

    @Test fun creatorQuickStartRefusesIncompleteProfileThenSavesExplicitChoices() {
        HardeningTestEnvironment.requireCiEmulator()
        var saved: CreatorProfile? = null
        val mode = CreatorModeRegistry.definitions.first().label
        val platform = CreatorPlatformRegistry.supportedPlatforms.first()
        val goal = "Build a portfolio"
        rule.setContent {
            FrameByNavinTheme {
                V393QuickCreatorSetup(CreatorProfile()) {
                    saved = it
                    true
                }
            }
        }
        rule.onNodeWithTag("v393-quick-creator-setup").assertExists()
        rule.onNodeWithTag("v393-creator-start").assertIsNotEnabled()
        rule.onNodeWithTag("v393-pick-1-${mode.hashCode()}")
            .performScrollTo().performClick()
        rule.onNodeWithTag("v393-pick-2-${platform.hashCode()}")
            .performScrollTo().performClick()
        rule.onNodeWithTag("v393-pick-3-${goal.hashCode()}")
            .performScrollTo().performClick()
        rule.onNodeWithTag("v393-creator-start").assertIsEnabled().performClick()
        rule.runOnIdle {
            val actual = requireNotNull(saved)
            assertTrue(actual.isComplete)
            assertEquals(mode, actual.resolvedPrimaryCreatorMode)
            assertTrue(platform in actual.platforms)
            assertEquals(goal, actual.primaryGoal)
        }
    }

    @Test fun mirrorGuideIsSelectableAndAllSixArtResourcesDecode() {
        HardeningTestEnvironment.requireCiEmulator()
        assertEquals(
            listOf(V144GuideIdentity.FUNNY, V144GuideIdentity.MIRA),
            V145SelectableGuides,
        )
        assertEquals(V144GuideIdentity.CUTE, v145NormalizeSavedGuide("CUTE"))
        assertEquals(V144GuideIdentity.NAVI, v145NormalizeSavedGuide("NAVI"))
        val artwork = listOf(
            R.drawable.guide_mira_welcome,
            R.drawable.guide_mira_point,
            R.drawable.guide_mira_present,
            R.drawable.guide_mira_listen,
            R.drawable.guide_mira_think,
            R.drawable.guide_mira_celebrate,
        )
        artwork.forEach { resource ->
            val bitmap = requireNotNull(BitmapFactory.decodeResource(app.resources, resource)) {
                "Mira resource must decode: $resource"
            }
            assertTrue("Mira art must have meaningful resolution: $resource",
                bitmap.width > 32 && bitmap.height > 32)
            bitmap.recycle()
        }
    }

    @Test fun voiceAndAlarmSceneSelectionsStayIndependent() {
        HardeningTestEnvironment.requireCiEmulator()
        V391ReminderScenePrefs.initialize(app)
        val oldVoice = V391ReminderScenePrefs.selected(ReminderCheckInKind.VOICE)
        val oldAlarm = V391ReminderScenePrefs.selected(ReminderCheckInKind.ALARM)
        try {
            rule.setContent {
                FrameByNavinTheme {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        V391ReminderSceneSettings()
                    }
                }
            }
            rule.onNodeWithTag("v391-scene-settings").assertExists()
            rule.onNodeWithTag("v391-pick-VOICE-AURORA_SILK")
                .performScrollTo().performClick()
            rule.onNodeWithTag("v391-pick-ALARM-GLASSWAVE")
                .performScrollTo().performClick()
            rule.runOnIdle {
                assertEquals(V391ReminderScene.AURORA_SILK,
                    V391ReminderScenePrefs.selected(ReminderCheckInKind.VOICE))
                assertEquals(V391ReminderScene.GLASSWAVE,
                    V391ReminderScenePrefs.selected(ReminderCheckInKind.ALARM))
            }
        } finally {
            V391ReminderScenePrefs.set(ReminderCheckInKind.VOICE, oldVoice)
            V391ReminderScenePrefs.set(ReminderCheckInKind.ALARM, oldAlarm)
        }
    }
}
