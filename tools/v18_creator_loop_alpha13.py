from pathlib import Path

ROOT = Path('.')
BUILD = ROOT / 'app/build.gradle.kts'
ROOT_UI = ROOT / 'app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt'
LOOP = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V18CreatorLoop.kt'
UNIT_TEST = ROOT / 'app/src/test/java/com/framebynavin/app/ui/V18CreatorLoopAlpha13Test.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


build = BUILD.read_text()
build = replace_once(build, 'versionCode = 52', 'versionCode = 53', 'versionCode')
build = replace_once(
    build,
    'versionName = "1.8.0-product-alpha12"',
    'versionName = "1.8.0-product-alpha13"',
    'versionName',
)
BUILD.write_text(build)

LOOP.parent.mkdir(parents=True, exist_ok=True)
LOOP.write_text(r'''package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine

internal enum class V18CreatorLoopAction {
    CAPTURE_NEXT_IDEA,
    REVIEW_INSIGHTS,
    START_NEXT_PROJECT,
}

/**
 * Alpha 13 closes the creator loop after publishing instead of ending the journey.
 * This policy stays deterministic/offline-first and can later be upgraded with richer signals.
 */
internal object V18CreatorLoop {
    fun afterPublished(task: CreatorTask, hasIdeas: Boolean): V18CreatorLoopAction {
        val template = CreatorWorkflowEngine.templateFor(task)
        val stage = CreatorWorkflowEngine.stageIndex(task)
        require(stage >= template.stages.lastIndex) {
            "Creator loop should only run when a project reaches its final workflow stage"
        }
        return if (hasIdeas) V18CreatorLoopAction.REVIEW_INSIGHTS else V18CreatorLoopAction.CAPTURE_NEXT_IDEA
    }

    fun afterInsightsReviewed(hasReadyIdea: Boolean): V18CreatorLoopAction =
        if (hasReadyIdea) V18CreatorLoopAction.START_NEXT_PROJECT else V18CreatorLoopAction.CAPTURE_NEXT_IDEA
}
''')

UNIT_TEST.parent.mkdir(parents=True, exist_ok=True)
UNIT_TEST.write_text(r'''package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class V18CreatorLoopAlpha13Test {
    @Test
    fun publishedProjectWithIdeasContinuesToInsights() {
        val task = finalTask()
        assertEquals(
            V18CreatorLoopAction.REVIEW_INSIGHTS,
            V18CreatorLoop.afterPublished(task, hasIdeas = true),
        )
    }

    @Test
    fun publishedProjectWithoutIdeasEncouragesCapture() {
        val task = finalTask()
        assertEquals(
            V18CreatorLoopAction.CAPTURE_NEXT_IDEA,
            V18CreatorLoop.afterPublished(task, hasIdeas = false),
        )
    }

    @Test
    fun insightsReviewCanContinueIntoNextProject() {
        assertEquals(
            V18CreatorLoopAction.START_NEXT_PROJECT,
            V18CreatorLoop.afterInsightsReviewed(hasReadyIdea = true),
        )
        assertEquals(
            V18CreatorLoopAction.CAPTURE_NEXT_IDEA,
            V18CreatorLoop.afterInsightsReviewed(hasReadyIdea = false),
        )
    }

    private fun finalTask(): CreatorTask {
        val base = CreatorTask(
            id = "task-1",
            title = "Published project",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "This week",
        )
        val finalIndex = CreatorWorkflowEngine.templateFor(base).stages.lastIndex
        return base.copy(workflowStageIndex = finalIndex)
    }
}
''')

root = ROOT_UI.read_text()
root = replace_once(
    root,
    '''    fun advanceWorkflowWithJourney(id: String) {
        val destination = vm.tasks.firstOrNull { it.id == id }?.let(V18CreatorJourney::afterWorkflowAdvance)
        vm.advanceWorkflow(id)
        routeJourney(destination)
    }
''',
    '''    fun advanceWorkflowWithJourney(id: String) {
        val taskBeforeAdvance = vm.tasks.firstOrNull { it.id == id }
        val destination = taskBeforeAdvance?.let(V18CreatorJourney::afterWorkflowAdvance)
        val loopAction = taskBeforeAdvance?.let { task ->
            if (destination == V18JourneyDestination.INSIGHTS) {
                V18CreatorLoop.afterPublished(task, hasIdeas = vm.ideas.isNotEmpty())
            } else null
        }
        vm.advanceWorkflow(id)
        routeJourney(destination)
        when (loopAction) {
            V18CreatorLoopAction.CAPTURE_NEXT_IDEA -> showQuickCapture = true
            V18CreatorLoopAction.REVIEW_INSIGHTS,
            V18CreatorLoopAction.START_NEXT_PROJECT,
            null -> Unit
        }
    }
''',
    'published creator loop',
)
ROOT_UI.write_text(root)
