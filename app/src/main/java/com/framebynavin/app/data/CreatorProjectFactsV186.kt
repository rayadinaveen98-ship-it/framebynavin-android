package com.framebynavin.app.data

/** Read-only projection. Workflow, checklist and publication have separate authorities. */
data class CreatorProjectFactsV186(
    val workflowCompletedStages: Int,
    val workflowTotalStages: Int,
    val workflowLabel: String,
    val checklistLabel: String,
    val publicationLabel: String,
    val projectStatusLabel: String,
) {
    val workflowFraction: Float get() =
        (workflowCompletedStages.toFloat() / workflowTotalStages.coerceAtLeast(1)).coerceIn(0f, 1f)
}

object CreatorProjectFactsV186Policy {
    fun facts(task: CreatorTask): CreatorProjectFactsV186 {
        val total = CreatorWorkflowEngine.templateFor(task).stages.size.coerceAtLeast(1)
        val completed = CreatorWorkflowEngine.completedStageCount(task).coerceIn(0, total)
        val checklist = task.workspace.checklist
        val checked = checklist.count { it.status == CreatorChecklistStatus.DONE }
        val skipped = checklist.count { it.status == CreatorChecklistStatus.SKIPPED }
        val checklistLabel = if (checklist.isEmpty()) "No checklist" else buildString {
            append("Checklist $checked/${checklist.size} done")
            if (skipped > 0) append(" · $skipped skipped")
        }
        val outputs = task.workspace.deliverables
        val publishedOutputs = outputs.count {
            it.status == CreatorDeliverableStatus.PUBLISHED && it.publishedAtMillis > 0L
        }
        val publicationLabel = when {
            task.publishedAtMillis > 0L -> "Project publication recorded"
            publishedOutputs > 0 -> "$publishedOutputs/${outputs.size} outputs published"
            else -> "Not published"
        }
        val status = when {
            task.archivedAtMillis > 0L -> "Archived"
            task.status == TaskStatus.DONE -> "Finished"
            task.status == TaskStatus.SKIPPED -> "Skipped"
            task.status == TaskStatus.WORKING -> "In progress"
            else -> "Planned"
        }
        val workflow = when (task.status) {
            TaskStatus.DONE -> "Workflow finished ($total/$total stages)"
            TaskStatus.SKIPPED -> "Workflow skipped ($completed/$total stages)"
            TaskStatus.PLANNED, TaskStatus.WORKING -> "Workflow $completed/$total stages"
        }
        return CreatorProjectFactsV186(completed, total, workflow, checklistLabel, publicationLabel, status)
    }
}
