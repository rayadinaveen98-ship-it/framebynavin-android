package com.framebynavin.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.framebynavin.app.MainActivity
import com.framebynavin.app.R
import com.framebynavin.app.data.CreatorAutoPlanEngine
import com.framebynavin.app.data.CreatorAutomationPreferencesStore
import com.framebynavin.app.data.CreatorAutomationStateStore
import com.framebynavin.app.data.CreatorDailyBriefEngine
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorRoutine
import com.framebynavin.app.data.CreatorRoutinePolicy
import com.framebynavin.app.data.IdeaStatus
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.data.WeeklyScheduleStore
import com.framebynavin.app.widget.CreatorWidgetContract
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class CreatorAutoPlanWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        val settings = CreatorOsSettingsStore(app).snapshot()
        val state = CreatorAutomationStateStore(app)
        if (!settings.weeklyAutoPlanEnabled) {
            state.recordPlannerRun(0)
            return Result.success()
        }

        return runCatching {
            val taskStore = TaskStore(app)
            val slots = WeeklyScheduleStore(app).loadOrSeed()
            val current = taskStore.load()
            val result = CreatorAutoPlanEngine.merge(
                tasks = current,
                slots = slots,
                defaults = settings,
                daysAhead = CreatorAutoPlanEngine.DEFAULT_HORIZON_DAYS,
            )
            if (result.created.isNotEmpty()) {
                taskStore.save(result.tasks)
                val regular = ReminderScheduler(app)
                val smart = SmartEscalationScheduler(app)
                val smartConfig = SmartEscalationConfigStore(app)
                result.created.forEach { task ->
                    when {
                        task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled -> {
                            smartConfig.put(task, SmartEscalationConfigStore.DEFAULT)
                            smart.schedule(task)
                        }
                        task.reminderEnabled && task.reminderMode != ReminderMode.NONE -> regular.schedule(task)
                    }
                }
            }
            state.recordPlannerRun(result.created.size)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val PERIODIC_WORK = "creator-background-auto-plan-v17"
        private const val NOW_WORK = "creator-background-auto-plan-now-v17"

        fun ensurePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<CreatorAutoPlanWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun enqueueNow(context: Context) {
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                NOW_WORK,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<CreatorAutoPlanWorker>().build(),
            )
        }
    }
}

class CreatorRoutineWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        val prefs = CreatorAutomationPreferencesStore(app).snapshot()
        if (!prefs.dailyBriefRoutineEnabled && !prefs.weeklyReviewRoutineEnabled && !prefs.ideaReviewRoutineEnabled) {
            return Result.success()
        }
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) return Result.success()

        return runCatching {
            val state = CreatorAutomationStateStore(app)
            val now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
            val tasks = TaskStore(app).load()
            val slots = WeeklyScheduleStore(app).loadOrSeed()
            val ideas = IdeaVaultStore(app).load()

            if (prefs.dailyBriefRoutineEnabled) {
                maybeNotify(
                    routine = CreatorRoutine.DAILY_BRIEF,
                    now = now,
                    state = state,
                    title = "FrameByNavin · Daily Brief",
                    body = CreatorDailyBriefEngine.build(tasks, slots).let { brief ->
                        brief.focusTask?.let { "Focus: ${it.title} · ${brief.focusAction}" }
                            ?: "Your creator queue is clear. Capture the next thing when it hits."
                    },
                    action = CreatorWidgetContract.ACTION_DAILY_BRIEF,
                    notificationId = 8711,
                )
            }

            if (prefs.weeklyReviewRoutineEnabled) {
                val weekAgo = System.currentTimeMillis() - 7L * 24L * 60L * 60_000L
                val completed = tasks.count { it.status == TaskStatus.DONE && it.completedAtMillis >= weekAgo }
                val active = tasks.count { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
                maybeNotify(
                    routine = CreatorRoutine.WEEKLY_REVIEW,
                    now = now,
                    state = state,
                    title = "FrameByNavin · Weekly Review",
                    body = "$completed completed this week · $active active now. Open Insights for the full creator review.",
                    action = CreatorWidgetContract.ACTION_OPEN_INSIGHTS,
                    notificationId = 8712,
                )
            }

            if (prefs.ideaReviewRoutineEnabled) {
                val unfinished = ideas.count { it.status != IdeaStatus.ARCHIVED && it.status != IdeaStatus.CONVERTED }
                maybeNotify(
                    routine = CreatorRoutine.IDEA_REVIEW,
                    now = now,
                    state = state,
                    title = "FrameByNavin · Idea Review",
                    body = if (unfinished == 0) "Idea Vault is clear." else "$unfinished ideas are still waiting in your vault. Pick one worth moving forward.",
                    action = CreatorWidgetContract.ACTION_IDEA_VAULT,
                    notificationId = 8713,
                )
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    private fun maybeNotify(
        routine: CreatorRoutine,
        now: ZonedDateTime,
        state: CreatorAutomationStateStore,
        title: String,
        body: String,
        action: String,
        notificationId: Int,
    ) {
        val previous = state.routineToken(routine)
        if (!CreatorRoutinePolicy.due(routine, now, previous)) return
        ensureChannel(applicationContext)
        val intent = Intent(applicationContext, MainActivity::class.java)
            .setAction(action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(
            applicationContext,
            ("routine:$action").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_framebynavin_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
        state.markRoutine(routine, CreatorRoutinePolicy.token(routine, now))
    }

    companion object {
        private const val WORK_NAME = "creator-routines-v17"
        private const val CHANNEL_ID = "creator_routines"

        fun ensurePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<CreatorRoutineWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        private fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Creator routines",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Optional FrameByNavin daily brief, weekly review and idea review routine notifications."
            }
            manager.createNotificationChannel(channel)
        }
    }
}
