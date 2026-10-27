package com.framebynavin.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.framebynavin.app.MainActivity
import com.framebynavin.app.R
import com.framebynavin.app.widget.CreatorWidgetContract
import com.framebynavin.app.data.CreatorContextNudgeEngine
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.data.CreatorDataGate
import java.util.concurrent.TimeUnit

class CreatorContextNudgeWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching { CreatorDataGate.readyTransaction(applicationContext) {
        val settings = CreatorOsSettingsStore(applicationContext).snapshot()
        if (!settings.contextNudgesEnabled) return@readyTransaction Result.success()
        // Marketing-style engagement is strictly optional, unlike real reminders.
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return@readyTransaction Result.success()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return@readyTransaction Result.success()

        val tasks = TaskStore(applicationContext).load()
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (V205NudgePausePolicy.isPaused(
                now, prefs.getLong(V205NudgePausePolicy.KEY_PAUSED_UNTIL, 0L)
            )
        ) return@readyTransaction Result.success()
        val tasksById = tasks.associateBy { it.id }
        val freshNudges = CreatorContextNudgeEngine.nudges(tasks, now).filter { candidate ->
            V204NudgeFreshnessPolicy.relevant(candidate, tasksById[candidate.taskId], now)
        }
        val previousKey = prefs.getString(KEY_LAST_NUDGE, "").orEmpty()
        val previousAt = prefs.getLong(KEY_LAST_AT, 0L)
        // The highest-ranked project might still be in cooldown. Instead of
        // skipping everyone, use the next genuinely eligible active project.
        val nudge = V203NudgeSelector.choose(
            candidates = freshNudges,
            nowMillis = now,
            lastGlobalShownAtMillis = previousAt,
            lastGlobalKey = previousKey,
            lastShownAtForKey = { key -> V203NudgeHistory.lastShown(prefs, key) },
            quietHoursEnabled = settings.quietHoursEnabled,
            quietStartMinute = settings.quietHoursStartMinute,
            quietEndMinute = settings.quietHoursEndMinute,
        ) ?: return@readyTransaction Result.success()

        ensureChannel(applicationContext)
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
        ) return@readyTransaction Result.success()
        val openApp = PendingIntent.getActivity(
            applicationContext,
            8401,
            Intent(applicationContext, MainActivity::class.java).apply {
                // Tap opens the real active project's workspace, not generic Home.
                action = CreatorWidgetContract.ACTION_OPEN_STUDIO
                putExtra(CreatorWidgetContract.EXTRA_TASK_ID, nudge.taskId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val message = V202CreatorNudgeContent.forNudge(nudge)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.backlot_v198_notification_monochrome)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setContentIntent(openApp)
            .addAction(
                0,
                "Pause 7 days",
                PendingIntent.getBroadcast(
                    applicationContext,
                    8402,
                    Intent(applicationContext, V205NudgePauseReceiver::class.java).apply {
                        action = V205NudgePausePolicy.ACTION_PAUSE_WEEK
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
        // Preserve both the global cap and per-project-key cooldown durably.
        V203NudgeHistory.recordShown(prefs, nudge.key, now)
        Result.success()
    } }.getOrElse { Result.retry() }

    companion object {
        private const val WORK_NAME = "creator-context-nudges-v1"
        private const val WORK_NOW_NAME = "creator-context-nudges-now-v1"
        private const val CHANNEL_ID = "creator_context_nudges"
        private const val PREFS = "creator_context_nudge_state"
        private const val KEY_LAST_NUDGE = "last_key"
        private const val KEY_LAST_AT = "last_at"
        private const val NOTIFICATION_ID = 8401

        fun ensurePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<CreatorContextNudgeWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<CreatorContextNudgeWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                WORK_NOW_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        private fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Creator context nudges",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Optional Backlot nudges when active creator work is at risk or needs attention."
            }
            manager.createNotificationChannel(channel)
        }
    }
}
