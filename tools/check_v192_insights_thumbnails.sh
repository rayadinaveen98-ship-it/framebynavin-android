#!/usr/bin/env bash
set -euo pipefail
root=app/src/main/java/com/framebynavin/app/ui
header=$(sed -n '/private fun YTChannelHeader(/,/private fun YTMetrics(/p' "$root/V11YouTubeInsights.kt")
grep -Fq 'listOf(7, 28, 90)' "$root/V11YouTubeInsights.kt"
if grep -Eq 'daily rows|finalizedWindowLabel|Report pending|val selectedReady' <<< "$header"; then
  echo 'Unexpected analytics explanation below the period selectors' >&2
  exit 1
fi
grep -Fq 'key(videoId)' "$root/V148YouTubeThumbnail.kt"
grep -Fq 'initialValue = V148YouTubeThumbnailCache.get(videoId)' "$root/V148YouTubeThumbnail.kt"
grep -Fq 'v148-thumbnail-$videoId' "$root/V148YouTubeThumbnail.kt"
grep -Fq 'key(video.videoId)' "$root/V189VideoCatalogUi.kt"
grep -Fq 'v192-row-${video.videoId}' "$root/V189VideoCatalogUi.kt"
test -f app/src/androidTest/java/com/framebynavin/app/ui/V192ThumbnailSortUiTest.kt
echo 'V192 minimal header and video-ID thumbnail identity verified.'
