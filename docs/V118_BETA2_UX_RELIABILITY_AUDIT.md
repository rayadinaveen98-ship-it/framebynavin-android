# FrameByNavin v118 — Beta 2 UX & Reliability Audit

## Purpose

Beta 2 is a stabilization milestone. It deliberately avoids adding another major intelligence system. The goal is to make the existing creator workflow simpler, more predictable, and safer before Release Candidate work.

## Product rule

- **Today** = what needs attention now.
- **Ideas** = capture and shape possibilities.
- **Create** = active and completed projects.
- **Insights** = channel intelligence and recommendations.
- Technical implementation details stay behind progressive disclosure or out of the creator UI entirely.

## Audit findings addressed in v118

### 1. Stale project expansion state
A project opened automatically in Create could leave its external expansion ID in app state after navigating away. A later manual return to Create could therefore reopen an old project unexpectedly.

**Fix:** manual bottom navigation and non-Create journey routing clear stale external project expansion state. Back-to-Today also clears it.

### 2. Idea editor overload
The Idea Vault editor exposed category, status, potential, platform, format and topic controls immediately, even when the creator only wanted to save or edit a thought.

**Fix:** title and notes remain primary. Organization metadata now lives under **Organize Idea · Optional**.

### 3. Reminder settings density
Settings exposed four permission rows at all times.

**Fix:** show one compact Project Reminders status card (`Ready` / `Needs setup`) and expand the individual permissions only on demand.

### 4. Creator-facing technical copy
Several management confirmations and Control Center descriptions still exposed implementation or overly detailed system language.

**Fix:** simplify copy around recurring schedules, Quick Capture, Daily Brief, Calendar, Automation, Creator Progress and backup/voice settings.

### 5. Bottom action accessibility mismatch
The center `+` button was correctly changed to New Project in v116, but its accessibility description still said `Capture idea`.

**Fix:** accessibility description now matches the actual New Project behavior.

### 6. Voice stop reliability
Some Android speech recognizers may delay or omit a terminal callback after Stop.

**Fix:** Voice Quick Idea now leaves the listening UI immediately on Stop while still allowing a final recognition result to arrive.

## Existing v116/v117 stabilization preserved

- Finish Project stays in Create.
- Opportunity guidance remains in Insights, not Home.
- Creator Focus is compact by default.
- Create cards are compact in multi-select mode and completed projects are compact by default.
- Project composer uses progressive disclosure and IME-safe layout.
- YouTube Reporting failures remain creator-friendly.
- Voice Quick Idea supports Telugu, English and Auto Telugu+English.
- Reminder completion safety remains intact.
- Gemini Video Autopsy remains available.

## Beta 2 validation gates

The v118 CI gate must pass:

1. unit tests
2. Android lint
3. Android test APK compilation
4. debug APK compilation
5. strict UX/reliability invariants
6. no hardcoded Firebase API key in Kotlin/Java source
7. validated staging branch creation
8. installable APK artifact

## Deferred to RC / release-hardening

These are intentionally not hidden or forgotten; they require dedicated production validation rather than being mixed into UI cleanup:

- App Check enforcement + Play Integrity real-device validation
- production signing and `debuggable=false` release verification
- production OAuth fingerprints
- Supabase authorization/RLS audit
- backup/restore and account deletion destructive-path testing
- Android 13–15 notification/exact-alarm/full-screen reminder matrix
- clean-install and upgrade/migration matrix
- real-device Telugu-English speech accuracy evaluation
- performance and long-session regression testing

## Exit criterion

v118 is complete only when CI is green, the validated staging branch points to the materialized source, and the installable APK artifact is produced.
