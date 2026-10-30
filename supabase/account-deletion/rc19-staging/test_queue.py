#!/usr/bin/env python3
"""RC19 isolated REAL Postgres assertions. Uses CI-only synthetic UUIDs.

Must run against the disposable backlot_rc19_ephemeral database. No Supabase
connection, real account, storage media or production credentials are used.
"""
import concurrent.futures
import os
import subprocess

DB = "backlot_rc19_ephemeral"
OWNER = "7eb549e3-b454-4acd-9bba-dc1cf7d6ba73"
OTHER = "c719ec81-a544-456b-ac02-1c5039c77739"
NOW = "2026-10-30T10:00:00+00"
STAGES = ["fence", "voice_media", "heads", "snapshots", "profile",
          "verify_data", "auth", "verify_auth", "complete"]
checks = 0


def psql(query, *, role="rc19_worker", should_fail=False):
    if os.environ.get("PGDATABASE") != DB:
        raise RuntimeError("Refusing to run outside the disposable RC19 database")
    cmd = ["psql", "-X", "-q", "-t", "-A", "-v", "ON_ERROR_STOP=1"]
    if role:
        query = "SET ROLE " + role + "; " + query
    result = subprocess.run(cmd + ["-c", query], capture_output=True, text=True,
                            check=False, timeout=25)
    if should_fail:
        assert result.returncode != 0, "Forbidden SQL unexpectedly succeeded"
        return "rejected"
    if result.returncode:
        raise AssertionError("Postgres command failed: " + result.stderr[:1700])
    output = [s.strip() for s in result.stdout.splitlines() if s.strip()]
    return output[-1] if output else ""


def assert_equal(actual, expected, name):
    global checks
    assert actual == expected, f"{name}: {actual!r} != {expected!r}"
    checks += 1
    print("PASS:", name, flush=True)


def submit(request, user=OWNER):
    return psql("SELECT (backlot_rc19_ci.submit('%s','%s')).user_id" % (request, user))


def claim(request, timestamp=NOW, ttl=120):
    return psql("SELECT COALESCE(((backlot_rc19_ci.claim('%s','%s',%d)).lease_token)::text,'')"
                % (request, timestamp, ttl))


def stage(request):
    return psql("SELECT status||'|'||stage||'|'||attempts::text "
                "FROM backlot_rc19_ci.requests WHERE request_id='%s'" % request)


def run():
    # CI installs this schema as postgres but all normal queue access is
    # tested using SET ROLE rc19_worker (non-superuser with RLS enforcement).
    assert_equal(psql("SELECT current_database()", role=None), DB,
                 "ephemeral database guard")
    assert_equal(psql("SELECT rolbypassrls::text FROM pg_roles "
                      "WHERE rolname='rc19_worker'", role=None), "false",
                 "worker cannot bypass RLS")
    assert_equal(psql("SELECT relrowsecurity::text||'|'||relforcerowsecurity::text "
                      "FROM pg_class WHERE oid='backlot_rc19_ci.requests'::regclass",
                      role=None), "true|true", "RLS and FORCE RLS active")
    assert_equal(psql("SELECT has_schema_privilege('anon','backlot_rc19_ci','USAGE')::text",
                      role=None), "false", "anon cannot access private schema")
    assert_equal(psql("SELECT has_schema_privilege('authenticated','backlot_rc19_ci','USAGE')::text",
                      role=None), "false", "authenticated cannot access private schema")
    assert_equal(psql("SELECT has_table_privilege('anon',"
                      "'backlot_rc19_ci.requests','SELECT')::text", role=None),
                 "false", "anon denied table grant")
    assert_equal(psql("SELECT has_function_privilege('anon',"
                      "'backlot_rc19_ci.claim(text,timestamptz,integer)',"
                      "'EXECUTE')::text", role=None), "false",
                 "anon denied worker claim function")
    assert_equal(psql("SELECT count(*) FROM backlot_rc19_ci.requests",
                      role="anon", should_fail=True), "rejected",
                 "untrusted SQL role cannot SELECT job data")
    assert_equal(psql("SELECT count(*) FROM backlot_rc19_ci.requests",
                      role="authenticated", should_fail=True), "rejected",
                 "signed-in client role cannot directly SELECT job data")

    request = "rc19-owner-request-01"
    assert_equal(submit(request), OWNER, "first owner request persisted")
    assert_equal(submit(request), OWNER, "same request idempotent for same owner")
    assert_equal(psql("SELECT (backlot_rc19_ci.submit('%s','%s')).user_id" %
                      (request, OTHER), should_fail=True), "rejected",
                 "request ID cannot be rebound to different user")
    assert_equal(psql("SELECT COALESCE((backlot_rc19_ci.read_owned('%s','%s')).request_id,'')"
                      % (request, OTHER)), "", "sibling request status not exposed")
    assert_equal(psql("SELECT (backlot_rc19_ci.read_owned('%s','%s')).request_id" %
                      (request, OWNER)), request, "verified owner may read request")
    assert_equal(psql("SELECT (backlot_rc19_ci.submit('invalid','%s')).request_id" %
                      OWNER, should_fail=True), "rejected",
                 "invalid opaque request id rejected")

    # Eight genuinely concurrent PostgreSQL client processes compete for
    # exactly one claim. A single atomic UPDATE must win.
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        tokens = list(executor.map(lambda _: claim(request), range(8)))
    winner = [token for token in tokens if token]
    assert_equal(len(winner), 1, "eight concurrent workers -> one lease winner")
    token = winner[0]
    assert_equal(stage(request), "processing|fence|1", "leased job attempts persisted")

    # No stage advancement before the DB marker, and no stale token.
    assert_equal(psql(
        "SELECT backlot_rc19_ci.advance('%s','%s','fence','voice_media','%s')"
        % (request, token, NOW), should_fail=True), "rejected",
        "unconfirmed write fence blocks stage advancement")
    assert_equal(psql(
        "SELECT backlot_rc19_ci.record_fence('%s',"
        "'aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa','%s')"
        % (request, NOW), should_fail=True), "rejected",
        "forged worker token cannot mark write fence")
    psql("SELECT backlot_rc19_ci.record_fence('%s','%s','%s')" %
         (request, token, NOW))
    assert_equal(psql("SELECT fence_recorded::text FROM backlot_rc19_ci.requests "
                      "WHERE request_id='%s'" % request),
                 "true", "write fence marker persisted")
    assert_equal(psql(
        "SELECT backlot_rc19_ci.advance('%s','%s','fence','auth','%s')"
        % (request, token, NOW), should_fail=True), "rejected",
        "stage skipping blocked")
    for current, nxt in zip(STAGES, STAGES[1:]):
        psql("SELECT backlot_rc19_ci.advance('%s','%s','%s','%s','%s')" %
             (request, token, current, nxt, NOW))
    assert_equal(stage(request), "processing|complete|1",
                 "stage progression reaches completed checkpoint")
    psql("SELECT backlot_rc19_ci.complete('%s','%s','%s')" %
         (request, token, NOW))
    assert_equal(psql("SELECT status||'|'||(completed_at IS NOT NULL)::text "
                      "FROM backlot_rc19_ci.requests WHERE request_id='%s'" %
                      request), "completed|true", "completion receipt persisted")
    assert_equal(claim(request, "2026-11-01T10:00:00+00"), "",
                 "completed request is not claimed again")

    # Independent lease recovery and stale owner token fencing.
    second = "rc19-lease-recovery-02"
    submit(second)
    old = claim(second, NOW, 30)
    assert_equal(claim(second, "2026-10-30T10:00:20+00", 30), "",
                 "unexpired worker lease excludes second claim")
    latest = claim(second, "2026-10-30T10:00:31+00", 30)
    assert_equal(bool(latest and latest != old), True,
                 "expired lease produces a different fencing token")
    assert_equal(psql("SELECT backlot_rc19_ci.record_fence('%s','%s','%s')" %
                      (second, old, "2026-10-30T10:00:31+00"), should_fail=True),
                 "rejected", "expired worker cannot mutate job")
    psql("SELECT backlot_rc19_ci.record_fence('%s','%s','%s')" %
         (second, latest, "2026-10-30T10:00:31+00"))
    psql("SELECT backlot_rc19_ci.defer('%s','%s','fence','%s','%s')" %
         (second, latest, "2026-10-30T10:00:32+00",
          "2026-10-30T10:02:00+00"))
    assert_equal(stage(second), "retry_wait|fence|2",
                 "retry state and attempt count persisted")
    assert_equal(claim(second, "2026-10-30T10:01:59+00"), "",
                 "backoff prevents early claim")
    assert_equal(bool(claim(second, "2026-10-30T10:02:01+00")), True,
                 "eligible retry is claimable after delay")

    third = "rc19-support-fail-03"
    submit(third)
    for i in range(5):
        when = "2026-11-%02dT10:00:00+00" % (i + 1)
        future = "2026-11-%02dT10:05:00+00" % (i + 1)
        lease = claim(third, when)
        assert lease, "expected retry #%d to be claimable" % (i + 1)
        psql("SELECT backlot_rc19_ci.defer('%s','%s','fence','%s','%s')" %
             (third, lease, when, future))
    assert_equal(stage(third), "needs_support|fence|5",
                 "fifth failure raises durable support state")
    assert_equal(claim(third, "2027-12-01T10:00:00+00"), "",
                 "support escalation is not retried forever")

    # Simulate an Auth provider identity disappearing. Receipt must remain,
    # because the queue DOES NOT foreign-key to auth.users.
    psql("CREATE TABLE backlot_rc19_ci.synthetic_auth(id uuid PRIMARY KEY)",
         role=None)
    psql("INSERT INTO backlot_rc19_ci.synthetic_auth VALUES('%s')" % OWNER,
         role=None)
    psql("DELETE FROM backlot_rc19_ci.synthetic_auth WHERE id='%s'" % OWNER,
         role=None)
    assert_equal(psql("SELECT count(*) FROM backlot_rc19_ci.requests WHERE user_id='%s'" %
                      OWNER), "3", "queue receipt survives synthetic Auth deletion")
    assert_equal(psql("SELECT count(*) FROM pg_constraint WHERE conrelid="
                      "'backlot_rc19_ci.requests'::regclass AND contype='f'",
                      role=None), "0", "deletion queue has no Auth FK cascade")

    # Fifth-attempt expired lease: claim must escalate to needs_support.
    fourth = "rc19-crashed-worker-04"
    submit(fourth)
    for i in range(4):
        when = "2026-12-%02dT10:00:00+00" % (i + 1)
        later = "2026-12-%02dT10:05:00+00" % (i + 1)
        lease = claim(fourth, when, 5)
        psql("SELECT backlot_rc19_ci.defer('%s','%s','fence','%s','%s')" %
             (fourth, lease, when, later))
    last = claim(fourth, "2026-12-05T10:00:00+00", 5)
    assert_equal(bool(last), True, "fifth crash-prone lease claimed")
    assert_equal(claim(fourth, "2026-12-05T10:00:06+00"), "",
                 "expired final lease not reclaimed")
    assert_equal(stage(fourth), "needs_support|fence|5",
                 "expired fifth lease escalates to manual support")

    print("RC19 POSTGRES PASS: %d independently asserted safety properties" % checks,
          flush=True)


if __name__ == "__main__":
    run()
