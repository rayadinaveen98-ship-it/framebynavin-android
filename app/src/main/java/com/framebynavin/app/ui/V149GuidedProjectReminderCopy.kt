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
            body = "Open a project to see its next step. You can add reminders when you create it.",
            speech = "Keep your content in one project, from idea to publishing. Choose reminders only if you need them.",
        )
    } else {
        V149GuidedProjectReminderCopy(
            title = "Build your first project",
            body = "Start a project and choose reminders if you'd like help staying on track.",
            speech = "Turn an idea into a project and keep the steps together. You can add reminders if you'd like.",
        )
    }
