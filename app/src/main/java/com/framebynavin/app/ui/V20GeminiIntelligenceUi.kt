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
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*

@Composable
internal fun V20GeminiIntelligenceCard(postmortem: CreatorVideoPostmortem) {
    val context = LocalContext.current.applicationContext
    val store = remember { CreatorAiSettingsStore(context) }
    var consent by remember { mutableStateOf(store.load()) }
    var preview by remember { mutableStateOf<CreatorAiEvidencePack?>(null) }

    fun save(next: CreatorAiConsent) {
        consent = next
        store.save(next)
        preview = null
    }

    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(16.dp),
        Color(0xFF171719),
        border = BorderStroke(1.dp, RecRed.copy(alpha = .28f)),
    ) {
        Column(Modifier.padding(13.dp)) {
            Text("GEMINI INTELLIGENCE · OPTIONAL", color = RecRed, fontSize = 7.7.sp, fontWeight = FontWeight.Black, letterSpacing = .85.sp)
            Spacer(Modifier.height(4.dp))
            Text("AI is off by default", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(
                "Nothing is sent unless you enable AI and choose the exact evidence classes below.",
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
                    onClick = { preview = CreatorAiEvidencePackBuilder.build(postmortem, consent) },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedGold.copy(alpha = .18f), contentColor = ProjectorIvory),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("BUILD SAFE ANALYSIS PACK", fontSize = 7.8.sp, fontWeight = FontWeight.Black)
                }
            }

            preview?.let { pack ->
                Spacer(Modifier.height(8.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp), Color(0xFF202022)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("READY FOR PROVIDER", color = MutedGold, fontSize = 6.8.sp, fontWeight = FontWeight.Black)
                        Text(
                            "${pack.publicEvidenceCount} public evidence items · ${pack.privateEvidenceCount} private creator-data items",
                            color = ProjectorIvory,
                            fontSize = 8.2.sp,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Full scripts and research notes are withheld in v104. Every future AI claim must cite these evidence IDs.",
                            color = MutedText,
                            fontSize = 7.4.sp,
                            lineHeight = 10.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Provider status: Firebase AI Logic not connected yet. This build embeds no Gemini API key and cannot silently call Gemini.",
                color = MutedText.copy(alpha = .78f),
                fontSize = 7.1.sp,
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
