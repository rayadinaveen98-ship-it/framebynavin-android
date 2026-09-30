package com.framebynavin.app.ui

/** Copy contract that keeps the first-run tour aligned with the real New Project support/reminder step. */
internal data class V149GuidedProjectReminderCopy(
    val title: String,
    val body: String,
    val speech: String,
)

internal fun v149GuidedProjectReminderCopy(hasProjects: Boolean): V149GuidedProjectReminderCopy =
    if (hasProjects) {
        V149GuidedProjectReminderCopy(
            title = "Open a project",
            body = "Open one to see its next step. New projects can also choose Project Support and reminders during setup.",
            speech = "A project keeps one piece of content together from idea to publish. When you create a new one, Project Support lets you choose Light, Guided, Urgent or Custom reminders so Backlot can bring the next step back at the right time.",
        )
    } else {
        V149GuidedProjectReminderCopy(
            title = "Build your first project",
            body = "Create one now. During Project Support, choose how strongly Backlot should remind you while you work.",
            speech = "When an idea becomes real work, create a project. In Project Support choose Light, Guided, Urgent or Custom reminders, then Backlot keeps the work moving from planning to publish.",
        )
    }
