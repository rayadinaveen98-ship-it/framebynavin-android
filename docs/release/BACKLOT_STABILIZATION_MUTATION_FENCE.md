# Backlot pending-recovery mutation fence — scoped closure

This is existing stabilization work, not a new app version. It blocks edits while a verified recovery is pending; it never authorizes an owner restore or deletes a journal.

## Covered

- One coroutine Mutex shared by Personal/Done, Challenges, Reminder Groups, and consistent Personal recovery snapshots
- Owner-facing Space actions and optional Personal reminder Space linking
- Direct V290PhotoJournal add/remove and V290InterruptedPhotoRecovery recover/discard/temporary-file cleanup, even outside the Space screen
- A durable private restore-journal directory blocks all covered operations after process death, even if the journal is malformed
- A short-lived same-coroutine permit makes nested Space-to-photo calls safe; other concurrent work waits and another journal root cannot borrow the permit

## Verified through tests when CI completes

Ten JVM cases cover concurrency, cancellation, journal blocking, nested Space/photo changes, mismatched roots and a journal appearing inside a transaction. Android instrumentation uses a disposable emulator with real Personal, Group and Challenge DataStores.

## Still blocked

This is **partial** protection, not global write freeze: V289SpacesMetadataRecovery and other internal direct Room DAO/file mutations, synchronous file helpers and all potential cloud/restore entrypoints require further audit and gating. The future live restore coordinator must acquire the same lock, durably journal before the first live mutation, enforce owner-authorized rollback and prove exact data and JPEG integrity. No APK or Play release is justified solely by these tests.

## Direct Room metadata import/export

`V289SpacesMetadataRecovery` now takes an explicit app Context and acquires
the same persistent gate for both export snapshots and merge-only Room imports.
The existing owner-facing UI and Android tests pass that Context. This prevents
direct internal callers from bypassing the owner-facing Space-screen lock.
A fourth Room metadata emulator case checks that a pending recovery journal
blocks both direct import and export without changing the in-memory database.
The Android mutation-fence CI requires all five device tests in this fixture.

This still does not intercept every raw Room DAO call; the global freeze and
restoration transaction remain release blockers.

## Personal settings and authenticated archive snapshot

Personal follow-up reminder settings are separately stored in DataStore; their
enabled and delay mutations now use the same recovery gate. The V300 encrypted
full-Personal archive export now holds a stable snapshot lock while reading
reminders, Done history, Groups, Challenges, Room and private JPEG bytes, rather
than collecting a potentially torn archive during concurrent cooperating writes.

Two extra emulator cases verify that follow-up preferences are unchanged
and an encrypted export writes no bytes while a recovery journal exists.
The Android 15 CI checks eight tests across the Personal and Room fixture
classes. These safeguards are still not a live crash-safe all-domain restore.

## Combined Phase A test gates

The exact integration head must run all existing recovery safety tests again:
12 durable journal JVM cases, 12 photo installer JVM cases, 10 nested-lock
JVM cases, three Android 15 journal/JPEG/interrupted-real-DataStore cases and
eight Android 15 Personal/FollowUp/Room mutation-gate cases. Each emulator
cluster runs on its own **cleared disposable emulator app data** and never on
an owner's device. No test success from an earlier branch substitutes for
the integrated exact-head run.

This is only an owner-data safety prerequisite. No live cross-store restore
or rollback is enabled, and Phase B requires a fully audited global freeze
for actual restore initiation plus crash-safe final verification.

## Repository-level Space write fence

V289SpacesRepository now requires an explicit application Context and protects
create, edit, archive, unarchive, owner-confirmed Space delete, reminder link
and manual entry writes with the persistent mutation gate. Callers in the main
UI, V310 link coordinator and existing Android device tests were migrated.
The repository no longer offers a raw DAO-only constructor that would
silently bypass the freeze.

The integrated emulator gate runs three independent clusters on a cleared,
disposable Android 15 emulator: 3 crash/JPEG/restart tests, 8 new Personal,
encrypted-export and Room mutation-fence tests, and 21 existing Space
database/photo recovery/UI tests. **33 tests total** are required on this
exact commit before Phase A can be considered verified. Existing UI behavior
must not regress, and no device/user data is touched.

A fully journaled live restore writer, escrow-bound rollback, and successful
cloud/full alarm migration remain later phases.

## Legacy import preflight race closed

The older Personal-only restore now runs the linked Space/Group orphan
validation and its canonical DataStore replacement under **one** owner
mutation gate, preventing a newly created link from appearing after the
preflight but before replacement. The nested canonical Personal store
mutation uses the same scoped permit and still retains its independent
rollback copy before a write. A separate cleared-emulator orphan-reference
regression is required, along with the existing Space metric UI case.

Exact-head Android requirement: 3 journal/JPEG/interruption + 8 protected
Personal/Room/settings cases + 21 existing Space/metric/Compose cases +
1 legacy Personal restore linked-reference case = **33 Android 15 tests**.

Phase A safety prerequisites can be signed off only after the integrated
exact-head test suite is green. Phase B still must implement and test the
actual crash-safe, owner-authorized multi-store restore transaction.
