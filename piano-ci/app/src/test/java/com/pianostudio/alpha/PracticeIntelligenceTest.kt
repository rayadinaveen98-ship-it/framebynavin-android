package com.pianostudio.alpha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeIntelligenceTest {
    @Test
    fun analyzerFindsWeakestNoteAndSlowsTempo() {
        val attempts = listOf(
            PracticeAttempt(expected = 60, actual = 60, correct = true),
            PracticeAttempt(expected = 64, actual = 62, correct = false),
            PracticeAttempt(expected = 64, actual = 65, correct = false),
            PracticeAttempt(expected = 64, actual = 64, correct = true),
            PracticeAttempt(expected = 67, actual = 67, correct = true),
        )

        val report = PracticeAnalyzer.analyze(
            attempts = attempts,
            bpm = 80,
            sourceSequence = listOf(60, 62, 64, 67, 64, 62, 60),
        )

        assertEquals(64, report.weakestMidi)
        assertEquals(60, report.noteAccuracy)
        assertEquals(70, report.recommendedBpm)
        assertTrue(report.focusSequence.contains(64))
    }

    @Test
    fun cleanSteadyRunRaisesTempo() {
        val attempts = listOf(
            PracticeAttempt(60, 60, true, null),
            PracticeAttempt(62, 62, true, 750),
            PracticeAttempt(64, 64, true, 750),
            PracticeAttempt(67, 67, true, 750),
        )

        val report = PracticeAnalyzer.analyze(
            attempts = attempts,
            bpm = 80,
            sourceSequence = listOf(60, 62, 64, 67),
        )

        assertEquals(100, report.noteAccuracy)
        assertEquals(100, report.timingScore)
        assertEquals(85, report.recommendedBpm)
        assertEquals(null, report.weakestMidi)
    }

    @Test
    fun guidedRuntimeWaitsAfterWrongNote() {
        val runtime = GuidedPracticeRuntime(listOf(60, 62), bpm = 80)

        val wrong = runtime.onNote(61, true, nowNanos = 1_000_000_000L)
        assertFalse(wrong.correct)
        assertEquals(60, runtime.expected)
        assertEquals(1, runtime.mistakes)

        val first = runtime.onNote(60, true, nowNanos = 2_000_000_000L)
        assertTrue(first.correct)
        assertEquals(62, runtime.expected)

        val second = runtime.onNote(62, true, nowNanos = 2_750_000_000L)
        assertTrue(second.completed)
        assertTrue(runtime.completed)
    }

    @Test
    fun poorTimingReducesTempoEvenWhenNotesAreCorrect() {
        val attempts = listOf(
            PracticeAttempt(60, 60, true, null),
            PracticeAttempt(62, 62, true, 1500),
            PracticeAttempt(64, 64, true, 1500),
        )

        val report = PracticeAnalyzer.analyze(
            attempts = attempts,
            bpm = 80,
            sourceSequence = listOf(60, 62, 64),
        )

        assertEquals(100, report.noteAccuracy)
        assertTrue(report.timingScore < 65)
        assertEquals(70, report.recommendedBpm)
    }
}
