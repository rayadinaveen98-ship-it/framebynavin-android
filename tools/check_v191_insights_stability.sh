#!/usr/bin/env bash
set -euo pipefail
ui=app/src/main/java/com/framebynavin/app/ui
grep -Fq 'V191InsightsPeriodPolicy.isSelectedRangeReady(windowDays, data)' "$ui/V11YouTubeInsights.kt"
grep -Fq 'else V172InsightsBody(' "$ui/V11YouTubeInsights.kt"
grep -Fq 'Report pending' "$ui/V11YouTubeInsights.kt"
grep -Fq 'V191InsightMetricPolicy.evaluate(snapshot, kind)' "$ui/V20InsightsDrilldownUi.kt"
grep -Fq 'V191InsightMetricPolicy.evaluate(snapshot, V20InsightsDetailKind.AVG_VIEW)' "$ui/V20InsightsDrilldownUi.kt"
grep -Fq '.filter { it.hasPeriodData }' "$ui/V20InsightsDrilldownUi.kt"
grep -Fq 'visible.take(visibleLimit)' "$ui/V189VideoCatalogUi.kt"
grep -Fq 'v191-catalog-show-more' "$ui/V189VideoCatalogUi.kt"
grep -Fq 'reachStore.channelId() == snapshot.channel.channelId' "$ui/V20InsightsFoundationUi.kt"
grep -Fq 'class V191InsightsStabilityTest' app/src/test/java/com/framebynavin/app/ui/V191InsightsStabilityTest.kt
grep -Fq 'class V191ContentStabilityUiTest' app/src/androidTest/java/com/framebynavin/app/ui/V191ContentStabilityUiTest.kt
bash tools/check_v190_insights_minimal.sh
echo 'V191 Insights stability source guards passed.'
