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
