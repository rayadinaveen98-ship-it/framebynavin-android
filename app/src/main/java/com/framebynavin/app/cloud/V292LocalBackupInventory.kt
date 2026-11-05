package com.framebynavin.app.cloud

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Read-only inventory for the owner's Backup & Sync screen.
 *
 * This is NOT an upload/restore operation or an assurance that private JPEG
 * files can be recovered. A Room photo row is only a record, not a verified
 * media byte copy. Do not open or create an unused Spaces DB simply because
 * the user visited Backup & Sync.
 */
internal sealed interface V292LocalBackupInventory {
    data object NoSpacesDatabase : V292LocalBackupInventory
    data class Recorded(
        val spaceCount: Int,
        val journalEntryCount: Int,
        val photoRecordCount: Int,
    ) : V292LocalBackupInventory {
        init {
            require(spaceCount >= 0 && journalEntryCount >= 0 && photoRecordCount >= 0)
        }
        val hasUnprotectedRecords: Boolean get() =
            spaceCount > 0 || journalEntryCount > 0 || photoRecordCount > 0
    }
    data object Unavailable : V292LocalBackupInventory
}

/** Pure presentation policy; accurate only for successfully enumerated local records. */
internal object V292BackupInventoryCopy {
    fun summary(inventory: V292LocalBackupInventory): String? = when (inventory) {
        V292LocalBackupInventory.NoSpacesDatabase -> null
        V292LocalBackupInventory.Unavailable ->
            "Local Spaces couldn't be checked. Cloud backup completeness is unknown."
        is V292LocalBackupInventory.Recorded ->
            if (!inventory.hasUnprotectedRecords) null
            else "On this phone only: ${inventory.spaceCount} Spaces, " +
                "${inventory.journalEntryCount} journal entries and " +
                "${inventory.photoRecordCount} private photo records. " +
                "None of these are included in Sync Now. Photo files weren't verified."
    }
}

internal class V292LocalBackupInventoryReader(context: Context) {
    private val app = context.applicationContext

    /**
     * Fail closed: if Room cannot be read, report Unavailable rather than zero.
     * The query does not read photo bytes, notes, reminder titles or auth tokens,
     * and has no account/cloud/network side effects.
     */
    suspend fun inspect(): V292LocalBackupInventory = withContext(Dispatchers.IO) {
        try {
            if (!app.getDatabasePath(V289SpacesDatabase.FILE_NAME).exists()) {
                return@withContext V292LocalBackupInventory.NoSpacesDatabase
            }
            // OPEN_READONLY is essential: Room.open() may migrate a V289
            // database even when the caller only wants to count rows.
            val db = SQLiteDatabase.openDatabase(
                app.getDatabasePath(V289SpacesDatabase.FILE_NAME).absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
            try {
                val version = db.version
                check(version == 1 || version == 2) {
                    "Unsupported Spaces database version"
                }
                fun count(table: String): Int {
                    // Table names are internal hard-coded identifiers only.
                    check(table in setOf(
                        "personal_spaces", "personal_space_entries",
                        "personal_space_photos"
                    ))
                    return db.rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
                        check(cursor.moveToFirst()) { "Count query failed" }
                        val value = cursor.getInt(0)
                        check(value >= 0) { "Negative inventory count" }
                        value
                    }
                }
                val spaces = count("personal_spaces")
                val entries = count("personal_space_entries")
                val photos = if (version == 1) 0
                    else count("personal_space_photos")
                V292LocalBackupInventory.Recorded(spaces, entries, photos)
            } finally {
                db.close()
            }
        } catch (_: Exception) {
            V292LocalBackupInventory.Unavailable
        }
    }
}
