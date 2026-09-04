package com.framebynavin.app.widget

object CreatorWidgetContract {
    const val ACTION_OPEN_TODAY = "com.framebynavin.app.widget.OPEN_TODAY"
    const val ACTION_OPEN_STUDIO = "com.framebynavin.app.widget.OPEN_STUDIO"
    const val ACTION_NEW_PROJECT = "com.framebynavin.app.widget.NEW_PROJECT"
    const val ACTION_RELEASE_DAY = "com.framebynavin.app.widget.RELEASE_DAY"
    const val EXTRA_TASK_ID = "widget_task_id"
}

data class CreatorWidgetLaunch(
    val action: String,
    val taskId: String = "",
    val nonce: Long = System.nanoTime(),
)
