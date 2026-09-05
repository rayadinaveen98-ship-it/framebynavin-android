from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel, text):
    (ROOT / rel).write_text(text, encoding="utf-8")


def replace(rel, old, new, count=None):
    text = read(rel)
    hits = text.count(old)
    if hits == 0:
        raise SystemExit(f"Missing expected text in {rel}: {old!r}")
    if count is not None and hits != count:
        raise SystemExit(f"Expected {count} occurrence(s) in {rel}, found {hits}: {old!r}")
    write(rel, text.replace(old, new))

# Version this isolated foundation build without touching locked v1.7.5 stable.
replace("app/build.gradle.kts", "versionCode = 40", "versionCode = 41", 1)
replace("app/build.gradle.kts", "versionName = \"1.7.5-brand-ident-rc4\"", "versionName = \"1.8.0-foundation-alpha1\"", 1)

# Quick Capture: remove founder-only example and Inbox-as-a-product wording.
replace(
    "app/src/main/java/com/framebynavin/app/ui/V14QuickCaptureUi.kt",
    'placeholder = { Text("Best Pawan Kalyan interval blocks") }',
    'placeholder = { Text("A scene, hook, topic or idea worth exploring") }',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/ui/V14QuickCaptureUi.kt",
    'Text("AUTO-SORT TO INBOX",',
    'Text("AUTO-SORT IN IDEA VAULT",',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/ui/V14QuickCaptureUi.kt",
    'Text("SAVE TO INBOX",',
    'Text("SAVE IDEA",',
    1,
)

# Weekly Plan: plain-language user copy; keep internal stage terminology in the domain model.
replace("app/src/main/java/com/framebynavin/app/ui/V08WeeklyScheduleUi.kt", 'Text("WEEKLY ENGINE",', 'Text("WEEKLY PLAN",', 1)
replace("app/src/main/java/com/framebynavin/app/ui/V08WeeklyScheduleUi.kt", 'Text("FrameByNavin Week",', 'Text("Your Week",', 1)
replace(
    "app/src/main/java/com/framebynavin/app/ui/V08WeeklyScheduleUi.kt",
    '"Enabled slots generate real Studio projects for the next 8 days. Each project is placed into the correct production stage and its reminder can follow stage checkpoints automatically."',
    '"Enabled slots create Studio projects for the next 8 days. Each project starts at the right step, and reminders can follow your project steps automatically."',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/ui/V08WeeklyScheduleUi.kt",
    'Text("RESET TO FRAMEBYNAVIN DEFAULT WEEK",',
    'Text("RESET TO DEFAULT WEEK",',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/ui/V08WeeklyScheduleUi.kt",
    '"Smart escalation targets the deadline of the current production stage and moves forward when you complete that stage."',
    '"Smart escalation follows the deadline of your current step and moves forward when you complete that step."',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/ui/V08WeeklyScheduleUi.kt",
    '"One notification targets the current stage deadline."',
    '"One notification targets the current step deadline."',
    1,
)

# Idea Vault: neutral creator language.
replace(
    "app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt",
    'placeholder = { Text("Search idea, movie, person, note…") }',
    'placeholder = { Text("Search ideas, topics or notes…") }',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt",
    'label = { Text("Movie / person / topic") }',
    'label = { Text("Topic · optional") }',
    1,
)

# Release Day: generic launch example.
replace(
    "app/src/main/java/com/framebynavin/app/ui/V09ReleaseDayUi.kt",
    'placeholder = { Text("Example: Spirit trailer") }',
    'placeholder = { Text("Example: New video, post or launch") }',
    1,
)

# Current-user surfaces: say step, not stage.
replace("app/src/main/java/com/framebynavin/app/ui/V133CreatorHome.kt", 'Text("CURRENT STAGE",', 'Text("CURRENT STEP",', 1)
replace(
    "app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt",
    'Text("Edit how long Smart waits before the next unanswered stage.",',
    'Text("Edit how long Smart waits before the next unanswered step.",',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/data/CreatorPriorityEngine.kt",
    '"Deadline passed — finish the current stage before lower-priority work."',
    '"Deadline passed — finish the current step before lower-priority work."',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/data/CreatorPriorityEngine.kt",
    '"Due today — completing the current stage protects the publish window."',
    '"Due today — completing the current step protects the publish window."',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/data/CreatorPriorityEngine.kt",
    '"Important project with the strongest current deadline and stage signal."',
    '"Important project with the strongest current deadline and progress signal."',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/data/CreatorPriorityEngine.kt",
    '"Best next step from deadline, priority, workflow stage and current progress."',
    '"Best next step from deadline, priority and current progress."',
    1,
)

# Backup: remove implementation jargon and avoid leaking raw exception messages.
replace(
    "app/src/main/java/com/framebynavin/app/ui/BackupActivity.kt",
    'message = if (result.isSuccess) "Backup saved successfully." else result.exceptionOrNull()?.message ?: "Backup failed."',
    'message = if (result.isSuccess) "Backup saved successfully." else "Could not save the backup. Try another location."',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/ui/BackupActivity.kt",
    'message = it.message ?: "That file is not a valid FrameByNavin backup."',
    'message = "That file could not be opened as a FrameByNavin backup."',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/ui/BackupActivity.kt",
    'message = it.message ?: "Could not create backup."',
    'message = "Could not create the backup. Try again."',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/ui/BackupActivity.kt",
    'Text("Before restore, FrameByNavin keeps a temporary local snapshot. If importing fails, your current data is put back automatically.",',
    'Text("Before restoring, FrameByNavin keeps a temporary safety backup. If anything fails, your current data is put back automatically.",',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/ui/BackupActivity.kt",
    'Text("This replaces the current local Creator OS data.",',
    'Text("This replaces the current FrameByNavin data stored on this device.",',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/ui/BackupActivity.kt",
    'message = result.exceptionOrNull()?.message ?: "Restore failed. Your previous data was put back."',
    'message = "Restore failed. Your previous data was put back."',
    1,
)

# YouTube: never expose arbitrary technical exceptions to the UI.
replace(
    "app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt",
    'raw.isNotBlank() -> raw\n        else -> "YouTube refresh failed. Check your internet and try connecting again."',
    'else -> "YouTube couldn\'t refresh right now. Check your connection and try again."',
    1,
)
replace("app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt", 'Text("CREATOR OS",', 'Text("YOUR CREATOR PROGRESS",', 1)

# Cloud: keep service-specific HTTP messages, but do not surface arbitrary local exception strings.
replace(
    "app/src/main/java/com/framebynavin/app/cloud/CloudSyncManager.kt",
    'CloudOperationResult.Success("Cloud data deleted. Local Creator OS data was kept.")',
    'CloudOperationResult.Success("Cloud data deleted. Your local FrameByNavin data was kept.")',
    1,
)
replace(
    "app/src/main/java/com/framebynavin/app/cloud/CloudSyncManager.kt",
    'else -> error.message?.takeIf { it.isNotBlank() } ?: fallback',
    'else -> fallback',
    1,
)

print("v1.8 foundation alpha1 cleanup applied")
