package com.framebynavin.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.framebynavin.app.MainActivity
import com.framebynavin.app.R
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.personal.PersonalDeliveryContract
import com.framebynavin.app.personal.PersonalReminderStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * V224 widget trilogy. Read-only widgets are launcher surfaces, not reminder
 * action receivers: all state-changing actions open an app screen for user input.
 * In particular, no personal title or notes are put into RemoteViews.
 */
internal object V224WidgetUpdater {
    suspend fun update(context: Context, tasks: List<CreatorTask>) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val capture = ids<V224QuickCaptureWidgetProvider>(app, manager)
        val today = ids<V224TodayFocusWidgetProvider>(app, manager)
        val myDay = ids<V224MyDayWidgetProvider>(app, manager)
        if (capture.isEmpty() && today.isEmpty() && myDay.isEmpty()) return

        capture.forEach { id ->
            manager.updateAppWidget(id, captureViews(app, compact(app, manager, id)))
        }
        if (today.isNotEmpty()) {
            today.forEach { id ->
                manager.updateAppWidget(id, todayViews(app, tasks, compact(app, manager, id)))
            }
        }
        if (myDay.isNotEmpty()) {
            // Corrupted / temporarily unreadable Personal DataStore must never
            // be presented as "zero reminders" or overwritten by a widget.
            val personal = runCatching {
                V224WidgetPolicy.personalSummary(
                    PersonalReminderStore(app).load(), System.currentTimeMillis(),
                )
            }.getOrNull()
            myDay.forEach { id ->
                manager.updateAppWidget(id, myDayViews(app, tasks, personal, compact(app, manager, id)))
            }
        }
    }

    /**
     * Notify the pinned My Day widget without performing TaskStore disk I/O
     * inside a Personal alarm broadcast or save path. BaseCreatorWidgetProvider
     * performs the actual read/render on its independent IO coroutine.
     * Explicit same-package broadcast; no external data or permissions.
     */
    fun refresh(context: Context) {
        runCatching {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val component = ComponentName(app, V224MyDayWidgetProvider::class.java)
            val pinned = manager.getAppWidgetIds(component)
            if (pinned.isNotEmpty()) {
                app.sendBroadcast(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .setComponent(component)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, pinned),
                )
            }
        }.onFailure {
            android.util.Log.w("BacklotWidgets", "V224 widget refresh unavailable", it)
        }
    }

    private inline fun <reified T> ids(context: Context, manager: AppWidgetManager): IntArray =
        manager.getAppWidgetIds(ComponentName(context, T::class.java))

    private fun compact(context: Context, manager: AppWidgetManager, id: Int): Boolean {
        val options = manager.getAppWidgetOptions(id)
        return V2241WidgetSizingPolicy.compact(
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0),
            context.resources.configuration.fontScale,
        )
    }

    private fun captureViews(context: Context, compact: Boolean): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_v224_capture).apply {
            if (compact) {
                setViewVisibility(R.id.v224_capture_prompt, View.GONE)
                setTextViewTextSize(R.id.v224_capture_idea, TypedValue.COMPLEX_UNIT_SP, 10f)
                setTextViewTextSize(R.id.v224_capture_project, TypedValue.COMPLEX_UNIT_SP, 10f)
            }
            setOnClickPendingIntent(R.id.v224_capture_idea, ideaIntent(context))
            setOnClickPendingIntent(
                R.id.v224_capture_project,
                launchIntent(context, CreatorWidgetContract.ACTION_NEW_PROJECT),
            )
        }

    private fun todayViews(context: Context, tasks: List<CreatorTask>, compact: Boolean): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_v224_today)
        val active = tasks.filter { it.status == TaskStatus.WORKING || it.status == TaskStatus.PLANNED }
        val current = active.sortedWith(
            compareByDescending<CreatorTask> { it.status == TaskStatus.WORKING }
                .thenBy { it.dueAtMillis.takeIf { x -> x > 0L } ?: Long.MAX_VALUE },
        ).firstOrNull()
        val now = System.currentTimeMillis()
        val dueToday = active.count { item ->
            item.dueAtMillis > 0L &&
                Instant.ofEpochMilli(item.dueAtMillis).atZone(ZoneId.systemDefault()).toLocalDate() ==
                LocalDate.now()
        }
        val next = active.filter { it.reminderEnabled && it.reminderAtMillis > now }
            .minByOrNull { it.reminderAtMillis }
        views.setTextViewText(R.id.v224_today_due, "$dueToday DUE TODAY")
        views.setTextViewText(R.id.v224_today_title, current?.title ?: "Your creator desk is clear")
        if (compact) {
            views.setViewVisibility(R.id.v224_today_detail, View.GONE)
            views.setViewVisibility(R.id.v224_today_reminder, View.GONE)
            views.setTextViewTextSize(R.id.v224_today_title, TypedValue.COMPLEX_UNIT_SP, 15f)
            views.setTextViewTextSize(R.id.v224_today_open, TypedValue.COMPLEX_UNIT_SP, 9f)
            views.setTextViewTextSize(R.id.v224_today_reminders, TypedValue.COMPLEX_UNIT_SP, 9f)
        }
        views.setTextViewText(
            R.id.v224_today_detail,
            if (current == null) "Capture an idea to begin"
            else "${active.size} active · ${if (current.status == TaskStatus.WORKING) "In progress" else "Planned"}",
        )
        views.setTextViewText(
            R.id.v224_today_reminder,
            next?.let { "Next creator reminder · ${clock(it.reminderAtMillis)}" }
                ?: "No upcoming creator reminders",
        )
        views.setOnClickPendingIntent(
            R.id.v224_today_open, launchIntent(context, CreatorWidgetContract.ACTION_OPEN_TODAY),
        )
        views.setOnClickPendingIntent(
            R.id.v224_today_reminders,
            launchIntent(context, CreatorWidgetContract.ACTION_OPEN_REMINDERS),
        )
        return views
    }

    private fun myDayViews(
        context: Context,
        tasks: List<CreatorTask>,
        personal: V224PrivateDaySummary?,
        compact: Boolean,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_v224_myday)
        val now = System.currentTimeMillis()
        val endToday = V224WidgetPolicy.tomorrowStartMillis(now, ZoneId.systemDefault())
        val creatorTimes = tasks.asSequence()
            .filter { it.status == TaskStatus.WORKING || it.status == TaskStatus.PLANNED }
            .filter { it.reminderEnabled && it.reminderAtMillis > now && it.reminderAtMillis < endToday }
            .map { it.reminderAtMillis }.toList()
        val creatorText = if (creatorTimes.isEmpty()) "CREATOR\nNone left today"
            else "CREATOR\n${creatorTimes.size} today" +
                (if (compact) "" else "\nNext ${clock(creatorTimes.min())}")
        val personalText = V224WidgetPolicy.personalSafeLabel(personal) +
            (if (compact) "" else personal?.nextAtMillis?.let { "\nNext ${clock(it)}" }.orEmpty())
        if (compact) {
            views.setViewVisibility(R.id.v224_day_headline, View.GONE)
            views.setTextViewTextSize(R.id.v224_day_creator, TypedValue.COMPLEX_UNIT_SP, 11f)
            views.setTextViewTextSize(R.id.v224_day_personal, TypedValue.COMPLEX_UNIT_SP, 11f)
            views.setTextViewTextSize(R.id.v224_day_open_creator, TypedValue.COMPLEX_UNIT_SP, 9f)
            views.setTextViewTextSize(R.id.v224_day_open_personal, TypedValue.COMPLEX_UNIT_SP, 9f)
        }
        views.setTextViewText(R.id.v224_day_creator, creatorText)
        views.setContentDescription(R.id.v224_day_creator, creatorText.replace("\n", ". "))
        views.setTextViewText(R.id.v224_day_personal, personalText)
        views.setContentDescription(R.id.v224_day_personal, personalText.replace("\n", ". "))
        views.setOnClickPendingIntent(
            R.id.v224_day_open_creator,
            launchIntent(context, CreatorWidgetContract.ACTION_OPEN_REMINDERS),
        )
        views.setOnClickPendingIntent(
            R.id.v224_day_open_personal,
            launchIntent(context, PersonalDeliveryContract.ACTION_OPEN_PERSONAL),
        )
        return views
    }

    private fun ideaIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 22401,
            Intent(context, QuickIdeaActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun launchIntent(context: Context, action: String): PendingIntent =
        PendingIntent.getActivity(
            context, 22402 + action.hashCode(),
            Intent(context, MainActivity::class.java)
                .setAction(action)
                .setData(Uri.parse("backlot-widget://v224/${Uri.encode(action)}"))
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun clock(epoch: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epoch))
}
