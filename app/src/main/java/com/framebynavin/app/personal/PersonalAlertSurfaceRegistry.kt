package com.framebynavin.app.personal

import java.lang.ref.WeakReference

/**
 * Presentation is not completion. Close only the same on-screen occurrence
 * after its audible playback ends; the actionable notification remains.
 */
internal object PersonalAlertSurfaceRegistry {
    private data class Entry(
        val activity: WeakReference<PersonalAlertActivity>,
        val id: String,
        val token: String,
    )
    private val entries = mutableListOf<Entry>()

    @Synchronized fun attach(activity: PersonalAlertActivity, id: String, token: String) {
        entries.removeAll { it.activity.get() == null || it.activity.get() === activity }
        entries.add(Entry(WeakReference(activity), id, token))
    }

    @Synchronized fun detach(activity: PersonalAlertActivity) {
        entries.removeAll { it.activity.get() == null || it.activity.get() === activity }
    }

    fun closeAfterPlayback(id: String, token: String) {
        if (id.isBlank() || token.isBlank()) return
        val matching = synchronized(this) {
            entries.removeAll { it.activity.get() == null }
            entries.filter { it.id == id && it.token == token }
                .mapNotNull { it.activity.get() }
        }
        matching.forEach { activity ->
            activity.runOnUiThread {
                if (!activity.isFinishing && !activity.isDestroyed) activity.finishAndRemoveTask()
            }
        }
    }
}
