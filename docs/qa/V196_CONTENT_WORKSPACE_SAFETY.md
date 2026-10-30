# Backlot V196 — Content Workspace Phase 1: Safety & Data Truth

Base: frozen V195.5, `stable/backlot-v195-5` at `957bfe0f17ca26e695371cd38f4a2bd29dea121e`.
Candidate: `feature/backlot-v196-content-workspace-safety-p1`. App versionCode 204, versionName `2.5.4-v196-workspace-safety-preview`.
**Preview only. Do not update the frozen V195.5 branch or move main.**

## In-scope changes

1. **Authoritative project progress.** Workspace Hub now displays workflow-stage progress only. Production checklist percentage remains contextual inside Plan. Finishing a checklist cannot make an unfinished project appear 100% complete.
2. **Publication-history-safe deletion.** Plan prompts before removing one output or a derivative subtree; it previews the count. The persistence policy rejects any removal (including ancestor removal) that would erase a currently published output or an output with publication history, even if the UI is bypassed. Unpublished-only subtrees may still be removed.
3. **Publication gate truth.** Required checks count as satisfied only if DONE, never SKIPPED. Skipping is an available action only for optional checks. The core rights/music/source check is protected against optionalizing, deletion, renaming, and skipping; previously removed or legacy UUID-based versions are normalized/recreated.
4. **Version isolation.** Version 204 and same applicationId/signing identity as the personal preview. No migrations, package rename, schedule change, or creator-data reset.

## Evidence and regression tests

- `CreatorProjectFactsV186PolicyTest.completedChecklistNeverAdvancesProjectWorkflow`.
- `CreatorContentStabilizationTest.deletingPublishedOrPreviouslyPublishedOutputIsRejected`.
- `CreatorContentStabilizationTest.deletingAncestorOfPublishedDerivativeIsRejected`.
- `CreatorPublishWorkflowTest.skippedRequiredGateNeverAllowsPublication`.
- `CreatorPublishWorkflowTest.rightsGateIsRestoredAndCannotBeMadeOptional`.
- `CreatorPublishWorkflowTest.optionalSkippedCheckDoesNotBlockPublication`.
- The branch workflow also runs inherited V161–V195.5 guards, all JVM tests, Android lint, instrumentation compile and emulator acceptance. CI success must be confirmed on the final commit, not inferred from workflow creation.

## Device acceptance required before promotion

- Create an untouched project and finish all production checklist items; Workspace Hub must still reflect real workflow stages.
- Create root deliverable plus two derivatives; remove the root, confirm count and cancel, then confirm on a test project and save. Reopen to check exact resulting set.
- Try deleting published root, published derivative, and reopened output with history; each attempt must preserve publication history and existing links.
- Test fresh, old and recovered publication gates: SKIP disabled for required checks, rights check impossible to remove/skip, publish blocked until every required check is DONE.
- Confirm optional checks can be intentionally SKIPPED and history still records publish/update/reopen.
- Repeat while rotating, force-stopping and reopening editor drafts, including simulated save failure and revision conflict.
- Verify same-signature in-place update preserves old projects, local assets, publication history, reminder schedules, and custom imagery. **Do not uninstall a data-bearing build to solve signature mismatch.**
- Test accessibility at 100%, 130%, 200% font size and inspect both dark/light modes. Android emulator is not a substitute for physical phone acceptance.

## Known limits / deferred

- Full interactive screenshot QA and physical-device acceptance remain outstanding.
- No UI redesign, new features, or comprehensive editor-draft hardening in this phase.
- Draft journaling is separate from canonical project saves. V197+ follow-up should test fast close and recoverability.
- The rights check records user attestation; it is not an automated copyright verification or platform API confirmation.
- A recorded publish event is not verified against an external live platform.

## Promotion rules

Candidate CI green + device QA + data-preservation checks + user acceptance required. Merge only through reviewed PR into appropriate integration branch. Leave `stable/backlot-v195-5` unchanged until separately approved.
