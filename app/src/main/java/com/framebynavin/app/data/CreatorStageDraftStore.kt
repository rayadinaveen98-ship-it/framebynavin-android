package com.framebynavin.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * CW3 non-authoritative edit journal. A stage draft can only be resumed against
 * the same project, template, stage, workspace revision and restore generation.
 * It is NEVER merged into CreatorTask automatically.
 */
class CreatorStageDraftStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("creator_stage_drafts_cw3", Context.MODE_PRIVATE)
    private val store = TaskStore(appContext)
    private val generationAtOpen = CreatorDataGate.generation(appContext)

    private fun key(projectId: String, templateId: String, stageId: String): String =
        "stage:${projectId.length}:$projectId:${templateId.length}:$templateId:${stageId.length}:$stageId"

    fun load(projectId: String, revision: Long, templateId: String, stageId: String): CreatorStageWork? {
        val raw = prefs.getString(key(projectId, templateId, stageId), null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            if (root.optString("projectId") != projectId ||
                root.optString("templateId") != templateId ||
                root.optString("stageId") != stageId ||
                root.optLong("revision", -1L) != revision ||
                root.optLong("generation", -1L) != generationAtOpen ||
                CreatorDataGate.generation(appContext) != generationAtOpen
            ) return@runCatching null
            val payload = root.optJSONObject("payload") ?: return@runCatching null
            val stage = store.decodeStageWork(JSONArray().put(payload)).singleOrNull()
            stage?.takeIf { it.workflowTemplateId == templateId && it.stageId == stageId }
        }.getOrNull()
    }

    fun save(projectId: String, revision: Long, stage: CreatorStageWork) {
        CreatorDataGate.checkGeneration(appContext, generationAtOpen)
        val payload = store.encodeStageWork(listOf(stage)).getJSONObject(0)
        val root = JSONObject()
            .put("projectId", projectId).put("templateId", stage.workflowTemplateId)
            .put("stageId", stage.stageId).put("revision", revision)
            .put("generation", generationAtOpen).put("payload", payload)
        check(prefs.edit().putString(
            key(projectId, stage.workflowTemplateId, stage.stageId), root.toString()
        ).commit()) { "Could not retain the current stage draft" }
    }

    /** Clear only a current-generation draft unless the owner explicitly discards it. */
    fun clear(projectId: String, templateId: String, stageId: String, explicitDiscard: Boolean = false) {
        val key = key(projectId, templateId, stageId)
        if (!explicitDiscard) {
            val raw = prefs.getString(key, null) ?: return
            val root = runCatching { JSONObject(raw) }.getOrNull() ?: return
            if (root.optLong("generation", -1L) != generationAtOpen ||
                CreatorDataGate.generation(appContext) != generationAtOpen) return
        }
        check(prefs.edit().remove(key).commit()) { "Could not clear the saved stage draft" }
    }
}
