package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorRewardEngine
import com.framebynavin.app.data.CreatorRewardEventType
import com.framebynavin.app.data.CreatorRewardReconciliation
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ProjectPulseEngine
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderStageRewardV148Test {
    private val now = 1_800_000_000_000L

    @Test
    fun reminderStageDoneUsesCanonicalWorkflowTransition() {
        val managed = ProjectPulseEngine.applyAttentionPlan(baseTask(), ProjectAttentionPlan.GUIDED, now)
        val beforeIndex = CreatorWorkflowEngine.stageIndex(managed)

        val advanced = ReminderActionSafety.stageDone(managed, now + 1_000L)

        assertEquals(beforeIndex + 1, CreatorWorkflowEngine.stageIndex(advanced))
        assertEquals(TaskStatus.WORKING, advanced.status)
        assertEquals(
            CreatorWorkflowEngine.progressForStage(
                beforeIndex + 1,
                CreatorWorkflowEngine.templateFor(advanced).stages.size,
            ),
            advanced.progress,
        )
    }

    @Test
    fun repeatedStageEvidenceCreditsRewardExactlyOnce() {
        val managed = ProjectPulseEngine.applyAttentionPlan(baseTask(), ProjectAttentionPlan.GUIDED, now)
        val stage = CreatorWorkflowEngine.currentStage(managed)
        val advanced = ReminderActionSafety.stageDone(managed, now + 1_000L)
        val evidence = CreatorRewardEngine.stageCompleted(
            managed.id,
            stage.id,
            stage.label,
            now + 1_000L,
        )

        val first = CreatorRewardReconciliation.reconcile(
            current = emptyList(),
            tasks = listOf(advanced),
            checkpoints = emptyList(),
            stageEvidence = evidence,
        )
        val replay = CreatorRewardReconciliation.reconcile(
            current = first.entries,
            tasks = listOf(advanced),
            checkpoints = emptyList(),
            stageEvidence = evidence,
        )

        assertEquals(1, first.newlyCredited.count { it.type == CreatorRewardEventType.PROJECT_STAGE_COMPLETED })
        assertTrue(replay.newlyCredited.isEmpty())
        assertEquals(1, replay.entries.count { it.eventKey == evidence.eventKey })
    }

    private fun baseTask() = CreatorTask(
        id = "v148-reminder-reward",
        title = "V148 reminder reward",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "Later",
        dueAtMillis = now + 24 * 60 * 60_000L,
        status = TaskStatus.PLANNED,
        progress = 0,
        workflowStageIndex = 0,
    )
}
