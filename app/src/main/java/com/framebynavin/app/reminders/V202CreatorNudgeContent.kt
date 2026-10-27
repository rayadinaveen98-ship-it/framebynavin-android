package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorContextNudge

/**
 * V202: concise creator-specific notification language, based only on a real
 * active project's already-computed workflow and deadline state. No invented
 * milestones, statistics, publish promises, or engagement analytics.
 */
internal object V202CreatorNudgeContent {
    internal data class Message(val title: String, val body: String)

    fun forNudge(nudge: CreatorContextNudge): Message {
        val project = nudge.title.trim().ifBlank { "Your project" }.let { title ->
            if (title.length <= 54) title else title.take(53).trimEnd() + "…"
        }
        val step = nudge.action.trim().ifBlank { "Open the project to continue." }
        val prefix = nudge.key.substringBefore(':')
        return when (prefix) {
            "overdue" -> Message(
                title = "Backlot · Pick up where you left off",
                body = "$project passed its planned publish time. $step",
            )
            "missed-reminder" -> Message(
                title = "Backlot · A project still needs attention",
                body = "Your check-in for $project passed while it's still active. $step",
            )
            "risk-6h" -> Message(
                title = "Backlot · Final stretch for $project",
                body = "${nudge.message} $step",
            )
            "risk-24h" -> Message(
                title = "Backlot · Today matters for $project",
                body = "${nudge.message} $step",
            )
            "critical-48h" -> Message(
                title = "Backlot · Priority check-in",
                body = "$project · ${nudge.message} $step",
            )
            else -> Message(
                title = "Backlot · $project",
                body = "${nudge.message.trim()} $step".trim(),
            )
        }
    }
}
