-- Backlot RC29: exact-live-column local fixture + proposed Storage INSERT/UPDATE
-- restrictive policy contract. NEVER execute on a hosted Supabase project.
-- Not a migration; creates/discovers no real account and touches no live data.
--
-- This SQL exercises the 2026-10-30 read-only live catalog: creator_profiles,
-- creator_sync_heads, creator_sync_snapshots and the creator-voice-ideas bucket.
-- RC22/RC24 test fixtures must already exist in disposable local Supabase.
DO $guard$
BEGIN
 IF current_database() <> 'postgres'
    OR current_user <> 'postgres'
    OR current_setting('backlot.rc22.ephemeral_only',true) IS DISTINCT FROM 'true'
    OR to_regclass('backlot_rc22_ci.deletion_fences') IS NULL
    OR to_regclass('public.creator_sync_snapshots') IS NULL
    OR to_regclass('storage.objects') IS NULL
 THEN RAISE EXCEPTION 'RC29 STRICTLY LOCAL only: fixture marker or expected tables absent';
 END IF;
END $guard$;

-- Upgrade the RC24 simplified tables into the column/constraint shape found
-- in the actual Backlot DB. Real rows are NOT copied or accessed.
ALTER TABLE public.creator_profiles ADD COLUMN avatar_url text;

ALTER TABLE public.creator_sync_snapshots
  ADD COLUMN base_content_sha256 text,
  ADD COLUMN payload_sha256 text NOT NULL,
  ADD COLUMN schema_version integer NOT NULL,
  ADD COLUMN captured_at timestamptz NOT NULL,
  ADD COLUMN app_version text NOT NULL,
  ADD COLUMN project_count integer NOT NULL DEFAULT 0,
  ADD COLUMN idea_count integer NOT NULL DEFAULT 0,
  ADD COLUMN device_id text NOT NULL;
ALTER TABLE public.creator_sync_heads
  ADD CONSTRAINT rc29_heads_content_sha CHECK(content_sha256 ~ '^[0-9a-f]{64}$'),
  ADD CONSTRAINT rc29_heads_positive_revision CHECK (revision>0);
ALTER TABLE public.creator_sync_snapshots
  ADD CONSTRAINT rc29_snapshots_content_sha CHECK(content_sha256 ~ '^[0-9a-f]{64}$'),
  ADD CONSTRAINT rc29_snapshots_payload_sha CHECK(payload_sha256 ~ '^[0-9a-f]{64}$'),
  ADD CONSTRAINT rc29_snapshots_schema CHECK(schema_version>0 AND schema_version<1000),
  ADD CONSTRAINT rc29_snapshots_payload_limit CHECK(octet_length(payload)<=5242880),
  ADD CONSTRAINT rc29_snapshots_counts CHECK(project_count>=0 AND idea_count>=0),
  ADD CONSTRAINT rc29_snapshots_app_version CHECK(char_length(app_version) BETWEEN 1 AND 96),
  ADD CONSTRAINT rc29_snapshots_device CHECK(char_length(device_id) BETWEEN 1 AND 128);

-- Reproduce the four *actual* Backlot Voice Idea ownership policies on the
-- same bucket name, but in the disposable local Storage instance. Their
-- existing permissive INSERT/UPDATE policy bodies do NOT contain a deletion
-- fence and cannot be fixed by adding a second PERMISSIVE policy (OR logic).
CREATE POLICY "Backlot voice ideas select own"
  ON storage.objects FOR SELECT TO authenticated
  USING(bucket_id='creator-voice-ideas'
    AND (storage.foldername(name))[1]=(SELECT auth.uid())::text);
CREATE POLICY "Backlot voice ideas insert own"
  ON storage.objects FOR INSERT TO authenticated
  WITH CHECK(bucket_id='creator-voice-ideas'
    AND (storage.foldername(name))[1]=(SELECT auth.uid())::text);
CREATE POLICY "Backlot voice ideas update own"
  ON storage.objects FOR UPDATE TO authenticated
  USING(bucket_id='creator-voice-ideas'
    AND (storage.foldername(name))[1]=(SELECT auth.uid())::text)
  WITH CHECK(bucket_id='creator-voice-ideas'
    AND (storage.foldername(name))[1]=(SELECT auth.uid())::text);
CREATE POLICY "Backlot voice ideas delete own"
  ON storage.objects FOR DELETE TO authenticated
  USING(bucket_id='creator-voice-ideas'
    AND (storage.foldername(name))[1]=(SELECT auth.uid())::text);

-- Public RPC is NOT added here: RC28's localhost contract creates it.
-- We use a private security-definer predicate so Auth clients cannot SELECT
-- or UPDATE the server-controlled fence table directly.
CREATE SCHEMA backlot_rc29_ci;
REVOKE ALL ON SCHEMA backlot_rc29_ci FROM PUBLIC;
GRANT USAGE ON SCHEMA backlot_rc29_ci TO authenticated;

CREATE FUNCTION backlot_rc29_ci.may_write_voice()
RETURNS boolean LANGUAGE plpgsql VOLATILE
SECURITY DEFINER SET search_path=''
AS $fn$
DECLARE
  v_user uuid := (SELECT auth.uid());
  v_open boolean;
BEGIN
  IF v_user IS NULL THEN RETURN false; END IF;
  -- Locks serialize with RC28's FOR UPDATE of the deletion_fences row:
  -- a concurrent Storage metadata write either commits BEFORE the fence or
  -- sees it blocked. Never rely on a stale metadata/JWT claim.
  SELECT NOT f.blocked INTO v_open
    FROM backlot_rc22_ci.deletion_fences f
    WHERE f.user_id=v_user
    FOR SHARE;
  RETURN COALESCE(v_open,false);
END
$fn$;
REVOKE ALL ON FUNCTION backlot_rc29_ci.may_write_voice() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION backlot_rc29_ci.may_write_voice() TO authenticated;

-- **Restrictive**, i.e. AND'd with ALL existing permissive policies.
-- Neither a permissive OR policy nor a benign additional bucket can undo it.
-- Storage's authenticated read/delete of old media stays possible so a
-- trusted cleanup process can remove files; privileged service_role still
-- bypasses all Storage RLS and must remain on the production STOP checklist.
CREATE POLICY "rc29 voice insert requires active account"
  AS RESTRICTIVE ON storage.objects FOR INSERT TO authenticated
  WITH CHECK(bucket_id <> 'creator-voice-ideas'
    OR (backlot_rc29_ci.may_write_voice()
        AND (storage.foldername(name))[1]=(SELECT auth.uid())::text));

CREATE POLICY "rc29 voice update requires active account"
  AS RESTRICTIVE ON storage.objects FOR UPDATE TO authenticated
  USING(bucket_id <> 'creator-voice-ideas'
    OR (backlot_rc29_ci.may_write_voice()
        AND (storage.foldername(name))[1]=(SELECT auth.uid())::text))
  WITH CHECK(bucket_id <> 'creator-voice-ideas'
    OR (backlot_rc29_ci.may_write_voice()
        AND (storage.foldername(name))[1]=(SELECT auth.uid())::text));

-- Local-only catalog regression: exact Backlot column names/typing must stay
-- aligned with the read-only live catalog; fail immediately on unexpected drift.
DO $audit$
DECLARE actual text;
BEGIN
 SELECT string_agg(column_name||':'||data_type||':'||is_nullable,',' ORDER BY ordinal_position)
 INTO actual FROM information_schema.columns
 WHERE table_schema='public' AND table_name='creator_sync_snapshots';
 IF actual IS DISTINCT FROM
 'user_id:uuid:NO,revision:bigint:NO,content_sha256:text:NO,payload:text:NO,created_at:timestamp with time zone:NO,'||
 'base_content_sha256:text:YES,payload_sha256:text:NO,schema_version:integer:NO,captured_at:timestamp with time zone:NO,'||
 'app_version:text:NO,project_count:integer:NO,idea_count:integer:NO,device_id:text:NO'
 THEN RAISE EXCEPTION 'RC29 snapshot column shape mismatch: %',actual; END IF;
END $audit$;
