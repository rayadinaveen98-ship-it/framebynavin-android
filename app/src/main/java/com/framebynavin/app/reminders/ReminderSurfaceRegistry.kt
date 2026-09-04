package com.framebynavin.app.reminders

import android.app.Activity
import java.lang.ref.WeakReference

/** Keeps Voice and Alarm full-screen surfaces mutually exclusive inside the app process. */
object ReminderSurfaceRegistry {
    @Volatile private var voiceActivity: WeakReference<Activity>? = null
    @Volatile private var alarmActivity: WeakReference<Activity>? = null

    fun attachVoice(activity: Activity) {
        voiceActivity = WeakReference(activity)
    }

    fun detachVoice(activity: Activity) {
        if (voiceActivity?.get() === activity) voiceActivity = null
    }

    fun attachAlarm(activity: Activity) {
        alarmActivity = WeakReference(activity)
    }

    fun detachAlarm(activity: Activity) {
        if (alarmActivity?.get() === activity) alarmActivity = null
    }

    fun closeVoice() = close(voiceActivity)

    fun closeAlarm() = close(alarmActivity)

    fun closeAll() {
        closeVoice()
        closeAlarm()
    }

    private fun close(reference: WeakReference<Activity>?) {
        val activity = reference?.get() ?: return
        activity.runOnUiThread {
            if (!activity.isFinishing && !activity.isDestroyed) activity.finishAndRemoveTask()
        }
    }
}
