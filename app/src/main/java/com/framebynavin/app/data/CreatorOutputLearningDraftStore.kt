package com.framebynavin.app.data

import android.content.Context
import org.json.JSONObject

/** Non-authoritative UX05.1E local journal; no publication or project data writes. */
data class CreatorOutputLearningDraft(
    val expectation: String = "",
    val observedOutcome: String = "",
    val whatWorked: String = "",
    val improveNext: String = "",
    val nextAction: String = "",
    val metricLabel: String = "",
    val metricValue: String = "",
    val metricSource: String = "",
) {
    fun hasContent(): Boolean = listOf(
        expectation, observedOutcome, whatWorked, improveNext, nextAction,
        metricLabel, metricValue, metricSource,
    ).any { it.isNotBlank() }
}

/**
 * Mirrors the existing Stage Desk draft epoch/revision fence. Stale drafts are
 * retained in preferences, not applied to a different project revision or
 * a new restore generation. Explicit output learning snapshots are saved only
 * by TaskStore.appendOutputLearning, never by this journal.
 */
class CreatorOutputLearningDraftStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(
        "creator_output_learning_drafts_ux051e", Context.MODE_PRIVATE,
    )
    private val openedGeneration = CreatorDataGate.generation(app)

    private fun key(projectId: String, outputId: String) =
        "learning:${projectId.length}:$projectId:${outputId.length}:$outputId"

    fun load(projectId: String, outputId: String, revision: Long): CreatorOutputLearningDraft? {
        val raw = prefs.getString(key(projectId, outputId), null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            if (root.optString("projectId") != projectId ||
                root.optString("outputId") != outputId ||
                root.optLong("revision", -1L) != revision ||
                root.optLong("generation", -1L) != openedGeneration ||
                CreatorDataGate.generation(app) != openedGeneration
            ) return@runCatching null

            val value = root.getJSONObject("payload")
            CreatorOutputLearningDraft(
                expectation = value.optString("expectation"),
                observedOutcome = value.optString("observedOutcome"),
                whatWorked = value.optString("whatWorked"),
                improveNext = value.optString("improveNext"),
                nextAction = value.optString("nextAction"),
                metricLabel = value.optString("metricLabel"),
                metricValue = value.optString("metricValue"),
                metricSource = value.optString("metricSource"),
            )
        }.getOrNull()
    }

    fun save(projectId: String, outputId: String, revision: Long, draft: CreatorOutputLearningDraft) {
        require(projectId.isNotBlank() && outputId.isNotBlank() && revision >= 0L)
        CreatorDataGate.checkGeneration(app, openedGeneration)
        val payload = JSONObject()
            .put("expectation", draft.expectation)
            .put("observedOutcome", draft.observedOutcome)
            .put("whatWorked", draft.whatWorked)
            .put("improveNext", draft.improveNext)
            .put("nextAction", draft.nextAction)
            .put("metricLabel", draft.metricLabel)
            .put("metricValue", draft.metricValue)
            .put("metricSource", draft.metricSource)
        val root = JSONObject()
            .put("projectId", projectId)
            .put("outputId", outputId)
            .put("revision", revision)
            .put("generation", openedGeneration)
            .put("payload", payload)
        check(prefs.edit().putString(key(projectId, outputId), root.toString()).commit()) {
            "Could not journal your unfinished output reflection"
        }
    }

    /** Discard is accepted only if the exact draft belongs to the current epoch/revision. */
    fun clear(projectId: String, outputId: String, revision: Long) {
        val storageKey = key(projectId, outputId)
        val source = prefs.getString(storageKey, null) ?: return
        val root = JSONObject(source)
        check(root.optString("projectId") == projectId &&
            root.optString("outputId") == outputId &&
            root.optLong("revision", -1L) == revision &&
            root.optLong("generation", -1L) == openedGeneration) {
            "The draft changed or belongs to another workspace revision"
        }
        CreatorDataGate.checkGeneration(app, openedGeneration)
        check(prefs.edit().remove(storageKey).commit()) {
            "Could not clear your unfinished output reflection"
        }
    }

    /** Clear an earlier revision after canonical save, but never clear a new-epoch draft. */
    fun clearSaved(projectId: String, outputId: String, savedFromRevision: Long) {
        clear(projectId, outputId, savedFromRevision)
    }
}
