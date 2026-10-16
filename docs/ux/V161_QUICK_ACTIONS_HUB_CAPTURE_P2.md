# Backlot V161 — Quick Actions Phase 2: Hub & Quick Capture

**Source:** V160 Quick Actions Phase 1 (`c9525b4b48ed87f2fdfaca762864e6bfba7b3dd6`)  
**Release state:** Preview; draft PR only, no merge without creator acceptance.

## Locked scope
Phase 2 modernizes only the Quick Actions entry sheet and Quick Capture interface. Preserve every pre-existing destination (New Project, Trending Moment, Daily Brief, Calendar, Idea Vault, Weekly Plan, Reminders, Automation, Creator Progress and Settings), all data contracts, reminder scheduling and creator preferences.

### Hub
- Two first-use choices: New Project and Quick Capture, both with readable minimum touch targets.
- Clear sections: **Your Work** (Daily Brief / Idea Vault), **Plan & Organize** (Content Calendar / Weekly Plan / Automation), **More Tools** (Trending Moment / Reminders), **Your Growth** (Creator Progress), with Settings still accessible.
- Overdue projects and their existing `+30 min`, `New time`, `Skip` callbacks stay in the hub; sorted by due time with no new mutation behavior.
- Context: ready-idea count, weekly auto-plan state, creator level/XP.
- Stable Compose test tags for all eleven existing destinations; no routing or controller rewrite.

### Quick Capture
- Show idea text input and **Save to Idea Vault** first.
- Dictation ("Speak text"), original audio recording ("Record voice"), extra notes and auto-suggested organization are available **on demand**.
- Preserve underlying `V117VoiceIdeaInput`, `VoiceIdeaRecorderInput`, `CreatorQuickCaptureEngine`, audio lifecycle, original storage paths and idea model.
- Voice-only captures still get a readable timestamp fallback title.
- Save callback returns accepted/rejected; rejected saves show feedback and keep text/audio. Audio ownership is marked before calling an accepted save handler, since the handler may immediately close the modal.
- On accepted save show "Saved to Idea Vault" and keep existing IDEAS routing and guided-tour behavior. No invented remote or AI evaluation.
- Voice audio portability and Weekly Plan data-safety fixes from V160 remain untouched.

## Automated checks
- V154–V160 smoke preservation check plus Phase 2 existence/routing guarantees.
- Full JVM unit tests, lint, instrumentation compilation, preview APK build and artifact upload.
- Compose device tests for all hub actions, optional fields, rejected save/retry and text note capture; test source compiled even if emulator itself crashes.
- Android emulator tests are not substituted for physical-device confirmation.

## Acceptance checklist
- [ ] Tap all 11 old destinations in Quick Actions; none is broken or missing.
- [ ] New Project still enters the full creator wizard; Trending Moment retains its previous flow.
- [ ] Existing overdue `+30 min`, `New time`, `Skip` actions still work.
- [ ] Quick Capture opens with an immediately understandable idea field and Save; optional controls start collapsed.
- [ ] Dictate a text idea, record an original voice-only idea, and create a text+recording idea; confirm playback in Idea Vault.
- [ ] Capture with optional notes and show/hide suggestions; saved category/platform hints are retained.
- [ ] Reject a save in tests: text and original audio must remain until successful save/discard.
- [ ] Success shows confirmation and routes to Idea Vault except where the guided onboarding journey intentionally overrides.
- [ ] Use compact screen, 200% font, TalkBack and keyboard; all controls must remain scroll-reachable.
- [ ] Upgrade over V160 with same debug/prototype signing identity; compare projects, ideas, recorded voice audio, weekly schedule, automation toggles and reminders.
- [ ] Verify no accidental changes to Reminders and Settings.

## Known limitations
- V160 Android 15 full instrumentation suite previously crashed before completion; this was not a failed app data assertion. V161 carries forward all safeguards but does not claim to resolve that runner issue.
- Quick Capture success reflects ViewModel acceptance. Persist errors occurring later are still handled by the global Backlot write-error recovery UI.
- Similar-idea detection, trash/undo and Idea Vault editor unification belong to Phase 3, not Phase 2.
- This phase is a usability pass, not a new background automation or notification engine.
