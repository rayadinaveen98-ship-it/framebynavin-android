package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorScriptStudioTest {
    @Test
    fun legacyHookAndScriptImportWithoutLosingReadableContent() {
        val studio = CreatorScriptStudio.fromLegacy(
            projectId = "project",
            hook = "Why does this frame feel dangerous?",
            script = "First paragraph.\n\nSecond paragraph.",
        )

        assertEquals("Why does this frame feel dangerous?", studio.selectedHook())
        assertEquals(1, studio.beats.size)
        assertEquals("First paragraph.\n\nSecond paragraph.", studio.compiledNarration())
        assertFalse(studio.isEmpty())
    }

    @Test
    fun selectedIdeasAreIndependentFromAlternatives() {
        val studio = CreatorScriptStudio(
            projectId = "p",
            hooks = listOf(
                CreatorHookIdea(id = "h1", text = "Hook one"),
                CreatorHookIdea(id = "h2", text = "Hook two", selected = true),
            ),
            titles = listOf(
                CreatorTitleIdea(id = "t1", text = "Title one", selected = true),
                CreatorTitleIdea(id = "t2", text = "Title two"),
            ),
        )

        assertEquals("Hook two", studio.selectedHook())
        assertEquals("Title one", studio.selectedTitle())
        assertEquals(2, studio.hooks.size)
        assertEquals(2, studio.titles.size)
    }

    @Test
    fun compiledNarrationKeepsBeatOrderAndCountsWords() {
        val studio = CreatorScriptStudio(
            projectId = "p",
            beats = listOf(
                CreatorScriptBeat(id = "a", label = "Open", narration = "Cinema begins here"),
                CreatorScriptBeat(id = "b", label = "Payoff", narration = "Then the image changes meaning"),
            ),
        )

        assertEquals("Cinema begins here\n\nThen the image changes meaning", studio.compiledNarration())
        assertEquals(8, studio.wordCount())
    }

    @Test
    fun beatReadinessDrivesScriptProgressWithoutPublishingAnything() {
        val studio = CreatorScriptStudio(
            projectId = "p",
            status = CreatorScriptStatus.READY_TO_RECORD,
            beats = listOf(
                CreatorScriptBeat(id = "1", narration = "A", status = CreatorScriptBeatStatus.READY),
                CreatorScriptBeat(id = "2", narration = "B", status = CreatorScriptBeatStatus.LOCKED),
                CreatorScriptBeat(id = "3", narration = "C", status = CreatorScriptBeatStatus.DRAFT),
            ),
        )

        assertEquals(2, studio.readyBeatCount())
        assertEquals(66, studio.progressPercent())
        assertEquals(CreatorScriptStatus.READY_TO_RECORD, studio.status)
        assertTrue(studio.compiledNarration().contains("A"))
    }
}
