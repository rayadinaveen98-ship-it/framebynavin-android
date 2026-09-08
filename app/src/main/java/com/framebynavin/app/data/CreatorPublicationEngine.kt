package com.framebynavin.app.data

/** Publication is an observed creator action, not an inference from project completion. */
object CreatorPublicationEngine {
    fun record(task: CreatorTask, atMillis: Long, url: String = ""): CreatorTask {
        require(atMillis > 0L) { "Publication time must be positive" }
        require(atMillis <= System.currentTimeMillis() + 60_000L) { "Publication cannot be in the future" }
        val cleanUrl = url.trim()
        require(cleanUrl.isEmpty() || cleanUrl.startsWith("https://", ignoreCase = true)) {
            "A live link must use HTTPS"
        }
        if (task.publishedAtMillis > 0L) return task
        return task.copy(
            publishedAtMillis = atMillis,
            publishedUrl = cleanUrl,
            publicationIsLegacy = false,
        )
    }

    /** Explicit correction, including removing an incorrect self-reported publication. */
    fun correct(task: CreatorTask, atMillis: Long, url: String = ""): CreatorTask {
        require(atMillis >= 0L)
        require(atMillis <= System.currentTimeMillis() + 60_000L) { "Publication cannot be in the future" }
        val cleanUrl = url.trim()
        require(cleanUrl.isEmpty() || cleanUrl.startsWith("https://", ignoreCase = true))
        return task.copy(
            publishedAtMillis = atMillis,
            publishedUrl = if (atMillis > 0L) cleanUrl else "",
            publicationIsLegacy = false,
        )
    }

    fun finish(task: CreatorTask, now: Long): CreatorTask {
        val template = CreatorWorkflowEngine.templateFor(task)
        return task.copy(
            status = TaskStatus.DONE,
            progress = 100,
            workflowStageIndex = template.stages.lastIndex,
            reminderEnabled = false,
            smartEscalationEnabled = false,
            voiceEnabled = false,
            reminderMode = ReminderMode.NONE,
            workingUntilMillis = 0L,
            pulseManagedReminder = false,
            checkpointStageId = "",
            checkpointAtMillis = 0L,
            autoStageReminder = false,
            completedAtMillis = task.completedAtMillis.takeIf { it > 0L } ?: now,
        )
    }

    fun advance(task: CreatorTask, now: Long): CreatorTask {
        if (task.status == TaskStatus.DONE) return task
        val template = CreatorWorkflowEngine.templateFor(task)
        val index = CreatorWorkflowEngine.stageIndex(task)
        val stage = template.stages[index]
        val published = if (CreatorWorkflowEngine.isPublicationStage(stage)) record(task, now) else task
        if (index >= template.stages.lastIndex) return finish(published, now)
        return published.copy(
            status = TaskStatus.WORKING,
            workflowStageIndex = index + 1,
            progress = CreatorWorkflowEngine.progressForStage(index + 1, template.stages.size),
            workingUntilMillis = 0L,
        )
    }

    fun publicationReward(before: CreatorTask, after: CreatorTask): CreatorRewardLedgerEntry? =
        if (before.publishedAtMillis <= 0L && after.publishedAtMillis > 0L)
            CreatorRewardEngine.projectPublished(after.id, after.title, after.publishedAtMillis)
        else null
}
