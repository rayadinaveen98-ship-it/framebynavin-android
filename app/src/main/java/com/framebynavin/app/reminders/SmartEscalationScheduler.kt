package com.framebynavin.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.framebynavin.app.MainActivity
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority

/**
 * Smart V2 schedules one stage at a time. A stage schedules only its immediate successor;
 * any creator acknowledgement cancels that successor. This removes the old percentage-based
 * compression and prevents an entire chain from being queued into a tiny time window.
 */
class SmartEscalationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val ledger = AlarmLedger(context)
    private val configStore = SmartEscalationConfigStore(context)
    private val sessions = SmartSessionStore(context)

    enum class Stage { SOFT, VOICE, ALARM, CRITICAL }

    fun schedule(task: CreatorTask) {
        cancel(task.id)
        if (!isSmartEnabled(task)) return
        val now = System.currentTimeMillis()
        if (task.reminderAtMillis <= now) return

        // New creator-made invalid Smart configurations are blocked in the composer.
        // Legacy/generated invalid configs degrade safely to a single gentle stage instead
        // of compressing Voice/Alarm into seconds.
        scheduleStage(task, Stage.SOFT, task.reminderAtMillis)
    }

    /** Rebuild the one pending stage after reboot/time/package recovery. */
    fun recover(task: CreatorTask) {
        cancelPending(task.id, clearSession = false)
        if (!isSmartEnabled(task)) {
            sessions.clear(task.id)
            return
        }
        val now = System.currentTimeMillis()
        val session = sessions.current(task.id)
        if (session == null) {
            if (task.reminderAtMillis > now) scheduleStage(task, Stage.SOFT, task.reminderAtMillis)
            return
        }

        // A snooze repeats the exact stage reached before snooze. Recovery must preserve it.
        session.snoozedStage?.let { snoozedStage ->
            val resumeAt = if (session.snoozedUntilMillis > now) session.snoozedUntilMillis else now + 5_000L
            scheduleStage(task, snoozedStage, resumeAt)
            return
        }

        val config = configStore.get(task)
        if (!SmartEscalationPolicy.isWindowValid(task.priority, task.reminderAtMillis, task.dueAtMillis, config) && task.priority != TaskPriority.NORMAL) {
            // A pre-V2 invalid reminder already delivered its current stage; do not escalate it.
            return
        }
        val next = SmartEscalationPolicy.nextStage(task.priority, session.stage) ?: return
        val gapMinutes = SmartEscalationPolicy.gapAfterMinutes(task.priority, session.stage, config)
        val planned = session.stageStartedAtMillis + gapMinutes * 60_000L
        val recoveredAt = if (planned > now) planned else now + 5_000L
        scheduleStage(task, next, recoveredAt)
    }

    /** Called immediately after a stage fires. The pending successor is cancelled by any user response. */
    fun scheduleNextIfUnanswered(task: CreatorTask, current: Stage, stageStartedAtMillis: Long = System.currentTimeMillis()) {
        if (!isSmartEnabled(task)) return
        if (!sessions.isCurrent(task.id, current)) return

        val config = configStore.get(task)
        if (!SmartEscalationPolicy.isWindowValid(task.priority, task.reminderAtMillis, task.dueAtMillis, config) && task.priority != TaskPriority.NORMAL) {
            return
        }
        val next = SmartEscalationPolicy.nextStage(task.priority, current) ?: return
        val gap = SmartEscalationPolicy.gapAfterMinutes(task.priority, current, config)
        scheduleStage(task, next, stageStartedAtMillis + gap * 60_000L)
    }

    /** Snooze repeats the stage the creator actually reached; it never restarts the chain at SOFT. */
    fun snoozeStage(task: CreatorTask, stage: Stage, resumeAtMillis: Long) {
        cancelPending(task.id, clearSession = false)
        if (!isSmartEnabled(task) || resumeAtMillis <= System.currentTimeMillis()) return
        sessions.markSnoozed(task.id, stage, resumeAtMillis)
        scheduleStage(task, stage, resumeAtMillis)
    }

    fun activeStage(taskId: String): Stage? {
        val session = sessions.current(taskId) ?: return null
        return session.snoozedStage ?: session.stage
    }

    fun markStageActive(taskId: String, stage: Stage, atMillis: Long = System.currentTimeMillis()) {
        sessions.markStage(taskId, stage, atMillis)
    }

    fun finishSession(taskId: String) {
        cancelPending(taskId, clearSession = true)
    }

    fun cancel(taskId: String) {
        cancelPending(taskId, clearSession = true)
    }

    fun isWindowValid(task: CreatorTask): Boolean = SmartEscalationPolicy.isWindowValid(
        task.priority,
        task.reminderAtMillis,
        task.dueAtMillis,
        configStore.get(task),
    )

    private fun isSmartEnabled(task: CreatorTask): Boolean =
        (task.smartEscalationEnabled || task.reminderMode == ReminderMode.SMART) && task.reminderEnabled

    private fun cancelPending(taskId: String, clearSession: Boolean) {
        Stage.entries.forEach { stage ->
            existingPendingIntent(taskId, stage)?.let { alarmManager.cancel(it) }
            ledger.clear(ledgerKey(taskId, stage))
        }
        if (clearSession) sessions.clear(taskId)
    }

    private fun scheduleStage(task: CreatorTask, stage: Stage, atMillis: Long) {
        val now = System.currentTimeMillis()
        if (atMillis <= now) return
        val pendingIntent = stagePendingIntent(task, stage, atMillis)
        val key = ledgerKey(task.id, stage)

        runCatching {
            if ((stage == Stage.ALARM || stage == Stage.CRITICAL) && canScheduleExact()) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    requestCode(task.id, stage) xor 0x4400,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, showIntent), pendingIntent)
            } else if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
            }
        }.onSuccess {
            ledger.markScheduled(key, atMillis)
        }.onFailure {
            ledger.clear(key)
        }
    }

    private fun stagePendingIntent(task: CreatorTask, stage: Stage, atMillis: Long): PendingIntent {
        val intent = Intent(context, EscalationReceiver::class.java)
            .putExtra(ReminderConstants.EXTRA_TASK_ID, task.id)
            .putExtra(ReminderConstants.EXTRA_TITLE, task.title)
            .putExtra(ReminderConstants.EXTRA_PLATFORM, task.platform)
            .putExtra(ReminderConstants.EXTRA_CONTENT_TYPE, task.contentType)
            .putExtra(ReminderConstants.EXTRA_DUE_LABEL, task.dueLabel)
            .putExtra(ReminderConstants.EXTRA_DUE_AT, task.dueAtMillis)
            .putExtra(ReminderConstants.EXTRA_PRIORITY, task.priority.name)
            .putExtra(ReminderConstants.EXTRA_PROGRESS, task.progress)
            .putExtra(ReminderConstants.EXTRA_NOTES, task.notes)
            .putExtra(ReminderConstants.EXTRA_SCHEDULED_AT, atMillis)
            .putExtra(ReminderConstants.EXTRA_TARGET_AT, task.reminderAtMillis)
            .putExtra(ReminderConstants.EXTRA_ALARM_SOUND_URI, task.alarmSoundUri)
            .putExtra(ReminderConstants.EXTRA_REMINDER_MODE, ReminderMode.SMART.name)
            .putExtra(ReminderConstants.EXTRA_VOICE_ENABLED, true)
            .putExtra(ReminderConstants.EXTRA_VOICE_PERSONA, task.voicePersona.name)
            .putExtra(ReminderConstants.EXTRA_VOICE_REPEAT_COUNT, task.voiceRepeatCount)
            .putExtra(ReminderConstants.EXTRA_VOICE_REPEAT_INTERVAL, task.voiceRepeatIntervalSeconds)
            .putExtra(ReminderConstants.EXTRA_ALARM_TIMEOUT_SECONDS, task.alarmTimeoutSeconds)
            .putExtra(ReminderConstants.EXTRA_ESCALATION_STAGE, stage.name)

        return PendingIntent.getBroadcast(
            context,
            requestCode(task.id, stage),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun existingPendingIntent(taskId: String, stage: Stage): PendingIntent? = PendingIntent.getBroadcast(
        context,
        requestCode(taskId, stage),
        Intent(context, EscalationReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun requestCode(taskId: String, stage: Stage): Int = ("$taskId:${stage.name}").hashCode()
    private fun ledgerKey(taskId: String, stage: Stage): String = "$taskId#${stage.name}"
    private fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
}
