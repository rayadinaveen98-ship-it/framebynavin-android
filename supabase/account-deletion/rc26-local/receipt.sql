-- Disposable RC26 local Supabase integration rehearsal, NOT a migration.
-- Reused local RC22/24 data; never execute on a hosted Supabase project.
DO $$
BEGIN
  IF current_database() <> 'postgres'
     OR current_user <> 'postgres'
     OR current_setting('backlot.rc22.ephemeral_only',true) IS DISTINCT FROM 'true'
     OR to_regclass('backlot_rc22_ci.deletion_fences') IS NULL
  THEN RAISE EXCEPTION 'RC26 requires explicit RC22 LOCAL ephemeral fixture'; END IF;
END $$;
CREATE SCHEMA backlot_rc26_ci;
REVOKE ALL ON SCHEMA backlot_rc26_ci FROM PUBLIC;
CREATE TABLE backlot_rc26_ci.requests (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL UNIQUE,
  stage text NOT NULL DEFAULT 'fenced'
    CHECK(stage IN ('fenced','storage_deleted','data_deleted','auth_deleted','complete')),
  status text NOT NULL DEFAULT 'pending'
    CHECK(status IN ('pending','retryable','complete')),
  attempts int NOT NULL DEFAULT 0 CHECK(attempts BETWEEN 0 AND 5),
  last_error text,
  created_at timestamptz NOT NULL DEFAULT now(),
  completed_at timestamptz,
  CHECK((status='complete')=(completed_at IS NOT NULL))
);
REVOKE ALL ON backlot_rc26_ci.requests FROM PUBLIC;
ALTER TABLE backlot_rc26_ci.requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE backlot_rc26_ci.requests FORCE ROW LEVEL SECURITY;
-- An authenticated or anon client cannot create, view, or alter a receipt.
-- The privileged local worker uses a direct, verified loopback DB connection.
-- Deliberately NO foreign key referencing auth.users so completion survives
-- actual Auth account deletion and its cascading related session cleanup.
