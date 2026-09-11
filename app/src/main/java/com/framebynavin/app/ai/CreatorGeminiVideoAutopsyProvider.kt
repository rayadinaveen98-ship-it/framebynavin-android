package com.framebynavin.app.ai

import com.framebynavin.app.data.CreatorAiEvidencePack
import com.framebynavin.app.data.CreatorAiEvidencePackBuilder
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content

/** Live Firebase AI Logic provider for the optional Video Autopsy feature. */
class CreatorGeminiVideoAutopsyProvider {
    suspend fun analyze(pack: CreatorAiEvidencePack): CreatorAiAutopsyReport {
        require(pack.publicVideoUrl != null || pack.evidence.isNotEmpty()) {
            "Choose at least one evidence class before asking Gemini to analyze this video."
        }

        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(MODEL_NAME)
        val promptText = CreatorAiEvidencePackBuilder.videoAutopsyPrompt(pack)
        val prompt = content {
            pack.publicVideoUrl?.let { fileData(it, "video/mp4") }
            text(promptText)
        }
        val response = model.generateContent(prompt)
        val reportText = response.text?.trim().orEmpty()
        require(reportText.isNotBlank()) { "Gemini returned an empty Video Autopsy." }

        return CreatorAiAutopsyReport(
            schemaVersion = CreatorAiAutopsyReport.SCHEMA_VERSION,
            projectId = pack.projectId,
            videoId = pack.videoId,
            modelName = MODEL_NAME,
            generatedAtMillis = System.currentTimeMillis(),
            reportText = reportText,
            citedEvidenceIds = extractEvidenceIds(reportText),
            evidenceSchemaVersion = pack.schemaVersion,
            evidenceItemCount = pack.evidence.size,
        )
    }

    internal fun extractEvidenceIds(text: String): List<String> = EVIDENCE_REF
        .findAll(text)
        .map { it.groupValues[1] }
        .distinct()
        .toList()

    companion object {
        const val MODEL_NAME = "gemini-3.8-flash"
        private val EVIDENCE_REF = Regex("\\[([A-Za-z0-9._-]+)]")
    }
}

data class CreatorAiAutopsyReport(
    val schemaVersion: Int,
    val projectId: String,
    val videoId: String,
    val modelName: String,
    val generatedAtMillis: Long,
    val reportText: String,
    val citedEvidenceIds: List<String>,
    val evidenceSchemaVersion: Int,
    val evidenceItemCount: Int,
) {
    companion object { const val SCHEMA_VERSION = 1 }
}
