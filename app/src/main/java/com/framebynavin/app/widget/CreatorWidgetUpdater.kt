package com.framebynavin.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
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
        val app = context.applicationContext
        VisualExperiencePrefs.initialize(app)
        val manager = AppWidgetManager.getInstance(app)

        ids<CreatorCompactWidgetProvider>(app, manager).forEach { manager.updateAppWidget(it, compactViews(app, tasks)) }
        ids<CreatorLargeWidgetProvider>(app, manager).forEach { manager.updateAppWidget(it, largeViews(app, tasks)) }
        ids<QuickIdeaWidgetProvider>(app, manager).forEach { manager.updateAppWidget(it, actionViews(app, ActionWidget.QUICK_IDEA, tasks)) }
        ids<NewProjectWidgetProvider>(app, manager).forEach { manager.updateAppWidget(it, actionViews(app, ActionWidget.NEW_PROJECT, tasks)) }
        ids<CurrentProjectWidgetProvider>(app, manager).forEach { manager.updateAppWidget(it, actionViews(app, ActionWidget.CURRENT_PROJECT, tasks)) }
        ids<NextReminderWidgetProvider>(app, manager).forEach { manager.updateAppWidget(it, actionViews(app, ActionWidget.NEXT_REMINDER, tasks)) }
        ids<ContentCalendarWidgetProvider>(app, manager).forEach { manager.updateAppWidget(it, actionViews(app, ActionWidget.CALENDAR, tasks)) }
        ids<DailyBriefWidgetProvider>(app, manager).forEach { manager.updateAppWidget(it, actionViews(app, ActionWidget.DAILY_BRIEF, tasks)) }
        ids<CreatorInsightsWidgetProvider>(app, manager).forEach { manager.updateAppWidget(it, actionViews(app, ActionWidget.INSIGHTS, tasks)) }
    }

    private inline fun <reified T> ids(context: Context, manager: AppWidgetManager): IntArray =
        manager.getAppWidgetIds(ComponentName(context, T::class.java))

    private fun compactViews(context: Context, tasks: List<CreatorTask>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_creator_compact)
        val active = activeTasks(tasks)
        val current = active.firstOrNull()
        val nextReminder = nextReminder(tasks)
        applyTheme(views, R.id.widget_root, listOf(R.id.widget_title), listOf(R.id.widget_project), listOf(R.id.widget_stage), listOf(R.id.widget_due, R.id.widget_reminder))

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
        applyTheme(
            views,
            R.id.widget_large_root,
            listOf(R.id.widget_large_title),
            listOf(R.id.widget_large_project),
            listOf(R.id.widget_large_today_count, R.id.widget_large_progress),
            listOf(R.id.widget_large_stage, R.id.widget_large_reminder),
        )

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
        return views
    }

    private enum class ActionWidget { QUICK_IDEA, NEW_PROJECT, CURRENT_PROJECT, NEXT_REMINDER, CALENDAR, DAILY_BRIEF, INSIGHTS }

    private fun actionViews(context: Context, type: ActionWidget, tasks: List<CreatorTask>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_creator_action)
        val current = activeTasks(tasks).firstOrNull()
        val reminder = nextReminder(tasks)
        val todayCount = activeTasks(tasks).count { isToday(it.dueAtMillis) }
        val palette = VisualExperiencePrefs.palette
        views.setInt(R.id.widget_action_root, "setBackgroundResource", themeBackground())
        views.setTextColor(R.id.widget_action_kicker, palette.secondary.toArgb())
        views.setTextColor(R.id.widget_action_title, palette.foreground.toArgb())
        views.setTextColor(R.id.widget_action_subtitle, palette.muted.toArgb())

        when (type) {
            ActionWidget.QUICK_IDEA -> {
                views.setTextViewText(R.id.widget_action_kicker, "QUICK IDEA")
                views.setTextViewText(R.id.widget_action_title, "Capture a thought")
                views.setTextViewText(R.id.widget_action_subtitle, "Text or voice · before it disappears")
                views.setOnClickPendingIntent(R.id.widget_action_root, quickIdeaPendingIntent(context))
            }
            ActionWidget.NEW_PROJECT -> {
                views.setTextViewText(R.id.widget_action_kicker, "NEW PROJECT")
                views.setTextViewText(R.id.widget_action_title, "Start creating")
                views.setTextViewText(R.id.widget_action_subtitle, "Open the complete project setup")
                views.setOnClickPendingIntent(R.id.widget_action_root, mainPendingIntent(context, CreatorWidgetContract.ACTION_NEW_PROJECT))
            }
            ActionWidget.CURRENT_PROJECT -> {
                views.setTextViewText(R.id.widget_action_kicker, "CURRENT PROJECT")
                if (current == null) {
                    views.setTextViewText(R.id.widget_action_title, "No active project")
                    views.setTextViewText(R.id.widget_action_subtitle, "Tap to plan your next piece")
                    views.setOnClickPendingIntent(R.id.widget_action_root, mainPendingIntent(context, CreatorWidgetContract.ACTION_NEW_PROJECT))
                } else {
                    views.setTextViewText(R.id.widget_action_title, current.title)
                    views.setTextViewText(R.id.widget_action_subtitle, "${CreatorWorkflowEngine.currentStage(current).label} · ${CreatorWorkflowEngine.progress(current)}% · ${dueText(current.dueAtMillis)}")
                    views.setOnClickPendingIntent(R.id.widget_action_root, mainPendingIntent(context, CreatorWidgetContract.ACTION_OPEN_STUDIO, current.id))
                }
            }
            ActionWidget.NEXT_REMINDER -> {
                views.setTextViewText(R.id.widget_action_kicker, "NEXT REMINDER")
                if (reminder == null) {
                    views.setTextViewText(R.id.widget_action_title, "Nothing queued")
                    views.setTextViewText(R.id.widget_action_subtitle, "Tap to open Reminder Center")
                } else {
                    views.setTextViewText(R.id.widget_action_title, clock(reminder.reminderAtMillis))
                    views.setTextViewText(R.id.widget_action_subtitle, reminder.title)
                }
                views.setOnClickPendingIntent(R.id.widget_action_root, mainPendingIntent(context, CreatorWidgetContract.ACTION_OPEN_REMINDERS))
            }
            ActionWidget.CALENDAR -> {
                views.setTextViewText(R.id.widget_action_kicker, "CONTENT CALENDAR")
                views.setTextViewText(R.id.widget_action_title, if (todayCount == 0) "Schedule is clear" else "$todayCount due today")
                views.setTextViewText(R.id.widget_action_subtitle, "Open your publishing calendar")
                views.setOnClickPendingIntent(R.id.widget_action_root, mainPendingIntent(context, CreatorWidgetContract.ACTION_CONTENT_CALENDAR))
            }
            ActionWidget.DAILY_BRIEF -> {
                views.setTextViewText(R.id.widget_action_kicker, "DAILY BRIEF")
                views.setTextViewText(R.id.widget_action_title, "What matters now")
                views.setTextViewText(R.id.widget_action_subtitle, "Projects · reminders · creator context")
                views.setOnClickPendingIntent(R.id.widget_action_root, mainPendingIntent(context, CreatorWidgetContract.ACTION_DAILY_BRIEF))
            }
            ActionWidget.INSIGHTS -> {
                views.setTextViewText(R.id.widget_action_kicker, "CREATOR INSIGHTS")
                views.setTextViewText(R.id.widget_action_title, "See the pattern")
                views.setTextViewText(R.id.widget_action_subtitle, "Progress · evidence · next moves")
                views.setOnClickPendingIntent(R.id.widget_action_root, mainPendingIntent(context, CreatorWidgetContract.ACTION_OPEN_INSIGHTS))
            }
        }
        return views
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
