package com.pianostudio.core.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pianoDataStore by preferencesDataStore(name = "piano_practice_settings")

data class PianoPracticeSettings(
    val bpm: Int = 96,
    val sustain: Boolean = false,
    val metronome: Boolean = false,
)

class PianoSettingsStore(private val context: Context) {
    private object Keys {
        val bpm = intPreferencesKey("bpm")
        val sustain = booleanPreferencesKey("sustain")
        val metronome = booleanPreferencesKey("metronome")
    }

    val settings: Flow<PianoPracticeSettings> = context.pianoDataStore.data.map { prefs ->
        PianoPracticeSettings(
            bpm = (prefs[Keys.bpm] ?: 96).coerceIn(40, 220),
            sustain = prefs[Keys.sustain] ?: false,
            metronome = prefs[Keys.metronome] ?: false,
        )
    }

    suspend fun setBpm(value: Int) {
        context.pianoDataStore.edit { it[Keys.bpm] = value.coerceIn(40, 220) }
    }

    suspend fun setSustain(value: Boolean) {
        context.pianoDataStore.edit { it[Keys.sustain] = value }
    }

    suspend fun setMetronome(value: Boolean) {
        context.pianoDataStore.edit { it[Keys.metronome] = value }
    }
}

data class RecordedNoteEvent(
    val offsetMs: Long,
    val midi: Int,
    val pressed: Boolean,
    val velocity: Int,
    val source: String,
)

data class PracticeSessionSummary(
    val id: Long,
    val startedAt: Long,
    val durationMs: Long,
    val eventCount: Int,
    val bpm: Int,
)

class PianoSessionStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "piano_practice.db",
    null,
    1,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE practice_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                started_at INTEGER NOT NULL,
                ended_at INTEGER NOT NULL,
                duration_ms INTEGER NOT NULL,
                event_count INTEGER NOT NULL,
                bpm INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE practice_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id INTEGER NOT NULL,
                offset_ms INTEGER NOT NULL,
                midi INTEGER NOT NULL,
                pressed INTEGER NOT NULL,
                velocity INTEGER NOT NULL,
                source TEXT NOT NULL,
                FOREIGN KEY(session_id) REFERENCES practice_sessions(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_practice_events_session ON practice_events(session_id)")
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    @Synchronized
    fun saveSession(
        startedAt: Long,
        endedAt: Long,
        bpm: Int,
        events: List<RecordedNoteEvent>,
    ): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val session = ContentValues().apply {
                put("started_at", startedAt)
                put("ended_at", endedAt)
                put("duration_ms", (endedAt - startedAt).coerceAtLeast(0L))
                put("event_count", events.size)
                put("bpm", bpm.coerceIn(40, 220))
            }
            val sessionId = db.insertOrThrow("practice_sessions", null, session)
            events.forEach { event ->
                val row = ContentValues().apply {
                    put("session_id", sessionId)
                    put("offset_ms", event.offsetMs.coerceAtLeast(0L))
                    put("midi", event.midi.coerceIn(0, 127))
                    put("pressed", if (event.pressed) 1 else 0)
                    put("velocity", event.velocity.coerceIn(0, 127))
                    put("source", event.source.take(16))
                }
                db.insertOrThrow("practice_events", null, row)
            }
            db.setTransactionSuccessful()
            return sessionId
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun recentSessions(limit: Int = 5): List<PracticeSessionSummary> {
        val result = mutableListOf<PracticeSessionSummary>()
        readableDatabase.query(
            "practice_sessions",
            arrayOf("id", "started_at", "duration_ms", "event_count", "bpm"),
            null,
            null,
            null,
            null,
            "started_at DESC",
            limit.coerceIn(1, 20).toString(),
        ).use { cursor ->
            val id = cursor.getColumnIndexOrThrow("id")
            val startedAt = cursor.getColumnIndexOrThrow("started_at")
            val duration = cursor.getColumnIndexOrThrow("duration_ms")
            val eventCount = cursor.getColumnIndexOrThrow("event_count")
            val bpm = cursor.getColumnIndexOrThrow("bpm")
            while (cursor.moveToNext()) {
                result += PracticeSessionSummary(
                    id = cursor.getLong(id),
                    startedAt = cursor.getLong(startedAt),
                    durationMs = cursor.getLong(duration),
                    eventCount = cursor.getInt(eventCount),
                    bpm = cursor.getInt(bpm),
                )
            }
        }
        return result
    }
}
