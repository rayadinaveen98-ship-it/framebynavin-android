#!/usr/bin/env python3
"""RC24 two-account local Auth/PostgREST write-fence regression only.

Non-production local Supabase, no hosted project and no deletion of any row.
"""
import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"rc22-local"))
from test_real_auth_storage import run as base_run, api, sql, ANON, ADMIN

checks=0
def verify(name,condition,detail=""):
    global checks
    if not condition: raise AssertionError(name+": "+str(detail)[:250])
    checks+=1
    print("RC24 PASS:",name,flush=True)

def identity(email):
    s,data=api("POST","/auth/v1/token?grant_type=password",ANON,
       {"email":email,"password":"rc22-local-test-only-Password77!"})
    verify("local signed-in user "+email,s==200,data)
    token=data["access_token"]
    s,user=api("GET","/auth/v1/user",token)
    verify("verified local Auth identity "+email,s==200,user)
    return user["id"],token

def write(method,table,token,body,where=""):
    return api(method,"/rest/v1/"+table+where,token,body,
               extra={"Prefer":"return=representation"})

def denied(status): return status in (400,401,403,409,422)

def main():
    base_run() # 36 actual LOCAL Supabase Auth, PostgREST and Storage checks
    a,ta=identity("rc22-a@example.invalid")
    b,tb=identity("rc22-b@example.invalid")
    verify("two separate Auth identities",a!=b)
    verify("all three Creator triggers installed",
        sql("select count(*) from pg_trigger where tgname in "
            "('rc24_guard_profiles','rc24_guard_snapshots','rc24_guard_heads')")=="3")
    # A is initially fenced by base_run(). Reopen only these synthetic local
    # accounts to seed existing project/backup records, then fence again.
    sql(f"update backlot_rc22_ci.deletion_fences set blocked=false where user_id='{a}'")
    for uid,token,label in ((a,ta,"A"),(b,tb,"B")):
        for table,body in (
            ("creator_profiles",{"user_id":uid,"username":label.lower()}),
            ("creator_sync_snapshots",{"user_id":uid,"revision":1,
                "content_sha256":"a"*64,"payload":"{}"}),
            ("creator_sync_heads",{"user_id":uid,"revision":1,
                "content_sha256":"a"*64}),
        ):
            s,out=write("POST",table,token,body)
            verify(label+" pre-fence "+table,s in (200,201),out)
    s,out=write("POST","creator_profiles",tb,{"user_id":a,"username":"cross"})
    verify("B denied cross-account profile write",denied(s),out)
    sql(f"update backlot_rc22_ci.deletion_fences set blocked=true,blocked_at=now() where user_id='{a}'")
    verify("A locked; B remains active",
        sql("select count(*) from backlot_rc22_ci.deletion_fences where blocked")=="1")
    for token,label in ((ta,"user"),(ADMIN,"privileged")):
        for table,body in (
            ("creator_profiles",{"user_id":a,"username":"blocked"}),
            ("creator_sync_snapshots",{"user_id":a,"revision":2,
                "content_sha256":"b"*64,"payload":"{}"}),
            ("creator_sync_heads",{"user_id":a,"revision":2,
                "content_sha256":"b"*64}),
        ):
            s,out=write("POST",table,token,body)
            verify(label+" insert blocked "+table,denied(s),out)
        for table,body,suffix in (
            ("creator_profiles",{"display_name":"no"},""),
            ("creator_sync_snapshots",{"payload":"no"},"&revision=eq.1"),
            ("creator_sync_heads",{"content_sha256":"c"*64},""),
        ):
            s,out=write("PATCH",table,token,body,f"?user_id=eq.{a}"+suffix)
            verify(label+" update blocked "+table,denied(s),out)
    s,out=write("PATCH","creator_profiles",tb,
       {"display_name":"still active"},f"?user_id=eq.{b}")
    verify("B profile update still works",s==200 and len(out)==1,out)
    s,out=write("POST","creator_sync_snapshots",tb,
       {"user_id":b,"revision":2,"content_sha256":"c"*64,"payload":"{}"})
    verify("B new snapshot still works",s in (200,201),out)
    s,out=write("PATCH","creator_sync_heads",tb,
       {"revision":2,"content_sha256":"c"*64},f"?user_id=eq.{b}")
    verify("B sync head update still works",s==200 and len(out)==1,out)
    verify("A existing backup was not erased",
       sql(f"select count(*) from public.creator_sync_snapshots where user_id='{a}'")=="1")
    verify("B existing profile still present",
       sql(f"select count(*) from public.creator_profiles where user_id='{b}'")=="1")
    print(f"RC24 PASS {checks} local Creator DB checks; 0 hosted changes",flush=True)

if __name__=="__main__": main()
