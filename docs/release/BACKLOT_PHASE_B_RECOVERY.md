# Backlot Phase B — owner-authorized complete Personal recovery

**Branch:** `stabilization/phase-b-restore-transaction`; based on the verified Phase A CI head `44f815f94885a25ab80ad4e3bc9f7ff6d97818b6`. This is an unreleased draft and must not modify the owner’s installed device.

## Scope and non-negotiable acceptance

Phase B must implement actual authenticated, consented all-domain recovery after independent rollback preparation, then survive process death at every boundary. **It is NOT complete merely because a precommit check or a JVM unit suite passes.**

1. Reopen and reauthenticate the chosen V300 encrypted archive, including the actual JPEG bytes, on the same invocation that enters the exclusive transaction.
2. Reauthenticate the separately sealed V305 owner rollback escrow with the owner-supplied password; repeat an integrity-checked, truly current local snapshot under the same exclusive process lock, and fail closed if any baseline changed.
3. Show a precise coverage report: Personal reminders/Done receipts, 21-day Challenges, Reminder Groups, Spaces, links, journals and private JPEGs are *portable*. Device alarm leases, snooze tokens, device-specific media URI grants, external custom-song bytes and unexported Creator/cloud content **are not losslessly portable**. Do not promise byte-identical rollback or one-tap all-data parity.
4. Require explicit, affirmative consent to the exact proposed changes and any nonportable loss. The receipt must bind the authenticated preparation ID, escrow ID, original and incoming archive fingerprints, complete merged-target fingerprint, and photo count.
5. Apply **additive-only** target IDs by a journaled global mutation freeze; do not replace conflicting records or remove phone-only data. All canonical DataStore, Room and direct file writers must remain frozen to unrelated coroutines while a restore is pending. Install JPEGs by create-only verified writes, never overwrite a different existing file.
6. Record durable BEFORE and AFTER checkpoints for each independent store. For Room, use a transaction with foreign-key/integrity checks and real receipt provenance. For photos, block partial/extra files and require exact byte hashes. Consider device process death at any checkpoint, including after DataStore edit but before its AFTER journal.
7. On interruption, fail closed and offer owner-initiated inspection, authenticated rollback/resume with fresh consent, and independent verification. Never auto-run recovery on startup, delete a journal/escrow, or infer completion from the phase marker.
8. After final write, re-read and hash each domain and recorded private photo; rebuild alarm leases safely for this device and report any scheduling/media limitations. Only then may the owner see a verified completion, with a retained rollback copy and recovery audit record.
9. Acceptance requires exact-head full Kotlin/JVM suite, synthetic Android emulator in-place upgrade plus forced crashes between each store and at final verification, integrity and rollback on two separate disposable Android profiles, and no regression to Phase A's 33 Android fixtures. A real device is a later Phase D requirement, not proof from emulator.

## Phase B1 (this first commit): cross-evidence read-only precommit policy

`B01RestorePrecommitPolicy.inspect` matches the V304 prepared archive, V305 rollback escrow, V305 barrier, current local snapshot, authenticated incoming content, and proposed additive target by *independently recomputed* SHA-256 fingerprints. It refuses missing consent, portable-loss acknowledgment, stale baselines, mismatched identities, archive drift, conflicts and photo-count mismatch. Both the receipt and older barrier explicitly grant **no live-write authority**. This is a safety primitive, not a restore.

The phrase "authenticated incoming" in the API is a call-site contract: the future coordinator must obtain it through `V300EncryptedPersonalArchive.stageAndVerify` and retain the staged JPEG handles until the transaction is finished. Simple in-memory test fixtures of this policy are not encryption/authentication tests.

## Remaining Phase B work after B1

- [ ] B2: in-process exclusive coordinator with fail-closed journal authority and no bypass for ordinary callers
- [ ] B3: live Personal/Challenge/Group/Room/photo staged writer, additive merge, receipt checks and post-write verification
- [ ] B4: interrupted commit/recovery and independent owner-consented rollback with explicit portable-vs-local limitations
- [ ] B5: crash injection on Android 15, data preservation, alarms rebuilt safely and UI recovery consent proof
- [ ] B6: all Phase B tests green on one immutable source SHA, security and visual review, no unauthorized user data effects

**Release boundary:** This branch is a draft stacked on Phase A, not merged into stable; it does not enable a Restore button, cloud sync, Play signing or an APK distribution.
