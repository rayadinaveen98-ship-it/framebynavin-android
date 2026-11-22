package com.framebynavin.app.ui

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.VoicePersona
import com.framebynavin.app.reminders.RX03InstalledVoice
import com.framebynavin.app.reminders.RX03VoiceIdentityRegistry
import com.framebynavin.app.reminders.RX03VoiceSelection
import com.framebynavin.app.reminders.VoicePersonaEngine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import java.util.Locale

/**
 * RX03 Local Voice Identity Studio. This is NOT a neural voice shop and cannot
 * guarantee eight different voices. It only exposes real installed, offline
 * speaker IDs from the currently selected Android TTS engine, and makes
 * identity assignments explicit, reversible and opt-in.
 *
 * Preview never saves. "Use this speaker" explicitly commits one ID for one
 * existing Creator persona. Other device voice engines are not installed here.
 */
@Composable
internal fun RX03LocalVoiceIdentityStudio() {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedPersona by rememberSaveable { mutableStateOf(VoicePersona.WARM) }
    var localVoices by remember { mutableStateOf<List<RX03InstalledVoice>>(emptyList()) }
    var engineId by remember { mutableStateOf("") }
    var inventoryStatus by remember { mutableStateOf("Checking offline voices…") }
    var updateCounter by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }

    DisposableEffect(context, expanded) {
        if (!expanded) return@DisposableEffect onDispose { }
        var disposed = false
        var tts: TextToSpeech? = null
        val handler = Handler(Looper.getMainLooper())
        inventoryStatus = "Checking offline voices…"
        tts = TextToSpeech(context.applicationContext) { status ->
            handler.post {
                if (disposed) return@post
                val engine = tts
                if (status != TextToSpeech.SUCCESS || engine == null) {
                    localVoices = emptyList()
                    inventoryStatus = "Android's text-to-speech engine is unavailable."
                    return@post
                }
                val language = Locale.getDefault()
                val languageCheck = engine.setLanguage(language)
                engineId = runCatching { engine.defaultEngine }.getOrNull().orEmpty()
                localVoices = if (
                    languageCheck == TextToSpeech.LANG_MISSING_DATA ||
                    languageCheck == TextToSpeech.LANG_NOT_SUPPORTED
                ) emptyList() else RX03VoiceIdentityRegistry.installedOffline(engine, language)
                inventoryStatus = when {
                    localVoices.isEmpty() -> "No compatible offline voice found for " +
                        language.displayLanguage + ". Check Android speech settings."
                    else -> localVoices.size.toString() +
                        " installed offline speaker IDs for " + language.displayLanguage +
                        " · audition to judge real differences."
                }
            }
        }
        onDispose {
            disposed = true
            tts?.let { runCatching { it.stop(); it.shutdown() } }
            V403VoicePreviewPlayer.stop()
        }
    }

    Column(Modifier.fillMaxWidth().testTag("rx03-voice-identity-studio"),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("REAL SPEAKER IDENTITIES", color = MutedGold, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, letterSpacing = 1.sp)
        Text(
            "The eight existing Backlot styles are NOT eight guaranteed speakers. " +
                "Here you can hear the offline voices installed on this phone and " +
                "assign one to a Creator style.",
            color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
        )
        OutlinedButton(
            onClick = { expanded = !expanded; feedback = "" },
            modifier = Modifier.fillMaxWidth().testTag("rx03-toggle-studio"),
        ) { Text(if (expanded) "HIDE INSTALLED SPEAKERS" else "EXPLORE INSTALLED SPEAKERS") }
        if (expanded) {
            Text(inventoryStatus, color = MutedText, fontSize = 11.sp,
                lineHeight = 16.sp, modifier = Modifier.testTag("rx03-offline-voice-count"))
            Text("CHOOSE A BACKLOT STYLE", color = ProjectorIvory,
                fontWeight = FontWeight.Bold, fontSize = 11.sp)
            VoicePersona.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { persona ->
                        FilterChip(
                            selected = selectedPersona == persona,
                            onClick = { selectedPersona = persona; feedback = "" },
                            label = { Text(VoicePersonaEngine.label(persona), fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag(
                                "rx03-persona-" + persona.name.lowercase()
                            ),
                        )
                    }
                }
            }
            val current = remember(selectedPersona, updateCounter) {
                RX03VoiceIdentityRegistry.saved(context, selectedPersona)
            }
            Text(
                if (current == null) "This style uses its original device voice selection."
                else "Saved offline speaker: " + current.name,
                color = MutedGold, fontSize = 11.sp, lineHeight = 17.sp,
                modifier = Modifier.testTag("rx03-current-binding"),
            )
            if (current != null) {
                TextButton(
                    onClick = {
                        if (RX03VoiceIdentityRegistry.clear(context, selectedPersona)) {
                            updateCounter++
                            feedback = "Original Backlot voice selection restored."
                        } else feedback = "Could not reset the speaker assignment."
                    },
                    modifier = Modifier.testTag("rx03-reset"),
                ) { Text("RESET TO ORIGINAL STYLE") }
            }
            Text(
                "Every preview below speaks the same sentence. Voice labels come " +
                    "from Android; different labels do not guarantee a different human sound.",
                color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
            )
            localVoices.take(12).forEachIndexed { index, voice ->
                val selected = current != null &&
                    current.enginePackage == engineId &&
                    current.name == voice.name &&
                    current.languageTag == voice.languageTag
                Surface(
                    color = CinemaSurface,
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth()
                        .testTag("rx03-voice-option-" + index),
                ) {
                    Column(Modifier.padding(11.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            voice.name,
                            color = ProjectorIvory, fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            voice.languageTag +
                                (if (selected) " · Saved for this style" else " · Installed offline"),
                            color = if (selected) MutedGold else MutedText,
                            fontSize = 10.sp,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    V403VoicePreviewPlayer.play(
                                        context, selectedPersona,
                                        voiceNameToCompare = voice.name,
                                        compareSameSample = true,
                                    )
                                },
                                modifier = Modifier.weight(1f)
                                    .testTag("rx03-audition-" + index),
                            ) { Text("LISTEN", fontSize = 11.sp) }
                            OutlinedButton(
                                onClick = {
                                    val selection = RX03VoiceSelection(
                                        enginePackage = engineId,
                                        name = voice.name,
                                        languageTag = voice.languageTag,
                                    )
                                    if (RX03VoiceIdentityRegistry.save(
                                            context, selectedPersona, selection
                                        )) {
                                        updateCounter++
                                        feedback = "Saved for " +
                                            VoicePersonaEngine.label(selectedPersona) + "."
                                    } else feedback = "Could not save that speaker."
                                },
                                modifier = Modifier.weight(1f)
                                    .testTag("rx03-assign-" + index),
                                enabled = engineId.isNotBlank(),
                            ) { Text(if (selected) "SELECTED" else "USE SPEAKER", fontSize = 10.sp) }
                        }
                    }
                }
            }
            if (localVoices.size > 12) {
                Text("Showing the first 12 of " + localVoices.size +
                    " offline speaker IDs. Android speech settings can change availability.",
                    color = MutedText, fontSize = 11.sp)
            }
            if (feedback.isNotBlank()) Text(
                feedback, color = MutedGold, fontSize = 11.sp,
                modifier = Modifier.testTag("rx03-save-status"),
            )
        }
    }
}
