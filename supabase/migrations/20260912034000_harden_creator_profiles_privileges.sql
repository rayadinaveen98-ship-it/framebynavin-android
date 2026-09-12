-- FrameByNavin 2.0 production hardening.
-- creator_profiles contains account identity metadata only; creator projects remain local/Drive.
-- RLS policies already scope SELECT/INSERT/UPDATE/DELETE to auth.uid() = user_id.
-- This migration removes broad default grants, especially TRUNCATE, from client roles.

revoke all privileges on table public.creator_profiles from anon;
revoke truncate, references, trigger on table public.creator_profiles from authenticated;
grant select, insert, update, delete on table public.creator_profiles to authenticated;

-- Username claim is authenticated-only and internally binds writes to auth.uid().
revoke execute on function public.claim_creator_username(text, text) from public;
revoke execute on function public.claim_creator_username(text, text) from anon;
grant execute on function public.claim_creator_username(text, text) to authenticated;
grant execute on function public.claim_creator_username(text, text) to service_role;
