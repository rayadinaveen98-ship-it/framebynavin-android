package com.pianostudio.alpha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonEngineTest {
    @Test
    fun sequenceRejectsWrongNoteAndAdvancesOnCorrectNotes() {
        val lesson = LessonCatalog.byId("first-three-notes")!!
        val runtime = LessonRuntime(lesson)
        runtime.continueInfo()

        val wrong = runtime.onNote(61, true)
        assertFalse(wrong.correct!!)
        assertEquals(1, runtime.mistakes)

        runtime.onNote(60, true)
        runtime.onNote(60, false)
        runtime.onNote(62, true)
        runtime.onNote(62, false)
        val result = runtime.onNote(64, true)

        assertTrue(result.stepCompleted)
        assertEquals(2, runtime.stepIndex)
    }

    @Test
    fun cMajorChordCompletesWhenAllThreeTargetsAreHeld() {
        val lesson = LessonCatalog.byId("c-major-chord")!!
        val runtime = LessonRuntime(lesson)
        runtime.continueInfo()

        runtime.onNote(60, true)
        runtime.onNote(64, true)
        val result = runtime.onNote(67, true)

        assertTrue(result.correct!!)
        assertTrue(result.stepCompleted)
        assertEquals(2, runtime.stepIndex)
    }

    @Test
    fun middleCLessonCanReachCompletion() {
        val lesson = LessonCatalog.byId("middle-c")!!
        val runtime = LessonRuntime(lesson)
        runtime.continueInfo()
        runtime.onNote(60, true)
        runtime.onNote(60, false)
        repeat(2) {
            runtime.onNote(60, true)
            runtime.onNote(60, false)
        }
        val result = runtime.onNote(60, true)

        assertTrue(result.lessonCompleted)
        assertTrue(runtime.completed)
        assertEquals(100, runtime.accuracy)
    }
}
