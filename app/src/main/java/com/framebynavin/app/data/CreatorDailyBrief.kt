package com.framebynavin.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CreatorDailyBrief(
    val focusTask: CreatorTask?,
    val focusAction: String,
    val focusReason: String,
    val activeCount: Int,
    val dueTodayCount: Int,
    val overdueCount: Int,
    val reminderCount: Int,
    val nudges: List<CreatorContextNudge>,
    val calendar: List<CreatorCalendarItem>,
)

object CreatorDailyBriefEngine {
    fun build(
        tasks: List<CreatorTask>,
        weeklySlots: List<WeeklyScheduleSlot>,
        nowMillis: Long = System.currentTimeMillis(),
    ): CreatorDailyBrief {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val active = CreatorPriorityEngine.rankActive(tasks, nowMillis)
        val focus = active.firstOrNull()
        val recommendation = focus?.let { CreatorPriorityEngine.recommendation(it, nowMillis) }
        val nudges = CreatorContextNudgeEngine.nudges(tasks, nowMillis)
        val calendar = CreatorContentCalendarEngine.upcoming(tasks, weeklySlots, nowMillis, daysAhead = 7)

        return CreatorDailyBrief(
            focusTask = focus,
            focusAction = recommendation?.action ?: "Capture your next idea or project.",
            focusReason = recommendation?.reason ?: "Your active creator queue is clear.",
            activeCount = active.size,
            dueTodayCount = active.count { it.dueAtMillis > 0L && date(it.dueAtMillis, zone) == today },
            overdueCount = active.count { it.dueAtMillis in 1 until nowMillis },
            reminderCount = active.count {
                it.reminderEnabled && it.reminderAtMillis >= nowMillis && it.reminderAtMillis <= nowMillis + 24 * 60 * 60_000L
            },
            nudges = nudges.take(4),
            calendar = calendar.take(6),
        )
    }

    private fun date(millis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
