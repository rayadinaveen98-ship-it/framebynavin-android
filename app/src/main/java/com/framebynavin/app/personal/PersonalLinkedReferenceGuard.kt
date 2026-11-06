package com.framebynavin.app.personal

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Prevent an owner-approved, reminder-only cloud/manual restore from silently
 * stranding existing Reminder Group or Room Space reference IDs.
 *
 * This is a READ-ONLY safety check, not a substitute for all-domain restore.
 * Existing custom challenges and private photos are not transferred by cloud.
 */
internal object PersonalLinkedReferencePolicy {
    fun requireRetained(
        incomingReminderIds: Set<String>,
        existingGroupLinks: Set<String>,
        existingSpaceLinks: Set<String>,
    ) {
        require(existingGroupLinks.all { it in incomingReminderIds } &&
            existingSpaceLinks.all { it in incomingReminderIds }) {
            "This Personal copy omits reminders linked to local Spaces or Groups. " +
                "Nothing was restored. Preserve your existing records and use a " +
                "complete verified backup before replacing Personal reminders."
        }
    }
}

internal class PersonalLinkedReferenceGuard(context: Context) {
    private val app = context.applicationContext
    private val groupStore = V298ReminderGroupStore(app)

    suspend fun verifyIncomingIds(incomingReminderIds: Set<String>) {
        // Broken or unreadable stores must fail closed, never be interpreted
        // as an empty set of links. Group names/reminder IDs are not logged.
        val groupLinks = groupStore.load().flatMapTo(HashSet()) { it.reminderIds }
        val spaceLinks = withContext(Dispatchers.IO) {
            val dbFile = app.getDatabasePath(V289SpacesDatabase.FILE_NAME)
            if (!dbFile.exists()) {
                emptySet()
            } else {
                check(dbFile.isFile) { "Existing Spaces database cannot be read" }
                val db = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY,
                )
                try {
                    require(db.version in 1..2) { "Unsupported Space database schema" }
                    db.rawQuery(
                        "SELECT DISTINCT reminderId FROM personal_space_reminder_links",
                        null,
                    ).use { cursor ->
                        buildSet {
                            while (cursor.moveToNext()) {
                                val id = cursor.getString(0)
                                require(!id.isNullOrBlank()) {
                                    "Invalid local Space reminder reference"
                                }
                                add(id)
                            }
                        }
                    }
                } finally {
                    db.close()
                }
            }
        }
        PersonalLinkedReferencePolicy.requireRetained(
            incomingReminderIds, groupLinks, spaceLinks,
        )
    }
}
