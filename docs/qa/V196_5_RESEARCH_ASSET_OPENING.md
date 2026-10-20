# Backlot V196.5 — Phase 3C: Open Research & Assets

Parent: `feature/backlot-v196-4-script-publish-handoff` (Phase 3B). Candidate: `feature/backlot-v196-5-research-asset-opening`. Preview versionCode **208**; versionName `2.5.8-v196.5-research-assets-preview`. Frozen V195.5 stable and `main` remain untouched.

## Behavior
- **Plan → Sources** shows **OPEN** for valid HTTP or HTTPS URLs only. The action uses Android `ACTION_VIEW` and `CATEGORY_BROWSABLE`, and launches only on explicit user taps.
- **Plan → Assets** shows **OPEN** for content:// SAF provider URIs or valid HTTP(S) asset links. Content documents use `ACTION_VIEW` with provider MIME if known plus *read-only* `FLAG_GRANT_READ_URI_PERMISSION`.
- External viewers are selected by Android; no embedded webview, remote script execution, media copying, automatic network fetch, file upload or changed storage format.
- Only HTTPS/HTTP with a real host and no embedded username/password can open in a browser. Never send `file://`, raw storage paths, `intent://`, `javascript:`, `data:`, missing-host URLs or arbitrary custom schemes via implicit intents.
- No OPEN control appears for unsupported values. A missing target app, revoked permission or malformed URI returns a short inline error in the currently selected section. Tab changes and edits clear old errors.
- Previously granted persistable SAF permissions remain in place; external apps receive only a temporary read grant. The existing picker still attaches by URI, not by copying file contents, and gracefully handles failed MIME lookup.
- Edit, remove, save, and recovery flows still use the existing `CreatorContentWorkspace`, revision/generation protections and draft journals. No change to the publication rights checks, script handoff or reminders.

## Automated acceptance
- `CreatorWorkspaceLinkRulesTest`: valid web links including whitespace trimming and case-insensitive schemes; invalid schemes, missing host, credentials, raw paths and control characters; allowed content URIs for files only.
- `V196ResearchAssetUiTest`: valid Source and valid content Asset show OPEN in the relevant Plan tab; disallowed Source and raw path Asset do not.
- Inherited V161–V195.5, V196 P1, V196.2, V196.3 and V196.4 source guards, unit tests, lint, APK builds and Android emulator instrumented suites.
- Workflow: `.github/workflows/backlot-v196-5-research-assets.yml`; artifact `Backlot-V196-P3C-Preview-APK`.

## Physical-device QA
1. Add a web source with URL; tap OPEN and check the browser opens the exact link. Return to Plan without losing edits.
2. Attach image, PDF, audio and video through SAF. Close/reopen Backlot and tap OPEN; verify external viewer and return behavior, including after device reboot.
3. Try an unsupported URL or `file://` legacy asset. No OPEN action; no crash.
4. Uninstall an external viewer or revoke provider permission. Confirm an inline error instead of app crash.
5. On low connectivity, OPEN launches a browser but Backlot never claims the remote URL was fetched.
6. Verify multiple sources, many files, long names, font scaling, navigation and scrolling.
7. Edit a source label or URL, tap OPEN, and discard edits; only the canonical saved copy persists according to ordinary Plan save/discard behavior.
8. Test an in-place upgrade from the earlier preview while preserving local projects, reminders, publication history and SAF permissions.
9. Verify Phase 3A Plan → Script and Phase 3B Script → Publish regressions.

## Acceptance boundary
This is a test candidate, not a stable release until full CI is green plus on-device testing. Next: Phase 4 end-to-end reliability and release certification.
