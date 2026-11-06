package com.framebynavin.app.personal

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Owner-invoked READ-ONLY preflight. It checks the actual private JPEG bytes,
 * not merely their Room rows. Not a backup, import, cloud push or an alarm action.
 */
internal sealed interface V299RecoveryPreflightResult {
    data class Checked(
        val counts: V299RecoveryCounts,
    ) : V299RecoveryPreflightResult

    data class Unavailable(val safeReason: String) : V299RecoveryPreflightResult
}

internal data class V299RecoveryCounts(
    val personalReminders: Int,
    val completionReceipts: Int,
    val challenges: Int,
    val reminderGroups: Int,
    val spaces: Int,
    val journalEntries: Int,
    val spaceLinks: Int,
    val privatePhotoRecords: Int,
    val verifiedPhotoFiles: Int,
) {
    init {
        require(listOf(
            personalReminders, completionReceipts, challenges, reminderGroups,
            spaces, journalEntries, spaceLinks, privatePhotoRecords, verifiedPhotoFiles,
        ).all { it >= 0 })
        require(verifiedPhotoFiles <= privatePhotoRecords)
    }
    val unverifiedPhotoFiles: Int get() = privatePhotoRecords - verifiedPhotoFiles

    /** A green sync receipt still only covers Creator & Personal reminder domains. */
    val hasDeviceOnlyData: Boolean get() =
        challenges > 0 || reminderGroups > 0 || spaces > 0 ||
            journalEntries > 0 || spaceLinks > 0 || privatePhotoRecords > 0

    val safeToClaimAllPrivatePhotosVerified: Boolean get() =
        unverifiedPhotoFiles == 0

    fun conciseSummary(): String =
        "${personalReminders} Personal reminders, ${completionReceipts} Done receipts; " +
            "${challenges} challenges, ${reminderGroups} groups, ${spaces} Spaces, " +
            "${journalEntries} journal notes, ${spaceLinks} Space links. " +
            "${verifiedPhotoFiles} of ${privatePhotoRecords} recorded private photos verified."

    fun caution(): String = when {
        unverifiedPhotoFiles > 0 ->
            "${unverifiedPhotoFiles} recorded photo(s) are missing or failed integrity verification. " +
                "Do not clear app data or change devices until the originals are recovered."
        hasDeviceOnlyData ->
            "Challenges, groups, Spaces, journal entries, links and photo files are not covered by Sync Now. " +
                "Do not uninstall or clear app data based on this check."
        else ->
            "This is a local read-only inventory, not a verified cloud or portable recovery backup."
    }
}

internal class V299PersonalRecoveryPreflight(context: Context) {
    private val app = context.applicationContext

    suspend fun inspect(): V299RecoveryPreflightResult = withContext(Dispatchers.IO) {
        try {
            val store = PersonalReminderStore(app)
            val personalCount = store.load().size
            val completionCount = store.loadCompletionHistory().size
            val challengeCount = V297ChallengeStore(app).load().size
            val groupCount = V298ReminderGroupStore(app).load().size
            val spaces = inspectSpacesReadonly()
            V299RecoveryPreflightResult.Checked(V299RecoveryCounts(
                personalReminders = personalCount,
                completionReceipts = completionCount,
                challenges = challengeCount,
                reminderGroups = groupCount,
                spaces = spaces.spaces,
                journalEntries = spaces.entries,
                spaceLinks = spaces.links,
                privatePhotoRecords = spaces.photos,
                verifiedPhotoFiles = spaces.verified,
            ))
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            V299RecoveryPreflightResult.Unavailable(
                "Could not verify all Personal records or private photo files. " +
                    "No data was changed; backup coverage remains unknown."
            )
        }
    }

    private data class SpaceCounts(
        val spaces: Int, val entries: Int, val links: Int,
        val photos: Int, val verified: Int,
    )

    private fun inspectSpacesReadonly(): SpaceCounts {
        val path = app.getDatabasePath(V289SpacesDatabase.FILE_NAME)
        val privateRoot = File(app.filesDir, "backlot-personal-space-photos")
        if (!path.exists()) {
            // A photo folder without its metadata DB can be a partial restore.
            check(!privateRoot.exists()) {
                "Private photo folder exists without Spaces DB"
            }
            return SpaceCounts(0, 0, 0, 0, 0)
        }
        val db = SQLiteDatabase.openDatabase(
            path.absolutePath, null, SQLiteDatabase.OPEN_READONLY,
        )
        return try {
            require(db.version in 1..2) { "Unsupported Spaces schema" }
            fun count(table: String): Int {
                require(table in setOf(
                    "personal_spaces", "personal_space_entries",
                    "personal_space_reminder_links", "personal_space_photos",
                ))
                return db.rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
                    check(cursor.moveToFirst()) { "Unreadable Spaces count" }
                    val value = cursor.getInt(0)
                    require(value >= 0) { "Invalid record count" }
                    value
                }
            }
            val nSpaces = count("personal_spaces")
            val nEntries = count("personal_space_entries")
            val nLinks = count("personal_space_reminder_links")
            val nPhotos = if (db.version == 1) 0 else count("personal_space_photos")
            require(nPhotos <= 10_000) { "Too many private photos for verification" }
            var verified = 0
            if (nPhotos > 0) {
                val query = """
                    SELECT id, spaceId, takenAtMillis, addedAtMillis, fileName,
                           sha256Hex, widthPx, heightPx, note
                    FROM personal_space_photos ORDER BY spaceId, id
                """.trimIndent()
                db.rawQuery(query, null).use { cursor ->
                    var seen = 0
                    while (cursor.moveToNext()) {
                        seen++
                        val photo = V290PhotoTimelinePolicy.validate(V290SpacePhotoEntity(
                            id = cursor.getString(0),
                            spaceId = cursor.getString(1),
                            takenAtMillis = cursor.getLong(2),
                            addedAtMillis = cursor.getLong(3),
                            fileName = cursor.getString(4),
                            sha256Hex = cursor.getString(5),
                            widthPx = cursor.getInt(6),
                            heightPx = cursor.getInt(7),
                            note = cursor.getString(8),
                        ))
                        // This verifies actual bytes against metadata SHA-256.
                        val saved = File(
                            File(privateRoot, photo.spaceId), photo.fileName
                        )
                        if (saved.isFile && saved.length() in 1L..V299PhotoIntegrityPolicy.MAX_BYTES &&
                            saved.inputStream().use {
                                V299PhotoIntegrityPolicy.matches(it, photo.sha256Hex)
                            }) verified++
                    }
                    check(seen == nPhotos) { "Photo inventory changed during verification" }
                }
            }
            SpaceCounts(nSpaces, nEntries, nLinks, nPhotos, verified)
        } finally {
            db.close()
        }
    }
}

/**
 * Bounded streaming verification for app-private, re-encoded JPEGs.
 * Memory stays constant even when an owner has thousands of photo records.
 */
internal object V299PhotoIntegrityPolicy {
    const val MAX_BYTES = 12L * 1024L * 1024L
    private val SHA256 = Regex("[0-9a-f]{64}")

    fun matches(stream: InputStream, expectedSha256: String): Boolean {
        require(SHA256.matches(expectedSha256)) { "Invalid photo checksum" }
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        while (true) {
            val n = stream.read(buffer)
            if (n < 0) break
            if (n == 0) continue
            total += n
            if (total > MAX_BYTES) return false
            digest.update(buffer, 0, n)
        }
        if (total == 0L) return false
        return digest.digest().joinToString("") { "%02x".format(it) } == expectedSha256
    }
}
