# Backlot V229.5 — Content Workspace Publish Readiness (Build 262 preview)

## Purpose
The existing "Next Useful Moves" section surfaces only two safe, read-only editor links; previously every PLANNED output got the same generic packaging suggestion. This slice makes the Publish suggestion precise and honest using only already-saved fields.

## Read-only advisory policy
For the first PLANNED output (in the user's stored order), suggest one concrete next move:
1. Missing platform or format: choose destination and format.
2. Missing working title: write a title.
3. Required pre-publish gate marked TODO: review the unfinished checks.
4. Missing description: draft it.
5. For a YouTube output with neither a thumbnail concept nor a non-blank thumbnail variant: develop a thumbnail idea.
6. Otherwise: inspect saved packaging before changing status.

When no PLANNED output exists, a READY output with a required TODO gate may get a review prompt. Already PUBLISHED outputs are never treated as unpublished. This policy **does not** certify readiness or claim publication, and it does not copy flags from other outputs. The existing Focus policy prioritizes the current workflow stage, never duplicates an editor in a hint pair and shows at most two prompts.

## Boundaries
- No new screen, persistence, DataStore writes, migration, new reminder, notifications, network request, analytics or AI-model claims.
- Existing Plan/Script/Publish editors, project versions, creative fields, backups and progress rules remain the source of truth.
- The publish workflow retains explicit user confirmation; "READY" and "PUBLISHED" are saved user records, not external verification.

## QA and release
- Six pure JVM policy tests for title/gate/description/YouTube thumbnail, READY/PUBLISHED distinction and Workspace integration.
- Android 11 and 15 Compose UI tests for an actual Publish editor action and no misleading output hint on a published deliverable.
- Full Build 262 tests, lint, full app, and package signer upgrade from verified Build 261 in the independent CI workflow.
- Draft until full CI and physical phone UI/upgrade/backup checks. No main/stable merge.
