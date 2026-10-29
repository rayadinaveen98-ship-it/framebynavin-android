package com.framebynavin.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * Read-only device widget inventory. No removal, disableComponent(), migration
 * of user preferences, or surprise write to Creator/Personal storage.
 */
internal enum class V2273WidgetKind(val label: String, val summary: String) {
    QUICK_CAPTURE("Quick Capture", "Capture ideas and create projects"),
    TODAY_FOCUS("Today Focus", "See current project priorities and reminders"),
    MY_DAY("My Day", "View Creator and Personal reminder counts privately"),
}

internal data class V2273WidgetInventory(
    val newPinned: Map<V2273WidgetKind, Int>,
    val legacyPinned: Map<String, Int>,
) {
    val newTotal: Int get() = newPinned.values.sum()
    val oldTotal: Int get() = legacyPinned.values.sum()
    val hasLegacyPinned: Boolean get() = oldTotal > 0
}

internal object V2273WidgetMigrationPolicy {
    /**
     * A successful pin *request* is not the same as successful launcher
     * placement. The launcher confirmation happens outside Backlot.
     */
    fun pinMessage(requestAccepted: Boolean): String =
        if (requestAccepted)
            "Pin request sent. Approve it in your launcher, then come back and refresh the counts."
        else "This launcher could not open a pin request. Long-press Home, choose Widgets, then Backlot."

    fun migrationMessage(newPinned: Int, oldPinned: Int): String = when {
        oldPinned == 0 && newPinned > 0 ->
            "No older widgets are pinned. You can keep using the new Backlot widgets."
        oldPinned == 0 ->
            "No older widgets detected. Add one of the three new widgets when you're ready."
        newPinned == 0 ->
            "Older widgets are still on your Home screen. Add a new widget before removing them."
        else ->
            "Both generations are pinned. Check the new widgets, then long-press each old widget to remove it from Home."
    }
}

internal object V2273WidgetInventoryReader {
    fun snapshot(context: Context): V2273WidgetInventory {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        fun ids(provider: Class<*>): Int =
            manager.getAppWidgetIds(ComponentName(app, provider)).size

        return V2273WidgetInventory(
            newPinned = linkedMapOf(
                V2273WidgetKind.QUICK_CAPTURE to ids(V224QuickCaptureWidgetProvider::class.java),
                V2273WidgetKind.TODAY_FOCUS to ids(V224TodayFocusWidgetProvider::class.java),
                V2273WidgetKind.MY_DAY to ids(V224MyDayWidgetProvider::class.java),
            ),
            legacyPinned = linkedMapOf(
                "Compact" to ids(CreatorCompactWidgetProvider::class.java),
                "Creator Desk" to ids(CreatorLargeWidgetProvider::class.java),
                "Quick Idea" to ids(QuickIdeaWidgetProvider::class.java),
                "New Project" to ids(NewProjectWidgetProvider::class.java),
                "Current Project" to ids(CurrentProjectWidgetProvider::class.java),
                "Next Reminder" to ids(NextReminderWidgetProvider::class.java),
                "Content Calendar" to ids(ContentCalendarWidgetProvider::class.java),
                "Daily Brief" to ids(DailyBriefWidgetProvider::class.java),
                "Creator Insights" to ids(CreatorInsightsWidgetProvider::class.java),
            ),
        )
    }

    fun provider(kind: V2273WidgetKind): Class<*> = when (kind) {
        V2273WidgetKind.QUICK_CAPTURE -> V224QuickCaptureWidgetProvider::class.java
        V2273WidgetKind.TODAY_FOCUS -> V224TodayFocusWidgetProvider::class.java
        V2273WidgetKind.MY_DAY -> V224MyDayWidgetProvider::class.java
    }
}
