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
 * Isolated Personal Challenge persistence. This does NOT touch Personal
 * reminders, Creator projects, existing Spaces, alarms or cloud backup.
 * Primary corruption fails closed; a validated previous revision remains.
 */
private val Context.v297ChallengesDataStore by preferencesDataStore(
    name = "backlot_personal_challenges_v1",
)
private val v297ChallengeMutex = Mutex()

internal class V297ChallengeStore(context: Context) {
    private val app = context.applicationContext
    private val data = app.v297ChallengesDataStore
    private val primary = stringPreferencesKey("challenges21_v1")
    private val lastGood = stringPreferencesKey("challenges21_last_good_v1")

    val challengesFlow: Flow<List<V297Challenge>> = data.data.map { prefs ->
        val raw = prefs[primary]
        if (raw == null) {
            check(prefs[lastGood] == null) {
                "Challenge primary missing. Recovery copy retained; no data overwritten."
            }
            emptyList()
        } else readChecked(raw)
    }

    suspend fun load(): List<V297Challenge> = challengesFlow.first()

    suspend fun upsert(challenge: V297Challenge): List<V297Challenge> =
        mutate { plans ->
            val checked = challenge.checked()
            val index = plans.indexOfFirst { it.id == checked.id }
            if (index < 0) plans + checked
            else plans.toMutableList().apply { set(index, checked) }
        }

    suspend fun update(
        id: String, change: (V297Challenge) -> V297Challenge,
    ): List<V297Challenge> = mutate { plans ->
        val index = plans.indexOfFirst { it.id == id }
        require(index >= 0) { "Challenge not found" }
        val replacement = change(plans[index]).checked()
        require(replacement.id == id) { "Challenge ID cannot change" }
        plans.toMutableList().apply { set(index, replacement) }
    }

    suspend fun mutate(
        change: (List<V297Challenge>) -> List<V297Challenge>,
    ): List<V297Challenge> = StabilizationPersonalMutationGate.withOwnerMutation(app) {
        v297ChallengeMutex.withLock {
        var result: List<V297Challenge> = emptyList()
        data.edit { prefs ->
            val previousRaw = prefs[primary]
            val previous = if (previousRaw == null) {
                check(prefs[lastGood] == null) {
                    "Challenge primary missing while recovery copy exists. No data changed."
                }
                emptyList()
            } else readChecked(previousRaw)
            val updated = change(previous)
            val encoded = V297ChallengeCodec.encode(updated)
            if (updated != previous) {
                prefs[lastGood] = previousRaw ?: V297ChallengeCodec.encode(emptyList())
                prefs[primary] = encoded
            }
            result = updated
        }
        result
        }
    }

    /** Owner-invoked, copyable, integrity-checked local backup payload. */
    suspend fun exportJson(): String = v297ChallengeMutex.withLock {
        val prefs = data.data.first()
        val raw = prefs[primary]
        val entries = if (raw == null) {
            check(prefs[lastGood] == null) { "Primary missing; refused incomplete export" }
            emptyList()
        } else readChecked(raw)
        V297ChallengeCodec.encode(entries)
    }

    /** Validation is read-only; never imports without explicit user approval. */
    fun validateImport(raw: String): List<V297Challenge> =
        V297ChallengeCodec.decode(raw)

    suspend fun replaceFromApprovedImport(raw: String): List<V297Challenge> {
        val validated = validateImport(raw)
        return mutate { validated }
    }

    suspend fun validatedPreviousRevision(): List<V297Challenge>? =
        data.data.first()[lastGood]?.let { readChecked(it) }

    private fun readChecked(raw: String): List<V297Challenge> =
        try {
            V297ChallengeCodec.decode(raw)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Challenge data could not be read. No records were changed. A recovery revision may exist.",
                e,
            )
        }
}
