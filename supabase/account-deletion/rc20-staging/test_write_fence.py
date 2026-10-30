#!/usr/bin/env python3
"""Only disposable PostgreSQL. Supabase Auth and Storage API are NOT tested."""
import os
import subprocess
import time

DB="backlot_rc20_ephemeral"
A="7eb549e3-b454-4acd-9bba-dc1cf7d6ba73"
B="c719ec81-a544-456b-ac02-1c5039c77739"
count=0

def sql(q, role=None, fail=False):
    assert os.environ.get("PGDATABASE")==DB, "Refusing external database"
    if role: q=f"SET ROLE rc20_user_{role}; "+q
    p=subprocess.run(["psql","-X","-q","-t","-A","-v","ON_ERROR_STOP=1","-c",q],
                     text=True,capture_output=True,timeout=30)
    if fail:
        assert p.returncode!=0, "Unsafe operation succeeded: "+q[:140]
        return "rejected"
    assert p.returncode==0, q[:180]+": "+p.stderr[:1300]
    return next((x.strip() for x in reversed(p.stdout.splitlines()) if x.strip()),"")

def verify(actual, expected, label):
    global count
    assert actual==expected, f"{label}: {actual!r} != {expected!r}"
    count+=1
    print("PASS:",label,flush=True)

def creator(user, key):
    return (f"INSERT INTO backlot_rc20_ci.creator_projects(id,owner_id,notes)"
            f" VALUES('{key}','{user}','synthetic')")

def voice(user, key):
    return ("INSERT INTO backlot_rc20_ci.voice_object_metadata_mock(bucket_id,name,owner_id,bytes)"
            f" VALUES('creator-voice-ideas','{user}/idea/media/{key:0>64}.m4a','{user}',10)")

verify(sql("SELECT current_database()"),DB,"ephemeral database enforced")
for role in ("a","b"):
    verify(sql(f"SELECT rolbypassrls::text FROM pg_roles WHERE rolname='rc20_user_{role}'"),
           "false",f"user {role} cannot bypass RLS")
    verify(sql(f"SELECT has_table_privilege('rc20_user_{role}','backlot_rc20_ci.fences','UPDATE')::text"),
           "false",f"user {role} cannot modify lock")
    verify(sql(f"SELECT has_function_privilege('rc20_user_{role}',"
               "'backlot_rc20_ci.mark_deleting(uuid,timestamptz)','EXECUTE')::text"),
           "false",f"user {role} cannot invoke admin lock")
for table in ("fences","creator_projects","voice_object_metadata_mock"):
    verify(sql("SELECT relrowsecurity::text||'|'||relforcerowsecurity::text "
               f"FROM pg_class WHERE oid='backlot_rc20_ci.{table}'::regclass"),
           "true|true",table+" RLS enforced")
verify(sql("SELECT backlot_rc20_ci.actor_id()", "a"),A,"A session maps to A")
verify(sql("SELECT backlot_rc20_ci.actor_id()", "b"),B,"B session maps to B")
verify(sql("SELECT backlot_rc20_ci.actor_id()"),"","postgres isn't a synthetic account")
verify(sql(creator(A,"a-original"),"a"),"","A project before deletion")
verify(sql(creator(B,"b-original"),"b"),"","B project before deletion")
verify(sql(voice(A,"1"),"a"),"","A mock voice before deletion")
verify(sql(voice(B,"2"),"b"),"","B mock voice before deletion")
verify(sql(creator(A,"b-cross-user"),"b",True),"rejected","B cannot write A projects")
verify(sql(voice(A,"3"),"b",True),"rejected","B cannot write A voice prefix")
verify(sql("SELECT count(*) FROM backlot_rc20_ci.creator_projects","a"),"1","A cannot view B projects")
verify(sql("SELECT count(*) FROM backlot_rc20_ci.creator_projects","b"),"1","B cannot view A projects")
verify(sql("SELECT count(*) FROM backlot_rc20_ci.fences","a"),"1","A cannot view B lock")
verify(sql("SELECT count(*) FROM backlot_rc20_ci.fences","b"),"1","B cannot view A lock")

# Real transaction: RLS writer takes shared per-user advisory lock while
# holding a transaction open. Admin's exclusive fence MUST wait for commit.
writer=subprocess.Popen(["psql","-X","-q","-t","-A","-v","ON_ERROR_STOP=1","-c",
    "BEGIN; SET ROLE rc20_user_a; "+creator(A,"a-before-lock-commit")+
    "; SELECT pg_sleep(1.5); COMMIT;"],stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
seen=False
for _ in range(120):
    if writer.poll() is not None: raise AssertionError("Writer ended early")
    if int(sql("SELECT count(*) FROM pg_locks WHERE locktype='advisory' "
               "AND mode='ShareLock' AND granted"))>0:
        seen=True
        break
    time.sleep(.05)
verify(seen,True,"in-flight write held RLS advisory lock")
started=time.monotonic()
sql(f"SELECT backlot_rc20_ci.mark_deleting('{A}','2026-10-30T10:00:00+00')")
waited=time.monotonic()-started
_,stderr=writer.communicate(timeout=10)
verify(writer.returncode,0,"in-flight Creator write completes before fence")
verify(waited>=.35,True,"admin lock waited for prior transaction")
verify(sql(f"SELECT blocked::text FROM backlot_rc20_ci.fences WHERE user_id='{A}'"),
       "true","A now fenced")
verify(sql(f"SELECT blocked::text FROM backlot_rc20_ci.fences WHERE user_id='{B}'"),
       "false","B unfenced")

verify(sql(creator(A,"a-after"),"a",True),"rejected","A project INSERT fenced")
verify(sql(voice(A,"4"),"a",True),"rejected","A voice mock INSERT fenced")
verify(sql("UPDATE backlot_rc20_ci.creator_projects SET notes='changed' "
           "WHERE id='a-original'","a",True),"rejected","A project UPDATE fenced")
verify(sql("UPDATE backlot_rc20_ci.voice_object_metadata_mock SET bytes=15 "
           f"WHERE owner_id='{A}'","a",True),"rejected","A voice mock UPDATE fenced")
verify(sql("SELECT count(*) FROM backlot_rc20_ci.creator_projects","a"),
       "2","A existing data still readable")
verify(sql(creator(B,"b-after"),"b"),"","B project INSERT unaffected")
verify(sql(voice(B,"5"),"b"),"","B voice mock INSERT unaffected")
verify(sql("UPDATE backlot_rc20_ci.creator_projects SET notes='changed' "
           "WHERE id='b-original'","b"),"","B project UPDATE unaffected")
verify(sql("UPDATE backlot_rc20_ci.voice_object_metadata_mock SET bytes=15 "
           f"WHERE owner_id='{B}'","b"),"","B voice mock UPDATE unaffected")
verify(sql("UPDATE backlot_rc20_ci.creator_projects "
           f"SET owner_id='{A}' WHERE id='b-original'","b",True),
       "rejected","B cannot reassign project ownership")
verify(sql("SELECT count(*) FROM backlot_rc20_ci.creator_projects "
           "WHERE id='a-after'"),"0","rejected A write absent")
verify(sql("SELECT count(*) FROM backlot_rc20_ci.creator_projects "
           "WHERE id='b-after'"),"1","accepted B write persisted")
verify(sql("SELECT count(*) FROM backlot_rc20_ci.voice_object_metadata_mock","a"),
       "1","A original voice metadata remains")
verify(sql("SELECT count(*) FROM backlot_rc20_ci.voice_object_metadata_mock","b"),
       "2","B new voice metadata persists")
sql(f"SELECT backlot_rc20_ci.mark_deleting('{A}','2026-10-30T10:20:00+00')")
verify(sql(creator(A,"a-repeat"),"a",True),"rejected","repeat fence remains active")
print(f"RC20 STAGING PASS: {count} verified two-user PostgreSQL checks",flush=True)
