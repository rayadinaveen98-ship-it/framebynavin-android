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
assert "versionCode = 231" in gradle
assert 'versionName = "2.8.1-v207-stability-rc"' in gradle
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

# V206: signed preview must never include retired pre-approved logo drawables.
import xml.etree.ElementTree as ET
res = root / "res"
retired = {
    "ic_framebynavin_launcher",
    "ic_framebynavin_splash",
    "backlot_icon_cinematic",
    "backlot_icon_minimal",
    "backlot_icon_light",
    "backlot_icon_horizontal",
    "backlot_splash_mark",
}
for name in retired:
    assert not (res / "drawable" / f"{name}.xml").exists(), f"Retired icon remains packaged: {name}"

# Check every source file, not only the eight original notification producers.
for path in root.rglob("*"):
    if path.is_file() and path.suffix in {".kt", ".xml"}:
        contents = path.read_text(encoding="utf-8")
        for name in retired:
            assert not re.search(r"(?:R\.drawable\.|@drawable/|R\.mipmap\.|@mipmap/)" + re.escape(name) + r"\b", contents), f"{path}: references retired {name}"

for path in src.rglob("*.kt"):
    contents = path.read_text(encoding="utf-8")
    if "NotificationCompat.Builder(" in contents or "Notification.Builder(" in contents:
        found = re.findall(r"(?:\.)?setSmallIcon\(\s*([^)]+)\)", contents)
        assert found, f"{path}: notification builder without branded small icon"
        assert all(x.strip() == icon for x in found), f"{path}: noncanonical notification icons {found}"
    assert "R.drawable.ic_framebynavin_launcher" not in contents, f"{path}: legacy launcher icon"
    assert "R.drawable.ic_framebynavin_splash" not in contents, f"{path}: legacy splash icon"

ns = "{http://schemas.android.com/apk/res/android}"
manifest_root = ET.parse(root / "AndroidManifest.xml").getroot()
application = manifest_root.find("application")
assert application is not None
assert application.attrib[ns + "icon"] == "@mipmap/backlot_v198_launcher"
assert application.attrib[ns + "roundIcon"] == "@mipmap/backlot_v198_launcher"
assert application.attrib[ns + "label"] == "Backlot"
aliases = application.findall("activity-alias")
assert len(aliases) == 4
assert all(a.attrib[ns + "icon"] == "@mipmap/backlot_v198_launcher" for a in aliases)
assert all(a.attrib[ns + "label"] == "Backlot" for a in aliases)

adaptive = (res / "mipmap-anydpi-v26/backlot_v198_launcher.xml").read_text(encoding="utf-8")
assert '@drawable/backlot_v198_approved_icon' in adaptive
assert '@drawable/backlot_v198_notification_monochrome' in adaptive

small_icon = ET.parse(res / "drawable/backlot_v198_notification_monochrome.xml").getroot()
paths = small_icon.findall("path")
assert paths and all(p.attrib.get(ns + "fillColor", "").lower() in {"#ffffffff", "#ffffff"} for p in paths)
assert not small_icon.findall(".//gradient"), "Notification small icon must be monochrome"
picker = (src / "ui/V133BacklotAppIcon.kt").read_text(encoding="utf-8")
assert 'painterResource(R.drawable.backlot_v198_approved_icon)' in picker
assert "Reduced gold mark" not in picker
print("V206 complete brand-asset and status-bar icon source audit passed.")

# V207-A: Settings must paint opaque at the Activity edge. Applying
# windowInsetsPadding to Surface itself causes Home content to bleed through.
settings = (src / "ui/V144SettingsHub.kt").read_text(encoding="utf-8")
assert 'modifier = Modifier.fillMaxSize().testTag("v207-settings-fullscreen")' in settings
assert 'Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding())' in settings
assert '.windowInsetsPadding(WindowInsets.safeDrawing)\n            .imePadding(),\n        color = CinemaBlack,' not in settings
assert 'V144SettingsPage(' in settings and 'V144SettingsHome(' in settings

# V207-B: show only one voice row inside a project. Keep the original eight
# style labels and real TextToSpeech selection & preview behavior unchanged.
voice = (src / "ui/V207ProjectVoiceSelector.kt").read_text(encoding="utf-8")
wizard = (src / "ui/V20NewProjectWizard.kt").read_text(encoding="utf-8")
composer = (src / "ui/V101BReminderUi.kt").read_text(encoding="utf-8")
engine = (src / "reminders/VoicePersonaEngine.kt").read_text(encoding="utf-8")
assert wizard.count("V207ProjectVoiceSelector(") == 2
assert composer.count("V207ProjectVoiceSelector(") == 1
assert "V140VoiceStudioPicker(" not in wizard and "V140VoiceStudioPicker(" not in composer
assert "V140VoiceStudioPicker(" in settings  # Dedicated Settings still has full controls.
assert 'testTag("v207-voice-collapsed")' in voice
assert 'testTag("v207-voice-dialog")' in voice
assert "VoicePersona.entries.forEach" in voice
assert 'onSelected(persona)' in voice and 'onPreview(persona)' in voice
assert 'onPreview(selected)' in voice
assert "VoicePersonaEngine.label(selected)" in voice
assert "tts.setPitch(pitch)" in engine and "tts.setSpeechRate(rate)" in engine
print("V207-A full-bleed Settings and V207-B compact project voice UX guards passed.")

# V207-C: three creator setup screens and backwards-compatible valid profiles.
setup = (src / "ui/V18CreatorOnboarding.kt").read_text(encoding="utf-8")
first_run = (src / "ui/V207FirstRunPolicy.kt").read_text(encoding="utf-8")
actions = (src / "ui/V197OnboardingActions.kt").read_text(encoding="utf-8")
assert "const val PAGE_COUNT = 3" in first_run
assert 'const val DEFAULT_GOAL = "Stay organized"' in first_run
assert "profile.copy(" in first_run and ".normalized()" in first_run
assert "require(completed.isComplete)" in first_run
assert 'V207FirstRunPolicy.PAGE_COUNT' in setup
assert "V207FirstRunPolicy.completeProfile(profile, primaryMode, platforms)" in setup
assert "lastPage = V207FirstRunPolicy.LAST_PAGE" in setup
assert "V197OnboardingActions(" in setup and "START BACKLOT" in actions
assert "V18OnboardingPermissionRow(" not in setup
assert "V127SetupGuideStrip(page)" not in setup
assert "CreatorPlatformRegistry.supportedPlatforms.forEach" in setup
assert "CreatorModeRegistry.labels.forEach" in setup
account = (src / "ui/V23AccountOnboarding.kt").read_text(encoding="utf-8")
recovery = (src / "ui/V20DriveRecoveryGate.kt").read_text(encoding="utf-8")
assert "CONTINUE WITHOUT ACCOUNT" in account
assert "V20DriveRecoveryGate(" in (src / "ui/FrameByNavinV101BApp.kt").read_text(encoding="utf-8")
assert "sync.restoreCloud()" in recovery and "sync.keepThisPhone()" in recovery
print("V207-C three-step onboarding and returning-account preservation passed.")

# V207-D: anchors are measured from real laid-out navigation, never guessed
# percentages that break on different screen sizes or font scales.
spotlight = (src / "ui/V137GuideSpotlight.kt").read_text(encoding="utf-8")
coach = (src / "ui/V20GuidedFirstRun.kt").read_text(encoding="utf-8")
tour_policy = (src / "ui/V207GuidedTourPolicy.kt").read_text(encoding="utf-8")
app = (src / "ui/FrameByNavinV101BApp.kt").read_text(encoding="utf-8")
assert "val lessons = listOf(" in tour_policy
for entry in ("CreatorGuidedTourStep.TODAY", "CreatorGuidedTourStep.IDEAS", "CreatorGuidedTourStep.PROJECT", "CreatorGuidedTourStep.INSIGHTS"):
    assert entry in tour_policy
assert "CreatorGuidedTourStep.WORKSPACE -> 2" in tour_policy
assert "CreatorGuidedTourStep.CONTROL -> 3" in tour_policy
assert "targetBounds: Rect? = null" in spotlight
assert "v137Spot(" not in spotlight and "val x: Float" not in spotlight
assert "if (bounds != null)" in spotlight
assert "Canvas(" not in coach and "spotlightFor(" not in coach
assert "repeat(V207GuidedTourPolicy.lessons.size)" in coach
assert '"EXIT GUIDE"' in coach
assert "fontSize = 13.sp, lineHeight = 18.sp" in coach
assert "guideTargets = remember { mutableStateMapOf<CreatorGuidedTourStep, Rect>() }" in app
assert "onGloballyPositioned { onPositioned(it.boundsInRoot()) }" in app
assert "targetBounds = guideTargets[guidedTourStep]" in app
assert "V207GuidedTourPolicy.next(guidedTourStep)" in app
assert "V207GuidedTourPolicy::previous" in app
back_handler = app[app.index("    BackHandler("):app.index("    publicationTaskId?.let")]
assert "settingsStore.markGuidedTourComplete()" not in back_handler
primary_tour = app[app.index("            onPrimary = {", app.index("V20GuidedFirstRunCoach(")):app.index("            onSecondary = {", app.index("V20GuidedFirstRunCoach("))]
assert "openComposer()" not in primary_tour and "showQuickCapture = true" not in primary_tour
assert "fun isLast(" in tour_policy
print("V207-D live-geometry, 4-lesson non-destructive guided tour source gates passed.")

# V207-E: the channel-wide estimated total MUST come only from an evidenced
# summary response. Video format groups are at-most-50 sampled content rows.
revenue_client = (root / "java/com/framebynavin/app/youtube/YouTubeRevenue.kt").read_text(encoding="utf-8")
revenue_rules = (root / "java/com/framebynavin/app/youtube/V207RevenuePresentation.kt").read_text(encoding="utf-8")
revenue_ui = (src / "ui/V144YouTubeRevenueUi.kt").read_text(encoding="utf-8")
revenue_bridge = (src / "ui/V144YouTubeRevenueIntegration.kt").read_text(encoding="utf-8")
assert '"maxResults" to "50"' in revenue_client
assert '"estimatedRevenue" to summary' not in revenue_client
assert "evidence?.hasSummary(\"estimatedRevenue\")" in revenue_rules
assert "snapshot.estimatedRevenue.isFinite()" in revenue_rules
assert "snapshot.evidence?.contentState != YouTubeDatasetState.READY" in revenue_rules
assert "snapshot.content.groupBy" in revenue_rules
assert "Top " in revenue_rules and "sample, not a full channel format split" in revenue_rules
assert '"Not an AdSense payout"' in revenue_ui or 'Not an AdSense payout' in revenue_ui
assert "V207RevenuePresentation.hasEstimatedTotal(data)" in revenue_ui
assert "V207RevenuePresentation.sampleLabel(data)" in revenue_ui
assert "V207RevenuePresentation.formatGroups(data)" in revenue_ui
assert "V207RevenuePresentation.refreshLabel(data)" in revenue_ui
assert "More ad metrics" in revenue_ui
assert "Top 3 Share" not in revenue_ui
assert "% of revenue" not in revenue_ui
assert "Revenue by Format" not in revenue_ui
assert "Revenue / 1K views is calculated" in revenue_ui
assert "Revenue by Format" not in revenue_rules
# No changes to independent read-only Google authorization or channel-owned cache.
assert "YouTubeAuthorization.revenueRequest()" in revenue_bridge
assert "revenueStore.saveIfOwner(" in revenue_bridge
assert "revenueStore.load(selectedPeriod, channelId)" in revenue_bridge
print("V207-E Revenue format sample disclosure, verified channel total and consent isolation passed.")

# V207-F is an integration gate rather than a new feature.
# A-E must coexist on one commit, with *all* earlier stability checks.
app = (src / "ui/FrameByNavinV101BApp.kt").read_text(encoding="utf-8")
settings = (src / "ui/V144SettingsHub.kt").read_text(encoding="utf-8")
project_editor = (src / "ui/V101BReminderUi.kt").read_text(encoding="utf-8")
project_wizard = (src / "ui/V20NewProjectWizard.kt").read_text(encoding="utf-8")
setup = (src / "ui/V18CreatorOnboarding.kt").read_text(encoding="utf-8")
coach = (src / "ui/V20GuidedFirstRun.kt").read_text(encoding="utf-8")
revenue = (src / "ui/V144YouTubeRevenueUi.kt").read_text(encoding="utf-8")
assert 'testTag("v207-settings-fullscreen")' in settings
assert 'Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding())' in settings
assert project_wizard.count("V207ProjectVoiceSelector(") == 2
assert project_editor.count("V207ProjectVoiceSelector(") == 1
assert "V140VoiceStudioPicker(" not in project_wizard
assert "V140VoiceStudioPicker(" not in project_editor
assert "V207FirstRunPolicy.completeProfile(" in setup
assert "V207GuidedTourPolicy.next(guidedTourStep)" in app
assert "V207GuidedTourPolicy::previous" in app
assert "onGloballyPositioned { onPositioned(it.boundsInRoot()) }" in app
assert '"EXIT GUIDE"' in coach
assert "V207RevenuePresentation.hasEstimatedTotal(data)" in revenue
assert "V207RevenuePresentation.sampleLabel(data)" in revenue
# Existing app identity is intentionally unchanged so Android updates preserve data.
manifest = (root / "AndroidManifest.xml").read_text(encoding="utf-8")
gradle = (Path("app/build.gradle.kts")).read_text(encoding="utf-8")
assert 'applicationId = "com.framebynavin.app"' in gradle
assert 'android:name=".MainActivity"' in manifest or 'android:name="com.framebynavin.app.MainActivity"' in manifest
for p in (
    "data/CreatorProfile.kt",
    "data/CreatorGuidedTour.kt",
    "youtube/YouTubeRevenue.kt",
    "reminders/ReminderNotifications.kt",
    "reminders/VoiceReminderService.kt",
    "reminders/AlarmRingingService.kt",
):
    assert (src / p).is_file(), p
for p in (
    "ui/V207FullscreenVoiceUiTest.kt",
    "ui/V207FirstRunUiTest.kt",
    "ui/V207GuidedTourUiTest.kt",
    "ui/V207RevenueUiTest.kt",
):
    assert (Path("app/src/androidTest/java/com/framebynavin/app") / p).is_file(), p
print("V207-F integrated A-E contract / data identity / emulator acceptance inventory passed.")
