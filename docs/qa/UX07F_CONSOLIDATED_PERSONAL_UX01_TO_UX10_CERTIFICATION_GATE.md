# Backlot UX07F — Consolidated Personal certification and P0 reconciliation gate

**Date:** 2026-11-17  
**Status:** DEFERRED TEST MATRIX / **NOT EXECUTED**. No Kotlin compile, Gradle, JVM tests, CI, emulator, Android instrumentation, physical device APK, owner data restore, merge or release.
**Audited source:** draft UX07E #415 `08ee9ad1acb1e48cd406ee5cff8958815eb69572`.  
**Protected accepted P0 stable:** `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, divergently advanced by 19 P0 commits from common V337 merge base. No modifications to accepted stable/main.
**Explicit founder sequence:** finish source staging through UX10, then one integrated certification campaign. Earlier source-level inspections do not satisfy this gate.

## 1. Current coverage inventory (all pending)

| Source QA matrix | Distinct planned cases | Executed |
|---|---:|---:|
| UX07A `UX07A_PERSONAL_PROTECTION_AND_POST_UX10_TEST_MATRIX.md` | P01–P38: 38 | 0 |
| UX07B `UX07B_PERSONAL_MY_DAY_CERTIFICATION_AFTER_UX10.md` | B01–B30: 30 | 0 |
| UX07C `UX07C_SPACES_JOURNAL_CERTIFICATION_AFTER_UX10.md` | C01–C32: 32 | 0 |
| UX07D `UX07D_CONNECTIONS_AND_GROUP_ACTIONS_CERTIFICATION_AFTER_UX10.md` | D01–D36: 36 | 0 |
| UX07E `UX07E_PROGRESS_REFLECTION_CERTIFICATION_AFTER_UX10.md` | E01–E40: 40 | 0 |
| **UX07A–E planned subtotal** | **176 scenarios** | **0** |
| UX06F `UX06F_POST_UX10_CONSOLIDATED_CERTIFICATION_GATE.md` | F01–F30: 30, separate | 0 |

These are test *designs*; they are not 176 independent instrumented test methods, promises of full branch coverage, or a claim that a device passed. Historical UX01–UX06 and original P0 suites must also be run in the final integrated gate.

## 2. UX07F static-integration cross-stage matrix

| ID | Priority | Certification action AFTER UX10 | Required evidence |
|---|---|---|---|
| F07-01 | P0 | Compare accepted P0 V337 against final integrated candidate | Exact disposition of all 22 accepted P0-changed paths with no lost alert/ledger/restore guard |
| F07-02 | P0 | Compile entire stacked Android app + all new UX07 files | Kotlin/Compose Gradle task PASS on exact SHA, dependency/report link |
| F07-03 | P0 | JVM regressions: original Personal, Phase1Habit, Spaces, Groups, Creator rewards and UX07B–E source tests | Full test suite names, PASS results, coverage caveats and logs |
| F07-04 | P0 | Android 15 exact challenge Snooze actual AlarmManager delivery/recovery, accepted P0 ledger | Correct millisecond matching, Android permission degradation, no stale valid occurrence |
| F07-05 | P0 | Android notification response token acceptance / non-Done actions | Only authenticated Done appends one Personal journal receipt |
| F07-06 | P0 | v1/v2 → v3 single-activity challenge migration and rollback to old reader | No silent decode corruption, orphan receipts, lost saved notes or unchecked voice/music defaults |
| F07-07 | P0 | Upgrade old V337 owner profile with Spaces, Groups, Photo Journal, current/legacy Challenges and Creator records | Same stable IDs, complete relevant data, privacy and archived state |
| F07-08 | P0 | Real device fail/recover private photo writes | Owner Recover/Discard, checksum, source photo preservation, no auto-deletion |
| F07-09 | P0 | Owner Spaces metadata export/import | SHA/schema/merge conflict gates, plaintext warning; no false photo/reminder full backup |
| F07-10 | P0 | Group Pause/Resume with shared members and stale links | Original Personal owner state saved at most once, exact membership validation; Android reconcile failure signaled |
| F07-11 | P0 | Space link action with deleted/archived Space or removed Personal ID | No phantom link, original Personals intact, no duplicate alarms |
| F07-12 | P0 | Space deletion while Room source or photo recovery unavailable | Disabled until verified owner preflight; no photo or original Personal deletion |
| F07-13 | P1 | UI Home My Day/Today/Settings/Activity/Creator switching, exact Home Space ID | Every prior button and deep link preserved; no wrong record or phantom Done |
| F07-14 | P1 | Original agenda filters and bulk delete selection | No hidden item deletion across scopes or separate Challenge owner |
| F07-15 | P1 | Rhythm, My Spaces, Group search and Progress panel with huge data, 320dp / landscape / tablet | Responsive scroll and bounded filtering, all original controls reachable |
| F07-16 | P1 | Screen reader, large fonts, switch input, reduced motion and animation cancellation | Semantic order, ≥48dp hit targets, actions possible with no motion |
| F07-17 | P1 | Personal completion journal unavailable / Challenge read error / Space note read failure | Unavailable distinctly shown, not zero or successful data reset |
| F07-18 | P1 | Manual Space logs vs Personal Done vs Challenge confirmed date | Three source identities distinct; note, photo, notification and view do not count |
| F07-19 | P1 | Flexible Challenge catch-up confirmed later, timezone change and day windows | Historical target day distinct from actual confirmation instant, no backdating |
| F07-20 | P1 | Archived Challenge notes in new reflection list | History read-only, exact selected day, original Done/Undo and notes preserved |
| F07-21 | P1 | Progress Journey include-Personal switch OFF/ON | OFF by default, accurate source, no private data leaked to Creator events |
| F07-22 | P0 | Exact user-facing lockscreen/notification/diagnostics with confidential Personal and Space content | No private notes/photos/unapproved titles on lockscreen or logs |
| F07-23 | P1 | Full backup/recovery, Android OS permission states, app process death, long-lived task resumption | Appropriate fail-closed read/write behaviour; no data reset or claims of delivery |
| F07-24 | P0 | Owner physical visual/functional acceptance of exact signed integrated APK | Device model/Android build, APK hash, CI source SHA, owner screenshots + signed-off pass/fail |
| F07-25 | P0 | Full Creator rewards and 21-day/Personal regressions after UX08–10 changes | No XP reward from mere view, notes, imported backup or false challenge receipt |
| F07-26 | P0 | Final release-gate review | No unresolved unwaived P0; scoped P1 disposition, P0 correction lineage, founder explicit release permission |

**Executed UX07F cases: 0/26.** These cases are cross-cutting requirements and may overlap earlier 176; don't inflate a count of distinct independent runtime test methods.

## 3. Reconciliation plan — keep accepted V337 safe

1. Freeze exact owner-accepted stable P0 SHA and verify its user physical signoff as historical provenance. Don't change that branch.
2. At the owner-authorized integration milestone, compare both branches from common V337 ancestor `678502f4626150ea1945b81eed1593a362377454`, including accepted 19-commit/22-path P0 delta and all cumulative UX01–10 source.
3. Classify each P0 path as identical, semantically already incorporated, cleanly additive, conflicting, or still missing. Preserve version-specific recovery inverse/ledger invariants.
4. Reapply only reviewed additive P0 fixes to a NEW isolated integration candidate. Do not overwrite newer UX06 Voice codec or blindly replace Android workflows/restore logic.
5. Reconcile controlled P0 `Phase1HabitLiveDelivery` millisecond matching with accepted P0 ledger/readiness/forensics and the exact Android 15 regression scripts.
6. Verify challenge v3 reader/writer downgrade policy and owner backup restore without schema/data reset; do not treat old reader inability as an acceptable unannounced data-loss risk.
7. Run full Gradle compile, test, Android instrumented suite and owner physical P0/UX acceptance; tie all results to exact candidate SHA and APK hash.
8. Only after **actual evidence** and explicit founder approval may promotion/release or merge be considered.

This reconciliation is future planned work, not performed by a document-only UX07F draft.

## 4. No-go and acceptance rules

- **No Go / P0:** saved Personal/Space/Group/Challenge/Creator records lost or duplicated; accepted P0 alarm Snooze or restore fix missing; source history corrupted by challenge v3 downgrade; private photo/gallery leakage; unsafe Group bulk or Space deletion; mistaken successful Android delivery; achievement derived from non-Done action.
- **No Go / privacy:** lockscreen note text, raw photo, personal title or private input accidentally exposed; user-unapproved backend upload, new subscription or account data writes.
- **Pending:** all CI pass links, real Android 15 evidence, visual QA, app responsiveness, TalkBack, accessibility at large font, owner decision and signed APK. Nothing in this file asserts existing passes.
- **Acceptance:** all P0 cases pass on the exact integrated candidate; remaining P1 have explicit owner-approved disposition; company-level security, privacy, architecture, QA and release criteria documented; founder explicitly approves merge/stable/release.

**Current stage verdict:** UX07 source stages are documented and stacked in drafts; **0 of 176 previous UX07A–E planned scenarios plus 0 of 26 cross-cutting UX07F cases have executed**, and the V337 accepted P0 integration blocker remains open. UX08 source staging can proceed in new isolated drafts without presenting these unresolved gates as shipped capabilities.
