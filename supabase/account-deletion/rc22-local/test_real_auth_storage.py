#!/usr/bin/env python3
"""RC22: REAL local Supabase Auth/PostgREST/Storage HTTP tests; synthetic identities.

HARD SAFETY: refuses non-loopback API/DB URLs, any unexpected redirect, and
missing explicit CI marker. No cloud credentials are loaded. Does not delete
Auth users: immutable Build269 and production Supabase remain untouched.
"""
import json
import os
import re
import subprocess
import urllib.error
import urllib.parse
import urllib.request

BUCKET="backlot-rc22-ci"
UUID=re.compile(r"^[0-9a-f]{8}-[0-9a-f-]{27}$",re.I)
checks=0

def local_url(value, *, require_db=False):
    if not value: raise RuntimeError("Local Supabase URL missing")
    p=urllib.parse.urlsplit(value)
    if p.hostname not in ("127.0.0.1","localhost") or p.port not in (54321,54322):
        raise RuntimeError("REFUSING hosted Supabase or unexpected port")
    if require_db and p.scheme not in ("postgresql","postgres"):
        raise RuntimeError("Expected local PostgreSQL URL")
    if not require_db and p.scheme!="http":
        raise RuntimeError("Expected local HTTP")
    if p.username and not require_db: raise RuntimeError("No API credentials in URL")
    return value.rstrip("/")

if os.environ.get("BACKLOT_RC22_LOCAL_ONLY")!="true":
    raise RuntimeError("RC22 CI-only marker missing")
BASE=local_url(os.environ.get("API_URL",""))
DB=local_url(os.environ.get("DB_URL",""),require_db=True)
ANON=os.environ.get("ANON_KEY","")
ADMIN=os.environ.get("SERVICE_ROLE_KEY","")
if not (ANON and ADMIN and ANON!=ADMIN):
    raise RuntimeError("Missing separate local Supabase keys")
if "supabase.co" in BASE or "supabase.co" in DB:
    raise RuntimeError("A hosted project is strictly forbidden")

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self,req,fp,code,msg,headers,newurl):
        raise RuntimeError("Refusing any redirect")
opener=urllib.request.build_opener(NoRedirect)

def api(method,path,token,payload=None,content_type="application/json",extra=None):
    assert path.startswith("/") and not path.startswith("//")
    headers={"apikey":ANON if token!=ADMIN else ADMIN,
             "Authorization":"Bearer "+token,
             "Content-Type":content_type,
             "Accept":"application/json"}
    if extra: headers.update(extra)
    data=(json.dumps(payload).encode() if isinstance(payload,dict) else payload)
    req=urllib.request.Request(BASE+path,data=data,headers=headers,method=method)
    try:
        with opener.open(req,timeout=14) as r:
            raw=r.read(65536)
            return r.status, (json.loads(raw) if raw and "json" in r.headers.get("Content-Type","") else raw)
    except urllib.error.HTTPError as err:
        raw=err.read(65536)
        try: detail=json.loads(raw)
        except (ValueError,UnicodeDecodeError): detail=raw.decode(errors="replace")
        return err.code,detail

def expect(name,condition,explanation=""):
    global checks
    if not condition:
        raise AssertionError(name+": "+explanation)
    checks+=1
    print("PASS:",name,flush=True)

def user(email):
    # Local ephemeral Auth API, NOT the Supabase cloud Admin API.
    status,data=api("POST","/auth/v1/admin/users",ADMIN,
                    {"email":email,"password":"rc22-local-test-only-Password77!",
                     "email_confirm":True})
    expect("local Auth created "+email,status in (200,201),str(data)[:350])
    uid=data.get("id","")
    expect("Auth identity UUID "+email,bool(UUID.fullmatch(uid)))
    s,t=api("POST","/auth/v1/token?grant_type=password",ANON,
            {"email":email,"password":"rc22-local-test-only-Password77!"})
    expect("local Auth granted user token "+email,s==200,str(t)[:350])
    access=t.get("access_token","")
    expect("issued distinct scoped access token "+email,bool(access and access!=ADMIN))
    s,identity=api("GET","/auth/v1/user",access)
    expect("Auth getUser independently resolved "+email,s==200 and identity.get("id")==uid)
    return uid,access

def sql(stmt):
    # Safe only because DB_URL is loopback and all UUIDs are verified.
    result=subprocess.run(["psql",DB,"-X","-t","-A",
          "-v","ON_ERROR_STOP=1","-c",stmt],
          capture_output=True,text=True,timeout=20)
    if result.returncode:
        raise AssertionError("Local DB SQL failed: "+result.stderr[:500])
    return result.stdout.strip()

def path(uid,suffix):
    assert UUID.fullmatch(uid)
    return f"{uid}/idea-{suffix}/media/{suffix}.m4a"

def uploaded(token,pth,data=b"abc",upsert=False):
    url="/storage/v1/object/"+BUCKET+"/"+urllib.parse.quote(pth,safe="/")
    return api("POST",url,token,data,"audio/mp4",{"x-upsert":"true" if upsert else "false"})

def changed(token,pth,data):
    return api("PUT","/storage/v1/object/"+BUCKET+"/"+urllib.parse.quote(pth,safe="/"),
               token,data,"audio/mp4")

def transfer(op,token,src,dst):
    return api("POST","/storage/v1/object/"+op,token,
               {"bucketId":BUCKET,"sourceKey":src,"destinationKey":dst})

def project(token,uid,key):
    return api("POST","/rest/v1/rc22_creator_projects",token,
       {"id":key,"owner_id":uid,"title":"Synthetic RC22 Creator"},
       extra={"Prefer":"return=minimal"})

def update_project(token,key):
    return api("PATCH","/rest/v1/rc22_creator_projects?id=eq."+key,token,
               {"title":"Changed from local Auth"},extra={"Prefer":"return=representation"})

def denied(status):
    return status in (400,401,403,404,409,422)

def run():
    expect("cloud-free local HTTP binding", BASE.startswith("http://127.0.0.1:") or
         BASE.startswith("http://localhost:"))
    expect("cloud-free local DB binding",urllib.parse.urlsplit(DB).hostname in ("localhost","127.0.0.1"))
    s,b=api("POST","/storage/v1/bucket",ADMIN,
            {"id":BUCKET,"name":BUCKET,"public":False,
             "file_size_limit":1048576})
    expect("real local Storage created a private bucket",s in (200,201),str(b)[:400])
    a,ta=user("rc22-a@example.invalid")
    b,tb=user("rc22-b@example.invalid")
    expect("two independently verified local Auth users",a!=b and ta!=tb)

    sql(f"INSERT INTO backlot_rc22_ci.deletion_fences(user_id) VALUES('{a}'),('{b}')")
    expect("unblocked Creator A insert",project(ta,a,"rc22-a-before")[0] in (200,201))
    expect("unblocked Creator B insert",project(tb,b,"rc22-b-before")[0] in (200,201))
    s,bad=project(tb,a,"rc22-cross-attempt")
    expect("real PostgREST blocks cross-owner project insert",denied(s),str(bad)[:300])
    pa=path(a,"old")
    pb=path(b,"old")
    s,out=uploaded(ta,pa)
    expect("real Storage API A upload pre-fence",s in (200,201),str(out)[:320])
    s,out=uploaded(tb,pb)
    expect("real Storage API B upload pre-fence",s in (200,201),str(out)[:320])
    s,out=uploaded(tb,path(a,"intrude"))
    expect("real Storage API blocks cross-account upload",denied(s),str(out)[:300])

    # Durable ADMIN-controlled fence in the local PostgreSQL instance only.
    sql(f"UPDATE backlot_rc22_ci.deletion_fences SET blocked=true,blocked_at=now() WHERE user_id='{a}'")
    expect("A blocked while B stays active",
       sql("SELECT string_agg(user_id::text||':'||blocked::text,',') "
           "FROM backlot_rc22_ci.deletion_fences").count(":true")==1)
    s,out=project(ta,a,"rc22-a-after")
    expect("real PostgREST rejects A Creator insert after fence",denied(s),str(out)[:300])
    s,out=update_project(ta,"rc22-a-before")
    expect("real PostgREST rejects A Creator update after fence",denied(s),str(out)[:300])
    s,out=project(tb,b,"rc22-b-after")
    expect("real PostgREST allows B Creator insert after A fence",s in (200,201),str(out)[:300])
    s,out=update_project(tb,"rc22-b-before")
    expect("real PostgREST allows B Creator update",s==200 and len(out)==1,str(out)[:350])

    s,out=uploaded(ta,path(a,"new"))
    expect("real Storage denies A new upload",denied(s),str(out)[:300])
    s,out=uploaded(ta,pa,b"updated",upsert=True)
    expect("real Storage denies A upsert",denied(s),str(out)[:300])
    s,out=changed(ta,pa,b"updated")
    expect("real Storage denies A replace/update",denied(s),str(out)[:300])
    s,out=transfer("copy",ta,pa,path(a,"copy"))
    expect("real Storage denies A copy",denied(s),str(out)[:300])
    s,out=transfer("move",ta,pa,path(a,"move"))
    expect("real Storage denies A move",denied(s),str(out)[:300])
    s,out=uploaded(tb,path(b,"new"))
    expect("real Storage allows B upload after A fence",s in (200,201),str(out)[:300])
    s,out=changed(tb,pb,b"updated-B")
    expect("real Storage allows B update after A fence",s in (200,201),str(out)[:300])
    s,out=uploaded(tb,pb,b"upsert-B",upsert=True)
    expect("real Storage allows B upsert",s in (200,201),str(out)[:300])
    s,out=transfer("copy",tb,pb,path(b,"copy"))
    expect("real Storage allows B copy",s in (200,201),str(out)[:300])
    s,out=transfer("move",tb,path(b,"copy"),path(b,"move"))
    expect("real Storage allows B move",s in (200,201),str(out)[:300])

    # The local service-role can bypass RLS; THIS IS AN OPEN PRODUCTION GAP.
    s,out=uploaded(ADMIN,path(a,"privileged"))
    expect("document privileged service-role bypass requiring production guard",
           s in (200,201),str(out)[:300])
    print(f"RC22 REAL LOCAL AUTH+STORAGE PASSED: {checks} checks. "
          "Hosted project calls: 0; paid infrastructure: 0.",flush=True)

if __name__=="__main__":
    run()
