from pathlib import Path

ROOT = Path('.')
BUILD = ROOT / 'app/build.gradle.kts'
FRAMES = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V175BestFramesUi.kt'
REMINDERS = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt'
ALARM = ROOT / 'app/src/main/java/com/framebynavin/app/reminders/AlarmActivity.kt'
VOICE = ROOT / 'app/src/main/java/com/framebynavin/app/reminders/VoiceReminderActivity.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


# Version
build = BUILD.read_text()
build = replace_once(build, 'versionCode = 56', 'versionCode = 57', 'versionCode')
build = replace_once(
    build,
    'versionName = "1.8.0-product-alpha16"',
    'versionName = "1.8.0-product-alpha17"',
    'versionName',
)
BUILD.write_text(build)

# Home hero: Alpha17 intentionally resets the legacy Best Frames library once.
# Fresh installs stay empty; upgrades clear the old carried-over gallery exactly once,
# then any images the user adds in Alpha17+ persist normally.
frames = FRAMES.read_text()
frames = replace_once(
    frames,
    '    private const val KEY_ORDER = "frame_order"\n',
    '    private const val KEY_ORDER = "frame_order"\n    private const val KEY_ALPHA17_RESET = "alpha17_empty_hero_reset_done"\n',
    'alpha17 hero reset key',
)
frames = replace_once(
    frames,
    '''    fun current(context: Context): List<File> {\n        val directory = File(context.filesDir, DIRECTORY)\n        val names = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)\n            .getString(KEY_ORDER, "")\n            .orEmpty()\n            .split('\\n')\n            .filter { it.isNotBlank() }\n        return names.map { File(directory, it) }.filter { it.isFile }\n    }\n''',
    '''    fun current(context: Context): List<File> {\n        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)\n        if (!prefs.getBoolean(KEY_ALPHA17_RESET, false)) {\n            File(context.filesDir, DIRECTORY).deleteRecursively()\n            prefs.edit()\n                .remove(KEY_ORDER)\n                .putBoolean(KEY_ALPHA17_RESET, true)\n                .apply()\n            return emptyList()\n        }\n        val directory = File(context.filesDir, DIRECTORY)\n        val names = prefs\n            .getString(KEY_ORDER, "")\n            .orEmpty()\n            .split('\\n')\n            .filter { it.isNotBlank() }\n        return names.map { File(directory, it) }.filter { it.isFile }\n    }\n''',
    'one-time empty hero reset',
)
FRAMES.write_text(frames)

# Reminder Center: remove decorative/explanatory copy while retaining task identity,
# mode/time, grouping and all actions.
reminders = REMINDERS.read_text()
reminders = replace_once(
    reminders,
    '''                    Column(Modifier.weight(1f)) {\n                        Text("REMINDERS", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)\n                        Text("Stay on track", color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Black)\n                    }\n''',
    '''                    Text("REMINDERS", color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))\n''',
    'reminder header cleanup',
)
reminders = replace_once(
    reminders,
    '''                                Text("No active reminders", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)\n                                Text("Projects can still live in Today and Studio without an alert.", color = MutedText, fontSize = 9.5.sp, lineHeight = 14.sp)\n                                Spacer(Modifier.height(13.dp))\n''',
    '''                                Text("No active reminders", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)\n                                Spacer(Modifier.height(13.dp))\n''',
    'reminder empty copy cleanup',
)
reminders = replace_once(
    reminders,
    '''        val smartSummary = if (task.reminderMode == ReminderMode.SMART) {\n            val minutes = SmartEscalationPolicy.requiredWindowMinutes(task.priority, configStore.get(task))\n            if (task.priority == TaskPriority.NORMAL) "Normal Smart · notification only" else "${pPriorityLabel(task.priority)} Smart · ${minutes}m sequence"\n        } else null\n''',
    '',
    'remove reminder smart summary copy',
)
reminders = replace_once(
    reminders,
    '''                    Text("${pModeLabel(task.reminderMode)} · ${pFormatDateTime(task.reminderAtMillis)}", color = accent, fontSize = 9.sp)\n                    Text(smartSummary ?: "${task.platform} · ${CreatorWorkflowEngine.currentStage(task).label}", color = MutedText, fontSize = 8.6.sp)\n''',
    '''                    Text("${pModeLabel(task.reminderMode)} · ${pFormatDateTime(task.reminderAtMillis)}", color = accent, fontSize = 9.sp)\n''',
    'reminder card metadata cleanup',
)
reminders = replace_once(
    reminders,
    '                    Text("Choose how you want FrameByNavin to get your attention.", color = MutedText, fontSize = 9.3.sp)\n',
    '',
    'reminder setup helper cleanup',
)
reminders = replace_once(
    reminders,
    '                            Text("Edit how long Smart waits before the next unanswered step.", color = MutedText, fontSize = 8.8.sp)\n',
    '',
    'smart timing helper cleanup',
)
REMINDERS.write_text(reminders)

# Full-screen alarm: remove redundant labels/sentences. The alarm icon, current time,
# task title, due chip, notes and action controls remain.
alarm = ALARM.read_text()
alarm = replace_once(
    alarm,
    '''            Text("DEADLINE REMINDER", color = RecRed, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Black)\n            Spacer(Modifier.height(10.dp))\n            Text("This needs your attention.", color = MutedText, fontSize = 11.sp)\n            Spacer(Modifier.height(7.dp))\n''',
    '''            Spacer(Modifier.height(4.dp))\n''',
    'alarm redundant copy cleanup',
)
ALARM.write_text(alarm)

# Full-screen voice reminder: same principle. Keep the animated voice mark, task title,
# due chip, optional notes and actions; remove branding/labels/redundant stage text.
voice = VOICE.read_text()
voice = replace_once(
    voice,
    '        Text("FRAMEBYNAVIN", color = RecRed, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopCenter))\n\n',
    '',
    'voice top brand cleanup',
)
voice = replace_once(
    voice,
    '''            Text("VOICE REMINDER", color = MutedGold, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Black)\n            Spacer(Modifier.height(11.dp))\n            Text("Time to move this forward.", color = MutedText, fontSize = 11.sp)\n            Spacer(Modifier.height(7.dp))\n''',
    '''            Spacer(Modifier.height(4.dp))\n''',
    'voice redundant copy cleanup',
)
voice = replace_once(
    voice,
    '''            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {\n                Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFF17130F), border = BorderStroke(1.dp, MutedGold.copy(alpha = .28f))) {\n                    Text(task.dueLabel, color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))\n                }\n                Surface(shape = RoundedCornerShape(100.dp), color = CinemaSurface) {\n                    Text(stage.label.uppercase(), color = ProjectorIvory, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))\n                }\n            }\n''',
    '''            Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFF17130F), border = BorderStroke(1.dp, MutedGold.copy(alpha = .28f))) {\n                Text(task.dueLabel, color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))\n            }\n''',
    'voice stage chip cleanup',
)
voice = replace_once(
    voice,
    '    val stage = CreatorWorkflowEngine.currentStage(task)\n\n',
    '',
    'voice unused stage cleanup',
)
VOICE.write_text(voice)

print('Applied FrameByNavin v1.8 Alpha17 clean surfaces pass')
