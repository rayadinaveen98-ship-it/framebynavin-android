package com.framebynavin.app.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.WeeklyScheduleSlot
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class V163BriefCalendarUiTest {
    @get:Rule val rule = createComposeRule()

    private fun project() = CreatorTask(
        id = "project-to-open",
        title = "Shoot a scene",
        platform = "YouTube",
        contentType = "Short",
        dueLabel = "",
        dueAtMillis = System.currentTimeMillis() + 3 * 3_600_000L,
    )

    @Test fun briefFocusOpensExistingProject() {
        var opened = ""
        rule.setContent {
            V15DailyBriefScreen(
                tasks = listOf(project()),
                weeklySlots = emptyList(),
                onClose = {},
                onOpenProject = { opened = it },
            )
        }
        rule.onNodeWithText("OPEN PROJECT →").assertExists().performClick()
        rule.runOnIdle { assertEquals("project-to-open", opened) }
    }

    @Test fun calendarProjectAndWeeklyPlanHaveDifferentDestinations() {
        var openedProject = ""
        var weeklyOpened = false
        val tomorrow = LocalDate.now().plusDays(1)
        val slot = WeeklyScheduleSlot(
            id = "weekly-test", title = "Next weekly planning session",
            dayOfWeek = tomorrow.dayOfWeek, hour = 18, minute = 0,
            platform = "YouTube", contentType = "Short",
        )
        rule.setContent {
            V15ContentCalendarScreen(
                tasks = listOf(project()),
                weeklySlots = listOf(slot),
                onClose = {},
                onOpenProject = { openedProject = it },
                onOpenWeeklyPlan = { weeklyOpened = true },
            )
        }
        rule.onNodeWithText("Shoot a scene").assertExists().performClick()
        rule.runOnIdle { assertEquals("project-to-open", openedProject) }
        rule.onNodeWithText("Next weekly planning session").assertExists().performClick()
        rule.runOnIdle { assertEquals(true, weeklyOpened) }
    }
}
