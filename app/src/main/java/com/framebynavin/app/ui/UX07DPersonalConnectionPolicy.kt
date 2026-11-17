package com.framebynavin.app.ui

import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.V298ReminderGroup
import java.util.Locale

/**
 * UX07D read-only projections for existing Personal reminders, Space links
 * and Reminder Groups. No new owner, DataStore, alarm or Done receipt.
 */
internal data class UX07DBulkImpact(
    val linked: Int,
    val changing: Int,
    val unchanged: Int,
    val missing: Int,
    val sharedWithOtherGroups: Int,
)

internal object UX07DPersonalConnectionPolicy {
    fun matchingGroups(
        groups: List<V298ReminderGroup>,
        search: String,
    ): List<V298ReminderGroup> {
        val query = search.trim()
        if (query.isEmpty()) return groups
        return groups.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun availableSpaceReminders(
        reminders: List<PersonalReminder>,
        linkedIds: Set<String>,
        search: String,
    ): List<PersonalReminder> {
        val query = search.trim()
        return reminders.asSequence()
            .filterNot { it.id in linkedIds }
            .filter { query.isEmpty() || it.title.contains(query, ignoreCase = true) }
            .sortedWith(
                compareBy<PersonalReminder> { it.title.lowercase(Locale.ROOT) }
                    .thenBy { it.id }
            )
            .toList()
    }

    /** Snapshot for explanation only; the original owner command rechecks IDs
     * and atomically applies the real bulk mutation after confirmation. */
    fun bulkImpact(
        group: V298ReminderGroup,
        allGroups: List<V298ReminderGroup>,
        reminders: List<PersonalReminder>,
        targetPaused: Boolean,
    ): UX07DBulkImpact {
        val index = reminders.associateBy { it.id }
        val present = group.reminderIds.mapNotNull(index::get)
        val changing = present.filter { it.paused != targetPaused }
        val sharedIds = allGroups.asSequence()
            .filter { it.id != group.id }
            .flatMap { it.reminderIds.asSequence() }
            .toSet()
        return UX07DBulkImpact(
            linked = group.reminderIds.size,
            changing = changing.size,
            unchanged = present.size - changing.size,
            missing = group.reminderIds.size - present.size,
            sharedWithOtherGroups = changing.count { it.id in sharedIds },
        )
    }
}
