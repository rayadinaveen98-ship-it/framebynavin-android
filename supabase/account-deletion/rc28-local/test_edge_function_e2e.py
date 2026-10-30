#!/usr/bin/env python3
"""Actual Deno Edge Function plus two REAL LOCAL Supabase Auth accounts.

Supabase and Deno use loopback-only ports in CI. This script never contacts
the hosted Backlot project or any real user's data.
"""
from __future__ import annotations
import json
import sys
from pathlib import Path
from urllib.request import Request,build_opener,HTTPRedirectHandler
from urllib.error import HTTPError

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"rc22-local"))
from test_real_auth_storage import sql,user,api,uploaded,path,BUCKET,ADMIN,ANON
sys.path.insert(0,str(ROOT/"rc26-local"))
from local_deletion_worker import receipt_state,owned_storage_names,resume_internal

URL="http://127.0.0.1:8000/"
CONFIRM="DELETE MY BACKLOT ACCOUNT"
checks=0
def verify(name,condition,detail=None):
    global checks
    if not condition:raise AssertionError(name+": "+str(detail)[:300])
    checks+=1
    print("RC28 E2E PASS:",name,flush=True)

class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self,req,fp,code,msg,headers,url):
        raise ValueError("Redirect forbidden")

def edge(method="GET",jwt=None,body=None,extras=None,path="/"):
    headers={"Accept":"application/json"}
    if jwt is not None:headers["Authorization"]="Bearer "+jwt
    raw=json.dumps(body).encode("utf-8") if body is not None else None
    if raw is not None:headers["Content-Type"]="application/json"
    headers.update(extras or {})
    assert path.startswith("/") and not path.startswith("//")
    req=Request(URL.rstrip("/")+path,data=raw,headers=headers,method=method)
    try:
        with build_opener(NoRedirect).open(req,timeout=12) as f:
            return f.status,json.loads(f.read(32768)),dict(f.headers)
    except HTTPError as e:
        try:return e.code,json.loads(e.read(32768)),dict(e.headers)
        finally:e.close()

def seed(uid,token,label):
    for table,body in (
        ("creator_profiles",{"user_id":uid,"username":"rc28-"+label}),
        ("creator_sync_snapshots",{"user_id":uid,"revision":1,
            "content_sha256":"a"*64,"payload":"{}"}),
        ("creator_sync_heads",{"user_id":uid,"revision":1,"content_sha256":"a"*64}),
    ):
        s,out=api("POST","/rest/v1/"+table,token,body)
        verify(label+" seeded "+table,s in (200,201),out)
    s,out=uploaded(token,path(uid,"voice"),b"synthetic-voice-data")
    verify(label+" created real Storage voice object",s in (200,201),out)

def main():
    s,body=api("POST","/storage/v1/bucket",ADMIN,
               {"id":BUCKET,"name":BUCKET,"public":False,
                "file_size_limit":1048576})
    verify("real Storage API private bucket created",s in (200,201),body)
    a,ta=user("rc28-deletion-owner@example.invalid")
    b,tb=user("rc28-sibling@example.invalid")
    verify("separate real Supabase Auth users",a!=b and ta!=tb)
    sql(f"INSERT INTO backlot_rc22_ci.deletion_fences(user_id) VALUES('{a}'),('{b}')")
    seed(a,ta,"A")
    seed(b,tb,"B")
    status,data,headers=edge("GET")
    verify("missing Auth gets 401",status==401 and data["error"]=="auth_required")
    status,data,headers=edge("POST","forged-bearer",{"confirmation":CONFIRM})
    verify("forged bearer cannot submit",status==401)
    status,data,headers=edge("POST",ADMIN,{"confirmation":CONFIRM})
    verify("service key cannot impersonate owner",status==401)
    status,data,_=edge("POST",ta,{"confirmation":"DELETE"})
    verify("destructive confirmation required",status==422)
    status,data,_=edge("POST",ta,{"confirmation":CONFIRM,"user_id":b})
    verify("caller cannot target sibling",status==400)
    status,data,_=edge("GET",tb)
    verify("user B has no receipt before A requests",status==200 and data["status"]=="none")
    status,data,hdr=edge("POST",ta,{"confirmation":CONFIRM})
    verify("Edge Function submits real Auth JWT owner-scoped DB RPC",status==202,data)
    reqid=data["request_id"]
    verify("private receipt stored by SQL and tied to A",
           receipt_state(reqid)==(a,"fenced","pending"))
    verify("no private data in response and cache disabled",
           a not in json.dumps(data) and b not in json.dumps(data) and
           "no-store" in hdr.get("Cache-Control",""))
    status,again,_=edge("POST",ta,{"confirmation":CONFIRM})
    verify("repeated request idempotent",status==202 and again["request_id"]==reqid)
    status,status_a,_=edge("GET",ta)
    verify("A can read own status before worker",status==200 and status_a["request_id"]==reqid)
    status,status_b,_=edge("GET",tb)
    verify("B cannot read A receipt",status==200 and status_b=={"status":"none"})
    s,out=uploaded(ta,path(a,"after"),b"blocked")
    verify("fenced A Storage upload blocked",s in (400,401,403,422),out)
    s,out=uploaded(tb,path(b,"after"),b"allowed")
    verify("B Storage upload still possible",s in (200,201),out)
    verify("Edge request does NOT delete Auth or files",
           len(owned_storage_names(a))==1 and
           sql(f"SELECT count(*) FROM auth.users WHERE id='{a}'")=="1")
    try:
        resume_internal(reqid,fail_after_fence=True)
        raise AssertionError("Fault injection was not observed")
    except RuntimeError as exc:
        verify("separate worker injected failure and persisted receipt",
               "Injected" in str(exc) and
               receipt_state(reqid)==(a,"fenced","retryable"))
    status,rett,_=edge("GET",ta)
    verify("Edge status reports retryable non-completion",
           status==200 and rett["status"]=="retryable")
    verify("internal worker deletes real local A",
           resume_internal(reqid)=="complete")
    verify("A Auth and voice files removed",
           sql(f"SELECT count(*) FROM auth.users WHERE id='{a}'")=="0" and
           owned_storage_names(a)==[])
    verify("B Auth and voice files preserved",
           sql(f"SELECT count(*) FROM auth.users WHERE id='{b}'")=="1" and
           len(owned_storage_names(b))==2)
    verify("receipt completion survives Auth deletion",
           receipt_state(reqid)==(a,"complete","complete"))
    verify("internal replay is idempotent",resume_internal(reqid)=="complete")
    # Creation timestamp is NOT token refresh; stale sessions are rejected.
    sql(f"UPDATE auth.sessions SET created_at=clock_timestamp()-interval '10 minutes' "
        f"WHERE user_id='{b}'")
    status,out,_=edge("POST",tb,{"confirmation":CONFIRM})
    verify("stale Auth session rejected by SQL",status==503,out)
    verify("B never fenced by rejected stale session",
           sql(f"SELECT blocked::text FROM backlot_rc22_ci.deletion_fences "
               f"WHERE user_id='{b}'")=="false")
    print(f"RC28 REAL EDGE/LOCAL SUPABASE E2E PASS: {checks} checks; A deleted, B intact.",flush=True)
if __name__=="__main__":main()
