from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    (ROOT / path).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise RuntimeError(f"v132 migration could not find {label}")
    return text.replace(old, new, 1)


# Version the milestone without touching signing/release behavior.
gradle_path = "app/build.gradle.kts"
gradle = read(gradle_path)
gradle = replace_once(gradle, "versionCode = 131", "versionCode = 132", "versionCode")
gradle = replace_once(
    gradle,
    'versionName = "2.0.0-rc7-cine-pulse-motion-pass"',
    'versionName = "2.0.0-rc8-hero-visual-system"',
    "versionName",
)
write(gradle_path, gradle)

# Swap only the visual composable used by AlarmActivity. Action callbacks, reminder occurrence
# checks, ringing service, timeout and reschedule logic remain untouched.
alarm_path = "app/src/main/java/com/framebynavin/app/reminders/AlarmActivity.kt"
alarm = read(alarm_path)
alarm = replace_once(alarm, "NativeAlarmScreen(\n", "V132AlarmHeroScreen(\n", "alarm hero surface")
write(alarm_path, alarm)

# Same rule for voice: preserve TTS/service behavior and replace only the full-screen rendering.
voice_path = "app/src/main/java/com/framebynavin/app/reminders/VoiceReminderActivity.kt"
voice = read(voice_path)
voice = replace_once(voice, "VoiceReminderScreen(\n", "V132VoiceHeroScreen(\n", "voice hero surface")
write(voice_path, voice)

# Upgrade Quick Capture to the same volumetric renderer while retaining the already-approved
# idle -> explicit tap -> listen interaction and RMS-driven speech recognition path.
quick_path = "app/src/main/java/com/framebynavin/app/ui/V117VoiceQuickIdeaUi.kt"
quick = read(quick_path)
old_orb_call = '''                    FramePulseOrb(
                        active = listening,
                        rmsDb = rmsDb,
                        onTap = { if (listening) transcriber?.stop() else startVoiceFromOrb() },
                        modifier = Modifier.size(166.dp),
                    )'''
new_orb_call = '''                    V132HeroOrb(
                        state = if (listening) V132HeroOrbState.LISTENING else V132HeroOrbState.IDLE,
                        signal = if (listening) ((rmsDb + 2f) / 12f).coerceIn(0f, 1f) else 0f,
                        onTap = { if (listening) transcriber?.stop() else startVoiceFromOrb() },
                        modifier = Modifier.size(190.dp),
                    )'''
quick = replace_once(quick, old_orb_call, new_orb_call, "Quick Capture hero orb")
write(quick_path, quick)

print("v132 hero visual system applied")
