package com.framebynavin.app.personal

import android.content.Context
import androidx.room.withTransaction
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.personal.spaces.V289SpaceEntryEntity
import com.framebynavin.app.personal.spaces.V289SpaceReminderLinkEntity
import com.framebynavin.app.personal.spaces.V289SpacesDatabase

/**
 * No-photo, owner-consented rollback of imported Room Space rows only.
 * A Room transaction removes imported entries, imported links, then newly
 * created Spaces. Existing owner Spaces, notes, links and photo rows are never
 * removed or overwritten. Calls outside the recovery permit fail closed.
 */
internal class B04NoPhotoRollbackDomainWriter(context: Context) {
    private val app = context.applicationContext

    suspend fun removeOnlyImportedSpaces(
        target: V300PersonalComponents,
        delta: B04NoPhotoRollbackDelta,
    ) {
        StabilizationPersonalMutationGate.requireActiveRecovery(app)
        val personal = PersonalCompletionBackupCodec.decode(target.personal)
        val state = V303SpaceReceiptArchiveCodec.decode(target.spaces, personal)
        val entries = state.entries.associateBy { it.id }
        val links = state.links.associateBy { it.spaceId to it.reminderId }
        val spaces = state.spaces.associateBy { it.id }
        require(delta.newlyAddedSpaceEntryIds.all { it in entries } &&
            delta.newlyAddedSpaceLinks.all { it in links } &&
            delta.newlyAddedSpaceIds.all { it in spaces }) {
            "Rollback requested deletion outside the authenticated imported Space target"
        }
        val db = V289SpacesDatabase.open(app)
        try {
            db.withTransaction {
                val dao = db.spaces()
                val existingEntries = dao.allEntries().associateBy { it.id }
                val existingLinks = dao.allLinks().associateBy { it.spaceId to it.reminderId }
                val existingSpaces = dao.loadSpaces().associateBy { it.id }

                for (id in delta.newlyAddedSpaceEntryIds) {
                    val source = checkNotNull(entries[id])
                    val expected = V289SpaceEntryEntity(
                        source.id, source.spaceId, source.kind.name,
                        source.source.name, source.occurredAtMillis,
                        source.createdAtMillis, source.note, source.metricName,
                        source.metricValue, source.metricUnit, source.reminderId,
                        source.originalOccurrenceMillis,
                    )
                    val current = existingEntries[id]
                    if (current != null) {
                        check(current == expected) {
                            "Imported Space entry changed since rollback approval"
                        }
                        check(dao.deleteEntry(id) == 1) {
                            "Cannot remove confirmed imported Space journal entry"
                        }
                    }
                }
                for (key in delta.newlyAddedSpaceLinks) {
                    val source = checkNotNull(links[key])
                    val expected = V289SpaceReminderLinkEntity(
                        source.spaceId, source.reminderId, source.linkedAtMillis,
                    )
                    val current = existingLinks[key]
                    if (current != null) {
                        check(current == expected) {
                            "Imported Space link changed since rollback approval"
                        }
                        check(dao.unlinkReminder(source.spaceId, source.reminderId) == 1) {
                            "Cannot remove confirmed imported Space reminder link"
                        }
                    }
                }
                for (id in delta.newlyAddedSpaceIds) {
                    val source = checkNotNull(spaces[id])
                    val expected = V289SpaceEntity(
                        source.id, source.title, source.type.name, source.iconKey,
                        source.goalText, source.createdAtMillis, source.archivedAtMillis,
                        source.displayOrder,
                    )
                    val current = existingSpaces[id]
                    if (current != null) {
                        check(current == expected) {
                            "Imported Space metadata changed since rollback approval"
                        }
                        check(dao.entriesForSpace(id).isEmpty() &&
                            dao.linksForSpace(id).isEmpty() &&
                            dao.photosForSpace(id).isEmpty()) {
                            "Space contains unexpected owner records; refuse cascading deletion"
                        }
                        check(dao.deleteSpace(id) == 1) {
                            "Could not remove confirmed newly imported Space"
                        }
                    }
                }
                // Guard unrelated owner rows: the caller must have inspected
                // exact original-or-target Room state under the same mutex.
                // Every deletion above belongs to the imported ID delta.
            }
        } finally { db.close() }
    }
}
