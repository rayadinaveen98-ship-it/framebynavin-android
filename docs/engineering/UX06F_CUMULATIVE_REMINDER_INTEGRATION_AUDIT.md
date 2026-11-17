# Backlot UX06F — Cumulative Reminder Integration and P0 Compatibility Audit

**Date:** 2026-11-17
**Status:** SOURCE-AUDITED; one narrow source compatibility fix staged. NOT COMPILED, UNIT-TESTED, CI-TESTED, INSTRUMENTED, DEVICE-TESTED, MERGED OR RELEASED.
**Parent:** UX06E draft #408, head `f04cf3ac4ee791c7beaba9a88891b1a2422ebbda`.
**Protected owner-accepted internal stable:** `stable/backlot-v337-p0-owner-accepted-20261116`, exact `815ba69775f75b6e5f25bb838c2c17b4f09c203e`.
**Strict stage rule:** no merge, no main/stable branch mutation, no APK, no hosted service or Supabase change. Integrated build/testing is deferred until UX10.

## 1. What was inspected

- GitHub PR #403 (UX06A documentation), #404 (Creator voice repeats/Snooze compatibility), #405 (Reminder Center/composer), #406 (cinematic scenes), #407 (21-day Voice/music), and #408 (companion/readiness); exact stacked ancestry.
- Current cumulative #408 source, companion/readiness, Creator and Personal live surfaces, Personal lock-state guard, Premiere vs Classic dispatch, scene preferences, challenge codec, voice foreground service and source-owned response boundaries.
- GitHub compare: UX05.1E source `d9a4ef8271c34947ce238024dc31b9413bf47870` to UX06E `f04cf3ac...` = **ahead 63 / behind 0**, 43 affected paths, confirming sequential UX06 stack ancestry.
- GitHub compare: accepted P0 stable `815ba697...` to UX06E `f04cf3ac...` = **diverged; draft ahead 219, behind 19; merge-base `678502f4626150ea1945b81eed1593a362377454`**. These are not interchangeable certified baselines.
- Comparing original merge base to accepted P0 stable identifies 22 P0-changed paths, of which `Phase1HabitLiveDelivery.kt` overlaps this UX draft; the other 21 require separate reconciliation/retention review, not automatic assumptions of equivalence.
- This is **static source/connector evidence only**. No compile, JVM, Android execution, CI or owner phone proof.

## 2. Concrete regression discovered and narrowly corrected

**UX06F-01 [P0, source fix staged / runtime unverified]** The draft UX06D edit to `app/src/main/java/com/framebynavin/app/personal/Phase1HabitLiveDelivery.kt` had reverted the P0 fix in `Phase1HabitOccurrencePolicy.current`. It compared an `Instant` containing potential sub-millisecond nanoseconds to an epoch-millisecond `Instant`, which can falsely classify a naturally fired 21-day Snooze as stale.

On this isolated UX06F branch, restore matching to `challenge.nextAlertAfter(due.minusMillis(1))?.toInstant()?.toEpochMilli() == scheduledAt`. This uses Android AlarmManager/PendingIntent time precision and matches the tested P0 stable correction. **No scheduling, store, notification, response, payload or service behavior was otherwise deliberately changed here.** Confirm this fix with the accepted P0 actual Snooze regression test in the consolidated candidate; source equivalence is not runtime certification.

**UX06F-02 [P0, open integration blocker]** The approved P0 baseline still has 19 commits not present in the draft lineage. The other P0 changes cover challenge delivery ledger, alert health, restore-domain forensics, regression tests and workflows. A single-file fix does NOT prove that the draft preserves P0 certification. A later isolated integration candidate must replay or reconcile *all* accepted stable corrections and fixtures, review expected diffs, then run the complete combined regression gate. Do not fast-forward or overwrite the accepted stable branch. No production promotion until the owner approves the integrated candidate and evidence.

## 3. Actual UX06A–E dependency and ownership matrix

| Domain/layer | Canonical writer / live system | UX06 effect | Non-negotiable invariant |
|---|---|---|---|
| Creator tasks / Project Pulse | Existing Creator TaskStore, stage gate, Alarm/Voice services | A: 1–5 repeat compatibility and honest Smart Snooze labels; B: focus rails, existing creation navigation | Do not infer Stage Done from alarm, receipt, Dismiss or voice |
| Personal reminders | Existing PersonalReminderStore and Personal alert/response activities | B: read-only Today/due/7-days/needs-action scopes; C/E: art, selected companion, Android access | Explicit Done only; Dismiss is not Done, locked alerts reveal no private title/actions |
| Current single-activity 21-day | Phase1HabitStore, occurrence policy, scheduler, ledger, local codec | B: read-only projection; D: optional repeat/gap/music; E: saved-plan Alert Health | Exactly 21 explicit confirmations; no auto-Done, no duplicate occurrences; retain receipts |
| Legacy V297 challenge | Separate historical owner/format | Not modified | No forced import, migration or rewriting |
| Ideas | Existing Idea Vault reminder owner | Not modified | Preserve original Open/Make Project/Snooze/Stop actions |
| Visual scenes | V391 preferences + V406 native Canvas and V394 opt-in Premiere | C: five appended enums; E: selected companion live art | Original seven enum identities, separate Voice/Alarm selection, Director default preserved; Premiere takes presentation priority |
| Android readiness | Existing per-domain channels/schedulers, Android capabilities | E: read-only access inspector | Permission ALLOWED is never observed posting, audio, attention or completion |
| Speech/music | Existing Creator/Personal services; Challenge Phase1HabitVoiceService | D: 1–5 Voice repeats, 5/10/15/30/60s gaps, Piano/CUSTOM/OFF | No media upload, new paid TTS, background bypass, or speech-to-Done |
| Cross-device/storage | Existing local canonical records, backup/restore limits | v3 codec + local SAF URI only | No promise custom audio survives device switch; no automatic UX09 cloud sync |

### Dispatch, precedence and privacy

`Live Creator/Personal alert → validate original domain-owned occurrence/action → is Personal keyguard locked/record invalid? → generic privacy state (no private content) → otherwise if opted-in Premiere render V394 → else render original V391 Classic or new V406 cinematic selected backdrop → optional V407 selected companion in hero if enabled → original response controls immediately actionable in eligible presentation → user-invoked read-only access inspector → original owner action/store`.

UX06B focus rails are UI projections and do not become a unified canonical ReminderStore. The 21-day Challenge still does **not** have a new full-screen alert; its Premiere view is sample-only. Do not claim the Challenge cinematic design is live.

## 4. Data compatibility and upgrade risks

**UX06F-03 [P0, release gate]:** `Phase1HabitCodec` now writes **v3** and decodes v1/v2 with OFF music, 3 announcements and 5-second gap. Older APKs intentionally reject v3; therefore rollback, local restore, export/re-import, reinstall and cross-device recovery must be exercised with preserved day confirmations, notes, archived state, links, snooze, timezone and checksum. Never silently roll back this format or erase owner records.

**UX06F-04 [P1, device gate]:** Challenge CUSTOM music persists a SAF `content://` reference, not media bytes. Revocation, moved document, imported record and new device must degrade without crash or implying successful speech. A fallback to local Piano is proposed in source; observable audio behavior still requires device evidence.

**UX06F-05 [P1, runtime gate]:** Foreground-service/TTS initialization, overlapping alerts, service generation cancellation, 1–5 repeats with long gaps, Stop/Snooze/Edit/Archive, audio focus, DND, Android FGS restrictions and watchdog behavior are unverified. The TextToSpeech initializer's dependence on an assigned engine warrants explicit fast-initialization stress testing; this is a **risk**, not a proven runtime failure.

**UX06F-06 [P1, device accessibility gate]:** 320dp / 200% text / landscape may cause horizontal compression in diagnostic action rows, voice composer chips or foreground controls. TalkBack, system reduced motion, saver mode, Scroll/keyboard, Privacy keyguard, Android notification channel and FSI eligibility must be verified before approval.

**UX06F-07 [P1, provenance gate]:** Theme preference values and companion preference are separately local. Companion OFF is default; Spark/Mira uses selected identity only, is decorative and must not imitate a three-second achievement celebration. An alert's launch and access diagnostic are not observable delivery/attention evidence.

## 5. Preserved and explicitly NOT implemented

- V337 owner-stable branch, main, signed previews, release credentials, production data and hosted environments untouched.
- No new notification, PendingIntent identity, AlarmManager scheduler, Done policy, stage advancement, auto retry, migration of unrelated stores, Supabase schema, backup/restore UI, account deletion or permission.
- No new Creator/Personal source conversion, All Reminders canonical store, end-user cloud sync, paid services, or live Challenge full-screen conversion.
- Existing personal photos/Spaces, Personal Groups, routines, My Day and current 21-day history remain authoritative and are handed intact to UX07.

## 6. Engineering gates before candidate/Play

1. Finish UX07–UX10 source stages without corrupting existing canonical owners; review all draft ancestry and each cross-stage file overlap.
2. Produce a **separate** integration plan against accepted `815ba697...` stable: all 22 P0 path changes accounted for and approved (including delivery ledger, restore forensics and tests), with the exact final source SHA, no silent reset of V337 fixes.
3. Perform one consolidated UX01–UX10 compilation, lint, JVM/unit, instrumented Android + emulator, P0 Snooze/recovery, audio/privacy/accessibility/upgrade and owner physical certification as specified in `docs/qa/UX06F_POST_UX10_CONSOLIDATED_CERTIFICATION_GATE.md`.
4. Maintain explicit block/waiver disposition for any failing test; do not call a draft source-staged branch a stable APK.
5. Separately gate Play readiness: real signing/AAB, account deletion, privacy/Data Safety, permission eligibility, Google sign-in, package migration, current API compliance and owner approval.

**Exit status for UX06F:** audit documented; one P0 source regression corrected on draft only; remaining P0 integration and runtime certification **BLOCKED / DEFERRED**, not passed.
