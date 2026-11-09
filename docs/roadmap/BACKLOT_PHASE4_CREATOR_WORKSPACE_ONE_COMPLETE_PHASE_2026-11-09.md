# Backlot Phase 4 — Creator Workspace (ONE COMPLETE FEATURE PACKAGE)

Owner direction: stop splitting work into endless micro-phases. Implement the
requested workspace changes as **one feature package**, with *no new roadmap
phases*. Full emulator/physical-device QA is deferred to Phase 7, but nothing
may be called release-tested without that evidence.

**Branch:** `feature/backlot-phase4-complete-creator-workspace-20261109`
**Base:** integrated Phase 3F V324 review branch, not unfinished Trust T.
**Code state:** source implementation committed, **not compiled or tested yet**.
**Do not install this candidate over the owner's V322**.

## Feature and acceptance contract (single Phase 4)

| Owner requirement | Integrated source path | Exit behavior |
| --- | --- | --- |
| Guided New Project and separate Quick Create | Existing `PProjectComposer` + `V20NewProjectWizard`, preserved | Both routes still share the same creator data authority, defaults and existing explicit Save |
| Setup draft survives Settings, activity loss and a later return | New `CreatorNewProjectDraftStore` and real composer integration | Complete form is kept locally with Creator restore-generation fencing, explicit Resume/Start Fresh, Save Draft & Exit/Discard |
| No false project creation on local draft save | `V20NewProjectWizard` status label and draft journal | UI says 'draft saved on this phone', never that the project was created or backed up |
| Never erase someone else's/another editor's work | `V203PendingReminderConfirmation` + exact draft fingerprint on real `CreatorViewModel` durable commit receipt | Only the exact submitted new-project draft is cleared AFTER durable acceptance; a newer changed form or previous restore-epoch draft remains |
| Project stage, next action, useful saved materials | Existing Alpha6 project hub, linked to actual workflow engine and Phase4 status policy | Continue routes to the canonical Plan/Script/Publish editors; no status or progress is auto-advanced by opening them |
| Clear local save/draft/backup meaning | `Phase4CreatorWorkspacePolicy` + Hub | Actual project revision shown as local, recoverable editor drafts separately labelled, earlier/held drafts routed for review, no false cloud-backup claim |
| Brief, research, references, linked assets and production checklist | Existing Alpha3 Plan studio remains source of truth; Phase4 hub shows source counts | Does not create a second reference/asset/workflow system |
| Strong script editing and interruption recovery | Existing Alpha4 Script Studio and `CreatorEditorDraftStore` retained, surfaced in hub | One embedded script authority; revision/restore generation gates remain; 'recoverable' is not 'published' or 'saved to project' |
| General-purpose editable project starting points | `CreatorProjectTemplates.contentTemplates` | Analysis, Commentary/Education, Short/Reel added in existing Plan template picker; additive, editable, no automatic completion or publication |
| Prepare-to-Publish without false posting claims | Existing Alpha5 Publish Studio and explicit publication receipts | Hub displays planned / ready / published as separate states. 'Ready' never means posted online |
| One cohesive existing visual language | Director's Cut surfaces + RecRed action emphasis | No separate mock product, unapproved theme or duplicated hub |

## Data safety and unresolved release gates

- Branch is **stacked on Phase 3F review PR #342**, whose integrated Android
  11/15 run has not passed; it does NOT include any of the draft Trust PRs
  #343–#353. This is intentional: backup work stays parked.
- **Do not** reset, uninstall, replace signature, force cloud restore, or
  apply old partial backups to the owner's V322 device with actual reminders
  and 21-day Done receipts.
- The local *new-project* form is not included in the proven universal cloud
  backup. It is app-private device data, generation-scoped, and can be
  manually resumed or discarded; do not claim it will transfer phones.
- The existing Plan/Script/Publish draft journal and saved project data
  remain independent. A new-project form cannot be treated as a
  `CreatorTask` until the real save commits.
- Trust Foundation remains source-only and paused. T1 cloud backup is not
  complete; #327 stays open and no backup-success UI is introduced.
- This one Phase 4 package is finished at the **source-review boundary** only.
  Compilation/static checks and integrated V324 branch conflict resolution
  still need attention. Full UI/emulator/android11/15/cross-device/physical
  upgrade testing, including 2× fonts and screenshots, is intentionally
  deferred until Phase 7.
- No signed owner APK, Play Store release or production merge can be declared
  without testing and owner visual approval.

## Tests authored for Phase 7, not run now

- Pure JVM: truthful saved/draft/publication facts, non-publishing templates,
  repeat template application preserving existing completed/published items.
- Android disposable CI: new-project draft persists after recreation,
  previous restore-generation record is isolated, old save receipt cannot
  erase a newer draft, exact matching receipt clears only its own draft.
- Full accepted journey to validate later: Quick Create and Guided flows,
  open Settings and return, force-process-stop/reopen, Resume/Start Fresh,
  Save Draft & Exit, create project, delay durable-write confirmation,
  reopen Plan/Script/Publish, edit long bilingual script, recover drafts,
  apply each general template, save and return, explicit Publish readiness,
  offline and 1.5×/2× fonts, preserve original V322 project/reminder data.

## Next locked roadmap, no extra phases

After this single Phase 4 feature package: Phase 5 Calendar/Reminders/
Weekly Plan/Automation → Phase 6 Insights/Analytics → deferred Phase 2
Visual Polish → finish release-critical Trust gaps + Phase 7 full integration
and device tests → Phase 8 controlled Play beta/release.
