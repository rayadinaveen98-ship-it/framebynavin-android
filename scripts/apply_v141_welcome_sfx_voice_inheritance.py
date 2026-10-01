#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(rel: str, old: str, new: str) -> None:
    path = ROOT / rel
    text = path.read_text(encoding="utf-8")
    if new in text:
        return
    if old not in text:
        raise SystemExit(f"v141 transform: expected source block missing in {rel}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "app/build.gradle.kts",
    '        versionCode = 140\n        versionName = "2.0.0-rc13-motion-reminder-voice"',
    '        versionCode = 141\n        versionName = "2.0.0-rc14-welcome-sfx-voice-inheritance"',
)

# New Project no longer owns a second voice preference. It receives the inherited Settings voice
# as read-only state, keeps Preview, and routes any change request to the canonical Settings screen.
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V20NewProjectWizard.kt",
    '''    voicePersona: VoicePersona,
    onVoicePersonaChange: (VoicePersona) -> Unit,
    onPreviewVoice: (VoicePersona) -> Unit,''',
    '''    voicePersona: VoicePersona,
    onPreviewVoice: (VoicePersona) -> Unit,''',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V20NewProjectWizard.kt",
    '''                        if (attentionPlan != ProjectAttentionPlan.OFF) {
                            Spacer(Modifier.height(14.dp))
                            V140VoiceStudioPicker(
                                selected = voicePersona,
                                onSelected = onVoicePersonaChange,
                                onPreview = onPreviewVoice,
                            )
                        }''',
    '''                        if (attentionPlan != ProjectAttentionPlan.OFF) {
                            Spacer(Modifier.height(14.dp))
                            V141InheritedVoiceCard(
                                voice = voicePersona,
                                onPreview = onPreviewVoice,
                                onOpenSettings = onOpenSettings,
                            )
                        }''',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt",
    '''            voicePersona = voice,
            onVoicePersonaChange = { voice = it },
            onPreviewVoice = { pComposerPreviewVoice(context, it) },''',
    '''            voicePersona = voice,
            onPreviewVoice = { pComposerPreviewVoice(context, it) },''',
)

# The full eight-style Voice Studio now lives only in Settings, where the persisted default belongs.
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt",
    '''            PSettingsHeading("VOICE", "Choose how reminder voices sound.")
            Spacer(Modifier.height(8.dp))
            VoicePersona.entries.forEach { voice ->
                val selected = settings.defaultVoicePersona == voice
                Surface(Modifier.fillMaxWidth().padding(bottom = 7.dp).clickable { onVoice(voice) }, RoundedCornerShape(16.dp), if (selected) Color(0xFF17130F) else CinemaSurface, border = BorderStroke(1.dp, if (selected) MutedGold.copy(alpha = .5f) else CinemaLine)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected, { onVoice(voice) }, colors = RadioButtonDefaults.colors(selectedColor = MutedGold))
                        Text(VoicePersonaEngine.label(voice), color = ProjectorIvory, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { pPreviewVoice(context, voice) }) {
                            Icon(Icons.Outlined.PlayArrow, null, tint = RecRed, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(3.dp)); Text("PREVIEW", color = RecRed, fontSize = 10.sp)
                        }
                    }
                }
            }''',
    '''            PSettingsHeading("VOICE", "Your default voice for every new project.")
            Spacer(Modifier.height(8.dp))
            V140VoiceStudioPicker(
                selected = settings.defaultVoicePersona,
                onSelected = onVoice,
                onPreview = { pPreviewVoice(context, it) },
            )
            Text(
                "New projects inherit this voice automatically. Existing projects keep their saved voice unless you edit them.",
                color = MutedText,
                fontSize = 8.2.sp,
                lineHeight = 11.5.sp,
                modifier = Modifier.padding(top = 2.dp),
            )''',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt",
    '''            tts?.speak("Backlot. This is ${VoicePersonaEngine.label(persona)}.", TextToSpeech.QUEUE_FLUSH, null, "polish-${persona.name}")''',
    '''            tts?.speak(VoicePersonaEngine.previewText(persona), TextToSpeech.QUEUE_FLUSH, null, "polish-${persona.name}")''',
)

print("v141 welcome sound / voice inheritance integration applied")
