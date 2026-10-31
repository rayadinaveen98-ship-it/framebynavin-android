#!/usr/bin/env python3
"""Actual disposable local Supabase: delete-only worker with two real Auth users.

Intentional attack: raw service role can still POST new Storage objects despite
RLS. The trusted worker must never offer such a POST and must only DELETE
paths of its private, already fenced account receipt. Never production.
"""
from __future__ import annotations
import sys
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"rc22-local"))
from test_real_auth_storage import api,sql,user,uploaded,path,BUCKET,ADMIN
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"rc26-local"))
from local_deletion_worker import CONFIRM,request_deletion,receipt_state,resume_internal
from privileged_storage_delete_only import (
    DeleteOnlyDenied,DeleteOnlyStorageWorker
)
checks=0
def check(name,ok,detail=None):
    global checks
    if not ok:
        raise AssertionError(name+": "+str(detail)[:270])
    checks+=1
    print("RC30 REAL PASS:",name,flush=True)

def no_delete(action):
    try:action()
    except DeleteOnlyDenied:return True
    return False

def main():
    s,d=api("POST","/storage/v1/bucket",ADMIN,
            {"id":BUCKET,"name":BUCKET,"public":False,"file_size_limit":1048576})
    check("true local Supabase private bucket",s in (200,201),d)
    a,ta=user("rc30-delete-only-a@example.invalid")
    b,tb=user("rc30-delete-only-b@example.invalid")
    check("two real independently verified Auth users",a!=b and ta!=tb)
    sql(f"INSERT INTO backlot_rc22_ci.deletion_fences(user_id) VALUES('{a}'),('{b}')")
    for owner,token,label in ((a,ta,"a"),(b,tb,"b")):
        s,d=uploaded(token,path(owner,"old"),b"voice from "+label.encode())
        check(label+" original object uploaded with user JWT",s in (200,201),d)
    dotted=path(a,"shot.v2")
    upload_status,upload_result=uploaded(ta,dotted,b"synthetic dotted Voice Idea")
    check("real Storage accepts dotted Android Voice Idea path",upload_status in (200,201),upload_result)
    rid=request_deletion(ta,CONFIRM)
    check("owner A signed-in request recorded and fenced",
          receipt_state(rid)==(a,"fenced","pending"))
    check("A fence on, B stays active",
          sql("SELECT count(*) FROM backlot_rc22_ci.deletion_fences WHERE blocked")=="1")
    # Raw privileged bypass deliberately persists: separate production STOP.
    s,d=uploaded(ADMIN,path(a,"privileged"))
    check("known RAW admin Storage write bypass STILL exists",s in (200,201),d)

    calls=[]
    def wrapped_delete(bucket,names):
        calls.append((bucket,list(names)))
        if bucket!=BUCKET:return 400,{"error":"wrong bucket"}
        return api("DELETE",f"/storage/v1/object/{bucket}",ADMIN,{"prefixes":names})
    def fenced(uid):
        return sql("SELECT blocked::text FROM backlot_rc22_ci.deletion_fences "
                   f"WHERE user_id='{uid}'")=="true"
    gate=DeleteOnlyStorageWorker(
        receipt_state,fenced,wrapped_delete,
        bucket=BUCKET,allow_local_alias=True)
    check("B's files cannot be sent using A receipt",
          no_delete(lambda:gate.remove_objects_for_receipt(rid,[path(b,"old")])))
    check("failed owner path check makes ZERO admin calls",calls==[])
    check("bad receipt cannot delete files",
          no_delete(lambda:gate.remove_objects_for_receipt("forged", [path(a,"old")])))
    check("unfenced B has no authorized deletion receipt",
          no_delete(lambda:gate.remove_objects_for_receipt(rid,[path(b,"old")])))
    check("no exposed privileged upload/copy/upsert",
          not any(hasattr(gate,m) for m in ("upload","upsert","replace","copy","move","request")))
    names=[path(a,"old"),path(a,"privileged"),dotted]
    check("all three A paths including dotted idea can be removed",
          gate.remove_objects_for_receipt(rid,names)==3)
    check("exactly one privileged DELETE request to expected bucket",
          len(calls)==1 and calls[0]==(BUCKET,names))
    check("A Storage metadata gone after real Storage API delete",
          sql(f"SELECT count(*) FROM storage.objects WHERE bucket_id='{BUCKET}' "
              f"AND name LIKE '{a}/%'")=="0")
    check("B still retains its real Storage object",
          sql(f"SELECT count(*) FROM storage.objects WHERE bucket_id='{BUCKET}' "
              f"AND name LIKE '{b}/%'")=="1")
    # RC26 worker is now itself routed through the same strict gate.
    check("full account cleanup still finishes from same receipt",
          resume_internal(rid)=="complete")
    check("A real Auth user was removed",
          sql(f"SELECT count(*) FROM auth.users WHERE id='{a}'")=="0")
    check("B real Auth user remains",
          sql(f"SELECT count(*) FROM auth.users WHERE id='{b}'")=="1")
    check("completed request can no longer touch Storage through gate",
          no_delete(lambda:gate.remove_objects_for_receipt(rid,[path(a,"old")])))
    check("B can still upload after A was deleted",
          uploaded(tb,path(b,"new"),b"still-available")[0] in (200,201))
    print(f"RC30 REAL LOCAL DELETE-ONLY BOUNDARY: {checks} checks passed; "
          "direct raw service key bypass remains an explicit deployment blocker.",flush=True)

if __name__=="__main__":main()
