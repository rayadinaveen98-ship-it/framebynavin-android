package com.framebynavin.app.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/** Read-only forecast; never creates, reschedules or removes any existing project. */
data class CreatorWeeklyPlanPreview(
    val upcoming: List<ScheduleOccurrence>,
    val missing: List<ScheduleOccurrence>,
    val alreadyCreated: Int,
)

object CreatorWeeklyPlanPreviewEngine {
    fun build(
        slots: List<WeeklyScheduleSlot>,
        tasks: List<CreatorTask>,
        nowMillis: Long = System.currentTimeMillis(),
        daysAhead: Long = 14L,
        zone: ZoneId = ZoneId.systemDefault(),
    ): CreatorWeeklyPlanPreview {
        val lastDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            .plusDays(daysAhead.coerceAtLeast(1L) - 1L)
        val occurrences = WeeklyScheduleEngine.upcomingOccurrences(slots, nowMillis, daysAhead)
            .filter { !it.date.isAfter(lastDate) }
        // Include skipped/archived projects too. They are creator-owned work,
        // not permission to silently recreate a previously generated occurrence.
        val existingKeys = tasks.map { it.scheduleOccurrenceKey }.filter { it.isNotBlank() }.toSet()
        val missing = occurrences.filterNot { it.key in existingKeys }
        return CreatorWeeklyPlanPreview(occurrences, missing, occurrences.size - missing.size)
    }
}

/** Only used for an explicitly confirmed reset. Never migrates existing user schedules. */
object CreatorWeeklyPresetPolicy {
    fun previewFor(profile: CreatorProfile): List<WeeklyScheduleSlot> {
        val mode = profile.resolvedPrimaryCreatorMode
        if (mode.contains("film", ignoreCase = true) ||
            mode.contains("entertainment", ignoreCase = true)) {
            return WeeklyScheduleEngine.defaultSlots()
        }
        val platform = CreatorPlatformRegistry.primaryPlatform(profile)
        val format = CreatorPlatformRegistry.defaultFormat(platform)
        val weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY,
            DayOfWeek.SUNDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)
        return weekdays.take(profile.weeklyPublishingTarget.coerceIn(1, 7)).mapIndexed { index, day ->
            WeeklyScheduleSlot(
                id = "backlot_generic_weekly_${index + 1}",
                title = "My weekly post ${index + 1}",
                dayOfWeek = day,
                hour = 18,
                minute = 0,
                platform = platform,
                contentType = format,
                reminderMode = ReminderMode.NONE,
                priority = TaskPriority.NORMAL,
            )
        }
    }

    fun willReplaceCurrent(current: List<WeeklyScheduleSlot>, profile: CreatorProfile): Boolean =
        current != previewFor(profile)
}
