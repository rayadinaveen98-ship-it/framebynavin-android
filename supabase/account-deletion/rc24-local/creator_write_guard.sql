-- RC24 local-only rehearsal: Creator write protection against bypassrls roles.
-- This is NOT a Supabase production migration. Requires existing RC22 fixture.
-- The actual user tables in production have MORE columns/constraints.
DO $guard$
BEGIN
  IF current_setting('backlot.rc22.ephemeral_only', true) IS DISTINCT FROM 'true'
     OR current_database() <> 'postgres'
     OR current_user <> 'postgres'
     OR to_regclass('backlot_rc22_ci.deletion_fences') IS NULL
  THEN RAISE EXCEPTION 'Refusing RC24 outside local RC22 ephemeral fixture'; END IF;
END $guard$;

-- Exact names and owner keys from the live Backlot project, with minimal
-- synthetic columns needed to prove writes via the real PostgREST API.
-- Not intended to recreate or replace the production schema.
CREATE TABLE public.creator_profiles (
  user_id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  display_name text,
  username text,
  username_normalized text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE public.creator_sync_snapshots (
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  revision bigint NOT NULL CHECK(revision>0),
  content_sha256 text NOT NULL,
  payload text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY(user_id,revision)
);
CREATE TABLE public.creator_sync_heads (
  user_id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  content_sha256 text NOT NULL,
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT rc24_head_fk FOREIGN KEY(user_id,revision)
    REFERENCES public.creator_sync_snapshots(user_id,revision)
    ON DELETE RESTRICT
);

ALTER TABLE public.creator_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.creator_sync_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.creator_sync_heads ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.creator_profiles, public.creator_sync_snapshots,
  public.creator_sync_heads FROM PUBLIC;
GRANT SELECT,INSERT,UPDATE ON public.creator_profiles,
  public.creator_sync_snapshots, public.creator_sync_heads TO authenticated;
CREATE POLICY rc24_profile_owner ON public.creator_profiles
  FOR ALL TO authenticated USING(user_id=(SELECT auth.uid()))
  WITH CHECK(user_id=(SELECT auth.uid()));
CREATE POLICY rc24_snapshots_owner ON public.creator_sync_snapshots
  FOR ALL TO authenticated USING(user_id=(SELECT auth.uid()))
  WITH CHECK(user_id=(SELECT auth.uid()));
CREATE POLICY rc24_heads_owner ON public.creator_sync_heads
  FOR ALL TO authenticated USING(user_id=(SELECT auth.uid()))
  WITH CHECK(user_id=(SELECT auth.uid()));

-- Unlike RLS, a row trigger is invoked for user and service_role inserts
-- and updates. SECURITY DEFINER reads the private fence, which clients may
-- not select or change; row FOR SHARE serializes against a trusted UPDATE
-- that sets blocked=true. DELETE is deliberately NOT fenced: an authorized
-- deletion worker still needs to clear rows (heads -> snapshots -> profile).
CREATE FUNCTION backlot_rc22_ci.guard_creator_write()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=''
AS $fn$
DECLARE allowed boolean;
BEGIN
  IF NEW.user_id IS NULL THEN
    RAISE EXCEPTION 'Creator write owner missing' USING ERRCODE='42501';
  END IF;
  IF TG_OP='UPDATE' AND NEW.user_id <> OLD.user_id THEN
    RAISE EXCEPTION 'Creator owner cannot change' USING ERRCODE='42501';
  END IF;
  SELECT NOT blocked INTO allowed
    FROM backlot_rc22_ci.deletion_fences
    WHERE user_id=NEW.user_id
    FOR SHARE;
  IF allowed IS DISTINCT FROM TRUE THEN
    RAISE EXCEPTION 'Creator account is locked for deletion' USING ERRCODE='42501';
  END IF;
  RETURN NEW;
END
$fn$;
REVOKE ALL ON FUNCTION backlot_rc22_ci.guard_creator_write() FROM PUBLIC;
CREATE TRIGGER rc24_guard_profiles BEFORE INSERT OR UPDATE
  ON public.creator_profiles FOR EACH ROW
  EXECUTE FUNCTION backlot_rc22_ci.guard_creator_write();
CREATE TRIGGER rc24_guard_snapshots BEFORE INSERT OR UPDATE
  ON public.creator_sync_snapshots FOR EACH ROW
  EXECUTE FUNCTION backlot_rc22_ci.guard_creator_write();
CREATE TRIGGER rc24_guard_heads BEFORE INSERT OR UPDATE
  ON public.creator_sync_heads FOR EACH ROW
  EXECUTE FUNCTION backlot_rc22_ci.guard_creator_write();

-- Important: this DB trigger does NOT govern Storage API service_role
-- uploads. Direct privileged Storage calls still bypass Storage RLS;
-- the RC23 no-service-writes boundary remains essential.
