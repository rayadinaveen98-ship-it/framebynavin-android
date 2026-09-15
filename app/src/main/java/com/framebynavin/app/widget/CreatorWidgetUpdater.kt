package com.framebynavin.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.framebynavin.app.MainActivity
import com.framebynavin.app.R
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.QuickIdeaActivity
import com.framebynavin.app.ui.theme.FrameTheme
import com.framebynavin.app.ui.theme.VisualExperiencePrefs

object CreatorWidgetUpdater {
    fun updateAll(context: Context, tasks: List<CreatorTask>) {
        VisualExperiencePrefs.initialize(context.applicationContext)
        val manager = AppWidgetManager.getInstance(context)
        updateIds(context, manager, TodayWidgetProvider::class.java, tasks) { id -> todayViews(context, id, tasks) }
        updateIds(context, manager, QuickCaptureWidgetProvider::class.java, tasks) { id -> quickViews(context, id) }
        updateIds(context, manager, StudioWidgetProvider::class.java, tasks) { id -> studioViews(context, id, tasks) }
        updateIds(context, manager, ReleaseWidgetProvider::class.java, tasks) { id -> releaseViews(context, id, tasks) }
        updateIds(context, manager, CalendarWidgetProvider::class.java, tasks) { id -> actionViews(context, id, tasks, ActionWidget.CALENDAR) }
        updateIds(context, manager, DailyBriefWidgetProvider::class.java, tasks) { id -> actionViews(context, id, tasks, ActionWidget.DAILY_BRIEF) }
        updateIds(context, manager, InsightsWidgetProvider::class.java, tasks) { id -> actionViews(context, id, tasks, ActionWidget.INSIGHTS) }
    }

    private inline fun <reified T> updateIds(
        context: Context,
        manager: AppWidgetManager,
        clazz: Class<T>,
        tasks: List<CreatorTask>,
        views: (Int) -> RemoteViews,
    ) {
        manager.getAppWidgetIds(ComponentName(context, clazz)).forEach { manager.updateAppWidget(it, views(it)) }
    }

    private fun todayViews(context: Context, widgetId: Int, tasks: List<CreatorTask>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_today)
        applyTheme(
            views,
            R.id.widget_today_root,
            primaryIds = listOf(R.id.widget_today_kicker),
            foregroundIds = listOf(R.id.widget_today_title, R.id.widget_today_action),
            secondaryIds = listOf(R.id.widget_today_due),
            mutedIds = listOf(R.id.widget_today_subtitle),
        )
        val current = activeTasks(tasks).firstOrNull()
        views.setTextViewText(R.id.widget_today_kicker, "TODAY")
        views.setTextViewText(R.id.widget_today_title, current?.title ?: "The room is clear")
        views.setTextViewText(
            R.id.widget_today_subtitle,
            current?.let { "${it.status.name.lowercase().replaceFirstChar(Char::uppercase)} · ${it.platform.name.lowercase().replaceFirstChar(Char::uppercase)}" }
                ?: "Capture the next spark when it arrives.",
        )
        val dueToday = tasks.count { it.status != TaskStatus.DONE && it.status != TaskStatus.ARCHIVED && it.dueAtMillis in todayWindow() }
        views.setTextViewText(R.id.widget_today_due, if (dueToday == 0) "CLEAR TODAY" else "$dueToday DUE TODAY")
        views.setTextViewText(R.id.widget_today_action, if (current == null) "+ NEW PROJECT" else "OPEN BACKLOT")
        views.setOnClickPendingIntent(R.id.widget_today_root, mainPendingIntent(context, CreatorWidgetContract.ACTION_OPEN_TODAY))
        views.setOnClickPendingIntent(R.id.widget_today_action, mainPendingIntent(context, if (current == null) CreatorWidgetContract.ACTION_NEW_PROJECT else CreatorWidgetContract.ACTION_OPEN_TODAY))
        return views
    }

    private fun quickViews(context: Context, widgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_capture)
        applyTheme(
            views,
            R.id.widget_quick_root,
            primaryIds = listOf(R.id.widget_quick_kicker, R.id.widget_quick_plus),
            foregroundIds = listOf(R.id.widget_quick_title),
            secondaryIds = emptyList(),
            mutedIds = listOf(R.id.widget_quick_subtitle),
        )
        views.setOnClickPendingIntent(R.id.widget_quick_root, quickIdeaPendingIntent(context))
        views.setOnClickPendingIntent(R.id.widget_quick_plus, quickIdeaPendingIntent(context))
        return views
    }

    private fun studioViews(context: Context, widgetId: Int, tasks: List<CreatorTask>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_studio)
        applyTheme(
            views,
            R.id.widget_studio_root,
            primaryIds = listOf(R.id.widget_studio_kicker),
            foregroundIds = listOf(R.id.widget_studio_title),
            secondaryIds = listOf(R.id.widget_studio_stage),
            mutedIds = listOf(R.id.widget_studio_subtitle),
        )
        val current = activeTasks(tasks).firstOrNull()
        views.setTextViewText(R.id.widget_studio_title, current?.title ?: "No active project")
        views.setTextViewText(R.id.widget_studio_stage, current?.status?.name?.replace('_', ' ') ?: "READY")
        views.setTextViewText(R.id.widget_studio_subtitle, current?.let { "Open the creator studio and keep moving." } ?: "Start a project from your next idea.")
        views.setOnClickPendingIntent(R.id.widget_studio_root, mainPendingIntent(context, CreatorWidgetContract.ACTION_OPEN_STUDIO, current?.id.orEmpty()))
        return views
    }

    private fun releaseViews(context: Context, widgetId: Int, tasks: List<CreatorTask>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_release)
        applyTheme(
            views,
            R.id.widget_release_root,
            primaryIds = listOf(R.id.widget_release_kicker),
            foregroundIds = listOf(R.id.widget_release_title),
            secondaryIds = listOf(R.id.widget_release_action),
            mutedIds = listOf(R.id.widget_release_subtitle),
        )
        val ready = tasks.firstOrNull { it.status == TaskStatus.READY }
        views.setTextViewText(R.id.widget_release_title, ready?.title ?: "Nothing waiting to publish")
        views.setTextViewText(R.id.widget_release_subtitle, ready?.let { "Ready for your release checklist." } ?: "Projects marked Ready will appear here.")
        views.setTextViewText(R.id.widget_release_action, if (ready == null) "OPEN BACKLOT" else "RELEASE DAY")
        views.setOnClickPendingIntent(R.id.widget_release_root, mainPendingIntent(context, CreatorWidgetContract.ACTION_RELEASE_DAY, ready?.id.orEmpty()))
        return views
    }

    private enum class ActionWidget { CALENDAR, DAILY_BRIEF, INSIGHTS }

    private fun actionViews(context: Context, widgetId: Int, tasks: List<CreatorTask>, type: ActionWidget): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_action)
        applyTheme(
            views,
            R.id.widget_action_root,
            primaryIds = listOf(R.id.widget_action_kicker),
            foregroundIds = listOf(R.id.widget_action_title),
            secondaryIds = emptyList(),
            mutedIds = listOf(R.id.widget_action_subtitle),
        )
        val todayCount = tasks.count { it.status != TaskStatus.DONE && it.status != TaskStatus.ARCHIVED && it.dueAtMillis in todayWindow() }
        when (type) {
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
        FrameTheme.LUMEN_FLOW -> R.drawable.widget_bg_lumen
        FrameTheme.AURORA_GLASS -> R.drawable.widget_bg_aurora
        FrameTheme.PAPER_QUIET -> R.drawable.widget_bg_paper
        FrameTheme.MOSS_STUDIO -> R.drawable.widget_bg_moss
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

    private fun todayWindow(): LongRange {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return start until calendar.timeInMillis
    }
}
