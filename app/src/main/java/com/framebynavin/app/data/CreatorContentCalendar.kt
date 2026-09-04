package com.framebynavin.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class CreatorCalendarSource {
    PROJECT,
    WEEKLY_PLAN,
}

data class CreatorCalendarItem(
    val id: String,
    val title: String,
    val platform: String,
    val contentType: String,
    val atMillis: Long,
    val source: CreatorCalendarSource,
    val taskId: String = "",
    val stageLabel: String = "",
    val completed: Boolean = false,
)

object CreatorContentCalendarEngine {
    fun upcoming(
        tasks: List<CreatorTask>,
        weeklySlots: List<WeeklyScheduleSlot>,
        nowMillis: Long = System.currentTimeMillis(),
        daysAhead: Long = 14,
    ): List<CreatorCalendarItem> {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val end = start.plusDays(daysAhead.coerceAtLeast(1))
        val representedWeeklyKeys = tasks.mapNotNull { it.scheduleOccurrenceKey.takeIf(String::isNotBlank) }.toSet()

        val projectItems = tasks.asSequence()
            .filter { it.status != TaskStatus.SKIPPED && it.archivedAtMillis <= 0L && it.dueAtMillis > 0L }
            .filter { millis ->
                val date = Instant.ofEpochMilli(millis.dueAtMillis).atZone(zone).toLocalDate()
                !date.isBefore(start) && !date.isAfter(end)
            }
            .map { task ->
                CreatorCalendarItem(
                    id = "project:${task.id}",
                    title = task.title,
                    platform = task.platform,
                    contentType = task.contentType,
                    atMillis = task.dueAtMillis,
                    source = CreatorCalendarSource.PROJECT,
                    taskId = task.id,
                    stageLabel = CreatorWorkflowEngine.currentStage(task).label,
                    completed = task.status == TaskStatus.DONE,
                )
            }
            .toList()

        val weeklyItems = WeeklyScheduleEngine.upcomingOccurrences(weeklySlots, nowMillis, daysAhead)
            .filterNot { it.key in representedWeeklyKeys }
            .map { occurrence ->
                CreatorCalendarItem(
                    id = "weekly:${occurrence.key}",
                    title = occurrence.slot.title,
                    platform = occurrence.slot.platform,
                    contentType = occurrence.slot.contentType,
                    atMillis = occurrence.publishAtMillis,
                    source = CreatorCalendarSource.WEEKLY_PLAN,
                    stageLabel = "Scheduled",
                )
            }

        return (projectItems + weeklyItems)
            .distinctBy { it.id }
            .sortedBy { it.atMillis }
    }

    fun groupedByDate(items: List<CreatorCalendarItem>): Map<LocalDate, List<CreatorCalendarItem>> {
        val zone = ZoneId.systemDefault()
        return items.groupBy { Instant.ofEpochMilli(it.atMillis).atZone(zone).toLocalDate() }.toSortedMap()
    }
}
