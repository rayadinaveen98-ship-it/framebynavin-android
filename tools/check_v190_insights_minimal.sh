#!/usr/bin/env bash
set -euo pipefail
root=app/src/main/java/com/framebynavin/app/ui
overview="$root/V172InsightsUi.kt"
revenue="$root/V144YouTubeRevenueUi.kt"
entry="$root/V11YouTubeInsights.kt"
video="$root/V189VideoDetailScreen.kt"
catalog="$root/V189VideoCatalogUi.kt"
test -f "$root/V190InsightsUiPrimitives.kt"
grep -Fq 'V190Disclosure("Audience & opportunities"' "$overview"
grep -Fq 'V190Disclosure("Workflow & patterns"' "$overview"
grep -Fq 'V189VideoCatalog(' "$overview"
grep -Fq 'V189VideoDetailScreen(' "$overview"
grep -Fq 'V172TrendCard(snapshot)' "$overview"
grep -Fq 'V144RevenueMetric("Estimated Revenue"' "$revenue"
grep -Fq 'V144RevenueMetric("Rev / 1K Views"' "$revenue"
# V197.4: video detail retains the higher modal window and consumes safe drawing once.
grep -Fq 'Dialog(onDismissRequest = onDismiss,' "$video"
grep -Fq '.windowInsetsPadding(WindowInsets.safeDrawing)' "$video"
grep -Fq 'v189-catalog-search' "$catalog"
! grep -Fq 'personalization.insightBody' "$entry"
! grep -Fq 'personalization.insightTitle' "$entry"
! grep -Fq 'YTProfileFocusCard(creatorProfile, personalization)' "$entry"
! grep -Fq 'Text(signal.body' "$overview"
! grep -Fq 'Text("SHOPPING"' "$revenue"
! grep -Fq 'Text("WHERE EARNINGS COME FROM"' "$revenue"
! grep -Fq 'Text("YOUR CREATOR PROGRESS"' "$overview"
! grep -Fq 'V172VideoDetailDialog(' "$overview"
echo 'V190 minimal copy/navigation source guards passed.'
