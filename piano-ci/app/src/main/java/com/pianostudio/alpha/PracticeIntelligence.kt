package com.pianostudio.alpha

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.abs

private val Context.practiceCoachDataStore by preferencesDataStore(name = "piano_practice_coach")

data class PracticeAttempt(
    val expected: Int,
    val actual: Int,
    val correct: Boolean,
    val intervalMs: Long? = null,
)

data class PracticeReport(
    val noteAccuracy: Int,
    val timingScore: Int,
    val mistakes: Int,
    val weakestMidi: Int?,
    val recommendedBpm: Int,
    val focusSequence: List<Int>,
    val message: String,
)

data class PracticeCoachSnapshot(
    val noteAccuracy: Int,
    val timingScore: Int,
    val weakestMidi: Int?,
    val recommendedBpm: Int,
    val updatedAt: Long,
)

data class PracticeInputResult(
    val correct: Boolean,
    val completed: Boolean,
    val feedback: String,
    val expected: Int?,
)

object AdaptiveTempoCoach {
    fun nextBpm(noteAccuracy: Int, timingScore: Int, currentBpm: Int): Int {
        val current = currentBpm.coerceIn(50, 120)
        val next = when {
            noteAccuracy < 75 || timingScore < 65 -> current - 10
            noteAccuracy < 90 || timingScore < 80 -> current - 5
            noteAccuracy >= 97 && timingScore >= 92 -> current + 5
            else -> current
        }
        return next.coerceIn(50, 120)
    }
}

object PracticeAnalyzer {
    fun analyze(
        attempts: List<PracticeAttempt>,
        bpm: Int,
        sourceSequence: List<Int>,
    ): PracticeReport {
        if (attempts.isEmpty()) {
            return PracticeReport(
                noteAccuracy = 100,
                timingScore = 100,
                mistakes = 0,
                weakestMidi = null,
                recommendedBpm = bpm.coerceIn(50, 120),
                focusSequence = emptyList(),
                message = "Play the diagnostic phrase to build a practice recommendation.",
            )
        }

        val correct = attempts.count { it.correct }
        val mistakes = attempts.size - correct
        val noteAccuracy = ((correct * 100.0) / attempts.size).toInt().coerceIn(0, 100)

        val idealInterval = 60_000.0 / bpm.coerceIn(50, 120)
        val timingSamples = attempts.mapNotNull { it.intervalMs }.filter { it > 0L }
        val timingScore = if (timingSamples.isEmpty()) {
            100
        } else {
            timingSamples.map { interval ->
                val errorRatio = (abs(interval - idealInterval) / idealInterval).coerceIn(0.0, 1.0)
                ((1.0 - errorRatio) * 100.0).toInt()
            }.average().toInt().coerceIn(0, 100)
        }

        val weakness = attempts
            .groupBy { it.expected }
            .mapValues { (_, values) ->
                val missCount = values.count { !it.correct }
                val missRate = missCount.toDouble() / values.size.coerceAtLeast(1)
                missCount to missRate
            }
            .filterValues { it.first > 0 }
            .maxWithOrNull(
                compareBy<Map.Entry<Int, Pair<Int, Double>>> { it.value.second }
                    .thenBy { it.value.first },
            )
            ?.key

        val recommendedBpm = AdaptiveTempoCoach.nextBpm(noteAccuracy, timingScore, bpm)
        val focusSequence = buildFocusSequence(weakness, sourceSequence)
        val message = when {
            weakness != null && recommendedBpm < bpm ->
                "${noteLabel(weakness)} needs attention. Slow down and make every press deliberate."
            weakness != null ->
                "${noteLabel(weakness)} is the least consistent note. A short focused loop should clean it up."
            timingScore < 80 ->
                "The notes are secure. Keep the same phrase and settle into the beat."
            recommendedBpm > bpm ->
                "Clean and steady. You are ready to raise the tempo slightly."
            else ->
                "The phrase is stable. Repeat once more before moving on."
        }

        return PracticeReport(
            noteAccuracy = noteAccuracy,
            timingScore = timingScore,
            mistakes = mistakes,
            weakestMidi = weakness,
            recommendedBpm = recommendedBpm,
            focusSequence = focusSequence,
            message = message,
        )
    }

    private fun buildFocusSequence(weakestMidi: Int?, source: List<Int>): List<Int> {
        val weak = weakestMidi ?: return emptyList()
        if (source.isEmpty()) return listOf(weak, weak, weak, weak)
        val index = source.indexOf(weak).takeIf { it >= 0 } ?: return listOf(weak, weak, weak, weak)
        val context = buildList {
            if (index > 0) add(source[index - 1])
            add(weak)
            if (index + 1 < source.size) add(source[index + 1])
            add(weak)
        }
        return (context + context).take(8)
    }
}

class GuidedPracticeRuntime(
    val sequence: List<Int>,
    val bpm: Int,
) {
    private val attempts = mutableListOf<PracticeAttempt>()
    private var lastCorrectAtNanos: Long? = null

    var index: Int = 0
        private set

    val completed: Boolean
        get() = index >= sequence.size

    val expected: Int?
        get() = sequence.getOrNull(index)

    val mistakes: Int
        get() = attempts.count { !it.correct }

    val noteAccuracy: Int
        get() {
            if (attempts.isEmpty()) return 100
            return ((attempts.count { it.correct } * 100.0) / attempts.size).toInt().coerceIn(0, 100)
        }

    fun onNote(midi: Int, pressed: Boolean, nowNanos: Long = System.nanoTime()): PracticeInputResult {
        val target = expected ?: return PracticeInputResult(true, true, "Practice loop complete.", null)
        if (!pressed) return PracticeInputResult(true, completed, "", target)

        if (midi != target) {
            attempts += PracticeAttempt(expected = target, actual = midi, correct = false)
            return PracticeInputResult(
                correct = false,
                completed = false,
                feedback = "That was ${noteLabel(midi)}. Stay on ${noteLabel(target)}.",
                expected = target,
            )
        }

        val intervalMs = lastCorrectAtNanos?.let { previous ->
            ((nowNanos - previous).coerceAtLeast(0L) / 1_000_000L)
        }
        attempts += PracticeAttempt(expected = target, actual = midi, correct = true, intervalMs = intervalMs)
        lastCorrectAtNanos = nowNanos
        index += 1
        val done = completed
        return PracticeInputResult(
            correct = true,
            completed = done,
            feedback = if (done) "Focus loop complete." else "Good. Next: ${noteLabel(expected)}",
            expected = expected,
        )
    }

    fun report(): PracticeReport = PracticeAnalyzer.analyze(attempts, bpm, sequence)
}

class PracticeCoachStore(private val context: Context) {
    private object Keys {
        val noteAccuracy = intPreferencesKey("latest_note_accuracy")
        val timingScore = intPreferencesKey("latest_timing_score")
        val weakestMidi = intPreferencesKey("latest_weakest_midi")
        val recommendedBpm = intPreferencesKey("latest_recommended_bpm")
        val updatedAt = longPreferencesKey("latest_updated_at")
    }

    val snapshot: Flow<PracticeCoachSnapshot?> = context.practiceCoachDataStore.data.map { prefs ->
        val updatedAt = prefs[Keys.updatedAt] ?: 0L
        if (updatedAt <= 0L) {
            null
        } else {
            PracticeCoachSnapshot(
                noteAccuracy = (prefs[Keys.noteAccuracy] ?: 0).coerceIn(0, 100),
                timingScore = (prefs[Keys.timingScore] ?: 0).coerceIn(0, 100),
                weakestMidi = (prefs[Keys.weakestMidi] ?: -1).takeIf { it >= 0 },
                recommendedBpm = (prefs[Keys.recommendedBpm] ?: 80).coerceIn(50, 120),
                updatedAt = updatedAt,
            )
        }
    }

    suspend fun save(report: PracticeReport) {
        context.practiceCoachDataStore.edit { prefs ->
            prefs[Keys.noteAccuracy] = report.noteAccuracy.coerceIn(0, 100)
            prefs[Keys.timingScore] = report.timingScore.coerceIn(0, 100)
            prefs[Keys.weakestMidi] = report.weakestMidi ?: -1
            prefs[Keys.recommendedBpm] = report.recommendedBpm.coerceIn(50, 120)
            prefs[Keys.updatedAt] = System.currentTimeMillis()
        }
    }
}
