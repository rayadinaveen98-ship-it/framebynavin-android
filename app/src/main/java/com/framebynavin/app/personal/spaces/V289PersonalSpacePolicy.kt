package com.framebynavin.app.personal.spaces

import java.util.Locale
import java.util.UUID

/**
 * V289 pure local-first domain contract. Does not modify PersonalReminderStore,
 * Android alarms, Creator projects, Room, cloud backup or notification history.
 * Persistence and UI will be introduced behind independent migration gates.
 */
internal enum class V289SpaceType(val defaultTitle: String, val iconKey: String) {
    SKIN_CARE("Skin Care", "sun"),
    FITNESS("Fitness", "dumbbell"),
    READING("Reading", "book-open"),
    EDITING("Editing & Filmmaking", "clapperboard"),
    GAME_DEVELOPMENT("Game Development", "code"),
    GAMING("Gaming", "gamepad"),
    CUSTOM("My Space", "sparkles"),
}

internal data class V289PersonalSpace(
    val id: String,
    val title: String,
    val type: V289SpaceType,
    val iconKey: String = type.iconKey,
    val goalText: String = "",
    val createdAtMillis: Long,
    val archivedAtMillis: Long? = null,
    val displayOrder: Int = 0,
)

internal data class V289SpaceReminderLink(
    val spaceId: String,
    val reminderId: String,
    val linkedAtMillis: Long,
)

/** A Space entry is independently durable; a reminder deletion must not erase it. */
internal enum class V289SpaceEntryKind { CHECK_IN, NOTE, METRIC, MILESTONE }
internal enum class V289SpaceEntrySource { MANUAL, CONFIRMED_REMINDER_DONE }

internal data class V289SpaceEntry(
    val id: String,
    val spaceId: String,
    val kind: V289SpaceEntryKind,
    val source: V289SpaceEntrySource,
    val occurredAtMillis: Long,
    val createdAtMillis: Long,
    val note: String = "",
    val metricName: String? = null,
    val metricValue: Double? = null,
    val metricUnit: String? = null,
    /** Proof of an explicitly acknowledged receipt; never inferred from notification posting. */
    val reminderId: String? = null,
    val originalOccurrenceMillis: Long? = null,
)

internal data class V289SpaceState(
    val spaces: List<V289PersonalSpace> = emptyList(),
    val links: List<V289SpaceReminderLink> = emptyList(),
    val entries: List<V289SpaceEntry> = emptyList(),
)

internal data class V289SpaceOverview(
    val space: V289PersonalSpace,
    val manualEntries: Int,
    val confirmedDoneEntries: Int,
    val linkedReminderCount: Int,
)

/**
 * Side-effect-free policy for the upcoming Room repository. Prevent bad or fake
 * activity records at both the editing and database boundaries.
 */
internal object V289PersonalSpacePolicy {
    fun canonicalUuid(): String = UUID.randomUUID().toString()

    private fun checkUuid(id: String) {
        val uuid = runCatching { UUID.fromString(id) }.getOrNull()
        require(uuid != null && uuid.toString().lowercase(Locale.ROOT) == id) {
            "Invalid Space identifier"
        }
    }

    fun validateSpace(space: V289PersonalSpace): V289PersonalSpace {
        checkUuid(space.id)
        require(space.title == space.title.trim() && space.title.length in 1..80) {
            "Space title must be 1–80 characters without surrounding spaces"
        }
        require(space.iconKey.matches(Regex("[a-z0-9-]{1,40}"))) {
            "Use an approved icon identifier"
        }
        require(space.goalText.length <= 500) { "Space goal is too long" }
        require(space.createdAtMillis > 0L) { "Space creation time is invalid" }
        require(space.archivedAtMillis == null || space.archivedAtMillis >= space.createdAtMillis) {
            "Space archive time is invalid"
        }
        require(space.displayOrder >= 0) { "Space order cannot be negative" }
        return space
    }

    fun newSpace(
        type: V289SpaceType,
        nowMillis: Long,
        id: String = canonicalUuid(),
    ): V289PersonalSpace = validateSpace(
        V289PersonalSpace(
            id = id,
            title = type.defaultTitle,
            type = type,
            createdAtMillis = nowMillis,
        )
    )

    fun archive(space: V289PersonalSpace, nowMillis: Long): V289PersonalSpace =
        validateSpace(space.copy(archivedAtMillis = nowMillis))

    fun unarchive(space: V289PersonalSpace): V289PersonalSpace =
        validateSpace(space.copy(archivedAtMillis = null))

    fun validateLink(link: V289SpaceReminderLink): V289SpaceReminderLink {
        checkUuid(link.spaceId)
        require(link.reminderId.isNotBlank() && link.reminderId.length <= 128) {
            "Reminder reference is invalid"
        }
        require(link.linkedAtMillis > 0L) { "Reminder link time is invalid" }
        return link
    }

    fun validateEntry(entry: V289SpaceEntry): V289SpaceEntry {
        checkUuid(entry.id)
        checkUuid(entry.spaceId)
        require(entry.occurredAtMillis > 0L && entry.createdAtMillis > 0L) {
            "Journal entry times are invalid"
        }
        require(entry.note.length <= 2000) { "Journal note is too long" }
        val metricFields = listOf(entry.metricName, entry.metricValue, entry.metricUnit)
        if (entry.kind == V289SpaceEntryKind.METRIC) {
            require(metricFields.none { it == null }) {
                "Metric name, value and unit are required"
            }
            require(entry.metricName!!.length in 1..60 && entry.metricUnit!!.length in 1..30) {
                "Metric label is invalid"
            }
            require(entry.metricValue!!.isFinite()) { "Metric value must be finite" }
        } else {
            require(metricFields.all { it == null }) {
                "Only metric entries can contain numeric measurements"
            }
        }
        when (entry.source) {
            V289SpaceEntrySource.MANUAL -> {
                require(entry.reminderId == null && entry.originalOccurrenceMillis == null) {
                    "Manual check-ins cannot claim a delivered reminder"
                }
            }
            V289SpaceEntrySource.CONFIRMED_REMINDER_DONE -> {
                require(entry.reminderId?.isNotBlank() == true &&
                    entry.reminderId.length <= 128 &&
                    entry.originalOccurrenceMillis != null &&
                    entry.originalOccurrenceMillis > 0L &&
                    entry.kind == V289SpaceEntryKind.CHECK_IN) {
                    "Confirmed reminder activity needs an accepted Done receipt"
                }
            }
        }
        return entry
    }

    fun addSpace(state: V289SpaceState, space: V289PersonalSpace): V289SpaceState {
        validateSpace(space)
        require(state.spaces.none { it.id == space.id }) { "Space already exists" }
        return state.copy(spaces = state.spaces + space)
    }

    fun addLink(state: V289SpaceState, link: V289SpaceReminderLink): V289SpaceState {
        validateLink(link)
        require(state.spaces.any { it.id == link.spaceId }) { "Space does not exist" }
        if (state.links.any { it.spaceId == link.spaceId && it.reminderId == link.reminderId }) {
            return state // idempotent; one reminder delivery may be referenced by several Spaces
        }
        return state.copy(links = state.links + link)
    }

    /** Only entries explicitly produced by check-in or accepted Done actions may reach this API. */
    fun addEntry(state: V289SpaceState, entry: V289SpaceEntry): V289SpaceState {
        validateEntry(entry)
        require(state.spaces.any { it.id == entry.spaceId }) { "Space does not exist" }
        require(state.entries.none { it.id == entry.id }) { "Entry ID already exists" }
        if (entry.source == V289SpaceEntrySource.CONFIRMED_REMINDER_DONE &&
            state.entries.any {
                it.spaceId == entry.spaceId &&
                    it.source == V289SpaceEntrySource.CONFIRMED_REMINDER_DONE &&
                    it.reminderId == entry.reminderId &&
                    it.originalOccurrenceMillis == entry.originalOccurrenceMillis
            }) {
            return state // repeated delivery/restore cannot inflate recorded progress
        }
        return state.copy(entries = state.entries + entry)
    }

    /** Space deletion is local to Spaces. Caller must *not* delete Personal reminders. */
    fun deleteSpace(state: V289SpaceState, spaceId: String): V289SpaceState {
        require(state.spaces.any { it.id == spaceId }) { "Space does not exist" }
        return V289SpaceState(
            spaces = state.spaces.filterNot { it.id == spaceId },
            links = state.links.filterNot { it.spaceId == spaceId },
            entries = state.entries.filterNot { it.spaceId == spaceId },
        )
    }

    fun overviews(state: V289SpaceState): List<V289SpaceOverview> =
        state.spaces.sortedWith(compareBy<V289PersonalSpace> { it.archivedAtMillis != null }
            .thenBy { it.displayOrder }.thenBy { it.createdAtMillis }).map { space ->
            val items = state.entries.filter { it.spaceId == space.id }
            V289SpaceOverview(
                space = space,
                manualEntries = items.count { it.source == V289SpaceEntrySource.MANUAL },
                confirmedDoneEntries = items.count {
                    it.source == V289SpaceEntrySource.CONFIRMED_REMINDER_DONE
                },
                linkedReminderCount = state.links.count { it.spaceId == space.id },
            )
        }
}
