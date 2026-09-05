from pathlib import Path

ROOT = Path('.')
BUILD = ROOT / 'app/build.gradle.kts'
ROOT_UI = ROOT / 'app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt'
IDEAS = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt'
POLISH = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt'
IDEA_MODEL = ROOT / 'app/src/main/java/com/framebynavin/app/data/IdeaVault.kt'
JOURNEY = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V18CreatorJourney.kt'
UNIT_TEST = ROOT / 'app/src/test/java/com/framebynavin/app/ui/V18CreatorJourneyAlpha12Test.kt'
UI_TEST = ROOT / 'app/src/androidTest/java/com/framebynavin/app/ui/V18CreatorJourneyAlpha12UiTest.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


build = BUILD.read_text()
build = replace_once(build, 'versionCode = 51', 'versionCode = 52', 'versionCode')
build = replace_once(
    build,
    'versionName = "1.8.0-product-alpha11"',
    'versionName = "1.8.0-product-alpha12"',
    'versionName',
)
BUILD.write_text(build)

JOURNEY.parent.mkdir(parents=True, exist_ok=True)
JOURNEY.write_text(r'''package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine

internal enum class V18JourneyDestination { IDEAS, CREATE, INSIGHTS }

/**
 * Small deterministic routing policy for the v1.8 creator journey.
 * Keeps post-action navigation intentional without coupling the data layer to Compose tabs.
 */
internal object V18CreatorJourney {
    fun afterCapture(): V18JourneyDestination = V18JourneyDestination.IDEAS

    fun afterProjectCreated(projectId: String?): V18JourneyDestination? =
        projectId?.takeIf { it.isNotBlank() }?.let { V18JourneyDestination.CREATE }

    fun afterWorkflowAdvance(taskBeforeAdvance: CreatorTask): V18JourneyDestination? {
        val template = CreatorWorkflowEngine.templateFor(taskBeforeAdvance)
        val stage = CreatorWorkflowEngine.stageIndex(taskBeforeAdvance)
        return if (stage >= template.stages.lastIndex) V18JourneyDestination.INSIGHTS else null
    }
}
''')

UNIT_TEST.parent.mkdir(parents=True, exist_ok=True)
UNIT_TEST.write_text(r'''package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.IdeaCategory
import com.framebynavin.app.data.IdeaVaultLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class V18CreatorJourneyAlpha12Test {
    @Test
    fun captureContinuesIntoIdeas() {
        assertEquals(V18JourneyDestination.IDEAS, V18CreatorJourney.afterCapture())
    }

    @Test
    fun createdProjectContinuesIntoCreate() {
        assertEquals(V18JourneyDestination.CREATE, V18CreatorJourney.afterProjectCreated("project-1"))
        assertNull(V18CreatorJourney.afterProjectCreated(null))
        assertNull(V18CreatorJourney.afterProjectCreated(""))
    }

    @Test
    fun onlyPublishingStepContinuesIntoInsights() {
        val base = task(stage = 0)
        assertNull(V18CreatorJourney.afterWorkflowAdvance(base))

        val finalIndex = CreatorWorkflowEngine.templateFor(base).stages.lastIndex
        val finalTask = base.copy(workflowStageIndex = finalIndex)
        assertEquals(V18JourneyDestination.INSIGHTS, V18CreatorJourney.afterWorkflowAdvance(finalTask))
    }

    @Test
    fun ideaTaxonomyIsCreatorNeutralAtTheUiBoundary() {
        val labels = IdeaCategory.entries.map(IdeaVaultLabels::category)
        val joined = labels.joinToString(" ").lowercase()
        assertFalse(joined.contains("cinematic"))
        assertFalse(joined.contains("frame"))
        assertFalse(joined.contains("scene"))
        assertEquals("Deep Dive", IdeaVaultLabels.category(IdeaCategory.CINEMATIC_ANALYSIS))
    }

    private fun task(stage: Int) = CreatorTask(
        id = "task-1",
        title = "Test project",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "This week",
        workflowStageIndex = stage,
    )
}
''')

UI_TEST.parent.mkdir(parents=True, exist_ok=True)
UI_TEST.write_text(r'''package com.framebynavin.app.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.IdeaStatus
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V18CreatorJourneyAlpha12UiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ideaConversionUsesCreateLanguageAndCompletesCallback() {
        var converted = false
        composeRule.setContent {
            V09IdeaVaultScreen(
                ideas = listOf(
                    CreatorIdea(
                        id = "idea-1",
                        title = "A creator idea",
                        status = IdeaStatus.READY_TO_PRODUCE,
                        platformHint = "YouTube",
                        formatHint = "Long-form",
                    ),
                ),
                onClose = null,
                onSave = { null },
                onDelete = {},
                onArchive = {},
                onConvert = { _, _, _, _ ->
                    converted = true
                    "project-1"
                },
            )
        }

        composeRule.onNodeWithText("TURN INTO PROJECT").performClick()
        composeRule.onNodeWithText("CREATE PROJECT").assertIsDisplayed()
        composeRule.onNodeWithText("The project will open in Create with the right workflow and reminder timing for its format.").assertIsDisplayed()
        composeRule.onNodeWithText("The project will enter Studio with the correct workflow and an automatic reminder mode for its format.").assertDoesNotExist()
        composeRule.onNodeWithText("CREATE PROJECT").performClick()
        composeRule.runOnIdle { assertTrue(converted) }
    }
}
''')

idea_model = IDEA_MODEL.read_text()
idea_model = replace_once(idea_model, 'IdeaCategory.CINEMATIC_ANALYSIS -> "Cinematic Analysis"', 'IdeaCategory.CINEMATIC_ANALYSIS -> "Deep Dive"', 'generic idea label deep dive')
idea_model = replace_once(idea_model, 'IdeaCategory.EVERY_CINEMATIC_MOMENT -> "Every Cinematic Moment"', 'IdeaCategory.EVERY_CINEMATIC_MOMENT -> "Compilation / List"', 'generic idea label compilation')
idea_model = replace_once(idea_model, 'IdeaCategory.FRAME_OF_TODAY -> "#TheFrameOfToday"', 'IdeaCategory.FRAME_OF_TODAY -> "Daily Series"', 'generic idea label daily')
idea_model = replace_once(idea_model, 'IdeaCategory.FRAME_BREAKDOWN -> "Frame Breakdown"', 'IdeaCategory.FRAME_BREAKDOWN -> "Breakdown"', 'generic idea label breakdown')
idea_model = replace_once(idea_model, 'IdeaCategory.WHY_THIS_SCENE_WORKS -> "Why This Scene Works"', 'IdeaCategory.WHY_THIS_SCENE_WORKS -> "Explainer"', 'generic idea label explainer')
idea_model = replace_once(idea_model, 'IdeaCategory.RELEASE_REACTION -> "Release Reaction"', 'IdeaCategory.RELEASE_REACTION -> "Reaction / Update"', 'generic idea label reaction')
IDEA_MODEL.write_text(idea_model)

ideas = IDEAS.read_text()
ideas = replace_once(
    ideas,
    'Text("The project will enter Studio with the correct workflow and an automatic reminder mode for its format.", color = MutedText, fontSize = 9.3.sp)',
    'Text("The project will open in Create with the right workflow and reminder timing for its format.", color = MutedText, fontSize = 9.3.sp)',
    'idea conversion helper copy',
)
ideas = replace_once(ideas, 'Text("CREATE TURN INTO PROJECT")', 'Text("CREATE PROJECT")', 'idea conversion button copy')
ideas = replace_once(ideas, '"YouTube" -> listOf("Long-form", "Short", "Cinematic Moment")', '"YouTube" -> listOf("Long-form", "Short", "Video")', 'generic YouTube formats')
IDEAS.write_text(ideas)

polish = POLISH.read_text()
polish = replace_once(
    polish,
    'This removes the selected project data from Plan and Studio. Weekly generated occurrences are suppressed so they do not immediately come back.',
    'This removes the selected project data from your creator workspace. Weekly generated occurrences are suppressed so they do not immediately come back.',
    'project delete copy',
)
polish = replace_once(
    polish,
    'Projects can still live in Today, Plan and Studio without alerts.',
    'Projects can still live in Today and Create without alerts.',
    'reminder empty copy',
)
polish = replace_once(
    polish,
    'Only the reminder settings and scheduled alerts will be removed. Your projects remain in Plan and Studio.',
    'Only the reminder settings and scheduled alerts will be removed. Your projects remain in Today and Create.',
    'reminder delete copy',
)
POLISH.write_text(polish)

root = ROOT_UI.read_text()
root = replace_once(
    root,
    '''    fun openComposer(id: String? = null) {
        editTaskId = id
        showComposer = true
    }
''',
    '''    fun openComposer(id: String? = null) {
        editTaskId = id
        showComposer = true
    }
    fun routeJourney(destination: V18JourneyDestination?) {
        when (destination) {
            V18JourneyDestination.IDEAS -> tab = PTab.IDEAS
            V18JourneyDestination.CREATE -> tab = PTab.CREATE
            V18JourneyDestination.INSIGHTS -> tab = PTab.INSIGHTS
            null -> Unit
        }
        if (destination != null) overlay = POverlay.NONE
    }
    fun advanceWorkflowWithJourney(id: String) {
        val destination = vm.tasks.firstOrNull { it.id == id }?.let(V18CreatorJourney::afterWorkflowAdvance)
        vm.advanceWorkflow(id)
        routeJourney(destination)
    }
''',
    'journey routing helpers',
)
root = replace_once(
    root,
    '''                    onStart = vm::startTask,
                    onAdvance = vm::advanceWorkflow,
                    onViewAllReminders = { showReminders = true },''',
    '''                    onStart = vm::startTask,
                    onAdvance = ::advanceWorkflowWithJourney,
                    onViewAllReminders = { showReminders = true },''',
    'Today journey advance',
)
root = replace_once(
    root,
    '                    onConvert = vm::convertIdeaToProject,',
    '''                    onConvert = { ideaId, platform, format, dueAtMillis ->
                        val projectId = vm.convertIdeaToProject(ideaId, platform, format, dueAtMillis)
                        if (V18CreatorJourney.afterProjectCreated(projectId) == V18JourneyDestination.CREATE) {
                            externalStudioId = projectId
                            externalStudioNonce += 1L
                        }
                        routeJourney(V18CreatorJourney.afterProjectCreated(projectId))
                        projectId
                    },''',
    'Idea to Create journey',
)
root = replace_once(
    root,
    '''                    onAdd = { openComposer() },
                    onAdvance = vm::advanceWorkflow,
                    onBack = vm::moveWorkflowBack,''',
    '''                    onAdd = { openComposer() },
                    onAdvance = ::advanceWorkflowWithJourney,
                    onBack = vm::moveWorkflowBack,''',
    'Create journey advance',
)
root = replace_once(
    root,
    '''                onStageDone = {
                    vm.advanceWorkflow(focusTask.id)
                    focusTaskId = null
                },''',
    '''                onStageDone = {
                    advanceWorkflowWithJourney(focusTask.id)
                    focusTaskId = null
                },''',
    'Focus journey advance',
)
root = replace_once(
    root,
    '''            onSave = { idea ->
                vm.saveIdea(idea)
                showQuickCapture = false
            },''',
    '''            onSave = { idea ->
                vm.saveIdea(idea)
                showQuickCapture = false
                routeJourney(V18CreatorJourney.afterCapture())
            },''',
    'Capture to Ideas journey',
)
root = replace_once(
    root,
    '''                vm.saveTaskConfiguration(
                    id = task?.id,''',
    '''                val savedTaskId = vm.saveTaskConfiguration(
                    id = task?.id,''',
    'capture project save id',
)
root = replace_once(
    root,
    '''                showComposer = false
                if (task == null) tab = PTab.TODAY
            },''',
    '''                showComposer = false
                if (task == null && V18CreatorJourney.afterProjectCreated(savedTaskId) == V18JourneyDestination.CREATE) {
                    externalStudioId = savedTaskId
                    externalStudioNonce += 1L
                    routeJourney(V18JourneyDestination.CREATE)
                }
            },''',
    'Project save to Create journey',
)
ROOT_UI.write_text(root)
