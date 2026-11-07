package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity

/**
 * Authenticated-archive comparison for a strictly additive no-JPEG restore.
 *
 * Only identities ADDED by the imported target can ever be removed by a
 * future owner-approved rollback. An existing Personal reminder, completion,
 * Challenge, Group, Space, linked reminder or Space note must have exactly
 * identical canonical data in the target. The caller must authenticate both
 * encrypted archives and separately confirm current crash chronology first.
 */
internal data class B04NoPhotoRollbackDelta(
    val transactionId: String,
    val originalDigest: String,
    val targetDigest: String,
    val newlyAddedReminderIds: Set<String>,
    val newlyAddedCompletionKeys: Set<String>,
    val newlyAddedChallengeIds: Set<String>,
    val newlyAddedGroupIds: Set<String>,
    val newlyAddedSpaceIds: Set<String>,
    val newlyAddedSpaceLinks: Set<Pair<String, String>>,
    val newlyAddedSpaceEntryIds: Set<String>,
) {
    val grantsWriteAuthority: Boolean get() = false
    val mayRemoveOriginalData: Boolean get() = false
    val mayDeletePrivatePhotos: Boolean get() = false
}

internal object B04NoPhotoRollbackDeltaPolicy {
    fun calculate(
        journal: StabilizationRestoreJournalRecord,
        original: V300PersonalComponents,
        target: V300PersonalComponents,
        originalPhotos: List<V290SpacePhotoEntity>,
        targetPhotos: List<V290SpacePhotoEntity>,
    ): B04NoPhotoRollbackDelta {
        StabilizationRestoreJournalCodec.validate(journal)
        require(originalPhotos.isEmpty() && targetPhotos.isEmpty()) {
            "Restores with private photos require a separate crash-safe rollback"
        }
        require(V304RestorePlanPolicy.fingerprint(original, originalPhotos) ==
            journal.originalDigest &&
            V304RestorePlanPolicy.fingerprint(target, targetPhotos) ==
                journal.targetDigest) {
            "Rollback delta does not match the authenticated journal and escrow"
        }
        val oldPersonal = PersonalCompletionBackupCodec.decode(original.personal)
        val newPersonal = PersonalCompletionBackupCodec.decode(target.personal)
        val oldSpace = V303SpaceReceiptArchiveCodec.decode(original.spaces, oldPersonal)
        val newSpace = V303SpaceReceiptArchiveCodec.decode(target.spaces, newPersonal)
        fun <T, K> introduced(
            before: List<T>, after: List<T>, key: (T) -> K,
            name: String,
        ): Set<K> {
            val baseline = before.associateBy(key)
            val proposed = after.associateBy(key)
            require(baseline.size == before.size && proposed.size == after.size) {
                "Repeated $name identity in rollback evidence"
            }
            require(baseline.all { (id, existing) -> proposed[id] == existing }) {
                "Target changed or removed an original owner's $name"
            }
            return proposed.keys - baseline.keys
        }
        val addedReminders = introduced(
            oldPersonal.reminders, newPersonal.reminders, { it.id }, "Personal reminder",
        )
        val addedCompletions = introduced(
            oldPersonal.completions, newPersonal.completions,
            { it.stableKey }, "Personal Done receipt",
        )
        val addedChallenges = introduced(
            V297ChallengeCodec.decode(original.challenges),
            V297ChallengeCodec.decode(target.challenges),
            { it.id }, "Challenge",
        )
        val addedGroups = introduced(
            V298ReminderGroupCodec.decode(original.groups),
            V298ReminderGroupCodec.decode(target.groups),
            { it.id }, "Reminder Group",
        )
        val addedSpaces = introduced(
            oldSpace.spaces, newSpace.spaces, { it.id }, "Space",
        )
        val addedLinks = introduced(
            oldSpace.links, newSpace.links, { it.spaceId to it.reminderId },
            "Space reminder link",
        )
        val addedEntries = introduced(
            oldSpace.entries, newSpace.entries, { it.id }, "Space journal entry",
        )
        val existingIds = oldPersonal.reminders.mapTo(HashSet()) { it.id }
        val targetIds = newPersonal.reminders.mapTo(HashSet()) { it.id }
        require(newSpace.links.all { it.reminderId in targetIds } &&
            V298ReminderGroupCodec.decode(target.groups).all { g ->
                g.reminderIds.all { it in targetIds }
            }) { "Proposed rollback contains a broken reminder relationship" }
        require(oldSpace.links.all { it.reminderId in existingIds }) {
            "The owner baseline contains an invalid Space link"
        }
        require(addedCompletions.all { key ->
            val item = newPersonal.completions.single { it.stableKey == key }
            item.reminderId in targetIds
        }) { "Completion history refers to a missing Personal reminder" }
        // A pure additive delta may include a new entry/link inside an
        // EXISTING owner Space. Those rows can be removed individually,
        // but never cascade-delete an existing Space itself.
        return B04NoPhotoRollbackDelta(
            transactionId = journal.transactionId,
            originalDigest = journal.originalDigest,
            targetDigest = journal.targetDigest,
            newlyAddedReminderIds = addedReminders,
            newlyAddedCompletionKeys = addedCompletions,
            newlyAddedChallengeIds = addedChallenges,
            newlyAddedGroupIds = addedGroups,
            newlyAddedSpaceIds = addedSpaces,
            newlyAddedSpaceLinks = addedLinks,
            newlyAddedSpaceEntryIds = addedEntries,
        )
    }
}
