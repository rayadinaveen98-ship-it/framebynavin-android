# Backlot V153 — Release and usability acceptance checklist

**Status:** implementation checkpoint; **not production accepted** until physical-device verification.

## P0 — Source inventory and language rules
- [x] Source route inventory and glossary: `V153_SIMPLE_CREATOR_LANGUAGE.md`
- [ ] Reconcile actual UI screenshots with source route inventory
- [ ] Inventory generated speech, notification and backend recommendation strings beyond first-pass source review

## P1 — Remove technical/obsolete text
- [x] Correct Spark copy in Settings
- [x] Remove RC/ALPHA editor headings from active workspace views
- [x] Add language smoke test in `tools/check_v153_copy.sh`
- [ ] Recheck live screens for remaining internal naming and hidden developer labels

## P2 — Home and onboarding
- [x] Navigation label `Create` -> `Projects`
- [x] Quick Actions and shorter setup/tour wording
- [ ] Real-device test: fresh install, restart, account link, repeated launches, return to Home, no forced setup

## P3 — New project and ideas
- [x] Plain language for Ideas and Quick Capture
- [x] Fast default Create Project path with project name, platform, format, due date and reminder controls
- [x] More Options retains original 7-step advanced wizard and project settings
- [x] Shared create eligibility policy and unit tests for missing fields, expired due dates, invalid reminder setup
- [ ] Device test: quick save and advanced save create same correct project metadata
- [ ] Device test: platform switch resets available formats; due date/reminders/voice/notifications are preserved
- [ ] Device test: larger text, keyboard and landscape do not hide Create button

## P4 — Project workspace
- [x] Copy pass on Plan, Script, Prepare to Publish, current step and save labels
- [ ] Confirm editor save/revision conflict, publication history and per-post status work unchanged
- [ ] Confirm UI wording does not imply upload to YouTube or automatic external publishing

## P5 — Reminders/automation/calendar
- [x] Plain labels for reminders, weekly plans and voice
- [x] Preserve all five ProjectAttentionPlan values internally
- [ ] Test reminders: OFF, GUIDED, LIGHT, URGENT, CUSTOM and custom time validation
- [ ] Test permission denied, alarm voice fallback, snooze and date changes

## P6 — Insights and AI explanations
- [x] Replace developer-first titles and lengthy evidence caveats
- [ ] Verify top videos, date windows and graph labelling match source data
- [ ] Check estimated earnings and RPM caveats remain readable
- [ ] Confirm AI uncertainty and missing data never shown as proven fact

## P7 — Settings/backup/recovery
- [x] Simplify copy while retaining warnings about data replacement
- [ ] Test no account, connected account, phone/cloud conflict, manual export, recovery and restore
- [ ] Confirm there is no silent overwrite and no public-sharing implication

## P8 — Accessibility and release QA
- [x] Increase key small text in quick wizard, bottom navigation and settings
- [ ] Screenshot review at 100%, 130% and 200% Android font scale
- [ ] Screen reader labels, contrast and minimum touch-target audit
- [ ] Cold-start and return-to-app smoke tests on Android
- [ ] Run unit tests, lint, instrumentation-test compile and APK assemble
- [ ] Install and test V153 over working V152 without losing ideas, projects, reminders or settings
- [ ] Verify release APK metadata, hash, artifact, reproducible build and user acceptance

**Important:** Static copy checks and successful CI are necessary but not enough to claim V153 is fully ready. Do not merge to stable or call a build final until full on-device acceptance.
