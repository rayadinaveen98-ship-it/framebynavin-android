#!/usr/bin/env bash
set -euo pipefail
yt=app/src/main/java/com/framebynavin/app/youtube
ui=app/src/main/java/com/framebynavin/app/ui
grep -Fq 'val channelId: String = ""' "$yt/YouTube24HourPulse.kt"
grep -Fq 'channelId = latest.channelId' "$yt/YouTubePulseComputationV193.kt"
grep -Fq 'report.channelId != selectedChannelId' "$yt/YouTubePulseVisibilityPolicyV194.kt"
grep -Fq 'nowMillis - report.currentCapturedAtMillis > MAX_AGE_MS' "$yt/YouTubePulseVisibilityPolicyV194.kt"
grep -Fq 'YouTubePulseVisibilityPolicy.visible(' "$yt/YouTubeInsightEngine.kt"
grep -Fq 'YouTubePulseVisibilityPolicy.visible(' "$ui/V20InsightsDrilldownUi.kt"
test -f app/src/test/java/com/framebynavin/app/youtube/YouTubePulseVisibilityV194Test.kt
bash tools/check_v193_pulse_truth.sh
echo 'V194 owner/freshness acceptance contracts passed.'
