#!/usr/bin/env python3
"""End-to-end REAL local Supabase Auth/Storage/Creator account deletion safety.

Uses two never-real synthetic Auth users. Does NOT deploy endpoint to any
hosted Supabase project or connect the public website. Test order is fixed.
"""
from __future__ import annotations
from local_deletion_worker import (CONFIRM,DeletionError,request_deletion,
                                   resume_internal,receipt_state,owned_storage_names)
import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"rc22-local"))
from test_real_auth_storage import (api,sql,user,uploaded,path,BUCKET,ADMIN,ANON)

checks=0
def check(label,ok,detail=""):
    global checks
    if not ok:raise AssertionError(label+" :: "+str(detail)[:350])
    checks+=1
    print("RC26 PASS:",label,flush=True)

def post(table,token,body):
    return api("POST","/rest/v1/"+table,token,body,extra={"Prefer":"return=minimal"})

def seed_account(uid,token,tag):
    for table,body in (
        ("creator_profiles",{"user_id":uid,"username":f"rc26-{tag}"}),
        ("creator_sync_snapshots",{"user_id":uid,"revision":1,
                                   "content_sha256":"a"*64,"payload":'{"test":true}'}),
        ("creator_sync_heads",{"user_id":uid,"revision":1,"content_sha256":"a"*64}),
        ("rc22_creator_projects",{"id":"rc26-"+tag+"-project","owner_id":uid,
                                  "title":"private test project"}),
    ):
        status,out=post(table,token,body)
        check(tag+" seeded "+table,status in (200,201),out)

def must_reject(action):
    try: action()
    except DeletionError:return True
    return False

def main():
    s,d=api("POST","/storage/v1/bucket",ADMIN,
             {"id":BUCKET,"name":BUCKET,"public":False,"file_size_limit":1048576})
    if s==409 or (isinstance(d,dict) and d.get("code")=="BucketAlreadyExists"
                  and str(d.get("statusCode"))=="409"):
        # RC30 may run another isolated two-user regression against the same
        # disposable local bucket. It must already be PRIVATE and scoped.
        lookup,bucket=api("GET",f"/storage/v1/bucket/{BUCKET}",ADMIN)
        check("reused existing PRIVATE local test bucket only",
              lookup==200 and bucket.get("id")==BUCKET and
              bucket.get("public") is False,d)
    else:
        check("created a private bucket through real Storage API",s in (200,201),d)
    a,ta=user("rc26-delete-me@example.invalid")
    b,tb=user("rc26-keep-me@example.invalid")
    check("real Auth created different users",a!=b and ta!=tb)
    sql(f"INSERT INTO backlot_rc22_ci.deletion_fences(user_id) VALUES('{a}'),('{b}')")
    seed_account(a,ta,"a")
    seed_account(b,tb,"b")
    for uid,token,tag in ((a,ta,"a"),(b,tb,"b")):
        for suffix in (("x","y") if tag=="a" else ("z",)):
            s,d=uploaded(token,path(uid,suffix),b"tiny synthetic voice")
            check(tag+" real Storage object "+suffix,s in (200,201),d)
    check("pre-delete A files are present",len(owned_storage_names(a))==2)
    check("pre-delete B file is present",len(owned_storage_names(b))==1)
    check("unverified bearer is denied",
          must_reject(lambda:request_deletion("forged-token",CONFIRM)))
    check("service-role bearer cannot impersonate a requester",
          must_reject(lambda:request_deletion(ADMIN,CONFIRM)))
    check("explicit destructive confirmation required",
          must_reject(lambda:request_deletion(ta,"DELETE")))
    check("no unauthenticated direct receipt read",
          sql("SELECT has_table_privilege('authenticated',"
              "'backlot_rc26_ci.requests','SELECT')::text")=="false")
    receipt=request_deletion(ta,CONFIRM)
    check("real owner request created private receipt",
          receipt_state(receipt)==(a,"fenced","pending"))
    check("repeat request is owner idempotent",
          request_deletion(ta,CONFIRM)==receipt)
    check("server write fence is now active for A",
          sql(f"SELECT blocked::text FROM backlot_rc22_ci.deletion_fences "
              f"WHERE user_id='{a}'")=="true")
    s,d=uploaded(ta,path(a,"new"),b"blocked")
    check("A cannot upload after fence through real Storage",s in (400,401,403,422),d)
    s,d=uploaded(tb,path(b,"still-valid"),b"valid")
    check("B continues uploading during A deletion",s in (200,201),d)
    check("A's data has NOT been deleted just by requesting",
          sql(f"SELECT count(*) FROM public.creator_sync_snapshots WHERE user_id='{a}'")=="1")

    try:
        resume_internal(receipt,fail_after_fence=True)
        raise AssertionError("Failure injection unexpectedly succeeded")
    except DeletionError as ex:
        check("injected worker failure stays non-final", "Injected" in str(ex))
    check("failed job retains owned receipt",
          receipt_state(receipt)==(a,"fenced","retryable"))
    check("failed job leaves A files intact (recoverable)",
          len(owned_storage_names(a))==2)

    check("retry completes entire real Storage/DB/Auth deletion",
          resume_internal(receipt)=="complete")
    check("durable completion remains after Auth user removed",
          receipt_state(receipt)==(a,"complete","complete"))
    check("replaying completed job does not delete sibling",
          resume_internal(receipt)=="complete")
    check("A Voice object bytes removed via real Storage API",
          owned_storage_names(a)==[])
    check("B retained two private voice objects",
          len(owned_storage_names(b))==2)
    for table in ("creator_profiles","creator_sync_snapshots",
                  "creator_sync_heads","rc22_creator_projects"):
        owner_col="owner_id" if table=="rc22_creator_projects" else "user_id"
        check("A rows removed "+table,
          sql(f"SELECT count(*) FROM public.{table} WHERE {owner_col}='{a}'")=="0")
        check("B rows retained "+table,
          sql(f"SELECT count(*) FROM public.{table} WHERE {owner_col}='{b}'")=="1")
    check("A Auth record actually removed",
          sql(f"SELECT count(*) FROM auth.users WHERE id='{a}'")=="0")
    check("B Auth record retained",
          sql(f"SELECT count(*) FROM auth.users WHERE id='{b}'")=="1")
    s,d=api("GET","/auth/v1/admin/users/"+a,ADMIN)
    check("real Auth admin API no longer finds A",s==404,d)
    s,d=api("GET","/auth/v1/admin/users/"+b,ADMIN)
    check("real Auth admin API still finds B",s==200,d)
    s,d=api("POST","/auth/v1/token?grant_type=password",ANON,
            {"email":"rc26-delete-me@example.invalid",
             "password":"rc22-local-test-only-Password77!"})
    check("deleted A cannot sign in again",s in (400,401,403,404,422),d)
    s,d=api("POST","/auth/v1/token?grant_type=password",ANON,
            {"email":"rc26-keep-me@example.invalid",
             "password":"rc22-local-test-only-Password77!"})
    check("B can still sign in",s==200,d)
    check("recipient untouched and completion receipts have no auth FK",
          sql("SELECT count(*) FROM pg_constraint WHERE conrelid="
              "'backlot_rc26_ci.requests'::regclass AND contype='f'")=="0")
    print(f"RC26 REAL LOCAL E2E DELETION PASSED: {checks} assertions, "
          "Auth/Storage/Creator data actually cleared for synthetic A; "
          "B untouched; zero hosted calls.",flush=True)

if __name__=="__main__":
    main()
