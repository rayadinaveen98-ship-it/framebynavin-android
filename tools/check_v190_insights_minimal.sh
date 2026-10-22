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
# V207-E: headline is evidence-gated and sampled format data is explicitly scoped.
grep -Fq 'V207RevenuePresentation.hasEstimatedTotal(data)' "$revenue"
grep -Fq 'Text("ESTIMATED REVENUE"' "$revenue"
grep -Fq 'Revenue / 1K views is calculated' "$revenue"
grep -Fq 'V207RevenuePresentation.sampleLabel(data)' "$revenue"
grep -Fq 'V207RevenuePresentation.formatGroups(data)' "$revenue"
! grep -Fq '% of revenue' "$revenue"
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
