package com.framebynavin.app.personal

import android.content.Context
import com.framebynavin.app.widget.V224WidgetUpdater

/**
 * One-time, consented cleanup of only the old personalized rebuild installer.
 *
 * V322 shipped a manually installed 16-reminder pack and a walking challenge.
 * The owner asked for a fresh 21-day experience and approved deleting that
 * specific pack. Never delete independently created reminders or challenges.
 * No sample content is created here or by the new challenge screen.
 */
internal object FC14OwnerScheduleCleanup {
    private const val PREFS = "backlot_21day_owner_cleanup_v1"
    private const val DONE_KEY = "rebuild_20261109_removed"
    private const val REMINDER_PREFIX = "rebuild-owner-20261109-"

    /**
     * Idempotent and fail-closed. We set the migration flag only after both
     * stores have completed their atomic writes. Normal Personal completion
     * receipts for unrelated reminders are preserved by PersonalReminderStore.
     */
    suspend fun run(context: Context) {
        val app = context.applicationContext
        val flags = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (flags.getBoolean(DONE_KEY, false)) return

        Phase1HabitStore(app).removeOwnerRebuildPackChallenge()
        PersonalReminderStore(app).mutate { current ->
            current.filterNot { it.id.startsWith(REMINDER_PREFIX) }
        }

        check(flags.edit().putBoolean(DONE_KEY, true).commit()) {
            "Could not record completed old-schedule cleanup."
        }
        // Source stores are authoritative even if Android denies exact alarms.
        // Refreshing delivery must never undo or recreate deleted pack records.
        runCatching { Phase1HabitAlarmScheduler(app).reconcile() }
        runCatching { PersonalDeliveryCoordinator(app).reconcile() }
        runCatching { V224WidgetUpdater.refresh(app) }
    }
}
