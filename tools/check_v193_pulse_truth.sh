#!/usr/bin/env bash
set -euo pipefail
p=app/src/main/java/com/framebynavin/app/youtube
grep -Fq 'YouTubePulseComputation.build(loadSamples(), System.currentTimeMillis())' "$p/YouTube24HourPulse.kt"
grep -Fq 'rateChange == null -> YouTubePulseMomentum.INSUFFICIENT' "$p/YouTubePulseComputationV193.kt"
grep -Fq 'rawDelta < 0L -> YouTubePulseMomentum.CORRECTION' "$p/YouTubePulseComputationV193.kt"
grep -Fq 'val currentRate = views.toDouble() / windowMillis.toDouble()' "$p/YouTubePulseComputationV193.kt"
grep -Fq 'val previousRate = previousViews.toDouble() / previousMillis.toDouble()' "$p/YouTubePulseComputationV193.kt"
grep -Fq 'maxByOrNull { it.capturedAtMillis }' "$p/YouTubePulseComputationV193.kt"
grep -Fq 'MAX_FRESH_AGE_MS' "$p/YouTubePulseComputationV193.kt"
grep -Fq 'YouTubePulseMomentum.CORRECTION' "$p/YouTubeInsightEngine.kt"
grep -Fq 'YouTubePulseMomentum.INSUFFICIENT' "$p/YouTubeInsightEngine.kt"
grep -Fq 'previousWindowHours' app/src/main/java/com/framebynavin/app/ui/V20InsightsDrilldownUi.kt
test -f app/src/test/java/com/framebynavin/app/youtube/YouTubePulseComputationV193Test.kt
bash tools/check_v192_insights_thumbnails.sh
echo 'V193 normalized sample comparison and truthful state guards passed.'
