# Backlot UX12 D7 — Source Review Closure and D8 Acceptance Ledger

Date: 2026-11-22
State: **SOURCE-STAGED REVIEW CLOSURE ONLY**. No compile, Gradle, unit, instrumentation, lint, screenshots, physical APK, merge, deployment, or release. No source correctness/experience certification is implied.

## Scope and chain of custody

- Parent D7 at `516751a714f3a51c332274d6ea4867127472900c` on `feature/backlot-ux12-d7-three-reminder-experiences-20261122`.
- Review closure branch: `feature/backlot-ux12-d7-review-closure-20261122`, based exactly on parent D7; no main/stable or UX09 changes.
- Protected baseline: `abbe88984bac16cfafbd990d9cb00ed54cfdd07e` on main and owner-stable (verify again before final D8).
- Do **not** automatically run any tests, builds, APK assembly, CI dispatch, merge, hosted write or deployment. The founder expressly deferred comprehensive QA until `Done bro, start next steps and give APK` (or another equally explicit final-stage instruction).

## Real source finding / bounded remedy

**Found and closed in source:** D7 had unconditionally placed a `V144GuideCharacter` on the 21-day Challenge MAIN surface. That ignored the already-saved `V407ReminderCompanionPrefs.showOnLiveAlerts` (OFF by default), while the Creator and Personal MAIN surfaces already call `V407ReminderCompanionMoment` which honors that preference.

**Actual replacement in `RX01ChallengeAlertActivity.kt`:**

- Reuse `V407ReminderCompanionMoment` rather than invent an additional character renderer, animation or saved preference.
- Reuse `V407ReminderCompanionPrefs` initialized from the existing app context. **No companion at all** (and no reserved hero height) when opted out.
- When opted in, show the approved original Spark/Mira character via the existing cameo's 3-second fade-and-exit policy, with its low-power/reduced-motion behavior, no sound, recording, progress or completion.
- Keep a fixed 84dp/96dp small cameo region while opted in, to avoid vertical reflow when the cameo disappears. No overlay over Done/Working, Snooze or Close controls.
- Pass the true existing `requestedId` + `requestedAt` to the cameo key; title or activity text is **not** used as identity. Reuse the real RX01 owner-verified occurrence and the previously unlocked/valid MAIN composition branch.
- No challenge data/Day receipt mutations; all original `onOpenChallenge`, `onSnooze`, `onClose` handlers, Action Intent, challenge receiver, permissions, locked state, gestures, private content, background scheduling, notification and audio remain unchanged.

## What is—and isn't—in UX12's approved companion scope

**Source staged:** D3 selected-character first-run and entry transitions, D4 small Personal home character, D5 compact thoughtful Idea Vault cameo, D6 Settings summary companion, D7 existing optional Creator/Personal alerts and corrected optional Challenge cameo. Character art remains original approved Spark/Mira sprites. Existing state-dependent `CinePulseState` poses may differ by screen.

**NOT implemented or authorized here:** a new app-wide emotion AI engine, new unapproved character poses/assets, on-device facial expression recognition, automatic new voice interactions, gesture-controlled companions, art regeneration, or compulsory reminder companions. More extensive expression coverage is a potential separate proposal, not a blocker for the existing signed-off practical UX12 visual stages unless the founder expressly expands the scope.

## D7 source-only checklist (NOT test results)

| Domain | Confirmed owner wiring in source | Remaining D8 acceptance |
|---|---|---|
| Creator Voice and Alarm | RX01 gate from existing VoiceReminderActivity/AlarmActivity; current occurrence re-read, existing main action callbacks | Visual screenshots, playback and notification fallback, replay, Done/Working/Snooze/Reschedule and expiry on real device |
| Personal Voice and Alarm (eligible user-opened) | Unlocked ready occurrence passes RX01; title and actions remain behind original keyguard branch; `PersonalDeliveryCoordinator.apply()` retains action authority | Background/long idle/P0, correct keyguard visibility, valid token, auto retry, manual Snooze, close/dismiss, large-font compact layouts |
| 21-Day Challenge Voice and opt-in Alarm | Owner-scoped RX01 visual; locked/checking/invalid paths unchanged; original Challenge page retains confirmation; original 10-min Snooze receiver | New opt-in cameo on/off cases, original activity/day info, Android FSI eligibility and notification fallback, snooze outcome (not just sendBroadcast), process recovery |
| All three entrances | RX01 original domain palettes, accurate current mode icon, accessible double-tap gate, main content absent until two verification passes | 1 tap, 2 taps, TalkBack equivalent, stale-token behavior, low-power/zero-animation, safe insets and no false live-mic claim |
| Classic / Premiere | Source retains legacy route and preferences when RX01 disabled | No doubled layers or route regressions, legacy user preferences retained |

## Required D8 gate and acceptance order

1. Before integrating, review every D1–D7 and closure file diff and check source ownership against frozen `main` and prior RX01/UX11 integration PR #460. The source chain is isolated from UX09. Make only necessary fixes in the integration candidate, never stable.
2. Check compilation and Kotlin imports/calls and handle failures honestly. Unit, policy tests, instrumented Android15 gate and lint follow only founder D8 authorization; test evidence must correspond to the final integrated source SHA.
3. Confirm all three mains retain their original response callback semantics and do not display private Personal/Challenge titles over keyguard. Test optional cameo OFF and ON including duration, stability and exact occurrence key after repeated notifications. Verify no overlapping buttons at 320dp and 2x fonts or on Redmi Pad 2.
4. Do not assume that a notification posted, a UI passed, or an audio request was accepted proves actual ringing. Execute P0-long-idle, 5/10-min Snooze, retry and callback tests separately, under observed Android/HyperOS OS behavior. Record device build and times; preserve real data.
5. Verify appearance choices, Google/local first-run, original Creator setup, companion selection, Idea Vault, Settings, UX12 interactions and original RX01/UX11 regression matrix. Conduct physical visual acceptance and manual tests before any stable promotion.
6. Build only the final permitted **Lab** APK, verify signed package identity/SHA/source provenance and supply for founder acceptance; no production release or hosted modifications without separate authorization.

## Result

**D7 review finding closed in source**: optional companion preference is now consistent across the three alert mains. All D0–D7 practical visual source stages remain staged; the cumulative Android build and final UX12 QA are **NOT certified**. **D8 remains held**, and Personal Reminder reliability P0 remains OPEN.
