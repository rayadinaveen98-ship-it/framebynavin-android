# Backlot V207-E — Verified Revenue UI and sample-aware reporting

Preview code 230 / 2.8.0-v207-revenue-truth-preview. Parent V207-D. Draft only.

## Important data limitation
Channel-wide estimated total comes from a summary API query. Video data is restricted to the top 50 returned video/type rows sorted by estimated revenue. Per-format rows therefore are not an exhaustive channel-wide Shorts/Videos/Live split. Never use their shares as if they represent all earnings.

## UI and correctness changes
- Prominent, evidence-gated Estimated revenue hero from channel summary; missing values show a dash, but verified zero stays zero.
- Clearly distinguish estimated YouTube Analytics figures from finalized AdSense payouts.
- Display exact date range and real fetched-at timestamp.
- Keep calculated Revenue / 1K views and label it as NOT Studio RPM; keep estimated ad revenue as a secondary metric.
- Hide playback CPM, monetized plays and ad impressions under More ad metrics without discarding source fields.
- Group top-ranked video rows into SHORTS, VIDEOS, LIVE and OTHER, with an explicit sample warning and no invented 100% channel format shares.
- Keep ranked videos, trend and errors; errors alongside cached results never claim a successful recent refresh.
- Preserve optional read-only monetary scope, channel ownership, cache schema, periods, Google auth and revenue query.

## Verification
1. JVM V207RevenuePresentationTest covers sample-vs-summary, missing/zero, unknown format and freshness timestamps.
2. Android V207RevenueUiTest checks money label, sample disclosure, expandable secondary figures, and separate monetary access.
3. CI source guard inherits V200–V207-D contracts and prevents invented comprehensive per-format claims.
4. Clean Gradle build/unit/lint/personalStable signed APK and emulator regression on exact PR head.
5. Samsung acceptance: period switching, no consent, denied/expired token, offline cache, long titles, account switch isolation, same projects/reminders and font-size accessibility.

## Exclusions
No bank or AdSense integration, no changes to numeric source metrics, no invented payout estimates, no currency conversion, no new background sync.
Follow-up after approval: V207-F integrated stabilization and upgrade tests.