#!/usr/bin/env bash
set -euo pipefail
CLOUD="app/src/main/java/com/framebynavin/app/cloud/CloudSyncActivity.kt"
STATE="app/src/main/java/com/framebynavin/app/cloud/CreatorCloudBackupStatusV170.kt"
LOCAL="app/src/main/java/com/framebynavin/app/ui/BackupActivity.kt"
RECOVERY="app/src/main/java/com/framebynavin/app/ui/RecoveryCopiesActivity.kt"
PORTABLE="app/src/main/java/com/framebynavin/app/data/PortableCreatorBackupManager.kt"
BACKUP_CORE="app/src/main/java/com/framebynavin/app/data/CreatorBackupManager.kt"
for f in "$CLOUD" "$STATE" "$LOCAL" "$RECOVERY" "$PORTABLE" "$BACKUP_CORE"; do test -f "$f"; done
grep -Fq 'val backupView = CreatorCloudBackupStatusV170.resolve' "$CLOUD"
grep -Fq 'Last confirmed ·' "$CLOUD"
grep -Fq 'Latest backup not confirmed' "$STATE"
grep -Fq 'CreatorBackupBadgeStatus.CONFLICT' "$STATE"
grep -Fq 'No new backup has been confirmed.' "$STATE"
grep -Fq 'cloudChecking = true' "$CLOUD"
grep -Fq 'driveListAvailable && drivePoints.isEmpty()' "$CLOUD"
grep -Fq 'Drive copy saved, but the list could not be refreshed' "$CLOUD"
grep -Fq 'var recoveryCount by remember' "$LOCAL"
grep -Fq 'Lifecycle.Event.ON_RESUME' "$LOCAL"
grep -Fq 'not uploaded or encrypted' "$RECOVERY"
grep -Fq 'This file did not pass Backlot backup validation' "$LOCAL"
grep -Fq 'PortableBackupArchive.stage' "$PORTABLE"
grep -Fq 'creatorBackup.validate(original)' "$PORTABLE"
grep -Fq 'fun recoveryCopies()' "$BACKUP_CORE"
test -f app/src/test/java/com/framebynavin/app/cloud/CreatorCloudBackupStatusV170Test.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V170BackupSettingsUiTest.kt
echo 'V170 backup, status and recovery preservation checks passed'
