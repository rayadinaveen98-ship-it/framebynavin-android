-- FrameByNavin v1.8.3: server-enforced creator-data deletion barrier.
-- Backend-first candidate. Do not deploy until the matching client and rollback gates pass.
-- No existing creator rows are migrated, deleted, or rewritten by this migration.
-- The private lifecycle table contains no user content, email, credentials, or tokens.
-- All public RPCs bind the owner to auth.uid(); no client-supplied target user exists.

CREATE SCHEMA IF NOT EXISTS creator_guard_v183;
REVOKE ALL ON SCHEMA creator_guard_v183 FROM PUBLIC, anon, authenticated;
ALTER DEFAULT PRIVILEGES IN SCHEMA creator_guard_v183 REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;

CREATE TABLE IF NOT EXISTS creator_guard_v183.lifecycle (
    user_id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    phase text NOT NULL DEFAULT 'active' CHECK (phase IN ('active', 'deleted')),
    generation bigint NOT NULL DEFAULT 0 CHECK (generation >= 0),
    delete_txid bigint,
    updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE creator_guard_v183.lifecycle ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON creator_guard_v183.lifecycle FROM PUBLIC, anon, authenticated;

-- Every writer and lifecycle transition takes the same transaction-scoped lock.
-- The advisory lock precedes row locks for INSERT/UPDATE through statement triggers.
CREATE OR REPLACE FUNCTION creator_guard_v183.lock_state(p_user uuid)
RETURNS TABLE(phase text, generation bigint, delete_txid bigint)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $function$
BEGIN
    IF p_user IS NULL THEN
        RAISE EXCEPTION 'Authentication required' USING ERRCODE = '42501';
    END IF;
    PERFORM pg_catalog.pg_advisory_xact_lock(183, pg_catalog.hashtext(p_user::text));
    INSERT INTO creator_guard_v183.lifecycle(user_id) VALUES (p_user)
    ON CONFLICT (user_id) DO NOTHING;
    RETURN QUERY SELECT l.phase, l.generation, l.delete_txid
      FROM creator_guard_v183.lifecycle AS l WHERE l.user_id = p_user FOR UPDATE;
END;
$function$;
REVOKE ALL ON FUNCTION creator_guard_v183.lock_state(uuid) FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION creator_guard_v183.lock_current_writer()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $function$
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Authentication required' USING ERRCODE = '42501';
    END IF;
    PERFORM creator_guard_v183.lock_state(auth.uid());
    RETURN NULL;
END;
$function$;
REVOKE ALL ON FUNCTION creator_guard_v183.lock_current_writer() FROM PUBLIC, anon, authenticated;

-- The header is a generation fence, not an authentication secret. The signed JWT
-- and RLS still establish ownership. Legacy clients may omit it only at epoch 0.
CREATE OR REPLACE FUNCTION creator_guard_v183.assert_owned_write()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $function$
DECLARE
    v_owner uuid := auth.uid();
    v_phase text;
    v_generation bigint;
    v_header text;
    v_epoch bigint;
BEGIN
    IF v_owner IS NULL OR NEW.user_id IS DISTINCT FROM v_owner THEN
        RAISE EXCEPTION 'Creator write owner mismatch' USING ERRCODE = '42501';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.user_id IS DISTINCT FROM NEW.user_id THEN
        RAISE EXCEPTION 'Creator ownership cannot be changed' USING ERRCODE = '42501';
    END IF;
    SELECT s.phase, s.generation INTO v_phase, v_generation
      FROM creator_guard_v183.lock_state(v_owner) AS s;
    IF v_phase <> 'active' THEN
        RAISE EXCEPTION 'Creator cloud data is deleted. Explicit reactivation is required.' USING ERRCODE = '42501';
    END IF;
    v_header := COALESCE(
        NULLIF(pg_catalog.current_setting('request.headers', true), '')::jsonb ->> 'x-creator-write-epoch', '0');
    IF v_header !~ '^(0|[1-9][0-9]{0,17})$' THEN
        RAISE EXCEPTION 'Invalid creator write epoch' USING ERRCODE = '22023';
    END IF;
    v_epoch := v_header::bigint;
    IF v_epoch <> v_generation THEN
        RAISE EXCEPTION 'Stale creator write epoch. Refresh account state before writing.' USING ERRCODE = '42501';
    END IF;
    RETURN NEW;
END;
$function$;
REVOKE ALL ON FUNCTION creator_guard_v183.assert_owned_write() FROM PUBLIC, anon, authenticated;

-- Ordinary DELETE requests must also carry the current epoch after reactivation.
-- The private transaction permit allows only creator_delete_owned_data() to delete
-- while tombstoned. No public caller can write or forge this private row value.
CREATE OR REPLACE FUNCTION creator_guard_v183.lock_current_delete()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $function$
DECLARE v_phase text; v_generation bigint; v_permit bigint; v_header text;
BEGIN
    -- Auth's internal FK cascade may run with no user JWT. It can delete empty
    -- tables after creator cleanup; the row guard still rejects any owned rows.
    IF auth.uid() IS NULL THEN RETURN NULL; END IF;
    SELECT s.phase, s.generation, s.delete_txid INTO v_phase, v_generation, v_permit
      FROM creator_guard_v183.lock_state(auth.uid()) AS s;
    IF v_permit IS NOT NULL AND v_permit = pg_catalog.txid_current() THEN RETURN NULL; END IF;
    IF v_phase <> 'active' THEN
        RAISE EXCEPTION 'Creator cloud data is deleted. Explicit reactivation is required.' USING ERRCODE = '42501';
    END IF;
    v_header := COALESCE(NULLIF(pg_catalog.current_setting('request.headers', true), '')::jsonb ->> 'x-creator-write-epoch', '0');
    IF v_header !~ '^(0|[1-9][0-9]{0,17})$' THEN
        RAISE EXCEPTION 'Invalid creator write epoch' USING ERRCODE = '22023';
    END IF;
    IF v_header::bigint <> v_generation THEN
        RAISE EXCEPTION 'Stale creator write epoch. Refresh account state before deleting.' USING ERRCODE = '42501';
    END IF;
    RETURN NULL;
END;
$function$;
REVOKE ALL ON FUNCTION creator_guard_v183.lock_current_delete() FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION creator_guard_v183.assert_owned_delete()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $function$
BEGIN
    IF auth.uid() IS NULL OR OLD.user_id IS DISTINCT FROM auth.uid() THEN
        RAISE EXCEPTION 'Creator delete owner mismatch' USING ERRCODE = '42501';
    END IF;
    RETURN OLD;
END;
$function$;
REVOKE ALL ON FUNCTION creator_guard_v183.assert_owned_delete() FROM PUBLIC, anon, authenticated;

-- These guards apply to direct PostgREST writes and all existing RPCs, including
-- save_creator_backup and claim_creator_username. Existing RLS remains intact.
DO $block$
DECLARE v_table text;
BEGIN
    FOREACH v_table IN ARRAY ARRAY['creator_backups','creator_devices','creator_profiles'] LOOP
        EXECUTE pg_catalog.format('DROP TRIGGER IF EXISTS creator_v183_statement_guard ON public.%I', v_table);
        EXECUTE pg_catalog.format('CREATE TRIGGER creator_v183_statement_guard BEFORE INSERT OR UPDATE ON public.%I FOR EACH STATEMENT EXECUTE FUNCTION creator_guard_v183.lock_current_writer()', v_table);
        EXECUTE pg_catalog.format('DROP TRIGGER IF EXISTS creator_v183_row_guard ON public.%I', v_table);
        EXECUTE pg_catalog.format('CREATE TRIGGER creator_v183_row_guard BEFORE INSERT OR UPDATE ON public.%I FOR EACH ROW EXECUTE FUNCTION creator_guard_v183.assert_owned_write()', v_table);
        EXECUTE pg_catalog.format('DROP TRIGGER IF EXISTS creator_v183_statement_delete_guard ON public.%I', v_table);
        EXECUTE pg_catalog.format('CREATE TRIGGER creator_v183_statement_delete_guard BEFORE DELETE ON public.%I FOR EACH STATEMENT EXECUTE FUNCTION creator_guard_v183.lock_current_delete()', v_table);
        EXECUTE pg_catalog.format('DROP TRIGGER IF EXISTS creator_v183_row_delete_guard ON public.%I', v_table);
        EXECUTE pg_catalog.format('CREATE TRIGGER creator_v183_row_delete_guard BEFORE DELETE ON public.%I FOR EACH ROW EXECUTE FUNCTION creator_guard_v183.assert_owned_delete()', v_table);
    END LOOP;
END;
$block$;

CREATE OR REPLACE FUNCTION public.creator_cloud_status()
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $function$
DECLARE v_user uuid := auth.uid(); v_phase text; v_generation bigint;
BEGIN
    IF v_user IS NULL THEN RAISE EXCEPTION 'Authentication required' USING ERRCODE = '42501'; END IF;
    SELECT s.phase, s.generation INTO v_phase, v_generation FROM creator_guard_v183.lock_state(v_user) AS s;
    RETURN pg_catalog.jsonb_build_object('phase', v_phase, 'generation', v_generation);
END;
$function$;
REVOKE ALL ON FUNCTION public.creator_cloud_status() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.creator_cloud_status() TO authenticated;

-- One transaction: a failure rolls back the tombstone and every row deletion.
-- Repeated calls with the original generation are idempotent. No account identity or local phone data is removed.
CREATE OR REPLACE FUNCTION public.creator_delete_owned_data(p_expected_generation bigint)
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $function$
DECLARE v_user uuid := auth.uid(); v_phase text; v_generation bigint;
BEGIN
    IF v_user IS NULL THEN RAISE EXCEPTION 'Authentication required' USING ERRCODE = '42501'; END IF;
    SELECT s.phase, s.generation INTO v_phase, v_generation FROM creator_guard_v183.lock_state(v_user) AS s;
    -- A delayed request from before reactivation must never delete new data.
    IF p_expected_generation IS NULL OR NOT (
        p_expected_generation = v_generation OR
        (v_phase = 'deleted' AND p_expected_generation = v_generation - 1)
    ) THEN
        RAISE EXCEPTION 'Creator lifecycle generation changed. Review account state again.' USING ERRCODE = '42501';
    END IF;
    IF v_phase = 'active' THEN
        UPDATE creator_guard_v183.lifecycle SET phase = 'deleted', generation = generation + 1, updated_at = now()
          WHERE user_id = v_user RETURNING generation INTO v_generation;
    END IF;
    -- The permit exists only during this transaction and is cleared before return.
    UPDATE creator_guard_v183.lifecycle SET delete_txid = pg_catalog.txid_current() WHERE user_id = v_user;
    DELETE FROM public.creator_backups WHERE user_id = v_user;
    DELETE FROM public.creator_devices WHERE user_id = v_user;
    DELETE FROM public.creator_profiles WHERE user_id = v_user;
    IF EXISTS (SELECT 1 FROM public.creator_backups WHERE user_id = v_user)
       OR EXISTS (SELECT 1 FROM public.creator_devices WHERE user_id = v_user)
       OR EXISTS (SELECT 1 FROM public.creator_profiles WHERE user_id = v_user) THEN
        RAISE EXCEPTION 'Creator deletion verification failed' USING ERRCODE = '23514';
    END IF;
    UPDATE creator_guard_v183.lifecycle SET delete_txid = NULL WHERE user_id = v_user;
    RETURN pg_catalog.jsonb_build_object('phase', 'deleted', 'generation', v_generation);
END;
$function$;
REVOKE ALL ON FUNCTION public.creator_delete_owned_data(bigint) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.creator_delete_owned_data(bigint) TO authenticated;

-- Reactivation never restores deleted data. The caller must explicitly acknowledge
-- the current deletion generation; a different or stale generation is rejected.
-- Every reactivation increments the epoch so pre-deletion requests can never replay.
CREATE OR REPLACE FUNCTION public.creator_resume_owned_data(p_expected_generation bigint)
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $function$
DECLARE v_user uuid := auth.uid(); v_phase text; v_generation bigint;
BEGIN
    IF v_user IS NULL THEN RAISE EXCEPTION 'Authentication required' USING ERRCODE = '42501'; END IF;
    SELECT s.phase, s.generation INTO v_phase, v_generation FROM creator_guard_v183.lock_state(v_user) AS s;
    IF p_expected_generation IS NULL OR p_expected_generation <> v_generation THEN
        RAISE EXCEPTION 'Creator lifecycle generation changed. Review account state again.' USING ERRCODE = '42501';
    END IF;
    IF v_phase = 'deleted' THEN
        UPDATE creator_guard_v183.lifecycle SET phase = 'active', generation = generation + 1, updated_at = now()
          WHERE user_id = v_user RETURNING generation INTO v_generation;
    END IF;
    RETURN pg_catalog.jsonb_build_object('phase', 'active', 'generation', v_generation);
END;
$function$;
REVOKE ALL ON FUNCTION public.creator_resume_owned_data(bigint) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.creator_resume_owned_data(bigint) TO authenticated;

-- Explicitly revoke default PUBLIC access even on an existing installation.
REVOKE ALL ON FUNCTION public.creator_cloud_status() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.creator_delete_owned_data(bigint) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.creator_resume_owned_data(bigint) FROM PUBLIC;
