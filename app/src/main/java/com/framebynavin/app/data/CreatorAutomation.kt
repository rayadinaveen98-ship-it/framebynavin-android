package com.framebynavin.app.data

import android.content.Context
import org.json.JSONObject
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

    /** The routine choices are user-authored state, not disposable WorkManager cache. */
    fun exportJson(): String = snapshot().let { current ->
        JSONObject()
            .put("dailyBriefRoutineEnabled", current.dailyBriefRoutineEnabled)
            .put("weeklyReviewRoutineEnabled", current.weeklyReviewRoutineEnabled)
            .put("ideaReviewRoutineEnabled", current.ideaReviewRoutineEnabled)
            .toString()
    }

    fun validateJson(raw: String): CreatorAutomationPreferences {
        val json = JSONObject(raw)
        require(json.keys().asSequence().toSet() == setOf(
            "dailyBriefRoutineEnabled", "weeklyReviewRoutineEnabled", "ideaReviewRoutineEnabled",
        )) { "Unexpected creator routine backup fields" }
        fun readBoolean(key: String): Boolean {
            val value = json.get(key)
            require(value is Boolean) { "Creator routine preference '$key' must be true or false" }
            return value
        }
        return CreatorAutomationPreferences(
            dailyBriefRoutineEnabled = readBoolean("dailyBriefRoutineEnabled"),
            weeklyReviewRoutineEnabled = readBoolean("weeklyReviewRoutineEnabled"),
            ideaReviewRoutineEnabled = readBoolean("ideaReviewRoutineEnabled"),
        )
    }

    fun importJson(raw: String) {
        val value = validateJson(raw)
        check(prefs.edit()
            .putBoolean(KEY_DAILY_BRIEF, value.dailyBriefRoutineEnabled)
            .putBoolean(KEY_WEEKLY_REVIEW, value.weeklyReviewRoutineEnabled)
            .putBoolean(KEY_IDEA_REVIEW, value.ideaReviewRoutineEnabled)
            .commit()
        ) { "Could not restore automation routine choices" }
    }

    companion object {
        private const val PREFS = "creator_automation_preferences_v17"
        private const val KEY_DAILY_BRIEF = "daily_brief"
        private const val KEY_WEEKLY_REVIEW = "weekly_review"
        private const val KEY_IDEA_REVIEW = "idea_review"
    }
}

/** Evidence of a worker attempt, not evidence of published content or alarms. */
data class CreatorPlannerRunSnapshot(
    val lastAttemptAtMillis: Long,
    val lastConfirmedAtMillis: Long,
    val lastCreatedCount: Int,
    val retryAtMillis: Long,
    val retryAttempt: Int,
    val state: String,
) {
    val hasUnconfirmedAttempt: Boolean get() =
        state == "RUNNING" || state == "RETRY_PENDING"
}

class CreatorAutomationStateStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** All local evidence is fenced after Creator restore/account data changes. */
    private fun isCurrent(): Boolean =
        prefs.getLong(KEY_GENERATION, -1L) == CreatorDataGate.generation(app)

    fun plannerSnapshot(): CreatorPlannerRunSnapshot {
        if (!isCurrent()) return CreatorPlannerRunSnapshot(0L, 0L, 0, 0L, 0, "NOT_RUN")
        return CreatorPlannerRunSnapshot(
            lastAttemptAtMillis = prefs.getLong(KEY_ATTEMPT, 0L),
            lastConfirmedAtMillis = prefs.getLong(KEY_LAST_PLANNER_AT, 0L),
            lastCreatedCount = prefs.getInt(KEY_LAST_PLANNER_CREATED, 0),
            retryAtMillis = prefs.getLong(KEY_RETRY_AT, 0L),
            retryAttempt = prefs.getInt(KEY_RETRY_ATTEMPT, 0),
            state = prefs.getString(KEY_STATUS, "NOT_RUN").orEmpty(),
        )
    }

    fun recordPlannerAttempt(atMillis: Long = System.currentTimeMillis()) {
        commitState("RUNNING", atMillis, 0)
    }

    fun recordPlannerRetry(
        attemptNumber: Int,
        atMillis: Long = System.currentTimeMillis(),
    ) {
        commitState("RETRY_PENDING", atMillis, attemptNumber.coerceAtLeast(1))
    }

    fun recordPlannerDisabled(atMillis: Long = System.currentTimeMillis()) {
        commitState("SKIPPED_DISABLED", atMillis, 0)
    }

    private fun commitState(status: String, time: Long, retryNumber: Int) {
        val generation = CreatorDataGate.generation(app)
        val edit = prefs.edit()
            .putLong(KEY_GENERATION, generation)
            .putString(KEY_STATUS, status)
        if (status == "RUNNING") {
            edit.putLong(KEY_ATTEMPT, time).putLong(KEY_RETRY_AT, 0L)
                .putInt(KEY_RETRY_ATTEMPT, 0)
        } else if (status == "RETRY_PENDING") {
            edit.putLong(KEY_RETRY_AT, time).putInt(KEY_RETRY_ATTEMPT, retryNumber)
        } else {
            edit.putLong(KEY_ATTEMPT, time).putLong(KEY_RETRY_AT, 0L)
                .putInt(KEY_RETRY_ATTEMPT, 0)
        }
        check(edit.commit()) { "Creator automation run status could not be persisted" }
    }

    fun recordPlannerRun(createdCount: Int, atMillis: Long = System.currentTimeMillis()) {
        val generation = CreatorDataGate.generation(app)
        check(prefs.edit()
            .putLong(KEY_GENERATION, generation)
            .putString(KEY_STATUS, "CONFIRMED")
            .putLong(KEY_LAST_PLANNER_AT, atMillis)
            .putInt(KEY_LAST_PLANNER_CREATED, createdCount.coerceAtLeast(0))
            .putLong(KEY_RETRY_AT, 0L)
            .putInt(KEY_RETRY_ATTEMPT, 0)
            .commit()
        ) { "Creator automation confirmation could not be persisted" }
    }

    fun lastPlannerAtMillis(): Long = plannerSnapshot().lastConfirmedAtMillis
    fun lastPlannerCreatedCount(): Int = plannerSnapshot().lastCreatedCount

    fun routineToken(routine: CreatorRoutine): String = prefs.getString("routine_${routine.name}", "").orEmpty()
    fun markRoutine(routine: CreatorRoutine, token: String) {
        prefs.edit().putString("routine_${routine.name}", token).apply()
    }

    companion object {
        private const val PREFS = "creator_automation_state_v17"
        private const val KEY_LAST_PLANNER_AT = "planner_last_at"
        private const val KEY_LAST_PLANNER_CREATED = "planner_last_created"
        private const val KEY_ATTEMPT = "planner_attempt_at"
        private const val KEY_RETRY_AT = "planner_retry_at"
        private const val KEY_RETRY_ATTEMPT = "planner_retry_attempt"
        private const val KEY_STATUS = "planner_run_state"
        private const val KEY_GENERATION = "planner_restore_generation"
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
