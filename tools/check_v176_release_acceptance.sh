#!/usr/bin/env bash
set -euo pipefail

APP="app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
EDITOR="app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt"
WIZARD="app/src/main/java/com/framebynavin/app/ui/V20NewProjectWizard.kt"
VIEWMODEL="app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt"
BACKUP="app/src/main/java/com/framebynavin/app/ui/V175ProjectDetailsUi.kt"
for f in "$APP" "$EDITOR" "$WIZARD" "$VIEWMODEL" "$BACKUP"; do test -s "$f"; done

# Stack integrity: V171 safety, V172 editor state, V173 layout, V174 Quick, V175 details.
grep -Fq 'V171ProjectEditSafety.remindersNeedRefresh(current, requested, formatChanged)' "$VIEWMODEL"
grep -Fq 'snoozeCount = if (refreshReminders) 0 else current.snoozeCount' "$VIEWMODEL"
grep -Fq 'V171ProjectEditSafety.initialDeadline(task?.dueAtMillis, fallbackDue)' "$EDITOR"
grep -Fq 'rememberSaveableStateHolder()' "$APP"
grep -Fq 'SaveableStateProvider("project-composer-$composerSession")' "$APP"
grep -Fq 'V172ConfirmDiscardDialog(' "$EDITOR"
grep -Fq 'V173ProjectUiMetrics.touchTarget' "$WIZARD"
grep -Fq 'var showAdvancedSetup by rememberSaveable(startWithQuickCreate)' "$WIZARD"
grep -Fq 'V174QuickReminderSummary(' "$WIZARD"
grep -Fq 'V175ProjectNotesField(notes = notes, onNotesChange = onNotesChange)' "$WIZARD"
grep -Fq 'onVoicePersonaChange = { voice = it }' "$EDITOR"
grep -Fq 'V175ReminderMeaning(attentionPlan, deliveryPreference, permissionWarning)' "$WIZARD"

# Require real Android checks in release candidate CI (not just test compilation).
WORKFLOW=".github/workflows/backlot-v176-new-project-p6.yml"
test -s "$WORKFLOW"
grep -Fq 'connectedDebugAndroidTest' "$WORKFLOW"
grep -Fq 'reactivecircus/android-emulator-runner@v2' "$WORKFLOW"
grep -Fq 'V176ProjectPersistenceInstrumentedTest' "$WORKFLOW"
grep -Fq 'V176ProjectJourneyUiTest' "$WORKFLOW"
grep -Fq 'V172DraftDiscardUiTest' "$WORKFLOW"
grep -Fq 'V175ProjectNotesUiTest' "$WORKFLOW"
grep -Fq 'if: always()' "$WORKFLOW"

test -f app/src/androidTest/java/com/framebynavin/app/data/V176ProjectPersistenceInstrumentedTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V176ProjectJourneyUiTest.kt
test -f app/src/test/java/com/framebynavin/app/ui/V176NewProjectReleaseContractTest.kt
test -f docs/qa/V176_NEW_PROJECT_RELEASE_ACCEPTANCE.md

grep -Eq 'versionCode = (176|177)' app/build.gradle.kts
grep -Eq '2.2.6-v176-new-project-acceptance-preview|2.2.7-v177-reminder-ack-p1-preview' app/build.gradle.kts

echo "V176 code-preservation, emulator and manual sign-off gates are defined"
