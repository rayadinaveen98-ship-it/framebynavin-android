# Backlot UX09-02A-N — Recursive workspace field-audit evidence

**Status (2026-11-17):** SOURCE-ONLY PROPOSED/CI PENDING. Parent top-level field gate PR #438 passed; disconnected journal PR #437 passed 18 focused JVM tests at its exact prior commit. This PR does not change either tested parent.

## Exact scope
- Versioned manifest: `docs/engineering/UX09_02A_NESTED_SERIALIZATION_MANIFEST_20261117.json`
- Read-only stdlib verifier: `tools/qa/backlot_ux09_02a_nested_coverage.py`
- Python-only CI: `.github/workflows/backlot-ux09-02a-nested-coverage.yml`
- This document only. No changes to Android Kotlin/Java, Google Auth, Supabase tables/RPCs, users, backups, APKs or release workflows.

## Audited nested paths
22 explicit serializer paths covering 21 current Kotlin data classes and 143 field occurrences (includes both title and thumbnail `CreatorVariantIdea` encoders), based on current `TaskStore.kt`:
- Project Content DNA, Workspace, Project References, Checklist items, Assets, Deliverables, title/thumbnail variants, Publish Gate checks and Publication History.
- Script Studio, hooks, title alternatives, script beats.
- Stage Work, stage checklists/evidence/decisions, stage transition approvals.
- Derivative output scripts, script revision snapshots and edit/cut versions.

The guard checks the **model's field list equals the reviewed field list**, and that the narrow JSON encoder's key set equals those model fields (except deliberately parent-derived fields). It also runs four in-memory negative test mutations: unreviewed model field, removed nested class, unexpected implicit field and missing asset location key. Existing top-level Project/Idea coverage guard is rerun in the same CI.

## Specific product-preservation findings
- **Script Studio `projectId`** is deliberately NOT written in nested JSON: the existing `decodeScriptStudio(projectId, ...)` reconstructs the parent identity. The manifest treats `projectId` as implicit-parent, never as a remote-generated ID.
- **Asset `location` and edit-version `sourceLocation`** can contain Android content links/local file references. The audit tracks these keys to prevent accidental loss, but they must never be uploaded as raw device paths.
- **Large scripts, title variants, deliverables, histories and notes** can exceed the existing 16,384-byte hosted Lab RPC JSON payload limit. Audit parity does NOT solve oversized-record sync.
- **Idea reversible trash (`trashedAtMillis`)** must not become an irreversible backend tombstone. The separate top-level gate continues to enforce this.
- Legacy Project/Idea IDs, nested link IDs and media portability need explicit versioned mapping and two-way round-trip tests.

## What source audit does NOT establish
- Source regex is not an AST or runtime serializer test. It proves reviewed field names and `.put()` key coverage in bounded encoder spans, **not** that values are semantically mapped correctly, decoded losslessly, correctly normalized, or fully preserved across Android versions.
- A future recursive, typed domain codec with parameterized round-trip tests is essential before actual sync.
- Per-account encrypted durable storage, process-kill recovery, multi-offline-edit ordering, remote idempotency, legacy snapshot handoff, privacy/backup/delete and physical two-device UX remain separate gates.
- No real user content should be written to the experimental plaintext journal.

## Acceptance to close this audit gate
- GitHub Actions completed SUCCESS for the precise PR head, both source checkers passed including synthetic negative mutations, and CI proves no app source modifications.
- PR remains DRAFT and unmerged; no merges or releases without explicit review.

**Next work:** plan safe oversized-content architecture and a portable, versioned Creator Project/Idea *codec* that preserves every approved field (with local-only device/voice state kept separately), plus a process-safe encrypted journal and ordered offline outbox before application integration.
