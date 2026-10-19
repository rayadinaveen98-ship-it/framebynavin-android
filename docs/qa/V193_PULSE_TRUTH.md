# Backlot V193 — Pulse accuracy and status correction

Baseline: green V192 preview, commit 9f05136c8f41b1edcaa4285a1d60849c13e2e5a5.

## Changes
1. Compare the *per-hour rates* of two observed 18–30h sample intervals, instead of raw totals across potentially unequal intervals. Previous interval selected relative to the prior snapshot, not from the current timestamp.
2. Distinguish RISING, STEADY and COOLING when valid comparison evidence exists. Mark INSUFFICIENT when no previous comparison or a zero/invalid prior denominator exists; do not label it STEADY by default.
3. Mark CORRECTION if the current lifetime view counter is lower than the previous sample; show adjustment, not an invented +0 view gain or momentum alerts.
4. Prevent cached snapshot cross-channel comparisons; suppress stale Pulse reports older than 36 hours when recomputed. Existing offline past samples are retained locally for valid future comparisons, not deleted.
5. Detail screen shows actual approximate previous interval hours and normalized percent; no educational copy is added to default Insights screen.
6. Correct sampled-mover messaging to avoid implying complete-channel or real-time video attribution.

## Acceptance
Eight JVM tests: different-duration equal/rising rates, missing or zero prior, corrections, stale data, channel isolation, and sampled mover attribution. Existing Android 11 emulator suites (41 tests) and entire build/test/lint pipeline remain mandatory.

## Still pending for stable release
Real YouTube Studio comparison, actual device behavior, cold-start freshness, upgrade/backup integrity, disabled internet, and signed APK installation. User-owned data, Home slideshow and all existing analytics permissions remain unchanged.
