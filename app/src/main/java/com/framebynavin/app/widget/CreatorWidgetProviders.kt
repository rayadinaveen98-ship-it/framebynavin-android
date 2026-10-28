package com.framebynavin.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

abstract class BaseCreatorWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        widgetScope.launch {
            CreatorWidgetUpdater.updateAll(context, TaskStore(context.applicationContext).load())
        }
    }

    override fun onEnabled(context: Context) {
        widgetScope.launch {
            CreatorWidgetUpdater.updateAll(context, TaskStore(context.applicationContext).load())
        }
    }
}

/**
 * V224 isolated receiver lifetime. Android may kill the process as soon as a
 * BroadcastReceiver returns, so a detached launch is unsafe for new widgets.
 * goAsync holds a bounded PendingResult until the new widget render completes.
 * Legacy receiver wiring is deliberately not altered during migration.
 */
abstract class V224BaseWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        renderDuringBroadcast(context)
    }

    override fun onEnabled(context: Context) {
        renderDuringBroadcast(context)
    }

    private fun renderDuringBroadcast(context: Context) {
        val app = context.applicationContext
        val pending = goAsync()
        widgetScope.launch {
            try {
                withTimeout(9_000L) {
                    CreatorWidgetUpdater.updateAll(app, TaskStore(app).load())
                }
            } catch (error: Exception) {
                android.util.Log.w("BacklotWidgets", "V224 widget update interrupted; next refresh can retry", error)
            } finally {
                pending.finish()
            }
        }
    }
}

class V224QuickCaptureWidgetProvider : V224BaseWidgetProvider()
class V224TodayFocusWidgetProvider : V224BaseWidgetProvider()
class V224MyDayWidgetProvider : V224BaseWidgetProvider()

class CreatorCompactWidgetProvider : BaseCreatorWidgetProvider()
class CreatorLargeWidgetProvider : BaseCreatorWidgetProvider()
class QuickIdeaWidgetProvider : BaseCreatorWidgetProvider()
class NewProjectWidgetProvider : BaseCreatorWidgetProvider()
class CurrentProjectWidgetProvider : BaseCreatorWidgetProvider()
class NextReminderWidgetProvider : BaseCreatorWidgetProvider()
class ContentCalendarWidgetProvider : BaseCreatorWidgetProvider()
class DailyBriefWidgetProvider : BaseCreatorWidgetProvider()
class CreatorInsightsWidgetProvider : BaseCreatorWidgetProvider()
