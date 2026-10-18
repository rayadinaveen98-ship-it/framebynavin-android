package com.framebynavin.app.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Tests actual Android AlarmManager scheduling and Backlot's own durable
 * ledger. Does not claim the OS will fire an alarm exactly on every OEM device.
 *
 * No production project data is created, and scheduled test alarms are removed.
 */
@RunWith(AndroidJUnit4::class)
class V159ReminderScheduleAcceptanceTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val hour = 60L * 60_000L

    private fun reminder(mode: ReminderMode, suffix: String = UUID.randomUUID().toString()): CreatorTask {
        val now = System.currentTimeMillis()
        return CreatorTask(
            id = "v159-qa-$suffix",
            title = "Reminder acceptance test",
            platform = "YouTube",
            contentType = "Long",
            dueLabel = "In four hours",
            reminderEnabled = true,
            reminderAtMillis = now + 2 * hour,
            dueAtMillis = now + 4 * hour,
            reminderMode = mode,
            smartEscalationEnabled = mode == ReminderMode.SMART,
            priority = TaskPriority.IMPORTANT,
            attentionPlan = ProjectAttentionPlan.CUSTOM,
        )
    }

    @Test fun schedulingAndCancellingOneReminderNeverCancelsAnother() {
        val first = reminder(ReminderMode.SIMPLE)
        val second = reminder(ReminderMode.SIMPLE)
        val scheduler = ReminderScheduler(context)
        val ledger = AlarmLedger(context)
        try {
            scheduler.schedule(first)
            scheduler.schedule(second)
            assertEquals(first.reminderAtMillis, ledger.scheduledAt(first.id))
            assertEquals(second.reminderAtMillis, ledger.scheduledAt(second.id))

            scheduler.cancel(first.id)
            assertNull(ledger.scheduledAt(first.id))
            assertEquals(
                "Cancelling the first project must preserve the second reminder",
                second.reminderAtMillis, ledger.scheduledAt(second.id),
            )
        } finally {
            scheduler.cancel(first.id)
            scheduler.cancel(second.id)
            ledger.clearDelivered(first.id)
            ledger.clearDelivered(second.id)
        }
    }

    @Test fun appResumeAndTimeChangeReconciliationKeepsSingleAuthoritativeAlarm() {
        val task = reminder(ReminderMode.SIMPLE)
        val scheduler = ReminderScheduler(context)
        val ledger = AlarmLedger(context)
        try {
            scheduler.schedule(task)
            val scheduled = ledger.scheduledAt(task.id)
            assertEquals(task.reminderAtMillis, scheduled)
            ReminderRecoveryEngine.reconcileTask(context, task)
            ReminderRecoveryEngine.reconcileTask(context, task)
            assertEquals(
                "Two recovery passes must not change reminder time",
                scheduled, ledger.scheduledAt(task.id),
            )

            val changed = task.copy(reminderAtMillis = task.reminderAtMillis + 30 * 60_000L)
            scheduler.schedule(changed)
            assertEquals(changed.reminderAtMillis, ledger.scheduledAt(changed.id))
            ReminderRecoveryEngine.reconcileTask(context, changed)
            assertEquals(changed.reminderAtMillis, ledger.scheduledAt(changed.id))
        } finally {
            scheduler.cancel(task.id)
            ledger.clearDelivered(task.id)
        }
    }

    @Test fun pendingOnlyCleanupPreservesAnAlreadyDisplayedRemindersActionToken() {
        val task = reminder(ReminderMode.SIMPLE)
        val scheduler = ReminderScheduler(context)
        val ledger = AlarmLedger(context)
        val occurrences = ReminderOccurrenceStore(context)
        try {
            scheduler.schedule(task)
            val token = occurrences.issue(task)
            scheduler.cancelPendingOnly(task.id)
            assertNull(ledger.scheduledAt(task.id))
            assertTrue("The visible notification's action token must stay valid", occurrences.matches(task, token))
        } finally {
            scheduler.cancel(task.id)
            ledger.clearDelivered(task.id)
        }
    }

    @Test fun smartRecoveryPreservesOriginalFinalTargetAndPendingSoftStage() {
        val task = reminder(ReminderMode.SMART)
        val smart = SmartEscalationScheduler(context)
        val ledger = AlarmLedger(context)
        val occurrences = ReminderOccurrenceStore(context)
        val stageKey = "${task.id}#SOFT"
        try {
            smart.schedule(task)
            val initial = ledger.scheduledAt(stageKey)
            assertNotNull("Smart reminder must schedule a first notification stage", initial)
            assertTrue(initial!! > System.currentTimeMillis())
            assertTrue(initial <= task.reminderAtMillis)

            val token = occurrences.issue(task)
            smart.recover(task)
            assertEquals(
                "App resume must preserve the original pending Smart stage",
                initial, ledger.scheduledAt(stageKey),
            )
            assertTrue("Smart recovery must preserve an active occurrence action", occurrences.matches(task, token))
        } finally {
            smart.cancel(task.id)
            ledger.clearDelivered(task.id)
        }
    }

    @Test fun disablingAnOldScheduledTaskRemovesItsPendingAlert() {
        val task = reminder(ReminderMode.SIMPLE)
        val scheduler = ReminderScheduler(context)
        val ledger = AlarmLedger(context)
        try {
            scheduler.schedule(task)
            assertEquals(task.reminderAtMillis, ledger.scheduledAt(task.id))
            ReminderRecoveryEngine.reconcileTask(context, task.copy(reminderEnabled = false))
            assertNull(ledger.scheduledAt(task.id))
        } finally {
            scheduler.cancel(task.id)
            ledger.clearDelivered(task.id)
        }
    }
}
