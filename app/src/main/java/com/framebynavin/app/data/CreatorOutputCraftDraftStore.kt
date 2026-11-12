package com.framebynavin.app.data

import android.content.Context
import org.json.JSONObject

/**
 * Non-authoritative CW4 editor draft, kept separate from canonical output
 * scripts and versions. A backup restore/revision change makes this journal
 * ineligible for automatic recovery without erasing the old draft bytes.
 */
data class CreatorOutputCraftDraft(
    val hook: String = "",
    val narration: String = "",
    val visualNotes: String = "",
    val creatorNotes: String = "",
    val startingParentRevision: Long = 0L,
    val usedParentSource: Boolean = false,
    val versionLabel: String = "",
    val versionUrl: String = "",
    val versionNotes: String = "",
)

class CreatorOutputCraftDraftStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("creator_output_craft_drafts_cw4", Context.MODE_PRIVATE)
    private val openedGeneration = CreatorDataGate.generation(appContext)

    private fun key(projectId: String, outputId: String) =
        "cw4:${projectId.length}:$projectId:${outputId.length}:$outputId"

    fun load(projectId: String, outputId: String, revision: Long): CreatorOutputCraftDraft? {
        val source = prefs.getString(key(projectId, outputId), null) ?: return null
        return runCatching {
            val root = JSONObject(source)
            if (root.optString("projectId") != projectId ||
                root.optString("outputId") != outputId ||
                root.optLong("revision", -1L) != revision ||
                root.optLong("generation", -1L) != openedGeneration ||
                CreatorDataGate.generation(appContext) != openedGeneration
            ) return@runCatching null
            val payload = root.getJSONObject("payload")
            CreatorOutputCraftDraft(
                hook = payload.optString("hook"), narration = payload.optString("narration"),
                visualNotes = payload.optString("visualNotes"),
                creatorNotes = payload.optString("creatorNotes"),
                startingParentRevision = payload.optLong("startingParentRevision", 0L),
                usedParentSource = payload.optBoolean("usedParentSource", false),
                versionLabel = payload.optString("versionLabel"),
                versionUrl = payload.optString("versionUrl"),
                versionNotes = payload.optString("versionNotes"),
            )
        }.getOrNull()
    }

    fun save(projectId: String, outputId: String, revision: Long, draft: CreatorOutputCraftDraft) {
        CreatorDataGate.checkGeneration(appContext, openedGeneration)
        val payload = JSONObject()
            .put("hook", draft.hook).put("narration", draft.narration)
            .put("visualNotes", draft.visualNotes).put("creatorNotes", draft.creatorNotes)
            .put("startingParentRevision", draft.startingParentRevision)
            .put("usedParentSource", draft.usedParentSource)
            .put("versionLabel", draft.versionLabel).put("versionUrl", draft.versionUrl)
            .put("versionNotes", draft.versionNotes)
        val root = JSONObject()
            .put("projectId", projectId).put("outputId", outputId)
            .put("revision", revision).put("generation", openedGeneration)
            .put("payload", payload)
        check(prefs.edit().putString(key(projectId, outputId), root.toString()).commit()) {
            "Could not retain the Output Craft draft"
        }
    }

    fun clear(projectId: String, outputId: String, explicitDiscard: Boolean = false) {
        val key = key(projectId, outputId)
        if (!explicitDiscard) {
            val root = prefs.getString(key, null)?.let { runCatching { JSONObject(it) }.getOrNull() }
                ?: return
            if (root.optLong("generation", -1L) != openedGeneration ||
                CreatorDataGate.generation(appContext) != openedGeneration) return
        }
        check(prefs.edit().remove(key).commit()) { "Could not clear Output Craft draft" }
    }
}
