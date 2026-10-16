#!/usr/bin/env bash
set -euo pipefail
HUB="app/src/main/java/com/framebynavin/app/ui/V144SettingsHub.kt"
PROFILE="app/src/main/java/com/framebynavin/app/ui/V23ProfileAccountUi.kt"
NAV="app/src/main/java/com/framebynavin/app/ui/V167SettingsNavigationPolicy.kt"
for p in "$HUB" "$PROFILE" "$NAV"; do test -f "$p"; done
grep -Fq 'V167SettingsSection.PERSONALIZATION -> V144SettingsPage' "$HUB"
grep -Fq 'V167SettingsNavigationPolicy.parent(section)' "$HUB"
grep -Fq 'onOpen(V167SettingsSection.PERSONALIZATION)' "$HUB"
grep -Fq 'V167SettingsSection.APPEARANCE.name' "$HUB"
grep -Fq 'V167SettingsSection.GUIDE.name' "$HUB"
grep -Fq 'V167SettingsSection.VOICE.name' "$HUB"
grep -Fq '"My Profile"' "$HUB"
grep -Fq '"Personalization"' "$HUB"
grep -Fq '"Reminders & Alerts"' "$HUB"
grep -Fq '"Planning & Calendar"' "$HUB"
grep -Fq '"Backup & Data"' "$HUB"
grep -Fq '"About & Help"' "$HUB"
! grep -Fq 'V167SettingsSection.CONNECTIONS' "$HUB"
grep -Fq 'Settings → Backup & Data.' "$PROFILE"
! grep -Fq 'V23ActionRow("Google Drive Vault"' "$PROFILE"
grep -Fq 'V144CategoryCard("Cloud Sync"' "$HUB" || grep -Fq 'title = "Cloud Sync"' "$HUB"
grep -Fq 'title = "Local Data & Backup"' "$HUB"
grep -Fq 'V129GoogleCalendarSettings(tasks)' "$HUB"
grep -Fq 'modifier = Modifier.testTag("s1-settings-auto-plan")' "$HUB"
grep -Fq 'if (confirmAutoPlan)' "$HUB"
test -f app/src/test/java/com/framebynavin/app/ui/V167SettingsNavigationPolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V167SettingsNavigationUiTest.kt
echo 'V167 Settings S2 navigation and feature preservation checks passed'
