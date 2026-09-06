package com.pianostudio.alpha

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class LessonStepKind { INFO, SEQUENCE, CHORD }

data class LessonStep(
    val id: String,
    val title: String,
    val instruction: String,
    val kind: LessonStepKind,
    val targets: List<List<Int>> = emptyList(),
    val hint: String = "",
)

data class PianoLesson(
    val id: String,
    val title: String,
    val subtitle: String,
    val minutes: Int,
    val steps: List<LessonStep>,
)

data class LessonProgress(
    val stepIndex: Int = 0,
    val completed: Boolean = false,
    val bestAccuracy: Int = 0,
)

data class LessonInputResult(
    val correct: Boolean? = null,
    val stepCompleted: Boolean = false,
    val lessonCompleted: Boolean = false,
    val feedback: String = "",
    val expected: List<Int> = emptyList(),
)

object LessonCatalog {
    val foundations = listOf(
        PianoLesson(
            id = "middle-c",
            title = "Find Middle C",
            subtitle = "Your first landmark on the keyboard",
            minutes = 2,
            steps = listOf(
                LessonStep(
                    id = "intro",
                    title = "Meet Middle C",
                    instruction = "Middle C sits near the center of the piano, just before a group of two black keys.",
                    kind = LessonStepKind.INFO,
                    hint = "Look for two black keys. C is the white key immediately to their left.",
                ),
                LessonStep(
                    id = "find-c",
                    title = "Find it",
                    instruction = "Play Middle C once.",
                    kind = LessonStepKind.SEQUENCE,
                    targets = listOf(listOf(60)),
                    hint = "Middle C is C4.",
                ),
                LessonStep(
                    id = "steady-c",
                    title = "Make it familiar",
                    instruction = "Play Middle C three times. Release the key between each note.",
                    kind = LessonStepKind.SEQUENCE,
                    targets = listOf(listOf(60), listOf(60), listOf(60)),
                    hint = "Aim for three calm, even presses.",
                ),
            ),
        ),
        PianoLesson(
            id = "first-three-notes",
            title = "C · D · E",
            subtitle = "Build your first three-note phrase",
            minutes = 3,
            steps = listOf(
                LessonStep(
                    id = "intro",
                    title = "Three neighboring notes",
                    instruction = "C, D and E sit next to each other. We will move one white key at a time.",
                    kind = LessonStepKind.INFO,
                    hint = "C4 = 60, D4 = 62, E4 = 64 in MIDI.",
                ),
                LessonStep(
                    id = "up",
                    title = "Walk upward",
                    instruction = "Play C, then D, then E.",
                    kind = LessonStepKind.SEQUENCE,
                    targets = listOf(listOf(60), listOf(62), listOf(64)),
                    hint = "Move one white key to the right each time.",
                ),
                LessonStep(
                    id = "phrase",
                    title = "Your first phrase",
                    instruction = "Play C · D · E · D · C.",
                    kind = LessonStepKind.SEQUENCE,
                    targets = listOf(listOf(60), listOf(62), listOf(64), listOf(62), listOf(60)),
                    hint = "Up three notes, then return home.",
                ),
            ),
        ),
        PianoLesson(
            id = "c-major-chord",
            title = "Your First Chord",
            subtitle = "Hear C major as one sound",
            minutes = 4,
            steps = listOf(
                LessonStep(
                    id = "intro",
                    title = "Three notes become harmony",
                    instruction = "A C major chord uses C, E and G. Play all three together to make one harmony.",
                    kind = LessonStepKind.INFO,
                    hint = "Use C4, E4 and G4 for this first chord.",
                ),
                LessonStep(
                    id = "build",
                    title = "Build C major",
                    instruction = "Press C, E and G together and hold them briefly.",
                    kind = LessonStepKind.CHORD,
                    targets = listOf(listOf(60, 64, 67)),
                    hint = "You can place the fingers first, then press the three keys together.",
                ),
                LessonStep(
                    id = "arpeggio",
                    title = "Open the chord",
                    instruction = "Now play the same notes separately: C · E · G · E · C.",
                    kind = LessonStepKind.SEQUENCE,
                    targets = listOf(listOf(60), listOf(64), listOf(67), listOf(64), listOf(60)),
                    hint = "The same harmony can be heard one note at a time.",
                ),
            ),
        ),
    )

    val all: List<PianoLesson> = foundations
    fun byId(id: String): PianoLesson? = all.firstOrNull { it.id == id }
}

class LessonRuntime(
    val lesson: PianoLesson,
    startStep: Int = 0,
) {
    var stepIndex: Int = startStep.coerceIn(0, lesson.steps.lastIndex.coerceAtLeast(0))
        private set
    var targetIndex: Int = 0
        private set
    var correctHits: Int = 0
        private set
    var mistakes: Int = 0
        private set
    private val held = linkedSetOf<Int>()

    val currentStep: LessonStep?
        get() = lesson.steps.getOrNull(stepIndex)

    val completed: Boolean
        get() = stepIndex >= lesson.steps.size

    val accuracy: Int
        get() {
            val total = correctHits + mistakes
            return if (total == 0) 100 else ((correctHits * 100.0) / total).toInt().coerceIn(0, 100)
        }

    val currentExpected: List<Int>
        get() {
            val step = currentStep ?: return emptyList()
            return when (step.kind) {
                LessonStepKind.INFO -> emptyList()
                LessonStepKind.SEQUENCE -> step.targets.getOrNull(targetIndex).orEmpty()
                LessonStepKind.CHORD -> step.targets.firstOrNull().orEmpty()
            }
        }

    fun continueInfo(): LessonInputResult {
        val step = currentStep ?: return LessonInputResult(lessonCompleted = true)
        if (step.kind != LessonStepKind.INFO) {
            return LessonInputResult(feedback = "Play the highlighted target to continue.", expected = currentExpected)
        }
        return advanceStep(feedback = "Ready to play.")
    }

    fun onNote(midi: Int, pressed: Boolean): LessonInputResult {
        val step = currentStep ?: return LessonInputResult(lessonCompleted = true)
        if (!pressed) {
            held.remove(midi)
            return LessonInputResult(expected = currentExpected)
        }
        if (step.kind == LessonStepKind.INFO) {
            return LessonInputResult(feedback = "Read this step, then tap Continue.")
        }

        return when (step.kind) {
            LessonStepKind.SEQUENCE -> handleSequence(midi)
            LessonStepKind.CHORD -> handleChord(midi)
            LessonStepKind.INFO -> LessonInputResult()
        }
    }

    private fun handleSequence(midi: Int): LessonInputResult {
        val expected = currentExpected.firstOrNull()
            ?: return advanceStep(feedback = "Step complete.")
        return if (midi == expected) {
            correctHits++
            targetIndex++
            val step = currentStep
            if (step != null && targetIndex >= step.targets.size) {
                advanceStep(correct = true, feedback = "Nice. That step is complete.")
            } else {
                LessonInputResult(
                    correct = true,
                    feedback = "Good. Next: ${noteLabel(currentExpected.firstOrNull())}",
                    expected = currentExpected,
                )
            }
        } else {
            mistakes++
            LessonInputResult(
                correct = false,
                feedback = "That was ${noteLabel(midi)}. Try ${noteLabel(expected)}.",
                expected = listOf(expected),
            )
        }
    }

    private fun handleChord(midi: Int): LessonInputResult {
        val expected = currentExpected.toSet()
        held += midi
        if (midi !in expected) {
            mistakes++
            return LessonInputResult(
                correct = false,
                feedback = "${noteLabel(midi)} is outside this chord. Aim for ${expected.sorted().joinToString(" · ") { noteLabel(it) }}.",
                expected = expected.sorted(),
            )
        }
        if (expected.isNotEmpty() && expected.all { it in held }) {
            correctHits += expected.size
            return advanceStep(correct = true, feedback = "Beautiful. You built C major.")
        }
        val remaining = expected.filterNot { it in held }.sorted()
        return LessonInputResult(
            correct = true,
            feedback = "Good. Add ${remaining.joinToString(" + ") { noteLabel(it) }}.",
            expected = remaining,
        )
    }

    private fun advanceStep(
        correct: Boolean? = null,
        feedback: String,
    ): LessonInputResult {
        val finishedStep = currentStep != null
        stepIndex++
        targetIndex = 0
        held.clear()
        val lessonDone = stepIndex >= lesson.steps.size
        return LessonInputResult(
            correct = correct,
            stepCompleted = finishedStep,
            lessonCompleted = lessonDone,
            feedback = if (lessonDone) "Lesson complete." else feedback,
            expected = currentExpected,
        )
    }
}

private val Context.lessonProgressDataStore by preferencesDataStore(name = "piano_lesson_progress")

class LessonProgressStore(private val context: Context) {
    private fun safe(id: String) = id.replace(Regex("[^A-Za-z0-9_]"), "_")
    private fun stepKey(id: String) = intPreferencesKey("lesson_${safe(id)}_step")
    private fun completeKey(id: String) = booleanPreferencesKey("lesson_${safe(id)}_complete")
    private fun bestKey(id: String) = intPreferencesKey("lesson_${safe(id)}_best")

    val progress: Flow<Map<String, LessonProgress>> = context.lessonProgressDataStore.data.map { prefs ->
        LessonCatalog.all.associate { lesson ->
            lesson.id to LessonProgress(
                stepIndex = (prefs[stepKey(lesson.id)] ?: 0).coerceIn(0, lesson.steps.size),
                completed = prefs[completeKey(lesson.id)] ?: false,
                bestAccuracy = (prefs[bestKey(lesson.id)] ?: 0).coerceIn(0, 100),
            )
        }
    }

    suspend fun save(lesson: PianoLesson, stepIndex: Int, completed: Boolean, accuracy: Int) {
        context.lessonProgressDataStore.edit { prefs ->
            prefs[stepKey(lesson.id)] = stepIndex.coerceIn(0, lesson.steps.size)
            prefs[completeKey(lesson.id)] = completed
            prefs[bestKey(lesson.id)] = maxOf(prefs[bestKey(lesson.id)] ?: 0, accuracy.coerceIn(0, 100))
        }
    }

    suspend fun reset(lesson: PianoLesson) {
        context.lessonProgressDataStore.edit { prefs ->
            prefs[stepKey(lesson.id)] = 0
            prefs[completeKey(lesson.id)] = false
        }
    }
}

fun noteLabel(midi: Int?): String {
    if (midi == null) return "—"
    val names = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    return names[(midi % 12 + 12) % 12] + (midi / 12 - 1)
}
