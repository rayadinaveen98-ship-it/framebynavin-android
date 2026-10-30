# Backlot V192 — Insights header and thumbnail sort hotfix

Base: fully automated-test-passing V191 `70ace856d821f13d2ce53b05aa2de2d4709e1d84` (40 Android 11 emulator tests).

## User-reported fixes
- Remove the two verbose analytics-period/status labels displayed underneath the 7D / 28D / 90D filter chips. Keep the selected range, channel controls, loading indicator, and the V191 wrong-range verification and recovery logic intact.
- Key every Content catalog row by its YouTube video ID rather than positional identity; key the thumbnail's entire Compose state by video ID. A cached bitmap and an in-flight download may never visually attach to a different video's title after Recent / Views reordering.
- Instrumentation regression checks sort order in both directions and proves that each row contains the thumbnail tagged with the same video ID as the row, without relying on network image loading.

## 24H Pulse audit (code review; algorithm unchanged)
- `YouTubeAnalyticsStore.save()` records lifetime channel views, subscribers, and sampled top/recent video lifetime counters via `YouTubePulseStore.capture()` after a successful accepted sync.
- Snapshot interval target 24h, accepted 18–30h. Previous comparison uses a sample near 48h ago (40–58h).
- Change in lifetime channel views and subscribers gives the approximate recent totals. The preceding interval need not be equal length; so the growth percentage is NOT a normalized 24h-over-24h comparison.
- Momentum: +25% or higher = RISING, −25% or less = COOLING, otherwise STEADY. Missing or zero previous view growth also leads to STEADY, not confirmed evidence of steadiness.
- Video movers are limited to previously sampled top/recent videos. Samples within ten minutes overwrite the previous one; up to 96 samples from 8 days are retained.
- This is neither continuous hourly tracking nor live YouTube Analytics Realtime. Do not describe it as such in product copy.

## QA gates
Source guards, unit/lint/build, V191's 40 Android emulator cases, 1 added V192 sort/thumb case, then physical Samsung and real account verification. No changes to the Home slideshow, app data, authorization, Revenue, Reach, or Pulse calculations. Draft candidate until device acceptance.
