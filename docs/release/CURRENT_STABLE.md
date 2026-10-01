# Backlot Current Stable

## Stable checkpoint

**Backlot V149 / RC26 – Supabase Voice Reminder Fix** is the current stable daily-use checkpoint.

- Version code: `149`
- Binary version name: `2.0.0-rc26-supabase-voice-reminder-fix`
- Stable branch: `stable/backlot-v149`
- Manually verified application source head: `ac4b7b3ff961d7af39bcdfa3b88c3ffe17a2bed1`
- Android APK workflow run for verified source: `36799359846`
- CI result: `success`
- Manual/real-device verification: passed on October 1, 2026

## Stable scope

The V149 stable checkpoint includes the Supabase/voice-reminder reliability work and the V149 hardening pass. In particular, it preserves:

- Google/cached account first-name preference for spoken reminders, with safe fallbacks;
- `given_name` identity support;
- duplicate reminder-occurrence suppression so speech does not restart unexpectedly;
- full reminder sentence freezing before TTS starts;
- repeat timing that waits for the current utterance to complete;
- explicit manual Replay behavior;
- reminder scheduling and recovery hardening;
- secure backup/restore hardening;
- creator/settings/cloud-sync reliability coverage added during V149.

## Verification decision

The application source at `ac4b7b3ff961d7af39bcdfa3b88c3ffe17a2bed1` passed the repository Android pipeline and was manually verified on a real device. The Supabase identity and voice-reminder behavior were reported working as expected, so RC26 is promoted to the current stable checkpoint.

The binary version string remains unchanged intentionally. Changing the application version payload after manual verification would create a different build than the one that was actually tested. Stability is therefore recorded by this frozen Git checkpoint and release documentation.

## Branch policy

- `stable/backlot-v149` is the frozen rollback/reference branch for this checkpoint.
- Do not add feature work directly to the stable branch.
- `main` should represent this checkpoint after integration.
- New product work should start from the updated `main`/V149 stable baseline.

## Ongoing beta validation

The following are useful broader beta-validation areas, but they are not blockers for the V149 stable checkpoint:

- additional offline/online sync and account-switch stress coverage;
- backup/restore testing across more devices/accounts;
- YouTube Insights comparisons against YouTube Studio across additional data sets;
- onboarding usability studies with new creators.

Preserve an independent backup before installing future development candidates with irreplaceable creator data.
