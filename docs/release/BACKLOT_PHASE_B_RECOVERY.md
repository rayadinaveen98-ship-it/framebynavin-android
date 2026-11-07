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

## Phase B2 first emulator-backed recovery/journal seam (test-only)

The disposable Android 15 fixture `B02OwnerPrecommitDeviceTest` uses Backlot's **real** V300 encrypted ZIP exporter/validator, V304 private preparation, V305 independent encrypted rollback escrow, V301 current local snapshot, V305 barrier and B1 evidence policy. Inside the original Phase A owner-mutation mutex, it reopens the escrow and *checks archive contents against every current portable domain*, reauthenticates the selected import, computes the additive target, and creates a durable `PREPARED` journal. Afterward it proves ordinary Personal DataStore edits fail closed and confirms no reminders were changed. The suite also reruns all 33 existing Phase A Android fixtures on the new B source head.

This code exists **only in Android instrumentation** and is never a production entrypoint. It deliberately leaves a PREPARED marker on a cleared emulator; the script never runs the test against an owner's device. It is *not* the global owner-approved writer, recovery UI, rollback after power loss, or a finished Phase B.

The next engineering step must add a production-only exclusive restore authority that cannot be forged by a simple data class, plus a sealed source for in-device rollback state. The authenticated escrow currently contains portable records rather than device-local alarm leases or custom media URI grants, so "lossless rollback" remains unproven and must not be promised.

## B3 first real multi-store writer — code implemented, release blocked

`B03RestoreTransactionCoordinator` and `B03RestoreDomainWriter` are now production-source, **internal, unconnected** methods. They are not reachable through any Backlot screen, alarm, cloud sync or owner APK release. Under the exclusive Phase A mutex, they re-authenticate the actual owner-selected V300 archive and V305 encrypted escrow, compare real local inventory and fingerprints, and durably prepare a matching journal. A short-lived same-coroutine recovery permit is required by every canonical owner store, direct Space/Room writer and each private JPEG step; unauthorized concurrent writers remain blocked.

The coordinator has real stages, each with BEFORE/AFTER fsynced journal records: (1) atomic Personal reminder/Done write, (2) Challenge DataStore, (3) Group DataStore, (4) attested Room Space/links/notes/confirmed Done receipt merge in one Room transaction, (5) CREATE_NEW verified private JPEG install with Room photo record. It finally rereads all supported Personal domains and validates the complete merged-target fingerprint. Conflicts, absent encryption, orphan links, changed metadata and photo hash mismatches fail closed. It never overwrites an existing photo file. Existing local reminders are retained by the additive plan; newly imported device-specific leases are reset.

**Deliberately incomplete:** Even after `VERIFYING`, the durable journal remains active; `B03RestoreExecution.ownerRestoreFinished` remains false. This prevents a false "complete" when B4 owner-recovery/rollback after a process death, exact custom-music/snooze coverage, final retained-escrow verification and owner acknowledgement have not been implemented. **Do not invoke B3 on a real owner phone.** The disposable B03 Android 15 integration test is designed to generate an actual encrypted archive with Personal, Challenge, Group, Space/entry/link and a real JPEG, write those canonical domains and verify the existing owner reminder was preserved. The CI is not proof until the exact newest commit's full emulator and JVM suite is green.

The Phase B workflow's audited source allowlist and full compile + Phase A/B1 regression checks have been extended to include B3's permit, coordinator, Room/JPEG writers and Android fixture.


## B4 encrypted escrow reauthentication hardening (unreleased)

Cold-restart owner escrow inspection now binds the **decrypted, independently
authenticated V300 archive content** to the stored V305 escrow baseline digest,
not merely the encrypted ZIP's file checksum and metadata. A valid encrypted
archive whose portable records differ from the escrow's recorded baseline is
rejected. The existing Android 15 escrow fixture also independently fingerprints
the live synthetic snapshot and compares it with the escrow receipt before
verifying the reopened copy. This protects recovery assessment from treating a
valid but mismatched rollback archive as the owner's pre-restore state.

This is a safety improvement, **not a rollback implementation**. B4 still needs
durable owner-approved resume/rollback, verified completion and journal release;
B5/B6 still need crash-injection and exact-head full acceptance. The branch
remains draft, and the live UI must remain disabled.

## B4 independent recovery-consent evidence gate (unreleased)

`B04OwnerRecoveryConsentPolicy` now requires a fresh, explicit recovery-specific owner confirmation and separate acknowledgement of device-local alarms, snooze tokens and external media limitations. It binds the selected operation to the journal's transaction, preparation, escrow, baseline and target fingerprints and to the read-only authenticated inspection's exact durable checkpoint. Stale or substituted evidence is rejected. An unverifiable Room/JPEG inventory cannot be treated as empty. Until a crash-safe, idempotent per-store forward replay exists, forward resume is explicitly refused; a valid rollback *decision* never grants write authority, releases the journal or claims completion. Fourteen JVM negative/positive regressions are mandatory in exact-head CI. This is a consent safety seam, **not** an executable rollback, live UI or B4 completion.
