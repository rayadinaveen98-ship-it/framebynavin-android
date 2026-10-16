#!/usr/bin/env bash
set -euo pipefail
MODEL=app/src/main/java/com/framebynavin/app/data/CreatorWeeklyPlanPreviewV164.kt
WEEK=app/src/main/java/com/framebynavin/app/ui/V08WeeklyScheduleUi.kt
APP=app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt
AUTO=app/src/main/java/com/framebynavin/app/ui/V17AutomationCenter.kt
VM=app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt
test -f "$MODEL"
grep -Fq 'object CreatorWeeklyPlanPreviewEngine' "$MODEL"
grep -Fq 'object CreatorWeeklyPresetPolicy' "$MODEL"
grep -Fq 'val existingKeys = tasks.map' "$MODEL"
grep -Fq 'it.key in existingKeys' "$MODEL"
grep -Fq 'val preview = CreatorWeeklyPlanPreviewEngine.build(slots, tasks)' "$WEEK"
grep -Fq 'var confirmGenerate' "$WEEK"
grep -Fq 'if (confirmGenerate)' "$WEEK"
grep -Fq 'if (confirmReset)' "$WEEK"
grep -Fq 'pendingDeleteSlot' "$WEEK"
grep -Fq 'MORE OPTIONS · REMINDERS & PRIORITY' "$WEEK"
grep -Fq 'if (enabled) confirmAutoPlan = true' "$APP"
grep -Fq 'val template = CreatorWeeklyPresetPolicy.previewFor' "$VM"
grep -Fq 'if (enabled) confirmAutoEnable = true' "$AUTO"
grep -Fq 'No completed background run recorded yet' "$AUTO"
grep -Fq 'No completion verified yet' "$AUTO"
test -f app/src/test/java/com/framebynavin/app/data/CreatorWeeklyPlanPreviewV164Test.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V164WeeklyPlanPreviewUiTest.kt
echo 'V164 weekly plan and automation safety checks passed'
