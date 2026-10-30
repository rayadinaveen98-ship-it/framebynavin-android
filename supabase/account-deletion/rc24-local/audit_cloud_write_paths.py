#!/usr/bin/env python3
"""Fail-closed static guard for known Backlot Android -> Supabase cloud writers.

This does NOT inspect unknown external backend services, or prove
service-role key isolation there. Only release-source changes are audited.
"""
from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[3]
SRC=ROOT/"app/src/main"
CLOUD=SRC/"java/com/framebynavin/app/cloud"

def must(condition,description):
    if not condition: raise SystemExit("RC24 CLOUD AUDIT FAILED: "+description)
    print("PASS:",description)

def read(filename): return (CLOUD/filename).read_text()

sources=list(SRC.rglob("*.kt"))+list(SRC.rglob("*.java"))
must(len(sources)>100,"production Android source tree enumerated")
for f in sources:
    content=f.read_text()
    for secret in (r"\bsb_secret_[A-Za-z0-9_-]+",r"\bservice_role\b",
                   r"SUPABASE_SERVICE_ROLE",r"SUPABASE_SECRET_KEY",
                   r"/auth/v1/admin/"):
        must(not re.search(secret,content,re.I),
             "no privileged Supabase key or admin route in "+str(f.relative_to(ROOT)))

config=read("CloudConfig.kt")
must('const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_' in config,
     "mobile CloudConfig holds publishable key")
must("SUPABASE_SERVICE_ROLE" not in config,
     "mobile has no service-role constant")
api=read("CloudApiClient.kt")
must('setRequestProperty("apikey", CloudConfig.SUPABASE_PUBLISHABLE_KEY)' in api,
     "Creator API transport sends publishable key")
must('setRequestProperty("Authorization", "Bearer $token")' in api,
     "Creator API transport uses bearer session")
for signature,next_signature,operation in (
    ("suspend fun claimCreatorUsername","suspend fun fetchCreatorSyncHead","profile update"),
    ("suspend fun pushCreatorSyncSnapshot","suspend fun deleteCreatorSyncData","snapshot upload"),
    ("suspend fun deleteCreatorSyncData","private fun parseCreatorProfile","sync-only cleanup"),
):
    part=api.split(signature,1)
    must(len(part)==2,"found "+operation+" implementation")
    block=part[1].split(next_signature,1)[0]
    must("session.accessToken" in block and "authenticated = false" not in block,
         operation+" uses a user bearer, not anonymous/privileged credentials")
must('"/rest/v1/rpc/delete_creator_sync_data"' in api,
     "old sync-only deletion API not mistaken for full-account deletion")
voice=read("CreatorCloudVoiceMedia.kt")
must('setRequestProperty("apikey", CloudConfig.SUPABASE_PUBLISHABLE_KEY)' in voice,
     "Voice Storage transport uses publishable key")
must('setRequestProperty("Authorization", "Bearer '+chr(36)+'{session.accessToken}")' in voice,
     "Voice Storage transport uses signed-in user token")
must('validateObjectPath(session.userId, objectPath)' in voice,
     "Voice object paths are validated against account identity")
must('setRequestProperty("x-upsert", "true")' in voice,
     "Voice upsert needs a real server-side Storage policy fence")
must('const val BUCKET = "creator-voice-ideas"' in voice,
     "Voice uploads target intended private bucket")
manager=read("CreatorCloudSyncManager.kt")
must("api.pushCreatorSyncSnapshot(" in manager,
     "Creator backup sync delegates to audited client")
print("RC24 CLOUD WRITER AUDIT PASSED; external backend writers not covered")
