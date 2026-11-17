# Backlot UX08C — Reward/companion celebration certification AFTER UX10

**Date:** 2026-11-17.
**Stage:** DEFERRED QA SPECIFICATION — **0 TESTS EXECUTED**. No Kotlin compile, CI/JVM, instrumentation, physical APK validation, screenshot/animation verification, merge or release.
**Parent:** UX08B draft #418, SHA `e56f5b6df5284b88e04d4b667908af1c713a37b5`.
**Owner stable:** Accepted V337 P0 `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, unchanged; P0 19-commit/22-path reconciliation still blocked before integrated certification.
**Founder agreement:** Source staging UX01–10, one comprehensive combined Android/CI/owner acceptance cycle after UX10.

| ID | Priority | Case | Required real evidence |
|---|---|---|---|
| C01 | P0 | Creator idea saved for first daily reward | XP credited once by original store; chosen Spark/Mira celebration appears ≈3s only after durable new credit |
| C02 | P0 | Creator second/third idea saved same day | No duplicate `idea-day` XP or fake celebration; existing Idea saving unaffected |
| C03 | P0 | Idea converted to Project | One existing converted event credit, no new event key or schedule |
| C04 | P0 | Actual project stage completed and owner commit succeeds | Existing Stage Complete pulse plus chosen companion, real stage key, toast and real XP |
| C05 | P0 | Stage tap/pending write fails or gets rolled back | No newly credited reward, companion celebration, stage pulse or phantom XP |
| C06 | P0 | Project marked published on original owner path | Once-only published event and genuine credit, no duplicate on correction |
| C07 | P0 | Post-publish checkpoint explicitly confirmed complete | One eligible checkpoint credit; skip/undo doesn't farm new XP |
| C08 | P0 | Imported Creator rewards JSON then reopen Progress Journey | No replay celebration or recreated XP for past events |
| C09 | P0 | Rewarded project/item viewed, title updated, app reopened | No second credit or false on-screen celebration |
| C10 | P0 | App reopened with no current reward feedback | No companion overlay, haptic, toast or stage success claim |
| C11 | P0 | Duplicate event key in original feedback or import | Original store deduplicates; UI seen guard prevents replay within same Activity |
| C12 | P0 | Forged/malformed event key with stage event type | No companion celebration; no unsafe stage pulse unlock |
| C13 | P0 | Reward occurred >10 minutes before feedback UI | No stale/historical companion animation |
| C14 | P0 | Reward event timestamp in the future | No companion animation; original persisted data not changed |
| C15 | P1 | Valid sudden 4 reward burst | Original queue order, each eligible new credited event only, bounded memory |
| C16 | P1 | More than 4 simultaneous new credits | Original queue max4 loss policy documented; no duplicate XP/companion replay |
| C17 | P1 | Existing reward toast/aura with new companion | Both readable; no clipping/overlay that obscures original task controls |
| C18 | P1 | Stage-specific check pulse when another event type rewarded | Only genuine completed-stage credit can trigger stage pulse |
| C19 | P1 | Change chosen companion to Mira | Mira approved transparent CELEBRATE pose, Mira name, no Spark mismatch |
| C20 | P1 | Change chosen companion back to Spark | Spark original five WebP sprite pack intact and original celebration pose |
| C21 | P1 | Saved legacy Frame/Navi/Cute selected identity | Existing normalization/fallback remains; no preference overwrite or corrupted artwork |
| C22 | P0 | Notification response / Personal Reminder full-screen Done/Snooze when Creator credit arrives | Companion suppressed/noninteractive; no reminder token hijack, mis-completion or missed action |
| C23 | P1 | Quick Actions sheet, onboarding guided tour during reward | No popup over modal, no delayed replay on dismissal |
| C24 | P1 | Android animations disabled/Remove Animations | Approved static companion artwork, no moving pose/scale, ~3s polite confirmation |
| C25 | P1 | Device normal animation setting | Smooth appearance and fade-out with 2.65s hold + 0.35s exit, total ≈3s |
| C26 | P1 | Screen orientation rotates midway through celebration | No replay after recreation; original reward not credited again; no crash |
| C27 | P1 | App background/foreground midway through feedback | No replay on return, queue still safe and compact |
| C28 | P0 | Process killed/restarted after valid reward | No replay from restored ledger; original in-memory feedback not persisted |
| C29 | P1 | Saved recent celebration keys exceed 32 unique events | Bounded suppression key list; no memory increase across sessions |
| C30 | P1 | TalkBack screen reader | Polite announcement of actual reward and selected companion; no focus theft or duplicate intrusive announcements |
| C31 | P1 | 320dp screen, 200% font, landscape and tablet | Character and text readable, no controls blocked, card fits vertically and horizontally |
| C32 | P1 | Performance on low-end physical Android | Approved WebP decode/animate and feedback slot do not cause ANR or excessive frame drops |
| C33 | P0 | Personal Challenge note saved, Space photo viewed or manual Space note added | No Creator XP or new companion reward from those non-completion actions |
| C34 | P0 | Personal confirmed Done / Challenge day completion | No Creator XP contamination; Personal persistent rewards remain explicitly gated |
| C35 | P0 | Private Personal note/image/title while Creator celebration shows | No Personal data leaked in label, accessibility, logs, lockscreen or analytics |
| C36 | P0 | No source-owned credit, navigating screens or clicking Progress calendar | No XP, celebration or achievements minted by UI |
| C37 | P1 | Existing Creator Progress screen rewards/achievements | Original levels, badges, momentum, recent history and source keys unchanged |
| C38 | P1 | Android backup/import/restore of reward history | No new schema, no double credit, no animation for already credited source |
| C39 | P0 | Accepted V337 P0 branch delta integrated | All Snooze/ledger/restore/Android 15 protections preserved |
| C40 | P0 | Founder physically certifies signed integrated APK | Exact source SHA/APK hash, all UI/animation/critical reminder tests logged before any stable promotion |

## Unexecuted authored test source

`app/src/test/java/com/framebynavin/app/ui/UX08CCelebrationPolicyTest.kt` defines five JVM source tests for all five existing Creator event types, forged or missing reward keys/owners/XP, stale/future data rejection, bounded saveable seen-key policy and three-second sequence. They are NOT run, and cannot substitute for genuine Creator store/VM/Compose lifecycle/device tests.

## Stop-the-line rules

- Any unconfirmed reward/tap/import creates XP, milestone, toast or companion success.
- New overlay steals touch focus or blocks a time-sensitive Reminder Done/Snooze action.
- Cached Personal titles, photos, notes or sensitive day records leak into Creator reward labels.
- Unapproved second reward ledger, Personal/Creator shared XP, new paid provider or database migration.
- Wrong selected character, broken Spark/Mira assets, no accessibility alternative or reduced-motion failure.
- Accepted V337 P0 alarm/restore regression or release without actual combined Gradle/device evidence and owner approval.

**Current evidence:** Source-diff and GitHub commit/PR inspection only. **0/40 UX08C scenarios have executed.** No APK. No production or user data touched. All source must still compile and pass final unified tests.

**Next:** UX08D source-backed Daily Brief 2.0, followed by UX08E Content Calendar and UX08F Quick Actions integration. The persistent Personal achievements data/XP policy remains a separate founder decision.
