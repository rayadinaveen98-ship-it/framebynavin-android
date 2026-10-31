package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.personal.PersonalPurpose
import com.framebynavin.app.personal.PersonalReminderDraft
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pure composable flow with callback; no user DataStore mutations. */
@RunWith(AndroidJUnit4::class)
class V274SelfCareEditorUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun selectingSelfCarePurposeSuggestsEditableTitleAndSavesPurpose() {
        var actual: PersonalReminderDraft? = null
        rule.setContent {
            FrameByNavinTheme {
                V2232PersonalEditor(existing=null, saving=false,
                    onDismiss={}, onSave={actual=it})
            }
        }
        rule.onNodeWithTag("v274-personal-purpose-picker").assertExists()
        rule.onNodeWithTag("v274-purpose-hydration").performClick()
        rule.onNodeWithTag("v2232-personal-title").assertTextContains("Have some water")
        rule.onNodeWithTag("v274-personal-review").assertExists()
        rule.onNodeWithTag("v2232-personal-save").performClick()
        rule.runOnIdle {
            assertEquals(PersonalPurpose.HYDRATION, actual?.purpose)
            assertEquals("Have some water", actual?.title)
        }
    }
}
