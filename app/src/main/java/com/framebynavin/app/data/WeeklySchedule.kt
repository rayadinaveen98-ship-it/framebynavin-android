package com.framebynavin.app.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

enum class ScheduleCadence {
    EVERY_WEEK,
    WEEKS_1_3,
}

data class WeeklyScheduleSlot(
    val id: String,
    val title: String,
    val dayOfWeek: DayOfWeek,
    val hour: Int,
    val minute: Int,
    val platform: String,
    val contentType: String,
    val enabled: Boolean = true,
    val cadence: ScheduleCadence = ScheduleCadence.EVERY_WEEK,
    val reminderMode: ReminderMode = ReminderMode.SMART,
    val priority: TaskPriority = TaskPriority.IMPORTANT,
)

data class ScheduleOccurrence(
    val slot: WeeklyScheduleSlot,
    val date: LocalDate,
    val publishAtMillis: Long,
    val key: String,
)

data class StageCheckpoint(
    val stageIndex: Int,
    val label: String,
    val dueAtMillis: Long,
)

object WeeklyScheduleEngine {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun defaultSlots(): List<WeeklyScheduleSlot> = listOf(
        slot("mon_x_thought", "Monday Cinema Thought", DayOfWeek.MONDAY, 9, 30, "X", "Post", ReminderMode.SIMPLE, TaskPriority.NORMAL),
        slot("mon_frame_today", "#TheFrameOfToday", DayOfWeek.MONDAY, 19, 0, "Instagram", "Post", ReminderMode.SMART, TaskPriority.IMPORTANT),
        slot("tue_x_poll", "Tuesday Cinema Poll", DayOfWeek.TUESDAY, 9, 0, "X", "Post", ReminderMode.SIMPLE, TaskPriority.NORMAL),
        slot("tue_scene_works", "Why This Scene Works", DayOfWeek.TUESDAY, 19, 0, "Instagram", "Reel", ReminderMode.SMART, TaskPriority.IMPORTANT),
        slot("wed_carousel", "Wednesday Cinema Carousel", DayOfWeek.WEDNESDAY, 12, 0, "Instagram", "Post", ReminderMode.SMART, TaskPriority.IMPORTANT),
        WeeklyScheduleSlot(
            id = "wed_cinematic_moment",
            title = "Every Cinematic Moment",
            dayOfWeek = DayOfWeek.WEDNESDAY,
            hour = 19,
            minute = 0,
            platform = "YouTube",
            contentType = "Cinematic Moment",
            enabled = true,
            cadence = ScheduleCadence.WEEKS_1_3,
            reminderMode = ReminderMode.SMART,
            priority = TaskPriority.IMPORTANT,
        ),
        slot("thu_frame_breakdown_ig", "Frame Breakdown", DayOfWeek.THURSDAY, 19, 0, "Instagram", "Reel", ReminderMode.SMART, TaskPriority.IMPORTANT),
        slot("thu_frame_breakdown_yt", "Frame Breakdown Short", DayOfWeek.THURSDAY, 19, 0, "YouTube", "Short", ReminderMode.SMART, TaskPriority.IMPORTANT),
        slot("fri_release_short", "Release / Reaction Short", DayOfWeek.FRIDAY, 16, 0, "YouTube", "Short", ReminderMode.SMART, TaskPriority.IMPORTANT),
        slot("sun_flagship", "FrameByNavin Analysis", DayOfWeek.SUNDAY, 10, 0, "YouTube", "Long-form", ReminderMode.SMART, TaskPriority.CRITICAL),
        slot("sun_companion_reel", "Sunday Companion Reel", DayOfWeek.SUNDAY, 21, 0, "Instagram", "Reel", ReminderMode.SMART, TaskPriority.IMPORTANT),
    )

    fun upcomingOccurrences(
        slots: List<WeeklyScheduleSlot>,
        fromMillis: Long = System.currentTimeMillis(),
        daysAhead: Long = 8,
    ): List<ScheduleOccurrence> {
        val from = Instant.ofEpochMilli(fromMillis).atZone(zone)
        val start = from.toLocalDate()
        val end = start.plusDays(daysAhead)
        return buildList {
            var date = start
            while (!date.isAfter(end)) {
                slots.filter { it.enabled && it.dayOfWeek == date.dayOfWeek && cadenceMatches(it.cadence, date) }
                    .forEach { slot ->
                        val publish = date.atTime(slot.hour.coerceIn(0, 23), slot.minute.coerceIn(0, 59)).atZone(zone).toInstant().toEpochMilli()
                        if (publish > fromMillis) {
                            add(ScheduleOccurrence(slot, date, publish, occurrenceKey(slot.id, date)))
                        }
                    }
                date = date.plusDays(1)
            }
        }.sortedBy { it.publishAtMillis }
    }

    fun occurrenceKey(slotId: String, date: LocalDate): String = "$slotId@${date}"

    fun dateFromOccurrenceKey(key: String): LocalDate? =
        key.substringAfter('@', "").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun publishAt(slot: WeeklyScheduleSlot, date: LocalDate): Long =
        date.atTime(slot.hour.coerceIn(0, 23), slot.minute.coerceIn(0, 59)).atZone(zone).toInstant().toEpochMilli()

    fun checkpoints(platform: String, contentType: String, publishAtMillis: Long): List<StageCheckpoint> {
        val template = CreatorWorkflowEngine.templateFor(platform, contentType)
        val offsets = offsetsFor(template.id, template.stages.size)
        return template.stages.mapIndexed { index, stage ->
            val offsetMinutes = offsets.getOrElse(index) { 0L }
            StageCheckpoint(index, stage.label, publishAtMillis - offsetMinutes * 60_000L)
        }
    }

    fun checkpoints(task: CreatorTask): List<StageCheckpoint> = checkpoints(task.platform, task.contentType, task.dueAtMillis)

    fun suggestedStageIndex(platform: String, contentType: String, publishAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Int {
        val checkpoints = checkpoints(platform, contentType, publishAtMillis)
        if (checkpoints.isEmpty()) return 0
        val completedByClock = checkpoints.count { it.dueAtMillis <= nowMillis }
        return completedByClock.coerceIn(0, checkpoints.lastIndex)
    }

    fun reminderTargetForStage(task: CreatorTask, stageIndex: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val checkpoint = checkpoints(task).getOrNull(stageIndex)?.dueAtMillis ?: task.dueAtMillis
        if (checkpoint > nowMillis) return checkpoint
        val recovery = task.dueAtMillis - 15 * 60_000L
        return when {
            recovery > nowMillis -> recovery
            task.dueAtMillis > nowMillis -> task.dueAtMillis
            else -> checkpoint
        }
    }

    fun nextOccurrence(slots: List<WeeklyScheduleSlot>, nowMillis: Long = System.currentTimeMillis()): ScheduleOccurrence? =
        upcomingOccurrences(slots, nowMillis, 8).firstOrNull()

    fun formatOccurrence(millis: Long): String {
        val formatter = DateTimeFormatter.ofPattern("EEE · h:mm a", Locale.getDefault())
        return Instant.ofEpochMilli(millis).atZone(zone).format(formatter)
    }

    fun dueLabel(millis: Long): String {
        val selected = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        val time = Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
        return when (selected) {
            today -> "Today · $time"
            today.plusDays(1) -> "Tomorrow · $time"
            else -> Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", Locale.getDefault()))
        }
    }

    private fun cadenceMatches(cadence: ScheduleCadence, date: LocalDate): Boolean = when (cadence) {
        ScheduleCadence.EVERY_WEEK -> true
        ScheduleCadence.WEEKS_1_3 -> {
            val week = date.get(WeekFields.of(Locale.getDefault()).weekOfMonth())
            week == 1 || week == 3
        }
    }

    private fun offsetsFor(templateId: String, stageCount: Int): List<Long> = when (templateId) {
        "youtube_longform" -> listOf(96 * 60L, 72 * 60L, 48 * 60L, 32 * 60L, 20 * 60L, 10 * 60L, 2 * 60L, 0L)
        "youtube_cinematic_moment" -> listOf(48 * 60L, 30 * 60L, 16 * 60L, 8 * 60L, 2 * 60L, 0L)
        "youtube_short", "instagram_reel" -> listOf(24 * 60L, 10 * 60L, 6 * 60L, 3 * 60L, 60L, 30L, 0L)
        "instagram_post" -> listOf(8 * 60L, 4 * 60L, 2 * 60L, 60L, 0L)
        "instagram_story" -> listOf(3 * 60L, 2 * 60L, 60L, 0L)
        "x_post" -> listOf(2 * 60L, 60L, 30L, 0L)
        "x_video" -> listOf(8 * 60L, 6 * 60L, 3 * 60L, 60L, 30L, 0L)
        "x_update" -> listOf(60L, 30L, 0L)
        else -> {
            if (stageCount <= 1) listOf(0L)
            else (stageCount - 1 downTo 0).map { it * 120L }
        }
    }

    private fun slot(
        id: String,
        title: String,
        day: DayOfWeek,
        hour: Int,
        minute: Int,
        platform: String,
        contentType: String,
        reminderMode: ReminderMode,
        priority: TaskPriority,
    ) = WeeklyScheduleSlot(
        id = id,
        title = title,
        dayOfWeek = day,
        hour = hour,
        minute = minute,
        platform = platform,
        contentType = contentType,
        reminderMode = reminderMode,
        priority = priority,
    )
}
