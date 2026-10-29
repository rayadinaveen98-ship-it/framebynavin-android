package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.IdeaStatus
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus
import java.util.Locale

/**
 * Local, explainable prompts only. This is NOT a generative AI model.
 * No persistence, network, speech, scheduled notifications, project mutation,
 * or fabricated delivery/analytics status.
 */
internal enum class V2292SparkDestination { NONE, PROJECT, IDEAS, REMINDERS }

internal data class V2292SparkReply(
    val message: String,
    val destination: V2292SparkDestination = V2292SparkDestination.NONE,
    val targetProjectId: String = "",
    val actionLabel: String = "",
)

internal object V2292SparkConversationPolicy {
    const val INPUT_MAX = 240

    fun respond(
        message: String,
        tasks: List<CreatorTask>,
        ideas: List<CreatorIdea>,
    ): V2292SparkReply {
        val q = message.trim().take(INPUT_MAX).lowercase(Locale.ROOT)
        if (q.isBlank()) return V2292SparkReply("Ask about a next step, your ideas, or reminders.")
        val words = Regex("[a-z0-9]+").findAll(q).map { it.value }.toSet()
        if (words.any { it in setOf("idea", "ideas", "vault", "inspiration", "brainstorm") }) {
            val active = ideas.filter {
                !it.isTrashed && it.projectTaskId.isBlank() &&
                    it.status != IdeaStatus.ARCHIVED && it.status != IdeaStatus.CONVERTED
            }
            return if (active.isEmpty()) V2292SparkReply(
                "I can't see any unlinked active ideas in your Idea Vault. You can open it to capture one.",
                V2292SparkDestination.IDEAS, actionLabel = "OPEN IDEA VAULT",
            ) else V2292SparkReply(
                "Your Idea Vault has ${active.size} unlinked active idea${if (active.size == 1) "" else "s"}. Pick one to develop; I won't change its status for you.",
                V2292SparkDestination.IDEAS, actionLabel = "OPEN IDEA VAULT",
            )
        }
        if (words.any { it in setOf("reminder", "reminders", "alarm", "alarms", "notification", "notifications") }) {
            return V2292SparkReply(
                "I can open your Reminders hub. This offline guide can't verify whether Android showed or delivered an alert.",
                V2292SparkDestination.REMINDERS, actionLabel = "OPEN REMINDERS",
            )
        }
        if (words.any { it in setOf("next", "focus", "project", "projects", "step", "start", "continue", "work", "today", "priority") }) ||
            q.contains("what should")
        ) {
            // No archived, completed or skipped work. Due dates and priority
            // decide order only; they never certify actual delivery or work.
            val candidate = tasks.asSequence().filter {
                it.id.isNotBlank() && it.archivedAtMillis <= 0L &&
                    (it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING)
            }.sortedWith(
                compareBy<CreatorTask> {
                    it.dueAtMillis.takeIf { due -> due > 0L } ?: Long.MAX_VALUE
                }.thenByDescending { when (it.priority) {
                    TaskPriority.CRITICAL -> 3
                    TaskPriority.IMPORTANT -> 2
                    TaskPriority.NORMAL -> 1
                } }.thenBy { it.id }
            ).firstOrNull()
            return if (candidate == null) V2292SparkReply(
                "No active, unarchived project is available in Backlot right now. Your Idea Vault is a place to start.",
                V2292SparkDestination.IDEAS, actionLabel = "OPEN IDEA VAULT",
            ) else {
                val stage = CreatorWorkflowEngine.currentStage(candidate)
                val action = CreatorWorkflowEngine.nextAction(candidate)
                V2292SparkReply(
                    "For \"${candidate.title.trim().ifBlank { "your project" }}\", your recorded step is ${stage.label}. ${action}. That's a workflow suggestion, not proof the step is done.",
                    V2292SparkDestination.PROJECT,
                    targetProjectId = candidate.id,
                    actionLabel = "OPEN PROJECT",
                )
            }
        }
        return V2292SparkReply(
            "I'm Spark's first offline guide, not a full AI chat model. Try \"What should I do next?\", \"My ideas\" or \"Reminders\".",
        )
    }
}
