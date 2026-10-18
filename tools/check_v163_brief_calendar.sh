#!/usr/bin/env bash
set -euo pipefail
BASE=app/src/main/java/com/framebynavin/app
UI="$BASE/ui/V15ContextUi.kt"
ROOT="$BASE/ui/FrameByNavinV101BApp.kt"
ENGINE="$BASE/data/CreatorContentCalendar.kt"
POLICY="$BASE/data/CreatorCalendarNavigationPolicy.kt"
test -f "$UI"
test -f "$POLICY"
grep -Fq 'fun recentOverdue(' "$POLICY"
grep -Fq 'fun forDay(' "$POLICY"
grep -Fq 'Weekly plan · not a project yet' "$POLICY"
grep -Fq 'it.dueAtMillis >= nowMillis || it.status == TaskStatus.DONE' "$ENGINE"
grep -Fq 'var selectedDayOffset by rememberSaveable' "$UI"
grep -Fq 'V163OverdueProjectRow(' "$UI"
grep -Fq 'onOpenProject(nudge.taskId)' "$UI"
grep -Fq 'onOpenCalendar' "$UI"
grep -Fq 'CreatorCalendarNavigationPolicy.kindLabel(item)' "$UI"
grep -Fq 'onOpenProject = ::openProject' "$ROOT"
grep -Fq 'onOpenWeeklyPlan = { overlay = POverlay.WEEK }' "$ROOT"
grep -Fq 'onOpenCalendar = { overlay = POverlay.CALENDAR }' "$ROOT"
test -f app/src/test/java/com/framebynavin/app/data/CreatorCalendarNavigationPolicyV163Test.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V163BriefCalendarUiTest.kt
echo "V163 actionable brief and source-safe calendar preservation passed"
