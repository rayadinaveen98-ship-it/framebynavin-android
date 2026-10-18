#!/usr/bin/env bash
set -euo pipefail
HUB="app/src/main/java/com/framebynavin/app/ui/V144SettingsHub.kt"
POLICY="app/src/main/java/com/framebynavin/app/ui/V169SettingsClarityPolicy.kt"
ROOT="app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
for p in "$HUB" "$POLICY" "$ROOT"; do test -f "$p"; done
grep -Fq 'Standard notifications allowed' "$POLICY"
grep -Fq 'Notifications are off' "$POLICY"
grep -Fq 'snoozeChoices(current: Int)' "$POLICY"
grep -Fq 'alarmTimeoutChoices(current: Int)' "$POLICY"
grep -Fq 'quietHoursDescription(settings: CreatorOsSettings)' "$POLICY"
grep -Fq 'MORE ANDROID OPTIONS' "$HUB"
grep -Fq 'if (showExtraPermissions)' "$HUB"
grep -Fq 'V169SettingsClarityPolicy.snoozeChoices(settings.snoozeMinutes)' "$HUB"
grep -Fq 'V169SettingsClarityPolicy.alarmTimeoutChoices(settings.defaultAlarmTimeoutSeconds)' "$HUB"
grep -Fq 'V169SettingsClarityPolicy.quietHoursDescription(settings)' "$HUB"
grep -Fq 'modifier = Modifier.testTag("s1-settings-auto-plan")' "$HUB"
grep -Fq 'if (enable) confirmAutoPlan = true else onWeeklyAutoPlan(false)' "$HUB"
grep -Fq 'V129GoogleCalendarSettings(tasks)' "$HUB"
grep -Fq 'onOpenWeeklyPlan: () -> Unit = {}' "$HUB"
grep -Fq 'onOpenAutomation: () -> Unit = {}' "$HUB"
grep -Fq 'onOpenWeeklyPlan = { overlay = POverlay.WEEK }' "$ROOT"
grep -Fq 'onOpenAutomation = { overlay = POverlay.AUTOMATION }' "$ROOT"
grep -Fq 'onClick = onOpenWeeklyPlan' "$HUB"
grep -Fq 'onClick = onOpenAutomation' "$HUB"
test -f app/src/test/java/com/framebynavin/app/ui/V169SettingsClarityPolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V169SettingsRemindersPlanningUiTest.kt
echo 'V169 Settings S4 reminder + planning source safeguards passed'
