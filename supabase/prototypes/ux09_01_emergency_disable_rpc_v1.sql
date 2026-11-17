-- UX09-01 EMERGENCY SERVER-CONTRACT DISABLE CANDIDATE — NOT DEPLOYED.
-- OWNER APPROVAL REQUIRED. PostgreSQL statement revokes app-facing RPC
-- EXECUTE permissions only. It preserves all underlying account data.
-- Use when a deployed RPC is unsafe but records must survive for review.
-- Never DROP auth.users, internal tables, change logs, tombstones or media.
--
-- WARNING: this blocks all UX09-01 apply/pull callers until separately
-- approved restore; a signed-in Android client should report unavailable.
-- This is not a migration revert or a user account / cloud-data deletion.
begin;

revoke execute on function public.backlot_ux09_apply_v1(
  uuid,text,uuid,bigint,jsonb,boolean
) from authenticated;

revoke execute on function public.backlot_ux09_pull_v1(
  bigint,integer
) from authenticated;

commit;

-- Post-disable read-only checks after approval:
-- select has_function_privilege('authenticated',
--  'public.backlot_ux09_apply_v1(uuid,text,uuid,bigint,jsonb,boolean)',
--  'EXECUTE');
-- select has_function_privilege('authenticated',
--  'public.backlot_ux09_pull_v1(bigint,integer)','EXECUTE');
-- Both MUST be false. Existing Creator/Personal snapshot API remains
-- separate, subject to its own access policies and service state.
