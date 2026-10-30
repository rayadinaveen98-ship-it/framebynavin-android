-- RC28: actual Edge Function request/status SQL contract, TESTED ONLY in
-- disposable Supabase local DB with RC22/RC24/RC26 fixture schemas.
-- NON-MIGRATION: DO NOT APPLY TO CONNECTED/LIVE BACKLOT SUPABASE.
DO $guard$
BEGIN
 IF current_database()<>'postgres' OR current_user<>'postgres'
    OR current_setting('backlot.rc22.ephemeral_only',true) IS DISTINCT FROM 'true'
    OR to_regclass('backlot_rc26_ci.requests') IS NULL
    OR to_regclass('backlot_rc22_ci.deletion_fences') IS NULL
 THEN RAISE EXCEPTION 'RC28 contract must use isolated local Supabase fixtures';
 END IF;
END $guard$;

-- PUBLIC RPC surface: no user_id or request_id parameters. The owner identity
-- MUST come exclusively from verified PostgREST auth.uid() and auth.jwt().
-- SECURITY DEFINER needed to write the private receipt and fence atomically.
-- This function is restricted to authenticated, checks real Auth session
-- *creation* in last 5 mins; not merely JWT refresh. Not a replacement for
-- strong external Google OAuth reauthentication in the future web flow.
CREATE FUNCTION public.backlot_request_deletion(p_confirmation text)
RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path=''
AS $fn$
DECLARE
  v_user uuid := (SELECT auth.uid());
  v_session uuid;
  v_request uuid;
  v_state text;
  v_stage text;
BEGIN
  IF p_confirmation IS DISTINCT FROM 'DELETE MY BACKLOT ACCOUNT' THEN
    RAISE EXCEPTION 'Explicit confirmation required' USING ERRCODE='22023';
  END IF;
  IF v_user IS NULL THEN
    RAISE EXCEPTION 'Authentication required' USING ERRCODE='42501';
  END IF;
  BEGIN
    v_session := NULLIF((SELECT auth.jwt()->>'session_id'), '')::uuid;
  EXCEPTION WHEN invalid_text_representation THEN
    RAISE EXCEPTION 'Fresh sign-in required' USING ERRCODE='42501';
  END;
  IF v_session IS NULL OR NOT EXISTS (
    SELECT 1 FROM auth.sessions s WHERE s.id=v_session
      AND s.user_id=v_user
      AND s.created_at >= clock_timestamp()-interval '5 minutes'
  ) THEN
    RAISE EXCEPTION 'Recent verified sign-in required' USING ERRCODE='42501';
  END IF;
  -- FOR UPDATE locks the same fence row that the protected Creator DB write
  -- triggers read FOR SHARE. This prevents a competing DB update slipping
  -- through after account fencing commits.
  PERFORM 1 FROM backlot_rc22_ci.deletion_fences
    WHERE user_id=v_user FOR UPDATE;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Missing account fence row' USING ERRCODE='42501';
  END IF;
  INSERT INTO backlot_rc26_ci.requests(id,user_id)
    VALUES(gen_random_uuid(),v_user)
    ON CONFLICT(user_id) DO NOTHING;
  UPDATE backlot_rc22_ci.deletion_fences
     SET blocked=true, blocked_at=coalesce(blocked_at,clock_timestamp())
   WHERE user_id=v_user;
  SELECT r.id,r.status,r.stage INTO STRICT v_request,v_state,v_stage
    FROM backlot_rc26_ci.requests r WHERE r.user_id=v_user;
  RETURN jsonb_build_object('request_id',v_request::text,
                            'status',v_state,'stage',v_stage);
END
$fn$;
REVOKE ALL ON FUNCTION public.backlot_request_deletion(text) FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.backlot_request_deletion(text) TO authenticated;

CREATE FUNCTION public.backlot_my_deletion_status()
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path=''
AS $fn$
DECLARE
 v_user uuid := (SELECT auth.uid());
 v_id uuid;
 v_state text;
 v_stage text;
BEGIN
 IF v_user IS NULL THEN
   RAISE EXCEPTION 'Authentication required' USING ERRCODE='42501';
 END IF;
 SELECT id,status,stage INTO v_id,v_state,v_stage
 FROM backlot_rc26_ci.requests WHERE user_id=v_user;
 IF v_id IS NULL THEN RETURN NULL; END IF;
 RETURN jsonb_build_object('request_id',v_id::text,
                           'status',v_state,'stage',v_stage);
END
$fn$;
REVOKE ALL ON FUNCTION public.backlot_my_deletion_status() FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.backlot_my_deletion_status() TO authenticated;

-- Refresh PostgREST's local RPC discovery without restarting the stack.
NOTIFY pgrst, 'reload schema';
