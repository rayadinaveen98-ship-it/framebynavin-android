# Backlot V194 — Insights acceptance and release sign-off

Basis: successful V193 source `cf85eb9397b1dbcdcd8d9f1f6cde22846c82783e`, 41 Android 11 emulator tests plus the V193 normalized Pulse regressions.

## Release-safety changes
- Pulse reports now carry the actual channel ID. The previously stored singleton Pulse signal and the deeper Pulse drilldown **independently** reject reports owned by another channel, derived after the selected snapshot, expired by more than 36 hours, or from an invalid clock sequence.
- The policy rejects legacy reports without owner identity rather than assuming they are safe. Current reports remain available on the same account.
- Seven JVM regressions cover current, switched, legacy, expired, time ordering, malformed, and slight clock-skew cases. No additions to the default screen, no changes to metric calculation, authorization, projects or Home slideshow.

## Manual device sign-off — required, not simulated
Device: user's Android phone; record Android version, current installed Backlot version, APK SHA and date.
- Upgrade in place (do not clear app data). Verify creator projects, ideas, reminder schedule, video-project links and existing personal Home slideshow images.
- Insights: 7D → 28D → 90D → 7D, including no-cache states, waiting and Retry; every displayed chart/metric must match the currently selected period and channel.
- Content: Recent → Views → Recent, after Load videos and Load more. Confirm each video title and its YouTube thumbnail remain paired while scrolling quickly and returning from full-screen detail.
- Revenue: read-only authorization present/absent, connect, expiry, changing period and switching channel. Record the exact date span and currency, and never interpret unavailable as numeric zero.
- Reach and Audience: check availability, coverage, account switch and failed optional data with the core overview still visible.
- Pulse: same-account fresh report, no baseline, stale overnight sample and switched account; opening detail must never reveal another channel's counters.
- Offline: enter airplane mode, reopen, scroll through cached data, change periods; app must not hang or destroy creator-owned projects.
- Restore and upgrade: verify creator data, project links, scheduled reminders and personal assets before and after a backup/restore exercise.

## YouTube Studio parity worksheet — manual owner access needed
For each comparison record channel ID (avoid logging tokens), time zone, precise date start/end, metric label, value in Studio, value in Backlot, and expected definition/availability. Check Views, Engaged Views where available, Watch Time, Subscribers, Revenue and Reach. The Pulse is *counter-snapshot-derived*; it cannot be expected to equal Studio's realtime 48-hour report. Verify lifetime channel counters against Studio channel totals at nearby capture times, not an arbitrary realtime window.

## Release decision
Do not merge or promote as stable until required physical-device and live Studio parity checks have been performed and observed differences either fixed or explained. GitHub Actions proves build/tests on CI, not physical-device acceptance. Draft PR is intentional.
