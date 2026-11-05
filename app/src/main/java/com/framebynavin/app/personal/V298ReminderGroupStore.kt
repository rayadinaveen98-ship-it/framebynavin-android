package com.framebynavin.app.personal

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Group membership stores only Personal reminder IDs. No reminder is copied,
 * rescheduled, changed or deleted by group operations. Fail closed on corruption.
 */
private val Context.v298GroupsDataStore by preferencesDataStore(
    name = "backlot_personal_reminder_groups_v1",
)
private val groupMutationMutex = Mutex()

internal class V298ReminderGroupStore(context: Context) {
    private val data = context.applicationContext.v298GroupsDataStore
    private val primary = stringPreferencesKey("groups_v1")
    private val lastGood = stringPreferencesKey("groups_last_good_v1")

    val groupsFlow: Flow<List<V298ReminderGroup>> = data.data.map { prefs ->
        val raw = prefs[primary]
        if (raw == null) {
            check(prefs[lastGood] == null) {
                "Group primary missing while recovery copy exists; refusing empty replacement"
            }
            emptyList()
        } else readChecked(raw)
    }
    suspend fun load(): List<V298ReminderGroup> = groupsFlow.first()

    suspend fun create(group: V298ReminderGroup): List<V298ReminderGroup> =
        mutate { current ->
            val checked = group.checked()
            require(current.none { it.id == checked.id }) { "Group ID already exists" }
            current + checked
        }

    suspend fun update(
        id: String,
        transform: (V298ReminderGroup) -> V298ReminderGroup,
    ): List<V298ReminderGroup> = mutate { current ->
        val index = current.indexOfFirst { it.id == id }
        require(index >= 0) { "Group was not found" }
        val replacement = transform(current[index]).checked()
        require(replacement.id == id) { "Group ID may not change" }
        current.toMutableList().also { it[index] = replacement }
    }

    /** Removes the group only; the linked Personal reminders remain in their store. */
    suspend fun deleteGroup(id: String): List<V298ReminderGroup> = mutate { current ->
        require(current.any { it.id == id }) { "Group was not found" }
        current.filterNot { it.id == id }
    }

    suspend fun mutate(
        change: (List<V298ReminderGroup>) -> List<V298ReminderGroup>,
    ): List<V298ReminderGroup> = groupMutationMutex.withLock {
        var result = emptyList<V298ReminderGroup>()
        data.edit { prefs ->
            val oldRaw = prefs[primary]
            val current = if (oldRaw == null) {
                check(prefs[lastGood] == null) {
                    "Group recovery copy exists. Refusing to replace a missing primary."
                }
                emptyList()
            } else readChecked(oldRaw)
            val updated = change(current)
            val serialized = V298ReminderGroupCodec.encode(updated)
            if (updated != current) {
                prefs[lastGood] = oldRaw ?: V298ReminderGroupCodec.encode(emptyList())
                prefs[primary] = serialized
            }
            result = updated
        }
        result
    }

    /** An explicit owner export of groups/IDs only, NOT a complete reminder backup. */
    suspend fun exportJson(): String = groupMutationMutex.withLock {
        val prefs = data.data.first()
        val raw = prefs[primary]
        V298ReminderGroupCodec.encode(if (raw == null) {
            check(prefs[lastGood] == null) {
                "Incomplete group data; cannot export"
            }
            emptyList()
        } else readChecked(raw))
    }

    fun validateImport(raw: String): List<V298ReminderGroup> =
        V298ReminderGroupCodec.decode(raw)

    /** Owner must approve the import preview; never overwrite existing IDs. */
    suspend fun importMissingAfterApproval(raw: String): List<V298ReminderGroup> {
        val incoming = validateImport(raw)
        return mutate { current -> V298ReminderGroupMergePolicy.addMissing(current, incoming) }
    }

    suspend fun validatedPreviousRevision(): List<V298ReminderGroup>? =
        data.data.first()[lastGood]?.let { readChecked(it) }

    private fun readChecked(raw: String): List<V298ReminderGroup> =
        try { V298ReminderGroupCodec.decode(raw) }
        catch (e: Exception) {
            throw IllegalStateException(
                "Reminder groups could not be read. Nothing was changed; an earlier revision may exist.",
                e,
            )
        }
}
