package com.framebynavin.app.ui

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.VoicePersona
import com.framebynavin.app.reminders.P2VoiceIdentityPolicy
import com.framebynavin.app.reminders.P2OfflineVoiceStore
import com.framebynavin.app.ui.theme.*
import java.util.Locale

/**
 * Point 2: opt-in audition of ACTUAL device TTS IDs, not new synthetic voices.
 * The section starts collapsed to preserve the existing Voice Settings surface.
 * No external request, model download, recording, microphone or purchase.
 */
@Composable
internal fun P2VoiceIdentityLab() {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var personaName by rememberSaveable { mutableStateOf(VoicePersona.WARM.name) }
    var language by rememberSaveable { mutableStateOf("en-IN") }
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var engineReady by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Checking offline speakers…") }
    var feedback by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    val persona = VoicePersona.valueOf(personaName)

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("p2-voice-identity-lab"),
        color = CinemaSurface, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("VOICE IDENTITY LAB", color = MutedGold, fontSize = 10.sp,
                letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            Text("Hear actual speakers, not pitch variations",
                color = ProjectorIvory, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold)
            Text("The eight Backlot options are voice styles. Your phone may have fewer distinct offline speaker IDs.",
                color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
            OutlinedButton(
                onClick = { expanded = !expanded; feedback = "" },
                modifier = Modifier.fillMaxWidth().testTag("p2-expand-voice-lab"),
            ) { Text(if (expanded) "CLOSE VOICE LAB" else "COMPARE OFFLINE VOICES") }

            if (expanded) {
                DisposableEffect(context) {
                    val handler = Handler(Looper.getMainLooper())
                    engineReady = false
                    status = "Checking offline speakers…"
                    val created = TextToSpeech(context.applicationContext) { initStatus ->
                        handler.post {
                            if (initStatus == TextToSpeech.SUCCESS) {
                                engineReady = true
                                status = "Offline voice catalog ready"
                            } else {
                                engineReady = false
                                status = "No compatible text-to-speech engine is available"
                            }
                        }
                    }
                    engine = created
                    onDispose {
                        engineReady = false
                        engine = null
                        created.stop()
                        created.shutdown()
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("en-IN" to "English", "te-IN" to "Telugu").forEach { (code, title) ->
                        FilterChip(
                            selected = language == code,
                            onClick = { language = code; feedback = "" },
                            label = { Text(title, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Text(status, color = MutedText, fontSize = 11.sp)
                Text("Choose a Backlot voice style", color = ProjectorIvory,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                VoicePersona.entries.chunked(4).forEach { group ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        group.forEach { p ->
                            FilterChip(selected = persona == p,
                                onClick = { personaName = p.name; feedback = "" },
                                label = { Text(p.name.lowercase().replaceFirstChar {
                                    it.uppercase()
                                }, fontSize = 9.sp) },
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
                val selected = remember(persona, revision) {
                    P2OfflineVoiceStore.selected(context, persona)
                }
                Text(if (selected == null)
                    "Using original Backlot voice style"
                else
                    "Selected offline ID: " + selected.voiceName,
                    color = if (selected == null) MutedText else MutedGold,
                    fontSize = 11.sp, lineHeight = 16.sp)
                if (selected != null) {
                    OutlinedButton(
                        onClick = {
                            if (P2OfflineVoiceStore.reset(context, persona)) {
                                revision++
                                feedback = "Original Backlot voice restored."
                            } else feedback = "Could not reset this speaker."
                        }, modifier = Modifier.fillMaxWidth().testTag("p2-reset-identity"),
                    ) { Text("USE ORIGINAL BACKLOT VOICE") }
                }
                val available = remember(engine, engineReady, language) {
                    if (engineReady && engine != null)
                        P2OfflineVoiceStore.installed(engine!!, Locale.forLanguageTag(language))
                    else emptyList()
                }
                Text(available.size.toString() + " installed offline voice IDs for " +
                    if (language == "te-IN") "Telugu" else "English",
                    color = MutedGold, fontSize = 11.sp)
                Text("All previews speak the same sentence at neutral pitch and speed. Different IDs may still sound alike.",
                    color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                if (available.isEmpty() && engineReady) {
                    Text("No compatible offline speakers found. Your existing Backlot voice still works using the normal device fallback.",
                        color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
                }
                available.take(12).forEachIndexed { index, option ->
                    Surface(color = CinemaSurfaceRaised, shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.fillMaxWidth().padding(11.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Speaker " + (index + 1),
                                color = ProjectorIvory, fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp)
                            Text(option.identity.languageTag + " · " +
                                option.identity.voiceName,
                                color = MutedText, fontSize = 10.sp, lineHeight = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                OutlinedButton(
                                    modifier = Modifier.weight(1f)
                                        .testTag("p2-listen-" + index),
                                    onClick = {
                                        val active = engine
                                        if (active != null) {
                                            active.stop()
                                            active.setPitch(1f)
                                            active.setSpeechRate(1f)
                                            if (active.setVoice(option.deviceVoice) ==
                                                TextToSpeech.SUCCESS) {
                                                val spoken = active.speak(
                                                    P2VoiceIdentityPolicy.auditionText(
                                                        option.identity.languageTag),
                                                    TextToSpeech.QUEUE_FLUSH, null,
                                                    "backlot-p2-audition")
                                                feedback = if (spoken == TextToSpeech.SUCCESS)
                                                    "Auditioning speaker " + (index + 1)
                                                else "This voice could not play."
                                            } else feedback = "This speaker is unavailable."
                                        }
                                    },
                                ) { Text("LISTEN", fontSize = 10.sp) }
                                Button(
                                    modifier = Modifier.weight(1f)
                                        .testTag("p2-choose-" + index),
                                    enabled = language == "en-IN" && engineReady,
                                    onClick = {
                                        val current = engine
                                        if (current != null &&
                                            P2OfflineVoiceStore.save(context, current,
                                                persona, option.identity)) {
                                            revision++
                                            feedback = "Speaker saved for this Creator style."
                                        } else feedback = "Could not save this speaker."
                                    },
                                ) { Text("USE SPEAKER", fontSize = 9.sp) }
                            }
                        }
                    }
                }
                if (language == "te-IN") {
                    Text("Telugu audition is for listening only. Creator reminder wording is currently English; Telugu delivery requires a separately approved translation and pronunciation pass.",
                        color = MutedGold, fontSize = 10.sp, lineHeight = 15.sp)
                } else {
                    Text("Selecting a speaker affects existing and future Creator reminders using this style. Reset it anytime to restore the previous sound.",
                        color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                }
                if (available.size > 12) Text("Showing the first 12 installed IDs.",
                    color = MutedText, fontSize = 10.sp)
                if (feedback.isNotBlank()) Text(feedback, color = MutedGold, fontSize = 11.sp)
            }
        }
    }
}
