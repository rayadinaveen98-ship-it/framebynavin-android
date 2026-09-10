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
 * Smart delivery is target-time based. reminderAtMillis is a hard FINAL target: RC3 never creates
 * or recovers a Smart stage after it. If a delayed stage would cross the target, the strongest
 * eligible stage is compressed to the target instead.
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
        if (!isTargetBeforePublish(task)) return
        val now = System.currentTimeMillis()
        if (task.reminderAtMillis <= now) return
        val config = configStore.get(task)
        if (!SmartEscalationPolicy.isWindowValid(task.priority, now, task.reminderAtMillis, config)) return

        val firstAt = SmartEscalationPolicy.firstStageAtMillis(task.priority, task.reminderAtMillis, config)
        val eligibleAt = ReminderDeliveryPolicy.smartResumeAt(
            plannedAtMillis = firstAt,
            workingUntilMillis = task.workingUntilMillis,
            finalTargetAtMillis = task.reminderAtMillis,
            nowMillis = now,
        ) ?: return
        scheduleBounded(task, Stage.SOFT, eligibleAt)
    }

    /** Rebuild the one pending stage after reboot/time/package recovery without crossing target. */
    fun recover(task: CreatorTask) {
        if (!isSmartEnabled(task) || !isTargetBeforePublish(task)) {
            cancelPending(task.id, clearSession = true)
            return
        }

        val now = System.currentTimeMillis()
        if (task.reminderAtMillis <= now) {
            cancelPending(task.id, clearSession = true)
            return
        }
        if (task.workingUntilMillis > now) {
            val config = configStore.get(task)
            val firstAt = SmartEscalationPolicy.firstStageAtMillis(task.priority, task.reminderAtMillis, config)
            val resumeAt = ReminderDeliveryPolicy.smartResumeAt(
                plannedAtMillis = firstAt,
                workingUntilMillis = task.workingUntilMillis,
                finalTargetAtMillis = task.reminderAtMillis,
                nowMillis = now,
            )
            cancelPending(task.id, clearSession = true)
            if (resumeAt != null) scheduleBounded(task, Stage.SOFT, resumeAt)
            return
        }
        val session = sessions.current(task.id)
        val pendingStage = when {
            session == null -> Stage.SOFT
            session.snoozedStage != null -> session.snoozedStage
            else -> SmartEscalationPolicy.nextStage(task.priority, session.stage)
        }
        val preservedPendingAt = pendingStage?.let { ledger.scheduledAt(ledgerKey(task.id, it)) }

        cancelPending(task.id, clearSession = false)

        if (session == null) {
            val config = configStore.get(task)
            val firstAt = SmartEscalationPolicy.firstStageAtMillis(task.priority, task.reminderAtMillis, config)
            if (preservedPendingAt != null && preservedPendingAt > now) {
                scheduleBounded(task, Stage.SOFT, preservedPendingAt)
                return
            }
            if (firstAt <= now) {
                scheduleBounded(task, Stage.SOFT, now + 5_000L)
                return
            }
            if (!SmartEscalationPolicy.isWindowValid(task.priority, now, task.reminderAtMillis, config)) return
            scheduleBounded(task, Stage.SOFT, firstAt)
            return
        }

        session.snoozedStage?.let { snoozedStage ->
            val recoveredAt = SmartEscalationPolicy.recoveredStageAtMillis(
                plannedAtMillis = session.snoozedUntilMillis,
                preservedPendingAtMillis = if (pendingStage == snoozedStage) preservedPendingAt else null,
                nowMillis = now,
                fallbackDelayMillis = 5_000L,
            )
            scheduleBounded(task, snoozedStage, recoveredAt)
            return
        }

        val next = SmartEscalationPolicy.nextStage(task.priority, session.stage) ?: return
        val config = configStore.get(task)
        val gapMinutes = SmartEscalationPolicy.gapAfterMinutes(task.priority, session.stage, config)
        val planned = session.stageStartedAtMillis + gapMinutes * 60_000L
        val recoveredAt = SmartEscalationPolicy.recoveredStageAtMillis(
            plannedAtMillis = planned,
            preservedPendingAtMillis = if (pendingStage == next) preservedPendingAt else null,
            nowMillis = now,
            fallbackDelayMillis = gapMinutes * 60_000L,
        )
        scheduleBounded(task, next, recoveredAt)
    }

    /** Called immediately after a stage fires. A successor may never exceed the final target. */
    fun scheduleNextIfUnanswered(
        task: CreatorTask,
        current: Stage,
        stageStartedAtMillis: Long = System.currentTimeMillis(),
    ) {
        if (!isSmartEnabled(task) || task.reminderAtMillis <= System.currentTimeMillis()) return
        if (!sessions.isCurrent(task.id, current)) return

        val next = SmartEscalationPolicy.nextStage(task.priority, current) ?: return
        val config = configStore.get(task)
        val gap = SmartEscalationPolicy.gapAfterMinutes(task.priority, current, config)
        val nextAt = stageStartedAtMillis + gap * 60_000L
        scheduleBounded(task, next, nextAt)
    }

    /** Snooze repeats the reached stage unless the hard target requires final-stage compression. */
    fun snoozeStage(task: CreatorTask, stage: Stage, resumeAtMillis: Long) {
        cancelPending(task.id, clearSession = false)
        val now = System.currentTimeMillis()
        if (!isSmartEnabled(task) || task.reminderAtMillis <= now || resumeAtMillis <= now) return
        val bounded = SmartTargetBoundary.bound(task.priority, stage, resumeAtMillis, task.reminderAtMillis) ?: return
        sessions.markSnoozed(task.id, bounded.stage, bounded.atMillis)
        scheduleStage(task, bounded.stage, bounded.atMillis)
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

    fun isWindowValid(task: CreatorTask, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!isTargetBeforePublish(task)) return false
        return SmartEscalationPolicy.isWindowValid(
            task.priority,
            nowMillis,
            task.reminderAtMillis,
            configStore.get(task),
        )
    }

    private fun isSmartEnabled(task: CreatorTask): Boolean =
        (task.smartEscalationEnabled || task.reminderMode == ReminderMode.SMART) && task.reminderEnabled

    private fun isTargetBeforePublish(task: CreatorTask): Boolean =
        task.dueAtMillis <= 0L || task.reminderAtMillis <= task.dueAtMillis

    private fun scheduleBounded(task: CreatorTask, preferredStage: Stage, candidateAtMillis: Long) {
        val now = System.currentTimeMillis()
        val target = task.reminderAtMillis
        if (target <= now) return
        val bounded = SmartTargetBoundary.bound(task.priority, preferredStage, candidateAtMillis, target) ?: return
        scheduleStage(task, bounded.stage, bounded.atMillis)
    }

    private fun cancelPending(taskId: String, clearSession: Boolean) {
        if (clearSession) {
            ReminderOccurrenceStore(context).invalidate(taskId)
            ReminderSurfaceRegistry.closeTask(taskId)
            AlarmRingingService.stop(context, taskId)
            VoiceReminderService.stop(context, taskId)
        }
        Stage.entries.forEach { stage ->
            existingPendingIntent(taskId, stage)?.let { alarmManager.cancel(it) }
            ledger.clear(ledgerKey(taskId, stage))
        }
        if (clearSession) sessions.clear(taskId)
    }

    private fun scheduleStage(task: CreatorTask, stage: Stage, atMillis: Long) {
        val now = System.currentTimeMillis()
        if (atMillis <= now || atMillis > task.reminderAtMillis) return
        val exactDelivery = canScheduleExact()
        val pendingIntent = stagePendingIntent(task, stage, atMillis, exactDelivery)
        val key = ledgerKey(task.id, stage)

        runCatching {
            if ((stage == Stage.ALARM || stage == Stage.CRITICAL) && exactDelivery) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    requestCode(task.id, stage) xor 0x4400,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, showIntent), pendingIntent)
            } else if (exactDelivery) {
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

    private fun stagePendingIntent(task: CreatorTask, stage: Stage, atMillis: Long, exactDelivery: Boolean): PendingIntent {
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
            .putExtra(ReminderConstants.EXTRA_EXACT_DELIVERY, exactDelivery)

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
