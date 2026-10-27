package com.framebynavin.app.data

/** Live, read-only context from the saved project. Script Studio never clones or edits Plan fields. */
data class CreatorPlanBriefField(val label: String, val value: String)

object CreatorPlanScriptBridge {
    fun fields(workspace: CreatorContentWorkspace): List<CreatorPlanBriefField> = buildList {
        fun addIfPresent(label: String, value: String) {
            value.trim().takeIf(String::isNotBlank)?.let { add(CreatorPlanBriefField(label, it)) }
        }
        addIfPresent("Audience", workspace.audience)
        addIfPresent("Viewer need", workspace.viewerProblem)
        addIfPresent("Promise", workspace.promise)
        addIfPresent("Angle", workspace.angle)
        addIfPresent("Hook", workspace.scriptStudio?.selectedHook().orEmpty().ifBlank { workspace.hook })
    }
}
