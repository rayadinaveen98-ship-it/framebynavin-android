-- RC19 disposable CI DATABASE ONLY. Not a Supabase migration; do not deploy.
-- This is a restricted queue proof, NOT the account deletion service.
DO $guard$
BEGIN
  IF current_database() <> 'backlot_rc19_ephemeral' THEN
    RAISE EXCEPTION 'RC19 refuses a non-ephemeral database';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='rc19_worker') THEN
    RAISE EXCEPTION 'RC19 CI worker role must be created by test bootstrap';
  END IF;
END $guard$;

CREATE SCHEMA backlot_rc19_ci;
REVOKE ALL ON SCHEMA backlot_rc19_ci FROM PUBLIC;
GRANT USAGE ON SCHEMA backlot_rc19_ci TO rc19_worker;

CREATE TABLE backlot_rc19_ci.requests (
  request_id text PRIMARY KEY CHECK(request_id ~ '^[A-Za-z0-9_-]{12,100}$'),
  user_id uuid NOT NULL,
  status text NOT NULL DEFAULT 'requested'
    CHECK(status IN ('requested','processing','retry_wait','needs_support','completed')),
  stage text NOT NULL DEFAULT 'fence'
    CHECK(stage IN ('fence','voice_media','heads','snapshots','profile',
                    'verify_data','auth','verify_auth','complete')),
  attempts smallint NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 5),
  retry_after timestamptz NOT NULL DEFAULT '-infinity'::timestamptz,
  lease_token uuid,
  lease_until timestamptz,
  fence_recorded boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  completed_at timestamptz,
  CHECK(status <> 'completed' OR (stage = 'complete' AND completed_at IS NOT NULL)),
  CHECK(status <> 'processing' OR (lease_token IS NOT NULL AND lease_until IS NOT NULL))
);
CREATE INDEX idx_rc19_queue_ready ON backlot_rc19_ci.requests (status, retry_after);
ALTER TABLE backlot_rc19_ci.requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE backlot_rc19_ci.requests FORCE ROW LEVEL SECURITY;
-- Explicit least-privilege grants and worker-only policy on a NON-exposed schema.
REVOKE ALL ON backlot_rc19_ci.requests FROM PUBLIC;
GRANT SELECT, INSERT, UPDATE ON backlot_rc19_ci.requests TO rc19_worker;
CREATE POLICY rc19_worker_only ON backlot_rc19_ci.requests
  FOR ALL TO rc19_worker USING (true) WITH CHECK (true);

CREATE FUNCTION backlot_rc19_ci.submit(p_request text, p_user uuid)
RETURNS backlot_rc19_ci.requests
LANGUAGE plpgsql SECURITY INVOKER SET search_path = ''
AS $fn$
DECLARE row_ backlot_rc19_ci.requests;
BEGIN
  IF p_request !~ '^[A-Za-z0-9_-]{12,100}$' OR p_user IS NULL THEN
    RAISE EXCEPTION 'Invalid deletion request' USING ERRCODE = '22023';
  END IF;
  INSERT INTO backlot_rc19_ci.requests(request_id,user_id)
    VALUES(p_request,p_user)
    ON CONFLICT(request_id) DO NOTHING;
  SELECT * INTO STRICT row_ FROM backlot_rc19_ci.requests WHERE request_id=p_request;
  IF row_.user_id <> p_user THEN
    RAISE EXCEPTION 'Idempotency key already belongs to a different account'
      USING ERRCODE = '42501';
  END IF;
  RETURN row_;
END $fn$;

-- Atomic UPDATE claim; row lock/MVCC ensures exactly one concurrent winner.
CREATE FUNCTION backlot_rc19_ci.claim(
  p_request text, p_now timestamptz, p_ttl_seconds integer
)
RETURNS backlot_rc19_ci.requests
LANGUAGE plpgsql SECURITY INVOKER SET search_path = ''
AS $fn$
DECLARE row_ backlot_rc19_ci.requests;
BEGIN
  IF p_now IS NULL OR p_ttl_seconds NOT BETWEEN 1 AND 300 THEN
    RAISE EXCEPTION 'Invalid lease request' USING ERRCODE='22023';
  END IF;
  UPDATE backlot_rc19_ci.requests q
  SET status='processing', attempts=q.attempts+1,
      lease_token=gen_random_uuid(),
      lease_until=p_now + make_interval(secs => p_ttl_seconds),
      updated_at=p_now
  WHERE q.request_id=p_request AND q.attempts<5 AND
    (
      q.status='requested' OR
      (q.status='retry_wait' AND q.retry_after <= p_now) OR
      (q.status='processing' AND q.lease_until <= p_now)
    )
  RETURNING q.* INTO row_;
  IF row_.request_id IS NOT NULL THEN RETURN row_; END IF;
  -- A worker expired on its fifth attempt: operator intervention, never
  -- silently leave a permanently "processing" receipt.
  UPDATE backlot_rc19_ci.requests q
     SET status='needs_support', lease_token=NULL, lease_until=NULL, updated_at=p_now
   WHERE q.request_id=p_request AND q.status='processing'
     AND q.attempts>=5 AND q.lease_until<=p_now;
  RETURN NULL;
END $fn$;

-- Marker only; does NOT fence real Creator tables or Supabase Storage writes.
CREATE FUNCTION backlot_rc19_ci.record_fence(
 p_request text, p_token uuid, p_now timestamptz
)
RETURNS void LANGUAGE plpgsql SECURITY INVOKER SET search_path = ''
AS $fn$
BEGIN
 UPDATE backlot_rc19_ci.requests
 SET fence_recorded=true,updated_at=p_now
 WHERE request_id=p_request AND lease_token=p_token
   AND status='processing' AND stage='fence' AND lease_until > p_now;
 IF NOT FOUND THEN RAISE EXCEPTION 'Stale lease or wrong stage' USING ERRCODE='42501'; END IF;
END $fn$;

CREATE FUNCTION backlot_rc19_ci.advance(
  p_request text, p_token uuid, p_expected text, p_next text, p_now timestamptz
)
RETURNS void LANGUAGE plpgsql SECURITY INVOKER SET search_path = ''
AS $fn$
DECLARE order_ text[] := ARRAY[
  'fence','voice_media','heads','snapshots','profile',
  'verify_data','auth','verify_auth','complete'
];
BEGIN
 IF array_position(order_,p_expected) IS NULL OR
    p_next IS DISTINCT FROM order_[array_position(order_,p_expected)+1] THEN
   RAISE EXCEPTION 'Invalid deletion stage transition' USING ERRCODE='22023';
 END IF;
 UPDATE backlot_rc19_ci.requests
 SET stage=p_next,updated_at=p_now
 WHERE request_id=p_request AND lease_token=p_token
   AND stage=p_expected AND status='processing' AND lease_until > p_now
   AND (p_expected <> 'fence' OR fence_recorded);
 IF NOT FOUND THEN RAISE EXCEPTION 'Stale token or unconfirmed write fence'
   USING ERRCODE='42501'; END IF;
END $fn$;

CREATE FUNCTION backlot_rc19_ci.defer(
 p_request text, p_token uuid, p_stage text, p_now timestamptz, p_retry_at timestamptz
)
RETURNS void LANGUAGE plpgsql SECURITY INVOKER SET search_path = ''
AS $fn$
BEGIN
 IF p_retry_at <= p_now THEN RAISE EXCEPTION 'Retry must be in future' USING ERRCODE='22023'; END IF;
 UPDATE backlot_rc19_ci.requests
 SET status=CASE WHEN attempts>=5 THEN 'needs_support' ELSE 'retry_wait' END,
     retry_after=p_retry_at,lease_token=NULL,lease_until=NULL,updated_at=p_now
 WHERE request_id=p_request AND lease_token=p_token
   AND stage=p_stage AND status='processing' AND lease_until > p_now;
 IF NOT FOUND THEN RAISE EXCEPTION 'Stale token or wrong stage'
    USING ERRCODE='42501'; END IF;
END $fn$;

CREATE FUNCTION backlot_rc19_ci.complete(
 p_request text, p_token uuid, p_now timestamptz
)
RETURNS void LANGUAGE plpgsql SECURITY INVOKER SET search_path = ''
AS $fn$
BEGIN
 UPDATE backlot_rc19_ci.requests
 SET status='completed',completed_at=p_now,updated_at=p_now,
     lease_token=NULL,lease_until=NULL
 WHERE request_id=p_request AND lease_token=p_token
   AND status='processing' AND stage='complete'
   AND lease_until>p_now AND fence_recorded;
 IF NOT FOUND THEN RAISE EXCEPTION 'Invalid completion claim' USING ERRCODE='42501'; END IF;
END $fn$;

CREATE FUNCTION backlot_rc19_ci.read_owned(p_request text,p_user uuid)
RETURNS backlot_rc19_ci.requests
LANGUAGE sql SECURITY INVOKER SET search_path=''
AS $fn$
  SELECT q FROM backlot_rc19_ci.requests AS q
   WHERE q.request_id=p_request AND q.user_id=p_user
$fn$;

-- No direct Data API access. Even SQL callers have no function rights by default.
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA backlot_rc19_ci FROM PUBLIC;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA backlot_rc19_ci FROM anon,authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA backlot_rc19_ci TO rc19_worker;
