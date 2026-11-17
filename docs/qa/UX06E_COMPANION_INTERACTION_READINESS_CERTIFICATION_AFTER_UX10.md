# Backlot UX06E — Companion, Action Meaning & Android Access Certification (AFTER UX10)

**Status: SPECIFIED / NOT RUN.** No pass, build, emulator, owner-device acceptance or release claims.
**Cumulative source parent:** UX06D draft PR #407, head `aa5a6933e53f3e7acc2b609976dbcc7865f8f69d`.
**Protected owner-approved baseline:** stable V337 remains untouched.

## A. Core principles

- A user must always be able to respond to a reminder without seeing/waiting for a companion or opening a diagnostic panel.
- A companion is opt-in, uses the user's existing Spark/Mira choice, appears only in the hero artwork and disappears after three seconds without a sound/action side effect. The preference defaults OFF for old installations.
- The Android access inspector is **read-only** until the user explicitly opens Android Settings. Access Allowed is not OS fired, notification posted, speech heard, owner viewed, acknowledgement or Done.
- A missing channel is UNKNOWN; Personal Voice cannot offer an irrelevant automatic full-screen permission action.
- Personal locked and invalid contexts reveal neither title nor companion nor private notes nor response controls.
- Premiere sample/Challenge previews must not masquerade as a real alert or trigger Android permissions.
- Challenge Voice information in Alert Health must come from saved record/ledger and never be a delivery guarantee.
- Creator, Personal, current single-activity Challenge, Ideas and old V297 retain original separate canonical response/scheduling stores.

## B. Deferred post-UX10 test matrix

| ID | Test condition | Expected outcome |
|---|---|---|
| E01 | Upgrade V337 with existing selected Spark and no UX06E preference | Companion OFF; historical identity unchanged, no alarm action changed |
| E02 | Existing selected Mira and opt-in switch ON | The approved Mira assets appear, never replaced by Spark |
| E03 | Legacy Frame/Navi/Cute saved, app picker offers Spark/Mira | Preserve historical key, use documented current compatible guide fallback, do not delete original saved value |
| E04 | Toggle ON/OFF in Reminder Scenes | Local preference persists through restart; no change to Classic/Cinematic scene or Premiere switch |
| E05 | Creator Voice classic live alert, ON | Selected companion appears inside hero for ~3s, response buttons fully accessible immediately after intro reveal |
| E06 | Creator Alarm classic live alert, ON | Same; no Ring/Stop/Done/Snooze callback from artwork |
| E07 | Personal Voice unlocked live alert, ON | Companion appears in hero; no title/Done mutation |
| E08 | Personal Alarm unlocked live alert, ON | Companion appears in hero; same actions and timing |
| E09 | Personal Alarm locked/keyguard presenting | Generic lockscreen only; no private notes/title/companion/action text even when ON |
| E10 | Personal invalid/deleted/paused record | No companion and no obsolete action; Close behavior unchanged |
| E11 | Creator/Personal Premiere live ON | Companion overlays only hero artwork, does not delay or obscure controls |
| E12 | Premiere Settings preview, Creator/Personal | No companion/permission inspector, sample actions still no-op previews |
| E13 | Premiere Challenge visual preview | No live companion/permission warning or Challenge Done call |
| E14 | Challenge current voice notification | No forced full-screen, companion preference doesn't change schedule |
| E15 | Idea notification | No full-screen conversion, no new action/media start |
| E16 | Turn Animator scale 0 | No animation requirement, static companion and immediate accessible actions |
| E17 | Battery saver enabled | Static reduced-motion companion, no endless animation or battery drain |
| E18 | App backgrounded/alert dismissed before 3s | Lifecycle cancels companion effect, no overlay leak into another activity |
| E19 | New reminder occurrence with different owner ID/token | New three-second greeting only for new occurrence when ON |
| E20 | TalkBack focus traversal | Companion excluded as decoration; Done/Snooze/Dismiss, more options and access controls announced |
| E21 | Compact 320dp, 200% font, landscape, tablet | No clipped primary action, user can scroll to diagnostics and reach Exit |
| E22 | Native Presenter settings primary app notifications OFF | Snapshot reports BLOCKED; no 'Guaranteed' statement |
| E23 | Creator Voice/Alarm channel missing before first use | Channel UNKNOWN, not treated as Allowed |
| E24 | Creator channel blocked or importance low | BLOCKED / DEGRADED as appropriate, actions still selectable |
| E25 | Personal channel blocked/missing | Domain-specific channel checked, no false Creator channel use |
| E26 | Android API 31+ exact alarm denied | Limited precise timing with truthful inexact fallback and a user-invoked OS Settings route |
| E27 | Android API 34+ FSI denied for Creator Alarm/Voice | Limited full-screen, notification still actionable; manual access settings only |
| E28 | Personal Alarm FSI denied | Applicable warning, locked privacy remains generic |
| E29 | Personal Voice FSI denied | Full-screen = NOT USED; does not ask user to change irrelevant access |
| E30 | Permissions access changes after opening OS Settings | User-initiated RECHECK refreshes snapshot, no false live status |
| E31 | Android Notifications settings unavailable | Fallback app settings, no crash, no permission bypass |
| E32 | Android precise-alarm intent unsupported/restricted | App-settings fallback, never auto-opens OS Settings |
| E33 | Creator Stage Done vs I'm Working | Existing authoritative stage gate/reward, separate response meanings unchanged |
| E34 | Creator normal I'm Working vs Dismiss / Smart live-stage Snooze | No accidental Done, same final Smart target/occurrence action token |
| E35 | Personal Done vs Dismiss without completing | Exact distinct response receipts remain, no inferred done |
| E36 | Challenge Alert Health with v2 migrated Voice challenge | Shows historical 3 repeats, 5s and Music Off; saved Done receipts identical |
| E37 | Challenge Alert Health with v3 Voice custom music | Shows saved plan but never 'heard' or 'delivered' from selection alone |
| E38 | Challenge missing/revoked SAF music, TTS unavailable | Health uses actual ledger for voice evidence; setting does not imply audible success |
| E39 | Rapid multiple alarms and duplicate notification actions | No altered receiver, duplicate receipt, service or hidden acknowledgement due to UX06E |
| E40 | V337 upgrade/backup/restore/downgrade and cross-device | Original owner data preserved; new prefs opt-in safe; UX06D v3 rollback handled by gated policy |

## C. Required evidence

- Exact candidate SHA, Gradle + unit + instrumentation + emulator CI artifacts; full UX06A–E and UX07–UX10 regression logs.
- Physical captures: 3-second selected companion ON/OFF, Spark/Mira, compact and tablet, locked Personal, normal + Premiere.
- TalkBack, 200% font, keyboard/switch access, reduced-motion and battery-saver device recordings.
- Permission matrix for Android 13–16 supported API devices: precise, notifications, FSI, per-channel importance; screenshots of OS Settings taps and RECHECK.
- Owner data fixtures: Creator/Personal/Challenge/Idea receipts before and after; old V297 import/export untouched.
- 21-day alert ledger and raw TTS/music observation separate from UI access snapshot; never classify Unknown as a user ignoring an alert.
- Explicit owner physical visual/functional acceptance and signed off blockers with risk severity.

## D. Deferred risk ledger

- Full-screen readiness may differ from real Android UI policy, manufacturer settings, DND, channel muted sound and Play eligibility.
- Notification channel creation timing can result in UNKNOWN until first eligible delivery; this is honest and expected.
- 3-second companion reveal is a Compose lifecycle effect, not an OS timed/real-time guarantee. Verify it doesn't obscure controls on small devices.
- The existing V195 Creator particle intro still requires reveal by user; UX06E does not silently change this approved behavior. Real-device acceptance should evaluate whether it is ergonomic with urgent alarms.
- Companion selected identity snapshot on background alerts must be checked alongside Settings; no stored ID migration was introduced.
- App settings permissions may remain denied after return until the owner manually RECHECKs; no hidden background permission polling.
- UX06D challenge v3 archive downgrade and SAF URI cross-device restrictions remain separate release gates.

**Verdict at source stage:** IMPLEMENTED IN DRAFT; NOT TESTED or CERTIFIED.
