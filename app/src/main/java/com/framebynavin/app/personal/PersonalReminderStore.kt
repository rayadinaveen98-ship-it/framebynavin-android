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
    private val completionKey = stringPreferencesKey("personal_done_history_v1")
    private val completionLastGoodKey = stringPreferencesKey("personal_done_history_last_good_v1")

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

    /** Local-only, private, read-only completion history. Fail closed if damaged. */
    val completionHistoryFlow: Flow<List<PersonalCompletionEvent>> = data.data.map { prefs ->
        prefs[completionKey]?.let(PersonalCompletionJournal::decode) ?: emptyList()
    }

    suspend fun loadCompletionHistory(): List<PersonalCompletionEvent> = completionHistoryFlow.first()

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

    suspend fun mutate(
        onDoneAccepted: ((List<PersonalReminder>, List<PersonalReminder>) -> PersonalCompletionEvent?)? = null,
        replacementHistory: List<PersonalCompletionEvent>? = null,
        beforeReplace: ((List<PersonalReminder>, List<PersonalCompletionEvent>) -> Unit)? = null,
        change: (List<PersonalReminder>) -> List<PersonalReminder>,
    ): List<PersonalReminder> =
        StabilizationPersonalMutationGate.withOwnerMutation(app) {
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
                val previousHistoryRaw = prefs[completionKey]
                // Fail closed on damaged Personal history; never silently overwrite it.
                val previousHistory = previousHistoryRaw?.let(PersonalCompletionJournal::decode) ?: emptyList()
                val doneReceipt = if (updated != previous) onDoneAccepted?.invoke(previous, updated) else null
                val existingIds = updated.mapTo(HashSet()) { it.id }
                val nextHistory = when {
                    replacementHistory != null -> replacementHistory
                    doneReceipt != null -> PersonalCompletionJournal.append(previousHistory, doneReceipt)
                    else -> previousHistory.filter { it.reminderId in existingIds }
                }
                // Check orphan references and format before recording or replacing.
                val nextHistoryRaw = PersonalCompletionJournal.encode(nextHistory)
                require(nextHistory.all { it.reminderId in existingIds }) {
                    "Completion history must reference retained Personal reminders"
                }
                val remindersChanged = updated != previous
                val historyChanged = nextHistory != previousHistory
                if (remindersChanged || historyChanged) {
                    val encoded = PersonalReminderCodec.encode(updated)
                    // Independent recovery copy must be durable BEFORE the atomic replacement.
                    beforeReplace?.invoke(previous, previousHistory)
                    if (remindersChanged) {
                        if (currentRaw != null) prefs[lastGoodKey] = currentRaw
                        else prefs[lastGoodKey] = PersonalReminderCodec.encode(emptyList())
                        prefs[mainKey] = encoded
                    }
                    if (historyChanged) {
                        if (previousHistoryRaw != null) prefs[completionLastGoodKey] = previousHistoryRaw
                        prefs[completionKey] = nextHistoryRaw
                    }
                }
                committed = updated
            }
            committed
            }
        }

    suspend fun exportPersonalJson(): String = personalReminderMutationMutex.withLock {
        val prefs = data.data.first()
        val records = when (val raw = prefs[mainKey]) {
            null -> {
                check(prefs[lastGoodKey] == null) {
                    "Personal primary missing; refuse to export an incomplete backup"
                }
                emptyList()
            }
            else -> readChecked(raw)
        }
        val history = prefs[completionKey]?.let(PersonalCompletionJournal::decode) ?: emptyList()
        PersonalCompletionBackupCodec.encode(records, history)
    }

    /** Caller must show a preview and obtain explicit consent before replacement. */
    fun validateImport(raw: String): List<PersonalReminder> =
        PersonalCompletionBackupCodec.decode(raw).reminders

    suspend fun replaceFromApprovedImport(raw: String): List<PersonalReminder> {
        val replacement = PersonalCompletionBackupCodec.decode(raw)
        return mutate(replacementHistory = replacement.completions) { replacement.reminders }
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
