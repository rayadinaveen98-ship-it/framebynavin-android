# Backlot P0 founder-approved correction integration candidate
Date: 2026-11-16.

## Explicit approval and safety
Founder approved proceeding with pending Backlot work on 2026-11-16. This is the **first bounded, isolated correction candidate**; it is not authorization to claim release readiness before acceptance evidence. The owner-reference stable SHA `678502f4626150ea1945b81eed1593a362377454` stays untouched.

## Baseline
Candidate branch: `integration/backlot-v337-approved-p0-fixes-20261116` based directly on the previously verified recovery-fix PR #379 SHA `a9dc4dc4f61dc6e21182b9b482c5b1d979d88397`. This incorporates the fail-closed malformed-recovery-archive correction without merging or modifying V337. Earlier 1,713-test success applies to #379 only, not automatically to this new candidate.

## Narrow production changes
1. `Phase1HabitDeliveryEvidencePolicy.health`: use latest observed failure/start signal per exact challenge occurrence. A failure later than started reports `VOICE_UNAVAILABLE`. Later legitimate successful start can supersede earlier failure. Equal timestamps favor failure. No change to TTS scheduling, actual audio playback or Done semantics.
2. `Phase1HabitAlertReadinessInspector.inspect`: a truly missing Android 8+ notification channel is `channelAllowed=false`; not inferred from `null != IMPORTANCE_NONE`. No notification channel creation, permission mutation or OS alarm scheduling change.

## Test gates
- Complete JVM regression, no excluded JVM tests, retain full XML/report evidence.
- Four new chronology/occurrence isolation unit cases plus existing tests.
- Real Android 15 CI emulator must show missing-channel `channelAllowed=false` and attention-required state. CI app data/channel changes are limited to disposable emulator only.
- This initial Android test CI temporarily quarantines five existing V339-V343 UI instrumentation Kotlin sources (documented); therefore **it is NOT full Android instrumentation certification**. Separately reconcile these tests without resurrecting backup/restore UI.
- Full combination, device validation, legacy widget migration, actual voice/music and 21-day full-screen, signing and release gates still pending.
- Source must be retested at exact resulting SHA. Rollback reference V337 preserved.

## Unchanged
No developer account, Supabase/Neon, live cloud schema, owner device, APK release/signing, backup/restore UI, `Advanced > Erase local data`, reminder scheduler, permissions, companion selection, widgets, or saved challenge data was touched.
