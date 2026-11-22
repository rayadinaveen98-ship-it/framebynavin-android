package com.framebynavin.app.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.VoicePersona
import com.framebynavin.app.reminders.RX03BReminderScripts
import com.framebynavin.app.reminders.RX03BVoiceDomain
import com.framebynavin.app.reminders.RX03BVoicePackPreferences
import com.framebynavin.app.reminders.VoicePersonaEngine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/**
 * RX03B: independent Personal and 21-Day Challenge voice-pack defaults.
 *
 * Unlike Creator's per-task saved persona, these are domain-wide defaults:
 * no Personal/Challenge schema migrations or edits to occurrence records.
 * Null keeps the exact legacy TTS voice and script. No voice is downloaded.
 */
@Composable
internal fun RX03BReminderVoicePacksSettings() {
    val context = LocalContext.current
    var domain by rememberSaveable { mutableStateOf(RX03BVoiceDomain.PERSONAL) }
    var revision by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selected = remember(domain, revision) {
        RX03BVoicePackPreferences.selected(context, domain)
    }
    val domainTitle = if (domain == RX03BVoiceDomain.PERSONAL)
        "Personal" else "21-day Challenge"
    fun sample(pack: VoicePersona): String =
        when (domain) {
            RX03BVoiceDomain.PERSONAL -> RX03BReminderScripts.personalPreview(pack)
            RX03BVoiceDomain.CHALLENGE_21 -> RX03BReminderScripts.challengePreview(pack)
        }

    DisposableEffect(Unit) {
        onDispose { V403VoicePreviewPlayer.stop() }
    }
    Column(
        Modifier.fillMaxWidth().testTag("rx03b-domain-voice-packs"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("PERSONAL & CHALLENGE VOICE PACKS",
            color = MutedGold, fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp, fontSize = 11.sp)
        Text("All previews and live Voice reminders start with a greeting using " +
            "your saved first name when available. Repeats keep the reminder " +
            "instructions without repeating the greeting.",
            color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
        val personalPack = remember(revision) {
            RX03BVoicePackPreferences.selected(context, RX03BVoiceDomain.PERSONAL)
        }
        val challengePack = remember(revision) {
            RX03BVoicePackPreferences.selected(context, RX03BVoiceDomain.CHALLENGE_21)
        }
        Text(
            "Personal: " + (personalPack?.let(VoicePersonaEngine::label) ?: "Original") +
                "   ·   21-Day: " +
                (challengePack?.let(VoicePersonaEngine::label) ?: "Original"),
            color = MutedGold,
            fontSize = 11.sp,
            modifier = Modifier.testTag("rx03b-domain-pack-summary"),
        )
        OutlinedButton(
            onClick = {
                expanded = !expanded
                V403VoicePreviewPlayer.stop()
            },
            modifier = Modifier.fillMaxWidth().testTag("rx03b-expand-packs"),
        ) {
            Text(if (expanded) "HIDE VOICE PACK CHOICES" else "CUSTOMIZE VOICE PACKS")
        }
        if (expanded) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = domain == RX03BVoiceDomain.PERSONAL,
                onClick = {
                    V403VoicePreviewPlayer.stop()
                    domain = RX03BVoiceDomain.PERSONAL
                    message = ""
                },
                label = { Text("Personal") },
                modifier = Modifier.weight(1f).testTag("rx03b-personal-tab"),
            )
            FilterChip(
                selected = domain == RX03BVoiceDomain.CHALLENGE_21,
                onClick = {
                    V403VoicePreviewPlayer.stop()
                    domain = RX03BVoiceDomain.CHALLENGE_21
                    message = ""
                },
                label = { Text("21-Day Challenge") },
                modifier = Modifier.weight(1f).testTag("rx03b-challenge-tab"),
            )
        }
        Surface(color = CinemaSurface, shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("$domainTitle voice", color = ProjectorIvory,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    if (selected == null) "Original Android voice & wording (unchanged)"
                    else VoicePersonaEngine.label(selected) + " · saved for $domainTitle",
                    color = MutedGold, fontSize = 12.sp,
                    modifier = Modifier.testTag("rx03b-selected-pack"),
                )
                if (selected != null) {
                    Text(
                        sample(selected),
                        color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                        modifier = Modifier.testTag("rx03b-selected-script-example"),
                    )
                    TextButton(
                        onClick = {
                            if (RX03BVoicePackPreferences.set(context, domain, null)) {
                                revision++
                                message = "Original Android voice and wording restored for $domainTitle."
                            } else message = "Could not restore the original voice preference."
                        },
                        modifier = Modifier.testTag("rx03b-restore-original"),
                    ) { Text("RESTORE ORIGINAL VOICE", fontSize = 11.sp) }
                }
            }
        }
        Text("CHOOSE A PACK", color = ProjectorIvory, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, letterSpacing = .7.sp)
        VoicePersona.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { pack ->
                    val isSelected = selected == pack
                    Surface(
                        color = CinemaSurface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                            .testTag("rx03b-pack-" + domain.name + "-" + pack.name),
                    ) {
                        Column(
                            Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(VoicePersonaEngine.label(pack),
                                color = ProjectorIvory, fontWeight = FontWeight.Bold,
                                fontSize = 12.sp)
                            Text(
                                sample(pack),
                                color = MutedText, fontSize = 10.sp,
                                lineHeight = 14.sp,
                            )
                            OutlinedButton(
                                onClick = {
                                    V403VoicePreviewPlayer.play(
                                        context,
                                        pack,
                                        sampleTextOverride = sample(pack),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                                    .testTag("rx03b-listen-" + domain.name + "-" + pack.name),
                            ) { Text("LISTEN", fontSize = 10.sp) }
                            OutlinedButton(
                                onClick = {
                                    if (RX03BVoicePackPreferences.set(context, domain, pack)) {
                                        revision++
                                        message = VoicePersonaEngine.label(pack) +
                                            " saved for $domainTitle reminders."
                                    } else message = "Could not save the voice pack."
                                },
                                modifier = Modifier.fillMaxWidth()
                                    .testTag("rx03b-select-" + domain.name + "-" + pack.name),
                            ) { Text(if (isSelected) "SELECTED" else "USE PACK",
                                color = if (isSelected) RecRed else ProjectorIvory,
                                fontSize = 10.sp) }
                        }
                    }
                }
            }
        }
        if (message.isNotBlank()) Text(
            message, color = MutedGold, fontSize = 11.sp,
            modifier = Modifier.testTag("rx03b-pack-status"),
        )
        Text(
            "These are local speech styles, not eight guaranteed different human " +
                "speakers. An assigned installed speaker from Real Speaker " +
                "Identities is also used when available. Existing repeat, Snooze, " +
                "music and challenge completion rules stay unchanged. " +
                "This setting is shared by reminders of the selected type.",
            color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
        )
        }
    }
}
