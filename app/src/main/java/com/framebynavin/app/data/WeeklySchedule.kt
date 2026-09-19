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

    private const val FRAMEBYNAVIN_V4_PREFIX = "fbn_v4_"

    private val legacySeedSlotIds = setOf(
        "mon_x_thought",
        "mon_frame_today",
        "tue_x_poll",
        "tue_scene_works",
        "wed_carousel",
        "wed_cinematic_moment",
        "thu_frame_breakdown_ig",
        "thu_frame_breakdown_yt",
        "fri_release_short",
        "sun_flagship",
        "sun_companion_reel",
    )

    fun isLegacySeedSlot(slotId: String): Boolean = slotId in legacySeedSlotIds

    /**
     * Locked FrameByNavin V4 creator rhythm.
     *
     * Cross-platform content is represented as one master Backlot project rather than duplicate
     * Instagram / YouTube / X projects. The title records every delivery surface while the
     * platform + format choose the strongest production workflow for that master asset.
     */
    fun frameByNavinV4Slots(): List<WeeklyScheduleSlot> = buildList {
        DayOfWeek.values().forEach { day ->
            val dayKey = day.name.lowercase(Locale.ROOT).take(3)
            add(
                slot(
                    id = "${FRAMEBYNAVIN_V4_PREFIX}${dayKey}_frame",
                    title = "Frame of the Day · IG + YT Community + X",
                    day = day,
                    hour = 9,
                    minute = 0,
                    platform = "Instagram",
                    contentType = "Post",
                    reminderMode = ReminderMode.SIMPLE,
                    priority = TaskPriority.NORMAL,
                )
            )
            add(
                slot(
                    id = "${FRAMEBYNAVIN_V4_PREFIX}${dayKey}_recommendation",
                    title = "Movie Recommendation · IG Reel + YT Short + X",
                    day = day,
                    hour = 13,
                    minute = 0,
                    platform = "YouTube",
                    contentType = "Short",
                    reminderMode = ReminderMode.SIMPLE,
                    priority = TaskPriority.IMPORTANT,
                )
            )
            add(
                slot(
                    id = "${FRAMEBYNAVIN_V4_PREFIX}${dayKey}_10pm_cinema",
                    title = "10 PM Cinema · Instagram",
                    day = day,
                    hour = 22,
                    minute = 0,
                    platform = "Instagram",
                    contentType = "Reel",
                    reminderMode = ReminderMode.SIMPLE,
                    priority = TaskPriority.NORMAL,
                )
            )
        }

        add(
            slot(
                id = "${FRAMEBYNAVIN_V4_PREFIX}tue_scene_works",
                title = "Why This Scene Works · IG Reel + YT Short + X",
                day = DayOfWeek.TUESDAY,
                hour = 20,
                minute = 0,
                platform = "YouTube",
                contentType = "Short",
                reminderMode = ReminderMode.SMART,
                priority = TaskPriority.IMPORTANT,
            )
        )
        add(
            slot(
                id = "${FRAMEBYNAVIN_V4_PREFIX}wed_cinematic_moment",
                title = "Every Cinematic Moment",
                day = DayOfWeek.WEDNESDAY,
                hour = 19,
                minute = 0,
                platform = "YouTube",
                contentType = "Cinematic Moment",
                reminderMode = ReminderMode.SMART,
                priority = TaskPriority.IMPORTANT,
            )
        )
        add(
            slot(
                id = "${FRAMEBYNAVIN_V4_PREFIX}wed_ecm_promo",
                title = "Every Cinematic Moment Promo · IG Reel + YT Short + X",
                day = DayOfWeek.WEDNESDAY,
                hour = 20,
                minute = 30,
                platform = "YouTube",
                contentType = "Short",
                reminderMode = ReminderMode.SIMPLE,
                priority = TaskPriority.IMPORTANT,
            )
        )
        add(
            slot(
                id = "${FRAMEBYNAVIN_V4_PREFIX}fri_review",
                title = "Friday Movie Review",
                day = DayOfWeek.FRIDAY,
                hour = 19,
                minute = 0,
                platform = "YouTube",
                contentType = "Long-form",
                reminderMode = ReminderMode.SMART,
                priority = TaskPriority.IMPORTANT,
            )
        )
        add(
            slot(
                id = "${FRAMEBYNAVIN_V4_PREFIX}fri_review_promo",
                title = "Review Promo · IG Reel + YT Short + X",
                day = DayOfWeek.FRIDAY,
                hour = 20,
                minute = 30,
                platform = "YouTube",
                contentType = "Short",
                reminderMode = ReminderMode.SIMPLE,
                priority = TaskPriority.IMPORTANT,
            )
        )
        add(
            slot(
                id = "${FRAMEBYNAVIN_V4_PREFIX}sun_flagship",
                title = "Flagship Cinematic Analysis",
                day = DayOfWeek.SUNDAY,
                hour = 10,
                minute = 0,
                platform = "YouTube",
                contentType = "Long-form",
                reminderMode = ReminderMode.SMART,
                priority = TaskPriority.CRITICAL,
            )
        )
        add(
            slot(
                id = "${FRAMEBYNAVIN_V4_PREFIX}sun_flagship_promo",
                title = "Flagship Promo · IG Reel + YT Short + X",
                day = DayOfWeek.SUNDAY,
                hour = 18,
                minute = 0,
                platform = "YouTube",
                contentType = "Short",
                reminderMode = ReminderMode.SIMPLE,
                priority = TaskPriority.IMPORTANT,
            )
        )
    }

    fun defaultSlots(): List<WeeklyScheduleSlot> = frameByNavinV4Slots()

    fun isFrameByNavinV4Preset(slots: List<WeeklyScheduleSlot>): Boolean {
        val expectedIds = frameByNavinV4Slots().mapTo(linkedSetOf()) { it.id }
        val actualIds = slots.mapTo(linkedSetOf()) { it.id }
        return expectedIds == actualIds
    }

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
