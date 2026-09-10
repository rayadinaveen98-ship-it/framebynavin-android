package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorContentProjectAlpha6Test {
    @Test
    fun normalizedStudioKeepsOneSelectionAndCleansStructuredFields() {
        val studio = CreatorScriptStudio(
            projectId = "project",
            revision = 4,
            hooks = listOf(
                CreatorHookIdea(id = "h1", text = "  First hook  ", selected = true),
                CreatorHookIdea(id = "h2", text = "Second hook", selected = true),
                CreatorHookIdea(id = "h3", text = "   "),
            ),
            titles = listOf(
                CreatorTitleIdea(id = "t1", text = "  Final title ", selected = true),
                CreatorTitleIdea(id = "t1", text = "duplicate id"),
            ),
            beats = listOf(
                CreatorScriptBeat(
                    id = "b1",
                    label = "  Opening  ",
                    narration = "Narration  \n",
                    visualNotes = "  Visual note  \n",
                ),
                CreatorScriptBeat(id = "empty"),
            ),
            creatorNotes = "Creator note  \n",
        ).normalized()

        assertEquals(2, studio.hooks.size)
        assertEquals(1, studio.hooks.count { it.selected })
        assertEquals("First hook", studio.selectedHook())
        assertEquals(1, studio.titles.size)
        assertEquals("Final title", studio.selectedTitle())
        assertEquals(1, studio.beats.size)
        assertEquals("Opening", studio.beats.single().label)
        assertEquals("Narration", studio.beats.single().narration)
        assertEquals("Creator note", studio.creatorNotes)
        assertEquals(4L, studio.revision)
    }

    @Test
    fun legacyScriptBecomesStructuredWithoutLosingReadableSummary() {
        val studio = CreatorScriptStudio.fromLegacy(
            projectId = "legacy-project",
            hook = "Why does this scene work?",
            script = "Opening narration.\n\nSecond paragraph.",
        ).normalized()

        assertFalse(studio.isEmpty())
        assertEquals("Why does this scene work?", studio.selectedHook())
        assertEquals("Opening narration.\n\nSecond paragraph.", studio.compiledNarration())
        assertEquals("Legacy script", studio.beats.single().label)
    }

    @Test
    fun embeddedStructuredScriptMakesWorkspaceNonEmptyAndPortable() {
        val studio = CreatorScriptStudio(
            projectId = "p",
            revision = 3,
            beats = listOf(CreatorScriptBeat(id = "beat", narration = "Portable narration")),
        )
        val workspace = CreatorContentWorkspace(scriptStudio = studio)

        assertFalse(workspace.isEmpty())
        assertEquals("Portable narration", workspace.scriptStudio?.compiledNarration())
        assertEquals(3L, workspace.scriptStudio?.revision)
    }

    @Test
    fun embeddedCopyWinsOverLegacySummaryOnFutureEdits() {
        val embedded = CreatorScriptStudio(
            projectId = "p",
            revision = 7,
            hooks = listOf(CreatorHookIdea(id = "h", text = "Embedded hook", selected = true)),
            beats = listOf(CreatorScriptBeat(id = "b", narration = "Embedded script")),
        )
        val workspace = CreatorContentWorkspace(
            hook = "Readable hook",
            script = "Readable script",
            scriptStudio = embedded,
        )

        val copiedByOlderEditor = workspace.copy(audience = "Updated audience")
        assertEquals(7L, copiedByOlderEditor.scriptStudio?.revision)
        assertEquals("Embedded hook", copiedByOlderEditor.scriptStudio?.selectedHook())
        assertTrue(copiedByOlderEditor.scriptStudio?.compiledNarration()?.contains("Embedded") == true)
    }
}
