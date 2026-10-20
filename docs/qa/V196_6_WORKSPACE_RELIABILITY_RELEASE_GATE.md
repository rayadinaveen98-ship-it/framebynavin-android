# Backlot V196.6 — Phase 4 Workspace Reliability / Pre-Release QA

**Branch:** `feature/backlot-v196-6-workspace-reliability-qa`
**Parent:** `feature/backlot-v196-5-research-asset-opening`.
**Candidate:** `2.5.9-v196.6-workspace-reliability-preview`, versionCode **209**.
**Stable:** V195.5 `stable/backlot-v195-5` remains untouched.

## What Phase 4 changes in production
1. Introduce an activity-scoped atomic **single-flight save gate**. Rapid taps on Save, Save & Script, or Save & Publish cannot enqueue competing writes against the same stale workspace revision. The gate releases in `finally` on both normal completion and error; unrelated project operations remain untouched.
2. Move the generation read into the guarded transaction attempt so a synchronous pre-launch error cannot leave the save gate locked.
3. Treat *post-commit* sidecar publication reconciliation and old draft cleanup as retryable. These operations are logged if they fail; an already persisted canonical project update is not incorrectly shown as a failed save. The source of truth remains TaskStore. Pending recovery can be retried on future load.
4. Preserve all existing editor callbacks, rights/music/source publication gates, state restoration, data fields and installation identity.

## Automated suite coverage
| Area | Verification | Status / trigger |
|---|---|---|
| Historic app contracts | V161–V195.5 + reminders, Insights, source rules | Workflow |
| Phase 1 | Rights gate, linked published deletion, progress authority | Workflow |
| Phase 2 | Simplified Hub, Plan, Script, Publish, Guide UI | Workflow |
| Phase 3A | Plan → Script save-before-navigation + saved brief | Workflow |
| Phase 3B | Explicit Script → Publish confirmation + history isolation | Workflow |
| Phase 3C | HTTP(S)/SAF link validation + Sources/Assets UI tests | Workflow |
| Phase 4 single flight | Duplicate submissions, simultaneous taps, failure retry | `CreatorWorkspaceSaveGateTest` |
| Phase 4 lifecycle | Plan → Script → Publish; publish gate; derivative isolation; script preservation; prevention of historical deletion | `CreatorWorkspaceEndToEndTest` |
| Phase 4 on-device persistence | 12 sources + 24 asset links + 30 script beats + publication history + reminder metadata JSON round trip | `V196WorkspaceDurabilityAndroidTest` |
| Draft restoration | Plan, Script and Publish drafts accepted only at exact revisions, clear without deleting canonical project | `V196WorkspaceDurabilityAndroidTest` |
| Android regression | Lint, unit tests, compiled APK and Android 11 emulator acceptance | Workflow |

## Mandatory manual/device QA matrix (not replaceable by green CI)
- **Upgrade, never reinstall:** V195.5 → V196.6 or earlier V196 preview → V196.6 in place, same package/certificate; preserve real reminders, content projects, sources, scripts, linked assets and publication history.
- **New project:** create a cinematic-analysis project; set audience, problem, promise, angle and sources; save to Script; write multiple hooks, titles and beats; save to Publish; use selected title on one output only, confirm; save Publish; reopen and confirm the other output and script unchanged.
- **No accidental publication:** return from Publish without pressing confirm; no change to publication history/status, rights gate not skipped. Publishing requires all mandatory checks including rights/source/music.
- **Published/derived graph:** publish parent and derivative; reopen one; attempt to replace title or delete its ancestor. Confirm protections and event history.
- **Rapid taps:** tap Save/Save & Next multiple times rapidly across Plan, Script, Publish; only one durable save, no false stale-revision banner, no duplicate deliverables/events.
- **Crash/draft recovery:** edit each workspace, simulate process death and resume; recover only drafts based on the current revision. Switch to restored backup generation and ensure old drafts cannot override newer canonical edits.
- **Attachments:** attach PDF/image/video/audio via Android document picker, reopen in viewer after app restart, reboot, revoke permission and confirm error rather than crash. Unsupported schemes are not actionable.
- **Large content:** 20+ outputs, 50+ script beats, 30+ reference URLs and assets; scroll / edit / reorder / revisit without clipping, lag or lost edits.
- **Layout accessibility:** smallest phone viewport, large accessibility fonts (up to 200%), long multilingual titles, keyboard open, expanded disclosures; no obscured action buttons.
- **Reminders, unrelated features:** voice/alarm/manual reveal, progress, settings backup/restore, Insights channel switching, navigation and animation still work.
- **Performance:** manual warm/cold screen open, Script with large beats, back navigation, repeated project switching; collect logs and document perceived delays.
- **Failure modes:** no browser installed, missing file viewer, stale provider permission, low disk or interrupted save, failed publication sidecar, backup restore collision.

## Release checklist
- [ ] All Phase 4 source/Gradle/Android instrumentation steps passed for exact commit and APK artifact.
- [ ] QA matrix executed on creator device without user-data loss.
- [ ] In-place signing identity tested; no uninstall required.
- [ ] No new severe crashes, regressions, unrecorded publication changes or lost edits.
- [ ] Creator approves promotion from preview to a new stable branch/tag.

**Do not mark this as stable merely because the CI workflow creates an APK.** Physical device checks remain a separate required gate. The preceding frozen stable branch must not be rebased, force pushed or overwritten.
