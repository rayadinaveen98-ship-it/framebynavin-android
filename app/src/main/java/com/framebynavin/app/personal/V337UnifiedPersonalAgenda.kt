package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Read-only union of two independent Personal data sources for "Your Reminders".
 *
 * Neither a new PersonalReminder nor a second Android alarm is created.
 * Challenge completion, snooze, deletion and alert scheduling stay owned
 * exclusively by Phase1HabitStore/Phase1HabitAlarmScheduler.
 */
internal sealed interface V337AgendaEntry {
    val section: PersonalAgendaSection
    val dueAtMillis: Long?
    val stableKey: String
    val title: String

    data class Personal(
        val record: PersonalReminder,
        override val section: PersonalAgendaSection,
        override val dueAtMillis: Long?,
    ) : V337AgendaEntry {
        override val stableKey: String get() = "personal:" + record.id
        override val title: String get() = record.title
    }

    data class Challenge(
        val record: Phase1HabitChallenge,
        override val section: PersonalAgendaSection,
        override val dueAtMillis: Long?,
    ) : V337AgendaEntry {
        override val stableKey: String get() = "challenge:" + record.id
        override val title: String get() = record.activity
    }
}

internal data class V337AgendaGroup(
    val section: PersonalAgendaSection,
    val entries: List<V337AgendaEntry>,
)

internal object V337UnifiedPersonalAgenda {
    /**
     * A due-today but unfinished challenge remains visible after its scheduled
     * clock time. "Needs attention" means the activity has not been confirmed,
     * NOT that Android failed to show the notification.
     */
    fun challengeEntry(
        challenge: Phase1HabitChallenge,
        nowMillis: Long,
        displayZone: ZoneId = ZoneId.systemDefault(),
    ): V337AgendaEntry.Challenge? {
        challenge.checked()
        val time = challenge.alertTime ?: return null // Alert Off is not a reminder.
        if (challenge.archived || challenge.paused) {
            return V337AgendaEntry.Challenge(challenge, PersonalAgendaSection.PAUSED, null)
        }
        val now = Instant.ofEpochMilli(nowMillis)
        val zone = ZoneId.of(challenge.zoneId)
        val localDay = now.atZone(zone).toLocalDate()
        val dueToday = if (!localDay.isBefore(challenge.startDate) &&
            !localDay.isAfter(challenge.endDate) &&
            localDay !in challenge.confirmations
        ) {
            val regularDue = localDay.atTime(time).atZone(zone).toInstant()
            // An explicit pending snooze replaces the current-day due instant.
            challenge.snoozedUntil?.takeIf { challenge.snoozedDay == localDay &&
                it.isAfter(now) } ?: regularDue
        } else null
        val due = dueToday ?: challenge.nextAlertAfter(now)?.toInstant()
        val dueMillis = due?.toEpochMilli()
        val section = when {
            dueMillis == null -> PersonalAgendaSection.NO_UPCOMING
            dueMillis <= nowMillis -> PersonalAgendaSection.NEEDS_ATTENTION
            else -> {
                val today = now.atZone(displayZone).toLocalDate()
                when (Instant.ofEpochMilli(dueMillis).atZone(displayZone).toLocalDate()) {
                    today -> PersonalAgendaSection.TODAY
                    today.plusDays(1) -> PersonalAgendaSection.TOMORROW
                    else -> PersonalAgendaSection.UPCOMING
                }
            }
        }
        return V337AgendaEntry.Challenge(challenge, section, dueMillis)
    }

    /** Uses the existing Personal filters unchanged, then merges challenge rows. */
    fun groups(
        personal: List<PersonalReminder>,
        challenges: List<Phase1HabitChallenge>,
        nowMillis: Long,
        scope: V285AgendaScope,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<V337AgendaGroup> {
        val personalEntries = V285PersonalAgendaFilter.groups(
            personal, nowMillis, scope, zone,
        ).flatMap { group ->
            group.items.map { item ->
                V337AgendaEntry.Personal(item, group.section,
                    PersonalAgendaPolicy.dueAtMillis(item, nowMillis))
            }
        }
        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val lastDay = nowDate.plusDays(6)
        val challengeEntries = challenges.mapNotNull { challenge ->
            val entry = challengeEntry(challenge, nowMillis, zone) ?: return@mapNotNull null
            if (scope == V285AgendaScope.ALL) return@mapNotNull entry
            if (entry.section == PersonalAgendaSection.NEEDS_ATTENTION) {
                return@mapNotNull entry
            }
            if (entry.section == PersonalAgendaSection.PAUSED ||
                entry.section == PersonalAgendaSection.NO_UPCOMING) return@mapNotNull null
            val due = entry.dueAtMillis ?: return@mapNotNull null
            val dueDate = Instant.ofEpochMilli(due).atZone(zone).toLocalDate()
            when (scope) {
                V285AgendaScope.TODAY -> if (dueDate == nowDate) entry else null
                V285AgendaScope.SEVEN_DAYS ->
                    if (!dueDate.isBefore(nowDate) && !dueDate.isAfter(lastDay))
                        entry else null
                V285AgendaScope.ALL -> entry
            }
        }
        val combined = personalEntries + challengeEntries
        // A stable source namespace prevents Personal and Challenge ID collisions.
        check(combined.map { it.stableKey }.distinct().size == combined.size) {
            "Duplicate agenda source identity; no store was mutated."
        }
        val bySection = combined.groupBy { it.section }
        return PersonalAgendaSection.entries.mapNotNull { section ->
            val rows = bySection[section].orEmpty()
            if (rows.isEmpty()) return@mapNotNull null
            V337AgendaGroup(
                section,
                rows.sortedWith(compareBy<V337AgendaEntry> {
                    it.dueAtMillis ?: Long.MAX_VALUE
                }.thenBy { it.title.lowercase() }.thenBy { it.stableKey }),
            )
        }
    }
}
