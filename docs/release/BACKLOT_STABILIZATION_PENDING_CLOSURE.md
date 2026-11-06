# Backlot Stabilization — Existing Pending Work Only

**Owner instruction, 6 Nov 2026:** Pause new version/feature work until current implementation is tested, recoverable and prepared for a stable release. This branch is a release-stabilization track, NOT a new numbered feature version, Play-ready binary, or permission to wipe user data.

## Source baseline

This branch is stacked on `feature/backlot-v310-personal-composer-links` (including previously tested V306–V308.1). It does not change installed signing, app versionCode, reminder storage, Personal DataStore, Room schema, cloud sync, speech/audio, or the V309 frozen owner candidate. **Do not merge to main or ship an APK until consolidation and owner device acceptance.**

## Completed automated evidence

- V306 Activity Center: [run 42187452479](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/42187452479), success
- V307 compact Creator: [run 42187734039](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/42187734039), success
- V308 Spark multi-item preview: [run 42187788530](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/42187788530), success
- V308.1 individual hide: [run 42190109386](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/42190109386), success
- V309 signed V305→V309 upgrade: [run 42188874206](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/42188874206), success
- Direct V294→V309 signed upgrade: [run 42190654561](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/42190654561), success

These prove Android emulator and source-specific behavior, not physical-phone or cloud parity.

## Visual QA corrective finding

The V307 Android 15 screenshot `01-creator-clean-empty.png` showed **three large cards with 0 NEEDS / 0 TODAY / 0 DELAYED**, a search field and multiple empty filters, despite having no reminders. This is excessive visual weight in the empty state.

This branch keeps the original Activity Center action in the toolbar and a standalone History link, but collapses the redundant counts and search/filters while there are no active reminders. Valid reminders still show the full status, search and filtering controls; only active reminders trigger the delivery-permission warning. The empty-state test asserts retained History and Activity Center access and captures an Android 15 screenshot.

## Recovery target planning — additional safety work in this stabilization branch

The separate `StabilizationRestoreTargetPolicy` computes a **non-writing additive merge target** for the existing validated Personal archive domains. It preserves every original phone record and active lease, appends missing imported records, verifies Space/Group reminder references and exact Done receipts, normalizes nonportable incoming alarm tokens/music URIs, and rejects duplicate destination photo paths across distinct photo IDs. It returns only a comparison/digest and has `authorizedToWriteLive = false`.

Nine JVM tests exercise eight-domain additions, local-only preservation, portable same-ID idempotency, unsafe lease stripping, ID collision rejection, unverified local integrity, photo-path collision and unchanged Group membership. This is a prerequisite to safely implementing a journaled commit/rollback flow, **not** a complete restore. The actual transaction writer and crash recovery are still missing. Never expose a Restore button based on this pure target planner alone.

## Personal cloud/manual restore — linked-reference integrity fix

Backlot's existing Personal-only cloud/manual restore replaces Personal reminder IDs and Done receipts but leaves locally stored Reminder Groups and Room Space links untouched. Previously, a cloud copy missing a locally referenced reminder could strand those references.

Now `PersonalLinkedReferenceGuard` reads current Groups and Space link IDs without creating or updating Room tables, and refuses replacement if any referenced reminder ID would disappear. The selected cloud copy, existing reminders, Group memberships and Space links remain unchanged on this rejection. Cloud Sync reports a generic, privacy-preserving actionable explanation rather than silently claiming success.

This safeguard has six pure policy tests and a separate Android emulator test that creates **real** Group, Space and reminder records, rejects an incompatible Personal cloud copy, then permits an explicitly selected same-ID replacement without losing links. It is not all-domain sync and does not transfer media or Spaces to another device.

## Still blocking stable release

1. V309 **populated** Activity Center screenshot evidence and V310 optional Space/Group Android tests, currently running in their own QA branches.
2. Signed *integrated* owner candidate containing accepted V308.1 + V310 and this empty-state patch; no signed candidate yet for the combined branch.
3. Live full-domain Personal restore across independent DataStores/Room/photos: V300–V305 verify encrypted copies, conflict previews, prepared photo bytes and rollback escrow but do **not** commit or roll back live storage. Never imply those files alone restore a changed phone.
4. One-tap all-domain cloud sync and verified new-phone restore, including Creator, Personal, Spaces, notes, JPEGs, Groups, Challenges, and explicit handling of audio/media references.
5. On-phone alarm/voice three-cycle playback, full-screen locked alerts, native notification settings, snooze, background music/custom URI, Doze and auto-retry, on the exact candidate signed APK.
6. Acceptance of navigation, responsive layout/large fonts, Spaces/Photo Journal, widgets/calendar, creator content and screen-reader semantics.
7. Private Play production-signing identity and safe migration from public preview signer ([issue #180](https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/180)), production AAB, policies, privacy/Data Safety and internal testing.

## Release invariants

No uninstall or `pm clear` on the owner's phone; no re-sign of sideload APK with Play identity as an assumed update; no fabricated delivery/read/Done receipts; no automatic restore or deletion; no unsupported data migration; no claim of Play readiness from emulator-only tests.
