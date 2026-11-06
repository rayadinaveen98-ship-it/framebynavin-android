package com.framebynavin.app.personal

import android.content.Context
import androidx.room.withTransaction
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.personal.spaces.V289SpaceEntryEntity
import com.framebynavin.app.personal.spaces.V289SpaceReminderLinkEntity
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File

/**
 * Phase B3: isolated additive Room / verified JPEG stage writer.
 *
 * Caller MUST hold a verified, journal-matched recovery scope. This class
 * never creates that authority and must never be called from a screen.
 * Done entries are validated by the V303 attested receipt envelope, NOT the
 * standalone V289 metadata codec (which intentionally forbids Done claims).
 *
 * If interrupted after photo bytes but before its Room row, the persistent
 * recovery journal remains present and blocks ordinary writes. B4 must
 * implement authenticated resume/rollback; do not clear it here.
 */
internal class B03RestoreDomainWriter(context: Context) {
    private val app = context.applicationContext

    suspend fun writeSpaces(attested: V300PersonalComponents) {
        StabilizationPersonalMutationGate.requireActiveRecovery(app)
        val personal = PersonalCompletionBackupCodec.decode(attested.personal)
        val state = V303SpaceReceiptArchiveCodec.decode(attested.spaces, personal)
        val allPersonalIds = personal.reminders.mapTo(HashSet()) { it.id }
        require(state.links.all { it.reminderId in allPersonalIds }) {
            "Incoming Space link has no restored Personal reminder"
        }
        val db = V289SpacesDatabase.open(app)
        try {
            db.withTransaction {
                val dao = db.spaces()
                val currentSpaces = dao.loadSpaces().associateBy { it.id }
                val currentLinks = dao.allLinks().associateBy { it.spaceId to it.reminderId }
                val currentEntries = dao.allEntries().associateBy { it.id }

                val newSpaces = state.spaces.map { space ->
                    V289SpaceEntity(
                        space.id, space.title, space.type.name, space.iconKey,
                        space.goalText, space.createdAtMillis, space.archivedAtMillis,
                        space.displayOrder,
                    )
                }.filter { candidate ->
                    val old = currentSpaces[candidate.id]
                    require(old == null || old == candidate) {
                        "Conflicting Space metadata during recovery"
                    }
                    old == null
                }
                val newLinks = state.links.map { link ->
                    V289SpaceReminderLinkEntity(
                        link.spaceId, link.reminderId, link.linkedAtMillis,
                    )
                }.filter { candidate ->
                    val old = currentLinks[candidate.spaceId to candidate.reminderId]
                    require(old == null || old == candidate) {
                        "Conflicting Space reminder link during recovery"
                    }
                    old == null
                }
                val newEntries = state.entries.map { entry ->
                    V289SpaceEntryEntity(
                        entry.id, entry.spaceId, entry.kind.name, entry.source.name,
                        entry.occurredAtMillis, entry.createdAtMillis, entry.note,
                        entry.metricName, entry.metricValue, entry.metricUnit,
                        entry.reminderId, entry.originalOccurrenceMillis,
                    )
                }.filter { candidate ->
                    val old = currentEntries[candidate.id]
                    require(old == null || old == candidate) {
                        "Conflicting Space journal entry during recovery"
                    }
                    old == null
                }
                newSpaces.forEach { dao.insertSpace(it) }
                newLinks.forEach {
                    check(dao.insertLink(it) != -1L) {
                        "Space reminder link collided during recovery"
                    }
                }
                newEntries.forEach {
                    check(dao.insertEntry(it) != -1L) {
                        "Space entry collided during recovery"
                    }
                }
            }
        } finally {
            db.close()
        }
    }

    suspend fun writePhotos(
        target: List<V290SpacePhotoEntity>,
        originals: List<V290SpacePhotoEntity>,
        authenticated: List<V300VerifiedPhoto>,
    ) {
        StabilizationPersonalMutationGate.requireActiveRecovery(app)
        val oldIds = originals.mapTo(HashSet()) { it.id }
        val authenticatedById = authenticated.associateBy { it.photo.id }
        require(authenticatedById.size == authenticated.size) {
            "Duplicated incoming private photo identity"
        }
        val newPhotos = target.filter { it.id !in oldIds }
        require(newPhotos.all { it.id in authenticatedById }) {
            "New private photo lacks reauthenticated staged bytes"
        }
        val installer = StabilizationVerifiedPhotoInstaller(
            File(app.filesDir, "backlot-personal-space-photos"),
        )
        val db = V289SpacesDatabase.open(app)
        try {
            for (photo in newPhotos) {
                StabilizationPersonalMutationGate.requireActiveRecovery(app)
                val source = checkNotNull(authenticatedById[photo.id])
                require(source.photo == photo) {
                    "Photo metadata differs from the authenticated archive"
                }
                // CREATE_NEW never replaces existing owner's JPEG. It either
                // verifies already-installed bytes or durably adds new bytes.
                installer.installVerified(photo, source.stagedFile, source.sizeBytes)
                db.withTransaction {
                    val dao = db.spaces()
                    val current = dao.getPhoto(photo.id)
                    require(current == null || current == photo) {
                        "Conflicting private photo record during recovery"
                    }
                    if (current == null) {
                        check(dao.getSpace(photo.spaceId) != null) {
                            "Private photo has no restored Space"
                        }
                        dao.insertPhoto(photo)
                    }
                }
            }
        } finally {
            db.close()
        }
    }
}
