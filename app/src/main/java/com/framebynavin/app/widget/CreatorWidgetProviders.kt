package com.framebynavin.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

/** V224 new picker entries; legacy providers remain until owner migration approval. */
class V224QuickCaptureWidgetProvider : BaseCreatorWidgetProvider()
class V224TodayFocusWidgetProvider : BaseCreatorWidgetProvider()
class V224MyDayWidgetProvider : BaseCreatorWidgetProvider()

class CreatorCompactWidgetProvider : BaseCreatorWidgetProvider()
class CreatorLargeWidgetProvider : BaseCreatorWidgetProvider()
class QuickIdeaWidgetProvider : BaseCreatorWidgetProvider()
class NewProjectWidgetProvider : BaseCreatorWidgetProvider()
class CurrentProjectWidgetProvider : BaseCreatorWidgetProvider()
class NextReminderWidgetProvider : BaseCreatorWidgetProvider()
class ContentCalendarWidgetProvider : BaseCreatorWidgetProvider()
class DailyBriefWidgetProvider : BaseCreatorWidgetProvider()
class CreatorInsightsWidgetProvider : BaseCreatorWidgetProvider()
