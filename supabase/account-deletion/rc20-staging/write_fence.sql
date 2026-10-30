-- RC20: disposable PostgreSQL RLS / write-fence REHEARSAL ONLY.
-- NOT a production Supabase migration, not actual Storage API, not actual Auth.
DO $guard$
BEGIN
  IF current_database() <> 'backlot_rc20_ephemeral' THEN
    RAISE EXCEPTION 'Refusing RC20 script outside isolated ephemeral database';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='rc20_user_a') OR
     NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='rc20_user_b') THEN
    RAISE EXCEPTION 'Missing isolated test identities';
  END IF;
END $guard$;

CREATE SCHEMA backlot_rc20_ci;
REVOKE ALL ON SCHEMA backlot_rc20_ci FROM PUBLIC;
GRANT USAGE ON SCHEMA backlot_rc20_ci TO rc20_user_a, rc20_user_b;

-- Trusted session identity is modeled by isolated SQL roles, NOT a user
-- controllable GUC or auth JWT. Real Supabase must use auth.uid() from a
-- server-validated JWT and must be re-tested with the actual Storage API.
CREATE FUNCTION backlot_rc20_ci.actor_id() RETURNS uuid
LANGUAGE sql STABLE SECURITY INVOKER SET search_path=''
AS $fn$
 SELECT CASE current_user
   WHEN 'rc20_user_a' THEN '7eb549e3-b454-4acd-9bba-dc1cf7d6ba73'::uuid
   WHEN 'rc20_user_b' THEN 'c719ec81-a544-456b-ac02-1c5039c77739'::uuid
   ELSE NULL::uuid
 END
$fn$;

CREATE TABLE backlot_rc20_ci.fences (
  user_id uuid PRIMARY KEY,
  blocked boolean NOT NULL DEFAULT false,
  blocked_at timestamptz,
  CHECK (blocked OR blocked_at IS NULL)
);
INSERT INTO backlot_rc20_ci.fences (user_id) VALUES
 ('7eb549e3-b454-4acd-9bba-dc1cf7d6ba73'),
 ('c719ec81-a544-456b-ac02-1c5039c77739');
ALTER TABLE backlot_rc20_ci.fences ENABLE ROW LEVEL SECURITY;
ALTER TABLE backlot_rc20_ci.fences FORCE ROW LEVEL SECURITY;
REVOKE ALL ON backlot_rc20_ci.fences FROM PUBLIC;
GRANT SELECT ON backlot_rc20_ci.fences TO rc20_user_a, rc20_user_b;
CREATE POLICY read_own_fence ON backlot_rc20_ci.fences FOR SELECT
TO rc20_user_a,rc20_user_b
USING (user_id = (SELECT backlot_rc20_ci.actor_id()));

-- The shared transaction advisory lock serializes a write's eligibility
-- decision against the trusted administrator's exclusive deletion fence.
-- VOLATILE forces a fresh snapshot after waiting for a concurrent fence.
-- This is a PostgreSQL-only feasibility test, not proof that Supabase Storage
-- API enforces this same protocol. ALL production writers must participate.
CREATE FUNCTION backlot_rc20_ci.write_allowed(p_user uuid)
RETURNS boolean LANGUAGE plpgsql VOLATILE SECURITY INVOKER SET search_path=''
AS $fn$
DECLARE permitted boolean;
BEGIN
  IF p_user IS DISTINCT FROM backlot_rc20_ci.actor_id() THEN RETURN false; END IF;
  PERFORM pg_catalog.pg_advisory_xact_lock_shared(
    pg_catalog.hashtextextended(p_user::text, 20261030));
  SELECT NOT blocked INTO permitted
    FROM backlot_rc20_ci.fences WHERE user_id=p_user;
  RETURN COALESCE(permitted,false);
END $fn$;

-- Trusted admin ONLY. This has no callable privilege for either user role.
CREATE FUNCTION backlot_rc20_ci.mark_deleting(p_user uuid, p_at timestamptz)
RETURNS void LANGUAGE plpgsql VOLATILE SECURITY INVOKER SET search_path=''
AS $fn$
BEGIN
  IF p_user IS NULL OR p_at IS NULL THEN
    RAISE EXCEPTION 'Invalid deletion fence' USING ERRCODE='22023';
  END IF;
  PERFORM pg_catalog.pg_advisory_xact_lock(
    pg_catalog.hashtextextended(p_user::text, 20261030));
  UPDATE backlot_rc20_ci.fences
     SET blocked=true,blocked_at=COALESCE(blocked_at,p_at)
   WHERE user_id=p_user;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'No matching verified account' USING ERRCODE='22023';
  END IF;
END $fn$;

CREATE TABLE backlot_rc20_ci.creator_projects(
 id text PRIMARY KEY,
 owner_id uuid NOT NULL REFERENCES backlot_rc20_ci.fences(user_id),
 notes text NOT NULL
);
CREATE TABLE backlot_rc20_ci.voice_object_metadata_mock(
 bucket_id text NOT NULL DEFAULT 'creator-voice-ideas',
 name text NOT NULL,
 owner_id uuid NOT NULL REFERENCES backlot_rc20_ci.fences(user_id),
 bytes bigint NOT NULL CHECK (bytes>0),
 PRIMARY KEY(bucket_id,name),
 CHECK (bucket_id='creator-voice-ideas'),
 CHECK (split_part(name,'/',1)=owner_id::text)
);
ALTER TABLE backlot_rc20_ci.creator_projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE backlot_rc20_ci.voice_object_metadata_mock ENABLE ROW LEVEL SECURITY;
ALTER TABLE backlot_rc20_ci.creator_projects FORCE ROW LEVEL SECURITY;
ALTER TABLE backlot_rc20_ci.voice_object_metadata_mock FORCE ROW LEVEL SECURITY;
REVOKE ALL ON backlot_rc20_ci.creator_projects,
  backlot_rc20_ci.voice_object_metadata_mock FROM PUBLIC;
GRANT SELECT,INSERT,UPDATE ON backlot_rc20_ci.creator_projects,
  backlot_rc20_ci.voice_object_metadata_mock TO rc20_user_a,rc20_user_b;

CREATE POLICY creator_select ON backlot_rc20_ci.creator_projects FOR SELECT
TO rc20_user_a,rc20_user_b
USING (owner_id=(SELECT backlot_rc20_ci.actor_id()));
CREATE POLICY creator_insert ON backlot_rc20_ci.creator_projects FOR INSERT
TO rc20_user_a,rc20_user_b
WITH CHECK (owner_id=(SELECT backlot_rc20_ci.actor_id())
  AND backlot_rc20_ci.write_allowed(owner_id));
CREATE POLICY creator_update ON backlot_rc20_ci.creator_projects FOR UPDATE
TO rc20_user_a,rc20_user_b
USING (owner_id=(SELECT backlot_rc20_ci.actor_id()))
WITH CHECK (owner_id=(SELECT backlot_rc20_ci.actor_id())
  AND backlot_rc20_ci.write_allowed(owner_id));

CREATE POLICY voice_select ON backlot_rc20_ci.voice_object_metadata_mock FOR SELECT
TO rc20_user_a,rc20_user_b
USING (owner_id=(SELECT backlot_rc20_ci.actor_id()));
CREATE POLICY voice_insert ON backlot_rc20_ci.voice_object_metadata_mock FOR INSERT
TO rc20_user_a,rc20_user_b
WITH CHECK (owner_id=(SELECT backlot_rc20_ci.actor_id())
  AND split_part(name,'/',1)=owner_id::text
  AND backlot_rc20_ci.write_allowed(owner_id));
CREATE POLICY voice_update ON backlot_rc20_ci.voice_object_metadata_mock FOR UPDATE
TO rc20_user_a,rc20_user_b
USING (owner_id=(SELECT backlot_rc20_ci.actor_id()))
WITH CHECK (owner_id=(SELECT backlot_rc20_ci.actor_id())
  AND split_part(name,'/',1)=owner_id::text
  AND backlot_rc20_ci.write_allowed(owner_id));

-- New Postgres functions are otherwise PUBLIC-executable by default.
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA backlot_rc20_ci FROM PUBLIC;
GRANT EXECUTE ON FUNCTION backlot_rc20_ci.actor_id()
 TO rc20_user_a,rc20_user_b;
GRANT EXECUTE ON FUNCTION backlot_rc20_ci.write_allowed(uuid)
 TO rc20_user_a,rc20_user_b;
REVOKE ALL ON FUNCTION backlot_rc20_ci.mark_deleting(uuid,timestamptz)
 FROM rc20_user_a,rc20_user_b;
