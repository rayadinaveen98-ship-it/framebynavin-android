package com.framebynavin.app.ui

import com.framebynavin.app.data.ReminderCenterGroup

/** Filters EXISTING canonical reminder groups, never computes a new status. */
internal enum class V219ReminderHubFilter(val title: String, val category: ReminderCenterGroup?) {
    ALL("All", null),
    ATTENTION("Needs action", ReminderCenterGroup.NEEDS_ATTENTION),
    TODAY("Today", ReminderCenterGroup.TODAY),
    POSTPONED("Postponed", ReminderCenterGroup.SNOOZED_PAUSED),
    UPCOMING("Upcoming", ReminderCenterGroup.UPCOMING);

    fun includes(group: ReminderCenterGroup): Boolean =
        category == null || group == category

    fun count(grouped: Map<ReminderCenterGroup, List<*>>): Int =
        if (category == null) grouped.values.sumOf { it.size }
        else grouped[category].orEmpty().size
}
