package com.framebynavin.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.WeeklyScheduleSlot
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class V164WeeklyPlanPreviewUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun previewAndCancelNeverCreateProjects() {
        var generated = 0
        val slot = WeeklyScheduleSlot(
            id = "creator-preview", title = "My weekly video",
            dayOfWeek = LocalDate.now().plusDays(1).dayOfWeek,
            hour = 18, minute = 0, platform = "YouTube", contentType = "Short",
            reminderMode = ReminderMode.NONE,
        )
        rule.setContent {
            V08WeeklyScheduleScreen(
                slots = listOf(slot), tasks = emptyList(),
                onClose = {}, onToggle = { _, _ -> }, onSave = {}, onDelete = {},
                onRefresh = { generated++ }, onReset = {},
                autoPlanEnabled = true,
            )
        }
        rule.onNodeWithTag("v164-create-preview").performClick()
        rule.onNodeWithText("CREATE PROJECTS").assertExists()
        rule.runOnIdle { assertEquals(0, generated) }
        rule.onNodeWithText("CANCEL").performClick()
        rule.runOnIdle { assertEquals(0, generated) }
    }

    @Test fun confirmingPreviewOnlyCallsExistingGeneratorOnce() {
        var requested = 0
        val slot = WeeklyScheduleSlot(
            id = "creator-preview2", title = "My creative schedule",
            dayOfWeek = LocalDate.now().plusDays(1).dayOfWeek,
            hour = 18, minute = 0, platform = "YouTube", contentType = "Short",
        )
        rule.setContent {
            V08WeeklyScheduleScreen(
                slots = listOf(slot), tasks = emptyList(),
                onClose = {}, onToggle = { _, _ -> }, onSave = {}, onDelete = {},
                onRefresh = { requested++ }, onReset = {},
                autoPlanEnabled = true,
            )
        }
        rule.onNodeWithTag("v164-create-preview").performClick()
        rule.runOnIdle { assertEquals(0, requested) }
        rule.onNodeWithText("CREATE PROJECTS").performClick()
        rule.runOnIdle { assertEquals(1, requested) }
    }
}
