#!/usr/bin/env bash
set -euo pipefail
APP="app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
EDITOR="app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt"
WIZARD="app/src/main/java/com/framebynavin/app/ui/V20NewProjectWizard.kt"
POLICY="app/src/main/java/com/framebynavin/app/ui/V174QuickCreatePolicy.kt"
for f in "$APP" "$EDITOR" "$WIZARD" "$POLICY"; do test -s "$f"; done
# Quick Create defaults, a compact reminder disclosure, a persistent action and advanced fallback.
grep -Fq 'V174QuickCreatePolicy.defaultDueAt(now)' "$EDITOR"
grep -Fq 'if (task == null && startWithQuickCreate)' "$EDITOR"
grep -Fq 'var showQuickReminderChoices by rememberSaveable' "$WIZARD"
grep -Fq 'V174QuickReminderSummary(' "$WIZARD"
grep -Fq 'AnimatedVisibility(visible = showQuickReminderChoices)' "$WIZARD"
grep -Fq 'V156ReminderPlanPicker(' "$WIZARD"
grep -Fq 'V174QuickCreatePolicy.needsCustomReminderTime(attentionPlan)' "$WIZARD"
grep -Fq 'if (!quickReady)' "$WIZARD"
grep -Fq 'enabled = quickReady' "$WIZARD"
grep -Fq 'quick-create-footer' "$WIZARD"
grep -Fq 'quick-create-submit' "$WIZARD"
grep -Fq 'quick-create-more-options' "$WIZARD"
grep -Fq 'startWithQuickCreate = startWithQuickCreate' "$EDITOR"
grep -Fq 'onQuickCreate = { openComposer(quick = true) }' "$APP"
grep -Fq 'showAdvancedSetup by rememberSaveable(startWithQuickCreate)' "$WIZARD"
grep -Fq 'V20ProgressiveSection(' "$WIZARD"
grep -Fq 'V171ProjectEditSafety.deadlineReady(' "$EDITOR"
grep -Fq 'V172ConfirmDiscardDialog(' "$EDITOR"
grep -Fq 'V173ProjectUiMetrics.touchTarget' "$WIZARD"
test -f app/src/test/java/com/framebynavin/app/ui/V174QuickCreatePolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V174QuickCreateReminderUiTest.kt
echo "V174 Quick Create source and legacy parity guards passed"
