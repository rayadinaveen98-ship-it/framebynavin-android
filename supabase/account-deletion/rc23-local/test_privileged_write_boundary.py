#!/usr/bin/env python3
"""RC23 real LOCAL Supabase verification of a no-service-write facade.

Starts with RC22's true two-account Auth + Storage RLS regression, then
proves that a service-role JWT cannot use this NEW write boundary while
normal user JWTs remain subject to real Storage API RLS, even when stale.
Not production wiring; never sends requests to hosted Supabase.
"""
from __future__ import annotations
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"rc22-local"))
from test_real_auth_storage import (  # type: ignore[import-not-found]
    run as rc22_run, api, ADMIN, ANON, BUCKET,
)
from storage_write_boundary import (  # type: ignore[import-not-found]
    UserJwtStorageWrites, WriteDenied, validate_owner_path,
)

A_EMAIL="rc22-a@example.invalid"
B_EMAIL="rc22-b@example.invalid"
PASSWORD="rc22-local-test-only-Password77!"
checks=0

def check(name: str, condition: bool, detail: object=""):
    global checks
    if not condition:
        raise AssertionError(f"{name}: {str(detail)[:180]}")
    checks+=1
    print("RC23 PASS:",name,flush=True)

def denied(call):
    try: call()
    except WriteDenied: return True
    return False

def login(email):
    status,body=api("POST","/auth/v1/token?grant_type=password",ANON,
                   {"email":email,"password":PASSWORD})
    check("fresh real local JWT for "+email,status==200,body)
    return body["access_token"]

def verified(token):
    s,user=api("GET","/auth/v1/user",token)
    if s!=200 or not isinstance(user,dict):
        raise WriteDenied("Cannot verify bearer identity through Auth API")
    return user.get("id")

def user_only_api(method,path,token,data=None,mime="application/json",headers=None):
    # Contract proof: reject privileged credential even if someone accidentally
    # changes the facade's admin-token test in a later refactor.
    if token==ADMIN:
        raise AssertionError("No service_role key may reach a Storage WRITE")
    return api(method,path,token,data,mime,headers)

def path(uid,suffix):
    return f"{uid}/idea-{suffix}/media/{suffix}.m4a"

def main():
    rc22_run()  # 36 real Supabase checks including direct service-role bypass
    t_a=login(A_EMAIL)
    t_b=login(B_EMAIL)
    a=verified(t_a);b=verified(t_b)
    check("two independent real Auth identities",a!=b and a and b)
    # A remains blocked by RC22's server-controlled fence.
    client=UserJwtStorageWrites(verified,user_only_api,ADMIN)
    check("admin token refused before any network write",
          denied(lambda:client.put(ADMIN,path(a,"service-replay"),b"denied")))
    check("admin transfer refused before any network write",
          denied(lambda:client.transfer(ADMIN,"copy",path(a,"old"),path(a,"replay"))))
    check("missing token refused",denied(lambda:client.put("",path(a,"x"),b"x")))
    check("forged/expired user token cannot pass real Auth getUser",
          denied(lambda:client.put("invalid-user-bearer",path(a,"x"),b"x")))
    check("A cannot write B's owner path",
          denied(lambda:client.put(t_a,path(b,"other"),b"no")))
    check("B cannot write A's owner path",
          denied(lambda:client.put(t_b,path(a,"other"),b"no")))
    check("A cross-account copy rejected before Storage",
          denied(lambda:client.transfer(t_a,"copy",path(a,"old"),path(b,"other"))))
    check("path traversal refused",
          denied(lambda:client.put(t_a,a+"/../media/foo.m4a",b"bad")))
    check("malformed path refused",
          denied(lambda:client.put(t_a,"nonsense/file.m4a",b"bad")))
    check("unknown privileged operation refused",
          denied(lambda:client.transfer(t_b,"upload",path(b,"old"),path(b,"x"))))
    check("unbounded payload refused",
          denied(lambda:client.put(t_b,path(b,"big"),b"z"*1025)))
    check("ambiguous PUT/upsert refused",
          denied(lambda:client.put(t_b,path(b,"old"),b"x",replace=True,upsert=True)))
    check("same source and destination refused",
          denied(lambda:client.transfer(t_b,"move",path(b,"old"),path(b,"old"))))
    check("no privileged upload method exposed",
          not hasattr(client,"service_role_upload") and
          not hasattr(client,"admin_upload") and not hasattr(client,"privileged_copy"))
    status,out=client.put(t_a,path(a,"attempted"),b"x")
    check("real Storage RLS still rejects fenced A through facade",status in (400,401,403,422),out)
    status,out=client.put(t_b,path(b,"facade-new"),b"from-B")
    check("real Storage API accepts unfenced B user JWT",status in (200,201),out)
    status,out=client.put(t_b,path(b,"old"),b"upsert",upsert=True)
    check("B user JWT upsert accepted via facade",status in (200,201),out)
    status,out=client.put(t_b,path(b,"old"),b"update",replace=True)
    check("B user JWT replace accepted via facade",status in (200,201),out)
    status,out=client.transfer(t_b,"copy",path(b,"old"),path(b,"facade-copy"))
    check("B user JWT copy accepted via facade",status in (200,201),out)
    status,out=client.transfer(t_b,"move",path(b,"facade-copy"),path(b,"facade-move"))
    check("B user JWT move accepted via facade",status in (200,201),out)
    status,out=client.transfer(t_a,"copy",path(a,"old"),path(a,"facade-copy"))
    check("fenced A cannot copy via facade",status in (400,401,403,422),out)
    status,out=client.transfer(t_a,"move",path(a,"old"),path(a,"facade-move"))
    check("fenced A cannot move via facade",status in (400,401,403,422),out)
    print(f"RC23 REAL LOCAL PRIVILEGED-WRITE BOUNDARY PASS: {checks} checks, "
          "direct admin bypass still possible outside this boundary; "
          "no hosted project touched.",flush=True)

if __name__=="__main__":
    main()
