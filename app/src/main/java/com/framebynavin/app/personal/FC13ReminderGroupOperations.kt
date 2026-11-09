package com.framebynavin.app.personal

import android.content.Context
import com.framebynavin.app.widget.V224WidgetUpdater
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.CancellationException

/**
 * Daily group progress counts only user-confirmed Personal DONE receipts.
 * A scheduled alert, a dismissed notification, a snooze, or a paused item
 * does not count as Done. A reminder linked to several groups still has
 * one authoritative Personal record and one completion journal.
 */
internal data class FC13GroupTodayProgress(
    val linked: Int,
    val scheduledToday: Int,
    val completedToday: Int,
    val paused: Int,
    val missingLinks: Int,
) {
    val remainingToday: Int get() = (scheduledToday - completedToday).coerceAtLeast(0)
    val label: String get() = when {
        scheduledToday == 0 -> "No active reminders scheduled today"
        else -> "$completedToday/$scheduledToday confirmed today"
    }
}

internal object FC13ReminderGroupProgress {
    fun today(
        group: V298ReminderGroup,
        reminders: List<PersonalReminder>,
        doneReceipts: List<PersonalCompletionEvent>,
        nowMillis: Long = System.currentTimeMillis(),
    ): FC13GroupTodayProgress {
        val index = reminders.associateBy { it.id }
        val linked = group.reminderIds.mapNotNull(index::get)
        val now = Instant.ofEpochMilli(nowMillis)
        val scheduled = linked.filter { reminder ->
            if (reminder.paused) return@filter false
            val zone = reminder.zoneId
            val day = LocalDate.ofInstant(now, zone)
            val from = day.atStartOfDay(zone).toInstant().toEpochMilli() - 1L
            val to = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val candidate = PersonalRecurrence.nextAfter(
                reminder.copy(lastAcknowledgedOccurrenceMillis = 0L), from,
            )
            candidate != null && candidate < to
        }
        val completed = scheduled.count { reminder ->
            val today = LocalDate.ofInstant(now, reminder.zoneId)
            doneReceipts.any { event ->
                event.reminderId == reminder.id &&
                    event.occurrenceMillis > 0L &&
                    LocalDate.ofInstant(
                        Instant.ofEpochMilli(event.occurrenceMillis), reminder.zoneId,
                    ) == today
            }
        }
        return FC13GroupTodayProgress(
            linked = group.reminderIds.size,
            scheduledToday = scheduled.size,
            completedToday = completed,
            paused = linked.count(PersonalReminder::paused),
            missingLinks = group.reminderIds.size - linked.size,
        )
    }
}

/** Bulk pause/resume applies to the EXISTING personal records, not group links. */
internal class FC13ReminderGroupCommands(context: Context) {
    private val app = context.applicationContext
    private val reminders = PersonalReminderStore(app)
    private val groups = V298ReminderGroupStore(app)
    private val coordinator = PersonalDeliveryCoordinator(app)

    /**
     * One atomic Personal DataStore write for every linked member, then
     * existing alarm and notification reconciliation. Never silently
     * changes a missing reminder or changes a group with stale membership.
     */
    suspend fun setAllPaused(
        groupId: String,
        expectedMemberIds: List<String>,
        paused: Boolean,
    ): Int {
        val group = groups.load().singleOrNull { it.id == groupId }
            ?: error("Group was removed. Reopen it before changing reminders.")
        require(group.reminderIds == expectedMemberIds && expectedMemberIds.isNotEmpty()) {
            "Group membership changed. Reopen the group before applying a bulk action."
        }
        val members = expectedMemberIds.toSet()
        val changed = ArrayList<String>()
        reminders.mutate { current ->
            require(current.map { it.id }.toSet().containsAll(members)) {
                "A linked Personal reminder is unavailable; no reminders were changed."
            }
            val now = System.currentTimeMillis()
            current.map { original ->
                if (original.id !in members || original.paused == paused) original
                else {
                    changed += original.id
                    original.copy(
                        paused = paused,
                        pendingOccurrenceMillis = 0L,
                        pendingFireAtMillis = 0L,
                        pendingToken = "",
                        presentedToken = "",
                        presentedAtMillis = 0L,
                        followUpSentToken = "",
                        followUpSentOccurrenceMillis = 0L,
                        snoozedUntilMillis = 0L,
                        autoRetryCount = 0,
                        autoRetryScheduledAtMillis = 0L,
                        updatedAtMillis = now,
                    )
                }
            }
        }
        if (changed.isEmpty()) return 0
        val errors = ArrayList<Throwable>()
        changed.forEach { id ->
            try {
                coordinator.cancelPending(id)
                PersonalPlaybackService.stopForReminder(id)
                PersonalReminderNotifications.cancel(app, id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                errors += failure
            }
        }
        try {
            coordinator.reconcile()
            V224WidgetUpdater.refresh(app)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            errors += failure
        }
        if (errors.isNotEmpty()) {
            val error = IllegalStateException(
                "Group reminder states were saved, but Android alerts need attention. " +
                    "Open Personal Reminders to review delivery permissions.",
                errors.first(),
            )
            errors.drop(1).forEach(error::addSuppressed)
            throw error
        }
        return changed.size
    }
}
