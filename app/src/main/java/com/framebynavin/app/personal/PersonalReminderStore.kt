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

/** Separate file. No CreatorTask, CreatorBackupManager, cloud sync or YouTube dependency. */
private val Context.personalReminderDataStore by preferencesDataStore(name = "backlot_personal_reminders_v1")
private val personalReminderMutationMutex = Mutex()

/**
 * Atomic DataStore writes: validate existing primary before overwriting it,
 * retain one verified previous revision, and never turn corruption into an
 * empty list. All personal mutations serialize within this process.
 *
 * V223 foundation ONLY: no reminder delivery is armed by these writes.
 */
internal class PersonalReminderStore(context: Context) {
    private val app = context.applicationContext
    private val data = app.personalReminderDataStore
    private val mainKey = stringPreferencesKey("personal_records_v1")
    private val lastGoodKey = stringPreferencesKey("personal_records_last_good_v1")

    val remindersFlow: Flow<List<PersonalReminder>> = data.data.map { prefs ->
        when (val raw = prefs[mainKey]) {
            null -> {
                check(prefs[lastGoodKey] == null) {
                    "Personal reminder primary missing; recovery copy retained"
                }
                emptyList()
            }
            else -> readChecked(raw)
        }
    }

    suspend fun load(): List<PersonalReminder> = remindersFlow.first()

    suspend fun create(reminder: PersonalReminder): List<PersonalReminder> =
        mutate { current ->
            val validated = reminder.checked()
            require(current.none { it.id == validated.id }) { "Personal reminder ID already exists" }
            current + validated
        }

    suspend fun update(id: String, change: (PersonalReminder) -> PersonalReminder): List<PersonalReminder> =
        mutate { current ->
            val index = current.indexOfFirst { it.id == id }
            require(index >= 0) { "Personal reminder not found" }
            val replacement = change(current[index]).checked()
            require(replacement.id == id) { "Personal reminder identity cannot change" }
            current.toMutableList().also { it[index] = replacement }
        }

    suspend fun delete(id: String): List<PersonalReminder> =
        mutate { current -> current.filterNot { it.id == id } }

    suspend fun mutate(change: (List<PersonalReminder>) -> List<PersonalReminder>): List<PersonalReminder> =
        personalReminderMutationMutex.withLock {
            var committed = emptyList<PersonalReminder>()
            data.edit { prefs ->
                val currentRaw = prefs[mainKey]
                val previous = when (currentRaw) {
                    null -> {
                        check(prefs[lastGoodKey] == null) {
                            "Personal reminder primary missing; refuse to overwrite recovery data"
                        }
                        emptyList()
                    }
                    else -> readChecked(currentRaw)
                }
                val updated = change(previous)
                if (updated != previous) {
                    val encoded = PersonalReminderCodec.encode(updated)
                    if (currentRaw != null) prefs[lastGoodKey] = currentRaw
                    else prefs[lastGoodKey] = PersonalReminderCodec.encode(emptyList())
                    prefs[mainKey] = encoded
                }
                committed = updated
            }
            committed
        }

    suspend fun exportPersonalJson(): String = personalReminderMutationMutex.withLock {
        PersonalReminderCodec.encode(load())
    }

    /** Caller must show a preview and obtain explicit consent before replacement. */
    fun validateImport(raw: String): List<PersonalReminder> = PersonalReminderCodec.decode(raw)

    suspend fun replaceFromApprovedImport(raw: String): List<PersonalReminder> {
        val replacement = PersonalReminderCodec.decode(raw)
        return mutate { replacement }
    }

    /** Read-only recovery data, never auto-overwrite a damaged primary. */
    suspend fun validatedPreviousRevision(): List<PersonalReminder>? {
        val prior = data.data.first()[lastGoodKey] ?: return null
        return readChecked(prior)
    }

    private fun readChecked(raw: String): List<PersonalReminder> = try {
        PersonalReminderCodec.decode(raw)
    } catch (e: Exception) {
        throw IllegalStateException(
            "Personal reminder data could not be read. No records were changed; a previous revision may be available.",
            e,
        )
    }
}
