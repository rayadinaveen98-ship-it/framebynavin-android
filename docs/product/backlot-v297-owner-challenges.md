# Backlot V297 — owner-authored 21-day challenges

**Status: development draft, not release-approved.** V297 is stacked on V296 guided Personal composer, which itself is stacked on V295 universal Personal foundation and the verified V294 signed preview. Do not install or merge for owners without Android 15 instrumentation, code/UI QA, signed in-place upgrade and owner acceptance.

## Why
Previous hidden V285 21-day code contains hardcoded November 2026 publishing tasks, routines and start date. The universal Personal OS must provide the structure, never silently install one person's goals for everybody.

## Implemented
- New isolated `V297Challenge` model with exactly 21 activity records, chosen start date, owner-supplied challenge name, confirmed Done timestamps, and reversible archive.
- All 21 activities begin **blank**. User can edit any day at any time and see its date. Editing a completed activity resets its Done receipt rather than falsely claiming a changed activity was completed.
- A dedicated Personal Settings → My 21-Day Challenges screen with list of challenges, create dialog, day-at-a-time editor and manual Done/Undo. Progress is derived only from recorded Done. No synthetic samples or automatic scheduling.
- Private DataStore `backlot_personal_challenges_v1`, independent of existing reminders/Spaces/Creator. Atomic edits via mutex and fail-closed primary validation. Previous valid revision preserved; corrupt primary is not silently reset.
- Self-identifying JSON payload with version, max sizes, 21-day structural validation, distinct IDs and SHA-256 integrity. User-initiated **export to chosen document location** is in the UI. Decoding and explicit-approved replacement API exists in store, but **a tested in-app import/restore flow is not yet available**.
- Explicit disclosure: Challenges are local-only; neither current cloud sync nor existing Spaces JSON backup includes them, and exporting a challenge does **not** back up Space photos, reminders or other user data.
- No touched existing personal reminder codec/store, Android alarm scheduling, manifests, cloud/YouTube or package signing. The only existing user-facing files changed are the Personal Settings connection points.

## QA added
- Pure unit suite: owner date and blank state, explicit Done only, activity edit resets Done, archive preserves data, SHA-256 backup roundtrip and malformed data rejection.
- Android Compose UI: blank create, editable owner title/start date, cancel produces no plan records.
- Android 15 synthetic screenshot of blank Challenges screen. Existing V295/V296 UI and Spaces suites continue.
- Workflow boundary permits only the new isolated `V297Challenge.kt` and `V297ChallengeStore.kt` under Personal. Requires all six new tests and explicitly inherited Compose animation dependency.

## Not yet done
- Cloud backup or cloud restore for challenges; user should export before uninstall/reinstall or device move.
- **In-app import with preview/merge/conflict safeguards**; do not claim the export alone provides one-tap recovery.
- Optional reminder association and custom Reminder Groups.
- Real owner-phone acceptance, full accessibility/small-screen screenshot QA, V294→V297 signed upgrade validation, Play signing.

**Release hold:** no V297 APK distribution before all current exact-commit CI tests pass and the existing signed APK is proven to upgrade in place without destroying any personal/creator/Space records.
