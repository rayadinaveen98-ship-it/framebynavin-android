#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(rel: str, old: str, new: str) -> None:
    path = ROOT / rel
    text = path.read_text(encoding="utf-8")
    if new in text:
        return
    if old not in text:
        raise SystemExit(f"v140 transform: expected source block missing in {rel}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "app/build.gradle.kts",
    '        versionCode = 139\n        versionName = "2.0.0-rc12-theme-system-finalization"',
    '        versionCode = 140\n        versionName = "2.0.0-rc13-motion-reminder-voice"',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/reminders/AlarmActivity.kt",
    '''            Box(Modifier.size(92.dp).background(RecRed.copy(alpha = 0.13f), CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(62.dp).background(RecRed.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Alarm, null, tint = RecRed, modifier = Modifier.size(34.dp))
                }
            }''',
    '''            V140AlarmPulse()''',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/reminders/VoiceReminderActivity.kt",
    '''    val transition = rememberInfiniteTransition(label = "voice")
    val pulse = transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "voicePulse"
    )
''',
    '''''',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/reminders/VoiceReminderActivity.kt",
    '''            Box(
                Modifier.size(104.dp).scale(pulse.value).background(MutedGold.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(20.dp, 37.dp, 27.dp, 46.dp, 24.dp).forEach { height ->
                        Box(Modifier.width(4.dp).height(height).background(MutedGold, RoundedCornerShape(10.dp)))
                    }
                }
            }
''',
    '''            V140VoicePulse()
''',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/reminders/VoiceReminderService.kt",
    '''        val stage = CreatorWorkflowEngine.currentStage(task).label
        val text = buildString {
            append("Backlot. ${task.title}. This is your $urgency.")
            append(" Current stage: $stage.")
            if (task.notes.isNotBlank() && index == 0) append(" ${task.notes}")
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "framebynavin-voice-${task.id}-$index")''',
    '''        val stage = CreatorWorkflowEngine.currentStage(task).label
        val text = VoicePersonaEngine.reminderText(
            persona = task.voicePersona,
            title = task.title,
            urgency = urgency,
            stage = stage,
            notes = task.notes,
            includeNotes = index == 0,
        )
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "framebynavin-voice-${task.id}-$index")''',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt",
    '''            priority = priority,
            onPriorityChange = { priority = it },
            notes = notes,''',
    '''            priority = priority,
            onPriorityChange = { priority = it },
            voicePersona = voice,
            onVoicePersonaChange = { voice = it },
            onPreviewVoice = { pComposerPreviewVoice(context, it) },
            notes = notes,''',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt",
    '''                    pulsePreview?.let { pulse ->''',
    '''                    if (attentionPlan != ProjectAttentionPlan.OFF) {
                        Spacer(Modifier.height(16.dp))
                        V140VoiceStudioPicker(
                            selected = voice,
                            onSelected = { voice = it },
                            onPreview = { pComposerPreviewVoice(context, it) },
                        )
                    }

                    pulsePreview?.let { pulse ->''',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt",
    '''            tts?.speak("Backlot. This is ${VoicePersonaEngine.label(persona)}.", TextToSpeech.QUEUE_FLUSH, null, "composer-${persona.name}")''',
    '''            tts?.speak(VoicePersonaEngine.previewText(persona), TextToSpeech.QUEUE_FLUSH, null, "composer-${persona.name}")''',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V20NewProjectWizard.kt",
    '''    priority: TaskPriority,
    onPriorityChange: (TaskPriority) -> Unit,
    notes: String,''',
    '''    priority: TaskPriority,
    onPriorityChange: (TaskPriority) -> Unit,
    voicePersona: VoicePersona,
    onVoicePersonaChange: (VoicePersona) -> Unit,
    onPreviewVoice: (VoicePersona) -> Unit,
    notes: String,''',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V20NewProjectWizard.kt",
    '''                        if (attentionPlan == ProjectAttentionPlan.CUSTOM) {''',
    '''                        if (attentionPlan != ProjectAttentionPlan.OFF) {
                            Spacer(Modifier.height(14.dp))
                            V140VoiceStudioPicker(
                                selected = voicePersona,
                                onSelected = onVoicePersonaChange,
                                onPreview = onPreviewVoice,
                            )
                        }

                        if (attentionPlan == ProjectAttentionPlan.CUSTOM) {''',
)

print("v140 motion/reminder/voice integration applied")
