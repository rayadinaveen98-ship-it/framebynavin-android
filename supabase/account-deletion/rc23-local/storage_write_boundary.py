"""RC23 prototype: user-scoped Storage WRITES never use a service-role token.

This is a dependency-injected offline proof, not a deployed backend and not a
solution for an already-existing unguarded privileged worker. Callers must
wire a REAL server-verified Auth getUser and a user-JWT backed Storage API.
Privileged workers should be deletion-only; all privileged write paths must
be independently inventoried/restricted before account deletion is enabled.
"""
from __future__ import annotations
import re
from urllib.parse import quote

UUID = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",re.I)
SEG = re.compile(r"^[A-Za-z0-9._-]{1,160}$")
BUCKET = "backlot-rc22-ci"
MAX_BYTES = 1024

class WriteDenied(ValueError):
    pass

def validate_owner_path(path: str, uid: str) -> str:
    if not UUID.fullmatch(uid) or not isinstance(path,str):
        raise WriteDenied("Unverified identity or invalid path")
    parts=path.split("/")
    if len(parts)!=4 or parts[0]!=uid or parts[2]!="media" or (
       any(not SEG.fullmatch(x) or x in (".","..") for x in parts)
    ) or not parts[3].endswith(".m4a"):
        raise WriteDenied("Voice object path does not belong to verified account")
    return path

class UserJwtStorageWrites:
    """Write-only API: there is no method that sends a service key.

    verify_identity is an independently trusted server-side Auth getUser
    callback, not a decoded client JWT or caller supplied account ID.
    send_as_user MUST set apikey to publishable/anon and Authorization to the
    independently verified user's JWT. Never pass service-role credentials.
    """
    def __init__(self, verify_identity, send_as_user, forbidden_privileged_token):
        self.verify_identity=verify_identity
        self.send_as_user=send_as_user
        self.forbidden_privileged_token=forbidden_privileged_token

    def _owner(self, token: str) -> str:
        if not isinstance(token,str) or not token or token==self.forbidden_privileged_token:
            raise WriteDenied("Privileged or missing token cannot write voice files")
        owner=self.verify_identity(token)
        if not isinstance(owner,str) or not UUID.fullmatch(owner):
            raise WriteDenied("A genuine server-verified user is required")
        return owner

    def put(self, token: str, path: str, content: bytes, *, replace=False, upsert=False):
        uid=self._owner(token)
        validate_owner_path(path,uid)
        if not isinstance(content,bytes) or not (0<len(content)<=MAX_BYTES):
            raise WriteDenied("Only bounded synthetic bytes are accepted")
        if replace and upsert: raise WriteDenied("Ambiguous write operation")
        method="PUT" if replace else "POST"
        extra={"x-upsert":"true" if upsert else "false"}
        return self.send_as_user(method,
            "/storage/v1/object/"+BUCKET+"/"+quote(path,safe="/"),
            token,content,"audio/mp4",extra)

    def transfer(self, token: str, action: str, source: str, destination: str):
        uid=self._owner(token)
        if action not in ("copy","move"):
            raise WriteDenied("Unrecognized write operation")
        validate_owner_path(source,uid)
        validate_owner_path(destination,uid)
        if source==destination: raise WriteDenied("Source and destination must differ")
        payload={"bucketId":BUCKET,"sourceKey":source,"destinationKey":destination}
        return self.send_as_user("POST","/storage/v1/object/"+action,token,payload)

    # No service_role upload/copy/move endpoint is exposed by this class.
