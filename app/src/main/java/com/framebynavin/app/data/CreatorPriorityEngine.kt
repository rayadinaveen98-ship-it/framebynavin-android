package com.framebynavin.app.data

private const val HOUR_MS = 60L * 60_000L
private const val DAY_MS = 24L * HOUR_MS

data class CreatorRecommendation(
    val taskId: String,
    val action: String,
    val urgencyLabel: String,
    val reason: String,
    val score: Int,
    /** Deterministic rough effort for the current workflow stage; never presented as measured actual time. */
    val estimatedMinutes: Int = 30,
    /** Small evidence set explaining why this action is being recommended. */
    val signals: List<String> = emptyList(),
)

object CreatorPriorityEngine {
    fun rankActive(tasks: List<CreatorTask>, now: Long = System.currentTimeMillis()): List<CreatorTask> =
        tasks.asSequence()
            .filter { it.archivedAtMillis <= 0L }
            .filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
            .sortedWith(
                compareByDescending<CreatorTask> { score(it, now) }
                    .thenBy { it.dueAtMillis.takeIf { due -> due > 0L } ?: Long.MAX_VALUE }
                    .thenBy { it.title.lowercase() }
            )
            .toList()

    fun recommendation(task: CreatorTask, now: Long = System.currentTimeMillis()): CreatorRecommendation {
        val dueDelta = task.dueAtMillis.takeIf { it > 0L }?.minus(now)
        val urgency = when {
            dueDelta != null && dueDelta < 0L -> "OVERDUE"
            dueDelta != null && dueDelta <= 2 * HOUR_MS -> "NOW"
            dueDelta != null && dueDelta <= DAY_MS -> "TODAY"
            task.priority == TaskPriority.CRITICAL -> "HIGH"
            task.status == TaskStatus.WORKING -> "CONTINUE"
            else -> "NEXT"
        }
        val reason = when {
            dueDelta != null && dueDelta < 0L -> "Deadline passed — finish the current step before lower-priority work."
            dueDelta != null && dueDelta <= 2 * HOUR_MS -> "Publishing is close, so this project has the least schedule buffer."
            task.status == TaskStatus.WORKING -> "You already started this project — keeping momentum reduces context switching."
            task.priority == TaskPriority.CRITICAL -> "Marked Critical, so it outranks Important and Normal work."
            dueDelta != null && dueDelta <= DAY_MS -> "Due today — completing the current step protects the publish window."
            task.priority == TaskPriority.IMPORTANT -> "Important project with the strongest current deadline and progress signal."
            CreatorWorkflowEngine.progress(task) >= 75 -> "Close to publish — finishing it now clears active work faster."
            else -> "Best next step from deadline, priority and current progress."
        }
        return CreatorRecommendation(
            taskId = task.id,
            action = CreatorWorkflowEngine.nextAction(task),
            urgencyLabel = urgency,
            reason = reason,
            score = score(task, now),
            estimatedMinutes = estimateStageMinutes(task),
            signals = evidence(task, now).take(3),
        )
    }

    /**
     * Rough, deterministic stage estimate used for planning guidance. It is deliberately conservative
     * and does not pretend to be measured creator history. Alpha 3 can later fit work to available time.
     */
    fun estimateStageMinutes(task: CreatorTask): Int {
        val stage = CreatorWorkflowEngine.currentStage(task).id
        val longWork = task.contentType.trim().lowercase() in setOf("long-form", "video", "episode", "article", "newsletter")
        return when (stage) {
            "idea", "angle", "select" -> 20
            "research" -> if (longWork) 50 else 30
            "outline" -> 30
            "script", "draft" -> if (longWork) 50 else 25
            "voice", "record" -> if (longWork) 35 else 20
            "edit" -> if (longWork) 60 else 35
            "sound_grade" -> 40
            "thumbnail", "create" -> 30
            "caption", "copy", "metadata" -> 20
            "review", "proof", "verify" -> 15
            "upload", "send", "published" -> 15
            "promote", "engage" -> 20
            else -> 30
        }
    }

    fun score(task: CreatorTask, now: Long = System.currentTimeMillis()): Int {
        if (task.archivedAtMillis > 0L || (task.status != TaskStatus.PLANNED && task.status != TaskStatus.WORKING)) {
            return Int.MIN_VALUE
        }

        var score = 0
        if (task.status == TaskStatus.WORKING) score += 260
        score += when (task.priority) {
            TaskPriority.NORMAL -> 0
            TaskPriority.IMPORTANT -> 90
            TaskPriority.CRITICAL -> 180
        }

        val due = task.dueAtMillis
        if (due > 0L) {
            val delta = due - now
            score += when {
                delta < 0L -> 500 + ((-delta / HOUR_MS).coerceAtMost(30L).toInt() * 8)
                delta <= 2 * HOUR_MS -> 360
                delta <= 6 * HOUR_MS -> 280
                delta <= DAY_MS -> 200
                delta <= 2 * DAY_MS -> 120
                delta <= 7 * DAY_MS -> 60
                else -> 20
            }
        }

        val progress = CreatorWorkflowEngine.progress(task)
        score += (progress / 5).coerceAtMost(20)
        if (progress >= 75) score += 40

        if (task.reminderEnabled && task.reminderAtMillis > 0L) {
            val reminderDelta = task.reminderAtMillis - now
            if (reminderDelta <= 2 * HOUR_MS) score += 35
        }
        return score
    }

    private fun evidence(task: CreatorTask, now: Long): List<String> = buildList {
        val due = task.dueAtMillis
        if (due > 0L) {
            val delta = due - now
            add(
                when {
                    delta < 0L -> "Deadline is overdue"
                    delta <= 2 * HOUR_MS -> "Deadline is within 2 hours"
                    delta <= DAY_MS -> "Due today"
                    delta <= 2 * DAY_MS -> "Due within 2 days"
                    else -> "Deadline still has buffer"
                }
            )
        }
        if (task.status == TaskStatus.WORKING) add("Already in progress")
        if (task.priority == TaskPriority.CRITICAL) add("Critical priority")
        else if (task.priority == TaskPriority.IMPORTANT) add("Important priority")

        val progress = CreatorWorkflowEngine.progress(task)
        add("${CreatorWorkflowEngine.currentStage(task).label} · $progress% workflow")

        val unfinished = task.workspace.checklist.count { it.status == CreatorChecklistStatus.TODO }
        val requiredPublishChecks = task.workspace.deliverables.sumOf { deliverable ->
            deliverable.publishGate.count { it.required && it.status == CreatorPublishGateStatus.TODO }
        }
        val remaining = unfinished + requiredPublishChecks
        if (remaining > 0) add("$remaining unfinished check${if (remaining == 1) "" else "s"}")

        if (task.reminderEnabled && task.reminderAtMillis in 1..(now + 2 * HOUR_MS)) add("Reminder is due soon")
    }
}
