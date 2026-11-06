package com.framebynavin.app.personal.spaces

import androidx.room.withTransaction
import android.content.Context
import com.framebynavin.app.personal.StabilizationPersonalMutationGate

/**
 * V289 internal opt-in metadata recovery adapter.
 *
 * Not wired to UI/Cloud Sync yet. The explicit owner-file import/export flow
 * is a separate gate. No PersonalReminderStore, alarm, Creator or photo writes.
 *
 * Restore is MERGE-ONLY and transactionally atomic. Existing Space data is
 * never deleted or overwritten: an ID with different data is a hard conflict.
 * References to absent Personal reminders are rejected rather than silently
 * restored as working reminder links.
 */
internal class V289SpacesMetadataRecovery(
    private val context: Context,
    private val database: V289SpacesDatabase,
) {
    private val dao get() = database.spaces()

    suspend fun prepareLocalMetadataExport(): String =
        StabilizationPersonalMutationGate.withStableSnapshot(context) {
        database.withTransaction {
        val spaces = dao.loadSpaces().map { it.toDomain() }
        val links = dao.allLinks().map {
            V289SpaceReminderLink(it.spaceId, it.reminderId, it.linkedAtMillis)
        }
        val entries = dao.allEntries().map { it.toDomain() }
        V289SpacesMetadataCodec.encode(V289SpaceState(spaces, links, entries))
        }
    }

    /** A deliberately explicit caller-provided set, never a guess about alarm state. */
    suspend fun mergeVerifiedMetadata(
        raw: String,
        existingPersonalReminderIds: Set<String>,
    ): V289MetadataImportReport {
        // Verify envelope, checksum, all record bounds and FK relationships
        // BEFORE accessing/writing the database.
        val snapshot = V289SpacesMetadataCodec.decode(raw)
        require(snapshot.links.all { it.reminderId in existingPersonalReminderIds }) {
            "Restore needs Personal reminders to be restored before their Space links"
        }
        return StabilizationPersonalMutationGate.withOwnerMutation(context) {
            database.withTransaction {
            val oldSpaces = dao.loadSpaces().associateBy { it.id }
            val oldLinks = dao.allLinks().associateBy { it.spaceId to it.reminderId }
            val oldEntries = dao.allEntries().associateBy { it.id }

            val pendingSpaces = snapshot.spaces.map { it.toEntity() }.filter { proposed ->
                val existing = oldSpaces[proposed.id]
                require(existing == null || existing == proposed) {
                    "A Space id already exists with different content"
                }
                existing == null
            }
            val pendingLinks = snapshot.links.map {
                V289SpaceReminderLinkEntity(it.spaceId, it.reminderId, it.linkedAtMillis)
            }.filter { proposed ->
                val existing = oldLinks[proposed.spaceId to proposed.reminderId]
                require(existing == null || existing == proposed) {
                    "A Space reminder link already exists with different content"
                }
                existing == null
            }
            val pendingEntries = snapshot.entries.map { it.toEntity() }.filter { proposed ->
                val existing = oldEntries[proposed.id]
                require(existing == null || existing == proposed) {
                    "A Space entry id already exists with different content"
                }
                existing == null
            }

            // Validate all collisions first; no partial inserts on failure.
            pendingSpaces.forEach { dao.insertSpace(it) }
            pendingLinks.forEach {
                check(dao.insertLink(it) != -1L) { "Space link collided during restore" }
            }
            pendingEntries.forEach {
                check(dao.insertEntry(it) != -1L) { "Space entry collided during restore" }
            }
            V289MetadataImportReport(
                spacesAdded = pendingSpaces.size,
                linksAdded = pendingLinks.size,
                entriesAdded = pendingEntries.size,
                alreadyPresent = oldSpaces.size + oldLinks.size + oldEntries.size,
            )
            }
        }
    }
}

internal data class V289MetadataImportReport(
    val spacesAdded: Int,
    val linksAdded: Int,
    val entriesAdded: Int,
    val alreadyPresent: Int,
)

private fun V289SpaceEntity.toDomain(): V289PersonalSpace =
    V289PersonalSpace(
        id = id,
        title = title,
        type = V289SpaceType.valueOf(type),
        iconKey = iconKey,
        goalText = goalText,
        createdAtMillis = createdAtMillis,
        archivedAtMillis = archivedAtMillis,
        displayOrder = displayOrder,
    )

private fun V289PersonalSpace.toEntity(): V289SpaceEntity =
    V289SpaceEntity(
        id, title, type.name, iconKey, goalText, createdAtMillis,
        archivedAtMillis, displayOrder,
    )

private fun V289SpaceEntryEntity.toDomain(): V289SpaceEntry =
    V289SpaceEntry(
        id = id,
        spaceId = spaceId,
        kind = V289SpaceEntryKind.valueOf(kind),
        source = V289SpaceEntrySource.valueOf(source),
        occurredAtMillis = occurredAtMillis,
        createdAtMillis = createdAtMillis,
        note = note,
        metricName = metricName,
        metricValue = metricValue,
        metricUnit = metricUnit,
        reminderId = reminderId,
        originalOccurrenceMillis = originalOccurrenceMillis,
    )

private fun V289SpaceEntry.toEntity(): V289SpaceEntryEntity =
    V289SpaceEntryEntity(
        id, spaceId, kind.name, source.name, occurredAtMillis, createdAtMillis,
        note, metricName, metricValue, metricUnit, reminderId,
        originalOccurrenceMillis,
    )
