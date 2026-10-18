#!/usr/bin/env bash
set -euo pipefail
APP="app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
EDITOR="app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt"
WIZARD="app/src/main/java/com/framebynavin/app/ui/V20NewProjectWizard.kt"
POLICY="app/src/main/java/com/framebynavin/app/ui/V172ProjectDraftPolicy.kt"

for file in "$APP" "$EDITOR" "$WIZARD" "$POLICY"; do test -s "$file"; done
grep -Fq 'rememberSaveableStateHolder()' "$APP"
grep -Fq 'SaveableStateProvider("project-composer-$composerSession")' "$APP"
grep -Fq 'returnToComposerFromSettings = true' "$APP"
grep -Fq 'onClose = ::leaveOverlay' "$APP"
grep -Fq 'dismissRequestToken = composerDismissRequest' "$APP"
grep -Fq 'composerStateHolder.removeState(key)' "$APP"
grep -Fq 'var savedStyleList by rememberSaveable(task?.id)' "$EDITOR"
grep -Fq 'V172ProjectDraftPolicy.changed(' "$EDITOR"
grep -Fq 'V172ConfirmDiscardDialog(' "$EDITOR"
grep -Fq 'onDismiss = { requestExit() }' "$EDITOR"
grep -Fq 'Dialog(onDismissRequest = { requestExit() }' "$EDITOR"
! grep -Fq 'animateScrollTo(scrollState.maxValue)' "$WIZARD"
grep -Fq 'bringToStep.bringIntoView()' "$WIZARD"
grep -Fq 'project-step-$index' "$WIZARD"
grep -Fq 'valid = dueAt > now' "$WIZARD"
grep -Fq 'valid = allReady' "$WIZARD"
grep -Fq 'startWithQuickCreate = startWithQuickCreate' "$EDITOR"
grep -Fq 'showAdvancedSetup by rememberSaveable(startWithQuickCreate)' "$WIZARD"
test -f app/src/test/java/com/framebynavin/app/ui/V172ProjectDraftPolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V172DraftDiscardUiTest.kt
echo "V172 project draft and wizard navigation safeguards present"
