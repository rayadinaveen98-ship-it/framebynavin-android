#!/usr/bin/env python3
"""RC29 true local Supabase Auth + actual bucket + live Creator schema contract.

Never runs on hosted projects. Supabase CLI + Deno Edge and local psql only.
Deliberately proves service-role Storage bypass is NOT fixed by Storage RLS.
"""
from __future__ import annotations
import json
import sys
from pathlib import Path
from urllib.parse import quote
from urllib.error import HTTPError
from urllib.request import Request,build_opener

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"rc22-local"))
from test_real_auth_storage import api,sql,user,ADMIN,ANON,denied
from test_real_auth_storage import BASE as LOCAL_API
VOICE="creator-voice-ideas"
ENDPOINT="http://127.0.0.1:8000/"
CONFIRM="DELETE MY BACKLOT ACCOUNT"
checks=0

def check(label,good,detail=None):
    global checks
    if not good:raise AssertionError(f"{label}: {str(detail)[:340]}")
    checks+=1
    print("RC29 PASS:",label,flush=True)

def voice_path(uid,suffix):
    return uid+f"/idea-{suffix}/media/{suffix}.m4a"

def upload(token,pth,data=b"synthetic",upsert=False,method="POST"):
    return api(method,"/storage/v1/object/"+VOICE+"/"+quote(pth,safe="/"),
               token,data,"audio/mp4",{"x-upsert":"true" if upsert else "false"})

def transfer(method,token,src,dst):
    return api("POST","/storage/v1/object/"+method,token,{
        "bucketId":VOICE,"sourceKey":src,"destinationKey":dst})

def edge(token,method="POST",data=None):
    body=json.dumps(data).encode() if data is not None else None
    h={"Authorization":"Bearer "+token,"Content-Type":"application/json"}
    r=Request(ENDPOINT,method=method,data=body,headers=h)
    try:
        with build_opener().open(r,timeout=12) as response:
            return response.status,json.loads(response.read(16384))
    except HTTPError as exc:
        try:return exc.code,json.loads(exc.read(16384))
        finally:exc.close()

def put_rest(table,token,body):
    return api("POST","/rest/v1/"+table,token,body)

def profile(uid,label):
    return {"user_id":uid,"username":"rc29-"+label,"avatar_url":"https://example.invalid/avatar.png",
            "display_name":"Synthetic "+label}

def snapshot(uid,revision):
    sha="a"*64 if revision==1 else "b"*64
    return {"user_id":uid,"revision":revision,
            "base_content_sha256":None,"content_sha256":sha,
            "payload_sha256":"f"*64,"schema_version":1,
            "payload":'{"projects":[]}',
            "captured_at":"2026-10-30T12:00:00Z",
            "app_version":"2.15.7","project_count":0,"idea_count":0,
            "device_id":"rc29-local-device"}

def seed(uid,token,label):
    for table,payload in (
      ("creator_profiles",profile(uid,label)),
      ("creator_sync_snapshots",snapshot(uid,1)),
      ("creator_sync_heads",{"user_id":uid,"revision":1,"content_sha256":"a"*64})
    ):
        status,data=put_rest(table,token,payload)
        check("full live shape "+label+" INSERT "+table,status in (200,201),data)

def main():
    check("verified local Supabase only",
          LOCAL_API.startswith(("http://127.0.0.1:54321","http://localhost:54321")))
    s,response=api("POST","/storage/v1/bucket",ADMIN,
                    {"id":VOICE,"name":VOICE,"public":False,"file_size_limit":1048576})
    check("real local bucket using actual Backlot bucket ID",s in (200,201),response)

    a,ta=user("rc29-owner-a@example.invalid")
    b,tb=user("rc29-owner-b@example.invalid")
    c,tc=user("rc29-no-fence@example.invalid")
    check("three independent verified synthetic Auth identities",len({a,b,c})==3)
    sql(f"INSERT INTO backlot_rc22_ci.deletion_fences(user_id) VALUES('{a}'),('{b}')")
    seed(a,ta,"a")
    seed(b,tb,"b")

    s,d=upload(tc,voice_path(c,"no-fence"))
    check("missing fence fails closed even with valid Auth",denied(s),d)
    s,d=upload(tb,voice_path(a,"cross-account"))
    check("cross-user Voice upload blocked",denied(s),d)

    pa=voice_path(a,"old")
    pb=voice_path(b,"old")
    s,d=upload(ta,pa)
    check("A Voice upload before deletion accepted",s in (200,201),d)
    s,d=upload(tb,pb)
    check("B Voice upload before deletion accepted",s in (200,201),d)

    # Prove restrictive policy cannot be defeated by an additional overly
    # broad PERMISSIVE Storage policy (Postgres OR vs AND semantics).
    sql("CREATE POLICY rc29_local_accidental_permissive "
        "ON storage.objects FOR INSERT TO authenticated "
        "WITH CHECK(bucket_id='creator-voice-ideas')")
    s,result=edge(ta,data={"confirmation":CONFIRM})
    check("actual Deno Edge + Auth PostgREST RPC fences account A",s==202,result)
    check("owner A now blocked and B still active",
          sql("SELECT count(*) FROM backlot_rc22_ci.deletion_fences WHERE blocked")=="1")
    check("real Edge returns private receipt",
          len(result.get("request_id",""))==36 and "user_id" not in result)

    s,d=upload(ta,voice_path(a,"new"))
    check("A new upload rejected after fence despite extra permissive policy",denied(s),d)
    s,d=upload(ta,pa,b"upsert",upsert=True)
    check("A upsert rejected",denied(s),d)
    s,d=upload(ta,pa,b"replace",method="PUT")
    check("A replace rejected",denied(s),d)
    s,d=transfer("copy",ta,pa,voice_path(a,"copy"))
    check("A copy rejected",denied(s),d)
    s,d=transfer("move",ta,pa,voice_path(a,"move"))
    check("A move rejected",denied(s),d)
    s,d=upload(tc,voice_path(c,"extra-permissive"))
    check("third user's missing fence remains blocked",denied(s),d)
    s,d=upload(tb,voice_path(a,"attack"))
    check("broad permissive policy did not permit cross-owner upload",denied(s),d)

    for table,payload in (
        ("creator_profiles",profile(a,"after")),
        ("creator_sync_snapshots",snapshot(a,2)),
        ("creator_sync_heads",{"user_id":a,"revision":2,"content_sha256":"b"*64}),
    ):
        s,d=put_rest(table,ta,payload)
        check("fenced A cannot insert to real-shaped "+table,denied(s),d)
        s,d=put_rest(table,ADMIN,payload)
        check("even privileged database INSERT denied "+table,denied(s),d)
    s,d=api("PATCH","/rest/v1/creator_profiles?user_id=eq."+a,ADMIN,
            {"display_name":"Forbidden privileged update"},
            extra={"Prefer":"return=representation"})
    check("privileged database UPDATE denied after fence",denied(s),d)
    s,d=api("PATCH","/rest/v1/creator_profiles?user_id=eq."+b,tb,
            {"display_name":"B still active"},
            extra={"Prefer":"return=representation"})
    check("unfenced B profile update works",s==200 and len(d)==1,d)
    s,d=upload(tb,voice_path(b,"after"))
    check("B can upload new Voice file",s in (200,201),d)
    s,d=upload(tb,pb,b"updated",upsert=True)
    check("B can still upsert Voice file",s in (200,201),d)
    s,d=transfer("copy",tb,pb,voice_path(b,"copy"))
    check("B can still copy media",s in (200,201),d)
    s,d=transfer("move",tb,voice_path(b,"copy"),voice_path(b,"moved"))
    check("B can still move media",s in (200,201),d)

    # Real proven gap; production activation remains blocked.
    s,d=upload(ADMIN,voice_path(a,"privileged"))
    check("explicit unresolved Storage service-role bypass reproduced",s in (200,201),d)
    check("Storage policy markers are restrictive",
      sql("SELECT count(*) FROM pg_policies WHERE schemaname='storage' "
          "AND tablename='objects' AND policyname LIKE 'rc29 voice %' "
          "AND permissive='RESTRICTIVE'")=="2")
    check("A original file persists and no false deletion was triggered",
       sql(f"SELECT count(*) FROM storage.objects WHERE bucket_id='{VOICE}' "
           f"AND name LIKE '{a}/%'")=="2")
    check("B remains unfenced and has original+new media",
       sql(f"SELECT count(*) FROM storage.objects WHERE bucket_id='{VOICE}' "
           f"AND name LIKE '{b}/%'")=="3")
    check("A's Auth account remains (only request, no internal worker)",
       sql(f"SELECT count(*) FROM auth.users WHERE id='{a}'")=="1")
    check("B's Auth remains untouched",
       sql(f"SELECT count(*) FROM auth.users WHERE id='{b}'")=="1")
    print(f"RC29 REAL LIVE-SHAPE & STORAGE FENCE QA: {checks} assertions passed; "
          "service-role Storage bypass remains an explicit production BLOCKER.",flush=True)

if __name__=="__main__":main()
