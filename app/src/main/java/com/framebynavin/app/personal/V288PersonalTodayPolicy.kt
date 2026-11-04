package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Read-only, conservative projection for Personal Today. This is NOT a new
 * occurrence scheduler, and none of these counts means Android delivered an
 * alarm, that a user failed, or that a future task was completed.
 */
internal data class V288TodaySnapshot(
    val date: LocalDate,
    val next: PersonalReminder?,
    val nextAtMillis: Long?,
    val attentionCount: Int,
    val scheduledTodayCount: Int,
    val confirmedDoneToday: Int,
    val activeReminderCount: Int,
)

internal object V288PersonalTodayPolicy {
    fun snapshot(
        reminders: List<PersonalReminder>,
        history: List<PersonalCompletionEvent>,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): V288TodaySnapshot {
        val date = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val grouped = PersonalAgendaPolicy.groups(reminders, nowMillis, zone)
        val attention = grouped.firstOrNull {
            it.section == PersonalAgendaSection.NEEDS_ATTENTION
        }?.items.orEmpty()
        val today = grouped.firstOrNull {
            it.section == PersonalAgendaSection.TODAY
        }?.items.orEmpty()
        // Postponed or missed reminders take priority over future plans.
        val next = attention.firstOrNull() ?: today.firstOrNull()
        val existingIds = reminders.mapTo(HashSet()) { it.id }
        val doneToday = history.count {
            it.reminderId in existingIds &&
                Instant.ofEpochMilli(it.doneAtMillis).atZone(zone).toLocalDate() == date
        }
        return V288TodaySnapshot(
            date = date,
            next = next,
            nextAtMillis = next?.let { PersonalAgendaPolicy.dueAtMillis(it, nowMillis) },
            attentionCount = attention.size,
            scheduledTodayCount = today.size,
            confirmedDoneToday = doneToday,
            activeReminderCount = reminders.count { !it.paused },
        )
    }

    /** No premature Done/Snooze: only an actual presented lease permits an action. */
    fun canRespond(reminder: PersonalReminder?): Boolean =
        reminder != null && !reminder.paused &&
            reminder.pendingToken.isNotEmpty() &&
            reminder.presentedToken == reminder.pendingToken &&
            reminder.pendingOccurrenceMillis > 0L

    fun isNeedsAttention(
        reminder: PersonalReminder?,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean = reminder != null &&
        PersonalAgendaPolicy.section(reminder, nowMillis, zone) ==
        PersonalAgendaSection.NEEDS_ATTENTION
}
