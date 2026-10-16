#!/usr/bin/env bash
set -euo pipefail
SETTINGS="app/src/main/java/com/framebynavin/app/ui/V144SettingsHub.kt"
CAL="app/src/main/java/com/framebynavin/app/ui/V129GoogleCalendarSettings.kt"
POLICY="app/src/main/java/com/framebynavin/app/ui/CreatorCalendarSyncS1Policy.kt"
BACKUP="app/src/main/java/com/framebynavin/app/ui/BackupActivity.kt"
ROOT="app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
for file in "$SETTINGS" "$CAL" "$POLICY" "$BACKUP" "$ROOT"; do test -f "$file"; done
grep -Fq 'weeklySlots = vm.weeklySlots' "$ROOT"
grep -Fq 'if (enable) confirmAutoPlan = true else onWeeklyAutoPlan(false)' "$SETTINGS"
grep -Fq 'if (confirmAutoPlan)' "$SETTINGS"
grep -Fq 'onWeeklyAutoPlan(true)' "$SETTINGS"
grep -Fq 'CreatorWeeklyPlanPreviewEngine.build(weeklySlots, tasks)' "$SETTINGS"
grep -Fq 'CreatorCalendarSyncS1Policy.description(task)' "$CAL"
grep -Fq 'CreatorCalendarSyncS1Policy.exportable(tasks)' "$CAL"
grep -Fq 'private const val KEY_S1_CONSENT' "$CAL"
grep -Fq 'fun consentToExport(context: Context)' "$CAL"
grep -Fq 'CreatorCalendarSync.consentToExport(context)' "$CAL"
grep -Fq 'ManagedEvent.NOT_BACKLOT -> blocked++' "$CAL"
grep -Fq 'if (existingCalendar == calendarId) ManagedEvent.OWNED' "$CAL"
grep -Fq 'Calendar permission is needed to sync.' "$CAL"
grep -Fq 'snapshotFlow { vm.tasks.toList() }' "$ROOT"
grep -Fq 'Backing up' "$BACKUP" || grep -Fq 'not encrypted' "$BACKUP"
grep -Fq 'phone Calendar connection' "$SETTINGS"
grep -Fq 'Theme, app icon, launch sound and Google Calendar connection' "$BACKUP"
test -f app/src/test/java/com/framebynavin/app/ui/CreatorCalendarSyncS1PolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V166SettingsSafetyUiTest.kt
echo 'S1 settings safety guards passed'
