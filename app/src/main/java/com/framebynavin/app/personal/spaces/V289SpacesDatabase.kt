package com.framebynavin.app.personal.spaces

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Separate app-private database. It never opens or migrates Personal reminder
 * DataStore and is not initialized by Home/notifications in V289 foundation.
 * The owner must explicitly create a Space before any rows are inserted.
 */
@Entity(
    tableName = "personal_spaces",
    indices = [Index(value = ["displayOrder"]), Index(value = ["createdAtMillis"])],
)
internal data class V289SpaceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val iconKey: String,
    val goalText: String,
    val createdAtMillis: Long,
    val archivedAtMillis: Long?,
    val displayOrder: Int,
)

@Entity(
    tableName = "personal_space_reminder_links",
    primaryKeys = ["spaceId", "reminderId"],
    foreignKeys = [
        ForeignKey(
            entity = V289SpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["reminderId"])],
)
internal data class V289SpaceReminderLinkEntity(
    val spaceId: String,
    val reminderId: String,
    val linkedAtMillis: Long,
)

@Entity(
    tableName = "personal_space_entries",
    foreignKeys = [
        ForeignKey(
            entity = V289SpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["spaceId", "occurredAtMillis"]),
        // SQLite treats NULL index members as distinct; manual entries have
        // no receipt and remain independent, while real Done receipts dedupe.
        Index(
            value = ["spaceId", "reminderId", "originalOccurrenceMillis", "source"],
            unique = true,
        ),
    ],
)
internal data class V289SpaceEntryEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val kind: String,
    val source: String,
    val occurredAtMillis: Long,
    val createdAtMillis: Long,
    val note: String,
    val metricName: String?,
    val metricValue: Double?,
    val metricUnit: String?,
    val reminderId: String?,
    val originalOccurrenceMillis: Long?,
)

@Dao
internal interface V289SpacesDao {
    @Query("SELECT * FROM personal_spaces ORDER BY (archivedAtMillis IS NOT NULL) ASC, displayOrder ASC, createdAtMillis ASC")
    fun observeSpaces(): Flow<List<V289SpaceEntity>>

    @Query("SELECT * FROM personal_spaces ORDER BY (archivedAtMillis IS NOT NULL) ASC, displayOrder ASC, createdAtMillis ASC")
    suspend fun loadSpaces(): List<V289SpaceEntity>

    @Query("SELECT * FROM personal_spaces WHERE id = :id LIMIT 1")
    suspend fun getSpace(id: String): V289SpaceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSpace(space: V289SpaceEntity)

    @Query("UPDATE personal_spaces SET title = :title, goalText = :goalText, iconKey = :iconKey WHERE id = :id")
    suspend fun updateSpaceDetails(id: String, title: String, goalText: String, iconKey: String): Int

    @Query("UPDATE personal_spaces SET archivedAtMillis = :archivedAtMillis WHERE id = :id")
    suspend fun setArchivedAt(id: String, archivedAtMillis: Long?): Int

    @Query("DELETE FROM personal_spaces WHERE id = :id")
    suspend fun deleteSpace(id: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(link: V289SpaceReminderLinkEntity): Long

    @Query("DELETE FROM personal_space_reminder_links WHERE spaceId = :spaceId AND reminderId = :reminderId")
    suspend fun unlinkReminder(spaceId: String, reminderId: String): Int

    @Query("SELECT * FROM personal_space_reminder_links WHERE spaceId = :spaceId ORDER BY linkedAtMillis")
    suspend fun linksForSpace(spaceId: String): List<V289SpaceReminderLinkEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entry: V289SpaceEntryEntity): Long

    @Query("SELECT * FROM personal_space_entries WHERE spaceId = :spaceId ORDER BY occurredAtMillis DESC, createdAtMillis DESC")
    suspend fun entriesForSpace(spaceId: String): List<V289SpaceEntryEntity>

    @Query("SELECT * FROM personal_space_photos WHERE spaceId = :spaceId ORDER BY takenAtMillis ASC, id ASC")
    suspend fun photosForSpace(spaceId: String): List<V290SpacePhotoEntity>

    @Query("SELECT * FROM personal_space_photos WHERE id = :photoId LIMIT 1")
    suspend fun getPhoto(photoId: String): V290SpacePhotoEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPhoto(photo: V290SpacePhotoEntity)

    @Query("DELETE FROM personal_space_photos WHERE id = :photoId")
    suspend fun deletePhoto(photoId: String): Int

    /** Internal metadata snapshots only; excludes photo file data by construction. */
    @Query("SELECT * FROM personal_space_reminder_links ORDER BY spaceId, reminderId")
    suspend fun allLinks(): List<V289SpaceReminderLinkEntity>

    @Query("SELECT * FROM personal_space_entries ORDER BY spaceId, id")
    suspend fun allEntries(): List<V289SpaceEntryEntity>
}

@Database(
    entities = [
        V289SpaceEntity::class,
        V289SpaceReminderLinkEntity::class,
        V289SpaceEntryEntity::class,
        V290SpacePhotoEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
internal abstract class V289SpacesDatabase : RoomDatabase() {
    abstract fun spaces(): V289SpacesDao

    companion object {
        const val FILE_NAME = "backlot_personal_spaces_v1.db"

        fun open(context: Context): V289SpacesDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                V289SpacesDatabase::class.java,
                FILE_NAME,
            ).addMigrations(V290SpacePhotosMigration.MIGRATION_1_2)
                .build() // Preserve all V289 Space, reminder-link and journal data.
    }
}

/** Validation gate: only owner-initiated Space writes, without touching alarms. */
internal class V289SpacesRepository(private val dao: V289SpacesDao) {
    suspend fun createSpace(space: V289PersonalSpace) {
        val checked = V289PersonalSpacePolicy.validateSpace(space)
        dao.insertSpace(
            V289SpaceEntity(
                checked.id, checked.title, checked.type.name, checked.iconKey,
                checked.goalText, checked.createdAtMillis, checked.archivedAtMillis,
                checked.displayOrder,
            )
        )
    }

    suspend fun editSpace(
        original: V289PersonalSpace,
        title: String,
        goalText: String,
        iconKey: String,
    ): Boolean {
        val replacement = V289PersonalSpacePolicy.validateSpace(
            original.copy(title = title, goalText = goalText, iconKey = iconKey)
        )
        return dao.updateSpaceDetails(
            replacement.id, replacement.title, replacement.goalText, replacement.iconKey
        ) == 1
    }

    suspend fun archiveSpace(space: V289PersonalSpace, nowMillis: Long): Boolean {
        val archived = V289PersonalSpacePolicy.archive(space, nowMillis)
        return dao.setArchivedAt(archived.id, archived.archivedAtMillis) == 1
    }

    suspend fun unarchiveSpace(id: String): Boolean =
        dao.setArchivedAt(id, null) == 1

    /**
     * Fail closed if a Space still has private photo files. The caller must
     * explicitly delete/clean photo files before deleting the Space record.
     * Avoid a silent Room FK cascade that would leave untracked private bytes.
     */
    suspend fun deleteSpaceAfterOwnerConfirmation(id: String): Boolean {
        check(dao.photosForSpace(id).isEmpty()) {
            "Remove and clean up Space photos before deleting this Space"
        }
        return dao.deleteSpace(id) == 1
    }

    suspend fun linkExistingReminder(
        link: V289SpaceReminderLink,
        existingReminderIds: Set<String>,
    ): Boolean {
        val checked = V289PersonalSpacePolicy.validateLink(link)
        require(checked.reminderId in existingReminderIds) {
            "Only an existing Personal reminder can be linked"
        }
        return dao.insertLink(
            V289SpaceReminderLinkEntity(
                checked.spaceId, checked.reminderId, checked.linkedAtMillis
            )
        ) != -1L
    }

    /** V289 only accepts manual entries; no automatic notification-to-Done import. */
    suspend fun logManualEntry(entry: V289SpaceEntry): Boolean {
        require(entry.source == V289SpaceEntrySource.MANUAL) {
            "Reminder Done receipts are not enabled in V289"
        }
        val checked = V289PersonalSpacePolicy.validateEntry(entry)
        return dao.insertEntry(
            V289SpaceEntryEntity(
                checked.id, checked.spaceId, checked.kind.name, checked.source.name,
                checked.occurredAtMillis, checked.createdAtMillis, checked.note,
                checked.metricName, checked.metricValue, checked.metricUnit,
                checked.reminderId, checked.originalOccurrenceMillis,
            )
        ) != -1L
    }
}
