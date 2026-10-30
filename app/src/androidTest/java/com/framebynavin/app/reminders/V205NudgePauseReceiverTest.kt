package com.framebynavin.app.reminders

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs only in a disposable CI emulator; never alters production reminders. */
@RunWith(AndroidJUnit4::class)
class V205NudgePauseReceiverTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun explicitNotificationActionPausesOnlyOptionalNudges() {
        val prefs = context.getSharedPreferences(V205NudgePausePolicy.PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getLong(V205NudgePausePolicy.KEY_PAUSED_UNTIL, 0L)
        val sentinel = "v205-test-unrelated-reminder"
        try {
            assertTrue(prefs.edit().putLong(V205NudgePausePolicy.KEY_PAUSED_UNTIL, 0L).putString(sentinel, "keep").commit())
            V205NudgePauseReceiver().onReceive(
                context,
                Intent(context, V205NudgePauseReceiver::class.java).apply {
                    action = V205NudgePausePolicy.ACTION_PAUSE_WEEK
                },
            )
            val now = System.currentTimeMillis()
            val until = prefs.getLong(V205NudgePausePolicy.KEY_PAUSED_UNTIL, 0L)
            assertTrue(until > now + 6L * 24L * 60L * 60L * 1000L)
            assertTrue(until <= now + 7L * 24L * 60L * 60L * 1000L)
            assertEquals("keep", prefs.getString(sentinel, null))
        } finally {
            prefs.edit().putLong(V205NudgePausePolicy.KEY_PAUSED_UNTIL, previous).remove(sentinel).commit()
        }
    }

    @Test fun unrelatedBroadcastDoesNotPauseNudges() {
        val prefs = context.getSharedPreferences(V205NudgePausePolicy.PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getLong(V205NudgePausePolicy.KEY_PAUSED_UNTIL, 0L)
        try {
            assertTrue(prefs.edit().putLong(V205NudgePausePolicy.KEY_PAUSED_UNTIL, 0L).commit())
            V205NudgePauseReceiver().onReceive(
                context,
                Intent(context, V205NudgePauseReceiver::class.java).apply { action = "wrong-action" },
            )
            assertFalse(V205NudgePausePolicy.isPaused(
                System.currentTimeMillis(),
                prefs.getLong(V205NudgePausePolicy.KEY_PAUSED_UNTIL, 0L),
            ))
        } finally {
            prefs.edit().putLong(V205NudgePausePolicy.KEY_PAUSED_UNTIL, previous).commit()
        }
    }
}
