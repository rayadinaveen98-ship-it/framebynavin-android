# Backlot UX07F — Cumulative Personal integration source audit

**Audited:** 2026-11-17  
**Stage/state:** UX07F static/source audit, **DOCUMENTATION ONLY**. No compilation, Gradle, JVM test, CI, emulator, instrumentation, physical Android APK, backend operation, merge or release is represented by this report.
**Audited staged head:** UX07E draft PR #415 at `08ee9ad1acb1e48cd406ee5cff8958815eb69572`.  
**UX07 stage parent:** UX06F draft PR #409 at `528f22334beb83f6c0a39199699c2ea216329b5e`.  
**Owner-accepted internal stable:** V337 P0 branch head `815ba69775f75b6e5f25bb838c2c17b4f09c203e` — protected, unchanged.

## 1. Exact GitHub ancestry and state

| Stage | Draft PR | HEAD SHA | Stage purpose | Status |
|---|---|---|---|---|
| UX06F | #409 | `528f22334beb83f6c0a39199699c2ea216329b5e` | Reminder/P0 millisecond Snooze fix and handoff | DRAFT, unmerged |
| UX07A | #410 | `d9ba477b279a94b33dd4d367cd27ce3271af4511` | Personal owner/button audit and protection contract | DRAFT, unmerged, docs |
| UX07B | #412 | `df75d09a539a7f825d0dbfaaf8891b327fd6ce69` | My Day source-owned Rhythm, safe Spaces preview | DRAFT, unmerged, source |
| UX07C | #413 | `be8f00bb86d8e9dbdf4c07bed5dbac326731fa52` | My Spaces private journals and safe owner read states | DRAFT, unmerged, source |
| UX07D | #414 | `3246059706a2cc433278249fad10700142bcaa3a` | Space and Group ID links, accurate bulk impact | DRAFT, unmerged, source |
| UX07E | #415 | `08ee9ad1acb1e48cd406ee5cff8958815eb69572` | Source-backed 7-day progress and day reflections | DRAFT, unmerged, source |

GitHub compare from #409 HEAD to #415 HEAD returned **ahead 43 / behind 0**, exact merge base #409 HEAD and **26 changed paths**: 11 production Kotlin paths (5 new pure/presentation files, 6 existing UI files), 4 added JVM test-source paths and 11 documentation paths. No Personal/Creator DataStore, Room schema, reminder scheduling service, Android manifest, export/restore codec or backend change was added during UX07A–E. The unexecuted test-source additions **do not constitute passed tests**.

### V337 P0 stable divergence — blocking final integration

Comparing **accepted stable** `815ba69775f75b6e5f25bb838c2c17b4f09c203e` to **UX07E staged head** `08ee9ad1acb1e48cd406ee5cff8958815eb69572` returned `status=diverged`, **staged branch ahead 266 commits and behind 19**, with merge base `678502f4626150ea1945b81eed1593a362377454`. Do **not** interpret ahead 266 as UX07-only changes or as proof of release readiness.

An independent compare `678502f...` → accepted P0 stable reported **19 P0 commits affecting exactly 22 paths**:

1. `.github/workflows/backlot-p0-approved-integration-candidate.yml`
2. `.github/workflows/backlot-p0-recovery-regression-isolated.yml`
3. `.github/workflows/backlot-p0-v337-combined-reliability.yml`
4. `app/src/androidTest/java/com/framebynavin/app/personal/P0ActualChallengeSnoozeDeviceTest.kt`
5. `app/src/androidTest/java/com/framebynavin/app/personal/P0AlertHealthCorrectedDeviceTest.kt`
6. `app/src/androidTest/java/com/framebynavin/app/ui/V339HomeOverflowMenuUiTest.kt`
7. `app/src/androidTest/java/com/framebynavin/app/ui/V340HomeSpacesEntryUiTest.kt`
8. `app/src/androidTest/java/com/framebynavin/app/ui/V341UnifiedBackupPanelUiTest.kt`
9. `app/src/androidTest/java/com/framebynavin/app/ui/V342UnifiedRestorePreparationUiTest.kt`
10. `app/src/androidTest/java/com/framebynavin/app/ui/V343UnifiedRecoveryEscrowUiTest.kt`
11. `app/src/main/java/com/framebynavin/app/personal/Phase1HabitAlertReadiness.kt`
12. `app/src/main/java/com/framebynavin/app/personal/Phase1HabitDeliveryLedger.kt`
13. `app/src/main/java/com/framebynavin/app/personal/Phase1HabitLiveDelivery.kt`
14. `app/src/main/java/com/framebynavin/app/personal/Phase1V2RestoreDomainForensics.kt`
15. `app/src/test/java/com/framebynavin/app/data/V352UnifiedRecoveryDomainPolicyTest.kt`
16. `app/src/test/java/com/framebynavin/app/personal/Phase1HabitDeliveryEvidencePolicyTest.kt`
17. `app/src/test/java/com/framebynavin/app/personal/Phase1HabitLiveDeliveryPolicyTest.kt`
18. `app/src/test/java/com/framebynavin/app/personal/Phase1V2RestoreDomainForensicsTest.kt`
19. `docs/qa/BACKLOT_P0_APPROVED_CORRECTIVE_INTEGRATION_CANDIDATE_2026-11-16.md`
20. `docs/qa/BACKLOT_P0_V337_COMBINED_RELIABILITY_INTEGRATION_2026-11-16.md`
21. `tools/qa/backlot_p0_actual_snooze_android15.sh`
22. `tools/qa/backlot_p0_combined_android15.sh`

**Source-specific collision:** UX06F reintroduced the P0 `Phase1HabitLiveDelivery.kt` millisecond-precision Snooze comparison, but this is **only a targeted fix**. It does **not** prove that the other 21 paths, or changes surrounding that 1 path, are safely integrated. The original P0 alert-readiness, delivery ledger, restore-forensics, tests, workflows and shell verification paths require an explicit path-by-path disposition and post-UX10 combined CI/device evidence. Do not blindly cherry-pick whole, conflicting workflows or overwrite either branch.

## 2. Feature and button preservation matrix

| Existing user surface | Canonical callback/action | UX07 implementation status and audit guard |
|---|---|---|
| Personal Home / My Day | Today↔Settings, Activity, Personal/Creator switch, Add, editor | `V2232PersonalReminderHub.kt`: original toolbar, filters, response actions, selection and page routing retained; UX07B and UX07E append read-only content. Device layout unverified |
| Personal next reminder | Done / Snooze / original PendingIntent token | `V288PersonalTodayCard` source-owner coordinator unchanged; only copy clarified |
| Existing agenda | Today / 7 days / Needs action / All; Challenge source namespace | `V337UnifiedPersonalAgenda` unchanged; one shared view, no second alarm |
| New / edit Personal reminder | Existing four-step editor, action validation, Pause/Resume/Delete | `PersonalReminderStore` and source editors unchanged by UX07 |
| My Routines | New/edit current recurring Personal reminder, links to Spaces/Groups/Challenge | Existing `V295UniversalPersonalRoutines` retained; UX07B preview filters actual recurring rather than creating new routine store |
| My Spaces index | Back, new Space, optional type/goal, owner metadata backup/merge | `V289PersonalSpacesScreen` uses UX07C Space cards but keeps same original controls and IDs |
| Space detail | Edit/Archive/Restore/More→confirmed Delete | Room owner/cascade guarded; UX07C adds source-availability read flags to prevent unsafe deletion on failed reads; device race verification open |
| Space manual journal | Log check-in, optional metric, Add Note, Show All/Less | Existing Room manual entry writer intact; notes are excluded from `progressEntries` before `V289SpaceProgressPolicy.summarize`, so note-only days do not inflate the Space's **on-screen** activity summary |
| Space photo journal | Picker / Save device copy / view/compare / Delete private copy / Recover/Discard interrupted | `V290PersonalPhotoSection` only wording/visual changes; private file sanitization, interruption rescue, owner confirmations and metadata-only backup unchanged |
| Space linked reminders | Link existing / Edit original / Unlink | UX07D adds searching and current live Personal ID + unarchived Space recheck; no copy/alarm duplication. Must test race with concurrent archive/delete |
| Reminder Groups | New/Edit/Link/Unlink/Delete/Import/Export | `V298ReminderGroupStore` unchanged. UX07D search, shared links and stale-link messaging added; missing source not falsely shown as empty |
| Group bulk actions | **Confirmed PAUSE ALL / RESUME ALL** | Original `FC13ReminderGroupCommands.setAllPaused` unchanged. UX07D reports preview counts; owner rechecks membership then changes original Personal records. Reconciliation failure may follow a successful saved write |
| Challenge manager | Create/Edit/Archive/Restore/Delete, Voice/music, alert health, Done/Undo, Strict/Flexible, Snooze, day note, links | `Phase1HabitStore` and owner coordinator unchanged. UX07E adds read-only latest day notes; original day-detail and confirmation callbacks remain |
| Personal weekly progress | Read-only genuine Personal Done receipts and challenge dates | UX07E adds 4 different source fields with unavailability indicated as em dash; does not unify or rewrite canonical records |
| Progress Journey | Month/week/day, historical Creator sources + opt-in Personal overlay | `V282ProgressJourneyUi` unchanged by UX07; Personal overlay remains OFF by default and belongs to UX08 expansion |

## 3. Verified owner boundaries and privacy

- **Personal Done** is only an accepted `PersonalCompletionJournal` receipt committed through the original exact-token Personal coordinator. Viewing/Snoozing/Dismissing an alert creates no Done.
- **Challenge confirmed day** is solely `Phase1HabitChallenge.confirmations`, each keyed to the owner-chosen 21-day **target date**. For flexible catch-up the actual confirmation `Instant` remains later than target day; UX07E retains this distinction. **Day note** is a different `dayNotes` map, not a receipt.
- **Space manual check-in/metric/note** uses original Room entities. UI filters out `NOTE` records for source-backed activity aggregates; photo viewing and linking never count. The original `V289SpaceProgressPolicy` accepts a caller-provided collection, so future callers MUST remember its input can contain notes unless filtered: add this as a test/integration note.
- **Reminder Group** stores only Personal IDs, and Group metadata export does not contain original reminders. Deleting a Group never deletes Personal reminders. Bulk pause/resume is explicitly different: it changes original Personal reminder pause states and triggers alarm reconciliation.
- **Photo** JPEG bytes remain app-private, bounded and re-encoded to strip source metadata. User-confirmed Spaces metadata JSON is plaintext and excludes photo bytes and actual reminder history. No new default cloud backup or account sync is introduced; do not infer two-device data parity.
- **Privacy:** Progress Journey Personal overlay remains off by default; UX07 screens do not deliberately publish Space titles, private photos or challenge notes to notifications/Creator analytics. Device screenshot, keyguard, diagnostics and telemetry audits remain pending.

## 4. Risks, uncovered verification gaps and severity

| ID | Severity | Finding and required disposition |
|---|---|---|
| F07-01 | P0 BLOCKER | P0 stable is diverged: reconcile exact 22-path/19-commit accepted stable delta with UX draft; preserve original ledger/restore and device tests |
| F07-02 | P0 BLOCKER | Challenge v3 codec is incompatible with older v1/v2 **writers**; synthetic upgrade, owner backup and downgrade/rollback certification must precede release |
| F07-03 | P0 BLOCKER | UX07B–E Kotlin/Compose sources have **never compiled or been instrumented** as one candidate; exact file ancestry alone cannot rule out syntax/API errors |
| F07-04 | P0 BLOCKER | Group Pause/Resume is an existing real Personal DataStore mutation. Confirm membership races and post-write Android reconciliation failure messaging on device |
| F07-05 | P0 BLOCKER | Room v2 photo/Spaces metadata owner recovery and explicit destructive preflight must pass real-device failure scenarios and P0 restore |
| F07-06 | P1 | UX07C screen-only LOADING/READY/UNAVAILABLE flags and per-Space data resets must be tested under rotating screens, IO exceptions, process death and stale responses |
| F07-07 | P1 | UX07D bounds Space linking to first 24 matching reminders. Search correctness/performance and group sharing beyond 1–5k links require TalkBack/large-library testing |
| F07-08 | P1 | UX07E seven-day counts deliberately mix Personal **confirmation time** and Challenge **target dates**, without an artificial combined score. Confirm clarity for flexible backfill and timezone crossover |
| F07-09 | P1 | UX07B Rhythm, My Spaces preview and UX07E new progress panel increase vertical height; UX10 density and device viewport audit must preserve all old controls |
| F07-10 | P1 | `V289SpaceProgressPolicy` counts whatever entries are supplied. Existing UX07C caller filters NOTE entries first, but standalone/reused callers need explicit contract coverage |
| F07-11 | P1 | Snapshot/flow recomposition at local midnight/clock-zone changes, after reads fail, and archived/linked record changes needs instrumentation. No runtime claim yet |
| F07-12 | P1 | All preview screens use foreground-private note titles and examples; verify keyguard/lockscreen and hidden Creator analytics paths have no unintended leakage |

**Do not mark these risks resolved simply because the UI code exists.** No release readiness, owner APK, or physical pass is asserted.

## 5. Evidence, downstream and current verdict

Source inspections include current `V2232PersonalReminderHub`, `V288PersonalTodayCard`, `V289PersonalSpacesScreen`, `V290PersonalPhotoSection`, `V298ReminderGroupsScreen`, `Phase1HabitSharedUi`, `PersonalCompletionJournal`, `Phase1HabitPolicy`, `V289SpaceProgressPolicy`, `FC13ReminderGroupCommands`, `V337UnifiedPersonalAgenda` and UX07B–E presentation/test source, plus Github PR/commit compares. Inspection is **static only**, not a passing compile result.

**UX07 source-development verdict:** UX07A–E are staged as separate draft PRs; UX07F evidence documentation can close the **planning/audit** substage. **Integrated shipping readiness: NOT CERTIFIED / BLOCKED** by full V337 P0 reconciliation, v3 upgrade/rollback proof and post-UX10 consolidated testing.  
**Next:** UX08 Quick Actions/Progress/Creator + Personal Achievements/Daily Brief/Content Calendar source stages, without taking on UX07 storage or unrelated cloud work.
