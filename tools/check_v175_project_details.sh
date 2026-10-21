#!/usr/bin/env bash
set -euo pipefail

ROOT="app/src/main/java/com/framebynavin/app/ui"
WIZARD="$ROOT/V20NewProjectWizard.kt"
EDITOR="$ROOT/V101BReminderUi.kt"
DETAILS="$ROOT/V175ProjectDetailsUi.kt"
for f in "$WIZARD" "$EDITOR" "$DETAILS"; do test -s "$f"; done

# No new wizard steps. Quick Create remains independent from the seven-step path.
grep -Fq 'showAdvancedSetup by rememberSaveable(startWithQuickCreate)' "$WIZARD"
grep -Fq 'V174QuickReminderSummary(' "$WIZARD"
grep -Fq 'index = 6,' "$WIZARD"
grep -Fq 'V175ProjectNotesField(notes = notes, onNotesChange = onNotesChange)' "$WIZARD"
grep -Fq 'V175ProjectNotesField(notes = notes, onNotesChange = { notes = it })' "$EDITOR"
! grep -Fq 'V20FieldLabel("NOTES · OPTIONAL")' "$WIZARD"
! grep -Fq 'PComposerLabel("NOTES · OPTIONAL")' "$EDITOR"
grep -Fq 'V20WizardReviewRow("NOTES", notes) { activeStage = 0 }' "$WIZARD"

# Per-project voice selector uses the same picker as existing-project edit.
grep -Fq 'onVoicePersonaChange: (VoicePersona) -> Unit' "$WIZARD"
grep -Fq 'onVoicePersonaChange = { voice = it }' "$EDITOR"
grep -Fq 'onSelected = onVoicePersonaChange' "$WIZARD"
test "$(grep -Fc 'V207ProjectVoiceSelector(' "$WIZARD")" -eq 2
# V207: preserve the same project-voice callback contract with a compact presentation.
grep -Fq 'V207ProjectVoiceSelector(' "$EDITOR"
grep -Fq 'onSelected = { voice = it }' "$EDITOR"

# Reminder guidance cannot suppress custom-time validation or permissions.
grep -Fq 'V175ReminderMeaning(attentionPlan, deliveryPreference, permissionWarning)' "$WIZARD"
grep -Fq 'V175ProjectDetailsCopy.permissionsWarning(' "$WIZARD"
grep -Fq 'V174QuickCreatePolicy.needsCustomReminderTime(attentionPlan)' "$WIZARD"
grep -Fq 'if (requiresAdvancedPermissions && !reminderSetupReady)' "$WIZARD"
grep -Fq 'if (attentionPlan == ProjectAttentionPlan.CUSTOM)' "$WIZARD"
grep -Fq 'var expanded by rememberSaveable' "$DETAILS"
test -f app/src/test/java/com/framebynavin/app/ui/V175ProjectDetailsCopyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V175ProjectNotesUiTest.kt
echo "V175 project-detail/reminder UX checks passed"
