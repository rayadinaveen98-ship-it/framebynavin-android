# Backlot — UX05.1 Advanced Creator Workspace: Integration Handoff to UX06

**Handoff date:** 2026-11-17  
**Owner approval:** UX05.1 advancement plan approved; implementation staged sequentially after UX05.  
**Stage result:** UX05.1A–E **SOURCE-STAGED ONLY**. Neither UX05.1 nor the cumulative UX01–UX05.1 code has been compiled, run, merged, certified or released as part of this series.  
**Protected baseline:** V337 owner-tested stable branch unchanged.

## 1. Product readiness at source level

The planned Creator Workspace now comprises:

| Package | Deliverable in draft source | Protection |
|---|---|---|
| UX05.1A (#398) | Save & Open, reviewed-content freshness, evidence source/asset links | no navigation before successful canonical write |
| UX05.1B (#399) | stage-aware Cockpit hints and a compact editable expanded Brief | additive, journaled optional fields; no parallel brief record |
| UX05.1C (#400) | read-only connected Creative Board, contextual creator modules, editable/orderable stage evidence and script-beat links | authoritative editor links; no copied media; stable IDs |
| UX05.1D (#401) | target-specific edit/publication checks and append-only per-output learning | unrelated published derivative cannot approve main output |
| UX05.1E (this draft) | local revision/epoch-bound learning draft recovery; guarded save and discard; integration/sync contract and QA gate | no remote writes, no migration, no extra release permissions |

The source model is a meaningful advancement, but **not a certified strongest-in-market or production-ready workspace**. That judgment requires functional, physical, usability and competitor acceptance after the final cumulative UX10 campaign.

## 2. UX05.1E integration findings and changes

**Critical gap addressed in source:** UX05.1D new Output Learning Desk had only `rememberSaveable`, which cannot promise durable recovery of an unfinished reflection after an Android process death. UX05.1E introduced `CreatorOutputLearningDraftStore`, a separate local journal scoped to exactly the project ID, output ID, workspace revision and CreatorDataGate restore generation. It is **not** a canonical project writer.

**Navigational correction:** system Back and the visible Back arrow initiate the Activity's controlled discard flow. Switching outputs or opening Publish while a reflection is unsaved requires explicit discard. A failed or stale journal clear must not navigate with a false discard claim.

**Save-flight protection:** editor text and interactive switching are disabled while a learning save is active. Duplicate Save attempts are blocked by the shared `CreatorWorkspaceSaveGate`. On confirmed canonical save, the previous revision's local journal is cleaned; cleanup failure logs a deferred warning and the old journal is not loaded into the new revision.

**Source review only:** inspected exact relevant files and aligned code paths. These remain unverified until compilation and Android runtime execution; do not treat the source edits as a passed race-condition test.

## 3. Integration boundaries

**Canonical data:** `TaskStore` CreatorTask.workspace and its per-project revision; explicit restore generation fence. Each editor must write only fields it owns and reattach concurrent authoritative histories.

**Preserve:** original project and workflow IDs, saved stage evidence/decisions, script beats, linked materials, derivative scripts, cut history, stage receipts, publication events, append-only output learning, and available archived records.

**Never silently claim:** real publication, verified sources, synced devices, hosted backup, restored local media bytes, automated analytics, a passed APK, owner physical certification or live support for a category merely because a local card exists.

**No modifications:** existing reminders, notification scheduling, voice, alarms, login, production backend, backup/restore settings, account deletion, payments, original V337 stable code or live data.

## 4. Deferred risks and gate decisions

| Severity | Risk / uncertainty | Required next gate |
|---|---|---|
| P0 | A–E code has not been compiled or run cumulatively | post-UX10 Gradle/CI/instrumentation and exact SHA evidence |
| P0 | Legacy root/derivative publication targeting can change approval experience | old + new multi-output fixtures and explicit owner review |
| P0 | V337 upgrade, creator/portable/unified restore and project histories not certified | fixture-driven round trips and before/after hashes |
| P0 | New draft journal concurrent debounce/discard/process-death cases not executed | Android UI + stress + epoch race tests |
| P1 | Local SharedPreferences journaling is device-local and not a remote encrypted-backup promise | privacy review, UX09 data disclosures and later security validation |
| P1 | 320dp layouts, TalkBack, keyboard, tablet, long content not visually reviewed | UX10 polish and physical accessibility pass |
| P1 | External source links and `content://` references may be unavailable on a new device | honest unavailable indicators and restore/permission checks |
| P1 | Same project edited offline on two devices has no implemented conflict protocol | UX09 separately approved server identity, schema and merge design |
| P2 | Creative Board is structured and card-based, not a freeform visual editor | defer complex canvas/video preview to a cost/performance-reviewed roadmap |

## 5. Test program handoff

The post-UX10 matrix has **G01–G25** spanning build, legacy upgrade, serialization, JSON corruption, backup/restore, revision and generation fences, stage/workspace navigation, script-beat links, output targeting, publication histories, learning draft lifecycle, multi-output/legacy source, data safety, 5000-card performance, accessibility and physical devices.

Reference: `docs/qa/UX051E_WORKSPACE_CERTIFICATION_GATE_AFTER_UX10.md`.
Source-of-truth and proposed UX09 sync: `docs/engineering/UX051E_CREATOR_WORKSPACE_INTEGRATION_CONTRACT.md`.

**Do not certify or promote the cumulative UX stack on a document-only review.**

## 6. UX06 handoff (next approved roadmap slot)

**Next stage:** UX06 — Complete Reminder Experience, source implementation only, stacked on UX05.1E.

**Existing owner constraints to preserve:**
- Personal, Creator and 21-Day reminder parity in visual experience, voice and optional background music, respecting each reminder's existing storage/notification implementation.
- Five cinematic selectable effects/themes as approved in prior planning: camera aperture, sunrise, atmospheric particles, projector lighting and glowing rings (actual visual designs and sound settings stay owner-governed).
- Spoken reminders' repeats, voice identity, snooze/full-screen entry, battery/OS restrictions, notification permission, silent/DND behavior and false-delivery claims must be evaluated against the existing reminder services and manifests **before** code edits.
- Do not regress dependable reminder scheduling or introduce paid cloud/TTS providers without explicit approval.
- Respect prior roadmap decision: stage source work first, perform comprehensive consolidated testing after UX10. Any untested OS-dependent behavior must be labelled provisional.

**Handoff guardrails:** start UX06 by inspecting the actual reminder engine and related modules from this branch, identify invariants, then stage one separate draft PR. Do not automatically merge the UX05.1 stack or rewrite V337.

## 7. Change log for UX05.1E

- Created local Output Learning draft journal tied to revision/restore generation.
- Wove journal into Learning Desk, its save/Back/discard/navigation routes and in-flight disable state.
- Added exact record ownership, JSON and future UX09 sync proposal document.
- Added G01–G25 consolidated certification gate.
- Updated UX05.1 roadmap and authored this handoff.

**Release / owner action:** none requested now. A later release/merge/hosted change requires separate explicit owner approval and evidence.
