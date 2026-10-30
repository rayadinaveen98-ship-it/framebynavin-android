# Backlot V196.4 — Content Workspace Phase 3B: Script → Publish

Parent: `feature/backlot-v196-3-plan-script-handoff` (tested V196.3). Implementation branch: `feature/backlot-v196-4-script-publish-handoff`. Preview versionName: `2.5.7-v196.4-script-publish-preview` (versionCode 207). Frozen V195.5 stable branch must remain untouched.

## User behavior
- Script Studio bottom bar: **SAVE** retains previous behavior; **SAVE & PUBLISH** writes the complete structured Script draft into the canonical project transaction, checks workspace + studio revisions and backup generation, clears its matching recovery draft only on successful persistence, then opens Publish Studio. Save failure does not navigate or overwrite previously saved data.
- Publish Studio now reads an **explicitly selected** Script title from canonical `workspace.scriptStudio.titles`; no arbitrary title proposal from the first unselected idea.
- On one selected *unpublished* deliverable, creator may tap **USE SCRIPT TITLE**, review destination, new title and current title in a confirmation dialog, then **USE TITLE**. This edits only that one output's title in the current Publish editor draft. The creator still taps **SAVE PUBLISH DETAILS** for durable persistence.
- Guard blocks any title transfer for an output with PUBLISHED status, prior `publishedAtMillis`, publication URL or *any* publication history, including REOPENED outputs.
- No automatic release, checklist changes, workflow completion, export, cross-output changes or Script mutations. Required rights/music/source checks remain intact.

## Automated tests
- `CreatorScriptPublishBridgeTest`: explicitly selected nonblank titles only; trimming; preserves description/tags/cover/status/checks/history; same-title avoidance; published/reopened/legacy/url-only safety; one-output isolation.
- `V196ScriptPublishHandoffUiTest`: save-and-open Publish invokes canonical Script save callback with expected revisions; per-output title confirmation, draft-only then explicit save; protected published outputs do not show action.
- Inherited Phase 1 safety, Phase 2 UI, Phase 3A plan-to-script source checks, all JVM unit tests, lint, candidate APK assembly, Android instrumentation build and automated emulator tests.
- CI workflow `.github/workflows/backlot-v196-4-script-publish-handoff.yml` uses versionCode 207 and candidate artifact `Backlot-V196-P3B-Preview-APK`.

## Device QA before stable
1. Open Script and select a title, edit beat/narration, tap SAVE & PUBLISH. Verify no content loss and Publish opens only after successful script write. Without selected title, no USE SCRIPT TITLE button.
2. Plan 2+ platform outputs. Select output A; click title handoff and confirm. Ensure output B and all script alternatives unchanged.
3. Existing output A title is never silently overwritten: confirmation includes old/new. Cancel leaves all data unchanged.
4. Tap USE TITLE but close without SAVE PUBLISH DETAILS: discard leaves canonical title unchanged. Reopen/recover draft according to normal behavior.
5. After SAVE PUBLISH DETAILS: reopen and confirm title persisted, original metadata intact, and no publication records generated.
6. With published/reopened/history-only outputs, title handoff cannot write. Rights/source gate remains required and cannot be skipped.
7. Simulate revision/generation conflict or process death during Script save. No navigation on error. Restore draft from valid revision only.
8. Verify small screens, long titles, accessibility font scale, rotation, resumed activity; verify unrelated reminders and projects unaffected.
9. Install in place from V195.5, V196, V196.3 with matching signature; never uninstall to bypass signature mismatch.

Do not merge to stable or call stable until green CI and physical device sign-off. Next: Phase 3C references/assets and Phase 4 integration QA.
