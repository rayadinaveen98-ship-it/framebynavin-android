package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ai.CreatorAiAutopsyReport
import com.framebynavin.app.ai.CreatorGeminiVideoAutopsyProvider
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
internal fun V20GeminiIntelligenceCard(postmortem: CreatorVideoPostmortem) {
    val context = LocalContext.current.applicationContext
    val settingsStore = remember { CreatorAiSettingsStore(context) }
    val reportStore = remember { CreatorAiReportStore(context) }
    val provider = remember { CreatorGeminiVideoAutopsyProvider() }
    val scope = rememberCoroutineScope()

    var consent by remember { mutableStateOf(settingsStore.load()) }
    var preview by remember(postmortem.videoId) { mutableStateOf<CreatorAiEvidencePack?>(null) }
    var report by remember(postmortem.videoId) { mutableStateOf(reportStore.load(postmortem.videoId)) }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun save(next: CreatorAiConsent) {
        consent = next
        settingsStore.save(next)
        preview = null
        error = null
    }

    fun analyze(pack: CreatorAiEvidencePack) {
        if (running) return
        running = true
        error = null
        scope.launch {
            runCatching { provider.analyze(pack) }
                .onSuccess {
                    reportStore.save(it)
                    report = it
                }
                .onFailure { error = friendlyAiError(it) }
            running = false
        }
    }

    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(16.dp),
        Color(0xFF171719),
        border = BorderStroke(1.dp, RecRed.copy(alpha = .28f)),
    ) {
        Column(Modifier.padding(13.dp)) {
            Text("GEMINI VIDEO AUTOPSY · OPTIONAL", color = RecRed, fontSize = 7.7.sp, fontWeight = FontWeight.Black, letterSpacing = .85.sp)
            Spacer(Modifier.height(4.dp))
            Text("AI is off by default", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(
                "Nothing is sent unless you enable AI and choose the exact evidence classes below. Firebase App Check protects live requests.",
                color = MutedText,
                fontSize = 7.8.sp,
                lineHeight = 11.sp,
            )

            Spacer(Modifier.height(9.dp))
            AiConsentRow("Enable optional AI", consent.enabled) { save(consent.copy(enabled = it)) }
            if (consent.enabled) {
                AiConsentRow("Public YouTube video", consent.sharePublicVideo) { save(consent.copy(sharePublicVideo = it)) }
                AiConsentRow("Project brief + selected hook", consent.shareProjectContext) { save(consent.copy(shareProjectContext = it)) }
                AiConsentRow("YouTube performance evidence", consent.sharePerformance) { save(consent.copy(sharePerformance = it)) }
                AiConsentRow("Your learning note", consent.shareCreatorLearning) { save(consent.copy(shareCreatorLearning = it)) }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        preview = CreatorAiEvidencePackBuilder.build(postmortem, consent)
                        error = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedGold.copy(alpha = .18f), contentColor = ProjectorIvory),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("REVIEW SAFE ANALYSIS PACK", fontSize = 7.8.sp, fontWeight = FontWeight.Black)
                }
            }

            preview?.let { pack ->
                Spacer(Modifier.height(8.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp), Color(0xFF202022)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("READY FOR GEMINI", color = MutedGold, fontSize = 6.8.sp, fontWeight = FontWeight.Black)
                        Text(
                            "${pack.publicEvidenceCount} public evidence items · ${pack.privateEvidenceCount} private creator-data items",
                            color = ProjectorIvory,
                            fontSize = 8.2.sp,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Full scripts and research notes remain withheld. Gemini is instructed to cite evidence IDs and to label inference rather than invent causation.",
                            color = MutedText,
                            fontSize = 7.4.sp,
                            lineHeight = 10.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            enabled = !running && (pack.publicVideoUrl != null || pack.evidence.isNotEmpty()),
                            onClick = { analyze(pack) },
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(if (running) "ANALYZING…" else "ANALYZE VIDEO WITH GEMINI", fontSize = 7.8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp), Color(0xFF2A1718)) {
                    Text(it, modifier = Modifier.padding(10.dp), color = Color(0xFFFFB7B7), fontSize = 7.7.sp, lineHeight = 11.sp)
                }
            }

            report?.let { saved ->
                Spacer(Modifier.height(10.dp))
                AiAutopsyReport(saved)
                TextButton(
                    onClick = {
                        reportStore.clear(postmortem.videoId)
                        report = null
                    },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("CLEAR SAVED AUTOPSY", color = MutedText, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Live provider: Firebase AI Logic · ${CreatorGeminiVideoAutopsyProvider.MODEL_NAME}. Debug APKs use App Check's debug provider; release builds use Play Integrity.",
                color = MutedText.copy(alpha = .78f),
                fontSize = 7.1.sp,
                lineHeight = 10.sp,
            )
        }
    }
}

@Composable
private fun AiAutopsyReport(report: CreatorAiAutopsyReport) {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(12.dp),
        Color(0xFF121A17),
        border = BorderStroke(1.dp, MutedGold.copy(alpha = .2f)),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text("SAVED VIDEO AUTOPSY", color = MutedGold, fontSize = 6.8.sp, fontWeight = FontWeight.Black)
            Text(
                "${report.modelName} · ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(report.generatedAtMillis))}",
                color = MutedText,
                fontSize = 7.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(report.reportText, color = ProjectorIvory, fontSize = 8.2.sp, lineHeight = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                if (report.citedEvidenceIds.isEmpty()) "No evidence IDs were cited in this response. Treat it as low-confidence AI commentary."
                else "Evidence cited: ${report.citedEvidenceIds.joinToString(", ")}",
                color = if (report.citedEvidenceIds.isEmpty()) RecRed else MutedGold,
                fontSize = 6.9.sp,
                lineHeight = 10.sp,
            )
        }
    }
}

@Composable
private fun AiConsentRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = ProjectorIvory, fontSize = 8.2.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(checked = checked, onCheckedChange = onChecked, modifier = Modifier.height(26.dp))
    }
}

private fun friendlyAiError(error: Throwable): String {
    val raw = error.message.orEmpty()
    return when {
        raw.contains("app check", ignoreCase = true) || raw.contains("403") ->
            "Firebase App Check has not accepted this debug build yet. Register the debug token from Logcat in Firebase App Check, then retry."
        raw.contains("model", ignoreCase = true) && raw.contains("not found", ignoreCase = true) ->
            "The configured Gemini model is not available to this Firebase project yet. No creator data was changed."
        raw.contains("network", ignoreCase = true) || raw.contains("timeout", ignoreCase = true) ->
            "Gemini could not be reached. Check the connection and retry; your project and saved postmortem are unchanged."
        else -> "Gemini Video Autopsy could not complete. Nothing was changed. ${raw.take(180)}"
    }
}
