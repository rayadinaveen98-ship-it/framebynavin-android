# Backlot Lab V337 — isolated fresh backend deployment

**Date:** 2026-11-17  
**Status:** Database installed in NEW Free Supabase development project; Android acceptance and storage bucket pending.  
**Reference:** stable/backlot-v337-p0-owner-accepted-20261116 (old stable preserved)  
**New project:** `vasleafrmsovdbdlrcsw` / Backlot Lab (Mumbai), separately connected account.

## Scope and provenance
- Only the NEW development project's database has been changed. No original Backlot service, old data, stable app binary, Android signing, production deploy, Play publication, or billing changes.
- `supabase/fresh-install/backlot_lab_v337_bootstrap.sql`: reconstructions of original Creator profile and timestamp defaults from V337 Android client fields and `tools/supabase_alpha23_creator_identity.sql`; verified Creator snapshot schema/RPC from `20260914174737_creator_snapshot_sync_v123.sql` and `20260914190438_add_creator_sync_head_fk_covering_index_v124.sql`; Personal snapshot schema/RPC from `20261101_personal_cloud_sync_v279.sql`.
- Intentionally does not apply historical lifecycle/backup migrations (they were later retired) or any synthetic account deletion fixture. Sync-only delete RPC is **not full account deletion**.
- `supabase/fresh-install/backlot_lab_v337_voice_storage_policies.sql` creates private object policies for client uploader `CreatorCloudVoiceMedia.kt`.

## Verified after installation
- Migration `backlot_lab_v337_fresh_install_20261117`: applied successfully.
- Four empty public application tables: `creator_profiles`, `creator_sync_snapshots`, `creator_sync_heads`, `personal_sync_heads`; 4/4 RLS enabled; 14 policies.
- Credential-less/public roles have zero read access to application tables and zero execute grants on app RPCs. `authenticated` has only necessary access, subject to RLS.
- Automatically generated RLS event helper (`public.rls_auto_enable`) had mistakenly been executable by public roles; its permissions were revoked. Security Advisors initially reported 2 WARN and now show 0 findings after first migration.
- Performance Advisor reports two unused indexes on an empty database (not actionable).
- Voice storage policy migration `backlot_lab_v337_private_voice_storage_policies`: applied successfully.
- **NOT YET VERIFIED**: Google sign-in end-to-end, account creation, cross-account authorization via real tokens, cloud conflict handling, private voice recording upload/download, Android build or physical device.

## Required owner setup — Storage bucket
Using new Backlot Lab Supabase Dashboard -> Storage -> New bucket:
- Name/ID: **`creator-voice-ideas`** (exact)
- Access: **Private / public OFF**
- Allowed MIME: **`audio/mp4`** (Backlot voice recordings are .m4a); optionally add compatible AAC types after validating client output
- Object size cap: **20 MB** for development (may adjust with evidence).
- Do not create SQL tables manually, copy old records, upload secrets, or make bucket public.
These bucket steps must be done via Supabase Storage dashboard/API, not direct SQL mutation to internal Storage tables.

## Required next engineering changes (separately reviewed)
- Isolated Android candidate should use NEW Supabase URL + NEW *publishable* key; preserve the Google Web Client ID and existing Android OAuth/signing identity. Never put service-role key or Google client secret into APK. Do not edit V337 stable branch or main.
- Old APK will **not** switch; old Auth sessions are not valid for new project. Use a disposable tester identity and separate app-state reset/test install with owner acceptance.
- Verify Google provider enabled (dashboard alone does not prove login success). Identity token -> new Auth session -> Creator username -> Personal and Creator sync -> two-account negative RLS -> conflict/retry -> Storage upload/download.
- Test network drops, app reinstalls, repeated reminders, account switching, notifications, and offline-only use. Do not conflate emulators with physical-device certification.
- Account deletion and complete cloud-data erasure remain a **separate blocked workstream**, not satisfied by `delete_creator_sync_data`.
- Deployment/release blocked pending explicit approval and evidence.

## Reproducibility and caution
These two SQL files are fresh-project-only. Do not replay against an existing database. Do not run all old migration files atop this composite script; it would duplicate objects, alter security contracts, or drop useful tables. A future clean-room automated test should validate schema from scratch and with authenticated two-user tokens before promoting Android candidate.
