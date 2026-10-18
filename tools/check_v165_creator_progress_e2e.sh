#!/usr/bin/env bash
set -euo pipefail
UI="app/src/main/java/com/framebynavin/app/ui/V22CreatorRewardsUi.kt"
ROOT="app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
GUIDE="app/src/main/java/com/framebynavin/app/data/CreatorProgressV165Policy.kt"
LEDGER="app/src/main/java/com/framebynavin/app/data/CreatorRewards.kt"
for path in "$UI" "$ROOT" "$GUIDE" "$LEDGER"; do test -f "$path"; done
grep -Fq 'fun nextStep(snapshot: CreatorRewardProgressSnapshot)' "$GUIDE"
grep -Fq 'Only actions recorded in Backlot count' "$GUIDE"
grep -Fq 'var showAchievements by rememberSaveable' "$UI"
grep -Fq 'var showTraits by rememberSaveable' "$UI"
grep -Fq 'var showHistory by rememberSaveable' "$UI"
grep -Fq 'onOpenIdeas: () -> Unit = {}' "$UI"
grep -Fq 'onOpenProjects: () -> Unit = {}' "$UI"
grep -Fq 'onOpenIdeas = { overlay = POverlay.NONE; tab = PTab.IDEAS }' "$ROOT"
grep -Fq 'onOpenProjects = { overlay = POverlay.NONE; tab = PTab.CREATE }' "$ROOT"
grep -Fq 'eventKey = "published:$projectId"' "$LEDGER"
grep -Fq 'if (current.any { it.eventKey == entry.eventKey })' "$LEDGER"
test -f app/src/test/java/com/framebynavin/app/data/CreatorProgressV165PolicyTest.kt
test -f app/src/androidTest/java/com/framebynavin/app/ui/V165CreatorProgressUiTest.kt
echo 'V165 Creator Progress source acceptance checks passed'
