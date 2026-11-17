# Backlot UX10F — Cumulative source, V337 P0 reconciliation and release gates

**Date:** 2026-11-17
**Status:** UX10F SOURCE-STAGED / integration assessment only. No GitHub PR merge, Java/Kotlin compilation, JVM/CI/Android instrumented test, APK, real-device certification, signed release or production/cloud migration.
**Stacked source parent:** UX10E PR #427 head `411b733dae308dcd3948d99c39faf840d443d059`.
**Protected current owner-accepted stable:** `stable/backlot-v337-p0-owner-accepted-20261116`, exact SHA `815ba69775f75b6e5f25bb838c2c17b4f09c203e`. No changes to that branch, main or production.
**Founder decision:** do UX10 BEFORE UX09. UX09 cloud/multi-device synchronization is deliberately DEFERRED, not canceled, implemented, tested or release-ready. The founder previously wanted one consolidated full test campaign only after all ten UX points. Do not interpret the sixth UX10 substage as completion of the deferred tenth feature point UX09.

## 1. Verified GitHub divergence — release P0 blocker

On exact current UX10E head compared with accepted V337 P0 head:
- GitHub compare `V337 -> UX10E`: **ahead 418, behind 19; 213 unique changed paths**. This is a whole-branch comparison, NOT 213 UX10E-only files.
- Reverse compare `UX10E -> V337`: **ahead 19, behind 418; 22 files not in the UX branch**.
- **One direct changed-path intersection:** `app/src/main/java/com/framebynavin/app/personal/Phase1HabitLiveDelivery.kt`. There are also functional dependencies across additional app/test files. The single intersection is NOT proof a merge is safe or conflict-free.

### Exact 22 accepted P0 corrective files to account for

**4 production Personal reliability files:**
- `app/src/main/java/com/framebynavin/app/personal/Phase1HabitAlertReadiness.kt`
- `app/src/main/java/com/framebynavin/app/personal/Phase1HabitDeliveryLedger.kt`
- `app/src/main/java/com/framebynavin/app/personal/Phase1HabitLiveDelivery.kt` **DIRECT SOURCE OVERLAP**
- `app/src/main/java/com/framebynavin/app/personal/Phase1V2RestoreDomainForensics.kt`

**11 tests:**
- `app/src/androidTest/java/com/framebynavin/app/personal/P0ActualChallengeSnoozeDeviceTest.kt`
- `app/src/androidTest/java/com/framebynavin/app/personal/P0AlertHealthCorrectedDeviceTest.kt`
- `app/src/androidTest/java/com/framebynavin/app/ui/V339HomeOverflowMenuUiTest.kt`
- `app/src/androidTest/java/com/framebynavin/app/ui/V340HomeSpacesEntryUiTest.kt`
- `app/src/androidTest/java/com/framebynavin/app/ui/V341UnifiedBackupPanelUiTest.kt`
- `app/src/androidTest/java/com/framebynavin/app/ui/V342UnifiedRestorePreparationUiTest.kt`
- `app/src/androidTest/java/com/framebynavin/app/ui/V343UnifiedRecoveryEscrowUiTest.kt`
- `app/src/test/java/com/framebynavin/app/data/V352UnifiedRecoveryDomainPolicyTest.kt`
- `app/src/test/java/com/framebynavin/app/personal/Phase1HabitDeliveryEvidencePolicyTest.kt`
- `app/src/test/java/com/framebynavin/app/personal/Phase1HabitLiveDeliveryPolicyTest.kt`
- `app/src/test/java/com/framebynavin/app/personal/Phase1V2RestoreDomainForensicsTest.kt`

**5 automation/reliability drivers:**
- `.github/workflows/backlot-p0-approved-integration-candidate.yml`
- `.github/workflows/backlot-p0-recovery-regression-isolated.yml`
- `.github/workflows/backlot-p0-v337-combined-reliability.yml`
- `tools/qa/backlot_p0_actual_snooze_android15.sh`
- `tools/qa/backlot_p0_combined_android15.sh`

**2 acceptance/QA records:**
- `docs/qa/BACKLOT_P0_APPROVED_CORRECTIVE_INTEGRATION_CANDIDATE_2026-11-16.md`
- `docs/qa/BACKLOT_P0_V337_COMBINED_RELIABILITY_INTEGRATION_2026-11-16.md`

Source QA docs distinguish the earlier V337 **reference** `678502f4626150ea1945b81eed1593a362377454` from the later **owner-accepted P0** `815ba69775f75b6e5f25bb838c2c17b4f09c203e`. Do not merge by symbolic version label or accidentally revert an accepted correction.

### Source-grounded P0 rules that MUST carry forward

- Challenge Snooze PendingIntent uses exact occurrence milliseconds: persisted Instant may contain nanoseconds, so full Instant equality falsely rejects legitimate Android 10-minute Snooze; preserve the corrected milliseconds comparison.
- Missing Android 8+ notification channel is genuinely not allowed/needs attention, rather than silently inferred allowed from a nullable importance check.
- Later failure/start chronology evidence for the exact Challenge occurrence must be ordered correctly; a stale failure cannot override a later genuine success or another occurrence.
- Recovery malformed archive/forensics must remain fail-closed, preserve authoritative local owner and avoid destructive restore or account cross-contamination.
- **All** Android instrumentation Kotlin sources should compile on same candidate. The older P0 isolated preview quarantined five stale UI tests; don't achieve green checks by excluding them or reintroducing founder-removed backup/restore Settings pages.
- Actual OS scheduled 10-minute Snooze, notification post, fresh occurrence ledger, full-screen/alarm/TTS/music, reboot/rearm and Android 15/OEM real device require true execution evidence. No forged callbacks.
- V337 P0 cannot be considered reconciled merely because one file conflict is resolved; tests, CI script assumptions and removed backup UI are nontrivial.

## 2. Cumulative UX design stage lineage

| Stage | Last verified source PR | Scope | Status |
|---|---|---|---|
| UX01–UX07 | Earlier staged UX chain | Welcome, onboarding, Home, Creator, reminders and Personal design/features | Source-staged historical branches; no combined compile claimed |
| UX08A–F | Draft PRs #417–#422 | Audit, progress Journey, XP companion celebration, Daily Brief, Content Calendar, Quick Actions | Source-staged; final UX08F head `47e785f8a598862154813bfe2ba409e7a5de73ed` |
| UX10A | Draft PR #423 | Shared font/spacing/target/border tokens, core Creator surface | Source-staged |
| UX10B | Draft PR #424 | First-run, Home and bottom navigation | Source-staged |
| UX10C | Draft PR #425 | New Project, Stage Desk, Idea Vault, Content Workspace | Source-staged |
| UX10D | Draft PR #426 | Personal Reminders/Spaces/Challenge/Group/Photo | Source-staged |
| UX10E | Draft PR #427 | Insights, Settings, Notification Center, widget previews | Source-staged |
| UX10F | THIS stacked draft | Remaining narrow analytics reflow, data-truth/a11y polish and exact integration handoff | Source stage only |
| UX09 | Not started in this ordered work | Account-based Creator/Personal per-record multi-device sync, RLS, offline reconciliation | Deferred by founder, not merged or tested |

UX10A–E contain 36 newly authored, **0 executed** source tests and 200 written, **0 executed** deferred QA scenarios. UX10F adds eight source tests and 40 QA scenarios. **TOTAL UX10A–F = 44 authored / 0 executed tests, 240 planned / 0 executed QA checks**. These counts are for newly introduced UX10 test files/checklists, not all original repository tests and not proof of Android correctness. UX08 has its separate deferred coverage.

## 3. Exact UX10F source fixes and limitations

- NEW `ui/UX10FContinuityPolicy.kt`: one readable analytics metric-card policy, stack below 400dp or fontScale >=1.35; fails safely on invalid fontScale; no access to accounts, report data or business state.
- `ui/V11YouTubeInsights.kt`: original four genuine YouTube metrics now produced in the same order from the original snapshot, with one/two-column responsive layout; no change to `YouTubeAnalyticsSnapshot`, OAuth, network fetch, data periods or values.
- `ui/V172InsightsUi.kt`: five original Creator metrics and the original PUBLISHED, ACTIVE, FINISHED, CONNECTED and IDEAS drilldown targets remain mapped one-to-one, rendered in responsive rows; no reward/Creator store changes.
- `ui/V306ActivityCenterScreen.kt`: original synchronous Creator acknowledgement read `creatorEvidence == null` represents a failed read (source uses `runCatching {...}.getOrNull()`), not pending async loading. Expose it as unavailable via already source-staged UX10E copy policy. Never fake empty history or write a receipt.
- `ui/Phase1HabitSharedUi.kt`: original 21-day grid TalkBack description now includes the actual saved local date, not only the day index and status. Does not alter date math, `habit.status`, Done, Snooze, undo or persistence.
- NEW `UX10FContinuityPolicyTest.kt`: eight **authored, unexecuted** source tests for metric breakpoints, malformed accessibility scale, read failure precedence, 21-day count and shared design minimums.

### Known UX01–08 gaps not fixed by global visual work

- UX08B historical progress timeline decode false-empty risk.
- UX08D/E weekly Plan typed-read/loading coverage risk.
- UX08C XP celebration on selected Spark/Mira physical 3-second motion/trigger validation missing.
- UX08F Skip owner/current task race gate.
- Personal reward ledger/achievements outside the already staged Creator rewards still need founder product decision.
- Reminder audio/music/full-screen, brand animation timing and Android display compatibility require actual device evidence; styling code is not a functioning playback service.
- UX09 new Supabase target, RLS, per-entity tombstones, offline queue and account-scoped parity have NOT been implemented. The source still has snapshot-based partial cloud coverage; no full autosync claims.

### Widget product gate

Founder earlier asked for all old widgets to be deleted and a new widget system planned. The current repository still contains old launcher widget providers and an opt-in migration/preview. UX10E preserved them because **deleting widget providers is not a visual polish action** and can break already pinned shortcuts; separately approve migration architecture and replacement/compatibility plan. Do not silently unpin or remove any existing widget, credential or local data.

## 4. Required integration sequence / no implicit authority

1. **Freeze all stacked source HEADs and audit exact PR ancestry**. Ensure UX10F direct parent is PR #427 head, and no new direct modifications in protected stable or main.
2. **Owner reviews proposed target integration shape**: isolated candidate branch from cumulative UX10F head (or an approved alternate) for correction incorporation, never direct merge into owner stable. Do not create it or merge the P0 changes without explicit authority.
3. **Reconcile the 22 accepted V337 P0 paths** against UX10F: retain corrected notification channel health, Snooze millisecond identity and failure chronology, and fail-closed recovery forensics. Analyze the direct Phase1HabitLiveDelivery overlap at hunk/behavior level. Preserve all 11 regression tests, 5 workflow/scripts and 2 docs with their actual provenance.
4. **Architectural review UX09** when founder returns to it: choose secure account/domain canonical sync, RLS/deletion/offline/conflict policy; verify new Supabase project cutover/credentials and projected costs with explicit founder gate. Never silently swap project URL/keys or upload Personal/private photo data.
5. **Compile and execute the true cumulative verification campaign on the exact integrated SHA** (original founder prefers one final campaign): compile Kotlin/Compose, all JVM/unit tests, full Android instrumentation Kotlin compile, CI/test XML, scripted Android 15 10-minute scheduled Snooze, real notifications/audio, offline/restore/Creator/Personal regression. Do not skip failing tests or mask missing-provider behaviors.
6. **Capture real phone/tablet visual proof** in both themes, 320/393dp, 1.0x/1.5x/2.0x font, TalkBack/reduced-motion, IME, light/dark contrast and all major first-run/empty/loading/error/selected/notification states. Fix real clipping while preserving semantic action.
7. **Owner receives a fresh identified APK** only from the verified exact source SHA and signs off physical acceptance. Require rollback/reference, migration recovery, privacy review and store listing before any separate stable promotion and Google Play release decision.

Do not confuse source-staged (UX10F) with implemented-and-verified release; a clean GitHub compare or a QA plan is not proof of a successful build.

## 5. Approval gates and stop-the-line

- **NOT AUTHORIZED:** merging any draft into main/stable, integrating V337 fixes without reviewed candidate approval, destructive widget changes, changing Supabase/project keys, enabling production notifications to personal accounts, manipulating owner data, paid subscriptions, building/sending a purported stable APK or publishing.
- **P0 NO-GO:** missing V337 19-commit/22-path repairs; P0 Snooze/missing-channel/recovery failure; lost Creator/Personal records, duplicate rewards, invalid Challenge occurrence response, backup/restore UI resurrection, accidental account erasure, incorrect Google/YouTube identity, notification privacy leaks.
- **UX NO-GO:** unreadable 7–9sp metric labels, 320dp/2x font clipped buttons, inaccessible icon-only navigation, hidden active/error/focus states, welcome animation not reduced-motion safe, inaccurate loading/empty copy, misleading user-facing full auto-sync claim.
- **RELEASE GO** requires complete tested source, Android device evidence, intact P0 policy, founder physical signoff and explicit release approval.

**Stage verdict:** UX10 visual source can be considered **source-staged after exact draft PR verification**, but not compiled, tested, released, or complete with UX09. Integration and publishing are strictly later gates.
