# Backlot V191 — Insights stabilization and final QA candidate

Baseline: V190 CI-green commit `4b374f7de12830dda20cc9e43977e3b2309ad68c` (39 Android 11 emulator tests).

## Confirmed source-level issues addressed
1. **Date-range mismatch:** when 7/28/90D selection requests a range not yet loaded, the app can retain a different cached snapshot for account controls, but it must not render that snapshot's metrics or dates as the selected range. Show a concise pending/unavailable state with retry, and keep the selected chip truthful.
2. **False zero in drilldowns:** the overview cards already use source evidence; detailed metric screens did not. Use the same evidence contract in detailed comparisons, including two-field subscriber metrics. Never infer a zero from an absent summary/previous report. Filter unreported videos from period-specific rankings.
3. **Content scrolling:** Backlot has a single outer scrolling column; composing every loaded video/thumbnail becomes expensive on large channels. Search and sorting still work across all loaded pages, while the initial rendered batch is 25 with an explicit Show more results action.
4. **Reach ownership:** Audience's cached Reach aggregation now verifies the selected YouTube channel before showing data.

## Automated acceptance
- Existing source contracts and Android test suites from V190.
- V191 JVM regressions: selected range, legacy/missing values, verified zero, unavailable previous report, subscriber net-data completeness.
- V191 Android UI regression: 40 loaded videos, 25 initial composed results, reveal more, search across all loaded uploads.
- Full Android unit, lint, personalStable APK, and Android 11 emulator suites.

## Release gates still open
- Physical device: compact typography, screen proportions, navigation, offline behavior, 7/28/90D changes, live account switch and scroll smoothness.
- Real owner-authorized YouTube Studio parity: same channel, reporting period, permissions and currency, including Revenue, Reach, Shorts and long-form.
- Backup/restore and upgrade with actual personal data. CI tests are necessary but not proof of physical-device and real-account parity.
- V191 is a testing candidate until those checks pass; do not merge or call it stable automatically.
- Keep Home slideshow and all creator data unaffected. No feature additions or on-screen teaching prose.
