# Backlot V207-C — Three-screen creator setup

Preview versionCode **228**, `2.7.8-v207-first-run-preview`.
Based on V207 A/B fullscreen + compact project voice. Draft only.

## Changes
- First-run creator setup reduced from five pages to three:
  1. **What do you create?** — choose one primary creator mode.
  2. **Where do you publish?** — choose one or more platforms.
  3. **You're ready to create** — review choices and a visible suggested focus.
- Required `CreatorProfile.isComplete` fields remain valid. Where no goal has been chosen, Backlot visibly shows and saves a reversible default **Stay organized**. The primary mode, platforms and default are displayed before Start Backlot.
- Existing creator secondary modes, production styles, secondary goals, weekly targets, profile display name are preserved through `profile.copy()` rather than cleared.
- Advanced permissions (alarms, notifications, battery, fullscreen) are not a first-run gate; they stay accessible when actually setting up a reminder and in Settings.
- Account sign-in remains optional. Returning creator cloud sync and conflict resolution are NOT replaced or automatically bypassed.
- Original V197 navigation layout and legacy policy remain valid for historical test compatibility; the V207 setup calls it with `lastPage=2`.
- Larger 12sp+ option labels, 46dp chips, save-failure retry and saved progress. No unrequested automatic permission prompts from this component.

## Tests
- JVM `V207FirstRunPolicyTest`: 3-step validation, complete profile default, enrichment preservation, missing field rejection.
- Emulator `V207FirstRunUiTest`: wizard happy path, final saved profile, disabled invalid steps, Back navigation, no permission wall.
- Inherited V207 A/B emulator UI tests and V200-V206 branding/notification regressions.
- Full source gate `tools/check_v207_first_run.py`, Android unit/lint/instrumentation and signer/APK resource checks on exact commit.
- Real device: first installation with no account, Google sign-in and return, offline path, denied permissions, small display/high font, Back and retry, app restart during setup, no resetting upgrades, cloud-data conflict prompts preserved, first actual reminder asks appropriate permissions then fires.
- Stable promotion requires green CI, real device acceptance and creator approval.

## Subsequent work (not part of this PR)
V207-D contextual tour with measured spotlight bounds; V207-E revenue data coverage + simplified display; V207-F integrated stability candidate.
