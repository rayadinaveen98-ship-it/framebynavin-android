package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import com.framebynavin.app.reminders.ReminderMusicMode

/**
 * Pure, non-writing preview of an already authenticated V300 archive.
 *
 * The preview is intentionally NOT a restore permit. A future restore engine
 * must re-verify the original source and local revision immediately before
 * every owner-authorized transaction; this report must never be reused as a
 * proof the target has not changed.
 */
internal data class V301LocalRecoverySnapshot(
    val components: V300PersonalComponents,
    val photos: List<V290SpacePhotoEntity>,
    val localIntegrityVerified: Boolean,
)

internal data class V301DomainDiff(
    val proposed: Int,
    val newRecords: Int,
    val identical: Int,
    val conflicting: Int,
    val localOnly: Int,
) {
    init {
        require(listOf(proposed, newRecords, identical, conflicting, localOnly).all { it >= 0 })
        require(proposed == newRecords + identical + conflicting)
    }

    val needsResolution: Boolean get() = conflicting > 0
}

internal data class V301RestoreDryRunReport(
    val reminders: V301DomainDiff,
    val completedActions: V301DomainDiff,
    val challenges: V301DomainDiff,
    val groups: V301DomainDiff,
    val spaces: V301DomainDiff,
    val spaceLinks: V301DomainDiff,
    val journalEntries: V301DomainDiff,
    val photos: V301DomainDiff,
    val localIntegrityVerified: Boolean,
) {
    val requiresConflictReview: Boolean get() =
        !localIntegrityVerified || listOf(
            reminders, completedActions, challenges, groups,
            spaces, spaceLinks, journalEntries, photos,
        ).any { it.needsResolution }

    val adds: Int get() = listOf(
        reminders, completedActions, challenges, groups,
        spaces, spaceLinks, journalEntries, photos,
    ).sumOf { it.newRecords }

    val conflicts: Int get() = listOf(
        reminders, completedActions, challenges, groups,
        spaces, spaceLinks, journalEntries, photos,
    ).sumOf { it.conflicting }

    /** Explicitly never a write authorization: backup import is not built. */
    val approvedToWrite: Boolean get() = false

    val neutralSummary: String get() =
        "${adds} potential new records and ${conflicts} ID conflicts. " +
            "No changes made to the phone."

    val warning: String get() = when {
        !localIntegrityVerified ->
            "Existing local photo/data integrity has not been fully verified. " +
                "Do not attempt a restore."
        conflicts > 0 ->
            "${conflicts} existing IDs have different data. " +
                "No automatic replacement or merge is permitted."
        else ->
            "No differing IDs were detected, but this is a preview only. " +
                "Archive restore is not enabled."
    }
}

internal object V301RestoreDryRun {
    /**
     * Call only after V300 encrypted-archive authentication, and preferably
     * while the stage remains open. This does not inspect ZIP or disk files.
     */
    fun compare(
        local: V301LocalRecoverySnapshot,
        incoming: V300PersonalComponents,
        incomingPhotos: List<V290SpacePhotoEntity>,
    ): V301RestoreDryRunReport {
        val existingPersonal = PersonalCompletionBackupCodec.decode(local.components.personal)
        val importedPersonal = PersonalCompletionBackupCodec.decode(incoming.personal)
        val existingChallenges = V297ChallengeCodec.decode(local.components.challenges)
        val importedChallenges = V297ChallengeCodec.decode(incoming.challenges)
        val existingGroups = V298ReminderGroupCodec.decode(local.components.groups)
        val importedGroups = V298ReminderGroupCodec.decode(incoming.groups)
        val existingSpaces = V289SpacesMetadataCodec.decode(local.components.spaces)
        val importedSpaces = V289SpacesMetadataCodec.decode(incoming.spaces)

        val incomingIds = importedPersonal.reminders.mapTo(HashSet()) { it.id }
        require(importedGroups.all { g -> g.reminderIds.all { it in incomingIds } }) {
            "Incoming group has a reminder missing from the archive"
        }
        require(importedSpaces.links.all { it.reminderId in incomingIds }) {
            "Incoming Space link has a reminder missing from the archive"
        }
        local.photos.forEach { V290PhotoTimelinePolicy.validate(it) }
        incomingPhotos.forEach { V290PhotoTimelinePolicy.validate(it) }
        val localSpaceIds = existingSpaces.spaces.mapTo(HashSet()) { it.id }
        val incomingSpaceIds = importedSpaces.spaces.mapTo(HashSet()) { it.id }
        require(local.photos.all { it.spaceId in localSpaceIds }) {
            "Local photo record has no Space"
        }
        require(incomingPhotos.all { it.spaceId in incomingSpaceIds }) {
            "Incoming photo record has no Space"
        }

        return V301RestoreDryRunReport(
            reminders = diff(portableReminders(existingPersonal.reminders),
                portableReminders(importedPersonal.reminders)) { it.id },
            completedActions = diff(existingPersonal.completions, importedPersonal.completions) {
                it.stableKey
            },
            challenges = diff(existingChallenges, importedChallenges) { it.id },
            groups = diff(existingGroups, importedGroups) { it.id },
            spaces = diff(existingSpaces.spaces, importedSpaces.spaces) { it.id },
            spaceLinks = diff(existingSpaces.links, importedSpaces.links) {
                it.spaceId to it.reminderId
            },
            journalEntries = diff(existingSpaces.entries, importedSpaces.entries) { it.id },
            photos = diff(local.photos, incomingPhotos) { it.id },
            localIntegrityVerified = local.localIntegrityVerified,
        )
    }

    /**
     * A ZIP cannot carry this device's scheduled-alarm leases or local song
     * content URIs. Compare the PORTABLE representation of both sides; do not
     * flag identical reminders solely because those volatile fields differ.
     */
    private fun portableReminders(items: List<PersonalReminder>): List<PersonalReminder> =
        PersonalBackupPolicy.resetDeviceLeases(items).map { item ->
            if (item.voiceMusicMode == ReminderMusicMode.CUSTOM) {
                item.copy(voiceMusicMode = ReminderMusicMode.PIANO, voiceMusicUri = "").checked()
            } else {
                item.copy(voiceMusicUri = "").checked()
            }
        }

    private fun <T, K> diff(
        local: List<T>,
        incoming: List<T>,
        identity: (T) -> K,
    ): V301DomainDiff {
        val before = local.associateBy(identity)
        val after = incoming.associateBy(identity)
        require(before.size == local.size && after.size == incoming.size) {
            "Duplicate record identity prevents safe recovery preview"
        }
        var additions = 0
        var identical = 0
        var conflicts = 0
        for ((id, record) in after) {
            if (!before.containsKey(id)) additions++
            else if (before[id] == record) identical++
            else conflicts++
        }
        return V301DomainDiff(
            proposed = incoming.size,
            newRecords = additions,
            identical = identical,
            conflicting = conflicts,
            localOnly = before.keys.count { it !in after },
        )
    }
}
