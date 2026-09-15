package com.framebynavin.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.toColorInt
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.toArgb
import com.framebynavin.app.MainActivity
import com.framebynavin.app.R
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.FrameTheme
import com.framebynavin.app.ui.theme.VisualExperiencePrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CreatorWidgetUpdater {
    suspend fun updateAll(context: Context, tasks: List<CreatorTask>) {
        VisualExperiencePrefs.initialize(context.applicationContext)
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val compactIds = manager.getAppWidgetIds(ComponentName(app, CreatorCompactWidgetProvider::class.java))
        val largeIds = manager.getAppWidgetIds(ComponentName(app, CreatorLargeWidgetProvider::class.java))
        val quickIdeaIds = manager.getAppWidgetIds(ComponentName(app, QuickIdeaWidgetProvider::class.java))
        val newProjectIds = manager.getAppWidgetIds(ComponentName(app, NewProjectWidgetProvider::class.java))
        val currentProjectIds = manager.getAppWidgetIds(ComponentName(app, CurrentProjectWidgetProvider::class.java))
        val nextReminderIds = manager.getAppWidgetIds(ComponentName(app, NextReminderWidgetProvider::class.java))
        val calendarIds = manager.getAppWidgetIds(ComponentName(app, ContentCalendarWidgetProvider::class.java))
        val briefIds = manager.getAppWidgetIds(ComponentName(app, DailyBriefWidgetProvider::class.java))
        val insightsIds = manager.getAppWidgetIds(ComponentName(app, CreatorInsightsWidgetProvider::class.java))
        compactIds.forEach { manager.updateAppWidget(it, compactViews(app, tasks)) }
        largeIds.forEach { manager.updateAppWidget(it, largeViews(app, tasks)) }
        quickIdeaIds.forEach { manager.updateAppWidget(it, actionViews(app, "QUICK IDEA", "Capture before it disappears", CreatorWidgetContract.ACTION_IDEA_VAULT, R.drawable.widget_icon_idea)) }
        newProjectIds.forEach { manager.updateAppWidget(it, actionViews(app, "NEW PROJECT", "Start the next piece", CreatorWidgetContract.ACTION_NEW_PROJECT, R.drawable.widget_icon_project)) }
        currentProjectIds.forEach { manager.updateAppWidget(it, currentProjectViews(app, tasks)) }
        nextReminderIds.forEach { manager.updateAppWidget(it, nextReminderViews(app, tasks)) }
        calendarIds.forEach { manager.updateAppWidget(it, actionViews(app, "CONTENT CALENDAR", "Plan what ships next", CreatorWidgetContract.ACTION_CONTENT_CALENDAR, R.drawable.widget_icon_calendar)) }
        briefIds.forEach { manager.updateAppWidget(it, actionViews(app, "DAILY BRIEF", "What needs attention today", CreatorWidgetContract.ACTION_DAILY_BRIEF, R.drawable.widget_icon_brief)) }
        insightsIds.forEach { manager.updateAppWidget(it, insightsViews(app, tasks)) }
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
        applyTheme(
            views,
            rootId = R.id.widget_root,
            primaryIds = listOf(R.id.widget_title),
            foregroundIds = listOf(R.id.widget_project),
            secondaryIds = listOf(R.id.widget_stage),
            mutedIds = listOf(R.id.widget_due, R.id.widget_reminder),
        )
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
        views.setOnClickPendingIntent(R.id.widget_large_brief, mainPendingIntent(context, CreatorWidgetContract.ACTION_DAILY_BRIEF))
        views.setOnClickPendingIntent(R.id.widget_large_calendar, mainPendingIntent(context, CreatorWidgetContract.ACTION_CONTENT_CALENDAR))
        applyTheme(
            views,
            rootId = R.id.widget_large_root,
            primaryIds = listOf(R.id.widget_large_title),
            foregroundIds = listOf(R.id.widget_large_project),
            secondaryIds = listOf(R.id.widget_large_today_count, R.id.widget_large_progress),
            mutedIds = listOf(R.id.widget_large_stage),
        )
        return views
    }

    private fun actionViews(context: Context, title: String, subtitle: String, action: String, iconRes: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_creator_action)
        views.setTextViewText(R.id.widget_action_title, title)
        views.setTextViewText(R.id.widget_action_subtitle, subtitle)
        views.setImageViewResource(R.id.widget_action_icon, iconRes)
        views.setOnClickPendingIntent(R.id.widget_action_root, mainPendingIntent(context, action))
        applyTheme(
            views,
            rootId = R.id.widget_action_root,
            primaryIds = listOf(R.id.widget_action_title),
            foregroundIds = emptyList(),
            secondaryIds = emptyList(),
            mutedIds = listOf(R.id.widget_action_subtitle),
        )
        return views
    }

    private fun currentProjectViews(context: Context, tasks: List<CreatorTask>): RemoteViews {
        val current = activeTasks(tasks).firstOrNull()
        val title = current?.title ?: "No active project"
        val subtitle = current?.let { "${CreatorWorkflowEngine.currentStage(it).label} · ${CreatorWorkflowEngine.progress(it)}%" } ?: "Start a project when you're ready"
        val action = if (current == null) CreatorWidgetContract.ACTION_NEW_PROJECT else CreatorWidgetContract.ACTION_OPEN_STUDIO
        val views = actionViews(context, title, subtitle, action, R.drawable.widget_icon_project)
        if (current != null) views.setOnClickPendingIntent(R.id.widget_action_root, mainPendingIntent(context, action, current.id))
        return views
    }

    private fun nextReminderViews(context: Context, tasks: List<CreatorTask>): RemoteViews {
        val reminder = nextReminder(tasks)
        val title = reminder?.let { clock(it.reminderAtMillis) } ?: "No reminder queued"
        val subtitle = reminder?.title ?: "Open reminders to plan one"
        return actionViews(context, title, subtitle, CreatorWidgetContract.ACTION_OPEN_REMINDERS, R.drawable.widget_icon_reminder)
    }

    private fun insightsViews(context: Context, tasks: List<CreatorTask>): RemoteViews {
        val done = tasks.count { it.status == TaskStatus.DONE }
        val active = activeTasks(tasks).size
        return actionViews(context, "CREATOR PROGRESS", "$done finished · $active active", CreatorWidgetContract.ACTION_OPEN_INSIGHTS, R.drawable.widget_icon_insights)
    }

    private fun applyTheme(
        views: RemoteViews,
        rootId: Int,
        primaryIds: List<Int>,
        foregroundIds: List<Int>,
        secondaryIds: List<Int>,
        mutedIds: List<Int>,
    ) {
        val palette = VisualExperiencePrefs.palette
        views.setInt(rootId, "setBackgroundResource", themeBackground())
        primaryIds.forEach { views.setTextColor(it, palette.primary.toArgb()) }
        foregroundIds.forEach { views.setTextColor(it, palette.foreground.toArgb()) }
        secondaryIds.forEach { views.setTextColor(it, palette.secondary.toArgb()) }
        mutedIds.forEach { views.setTextColor(it, palette.muted.toArgb()) }
    }

    private fun themeBackground(): Int = when (VisualExperiencePrefs.currentTheme) {
        FrameTheme.DIRECTORS_CUT -> R.drawable.widget_bg_directors
        FrameTheme.MIDNIGHT -> R.drawable.widget_bg_midnight
        FrameTheme.EMBER -> R.drawable.widget_bg_ember
        FrameTheme.VIOLET_NEON -> R.drawable.widget_bg_violet
        FrameTheme.IVORY_STUDIO -> R.drawable.widget_bg_ivory
        FrameTheme.AURORA_GLASS -> R.drawable.widget_bg_aurora
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
