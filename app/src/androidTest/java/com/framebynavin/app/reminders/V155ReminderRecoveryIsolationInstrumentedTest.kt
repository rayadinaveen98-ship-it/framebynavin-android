package com.framebynavin.app.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Verify that periodic app-resume recovery cannot invalidate the action token
 * of a reminder that has already reached the notification/voice/alarm surface.
 */
@RunWith(AndroidJUnit4::class)
class V155ReminderRecoveryIsolationInstrumentedTest {
    @Test
    fun cleaningObsoleteRegularAlarmPreservesActiveSmartToken() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val task = task(ReminderMode.SMART)
        val occurrences = ReminderOccurrenceStore(context)
        try {
            val token = occurrences.issue(task)
            ReminderScheduler(context).cancelPendingOnly(task.id)
            assertTrue("Smart check-in token must remain actionable", occurrences.matches(task, token))
        } finally {
            occurrences.invalidate(task.id)
        }
    }

    @Test
    fun cleaningObsoleteSmartStagesPreservesRegularNotificationToken() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val task = task(ReminderMode.SIMPLE)
        val occurrences = ReminderOccurrenceStore(context)
        try {
            val token = occurrences.issue(task)
            SmartEscalationScheduler(context).cancelPendingOnly(task.id)
            assertTrue("Regular reminder token must remain actionable", occurrences.matches(task, token))
        } finally {
            occurrences.invalidate(task.id)
        }
    }

    private fun task(mode: ReminderMode): CreatorTask {
        val now = System.currentTimeMillis()
        return CreatorTask(
            id = UUID.randomUUID().toString(),
            title = "Reminder recovery test",
            platform = "YouTube",
            contentType = "Long",
            dueLabel = "Tomorrow",
            dueAtMillis = now + 24 * 60 * 60_000L,
            reminderEnabled = true,
            reminderAtMillis = now + 60 * 60_000L,
            reminderMode = mode,
            smartEscalationEnabled = mode == ReminderMode.SMART,
        )
    }
}
