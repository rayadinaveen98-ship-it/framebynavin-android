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
import org.json.JSONArray

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
    // Atomic tombstones for a post-commit Android cleanup that may be interrupted.
    private val pendingDeleteCleanupKey = stringPreferencesKey("personal_delete_cleanup_pending_v1")

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

    /**
     * One owner-reviewed transaction. No partial delete on a stale selection.
     * The tombstones are committed in the SAME DataStore edit as the removals,
     * so the next health worker can cancel orphaned OS leases after a crash.
     */
    suspend fun deleteManyReviewed(
        tickets: Collection<PersonalDeletionTicket>,
    ): List<PersonalReminder> {
        val selected = tickets.toList()
        return mutate(
            deletionCleanupIds = selected.map { it.id }.toSet(),
            redactDeletedRecoveryCopies = true,
        ) { current ->
            PersonalBulkDeletePolicy.deleteReviewed(current, selected)
        }
    }

    suspend fun pendingDeletionCleanupIds(): Set<String> {
        val raw = data.data.first()[pendingDeleteCleanupKey]
        return parseDeletionCleanup(raw)
    }

    suspend fun markDeletionCleanupFinished(ids: Set<String>) {
        if (ids.isEmpty()) return
        StabilizationPersonalMutationGate.withOwnerMutation(app) {
            personalReminderMutationMutex.withLock {
                data.edit { prefs ->
                    val pending = parseDeletionCleanup(prefs[pendingDeleteCleanupKey])
                    val remaining = pending - ids
                    if (remaining != pending) {
                        prefs[pendingDeleteCleanupKey] = encodeDeletionCleanup(remaining)
                    }
                }
            }
        }
    }

    private fun parseDeletionCleanup(raw: String?): Set<String> {
        if (raw == null) return emptySet()
        val rows = JSONArray(raw)
        require(rows.length() <= 1000) { "Too many pending Personal cleanup IDs" }
        return (0 until rows.length()).map { rows.getString(it) }.toSet().also { ids ->
            require(ids.all { it.matches(Regex("[a-zA-Z0-9_:.-]{1,128}")) }) {
                "Invalid Personal alarm cleanup ID"
            }
            require(ids.size == rows.length()) { "Duplicate Personal alarm cleanup IDs" }
        }
    }

    private fun encodeDeletionCleanup(ids: Set<String>): String {
        require(ids.size <= 1000) { "Too many pending Personal cleanup IDs" }
        return JSONArray().also { array -> ids.sorted().forEach { array.put(it) } }.toString()
    }

    suspend fun mutate(
        onDoneAccepted: ((List<PersonalReminder>, List<PersonalReminder>) -> PersonalCompletionEvent?)? = null,
        replacementHistory: List<PersonalCompletionEvent>? = null,
        beforeReplace: ((List<PersonalReminder>, List<PersonalCompletionEvent>) -> Unit)? = null,
        deletionCleanupIds: Set<String> = emptySet(),
        redactDeletedRecoveryCopies: Boolean = false,
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
                if (deletionCleanupIds.isNotEmpty()) {
                    val actuallyRemoved = previous.map { it.id }.toSet() -
                        updated.map { it.id }.toSet()
                    require(actuallyRemoved == deletionCleanupIds) {
                        "Bulk deletion must remove exactly the confirmed Personal IDs"
                    }
                    val olderPending = parseDeletionCleanup(prefs[pendingDeleteCleanupKey])
                    prefs[pendingDeleteCleanupKey] =
                        encodeDeletionCleanup(olderPending + deletionCleanupIds)
                }
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
                        prefs[lastGoodKey] = if (redactDeletedRecoveryCopies) encoded
                            else currentRaw ?: PersonalReminderCodec.encode(emptyList())
                        prefs[mainKey] = encoded
                    }
                    if (historyChanged) {
                        prefs[completionLastGoodKey] = if (redactDeletedRecoveryCopies)
                            nextHistoryRaw else previousHistoryRaw ?: nextHistoryRaw
                        prefs[completionKey] = nextHistoryRaw
                    } else if (redactDeletedRecoveryCopies) {
                        // A previous revision must not resurrect deleted reminder
                        // completion events during a future recovery review.
                        prefs[completionLastGoodKey] = nextHistoryRaw
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

    /**
     * Task H3.3 original Personal inverse. Preserve the authenticated
     * original reminders and exact Done receipts, but make the LOCAL
     * last-good revision equal to the verified original—not the imported
     * target—so the target cannot resurrect after Task 4 releases H4.
     */
    internal suspend fun replaceFromApprovedV2OriginalRollback(
        original: String,
    ): List<PersonalReminder> {
        StabilizationPersonalMutationGate.requireActiveRecovery(app)
        val authenticated = PersonalCompletionBackupCodec.decode(original)
        return mutate(
            replacementHistory = authenticated.completions,
            redactDeletedRecoveryCopies = true,
        ) { authenticated.reminders }
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
