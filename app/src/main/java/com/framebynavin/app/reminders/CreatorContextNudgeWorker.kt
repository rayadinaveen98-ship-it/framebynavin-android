package com.framebynavin.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.framebynavin.app.MainActivity
import com.framebynavin.app.R
import com.framebynavin.app.data.CreatorContextNudgeEngine
import com.framebynavin.app.data.CreatorContextNudgeLevel
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.TaskStore
import java.util.concurrent.TimeUnit

class CreatorContextNudgeWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = CreatorOsSettingsStore(applicationContext).snapshot()
        if (!settings.contextNudgesEnabled) return Result.success()

        val tasks = TaskStore(applicationContext).load()
        val nudge = CreatorContextNudgeEngine.topNudge(tasks) ?: return Result.success()
        if (nudge.level == CreatorContextNudgeLevel.READY) return Result.success()

        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val previousKey = prefs.getString(KEY_LAST_NUDGE, "").orEmpty()
        val previousAt = prefs.getLong(KEY_LAST_AT, 0L)
        if (previousKey == nudge.key && now - previousAt < DEDUPE_MS) return Result.success()

        ensureChannel(applicationContext)
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val openApp = PendingIntent.getActivity(
            applicationContext,
            8401,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = "${nudge.message} ${nudge.action}"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_framebynavin_launcher)
            .setContentTitle("FrameByNavin · ${nudge.title}")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
        prefs.edit().putString(KEY_LAST_NUDGE, nudge.key).putLong(KEY_LAST_AT, now).apply()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "creator-context-nudges-v1"
        private const val CHANNEL_ID = "creator_context_nudges"
        private const val PREFS = "creator_context_nudge_state"
        private const val KEY_LAST_NUDGE = "last_key"
        private const val KEY_LAST_AT = "last_at"
        private const val NOTIFICATION_ID = 8401
        private const val DEDUPE_MS = 12L * 60L * 60_000L

        fun ensurePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<CreatorContextNudgeWorker>(6, TimeUnit.HOURS).build()
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
                "Creator context nudges",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Optional FrameByNavin nudges when active creator work is at risk or needs attention."
            }
            manager.createNotificationChannel(channel)
        }
    }
}
