package com.framebynavin.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.framebynavin.app.MainActivity
import com.framebynavin.app.R
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.TaskStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CreatorWidgetUpdater {
    suspend fun updateAll(context: Context, tasks: List<CreatorTask>) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val compactIds = manager.getAppWidgetIds(ComponentName(app, CreatorCompactWidgetProvider::class.java))
        val largeIds = manager.getAppWidgetIds(ComponentName(app, CreatorLargeWidgetProvider::class.java))
        compactIds.forEach { manager.updateAppWidget(it, compactViews(app, tasks)) }
        largeIds.forEach { manager.updateAppWidget(it, largeViews(app, tasks)) }
    }

    private fun compactViews(context: Context, tasks: List<CreatorTask>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_creator_compact)
        val active = activeTasks(tasks)
        val current = active.firstOrNull()
        val nextReminder = nextReminder(tasks)

        views.setTextViewText(R.id.widget_title, "FRAMEBYNAVIN")
        if (current == null) {
            views.setTextViewText(R.id.widget_project, "Nothing urgent right now")
            views.setTextViewText(R.id.widget_stage, "Capture an idea or plan the next piece.")
            views.setViewVisibility(R.id.widget_due, View.GONE)
            views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent(context, CreatorWidgetContract.ACTION_OPEN_TODAY))
        } else {
            views.setTextViewText(R.id.widget_project, current.title)
            views.setTextViewText(R.id.widget_stage, "${CreatorWorkflowEngine.currentStage(current).label} · ${CreatorWorkflowEngine.progress(current)}%")
            views.setTextViewText(R.id.widget_due, dueText(current.dueAtMillis))
            views.setViewVisibility(R.id.widget_due, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent(context, CreatorWidgetContract.ACTION_OPEN_STUDIO, current.id))
        }
        views.setTextViewText(
            R.id.widget_reminder,
            nextReminder?.let { "Next reminder · ${clock(it.reminderAtMillis)} · ${it.title}" } ?: "No upcoming reminders",
        )
        views.setOnClickPendingIntent(R.id.widget_idea, quickIdeaPendingIntent(context))
        return views
    }

    private fun largeViews(context: Context, tasks: List<CreatorTask>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_creator_large)
        val active = activeTasks(tasks)
        val current = active.firstOrNull()
        val nextReminder = nextReminder(tasks)
        val todayCount = active.count { isToday(it.dueAtMillis) }

        views.setTextViewText(R.id.widget_large_title, "FRAMEBYNAVIN")
        views.setTextViewText(R.id.widget_large_today_count, "$todayCount TODAY")
        if (current == null) {
            views.setTextViewText(R.id.widget_large_project, "Creator desk is clear")
            views.setTextViewText(R.id.widget_large_stage, "Use Quick Idea when something hits.")
            views.setTextViewText(R.id.widget_large_progress, "")
            views.setOnClickPendingIntent(R.id.widget_large_project_area, mainPendingIntent(context, CreatorWidgetContract.ACTION_OPEN_TODAY))
        } else {
            val progress = CreatorWorkflowEngine.progress(current)
            views.setTextViewText(R.id.widget_large_project, current.title)
            views.setTextViewText(R.id.widget_large_stage, "${CreatorWorkflowEngine.currentStage(current).label} · ${dueText(current.dueAtMillis)}")
            views.setTextViewText(R.id.widget_large_progress, "$progress%")
            views.setOnClickPendingIntent(R.id.widget_large_project_area, mainPendingIntent(context, CreatorWidgetContract.ACTION_OPEN_STUDIO, current.id))
        }
        views.setTextViewText(
            R.id.widget_large_reminder,
            nextReminder?.let { "${clock(it.reminderAtMillis)} · ${it.title}" } ?: "No upcoming reminders",
        )
        views.setOnClickPendingIntent(R.id.widget_large_idea, quickIdeaPendingIntent(context))
        views.setOnClickPendingIntent(R.id.widget_large_project_button, mainPendingIntent(context, CreatorWidgetContract.ACTION_NEW_PROJECT))
        views.setOnClickPendingIntent(R.id.widget_large_release, mainPendingIntent(context, CreatorWidgetContract.ACTION_RELEASE_DAY))
        return views
    }

    private fun activeTasks(tasks: List<CreatorTask>): List<CreatorTask> = tasks
        .filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
        .sortedWith(
            compareByDescending<CreatorTask> { it.status == TaskStatus.WORKING }
                .thenBy { it.dueAtMillis.takeIf { due -> due > 0L } ?: Long.MAX_VALUE },
        )

    private fun nextReminder(tasks: List<CreatorTask>): CreatorTask? {
        val now = System.currentTimeMillis()
        return tasks.asSequence()
            .filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
            .filter { it.reminderEnabled && it.reminderAtMillis > now }
            .minByOrNull { it.reminderAtMillis }
    }

    private fun mainPendingIntent(context: Context, action: String, taskId: String = ""): PendingIntent {
        val request = (action + taskId).hashCode()
        val intent = Intent(context, MainActivity::class.java)
            .setAction(action)
            .putExtra(CreatorWidgetContract.EXTRA_TASK_ID, taskId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, request, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun quickIdeaPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, QuickIdeaActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 99120, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun clock(millis: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

    private fun dueText(millis: Long): String {
        if (millis <= 0L) return "No deadline"
        val day = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(millis))
        return "$day · ${clock(millis)}"
    }

    private fun isToday(millis: Long): Boolean {
        if (millis <= 0L) return false
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
        return fmt.format(Date(millis)) == fmt.format(Date())
    }
}
