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
assert "versionCode = 225" in gradle
assert 'versionName = "2.7.5-v205-nudge-pause-preview"' in gradle
assert 'applicationId = "com.framebynavin.app"' in gradle
print("V201 inherited brand audit passed: 8 notification small icons, Settings, launcher, splash, startup, Home and application ID.")

# V201: discretionary nudges remain opt-in and bounded; remind/alarm flows unchanged.
policy = (src / "reminders/V201ContextNudgePolicy.kt").read_text()
worker = (src / "reminders/CreatorContextNudgeWorker.kt").read_text()
assert "GLOBAL_COOLDOWN_MS = 24L" in policy
assert "SAME_KEY_COOLDOWN_MS = 72L" in policy
assert "QuietHoursPolicy.active(" in policy
selector = (src / "reminders/V203NudgeSelector.kt").read_text()
assert "mayShow(" in policy and "V201ContextNudgePolicy.mayShow(" in selector
assert "if (!settings.contextNudgesEnabled)" in worker
assert "areNotificationsEnabled()" in worker
assert "POST_NOTIFICATIONS" in worker
assert "getNotificationChannel(CHANNEL_ID)" in worker
assert "private const val CHANNEL_ID = \"creator_context_nudges\"" in worker
assert "ExistingPeriodicWorkPolicy.UPDATE" in worker
assert "R.drawable.backlot_v198_notification_monochrome" in worker
print("V201 optional creator nudge acceptance rules passed.")

# V202 contextual copy is grounded in CreatorContextNudge and taps open that exact project.
presentation = (src / "reminders/V202CreatorNudgeContent.kt").read_text()
assert "CreatorContextNudge" in presentation
assert 'nudge.key.substringBefore(\':\')' in presentation
assert 'nudge.message' in presentation and 'nudge.action' in presentation
worker = (src / "reminders/CreatorContextNudgeWorker.kt").read_text()
assert "V202CreatorNudgeContent.forNudge(nudge)" in worker
assert "action = CreatorWidgetContract.ACTION_OPEN_STUDIO" in worker
assert "putExtra(CreatorWidgetContract.EXTRA_TASK_ID, nudge.taskId)" in worker
assert "setContentTitle(message.title)" in worker
assert "setContentText(message.body)" in worker
assert "setSmallIcon(R.drawable.backlot_v198_notification_monochrome)" in worker
print("V202 nudge content and deep-link contracts passed.")

# V203 preserves the global cap while preventing project A-B-A cooldown evasion.
history = (src / "reminders/V203NudgeHistory.kt").read_text()
assert "MessageDigest.getInstance(\"SHA-256\")" in history
assert "prefs.getLong(storageKey(key), 0L)" in history
assert "legacyKey" in history and "editor.putLong(storageKey(legacyKey)" in history
assert "editor.commit()" in history
assert "RETENTION_MS = 7L" in history
assert "nudge.level == CreatorContextNudgeLevel.READY" in selector
assert "lastShownAtForKey(nudge.key)" in selector
assert "V201ContextNudgePolicy.SAME_KEY_COOLDOWN_MS" in selector
assert "V203NudgeSelector.choose(" in worker
assert "CreatorContextNudgeEngine.nudges(tasks, now)" in worker
assert "V203NudgeHistory.lastShown(prefs, key)" in worker
assert "V203NudgeHistory.recordShown(prefs, nudge.key, now)" in worker
print("V203 persistent per-key fatigue suppression + eligible project routing passed.")

# V204: never re-engage obsolete projects or old missed reminders.
freshness = (src / "reminders/V204NudgeFreshnessPolicy.kt").read_text()
assert "MAX_OVERDUE_AGE_MS = 7L" in freshness
assert "MAX_MISSED_CHECKIN_AGE_MS = 24L" in freshness
assert "task.status != TaskStatus.PLANNED" in freshness
assert "task.status != TaskStatus.WORKING" in freshness
assert "task.archivedAtMillis > 0L" in freshness
assert "task.reminderAtMillis" in freshness
assert "task.dueAtMillis" in freshness
assert "V204NudgeFreshnessPolicy.relevant(candidate, tasksById[candidate.taskId], now)" in worker
assert "candidates = freshNudges" in worker
print("V204 notification freshness and live-task gates passed.")

# V205: one-tap, unexported pause applies to optional nudges only.
pause_policy = (src / "reminders/V205NudgePausePolicy.kt").read_text()
pause_receiver = (src / "reminders/V205NudgePauseReceiver.kt").read_text()
settings_store = (src / "data/CreatorOsSettings.kt").read_text()
settings_ui = (src / "ui/V144SettingsHub.kt").read_text()
assert "PAUSE_MS = 7L" in pause_policy
assert "isPaused(nowMillis:" in pause_policy
assert "KEY_PAUSED_UNTIL" in pause_policy
assert "ACTION_PAUSE_WEEK" in pause_policy
assert "V205NudgePausePolicy.isPaused(" in worker
assert '"Pause 7 days"' in worker
assert "V205NudgePauseReceiver::class.java" in worker
assert "PendingIntent.FLAG_IMMUTABLE" in worker
assert "V205NudgePausePolicy.ACTION_PAUSE_WEEK" in pause_receiver
assert "KEY_PAUSED_UNTIL" in pause_receiver
assert "commit()" in pause_receiver
assert "NotificationManagerCompat.from(app).cancel" in pause_receiver
assert 'android:name=".reminders.V205NudgePauseReceiver" android:exported="false"' in manifest
assert "remove(V205NudgePausePolicy.KEY_PAUSED_UNTIL)" in settings_store
assert "Paused · resumes" in settings_ui
print("V205 private seven-day pause, visible status and explicit resume verified.")
