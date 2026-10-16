package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus

/** Pure eligibility/privacy rules for user-opted-in Android Google Calendar export. */
internal object CreatorCalendarSyncS1Policy {
    const val TITLE_PREFIX = "Creator project · "

    fun eligible(task: CreatorTask): Boolean =
        task.dueAtMillis > 0L && task.title.isNotBlank() &&
            task.status != TaskStatus.SKIPPED && task.archivedAtMillis <= 0L

    fun exportable(tasks: List<CreatorTask>): List<CreatorTask> =
        tasks.filter(::eligible)

    /** Personal project notes and reminders must never be copied to an external calendar. */
    fun description(task: CreatorTask): String =
        listOf(task.platform, task.contentType).filter(String::isNotBlank).joinToString(" · ")

    fun title(task: CreatorTask): String = TITLE_PREFIX + task.title.trim()

    fun ownedTitle(title: String?): Boolean = title?.startsWith(TITLE_PREFIX) == true
}

internal data class CreatorCalendarSyncReport(
    val created: Int = 0,
    val updated: Int = 0,
    val removed: Int = 0,
    val blocked: Int = 0,
) {
    val changed: Int get() = created + updated + removed
    fun message(): String =
        if (blocked > 0) "${changed} calendar change(s) completed; ${blocked} could not be safely updated. Check the selected calendar."
        else if (changed == 0) "Calendar is already up to date."
        else "${created} added · ${updated} updated · ${removed} old Backlot entries removed."
}
