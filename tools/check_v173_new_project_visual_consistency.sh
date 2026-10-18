#!/usr/bin/env bash
set -euo pipefail

BASE="app/src/main/java/com/framebynavin/app/ui"
WIZARD="$BASE/V20NewProjectWizard.kt"
EDITOR="$BASE/V101BReminderUi.kt"
PICKER="$BASE/V156ReminderSetupUi.kt"
METRICS="$BASE/V173ProjectUiMetrics.kt"

for file in "$WIZARD" "$EDITOR" "$PICKER" "$METRICS"; do
  test -s "$file"
done

# Keep the full wizard, separate shortcut and project safety behavior.
grep -Fq 'showAdvancedSetup by rememberSaveable(startWithQuickCreate)' "$WIZARD"
grep -Fq 'V20ProgressiveSection(' "$WIZARD"
grep -Fq 'startWithQuickCreate = startWithQuickCreate' "$EDITOR"
grep -Fq 'V172ConfirmDiscardDialog(' "$EDITOR"
grep -Fq 'V171ProjectEditSafety.deadlineReady(' "$EDITOR"

# 48dp minimum touch surfaces, readable type, adaptable review and expanding actions.
grep -Fq 'val touchTarget: Dp = 48.dp' "$METRICS"
grep -Fq 'val supportingText: TextUnit = 12.sp' "$METRICS"
grep -Fq 'val reviewValueText: TextUnit = 13.sp' "$METRICS"
grep -Fq 'heightIn(min = V173ProjectUiMetrics.touchTarget)' "$WIZARD"
grep -Fq 'heightIn(min = V173ProjectUiMetrics.touchTarget)' "$EDITOR"
grep -Fq 'heightIn(min = V173ProjectUiMetrics.touchTarget)' "$PICKER"
grep -Fq 'heightIn(min = V173ProjectUiMetrics.reviewRowMinHeight)' "$WIZARD"
grep -Fq 'maxLines = 4, overflow = TextOverflow.Ellipsis' "$WIZARD"
grep -Fq 'fontSize = V173ProjectUiMetrics.actionText' "$EDITOR"
grep -Fq 'FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)' "$PICKER"
grep -Fq 'bringToStep.bringIntoView()' "$WIZARD"
test -f app/src/test/java/com/framebynavin/app/ui/V173ProjectUiMetricsTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V173ProjectVisualUiTest.kt

# No hard-coded tiny font sizes left in the primary New Project wizard.
if grep -En 'fontSize = ([0-9]|10|11)(\.[0-9]+)?\.sp' "$WIZARD"; then
  echo "New Project wizard contains undersized literal typography" >&2
  exit 1
fi

echo "V173 New Project visual consistency guards passed"
