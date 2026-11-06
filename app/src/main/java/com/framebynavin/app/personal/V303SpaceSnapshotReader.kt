package com.framebynavin.app.personal

import androidx.room.withTransaction
import com.framebynavin.app.personal.spaces.V289PersonalSpace
import com.framebynavin.app.personal.spaces.V289SpaceEntry
import com.framebynavin.app.personal.spaces.V289SpaceEntryKind
import com.framebynavin.app.personal.spaces.V289SpaceEntrySource
import com.framebynavin.app.personal.spaces.V289SpaceReminderLink
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity

/**
 * Explicit owner-export read within one Room transaction. Unlike V289's
 * standalone metadata export, this includes genuine confirmed Done rows
 * which V303 must cross-check with Personal completion history.
 *
 * Does not write entries, infer progress or change the existing V289 codec.
 */
internal data class V303SpaceSnapshot(
    val state: V289SpaceState,
    val photos: List<V290SpacePhotoEntity>,
)

internal class V303SpaceSnapshotReader(private val db: V289SpacesDatabase) {
    suspend fun read(): V289SpaceState = readWithPhotos().state

    /** Metadata and photo ROWS share one SQLite revision. Bytes are verified later. */
    suspend fun readWithPhotos(): V303SpaceSnapshot = db.withTransaction {
        val dao = db.spaces()
        val spaces = dao.loadSpaces().map { row ->
            V289PersonalSpace(
                id = row.id,
                title = row.title,
                type = V289SpaceType.valueOf(row.type),
                iconKey = row.iconKey,
                goalText = row.goalText,
                createdAtMillis = row.createdAtMillis,
                archivedAtMillis = row.archivedAtMillis,
                displayOrder = row.displayOrder,
            )
        }
        val links = dao.allLinks().map { row ->
            V289SpaceReminderLink(
                spaceId = row.spaceId,
                reminderId = row.reminderId,
                linkedAtMillis = row.linkedAtMillis,
            )
        }
        val entries = dao.allEntries().map { row ->
            V289SpaceEntry(
                id = row.id,
                spaceId = row.spaceId,
                kind = V289SpaceEntryKind.valueOf(row.kind),
                source = V289SpaceEntrySource.valueOf(row.source),
                occurredAtMillis = row.occurredAtMillis,
                createdAtMillis = row.createdAtMillis,
                note = row.note,
                metricName = row.metricName,
                metricValue = row.metricValue,
                metricUnit = row.metricUnit,
                reminderId = row.reminderId,
                originalOccurrenceMillis = row.originalOccurrenceMillis,
            )
        }
        val photos = spaces.flatMap { space ->
            dao.photosForSpace(space.id)
        }
        V303SpaceSnapshot(
            state = V289SpaceState(spaces, links, entries),
            photos = photos,
        )
    }
}
