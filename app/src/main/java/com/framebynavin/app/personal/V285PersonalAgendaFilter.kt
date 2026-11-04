package com.framebynavin.app.personal

import java.time.Instant
import java.time.ZoneId

/**
 * Presentation-only Personal agenda scopes. The list below is never the source
 * of truth for scheduling, Done history, or cloud backups. Attention items
 * remain visible regardless of the selected time window.
 */
internal enum class V285AgendaScope(val label: String) {
    TODAY("TODAY"),
    SEVEN_DAYS("NEXT 7 DAYS"),
    ALL("ALL"),
}

internal object V285PersonalAgendaFilter {
    fun groups(
        reminders: List<PersonalReminder>,
        nowMillis: Long,
        scope: V285AgendaScope,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<PersonalAgendaGroup> {
        val original = PersonalAgendaPolicy.groups(reminders, nowMillis, zone)
        if (scope == V285AgendaScope.ALL) return original

        val startDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val lastIncluded = startDate.plusDays(6)
        return original.mapNotNull { group ->
            val visible = group.items.filter { item ->
                if (group.section == PersonalAgendaSection.NEEDS_ATTENTION) return@filter true
                if (item.paused) return@filter false
                val due = PersonalAgendaPolicy.dueAtMillis(item, nowMillis) ?: return@filter false
                val dueDate = Instant.ofEpochMilli(due).atZone(zone).toLocalDate()
                when (scope) {
                    V285AgendaScope.TODAY -> dueDate == startDate
                    V285AgendaScope.SEVEN_DAYS -> !dueDate.isBefore(startDate) &&
                        !dueDate.isAfter(lastIncluded)
                    V285AgendaScope.ALL -> true
                }
            }
            if (visible.isEmpty()) null else group.copy(items = visible)
        }
    }

    fun visibleCount(
        reminders: List<PersonalReminder>, nowMillis: Long,
        scope: V285AgendaScope, zone: ZoneId = ZoneId.systemDefault(),
    ): Int = groups(reminders, nowMillis, scope, zone).sumOf { it.items.size }
}
