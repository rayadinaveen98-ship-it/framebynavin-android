#!/usr/bin/env python3
"""Protect visible Backlot V200 branding while preserving stable Android identifiers."""
from pathlib import Path
import re

root = Path("app/src/main")
src = root / "java/com/framebynavin/app"
icon = "R.drawable.backlot_v198_notification_monochrome"
producers = {
    "reminders/ReminderNotifications.kt": 1,
    "reminders/CreatorContextNudgeWorker.kt": 1,
    "reminders/CreatorAutomationWorkers.kt": 1,
    "reminders/IdeaReminderSystem.kt": 1,
    "reminders/AlarmRingingService.kt": 1,
    "reminders/VoiceReminderService.kt": 1,
    "reminders/ProjectPulseNextStagePrompt.kt": 2,
}
def icons(content):
    return re.findall(r"\.setSmallIcon\(\s*([^)]*?)\s*\)", content)

for name, count in producers.items():
    found = icons((src / name).read_text())
    assert len(found) == count, f"{name}: expected {count}, found {found}"
    assert all(x.strip() == icon for x in found), f"{name}: old/generic icon {found}"

for path in src.rglob("*.kt"):
    content = path.read_text()
    if "NotificationCompat.Builder(" in content:
        found = icons(content)
        assert found and all(x.strip() == icon for x in found), f"{path}: unbranded icons {found}"

picker = (src / "ui/V133BacklotAppIcon.kt").read_text()
assert "painterResource(R.drawable.backlot_v198_approved_icon)" in picker
assert "BacklotAppIcon.entries.chunked" not in picker
assert not re.search(r"R\.drawable\.backlot_icon_(?:cinematic|minimal|light|horizontal)", picker)
manifest = (root / "AndroidManifest.xml").read_text()
assert manifest.count('android:icon="@mipmap/backlot_v198_launcher"') == 5
assert 'android:label="Backlot"' in manifest
theme = (root / "res/values/themes.xml").read_text()
assert 'windowSplashScreenAnimatedIcon">@drawable/backlot_v198_approved_icon' in theme
launch = (src / "ui/V131LaunchGate.kt").read_text()
assert "V199ParticleWelcome(" in launch and "V174CinematicWelcome(" not in launch
host = (src / "ui/FrameByNavinV101BApp.kt").read_text()
assert "PTab.TODAY -> PTodayScreen(" in host and "V133CreatorHome(" not in host
gradle = Path("app/build.gradle.kts").read_text()
assert "versionCode = 221" in gradle
assert 'versionName = "2.7.1-v201-respectful-nudges-preview"' in gradle
assert 'applicationId = "com.framebynavin.app"' in gradle
print("V201 inherited brand audit passed: 8 notification small icons, Settings, launcher, splash, startup, Home and application ID.")

# V201: discretionary nudges remain opt-in and bounded; remind/alarm flows unchanged.
policy = (src / "reminders/V201ContextNudgePolicy.kt").read_text()
worker = (src / "reminders/CreatorContextNudgeWorker.kt").read_text()
assert "GLOBAL_COOLDOWN_MS = 24L" in policy
assert "SAME_KEY_COOLDOWN_MS = 72L" in policy
assert "QuietHoursPolicy.active(" in policy
assert "mayShow(" in policy and "V201ContextNudgePolicy.mayShow(" in worker
assert "if (!settings.contextNudgesEnabled)" in worker
assert "areNotificationsEnabled()" in worker
assert "POST_NOTIFICATIONS" in worker
assert "getNotificationChannel(CHANNEL_ID)" in worker
assert "private const val CHANNEL_ID = \"creator_context_nudges\"" in worker
assert "ExistingPeriodicWorkPolicy.UPDATE" in worker
assert "R.drawable.backlot_v198_notification_monochrome" in worker
print("V201 optional creator nudge acceptance rules passed.")
