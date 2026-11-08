package com.framebynavin.app.personal

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.framebynavin.app.personal.spaces.V289PersonalSpace
import com.framebynavin.app.personal.spaces.V289SpaceEntry
import com.framebynavin.app.personal.spaces.V289SpaceEntryKind
import com.framebynavin.app.personal.spaces.V289SpaceEntrySource
import com.framebynavin.app.personal.spaces.V289SpaceReminderLink
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Creates a comparable local snapshot without calling Room.open, changing
 * alarm state, touching cloud restore, writing any space rows or importing
 * data. Existing v1/v2 Spaces SQLite is opened READONLY, no migrations.
 */
internal class V301LocalRecoveryReader(context: Context) {
    private val app = context.applicationContext

    /**
     * STRICTLY READ-ONLY crash forensics. A partially installed JPEG or an
     * orphan .restore.pending file must not hide the still-decodable Personal,
     * Challenge, Group, Space and Room-photo metadata after process death.
     *
     * localIntegrityVerified is ALWAYS false: the caller MUST independently
     * authenticate the encrypted escrow, inspect real private photo bytes and
     * check the journal before considering any owner-mediated repair. Normal
     * owner-facing backup / import paths must continue using read().
     */
    suspend fun readForInterruptedRecovery(): V301LocalRecoverySnapshot =
        StabilizationPersonalMutationGate.withStableSnapshot(app) {
            withContext(Dispatchers.IO) {
                // This legacy recovery snapshot cannot authenticate the separate
                // single-activity DataStore; NEVER report complete crash forensics
                // while any of its confirmed owner receipts exist.
                check(Phase1HabitStore(app).load().isEmpty()) {
                    "Legacy Personal recovery excludes single-activity challenge progress. " +
                        "No restore or recovery was performed."
                }
                val personal = PersonalReminderStore(app).exportPersonalJson()
                val challenge = V297ChallengeStore(app).exportJson()
                val group = V298ReminderGroupStore(app).exportJson()
                val history = PersonalCompletionBackupCodec.decode(personal)
                val (spaces, photos) = readSpacesWithoutOpeningRoom(history)
                val spaceState = V303SpaceReceiptArchiveCodec.decode(spaces, history)
                val ids = history.reminders.mapTo(HashSet()) { it.id }
                require(V298ReminderGroupCodec.decode(group).all { candidate ->
                    candidate.reminderIds.all { it in ids }
                }) {
                    "Interrupted local Reminder Groups reference missing Personal data"
                }
                require(spaceState.links.all { it.reminderId in ids }) {
                    "Interrupted local Space links refer to missing Personal data"
                }
                val spaceIds = spaceState.spaces.mapTo(HashSet()) { it.id }
                require(photos.all { it.spaceId in spaceIds }) {
                    "Interrupted Room photo rows refer to missing Spaces"
                }
                V297ChallengeCodec.decode(challenge)
                V301LocalRecoverySnapshot(
                    components = V300PersonalComponents(personal, challenge, group, spaces),
                    photos = photos,
                    localIntegrityVerified = false,
                )
            }
        }

    /**
     * The legacy V301/B03 restore path must NOT suddenly treat new challenge
     * DataStore records as covered. Keep this v1 result and failure behavior.
     */
    suspend fun read(): V301LocalRecoverySnapshot =
        readVerified(includeSingleActivity = false)

    /**
     * Read-only, integrity-checked v2 baseline for the FUTURE owner-reviewed
     * recovery coordinator. The exact Done receipt instant is included under
     * the same stable snapshot lock as all other Personal stores.
     *
     * This is NOT the interrupted-v2-journal recovery path: normal snapshots
     * deliberately refuse to run while ANY recovery marker is present.
     */
    suspend fun readV2(): V301LocalRecoverySnapshot =
        readVerified(includeSingleActivity = true)

    private suspend fun readVerified(
        includeSingleActivity: Boolean,
    ): V301LocalRecoverySnapshot =
        StabilizationPersonalMutationGate.withStableSnapshot(app) {
        withContext(Dispatchers.IO) {
        val habitStore = Phase1HabitStore(app)
        val v2Habits = if (includeSingleActivity) {
            habitStore.exportJson()
        } else {
            // The v1 journal has no v2 stage and its fingerprint excludes
            // every single-activity Done receipt. Never assert completeness.
            check(habitStore.load().isEmpty()) {
                "Legacy Personal restore does not cover single-activity challenge data. " +
                    "Existing progress was left unchanged."
            }
            null
        }
        val verification = when (val inspected = V299PersonalRecoveryPreflight(app).inspect()) {
            is V299RecoveryPreflightResult.Checked -> inspected.counts
            is V299RecoveryPreflightResult.Unavailable ->
                throw IllegalStateException("Cannot confirm local Personal data integrity")
        }
        check(verification.unverifiedPhotoFiles == 0) {
            "Local private photos failed integrity checking"
        }
        val personal = PersonalReminderStore(app).exportPersonalJson()
        val challenges = V297ChallengeStore(app).exportJson()
        val groups = V298ReminderGroupStore(app).exportJson()
        val (spaceMetadata, photos) = readSpacesWithoutOpeningRoom(
            PersonalCompletionBackupCodec.decode(personal))
        val source = V300PersonalComponents(
            personal = personal, challenges = challenges,
            groups = groups, spaces = spaceMetadata,
            singleActivityChallenges = v2Habits,
        )
        val reminderData = PersonalCompletionBackupCodec.decode(personal)
        val spaceData = V303SpaceReceiptArchiveCodec.decode(
            spaceMetadata, reminderData)

        // If either source changes while the read progresses, fail closed,
        // never describe a torn multi-DataStore read as an atomic snapshot.
        check(
            verification.personalReminders == reminderData.reminders.size &&
            verification.completionReceipts == reminderData.completions.size &&
            verification.challenges == V297ChallengeCodec.decode(challenges).size &&
            verification.reminderGroups == V298ReminderGroupCodec.decode(groups).size &&
            verification.spaces == spaceData.spaces.size &&
            verification.journalEntries == spaceData.entries.size &&
            verification.spaceLinks == spaceData.links.size &&
            verification.privatePhotoRecords == photos.size
        ) { "Personal records changed during read; try the preview again" }

        val photoRoot = File(app.filesDir, "backlot-personal-space-photos")
        val expected = photos.mapTo(HashSet()) { photo ->
            File(File(photoRoot, photo.spaceId), photo.fileName).canonicalPath
        }
        val observed = HashSet<String>()
        if (photoRoot.exists()) {
            for (entry in photoRoot.walkTopDown()) {
                if (!entry.isFile) continue
                check(observed.size <= 10_000) { "Too many private photo files" }
                observed.add(entry.canonicalPath)
            }
        }
        V300PrivatePhotoInventoryPolicy.requireExactFiles(expected, observed)
        V301LocalRecoverySnapshot(
            components = source, photos = photos,
            localIntegrityVerified = true,
        )
        }
    }

    /**
     * Existing metadata only. If a current Space contains an unsupported
     * confirmed-Done entry, the existing metadata codec refuses to encode it:
     * an incomplete dry run is worse than a clearly blocked one.
     */
    private fun readSpacesWithoutOpeningRoom(
        personal: PersonalCompletionBackupSnapshot,
    ): Pair<String, List<V290SpacePhotoEntity>> {
        val file = app.getDatabasePath(V289SpacesDatabase.FILE_NAME)
        val photoRoot = File(app.filesDir, "backlot-personal-space-photos")
        if (!file.isFile) {
            check(!photoRoot.exists()) {
                "Space photo files exist without a matching database"
            }
            return V289SpacesMetadataCodec.encode(V289SpaceState()) to emptyList()
        }

        val db = SQLiteDatabase.openDatabase(
            file.absolutePath, null, SQLiteDatabase.OPEN_READONLY,
        )
        return try {
            require(db.version in 1..2) { "Unsupported local Space schema" }
            fun <T> read(sql: String, parse: (Cursor) -> T): List<T> =
                db.rawQuery(sql, null).use { c ->
                    buildList {
                        while (c.moveToNext()) add(parse(c))
                    }
                }

            val spaces = read(
                "SELECT id,title,type,iconKey,goalText,createdAtMillis," +
                    "archivedAtMillis,displayOrder FROM personal_spaces"
            ) { c ->
                V289PersonalSpace(
                    id = c.getString(0),
                    title = c.getString(1),
                    type = V289SpaceType.valueOf(c.getString(2)),
                    iconKey = c.getString(3),
                    goalText = c.getString(4),
                    createdAtMillis = c.getLong(5),
                    archivedAtMillis = if (c.isNull(6)) null else c.getLong(6),
                    displayOrder = c.getInt(7),
                )
            }
            val links = read(
                "SELECT spaceId,reminderId,linkedAtMillis " +
                    "FROM personal_space_reminder_links"
            ) { c ->
                V289SpaceReminderLink(
                    c.getString(0), c.getString(1), c.getLong(2),
                )
            }
            val entries = read(
                "SELECT id,spaceId,kind,source,occurredAtMillis,createdAtMillis," +
                    "note,metricName,metricValue,metricUnit,reminderId," +
                    "originalOccurrenceMillis FROM personal_space_entries"
            ) { c ->
                V289SpaceEntry(
                    id = c.getString(0),
                    spaceId = c.getString(1),
                    kind = V289SpaceEntryKind.valueOf(c.getString(2)),
                    source = V289SpaceEntrySource.valueOf(c.getString(3)),
                    occurredAtMillis = c.getLong(4),
                    createdAtMillis = c.getLong(5),
                    note = c.getString(6),
                    metricName = if (c.isNull(7)) null else c.getString(7),
                    metricValue = if (c.isNull(8)) null else c.getDouble(8),
                    metricUnit = if (c.isNull(9)) null else c.getString(9),
                    reminderId = if (c.isNull(10)) null else c.getString(10),
                    originalOccurrenceMillis = if (c.isNull(11)) null else c.getLong(11),
                )
            }
            val photos = if (db.version == 1) emptyList() else read(
                "SELECT id,spaceId,takenAtMillis,addedAtMillis,fileName," +
                    "sha256Hex,widthPx,heightPx,note FROM personal_space_photos"
            ) { c ->
                V290PhotoTimelinePolicy.validate(V290SpacePhotoEntity(
                    id = c.getString(0),
                    spaceId = c.getString(1),
                    takenAtMillis = c.getLong(2),
                    addedAtMillis = c.getLong(3),
                    fileName = c.getString(4),
                    sha256Hex = c.getString(5),
                    widthPx = c.getInt(6),
                    heightPx = c.getInt(7),
                    note = c.getString(8),
                ))
            }
            val metadata = V303SpaceReceiptArchiveCodec.encode(
                V289SpaceState(spaces, links, entries), personal,
            )
            metadata to photos
        } finally {
            db.close()
        }
    }
}
