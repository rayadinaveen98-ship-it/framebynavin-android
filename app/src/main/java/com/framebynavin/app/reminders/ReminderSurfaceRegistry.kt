package com.framebynavin.app.reminders

import android.app.Activity
import java.lang.ref.WeakReference

/** Multiple tasks may have live surfaces; only the requested occurrence is closed. */
object ReminderSurfaceRegistry {
    private data class Entry(val activity: WeakReference<Activity>, val taskId: String, val token: String, val voice: Boolean)
    private val entries = mutableListOf<Entry>()

    @Synchronized private fun attach(activity: Activity, taskId: String, token: String, voice: Boolean) {
        entries.removeAll { it.activity.get() == null || it.activity.get() === activity }
        entries.add(Entry(WeakReference(activity), taskId, token, voice))
    }
    fun attachVoice(activity: Activity, taskId: String, token: String) = attach(activity, taskId, token, true)
    fun attachAlarm(activity: Activity, taskId: String, token: String) = attach(activity, taskId, token, false)
    @Synchronized fun detachVoice(activity: Activity) { entries.removeAll { it.activity.get() == null || it.activity.get() === activity } }
    @Synchronized fun detachAlarm(activity: Activity) { entries.removeAll { it.activity.get() == null || it.activity.get() === activity } }

    fun close(taskId: String, token: String) = closeMatching { it.taskId == taskId && it.token == token }
    fun closeTask(taskId: String) = closeMatching { it.taskId == taskId }
    fun closeVoice() = closeMatching { it.voice }
    fun closeAlarm() = closeMatching { !it.voice }
    fun closeAll() = closeMatching { true }

    private fun closeMatching(predicate: (Entry) -> Boolean) {
        val targets = synchronized(this) {
            entries.removeAll { it.activity.get() == null }
            entries.filter(predicate).mapNotNull { it.activity.get() }
        }
        targets.forEach { activity ->
            activity.runOnUiThread {
                if (!activity.isFinishing && !activity.isDestroyed) activity.finishAndRemoveTask()
            }
        }
    }
}
