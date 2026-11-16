# Backlot — V337 to next owner-test build: non-destructive upgrade and acceptance gates
**Date:** 2026-11-16  
**Status:** PROPOSED, not implementation approval. Owner approved P0 closure documentation and six-stage integration *preparation only*. No code, manifest, signing, database, UI, release or APK work is authorized by this document.

## 0 — Authority and reference
- Immutable reference: `stable/backlot-v337-premiere-owner-testing-20261114` SHA `678502f4626150ea1945b81eed1593a362377454`; `com.framebynavin.app`, `versionCode=337`, `versionName=2.28.7-premiere-cinematic-enhanced-02`.
- P0 application-source candidate: draft [#389](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/389), SHA `815ba69775f75b6e5f25bb838c2c17b4f09c203e`; combined [CI 43855917050](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43855917050) SUCCESS.
- Owner QA signing: existing `personalStable` uses the repository's public preview keystore, not a Play Store release key. QA workflow [43867609066](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43867609066) SUCCESS, artifact ID `14228797432`.
- The existing six-stage source features are **already in V337**; do NOT re-merge outdated Stage 1–6 branches or clone widgets/resources. See source audit.
- Existing user settings contract: user-facing Backup/Restore stays hidden; Advanced local erase stays available behind confirmation but must not be exercised in acceptance.

## 1 — Proposed gates and deliverables
| Gate | Main task | Evidence needed | Current |
| --- | --- | --- | --- |
| G0: P0 closure handoff | Archive exact source, CI success and signed owner QA build; capture owner feedback | P0 closure doc, CI links, APK fingerprint, honest owner-smoke limitation | **Documented**; detailed physical matrix remains open |
| G1: Six-stage inventory and conflict audit | Reconcile stacked and alternate branches, identify active screens, providers, dialogs, callbacks, prefs | File/PR matrix, known gaps, selected source of truth | **Documented**; no merges |
| G2: UX QA plan | Fresh/returning users, reminder delivery, widgets, companions, Premiere, accessibility, privacy | Repeatable phone/tablet script and screenshots; each test tied to actual build ID | **Proposed** |
| G3: Explicit founder scope decision | Approve only true gaps/bugs, and distinguish visual enhancement from P0 correction | Written acceptance criteria and PR/file boundaries; no broad rewrite | **Pending** |
| G4: Controlled candidate | New *approved* branch from exact `815ba697`; versionCode > 337 for future installation; product source changes only where authorized | File diff, code review, new focused + full JVM/Android CI | **Not authorized / not created** |
| G5: Physical acceptance and upgrade | In-place install, data continuity, lock-screen/Doze/reboot, sound, privacy, launcher and onboarding on real devices | Device+OS+APK signer/version logs, timestamped video/screenshots, signed checklist | **Not certified** |
| G6: Release approval | Founder go/no-go, release signing/privacy/support/rollback plan | Signed release decision, staged distribution plan | **Blocked** |

## 2 — Controlled upgrade plan (PROPOSED)
1. Identify exact installed package, build version, public preview signing certificate, Android version and available free space **before** any install. Record current reminders, project count, Space count, 21-day challenge receipts, guide selection, scene selectors and pinned launcher widgets without exposing private contents.
2. Keep current installed V337 working; create non-destructive snapshots/evidence and a tested recovery approach **without relying on removed Backup/Restore Settings UI**. If source-level storage export is contemplated, obtain separate approval and privacy review; never copy live user data into CI.
3. Review P0 combined branch against current frozen V337. The next *candidate* should derive from exact P0 `815ba697` and preserve its millisecond Snooze comparison. Old PR #377 APK has `versionCode=336` and must **not** be installed as a “new” build over 337.
4. Future owner-sideload candidate should use the **same verified preview certificate** and a monotonic `versionCode` **at least 338** (proposed). Preserve package name and DataStore/DB schema; do not invent migration, delete data or modify production-signing keys.
5. First run an in-place upgrade on disposable, realistically seeded synthetic Android install from V337. Assert projects, ideas, Spaces, Personal reminders, 21-day records/receipts, guide choice and independently stored scene settings survive unchanged.
6. Test old pinned widgets migration (12 old providers removed; exactly 3 new ones). Expect some launchers to leave stale/empty tiles; explain how to remove/re-add **without touching app data**. Do not silently convert old widget IDs, fake data, or auto-create widgets.
7. Exercise interruption/restart during update and process death; ensure scheduled alarms rearm and no duplicate completion receipts/notifications.
8. After synthetic certification, founder manually installs on *own* Android device only after informed acceptance. If Android reports “App not installed” / signer or downgrade mismatch, **stop** and preserve data. **Never uninstall or clear storage as a workaround.**
9. Android does not generally permit safe installation of a lower-version APK over a higher version. A rollback must be a **separately tested forward-version hotfix / equivalent non-destructive approach** signed with the same certificate, not an unplanned V337 downgrade or uninstall. Do not promise a working rollback without verified procedure.
10. Maintain protected V337 and original P0 build artifacts for regression comparisons. No production release just because preview APK builds.

## 3 — Physical acceptance matrix (strict pass criteria)
| Domain | Minimum acceptance procedure | Release-blocking defects |
| --- | --- | --- |
| Existing owner install | Install over earlier V337, reboot, revisit all user domains; no account/login reset | Lost work, altered Done receipts, broken update/signing |
| Fresh Creator | Choose Creator, set explicit 3 fields, first project composer, restart; no extra forced setup | Blank onboarding, false project creation, stuck permissions |
| Fresh Personal | Choose Personal, immediate Personal hub, create reminder + 21-day challenge; enter Creator later | Forced Creator profile or sign-in, fabricated data |
| Fresh Both | Set actual Creator choices, retain Personal entry, persistent Spark/Mira | Misrouted focus, lost data |
| Returning/partial legacy | Existing complete account bypasses new onboarding; partial setup follows old path | Unexpected reset or profile overwrite |
| Companion | Switch Spark/Mira, notification cold-start and restart, dark/light art cutouts and locked public privacy | Identity resets, broken art/notifications, leaks private text |
| Creator + Personal alarms | Schedule naturally while app foreground/background/screen locked; sound, title, controls, Snooze, Done, Replay, dismiss | Missed/duplicated alarm, fake Done, wrong route, privacy leak |
| 21-day challenge | Real 10-min Snooze on the posted Android action; repeat, confirm no synthetic completion | Missing delayed alert, stale rejection, duplicate/false Done |
| Alert health | Deny notification/exact alarm intentionally **only on test device**; check accurate explanation and safe fallback | Claims permission allowed when blocked, no useful error |
| Reboot/Doze/OEM idle | Test on physical device with lock, sleep, reboot and ordinary battery mode | Missed reminders, infinite loops, excessive drain |
| Three new widgets | Fresh picker exactly 3; pin/resize, empty/corrupt stores, manual refresh, check private labels absent | Wrong numbers, crashes, private text, old providers advertised |
| Cinematic scenes | Voice/Alarm independent selections; V391 default retained; V394 Premiere OFF by default, safe preview, callback parity | Clipped controls, settings changes break alerts, privacy leak |
| 21-day Premiere | **Currently illustrative preview only**; do not present as live connected challenge UI | Demo “Day 08” shown as real progress or auto-alert |
| 21-day voice/music | **Not implemented as parity feature**; requires separate authorization | Claim of parity without real playback implementation |
| Accessibility | TalkBack, 1.5x/2x text, smallest phone, tablet, high contrast, reduced motion and saver | Unreachable Done/Snooze, obscured text, uncontrollable motion |
| Reliability/performance | Long session, background audio focus, app process death, multiple due alerts and low battery | ANR, excessive memory/heat, notification starvation |

All runs must record: exact APK SHA256 and certificate fingerprint, package/version, device/OEM/Android API, permission & battery state, screen-lock state, expected/actual due timestamps, pass/fail, screenshots/videos where appropriate, whether a reproducible failure exists. Only synthetic/test content in shared logs.

## 4 — Architecture/security/operations audit gates
- **State machine and storage:** read-only inventory of Personal/Creator/Challenge stores, persistence/version adapters, scheduling recovery, notification channels, ownership of pending intents and Done/Snooze state transitions.
- **Privacy:** Notification public version must remain anonymous for private activity; launcher widgets must not expose journal, Personal title or 21-day activity. No tracking SDK or third-party dependency added silently.
- **Internal backup and restore:** Settings links absent, internal non-exported activity classes remain. Perform reachability/call-site audit; no deletion or feature resurrection without approval.
- **Accessibility/performance:** scene animation frame throttling, power saver/reduced motion, semantics, long titles, full-screen intent permission fallbacks, battery constraints and audio focus.
- **Release build:** preview signer is publicly known; real distribution key must be owner-controlled and never committed; compare SHA, manifest, APK identity and package manager install output. Costs/paid services require separate founder sign-off.
- **Evidence:** separate SOURCE IMPLEMENTED, AUTOMATED TESTED, OWNER SMOKE REPORTED, PHYSICAL MATRIX CERTIFIED, APPROVED, RELEASED. Do not collapse statuses.

## 5 — Founder review needed before starting G3–G6
Approve or revise:
1. The **inventory-first** decision: no six-stage replay; stabilize and polish present V337 features against detailed acceptance.
2. Whether to prioritize **a full six-stage physical QA pass** or start a **separately scoped 21-day Premiere production UI + audio parity** proposal. The latter is not an already implemented feature.
3. Any specifically authorized per-file bugfix/refactor, version bump, migration, APK build, integration merge or production release. None is included in current documentation-only approval.

## 6 — Stop conditions
Stop before changes if a product-file diff appears during documentation work; branch moves; a CI result is not exact SHA; data retention/export requires consent; APK signer or version conflicts; old widget/backup code appears externally reachable; app reset is proposed; performance/accessibility result is unverified; any destructive operation is contemplated.

**Conclusion:** P0 engineering gates passed at one exact candidate SHA, owner general smoke reported. Six-stage feature code already exists in V337. The next responsible action is review and certify it, not speculative reimplementation. Leave stable/main untouched.
