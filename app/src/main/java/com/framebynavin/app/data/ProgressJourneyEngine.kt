package com.framebynavin.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Read-only creator activity projection for Backlot Progress Journey.
 *
 * This is a history of observed actions, not a measure of time worked.
 * Crucially, OBSERVED_CURRENT (legacy import/seeding), alarm delivery,
 * overdue reminders and scheduled deadlines never become achievements.
 *
 * Personal Reminders have a separate local privacy boundary. Do not add them
 * here until their own durable, consent-aware per-occurrence event journal exists.
 */
enum class ProgressJourneyKind {
    PROJECT_CREATED,
    STAGE_MOVED,
    PROJECT_COMPLETED,
    PROJECT_PUBLISHED,
    IDEA_CAPTURED,
}

data class ProgressJourneyEvent(
    val key: String,
    val kind: ProgressJourneyKind,
    val title: String,
    val description: String,
    val atMillis: Long,
    val sourceId: String,
) {
    fun localDate(zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(atMillis).atZone(zoneId).toLocalDate()
}

object ProgressJourneyEngine {
    fun build(
        tasks: List<CreatorTask>,
        ideas: List<CreatorIdea>,
        timeline: List<CreatorWorkflowTimelineEvent>,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<ProgressJourneyEvent> {
        val result = LinkedHashMap<String, ProgressJourneyEvent>()
        val projectById = tasks.associateBy { it.id }
        fun add(event: ProgressJourneyEvent) {
            if (event.atMillis > 0L && event.atMillis <= nowMillis) result.putIfAbsent(event.key, event)
        }

        timeline.asSequence()
            .filter { it.taskId in projectById && it.atMillis in 1..nowMillis }
            .forEach { entry ->
                val project = projectById.getValue(entry.taskId)
                val title = project.title.ifBlank { "Untitled project" }
                when {
                    entry.type == CreatorWorkflowTimelineType.ENTERED &&
                        entry.source == CreatorWorkflowTimelineSource.PROJECT_CREATED ->
                        add(ProgressJourneyEvent(
                            "project-created:${entry.taskId}:${entry.atMillis}",
                            ProgressJourneyKind.PROJECT_CREATED,
                            title, "Project created", entry.atMillis, entry.taskId,
                        ))
                    entry.type == CreatorWorkflowTimelineType.ENTERED &&
                        entry.source == CreatorWorkflowTimelineSource.STAGE_TRANSITION ->
                        add(ProgressJourneyEvent(
                            "stage-moved:${entry.id}",
                            ProgressJourneyKind.STAGE_MOVED,
                            title, "Moved to ${entry.stageLabel}", entry.atMillis, entry.taskId,
                        ))
                }
            }

        tasks.forEach { task ->
            val name = task.title.ifBlank { "Untitled project" }
            val completed = task.completedAtMillis.takeIf { it in 1..nowMillis }
                ?: timeline.firstOrNull {
                    it.taskId == task.id &&
                        it.type == CreatorWorkflowTimelineType.EXITED &&
                        it.source == CreatorWorkflowTimelineSource.PROJECT_FINISHED &&
                        it.atMillis in 1..nowMillis
                }?.atMillis
            if (task.status == TaskStatus.DONE && completed != null) {
                add(ProgressJourneyEvent(
                    "project-completed:${task.id}", ProgressJourneyKind.PROJECT_COMPLETED,
                    name, "Project completed", completed, task.id,
                ))
            }
            if (!task.publicationIsLegacy && task.publishedAtMillis in 1..nowMillis) {
                add(ProgressJourneyEvent(
                    "project-published:${task.id}", ProgressJourneyKind.PROJECT_PUBLISHED,
                    name, "Marked as published", task.publishedAtMillis, task.id,
                ))
            }
        }

        ideas.asSequence()
            .filter { !it.isTrashed && it.createdAtMillis in 1..nowMillis }
            .forEach { idea ->
                add(ProgressJourneyEvent(
                    "idea-captured:${idea.id}", ProgressJourneyKind.IDEA_CAPTURED,
                    idea.title, "Idea saved", idea.createdAtMillis, idea.id,
                ))
            }
        return result.values.sortedWith(compareByDescending<ProgressJourneyEvent> { it.atMillis }.thenBy { it.key })
    }

    fun onDay(events: List<ProgressJourneyEvent>, date: LocalDate, zoneId: ZoneId): List<ProgressJourneyEvent> =
        events.filter { it.localDate(zoneId) == date }

    fun activeDays(events: List<ProgressJourneyEvent>, zoneId: ZoneId): Set<LocalDate> =
        events.mapTo(mutableSetOf()) { it.localDate(zoneId) }
}
