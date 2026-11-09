package com.framebynavin.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class CreatorCalendarSource {
    PROJECT,
    WEEKLY_PLAN,
    /** Recorded actual publication; NEVER a projected posting time. */
    PUBLICATION,
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
        recentPublicationDays: Long = 7,
    ): List<CreatorCalendarItem> {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val end = start.plusDays(daysAhead.coerceAtLeast(1) - 1)
        val representedWeeklyKeys = tasks.mapNotNull {
            it.scheduleOccurrenceKey.takeIf(String::isNotBlank)
        }.toSet()
        val publicationStart = start.minusDays(recentPublicationDays.coerceIn(0L, 30L))
            .atStartOfDay(zone).toInstant().toEpochMilli()

        val projectItems = tasks.asSequence()
            .filter { it.status != TaskStatus.SKIPPED && it.archivedAtMillis <= 0L && it.dueAtMillis > 0L }
            .filter { it.dueAtMillis >= nowMillis || it.status == TaskStatus.DONE }
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

        // Only real owner-confirmed publication timestamps qualify. Publishing
        // readiness, due dates and template previews are NEVER publication.
        val publications = tasks.asSequence()
            // Archived projects can still have real publication history.
            .flatMap { task ->
                val known = task.workspace.deliverables.asSequence()
                    .filter {
                        it.status == CreatorDeliverableStatus.PUBLISHED &&
                            it.publishedAtMillis in publicationStart..nowMillis
                    }
                    .map { output ->
                        CreatorCalendarItem(
                            id = "published:${task.id}:${output.id}",
                            title = output.title.ifBlank { task.title },
                            platform = output.platform,
                            contentType = output.format,
                            atMillis = output.publishedAtMillis,
                            source = CreatorCalendarSource.PUBLICATION,
                            taskId = task.id,
                            stageLabel = "Publication recorded",
                        )
                    }.toList()
                // Legacy project receipts are a fallback only when no
                // explicit output receipt exists for this project.
                if (known.isNotEmpty()) known.asSequence()
                else if (task.publishedAtMillis in publicationStart..nowMillis) {
                    sequenceOf(
                        CreatorCalendarItem(
                            id = "published:${task.id}:project",
                            title = task.title, platform = task.platform,
                            contentType = task.contentType,
                            atMillis = task.publishedAtMillis,
                            source = CreatorCalendarSource.PUBLICATION,
                            taskId = task.id,
                            stageLabel = "Project publication recorded",
                        ),
                    )
                } else emptySequence()
            }.toList()

        val weeklyItems = WeeklyScheduleEngine.upcomingOccurrences(weeklySlots, nowMillis, daysAhead)
            .filter { it.publishAtMillis >= nowMillis && !Instant.ofEpochMilli(it.publishAtMillis).atZone(zone).toLocalDate().isAfter(end) }
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

        return (projectItems + weeklyItems + publications)
            .distinctBy { it.id }
            .sortedBy { it.atMillis }
    }

    fun groupedByDate(items: List<CreatorCalendarItem>): Map<LocalDate, List<CreatorCalendarItem>> {
        val zone = ZoneId.systemDefault()
        return items.groupBy { Instant.ofEpochMilli(it.atMillis).atZone(zone).toLocalDate() }.toSortedMap()
    }
}
