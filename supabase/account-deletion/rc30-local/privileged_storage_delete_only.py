#!/usr/bin/env python3
"""RC30 LOCAL ONLY: privileged Storage deletion-only boundary.

No upload, replace, upsert, copy, move, signed URL or arbitrary HTTP method is
exposed. Backend credentials belong ONLY in a trusted internal worker.
NOT an authorization control for whoever already has a raw Supabase
service-role key: that key STILL bypasses Storage RLS.
"""
from __future__ import annotations
import re
from uuid import UUID
from typing import Callable

VOICE_BUCKET = "creator-voice-ideas"
UUID_PATTERN = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", re.I)
# Match Android CreatorCloudVoiceMedia.safeSegment: dots are valid in an idea id.
# Parent traversal remains rejected below (including ".." anywhere in a path).
SEGMENT = re.compile(r"^[A-Za-z0-9._-]{1,160}$")
VOICE_FILE = re.compile(r"^[a-zA-Z0-9_-]{1,180}\.m4a$")
LOCAL_BUCKET_ALIAS = "backlot-rc22-ci"
MAX_DELETE_BATCH = 100

class DeleteOnlyDenied(ValueError):
    """Generic error: no user file contents, secrets or private paths leak."""

def _uuid(value: str) -> str:
    try:
        parsed = str(UUID(value))
    except (ValueError,TypeError,AttributeError) as exc:
        raise DeleteOnlyDenied("Invalid trusted identifier") from exc
    if not UUID_PATTERN.fullmatch(value) or value != parsed:
        raise DeleteOnlyDenied("Malformed trusted identifier")
    return parsed

def check_voice_path(owner: str, name: str) -> None:
    owner = _uuid(owner)
    if not isinstance(name, str) or not 1 < len(name) <= 700:
        raise DeleteOnlyDenied("Invalid object name")
    p = name.split("/")
    if len(p)!=4 or p[0]!=owner or p[2]!="media":
        raise DeleteOnlyDenied("Owner path mismatch")
    if not SEGMENT.fullmatch(p[1]) or not VOICE_FILE.fullmatch(p[3]):
        raise DeleteOnlyDenied("Unsafe object name")
    if ".." in name or "\\" in name or "%" in name or "?" in name or "#" in name:
        raise DeleteOnlyDenied("Noncanonical object name")

class DeleteOnlyStorageWorker:
    """Trusted backend deletion-only API.

    load_receipt returns (trusted_user_uuid, stage, status) from private DB.
    is_fenced reads live server-controlled state.
    send_delete sends ONLY HTTP DELETE to Storage using a backend-held key.
    The caller cannot choose owner, bucket, HTTP method or credentials.
    """
    __slots__=("_load_receipt","_is_fenced","_send_delete","_bucket")

    def __init__(
        self,
        load_receipt: Callable[[str],tuple[str,str,str]],
        is_fenced: Callable[[str],bool],
        send_delete: Callable[[str,list[str]],tuple[int,object]],
        *,
        bucket: str = VOICE_BUCKET,
        allow_local_alias: bool = False,
    ):
        if bucket != VOICE_BUCKET and not (allow_local_alias and bucket == LOCAL_BUCKET_ALIAS):
            raise DeleteOnlyDenied("Storage bucket outside deletion scope")
        self._load_receipt = load_receipt
        self._is_fenced = is_fenced
        self._send_delete = send_delete
        self._bucket = bucket

    def remove_objects_for_receipt(self, receipt_id: str, names: list[str]) -> int:
        receipt_id=_uuid(receipt_id)
        if not isinstance(names,list) or not (1<=len(names)<=MAX_DELETE_BATCH):
            raise DeleteOnlyDenied("Invalid deletion batch")
        owner,stage,status=self._load_receipt(receipt_id)
        owner=_uuid(owner)
        if stage!="fenced" or status not in ("pending","retryable"):
            raise DeleteOnlyDenied("Receipt not authorized for Storage phase")
        if not self._is_fenced(owner):
            raise DeleteOnlyDenied("Trusted deletion lock missing")
        if len(names)!=len(set(names)):
            raise DeleteOnlyDenied("Duplicate object names")
        for name in names:
            check_voice_path(owner,name)
        # Exactly one privileged operation: DELETE of canonical owner paths.
        code,_=self._send_delete(self._bucket,names)
        if code not in (200,204):
            raise DeleteOnlyDenied("Trusted Storage API removal unsuccessful")
        return len(names)
    # Deliberately NO upload/upsert/copy/move/replace or generic request API.
