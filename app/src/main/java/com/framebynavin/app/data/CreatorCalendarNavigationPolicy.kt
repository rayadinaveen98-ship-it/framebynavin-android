package com.framebynavin.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Presentation-only rules. Neither the calendar nor Daily Brief changes a project,
 * schedules a reminder, or manufactures a project from a weekly template.
 */
object CreatorCalendarNavigationPolicy {
    fun recentOverdue(
        tasks: List<CreatorTask>,
        nowMillis: Long = System.currentTimeMillis(),
        lookbackDays: Long = 14,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<CreatorTask> {
        val cutoff = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            .minusDays(lookbackDays.coerceAtLeast(1L)).atStartOfDay(zone).toInstant().toEpochMilli()
        return tasks.filter {
            it.archivedAtMillis <= 0L &&
                (it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING) &&
                it.dueAtMillis in cutoff until nowMillis
        }.sortedByDescending { it.dueAtMillis }
    }

    fun forDay(
        items: List<CreatorCalendarItem>,
        selectedDate: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<CreatorCalendarItem> = items.filter {
        Instant.ofEpochMilli(it.atMillis).atZone(zone).toLocalDate() == selectedDate
    }

    fun isProjectLink(item: CreatorCalendarItem): Boolean =
        item.source == CreatorCalendarSource.PROJECT && item.taskId.isNotBlank()

    fun kindLabel(item: CreatorCalendarItem): String = when {
        item.source == CreatorCalendarSource.WEEKLY_PLAN -> "Weekly plan · not a project yet"
        item.completed -> "Completed project"
        else -> "Project deadline"
    }
}
