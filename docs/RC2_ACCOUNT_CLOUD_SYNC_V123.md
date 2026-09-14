# FrameByNavin v123 — Account Cloud Sync

Branch: `feature/v2.0-rc2-account-cloud-sync`

This slice promotes Supabase from creator identity-only infrastructure into the primary private automatic creator backup/sync layer while preserving FrameByNavin's local-first working model.

## Safety contract

- Google sign-in remains the creator account identity.
- Local creator data remains the immediate working copy.
- Supabase stores private versioned creator snapshots with RLS and authenticated ownership checks.
- Automatic reconciliation uses content hashes and compare-and-swap semantics.
- Divergent two-device edits never use silent last-write-wins; the user must explicitly keep this phone or restore the cloud workspace.
- Existing backup payload validation, checksums, recovery journal, reminder rescheduling, and OAuth/session exclusions are reused.
- Google Drive is retained as an optional manual import/export recovery path rather than the primary sync store.

## Validation

CI for this branch runs unit tests, debug Kotlin compilation, APK assembly, and verifies the v123 version identity before publishing the debug APK artifact.
