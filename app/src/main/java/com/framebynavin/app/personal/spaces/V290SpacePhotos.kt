package com.framebynavin.app.personal.spaces

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * V290 private photo journal metadata. Actual JPEG bytes are saved only in
 * app-private files, never as embedded Room BLOBs or in V289 JSON backups.
 *
 * The first snapshot is the baseline. A comparison becomes eligible when a
 * later snapshot is at least 14 calendar days later in the owner's zone.
 */
@Entity(
    tableName = "personal_space_photos",
    foreignKeys = [
        ForeignKey(
            entity = V289SpaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["spaceId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["spaceId", "takenAtMillis"]),
        Index(value = ["spaceId", "fileName"], unique = true),
    ],
)
internal data class V290SpacePhotoEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val takenAtMillis: Long,
    val addedAtMillis: Long,
    /** Random basename only, never a full path or provider URI. */
    val fileName: String,
    /** Checksum of private re-encoded JPEG bytes. */
    val sha256Hex: String,
    val widthPx: Int,
    val heightPx: Int,
    val note: String,
)

internal data class V290PhotoPair(
    val baseline: V290SpacePhotoEntity,
    val comparison: V290SpacePhotoEntity,
    val daysBetween: Long,
    val meetsFourteenDayGoal: Boolean,
)

/**
 * Date handling is explicit and calendar-based; 14 days is NOT 14 x 24 hours
 * across daylight savings transitions. Displayed dates reflect device zone.
 * No claims of improvement are inferred from a pair of photos.
 */
internal object V290PhotoTimelinePolicy {
    const val MIN_COMPARISON_DAYS = 14L
    private val NAME_PATTERN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg")
    private val HEX_PATTERN = Regex("[0-9a-f]{64}")

    fun validate(photo: V290SpacePhotoEntity): V290SpacePhotoEntity {
        require(runCatching { UUID.fromString(photo.id).toString() == photo.id }.getOrDefault(false))
        require(runCatching { UUID.fromString(photo.spaceId).toString() == photo.spaceId }.getOrDefault(false))
        require(photo.takenAtMillis > 0 && photo.addedAtMillis > 0) {
            "Invalid photo timestamps"
        }
        require(NAME_PATTERN.matches(photo.fileName)) { "Unsafe photo file name" }
        require(HEX_PATTERN.matches(photo.sha256Hex)) { "Photo checksum missing" }
        require(photo.widthPx in 1..2048 && photo.heightPx in 1..2048) {
            "Photo dimensions are not supported"
        }
        require(photo.note.length <= 500) { "Photo note is too long" }
        return photo
    }

    fun compare(
        photos: List<V290SpacePhotoEntity>,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): V290PhotoPair? {
        val valid = photos.filter { photo ->
            photo.takenAtMillis in 1..nowMillis &&
                runCatching { validate(photo) }.isSuccess
        }
        val spaceId = valid.firstOrNull()?.spaceId ?: return null
        require(valid.all { it.spaceId == spaceId }) {
            "Never compare photos from different Spaces"
        }
        val ordered = valid.sortedWith(compareBy<V290SpacePhotoEntity> { it.takenAtMillis }
            .thenBy { it.addedAtMillis }.thenBy { it.id })
        if (ordered.size < 2) return null
        val baseline = ordered.first()
        val candidate = ordered.last()
        val from = Instant.ofEpochMilli(baseline.takenAtMillis).atZone(zone).toLocalDate()
        val to = Instant.ofEpochMilli(candidate.takenAtMillis).atZone(zone).toLocalDate()
        val calendarDays = java.time.temporal.ChronoUnit.DAYS.between(from, to)
        return V290PhotoPair(
            baseline = baseline,
            comparison = candidate,
            daysBetween = calendarDays,
            meetsFourteenDayGoal = calendarDays >= MIN_COMPARISON_DAYS,
        )
    }
}

/** V289 -> V290 adds a table; old spaces, reminders and journal rows stay intact. */
internal object V290SpacePhotosMigration {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `personal_space_photos` (" +
                    "`id` TEXT NOT NULL, " +
                    "`spaceId` TEXT NOT NULL, " +
                    "`takenAtMillis` INTEGER NOT NULL, " +
                    "`addedAtMillis` INTEGER NOT NULL, " +
                    "`fileName` TEXT NOT NULL, " +
                    "`sha256Hex` TEXT NOT NULL, " +
                    "`widthPx` INTEGER NOT NULL, " +
                    "`heightPx` INTEGER NOT NULL, " +
                    "`note` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`spaceId`) REFERENCES `personal_spaces`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_personal_space_photos_spaceId_takenAtMillis` " +
                    "ON `personal_space_photos` (`spaceId`, `takenAtMillis`)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_personal_space_photos_spaceId_fileName` " +
                    "ON `personal_space_photos` (`spaceId`, `fileName`)"
            )
        }
    }
}
