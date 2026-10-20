package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorPlanScriptBridgeTest {
    @Test fun savedBriefIsReadOnlyAndKeepsCorrectFieldOrder() {
        val selected = CreatorScriptStudio(
            projectId = "p3",
            hooks = listOf(CreatorHookIdea(text = "Script hook", selected = true)),
        )
        val workspace = CreatorContentWorkspace(
            audience = "  Film students ",
            viewerProblem = "How does this edit work?",
            promise = "Explain the sequence",
            angle = "Look at blocking",
            hook = "Outdated legacy hook",
            scriptStudio = selected,
        )
        assertEquals(
            listOf("Audience", "Viewer need", "Promise", "Angle", "Hook"),
            CreatorPlanScriptBridge.fields(workspace).map { it.label },
        )
        assertEquals(
            listOf("Film students", "How does this edit work?", "Explain the sequence", "Look at blocking", "Script hook"),
            CreatorPlanScriptBridge.fields(workspace).map { it.value },
        )
        assertEquals("Outdated legacy hook", workspace.hook)
        assertEquals("Script hook", workspace.scriptStudio?.selectedHook())
    }

    @Test fun emptyBriefShowsNoPanelAndExistingScriptIsNotTouched() {
        val script = CreatorScriptStudio(
            projectId = "p3",
            beats = listOf(CreatorScriptBeat(label = "Opening", narration = "Existing narration")),
        )
        val workspace = CreatorContentWorkspace(scriptStudio = script, audience = "   ", angle = "\n")
        assertTrue(CreatorPlanScriptBridge.fields(workspace).isEmpty())
        assertEquals(script, workspace.scriptStudio)
        assertEquals("Existing narration", workspace.scriptStudio?.compiledNarration())
    }

    @Test fun legacyHookIsShownWhenStructuredScriptHasNoSelectedHook() {
        val workspace = CreatorContentWorkspace(hook = "An original opening", angle = "Study composition")
        val fields = CreatorPlanScriptBridge.fields(workspace)
        assertEquals(listOf("Angle", "Hook"), fields.map { it.label })
        assertEquals("An original opening", fields.last().value)
    }
}
