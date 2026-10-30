-- RC22 FREE LOCAL Supabase Auth + Storage test fixture ONLY.
-- Executed by a dedicated GitHub Actions runner against 127.0.0.1:54322.
-- NEVER APPLY TO A HOSTED SUPABASE PROJECT. NOT A MIGRATION.
--
-- Defense in depth: the runner must deliberately opt in on a single
-- localhost-only psql session; the Python harness validates both URL hosts.
DO $guard$
BEGIN
  IF current_setting('backlot.rc22.ephemeral_only', true) IS DISTINCT FROM 'true' THEN
    RAISE EXCEPTION 'RC22 fixture requires explicit ephemeral-only session guard';
  END IF;
  IF current_database() <> 'postgres' OR current_user <> 'postgres' THEN
    RAISE EXCEPTION 'Unexpected local Supabase database identity';
  END IF;
END $guard$;

CREATE SCHEMA backlot_rc22_ci;
REVOKE ALL ON SCHEMA backlot_rc22_ci FROM PUBLIC;
GRANT USAGE ON SCHEMA backlot_rc22_ci TO authenticated;

CREATE TABLE backlot_rc22_ci.deletion_fences (
  user_id uuid PRIMARY KEY,
  blocked boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  blocked_at timestamptz
);
REVOKE ALL ON backlot_rc22_ci.deletion_fences FROM PUBLIC;
ALTER TABLE backlot_rc22_ci.deletion_fences ENABLE ROW LEVEL SECURITY;
ALTER TABLE backlot_rc22_ci.deletion_fences FORCE ROW LEVEL SECURITY;

-- The production implementation must use a separately reviewed trusted
-- write fence that handles all DB/Storage/privileged writers and races.
-- This staging RLS predicate fails CLOSED if no fence row is present.
-- It gets identity from auth.uid() validated by Supabase Auth/PostgREST,
-- never from request body, client-supplied user_id or user_metadata.
CREATE FUNCTION backlot_rc22_ci.may_write()
RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path=''
AS $fn$
  SELECT coalesce(
    (SELECT NOT blocked
       FROM backlot_rc22_ci.deletion_fences
       WHERE user_id=(SELECT auth.uid())),
    false)
$fn$;
REVOKE ALL ON FUNCTION backlot_rc22_ci.may_write() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION backlot_rc22_ci.may_write() TO authenticated;

CREATE TABLE public.rc22_creator_projects (
  id text PRIMARY KEY,
  owner_id uuid NOT NULL,
  title text NOT NULL
);
ALTER TABLE public.rc22_creator_projects ENABLE ROW LEVEL SECURITY;
GRANT SELECT,INSERT,UPDATE ON public.rc22_creator_projects TO authenticated;

CREATE POLICY rc22_creator_read
 ON public.rc22_creator_projects FOR SELECT TO authenticated
 USING (owner_id=(SELECT auth.uid()));
CREATE POLICY rc22_creator_insert
 ON public.rc22_creator_projects FOR INSERT TO authenticated
 WITH CHECK (owner_id=(SELECT auth.uid())
         AND (SELECT backlot_rc22_ci.may_write()));
CREATE POLICY rc22_creator_update
 ON public.rc22_creator_projects FOR UPDATE TO authenticated
 USING (owner_id=(SELECT auth.uid()))
 WITH CHECK (owner_id=(SELECT auth.uid())
         AND (SELECT backlot_rc22_ci.may_write()));

-- Applied to a local CI-only private Storage bucket. Actual file
-- mutations occur through the Storage HTTP API, never by modifying
-- storage.objects with SQL.
CREATE POLICY rc22_voice_select
 ON storage.objects FOR SELECT TO authenticated
 USING (bucket_id='backlot-rc22-ci'
  AND (storage.foldername(name))[1]=(SELECT auth.uid())::text);
CREATE POLICY rc22_voice_insert
 ON storage.objects FOR INSERT TO authenticated
 WITH CHECK (bucket_id='backlot-rc22-ci'
  AND (storage.foldername(name))[1]=(SELECT auth.uid())::text
  AND (SELECT backlot_rc22_ci.may_write()));
CREATE POLICY rc22_voice_update
 ON storage.objects FOR UPDATE TO authenticated
 USING (bucket_id='backlot-rc22-ci'
  AND (storage.foldername(name))[1]=(SELECT auth.uid())::text)
 WITH CHECK (bucket_id='backlot-rc22-ci'
  AND (storage.foldername(name))[1]=(SELECT auth.uid())::text
  AND (SELECT backlot_rc22_ci.may_write()));
CREATE POLICY rc22_voice_delete
 ON storage.objects FOR DELETE TO authenticated
 USING (bucket_id='backlot-rc22-ci'
  AND (storage.foldername(name))[1]=(SELECT auth.uid())::text);
