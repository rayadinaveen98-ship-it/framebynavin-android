#!/usr/bin/env bash
set -euo pipefail
ROOT="app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
PROFILE="app/src/main/java/com/framebynavin/app/ui/V23ProfileAccountUi.kt"
EDITOR="app/src/main/java/com/framebynavin/app/ui/V168CreatorProfileEditor.kt"
POLICY="app/src/main/java/com/framebynavin/app/data/CreatorProfileEditV168Policy.kt"
STORE="app/src/main/java/com/framebynavin/app/data/CreatorOsSettings.kt"
PERSONALIZE="app/src/main/java/com/framebynavin/app/ui/V144SettingsHub.kt"
VOICE="app/src/main/java/com/framebynavin/app/ui/V140VoiceStudioPicker.kt"
for p in "$ROOT" "$PROFILE" "$EDITOR" "$POLICY" "$STORE" "$PERSONALIZE" "$VOICE"; do test -f "$p"; done
grep -Fq 'onSaveCreatorProfile = { revised ->' "$ROOT"
grep -Fq 'settingsStore.saveCreatorProfileEdits(revised)' "$ROOT"
! grep -Fq 'settingsStore.setOnboardingComplete(false)' "$ROOT"
grep -Fq 'onClick = { showCreatorEditor = true }' "$PROFILE"
grep -Fq 'V168CreatorProfileEditor(' "$PROFILE"
grep -Fq 'onCancel = { showCreatorEditor = false }' "$PROFILE"
grep -Fq 'fun saveCreatorProfileEdits(value: CreatorProfile): Boolean' "$STORE"
grep -Fq 'if (!prefs.edit().putProfile(profile).commit()) return false' "$STORE"
grep -Fq 'val original' "$POLICY" || grep -Fq 'fun apply(original: CreatorProfile' "$POLICY"
grep -Fq 'original.copy(' "$POLICY"
grep -Fq 'primaryGoal = draft.primaryGoal' "$POLICY"
grep -Fq 'MORE OPTIONS · CREATOR TYPES & STYLES' "$EDITOR"
# V197.4 keeps validation in V168 and the same test tag in the adaptive footer.
grep -Fq 'V197ProfileEditorFooter(' "$EDITOR"
grep -Fq 'enabled = error == null' "$EDITOR"
grep -Fq 'CreatorProfileEditV168Policy.apply(profile, draft)' "$EDITOR"
grep -Fq 'testTag("v168-save-profile")' app/src/main/java/com/framebynavin/app/ui/V197ProfileEditorFooter.kt
grep -Fq 'Existing projects keep their own voice choice.' "$PERSONALIZE"
grep -Fq 'defaultMinSize(minHeight = 40.dp)' "$VOICE"
test -f app/src/test/java/com/framebynavin/app/data/CreatorProfileEditV168PolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V168ProfileEditorUiTest.kt
echo 'V168 Settings S3 profile editor and personalization checks passed'
