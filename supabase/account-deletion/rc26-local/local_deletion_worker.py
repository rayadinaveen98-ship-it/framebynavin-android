#!/usr/bin/env python3
"""RC26 disposable LOCAL Supabase deletion worker proof, NOT a deployed endpoint.

Uses real local Auth, PostgREST, Storage API. HARD GUARD comes from RC22
test_real_auth_storage: refuses hosted API/database URLs. Never expose worker
resume(id) to untrusted users: it is a privileged internal job operation.
Receipts are durable in isolated local PostgreSQL and survive Auth deletion.
"""
from __future__ import annotations
import sys
from pathlib import Path
from uuid import UUID, uuid4
from urllib.parse import quote

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"rc22-local"))
from test_real_auth_storage import api,sql,ADMIN,ANON,BUCKET  # type: ignore[import-not-found]

CONFIRM="DELETE MY BACKLOT ACCOUNT"
STAGES=("fenced","storage_deleted","data_deleted","auth_deleted","complete")

class DeletionError(RuntimeError): pass

def uid_of(token: str) -> str:
    if not token or token==ADMIN or token==ANON:
        raise DeletionError("Requires end-user Auth bearer; privileged keys forbidden")
    status,body=api("GET","/auth/v1/user",token)
    if status!=200 or not isinstance(body,dict):
        raise DeletionError("Unverified or expired account identity")
    user_id=body.get("id","")
    try: user_id=str(UUID(user_id))
    except (ValueError,TypeError,AttributeError) as e:
        raise DeletionError("Malformed server verified account") from e
    return user_id

def valid_id(value: str) -> str:
    try: return str(UUID(value))
    except (ValueError,TypeError,AttributeError) as e:
        raise DeletionError("Invalid internal receipt identifier") from e

def request_deletion(user_bearer: str, confirmation: str) -> str:
    """Owner identity ALWAYS comes from real Auth /user; never user-supplied id.

    For local QA only: no recent reauth/MFA or durable HTTP delivery endpoint.
    """
    if confirmation!=CONFIRM:
        raise DeletionError("Explicit destructive confirmation required")
    user_id=uid_of(user_bearer)
    request_id=str(uuid4())
    # One database transaction: receipt creation + persistent write fence.
    # This never deletes anything. Repeats before Auth removal return same ID.
    sql(f"""BEGIN;
      INSERT INTO backlot_rc26_ci.requests (id,user_id)
        VALUES ('{request_id}','{user_id}')
        ON CONFLICT (user_id) DO NOTHING;
      UPDATE backlot_rc22_ci.deletion_fences
         SET blocked=true,blocked_at=COALESCE(blocked_at,now())
       WHERE user_id='{user_id}';
      DO $chk$ BEGIN
        IF NOT EXISTS (SELECT 1 FROM backlot_rc22_ci.deletion_fences
                       WHERE user_id='{user_id}' AND blocked)
        THEN RAISE EXCEPTION 'Unregistered local owner'; END IF;
      END $chk$;
      COMMIT;""")
    receipt=sql(f"SELECT id::text FROM backlot_rc26_ci.requests WHERE user_id='{user_id}'")
    return valid_id(receipt)

def receipt_state(receipt_id: str) -> tuple[str,str,str]:
    rid=valid_id(receipt_id)
    row=sql(f"SELECT user_id::text || '|' || stage || '|' || status "
            f"FROM backlot_rc26_ci.requests WHERE id='{rid}'")
    fields=row.split("|")
    if len(fields)!=3: raise DeletionError("No trusted durable deletion receipt")
    return fields[0],fields[1],fields[2]

def transition(receipt_id: str, stage: str) -> None:
    if stage not in STAGES: raise DeletionError("Unknown stage")
    rid=valid_id(receipt_id)
    done=stage=="complete"
    sql(f"UPDATE backlot_rc26_ci.requests "
        f"SET stage='{stage}',status='{'complete' if done else 'pending'}',"
        f"completed_at={'now()' if done else 'NULL'},last_error=NULL "
        f"WHERE id='{rid}'")

def owned_storage_names(user_id: str) -> list[str]:
    """READ-ONLY metadata enumeration. Actual bytes always removed via API.

    Production needs keyset paging, bounded results and race-safe writers.
    """
    uid=valid_id(user_id)
    names=sql("SELECT name FROM storage.objects "
              f"WHERE bucket_id='{BUCKET}' AND name LIKE '{uid}/%' "
              "ORDER BY name LIMIT 100").splitlines()
    for path in names:
        if not path.startswith(uid+"/") or "\n" in path or "\r" in path:
            raise DeletionError("Unsafe Storage object path")
    return names

def purge_storage(owner: str) -> None:
    for batch in range(40): # bounded: 4,000 test files maximum, fail closed
        paths=owned_storage_names(owner)
        if not paths:return
        status,body=api("DELETE",f"/storage/v1/object/{BUCKET}",ADMIN,
                        {"prefixes":paths})
        if status not in (200,204):
            raise DeletionError("Storage API rejected object deletion: "+str(status))
    raise DeletionError("Storage deletion did not converge")

def wipe_creator(owner: str) -> None:
    # Correct FK order: heads first, then snapshots, finally profile.
    for table,field in (
        ("creator_sync_heads","user_id"),("creator_sync_snapshots","user_id"),
        ("creator_profiles","user_id"),("rc22_creator_projects","owner_id")
    ):
        status,_=api("DELETE",f"/rest/v1/{table}?{field}=eq.{owner}",ADMIN)
        if status not in (200,204):
            raise DeletionError("Creator cleanup failed: "+table)
        left=sql(f"SELECT count(*) FROM public.{table} WHERE {field}='{owner}'")
        if left!="0":
            raise DeletionError("Residual Creator rows: "+table)

def remove_auth(owner: str) -> None:
    status,_=api("GET",f"/auth/v1/admin/users/{owner}",ADMIN)
    if status==404:return # retries after the irreversible Auth action
    if status!=200: raise DeletionError("Cannot verify Auth account before removal")
    status,_=api("DELETE",f"/auth/v1/admin/users/{owner}",ADMIN,
                 {"should_soft_delete":False})
    if status not in (200,204):
        raise DeletionError("Auth API deletion failed: "+str(status))
    if sql(f"SELECT count(*) FROM auth.users WHERE id='{owner}'")!="0":
        raise DeletionError("Auth user persisted after deletion")

def resume_internal(receipt_id: str, *, fail_after_fence: bool=False) -> str:
    """Privileged trusted worker ONLY, internal local CI invocation.

    One worker/process in this test; production needs leases, CAS, fencing,
    reauthentication, signed external web request and rate limits.
    """
    rid=valid_id(receipt_id)
    owner,stage,status=receipt_state(rid)
    if status=="complete":return "complete"
    if stage not in STAGES:raise DeletionError("Unknown durable job stage")
    # psql's SQL-command tag can follow RETURNING output; query the numeric
    # counter separately rather than trusting the final printed line.
    previous=sql(f"SELECT attempts FROM backlot_rc26_ci.requests WHERE id='{rid}'")
    if not previous.isdigit() or int(previous)>=5:
        raise DeletionError("Manual review required after retries")
    sql(f"UPDATE backlot_rc26_ci.requests SET attempts=attempts+1,"
        f"status='pending' WHERE id='{rid}' AND attempts<5")
    if sql(f"SELECT blocked::text FROM backlot_rc22_ci.deletion_fences "
           f"WHERE user_id='{owner}'")!="true":
        raise DeletionError("Deletion owner fence missing; fail closed")
    try:
        if fail_after_fence and stage=="fenced":
            raise DeletionError("Injected recoverable failure after fence")
        if stage=="fenced":
            purge_storage(owner)
            transition(rid,"storage_deleted")
            stage="storage_deleted"
        if stage=="storage_deleted":
            if owned_storage_names(owner):
                raise DeletionError("Residual Voice objects remain")
            wipe_creator(owner)
            transition(rid,"data_deleted")
            stage="data_deleted"
        if stage=="data_deleted":
            if owned_storage_names(owner):
                raise DeletionError("Uncleared Voice objects before Auth deletion")
            remove_auth(owner)
            transition(rid,"auth_deleted")
            stage="auth_deleted"
        if stage=="auth_deleted":
            if sql(f"SELECT count(*) FROM auth.users WHERE id='{owner}'")!="0":
                raise DeletionError("Auth user still exists")
            if owned_storage_names(owner):
                raise DeletionError("Voice objects reappeared")
            transition(rid,"complete")
        return receipt_state(rid)[2]
    except Exception:
        # The owner-locked state and durable last successful stage survive
        # process interruption; no raw secret/error text stored in public DB.
        sql(f"UPDATE backlot_rc26_ci.requests "
            f"SET status='retryable',last_error='Local worker step failed' "
            f"WHERE id='{rid}' AND status<>'complete'")
        raise
