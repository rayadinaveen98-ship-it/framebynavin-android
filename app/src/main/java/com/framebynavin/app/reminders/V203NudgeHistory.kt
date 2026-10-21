package com.framebynavin.app.reminders

import android.content.SharedPreferences
import java.security.MessageDigest

/**
 * On-device, bounded notification history. The last-key fields are preserved
 * for V201 upgrade compatibility. No project text/IDs are persisted as keys.
 */
internal object V203NudgeHistory {
    private const val PREFIX = "key_v203_"
    private const val RETENTION_MS = 7L * 24L * 60L * 60L * 1000L

    fun storageKey(nudgeKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(nudgeKey.toByteArray(Charsets.UTF_8))
        return PREFIX + digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    fun lastShown(prefs: SharedPreferences, key: String): Long =
        prefs.getLong(storageKey(key), 0L)

    /** A durable write after handing the notification to Android. */
    fun recordShown(prefs: SharedPreferences, key: String, nowMillis: Long): Boolean {
        val editor = prefs.edit()
            .putString("last_key", key)
            .putLong("last_at", nowMillis)
            .putLong(storageKey(key), nowMillis)

        // At most a small local history remains, even if creators run for years.
        for ((savedKey, value) in prefs.all) {
            if (savedKey.startsWith(PREFIX) && value is Long &&
                value > 0L && nowMillis >= value &&
                nowMillis - value > RETENTION_MS
            ) editor.remove(savedKey)
        }
        return editor.commit()
    }
}
