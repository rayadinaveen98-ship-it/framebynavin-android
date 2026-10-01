package com.framebynavin.app.widget

object CreatorWidgetContract {
    const val ACTION_OPEN_TODAY = "com.framebynavin.app.widget.OPEN_TODAY"
    const val ACTION_OPEN_STUDIO = "com.framebynavin.app.widget.OPEN_STUDIO"
    const val ACTION_NEW_PROJECT = "com.framebynavin.app.widget.NEW_PROJECT"
    const val ACTION_RELEASE_DAY = "com.framebynavin.app.widget.RELEASE_DAY"
    const val ACTION_DAILY_BRIEF = "com.framebynavin.app.widget.DAILY_BRIEF"
    const val ACTION_CONTENT_CALENDAR = "com.framebynavin.app.widget.CONTENT_CALENDAR"
    const val ACTION_IDEA_VAULT = "com.framebynavin.app.widget.IDEA_VAULT"
    const val ACTION_OPEN_INSIGHTS = "com.framebynavin.app.widget.OPEN_INSIGHTS"
    const val ACTION_AUTOMATION_CENTER = "com.framebynavin.app.widget.AUTOMATION_CENTER"
    const val ACTION_OPEN_REMINDERS = "com.framebynavin.app.widget.OPEN_REMINDERS"
    const val EXTRA_TASK_ID = "widget_task_id"
    const val EXTRA_IDEA_ID = "widget_idea_id"
    const val EXTRA_IDEA_MODE = "widget_idea_mode"
    const val IDEA_MODE_OPEN = "open"
    const val IDEA_MODE_CONVERT = "convert"
}

data class CreatorWidgetLaunch(
    val action: String,
    val taskId: String = "",
    val ideaId: String = "",
    val ideaMode: String = "",
    val nonce: Long = System.nanoTime(),
)
