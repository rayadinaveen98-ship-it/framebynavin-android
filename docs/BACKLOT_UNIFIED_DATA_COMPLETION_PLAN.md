# Backlot — Unified Data Completion Plan

**Owner-approved priority:** Complete **Backup → Restore All → Local Delete/Reset**, without unrelated feature additions, UI redesign or moving the release goalposts. Work against development branches only. The signed **v336 H3+I QA APK** was tested by the owner; it is a baseline, **not proof** that Restore All is production-safe.

## Fixed progress map — review at every development update

Use these exact five gates and update only when new test evidence exists.

| Gate | Current state | Pass criteria |
| --- | --- | --- |
| **01 Unified Backup** | Implemented, **verification ongoing** | One authenticated Creator + Personal ZIP; manifest/bytes verified after export; no unreported lost media or 21-day receipts; drift/corrupt-file cases fail closed; off-device re-open |
| **02 Restore All** | Internal engine, **UI disabled** | One guided owner-controlled restore from a chosen ZIP; exact preview and conflicts; Creator replacement + Personal addition behavior disclosed; realistic non-empty cross-domain device fixture passes |
| **03 Interrupted Recovery** | Internal journal/rollback machinery, **acceptance incomplete** | Crash at every pre/post write checkpoint, fresh owner forward or original decision, byte-identical retained original media, correct journal release, no duplicate alarms or auto recovery without consent |
| **04 Local Delete/Reset** | Implemented, **non-destructive emulator checks passed** | Owner two-step confirmation/acknowledgements; refuse erase with any unfinished recovery; real system wipe on *disposable* device with independent before/after checks; external ZIP/cloud account unaffected |
| **05 Final APK Acceptance** | **Pending** | 01–04 green on target Android levels and disposable hardware; all unit and device suites green; same package/version/signer verified; owner explicitly tests upgrade path; no real owner data destroyed |

### Evidence verified as of 2026-11-14

- Integrated Kotlin/JVM regression suite, personalStable preview signing and artifact gate: Actions **43483182546**, successful, commit `fe26821dc934a25f11f70ecd3898cf0c27558a49`.
- Android 15 CI emulator, non-destructive reset safeguards + **identical-target** real journaled forward restore: Actions **43492935040**, successful; `RESET-SAFETY.log` records **OK (3 tests)**.
- Test above is an initial *identical existing data* case, not proof for imported foreign records, crashes, live cloud state or actual deletion. This change expands it to non-empty owner data and real saved Done/Focus receipts; **new results are pending**.
- Unified export drift guard added in `V341UnifiedBackupManager`; signed build passed. Consistency check does **not** make Creator+Personal one atomic snapshot.

## Evidence update — 2026-11-14 (current development turn)

- **Passed:** Actions [43552052418](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43552052418): Android 15 `OK (3 tests)`, including nonempty Creator Focus, Personal reminder and exact 21-day Done receipt through a completed H4 forward restore.
- **New test committed, pending CI:** A genuinely different Personal target with absent incoming reminder and 21-day challenge must produce additions while preserving the existing reminder and exact Done receipt. Creator Focus is also absent on the destination and must be restored. Failure is a release blocker, never silently waived.
- **New reset tests committed, pending CI:** Real Android 15 system `pm clear` on a disposable emulator only *after* the safe UI/restore tests, checking that app-private sentinels and retained recovery history disappear while an external file persists. This proves Android's local system wipe lifecycle but does **not** prove the irreversible UI button invokes it; that remains for disposable device acceptance.
- **New UI test committed, pending CI:** Five separately tagged owner acknowledgements + exact typed phrase open the final review dialog. The test deliberately presses **CANCEL**, never the final erase button.
- **Current successor CI:** [43556183954](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43556183954). This run must pass before any of these new checks count as accepted.
- **Current conclusion:** Backup/restore internals compile and the previously tested nonempty *identical-target* flow passes. Incoming additions, interruption recovery, live UI execution, and irreversible deletion validation remain blocked on evidence. Restore All stays disabled.

## Execution plan — no new stages

**A. Lock backup contract.** Verify Creator v10 + Personal v2 sections, all app-owned originals (Voice Ideas, Best Frames, private Space photos), exact Done instants, nonempty challenge and Focus records. Verify no silent omission, wrong password, changed ciphertext, partial ZIP, provider write failure and mutation during export. Re-open the saved file independently. Show user meaningful exclusions: third-party video/attachments, document URI grants, custom external sounds, Google/YouTube sessions, device alarm/notification permissions.

**B. Make Restore All truly usable.** Start from an owner-selected, authenticated ZIP; show Creator replacement and Personal addition counts/conflicts before mutation. Reuse the canonical source preparation, original escrow, dual-domain reservation, forward stages, final verification and journal release. Require fresh consent at irreversible boundaries. Show unmistakable recovery state after restart; never hide retained originals or automatically discard proof. Only expose the normal button when recovery gates pass; do not remove `canCommitUnifiedRestore=false` early.

**C. Verify recovery.** Inject failures at BEFORE_CREATOR, CREATOR_WRITTEN, BEFORE_PERSONAL, PERSONAL_WRITTEN, VERIFYING_BOTH and H4 release; inspect exact stored originals and targets, media hashes and 21-day receipts. Use emulator-only disposable data. Fail closed for unsupported incomplete cases, with owner instructions, rather than claim success.

**D. Validate local delete.** Verify fresh/dirty reset preflight and typed `ERASE THIS PHONE` plus five acknowledgements, then a separately isolated destructive test on a **disposable** emulator/phone. Confirm Android erases app-private Creator, Personal, private media and retained local recovery files. Confirm external owner ZIP and remote accounts persist. **Cloud/account deletion is a separate future service contract**; never describe local reset as deleting Supabase/Google records.

**E. Rebuild and sign final candidate.** Repeat full suite, emulator tests on Android 11/15/16, owner-controlled disposable hardware acceptance, artifact hash/package/version/certificate verification. Only then label the APK as the completed combined-data owner build.

## Immutable working rules

1. No feature changes outside these three outcomes.
2. Keep the existing production/stable app and owner's saved data intact.
3. Never turn on Restore All or alarm rearming through a shortcut; do not claim green without runnable evidence.
4. Failed tests become a named blocker with exact logs and a targeted fix.
5. Every future progress response includes the five-gate map, last verified run links, **changes in that turn**, **remaining blockers**, and next concrete action.
6. The hourly Backlot Full Data Readiness automation alerts the owner only when all gates are verified and the exact signed APK is accessible.
