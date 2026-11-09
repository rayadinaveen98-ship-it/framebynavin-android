package com.framebynavin.app.personal

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * New 21-day ONE-activity plans use a completely separate preference DataStore
 * from legacy V297 challenges, existing Personal reminders, Spaces and Groups.
 *
 * This is a deliberately unconnected, internal persistence adapter: no screen,
 * alarm, backup, cloud service or migration invokes it yet. Before wiring it
 * into a UI, the scheduler must atomically reconcile owner-selected alerts.
 *
 * Never treats a missing/corrupt primary as an empty replacement when a
 * previous valid revision exists; no implicit cloud restore or overwrite.
 */
private val Context.phase1HabitDataStore by preferencesDataStore(
    name = "backlot_personal_single_activity_habits_v1",
)
private val phase1HabitMutex = Mutex()

internal class Phase1HabitStore(context: Context) {
    private val app = context.applicationContext
    private val data = app.phase1HabitDataStore
    private val primary = stringPreferencesKey("single_activity21_v1")
    private val lastGood = stringPreferencesKey("single_activity21_last_good_v1")

    val challengesFlow: Flow<List<Phase1HabitChallenge>> = data.data.map { prefs ->
        val raw = prefs[primary]
        if (raw == null) {
            check(prefs[lastGood] == null) {
                "Single-activity challenge recovery copy exists; refusing empty replacement."
            }
            emptyList()
        } else readChecked(raw)
    }

    suspend fun load(): List<Phase1HabitChallenge> = challengesFlow.first()

    /** Owner-confirmed creation; does not schedule an alarm or send a notification. */
    suspend fun create(challenge: Phase1HabitChallenge): List<Phase1HabitChallenge> =
        mutate { current ->
            val valid = challenge.checked()
            require(current.none { it.id == valid.id }) {
                "Challenge ID already exists"
            }
            current + valid
        }

    /**
     * Immutable activity, date range, zone and alert policy across progress edits.
     * A later explicit edit flow must coordinate changes with real scheduling and
     * show the owner how changing the activity affects previous Done receipts.
     */
    suspend fun updateProgress(
        id: String,
        change: (Phase1HabitChallenge) -> Phase1HabitChallenge,
    ): List<Phase1HabitChallenge> = mutate { current ->
        val index = current.indexOfFirst { it.id == id }
        require(index >= 0) { "Challenge not found" }
        val before = current[index]
        val after = change(before).checked()
        require(after.id == before.id && after.startDate == before.startDate &&
            after.activity == before.activity && after.zoneId == before.zoneId &&
            after.alertTime == before.alertTime && after.alertKind == before.alertKind
        ) {
            "Progress updates cannot silently change challenge identity or alert schedule"
        }
        current.toMutableList().apply { set(index, after) }
    }

    suspend fun confirmToday(id: String, at: Instant): List<Phase1HabitChallenge> =
        updateProgress(id) { it.confirmToday(at) }

    /** Flexible-only past-day receipt; confirmedAt stays the REAL current time. */
    suspend fun confirmSelectedDay(
        id: String, day: LocalDate, at: Instant,
    ): List<Phase1HabitChallenge> = updateProgress(id) { it.confirmDay(day, at) }

    /** A journal note NEVER creates a completion receipt or changes its time. */
    suspend fun saveDayNote(
        id: String, day: LocalDate, note: String,
    ): List<Phase1HabitChallenge> = updateProgress(id) { it.noteForDay(day, note) }

    suspend fun changeCompletionMode(
        id: String, newMode: Phase1HabitCompletionMode,
    ): List<Phase1HabitChallenge> = updateProgress(id) { before ->
        before.copy(completionMode = newMode).checked()
    }

    suspend fun updateLinks(
        id: String, spaceId: String?, groupId: String?,
        validSpaceIds: Set<String>, validGroupIds: Set<String>,
    ): List<Phase1HabitChallenge> {
        require(spaceId == null || spaceId in validSpaceIds) {
            "The selected Space is no longer available"
        }
        require(groupId == null || groupId in validGroupIds) {
            "The selected reminder Group is no longer available"
        }
        return updateProgress(id) {
            it.copy(linkedSpaceId = spaceId, linkedGroupId = groupId).checked()
        }
    }

    suspend fun undoDay(id: String, day: LocalDate): List<Phase1HabitChallenge> =
        updateProgress(id) { it.undoDay(day) }

    /**
     * Future UI must reconcile alerts when toggling pause, before claiming an
     * alert was canceled. No scheduling occurs in this unconnected adapter.
     */
    suspend fun setPaused(id: String, paused: Boolean): List<Phase1HabitChallenge> =
        updateProgress(id) { it.setPaused(paused) }

    private suspend fun mutate(
        change: (List<Phase1HabitChallenge>) -> List<Phase1HabitChallenge>,
    ): List<Phase1HabitChallenge> = StabilizationPersonalMutationGate.withOwnerMutation(app) {
        phase1HabitMutex.withLock {
            var result = emptyList<Phase1HabitChallenge>()
            data.edit { prefs ->
                val previousRaw = prefs[primary]
                val previous = if (previousRaw == null) {
                    check(prefs[lastGood] == null) {
                        "Challenge primary missing, recovery copy present; no data changed."
                    }
                    emptyList()
                } else readChecked(previousRaw)
                val updated = change(previous)
                val encoded = Phase1HabitCodec.encode(updated)
                if (updated != previous) {
                    prefs[lastGood] = previousRaw ?: Phase1HabitCodec.encode(emptyList())
                    prefs[primary] = encoded
                }
                result = updated
            }
            result
        }
    }

    suspend fun exportJson(): String = phase1HabitMutex.withLock {
        val prefs = data.data.first()
        val raw = prefs[primary]
        val challenges = if (raw == null) {
            check(prefs[lastGood] == null) { "Cannot export incomplete challenge data." }
            emptyList()
        } else readChecked(raw)
        Phase1HabitCodec.encode(challenges)
    }

    /** Read-only import preview. Never imports without separate owner approval. */
    fun validateImport(raw: String): List<Phase1HabitChallenge> =
        Phase1HabitCodec.decode(raw)

    /**
     * Explicitly approved additive import: keep all existing challenge records
     * and exact completion receipts where IDs collide. Never replace old plans.
     */
    suspend fun importMissingAfterApproval(raw: String): List<Phase1HabitChallenge> {
        val incoming = validateImport(raw)
        return mutate { current -> Phase1HabitMergePolicy.addMissing(current, incoming) }
    }

    suspend fun validatedPreviousRevision(): List<Phase1HabitChallenge>? =
        data.data.first()[lastGood]?.let { readChecked(it) }

    private fun readChecked(raw: String): List<Phase1HabitChallenge> =
        try { Phase1HabitCodec.decode(raw) }
        catch (e: Exception) {
            throw IllegalStateException(
                "Challenge archive cannot be read; no data were changed. An earlier revision may exist.",
                e,
            )
        }
}

/** Never overwrites confirmed Done receipts or edits during approved imports. */
internal object Phase1HabitMergePolicy {
    fun addMissing(
        existing: List<Phase1HabitChallenge>,
        incoming: List<Phase1HabitChallenge>,
    ): List<Phase1HabitChallenge> {
        existing.forEach { it.checked() }
        incoming.forEach { it.checked() }
        require(existing.map { it.id }.distinct().size == existing.size) {
            "Existing challenge identity conflict"
        }
        require(incoming.map { it.id }.distinct().size == incoming.size) {
            "Import challenge identity conflict"
        }
        // A matching ID with different activity, date/zone, alert, pause or
        // EXACT Done receipt instant is a conflict, not a silent no-op.
        // The future restore UI must request an explicit owner decision.
        val byId = existing.associateBy { it.id }
        require(incoming.none { record ->
            byId[record.id]?.let { it != record } == true
        }) {
            "Conflicting 21-day challenge already exists. Owner review required."
        }
        return existing + incoming.filterNot { it.id in byId }
    }
}
