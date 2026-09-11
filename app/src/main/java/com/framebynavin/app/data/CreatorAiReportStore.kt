package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.ai.CreatorAiAutopsyReport
import org.json.JSONArray
import org.json.JSONObject

/** Device-local memory for evidence-grounded AI reports. Consent remains in a separate store. */
class CreatorAiReportStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(videoId: String): CreatorAiAutopsyReport? {
        val raw = prefs.getString(key(videoId), null) ?: return null
        return runCatching { decode(JSONObject(raw)) }.getOrNull()
    }

    fun save(report: CreatorAiAutopsyReport) {
        require(report.videoId.isNotBlank()) { "AI report requires a video id" }
        require(report.reportText.isNotBlank()) { "AI report cannot be empty" }
        check(prefs.edit().putString(key(report.videoId), encode(report).toString()).commit()) {
            "Could not save Video Autopsy"
        }
    }

    fun clear(videoId: String) {
        check(prefs.edit().remove(key(videoId)).commit()) { "Could not clear Video Autopsy" }
    }

    private fun encode(value: CreatorAiAutopsyReport): JSONObject = JSONObject()
        .put("schemaVersion", value.schemaVersion)
        .put("projectId", value.projectId)
        .put("videoId", value.videoId)
        .put("modelName", value.modelName)
        .put("generatedAtMillis", value.generatedAtMillis)
        .put("reportText", value.reportText)
        .put("citedEvidenceIds", JSONArray(value.citedEvidenceIds))
        .put("evidenceSchemaVersion", value.evidenceSchemaVersion)
        .put("evidenceItemCount", value.evidenceItemCount)

    private fun decode(obj: JSONObject): CreatorAiAutopsyReport {
        require(obj.optInt("schemaVersion") == CreatorAiAutopsyReport.SCHEMA_VERSION) {
            "Unsupported AI report version"
        }
        val ids = obj.optJSONArray("citedEvidenceIds") ?: JSONArray()
        return CreatorAiAutopsyReport(
            schemaVersion = obj.getInt("schemaVersion"),
            projectId = obj.optString("projectId"),
            videoId = obj.getString("videoId"),
            modelName = obj.optString("modelName"),
            generatedAtMillis = obj.optLong("generatedAtMillis"),
            reportText = obj.getString("reportText"),
            citedEvidenceIds = (0 until ids.length()).map(ids::getString),
            evidenceSchemaVersion = obj.optInt("evidenceSchemaVersion", 1),
            evidenceItemCount = obj.optInt("evidenceItemCount", 0),
        )
    }

    private fun key(videoId: String) = "video_${videoId.trim()}"

    companion object { private const val PREFS = "creator_ai_reports_v105" }
}
