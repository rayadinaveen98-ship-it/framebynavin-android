from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_required(rel: str, old: str, new: str, count: int = 1) -> None:
    text = read(rel)
    hits = text.count(old)
    if hits != count:
        raise SystemExit(f"Expected {count} occurrence(s) in {rel}, found {hits}: {old!r}")
    write(rel, text.replace(old, new, count))


# Alpha6: strengthen the actual creator-flow interaction contract and run it on-device in CI.
build = "app/build.gradle.kts"
replace_required(build, "versionCode = 45", "versionCode = 46")
replace_required(
    build,
    'versionName = "1.8.0-foundation-alpha5"',
    'versionName = "1.8.0-foundation-alpha6"',
)

test_rel = "app/src/androidTest/java/com/framebynavin/app/ui/V18CoreInteractionUiTest.kt"
if (ROOT / test_rel).exists():
    raise SystemExit(f"{test_rel} already exists; refusing to overwrite")

write(
    test_rel,
    r'''package com.framebynavin.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V18CoreInteractionUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun today_primaryActionsDispatchSelectedProject() {
        val task = projectTask("today-actions", "Today action project")
        val events = mutableListOf<String>()

        composeRule.setContent {
            PTodayScreen(
                tasks = listOf(task),
                onAdd = { events += "add" },
                onStart = { events += "start:$it" },
                onAdvance = { events += "advance:$it" },
                onViewAllReminders = { events += "reminders" },
                onFocus = { events += "focus:$it" },
            )
        }

        composeRule.onNodeWithText("START").performClick()
        composeRule.onNodeWithText("FOCUS").performClick()
        composeRule.onNodeWithText("MARK STEP DONE").performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf("start:${task.id}", "focus:${task.id}", "advance:${task.id}"),
                events,
            )
        }
    }

    @Test
    fun today_viewAllRemindersDispatches() {
        var opened = false

        composeRule.setContent {
            PTodayScreen(
                tasks = emptyList(),
                onAdd = {},
                onStart = {},
                onAdvance = {},
                onViewAllReminders = { opened = true },
                onFocus = {},
            )
        }

        composeRule.onNodeWithText("VIEW ALL").performClick()
        composeRule.runOnIdle { assertTrue(opened) }
    }

    @Test
    fun plan_createStartAndDoneDispatchCorrectProject() {
        val task = projectTask("plan-actions", "Plan action project")
        val events = mutableListOf<String>()

        composeRule.setContent {
            PPlanScreen(
                tasks = listOf(task),
                onAdd = { events += "add" },
                onStart = { events += "start:$it" },
                onDone = { events += "done:$it" },
            )
        }

        composeRule.onNodeWithContentDescription("Create project").performClick()
        composeRule.onNodeWithText("START").performClick()
        composeRule.onNodeWithText("DONE").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("add", "start:${task.id}", "done:${task.id}"), events)
        }
    }

    @Test
    fun studio_expandFocusAndAdvanceDispatchCorrectProject() {
        val task = projectTask("studio-actions", "Studio action project")
        val events = mutableListOf<String>()

        composeRule.setContent {
            PStudioScreen(
                tasks = listOf(task),
                onAdd = {},
                onAdvance = { events += "advance:$it" },
                onBack = { events += "back:$it" },
                onFocus = { events += "focus:$it" },
            )
        }

        composeRule.onNodeWithText(task.title).performClick()
        composeRule.onNodeWithText("WORK ON · IDEA").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("MARK STEP DONE").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("focus:${task.id}", "advance:${task.id}"), events)
        }
    }

    @Test
    fun insights_createProjectDispatches() {
        var addCount = 0

        composeRule.setContent {
            PInsightsScreen(
                tasks = emptyList(),
                ideas = emptyList(),
                onAdd = { addCount += 1 },
            )
        }

        composeRule.onNodeWithText("How you're creating.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Create project").performClick()
        composeRule.runOnIdle { assertEquals(1, addCount) }
    }

    @Test
    fun ideaVault_searchFieldFiltersVisibleIdeas() {
        val first = CreatorIdea(id = "idea-a", title = "Aurora lighting breakdown", topic = "Lighting")
        val second = CreatorIdea(id = "idea-b", title = "Rain sound design", topic = "Sound")

        composeRule.setContent {
            V09IdeaVaultScreen(
                ideas = listOf(first, second),
                onClose = {},
                onSave = { null },
                onDelete = {},
                onArchive = {},
                onConvert = { _, _, _, _ -> null },
            )
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("Aurora")
        composeRule.onNodeWithText(first.title).assertIsDisplayed()
        composeRule.onAllNodesWithText(second.title).assertCountEquals(0)
    }

    @Test
    fun ideaVault_newIdeaButtonOpensAndCancelsEditor() {
        composeRule.setContent {
            V09IdeaVaultScreen(
                ideas = emptyList(),
                onClose = {},
                onSave = { null },
                onDelete = {},
                onArchive = {},
                onConvert = { _, _, _, _ -> null },
            )
        }

        composeRule.onNodeWithContentDescription("New idea").performClick()
        composeRule.onNodeWithText("New Idea").assertIsDisplayed()
        composeRule.onNodeWithText("CANCEL").performClick()
        composeRule.onAllNodesWithText("New Idea").assertCountEquals(0)
    }

    @Test
    fun reminderCenter_addButtonDispatchesNewReminder() {
        var addCount = 0

        composeRule.setContent {
            V131ReminderCenter(
                tasks = emptyList(),
                onDismiss = {},
                onNew = { addCount += 1 },
                onEdit = {},
                onDeleteReminders = {},
            )
        }

        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.runOnIdle { assertEquals(1, addCount) }
    }

    private fun projectTask(id: String, title: String) = CreatorTask(
        id = id,
        title = title,
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "This week",
        status = TaskStatus.PLANNED,
        dueAtMillis = System.currentTimeMillis() + 48 * 60 * 60_000L,
        workflowStageIndex = 0,
    )
}
''',
)

print("v1.8 alpha6 interaction test pass applied")
print("Added V18CoreInteractionUiTest.kt with 8 creator-flow interaction tests")
