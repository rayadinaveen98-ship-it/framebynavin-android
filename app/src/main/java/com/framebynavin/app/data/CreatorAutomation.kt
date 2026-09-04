package com.framebynavin.app.data

import android.content.Context
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

data class CreatorAutoPlanResult(
    val tasks: List<CreatorTask>,
    val created: List<CreatorTask>,
)

object CreatorAutoPlanEngine {
    const val DEFAULT_HORIZON_DAYS = 14L

    fun merge(
        tasks: List<CreatorTask>,
        slots: List<WeeklyScheduleSlot>,
        defaults: CreatorOsSettings,
        nowMillis: Long = System.currentTimeMillis(),
        daysAhead: Long = DEFAULT_HORIZON_DAYS,
    ): CreatorAutoPlanResult {
        val merged = tasks.toMutableList()
        val created = mutableListOf<CreatorTask>()
        val existingKeys = tasks.mapNotNull { it.scheduleOccurrenceKey.takeIf(String::isNotBlank) }.toMutableSet()

        WeeklyScheduleEngine.upcomingOccurrences(
            slots = slots,
            fromMillis = nowMillis,
            daysAhead = daysAhead,
        ).forEach { occurrence ->
            if (!existingKeys.add(occurrence.key)) return@forEach
            val task = buildTask(occurrence, defaults, nowMillis)
            merged.add(0, task)
            created += task
        }

        return CreatorAutoPlanResult(merged, created)
    }

    private fun buildTask(
        occurrence: ScheduleOccurrence,
        defaults: CreatorOsSettings,
        nowMillis: Long,
    ): CreatorTask {
        val slot = occurrence.slot
        val template = CreatorWorkflowEngine.templateFor(slot.platform, slot.contentType)
        val stageIndex = WeeklyScheduleEngine.suggestedStageIndex(
            platform = slot.platform,
            contentType = slot.contentType,
            publishAtMillis = occurrence.publishAtMillis,
            nowMillis = nowMillis,
        )
        val mode = slot.reminderMode
        val enabled = mode != ReminderMode.NONE
        var task = CreatorTask(
            id = "auto:${occurrence.key}",
            title = slot.title,
            platform = slot.platform,
            contentType = slot.contentType,
            dueLabel = WeeklyScheduleEngine.dueLabel(occurrence.publishAtMillis),
            dueAtMillis = occurrence.publishAtMillis,
            status = TaskStatus.PLANNED,
            progress = CreatorWorkflowEngine.progressForStage(stageIndex, template.stages.size),
            workflowStageIndex = stageIndex,
            reminderEnabled = enabled,
            reminderAtMillis = 0L,
            priority = slot.priority,
            notes = "Weekly plan · Background Auto Plan",
            alertType = if (mode == ReminderMode.ALARM || mode == ReminderMode.SMART) ReminderAlertType.ALARM else ReminderAlertType.NOTIFICATION,
            voiceEnabled = mode == ReminderMode.VOICE || mode == ReminderMode.SMART,
            smartEscalationEnabled = mode == ReminderMode.SMART,
            reminderMode = mode,
            voicePersona = defaults.defaultVoicePersona,
            voiceRepeatCount = 3,
            voiceRepeatIntervalSeconds = 10,
            alarmTimeoutSeconds = defaults.defaultAlarmTimeoutSeconds,
            scheduleSlotId = slot.id,
            scheduleOccurrenceKey = occurrence.key,
            autoStageReminder = enabled,
            origin = CreatorTaskOrigin.WEEKLY,
            sourceRefId = occurrence.key,
        )
        if (enabled) {
            task = task.copy(
                reminderAtMillis = WeeklyScheduleEngine.reminderTargetForStage(task, stageIndex, nowMillis),
            )
        }
        return task
    }
}

data class CreatorAutomationPreferences(
    val dailyBriefRoutineEnabled: Boolean = false,
    val weeklyReviewRoutineEnabled: Boolean = false,
    val ideaReviewRoutineEnabled: Boolean = false,
)

class CreatorAutomationPreferencesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun snapshot() = CreatorAutomationPreferences(
        dailyBriefRoutineEnabled = prefs.getBoolean(KEY_DAILY_BRIEF, false),
        weeklyReviewRoutineEnabled = prefs.getBoolean(KEY_WEEKLY_REVIEW, false),
        ideaReviewRoutineEnabled = prefs.getBoolean(KEY_IDEA_REVIEW, false),
    )

    fun setDailyBriefRoutineEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_BRIEF, value).apply()
    }

    fun setWeeklyReviewRoutineEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_WEEKLY_REVIEW, value).apply()
    }

    fun setIdeaReviewRoutineEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_IDEA_REVIEW, value).apply()
    }

    companion object {
        private const val PREFS = "creator_automation_preferences_v17"
        private const val KEY_DAILY_BRIEF = "daily_brief"
        private const val KEY_WEEKLY_REVIEW = "weekly_review"
        private const val KEY_IDEA_REVIEW = "idea_review"
    }
}

class CreatorAutomationStateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun recordPlannerRun(createdCount: Int, atMillis: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putLong(KEY_LAST_PLANNER_AT, atMillis)
            .putInt(KEY_LAST_PLANNER_CREATED, createdCount.coerceAtLeast(0))
            .apply()
    }

    fun lastPlannerAtMillis(): Long = prefs.getLong(KEY_LAST_PLANNER_AT, 0L)
    fun lastPlannerCreatedCount(): Int = prefs.getInt(KEY_LAST_PLANNER_CREATED, 0)

    fun routineToken(routine: CreatorRoutine): String = prefs.getString("routine_${routine.name}", "").orEmpty()
    fun markRoutine(routine: CreatorRoutine, token: String) {
        prefs.edit().putString("routine_${routine.name}", token).apply()
    }

    companion object {
        private const val PREFS = "creator_automation_state_v17"
        private const val KEY_LAST_PLANNER_AT = "planner_last_at"
        private const val KEY_LAST_PLANNER_CREATED = "planner_last_created"
    }
}

enum class CreatorRoutine {
    DAILY_BRIEF,
    WEEKLY_REVIEW,
    IDEA_REVIEW,
}

object CreatorRoutinePolicy {
    private val weekFields = WeekFields.ISO

    fun due(routine: CreatorRoutine, now: ZonedDateTime, previousToken: String): Boolean {
        if (!inWindow(routine, now)) return false
        return token(routine, now) != previousToken
    }

    fun token(routine: CreatorRoutine, now: ZonedDateTime): String = when (routine) {
        CreatorRoutine.DAILY_BRIEF -> now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        CreatorRoutine.WEEKLY_REVIEW,
        CreatorRoutine.IDEA_REVIEW -> {
            val week = now.get(weekFields.weekOfWeekBasedYear())
            val year = now.get(weekFields.weekBasedYear())
            "$year-W${week.toString().padStart(2, '0')}"
        }
    }

    fun scheduleLabel(routine: CreatorRoutine): String = when (routine) {
        CreatorRoutine.DAILY_BRIEF -> "Every morning · after 8:00 AM IST"
        CreatorRoutine.WEEKLY_REVIEW -> "Sunday · after 7:00 PM IST"
        CreatorRoutine.IDEA_REVIEW -> "Wednesday · after 7:00 PM IST"
    }

    private fun inWindow(routine: CreatorRoutine, now: ZonedDateTime): Boolean = when (routine) {
        CreatorRoutine.DAILY_BRIEF -> now.hour in 8..11
        CreatorRoutine.WEEKLY_REVIEW -> now.dayOfWeek == DayOfWeek.SUNDAY && now.hour >= 19
        CreatorRoutine.IDEA_REVIEW -> now.dayOfWeek == DayOfWeek.WEDNESDAY && now.hour >= 19
    }
}
