# Backlot UX05.1E — Consolidated Workspace Certification Gate

**Testing status:** NOT RUN. This is an execution-ready matrix for the **post-UX10** campaign specifically requested by the owner. No current claim of Kotlin compile, lint, CI, instrumented QA, owner visual acceptance, APK readiness or release readiness.

**Source stack:** UX01 #393 → UX02 #394 → UX03 #395 → UX04 #396 → UX05 #397 → UX05.1A #398 → UX05.1B #399 → UX05.1C #400 → UX05.1D #401 → UX05.1E draft.  
**Protected baseline:** V337 physically approved owner-testing baseline remains untouched during source staging.

## Mandatory stop conditions

A cumulative candidate must **NOT** be labeled stable/production-ready if any of these is unverified or failing:

- Project source and histories survive V337 in-place upgrade and all export/import routes.
- No silent loss of stage drafts, evidence, script beats, publication events or manual learning under ordinary navigation, stale revision, restore, process death and deletion.
- Correct target-specific publication approval for main and derivative outputs.
- One project, one canonical writer per field; Plan/Publish cannot overwrite stage, script, cut or learning histories.
- Kotlin compilation/lint, all relevant JVM tests, Android instrumented tests, integrated CI and actual physical visual+functional acceptance.
- Accessibility and mobile reachability at compact phone and tablet breakpoints.
- Owner-approved release/merge decision only after evidence is recorded.

## Preflight (post-UX10 only)

1. Capture exact cumulative branch, parent SHAs, application ID, build variants, Gradle/JDK/Kotlin/Compose versions and all merged/stacked scope. Resolve package compatibility **before** trusting test results.
2. Inventory existing V337 test app data, developer/debug synthetic data and owner testing baseline. Never use live owner or hosted data for destructive testing.
3. Produce pristine V337 synthetic fixture(s) and cumulative candidate fixtures; run isolated upgrade and rollback *assessments* on disposable/emulator copies, preserving originals.
4. Collect code coverage and failures without skipping tests for a green build.

## Matrix and acceptance evidence

| Gate | Scenario / fixture | Expected invariant | Evidence required |
|---|---|---|---|
| G01 Build | compile Kotlin, Compose, lint, all app variants | no compile issues, missing symbols, API/minSDK errors | CI build URLs and exact SHAs |
| G02 Legacy model | old workspace without optional Brief, scriptBeatId or outputLearnings keys | empty defaults; unchanged legacy payload after round trip | parsed JSON before/after |
| G03 Restore | Creator normal, portable and unified archive from legacy/new fixtures | full project/stage/script/output/learning data round trip; no silent wipe | archive validation and diff |
| G04 Restore fence | save while backup restore changes generation | no stale task/draft write, no false success; preserve original bytes | restore epoch logs |
| G05 Canonical mutation | concurrent Plan, Publish, Stage, Script, Output and Learning updates | revision mismatch fails closed, owner records not overwritten | event/revision journal |
| G06 Stage switching | Save & Open success/failure, Keep Editing, Discard & Open, duplicate taps | successful save before navigation; explicit discard only; fail-closed errors | UI tests plus manual video |
| G07 Stage review | edit each notes, task, evidence, decision or link after marking reviewed | old review receipt invalidates; deliberate re-review possible | policy unit tests |
| G08 Evidence links | source/asset/beat links valid, removed, fake, cross-project and reordered | deny fabricated links; historic missing links retained and disclosed | link graph fixtures |
| G09 Stage module | film, news, podcast, food, travel, education, tech and unknown workflow | prompts relevant; opt-in tasks only; no auto-advance | snapshot/semantics suite |
| G10 Creative Board | no cards, long cards, 500 and 5000 cards, duplicate legacy IDs | stable lazy list, truthful data, direct canonical navigation, no crash | trace + UI screenshots |
| G11 Brief 2.0 | objective, successCriteria and constraints edited, draft-restored, publish saved | fields never lost by another editor; no optional field is a blocker | JSON and UI replay |
| G12 Output intent | one main with published derivative; unpublished main | cannot approve main publication from derivative alone | transition unit tests |
| G13 Multi-output | two roots, explicit targets, one missing linked ID | unlinked ambiguous blocks; explicit target must independently be live | deterministic fixtures |
| G14 Edit review | approved cut for unrelated output; target has no approved cut | stage warning pertains only to target; existing edit warnings remain warnings | policy tests |
| G15 Publication history | published, updated, reopened, corrected and legacy parent publication | no fabricated or dropped event, no stage-based fake publish | history diff |
| G16 Learning | confirmed live vs planned/ready, skipped, archived, completed | only permitted output can append; completed remains eligible | policy unit tests |
| G17 Learning journal | rotate, process death, clear, discard, save success/failure, stale epoch/revision | only eligible same-revision local draft restored; canonical record unchanged until Save | preference/epoch fixtures |
| G18 Learning concurrency | double-tap, long save, back/switch/publish mid-save, retry after conflict | no duplicate record, no false success or input loss | UI+store combined test |
| G19 Learning history | 0/1/many entries, long unicode, manual metric/source, orphan output ID | append only, source labelled manual; missing output remains read-only | replay + byte diff |
| G20 Deletion fences | delete output with derivative script/cut/publish/learning history | blocked with actionable message; no detached orphan data loss | negative tests |
| G21 Full UX regression | UX01–UX05.1 + UX06–UX10, existing reminders, home, idea vault, notifications | unrelated stable behavior preserved | integrated CI and physical checklist |
| G22 UI/UX | 320dp phone, regular Android, tablet, landscape, font 200%, keyboard | no clipped actions or inaccessible navigation; consistent design tokens | screen captures |
| G23 Accessibility | TalkBack, focus order, controls/descriptions, min targets, reduced motion | all critical workflows usable without color-only meaning | manual accessibility run |
| G24 Security | cross-project fake IDs, malformed JSON, stale saved-state/draft, restore race | fail-closed, no cross-account access claims | deterministic synthetic probes |
| G25 Performance | 5000 linked cards, 1000 beats, large learning histories | no ANR/OOM; measure time-to-render, scroll jank, memory | Android profiler/trace |

## Test-fixture boundaries

- Use disposable emulator/physical test profiles and synthetic identities, timestamps, publication URLs, source URLs, assets and metric figures. Never perform a real publication, charge, email, hosted schema operation or deletion.
- Keep a 1-root+3-derivatives case, a 2-independent-roots case, an old V337 case, a deleted/missing historical source case, an archived/finished case and a restored-backup case.
- Use explicit stable IDs in fixtures so before/after comparisons detect accidental reassignment and history shortening.
- In legacy fixture checks, verify parent output canonicalization did not silently transform an earlier approved owner publication.

## Artifact evidence required for founder acceptance

- Exact tested SHA / PR chain and CI URLs.
- Full pass/fail matrix with links to test names, logs, before/after JSON hashes and any critical screenshot/video.
- APK digest/build provenance and a **physical-device owner acceptance report** distinct from emulator results.
- Open defect list, severity, reproducer, corrective commit and retest evidence.
- Privacy/deletion/sync decisions recorded as either already approved and tested or explicitly **out of scope**.
- Final decision recorded as source-staged, tested, certified, or approved for merge/release; never combine these states.

## Current known high-risk questions (source inspection, not a failing test result)

1. UX05.1D publication targeting changes legacy and multi-output behavior; requires an exhaustive fixture matrix.
2. New output learning draft journal protects matching revision/epoch edits, but synchronous SharedPreferences writes, debounce, rapid navigation and restore-collision behavior still need runtime certification.
3. Project JSON fields are additive but old-app/downgrade interoperability is not guaranteed; do not launch mixed client versions without explicit compatibility design.
4. `content://` URIs and external media asset paths may not be accessible after transfer; UI must not claim that links imply preserved media bytes.
5. The Creative Board visual is a new entry point on compact Android screens; no screen-size, TalkBack or long-list benchmark has yet been performed.

**Schedule:** G01–G25 execute together only after the owner-approved UX10 implementation stage. This file alone does not certify anything.
