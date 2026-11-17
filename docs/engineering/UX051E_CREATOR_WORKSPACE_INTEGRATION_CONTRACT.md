# Backlot UX05.1E — Creator Workspace Data, Restore and Future Sync Contract

**Stage:** UX05.1E, source integration / architecture handoff  
**Status:** DOCUMENTED PROPOSED CONTRACT; repository draft branch ONLY. No production sync/hosting/auth/schema change has been approved or implemented.  
**Source:** stacked draft PRs #393–#401, including UX05.1A–D.  
**Protected baseline:** `stable/backlot-v337-premiere-owner-testing-20261114` remains owner-tested V337. No merge, delete, release or hosted data mutation.  
**Verification:** source inspection only; build, tests, emulator, actual device and restore certification are deferred until UX10.

## 1. Canonical ownership map

| Record | Canonical owner | Identity / version | Allowed writer | Must never imply |
|---|---|---|---|---|
| Project/task | `TaskStore` / `CreatorTask` | project ID | existing task writer behind CreatorDataGate | published just because a stage advanced |
| Workspace & brief | `CreatorTask.workspace` | project ID + `revision` | Plan owner; scoped from original TaskStore current state | fresh server data |
| Workflow/stage | workflow template and engine | stable template ID + stage ID | explicit stage advance/approval | output creation or actual publication |
| Stage work and evidence | `workspace.stageWork` | template ID + stage ID; evidence ID | Stage Desk via TaskStore revision + epoch checks | source fact-checking from mere link |
| Script Studio | `workspace.scriptStudio` | beat ID and studio revision | Script Studio | verified transcription or finished film |
| Source and asset links | `workspace.references` / `assets` | ID per project; URI/reference metadata | Plan editor | embedded bytes or indefinitely accessible local URI |
| Deliverables | `workspace.deliverables` | stable output ID; parent output ID | Plan for creation, Publish Studio for publish state | publication of another output |
| Derivative scripts / cut history | `outputScripts`, `editVersions` | output ID / immutable version ID | Output Craft | actual media rendering or hosting |
| Stage approval receipts | `stageTransitions` | UUID + stage identity + prior revision | explicit workflow approval | analytics, live URL or push confirmation |
| Publication events | each `CreatorDeliverable.publicationHistory` | append-only event ID | Publish Studio / publication engine | permission to release remotely |
| Manual learning | `outputLearnings` | learning ID + **one** real deliverable ID | Output Learning writer | platform API analytics or revenue |
| Draft journals | local SharedPreferences sidecars | exact project + editor/output + workspace revision + restore generation | editor's draft journal | canonical save / cross-device sync |

**Non-negotiable:** A derived read-only Creative Board is not a separate writer. It projects the existing references, assets, stage evidence, beats, deliverables and cut version records. Do not persist a parallel Board card collection unless a separately approved migration and conflict protocol are established.

## 2. Referential rules

1. A stage record is unique within the pair `(workflowTemplateId, stageId)`. Never reuse an ID for a different workflow meaning.
2. Evidence `referenceId`, `assetId` and `scriptBeatId` must belong to the same project's canonical collections. `CreatorStageWorkPolicy` rejects **new** links to missing IDs but preserves a previously saved unavailable link, which the Board/Stage Desk must label as unavailable.
3. A derivative has `parentDeliverableId` from the same project. Graph sanitization may remove broken/cyclic parent links, but must not rewrite publication histories.
4. Output Craft script, cut metadata, and Learning snapshots are scoped to one deliverable ID. Reused IDs across projects must not grant access to foreign records.
5. Current stage approval resolves *explicitly linked output IDs first*. With no links, one unambiguous root deliverable is the implicit primary. Multiple roots or missing explicit targets demand a creator selection. Every required target must be independently creator-confirmed live before publication-stage approval.
6. Learning is append-only: a new observation is a new ID; an old learning record is neither overwritten nor silently reassigned if a referenced deliverable disappears. Plan deletion of an output with saved learning is blocked.
7. Review receipts are attached to the **exact reviewed content** and become stale on changes; no sync merge may carry forward reviewedAtMillis as approval for different text/evidence.
8. Real publication is not inferred from `TaskStatus.DONE`, workflow position, a draft, a prepared upload, a cut approval or another output.

## 3. Current local write and restore contract

- Current local source of truth: `TaskStore` Creator JSON written within `CreatorDataGate` and guarded by the existing mutation mutex. Optimistic editor writes require current `workspace.revision`; restore-sensitive writes also require current `CreatorDataGate.generation`.
- UX05.1B adds optional Brief strings `objective`, `successCriteria`, `constraints`, appended after existing data-class parameters and defaulting to empty for older records.
- UX05.1C adds optional `CreatorStageEvidence.scriptBeatId`, appended after existing evidence fields and defaulting to empty for older records. The existing stage work JSON codec is used for both canonical saved stage work and Stage Draft journaling.
- UX05.1D adds optional `workspace.outputLearnings`; missing key decodes to empty. Malformed arrays, missing/duplicate learning IDs and invalid positive timestamps fail closed, retaining the original source bytes instead of replacing them with empty data.
- All Plan/Publish saves must reattach current authoritative stage work, output craft histories, approval receipts and learning snapshots. Plan deletion of outputs with linked craft/publication/learning histories must be blocked, and unrelated editor saves must not overwrite publication or learning.
- UX05.1E adds a local, noncanonical Output Learning draft journal (`CreatorOutputLearningDraftStore`). It loads only for identical project/output/workspace revision/restore generation, writes after a short debounce, and is cleared on successful canonical save or explicit owner discard. Stale or failed-to-clear drafts are **quarantined**, never silently merged. This journal is **not** part of explicit Backlot creator portable export; it is a temporary local recovery aid.
- Backup/restore certification must use TaskStore's actual export/import path (normal Creator, portable and unified archive), verify the new JSON fields survive round trips, and confirm activity/revision/draft invalidation after restore. No claim of test coverage until this is actually executed.
- Stored `content://` and device-local asset locations are references, not the bytes; imported metadata may display an unavailable link and must never claim guaranteed media availability on another device.

## 4. UX09 multi-device sync: PROPOSED, NOT IMPLEMENTED

A Gmail login by itself does not authorize or perform Backlot project synchronization. Before UX09 code, the founder must approve scope, cost, authorization, retention and remote-schema design. Suggested offline-first protocol:

**Write envelope** (proposed): account-scoped project ID; stable device ID; operation UUID; source schema version; base server revision; local project revision; timestamp for presentation only; operation type; record identity; signed-in account authorization derived on server from verified credentials, *not* accepted from user-supplied account IDs.

**Server semantics** (proposed):
- Authenticate/authorize account to every project, output, attachment and operation; reject cross-account writes and reads; rate-limit and log without leaking private content.
- Transactionally apply idempotent operation UUIDs under an expected server revision. Acknowledgements must include accepted remote revision; retries must not produce duplicate learning snapshots, publication events or stage approvals.
- Keep a durable outbound queue and clear offline/pending/synced/conflict UI. Never claim devices synced while offline, before acknowledgement or after a rejected conflict.
- Scalars such as audience, promise, notes and objective use three-way comparison; if concurrently changed, hold both and ask the owner to resolve. Last-write-wins timestamps are **not** safe as a generic strategy.
- Append-only records (learned observations, publication events, version histories and approval receipts) merge by stable ID with exact payload equality. Same ID with different payload is a conflict, not a silent replacement.
- Reordered evidence/checklists use stable IDs and explicit order operations; concurrent moves are conflicts or resolved with a documented deterministic rule. The authoring device must not silently lose order or status changes.
- Deletions require tombstones and dependency checks; do not resurrect a deleted project, or cascade-delete output history due to stale offline state.
- Treat publication status, project completion and stage approval as explicit guarded domain actions, not a blind merge of arbitrary object fields. Re-run target, current state, revision and permissions checks when applying operations.
- Never sync draft journals as canonical data. A device can resume its own eligible draft only under its local revision and restore epoch; if the server changes the project, show a review/rebase flow rather than overwriting remote changes.
- Plan for interrupted uploads, server partial failure, account switching, revoke/logout, wrong-account local data, account deletion, and recovery from old-app clients. Measure costs before selecting services. No paid service is authorized by this document.

## 5. Privacy / data minimization contract for future UX09

- Creator sources, unpublished scripts, stages, drafts, performance observations and URLs can be sensitive. Remote content must remain private by default and partitioned by verified account; no public reads or client-trusted RLS bypass.
- Do not upload local media bytes, `content://` URIs, contacts or private authentication tokens just because a project links them. Any file sync requires separate storage, consent, quota and privacy approval.
- Explicit owner deletion must be separately specified for local data vs hosted data vs offline backups, including tombstone/retention/recovery policy. UX05.1E changes **no** existing account deletion, backup/restore settings or permissions.
- Implement transport encryption and account-controlled remote access only after architectural approval; present accurate data storage and deletion descriptions in UI. Local SharedPreferences journals are not evidence of remote encryption.

## 6. Staged deployment and rollback

1. Retain current protected V337 and verified owner APK. The UX stack is **draft source**, not released product.
2. Continue UX06–UX10 on the latest cumulative draft head only after UX05.1E handoff. Do not merge any intermediate draft as a shortcut.
3. After UX10, compile/lint, run JVM/CI/emulator and physical certification using clean and V337-upgraded fixtures; correct failures and repeat affected checks. Inspect record bytes before/after.
4. Ask for explicit owner approval for merges, hosted schema/auth changes, remote sync, Play Store release and any data migration. Keep branch rollback distinct from any later irreversible hosted migration.

## 7. Remaining decisions requiring separate approval

- UX09 identity linking: can one Android local library attach to a newly signed-in account, and how does the owner resolve a second account on the same device?
- UX09 storage/hosting choice, operating costs, quotas and attachments; do not buy a service before approval.
- Multi-device conflict UI and exact deletion/tombstone/retention policy.
- If the user later wants custom freeform Creative Board items, specify new canonical IDs, ordering and cross-device migration first.
- All UX05.1D multi-root publication behavior must pass legacy-project and multi-output fixtures before promotion.

**Contract authority:** This file records current staged invariants and future **proposals**. It is not proof of implementation, testing, a hosted deployment, a passing APK or approved UX09 architecture.
