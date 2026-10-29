package com.framebynavin.app.personal

import java.time.Instant
import java.time.ZoneId

/**
 * Read-only agenda projection. Never writes a PersonalReminder, acknowledges a
 * delivery or guesses that a notification was seen. All labels derive from
 * actual occurrence journal state and local calendar time.
 */
internal enum class PersonalAgendaSection(val label: String) {
    NEEDS_ATTENTION("NEEDS ATTENTION"),
    TODAY("TODAY"),
    TOMORROW("TOMORROW"),
    UPCOMING("UPCOMING"),
    NO_UPCOMING("NO UPCOMING OCCURRENCE"),
    PAUSED("PAUSED"),
}

internal data class PersonalAgendaGroup(
    val section: PersonalAgendaSection,
    val items: List<PersonalReminder>,
)

internal object PersonalAgendaPolicy {
    fun dueAtMillis(reminder: PersonalReminder, nowMillis: Long): Long? {
        if (reminder.paused) return null
        if (reminder.pendingFireAtMillis > 0L && reminder.pendingToken.isNotEmpty()) {
            return reminder.pendingFireAtMillis
        }
        return PersonalRecurrence.nextAfter(reminder, nowMillis)
    }

    fun section(
        reminder: PersonalReminder,
        nowMillis: Long,
        displayZone: ZoneId = ZoneId.systemDefault(),
    ): PersonalAgendaSection {
        if (reminder.paused) return PersonalAgendaSection.PAUSED
        // A posted but unanswered notification is a real attention item,
        // whether it is overdue or still inside the 24-hour response grace.
        val awaitingResponse = reminder.pendingToken.isNotBlank() &&
            reminder.presentedToken == reminder.pendingToken
        val missed = reminder.lastMissedOccurrenceMillis > reminder.lastAcknowledgedOccurrenceMillis
        if (awaitingResponse || missed) return PersonalAgendaSection.NEEDS_ATTENTION

        val due = dueAtMillis(reminder, nowMillis) ?: return PersonalAgendaSection.NO_UPCOMING
        // An unposted late alarm has NOT been presented. Show it as needing
        // attention without falsely marking it Done, Missed or Delivered.
        if (due <= nowMillis) return PersonalAgendaSection.NEEDS_ATTENTION
        val today = Instant.ofEpochMilli(nowMillis).atZone(displayZone).toLocalDate()
        return when (Instant.ofEpochMilli(due).atZone(displayZone).toLocalDate()) {
            today -> PersonalAgendaSection.TODAY
            today.plusDays(1) -> PersonalAgendaSection.TOMORROW
            else -> PersonalAgendaSection.UPCOMING
        }
    }

    fun groups(
        reminders: List<PersonalReminder>,
        nowMillis: Long,
        displayZone: ZoneId = ZoneId.systemDefault(),
    ): List<PersonalAgendaGroup> {
        val grouped = reminders.groupBy { section(it, nowMillis, displayZone) }
        return PersonalAgendaSection.entries.mapNotNull { key ->
            val records = grouped[key] ?: return@mapNotNull null
            PersonalAgendaGroup(
                key,
                records.sortedWith(compareBy<PersonalReminder> {
                    dueAtMillis(it, nowMillis) ?: Long.MAX_VALUE
                }.thenBy { it.title.lowercase() }.thenBy { it.id }),
            )
        }
    }
}
