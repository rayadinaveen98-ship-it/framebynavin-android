"""Backlot RC21: strictly local HTTP contract client, NOT a production SDK.

This module can only contact 127.0.0.1/localhost for disposable CI mocks.
It is intentionally impossible to point it at a hosted Supabase project;
real two-account Supabase staging still requires separate owner approval.
"""
from __future__ import annotations
import json
import re
from dataclasses import dataclass
from urllib.parse import quote, urlsplit
from urllib.request import Request, build_opener, HTTPHandler
from urllib.error import HTTPError

UUID = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", re.I)
SAFE = re.compile(r"^[a-zA-Z0-9._-]{1,160}$")
BUCKET = "creator-voice-ideas"

@dataclass(frozen=True)
class HttpResult:
    status: int
    body: bytes

class NoRedirect(HTTPHandler):
    def http_request(self, req):
        return req
    https_request = http_request

def safe_local_origin(origin: str) -> str:
    parsed = urlsplit(origin)
    if (parsed.scheme != "http" or parsed.hostname not in ("127.0.0.1", "localhost")
        or not parsed.port or parsed.username or parsed.password
        or parsed.path not in ("", "/") or parsed.query or parsed.fragment):
        raise ValueError("RC21 is local-only: hosted/prod Supabase URLs are forbidden")
    return f"http://{parsed.hostname}:{parsed.port}"

def safe_object_path(path: str) -> str:
    parts = path.split("/")
    if len(parts) != 4 or not UUID.fullmatch(parts[0]) or any(
        not SAFE.fullmatch(p) or p in (".", "..") for p in parts
    ):
        raise ValueError("Invalid owner-scoped synthetic voice path")
    if parts[2] != "media" or not parts[3].endswith(".m4a"):
        raise ValueError("Only synthetic voice idea files may be used")
    return "/".join(parts)

class StorageHttpContract:
    def __init__(self, origin: str, publishable_key: str = "rc21-local-publishable"):
        self.origin = safe_local_origin(origin)
        self.key = publishable_key
        if not publishable_key or len(publishable_key)>120:
            raise ValueError("Invalid synthetic publishable key")
        # Never automatically follow a redirect out to another host.
        from urllib.request import HTTPRedirectHandler
        class RejectRedirect(HTTPRedirectHandler):
            def redirect_request(self, req, fp, code, msg, headers, newurl):
                raise ValueError("RC21 refuses HTTP redirects")
        self.opener=build_opener(RejectRedirect)

    def request(self, method: str, path: str, token: str,
                payload: bytes | None = None,
                content_type: str = "application/json",
                extra_headers: dict[str,str] | None = None) -> HttpResult:
        if not token or "\r" in token or "\n" in token:
            raise ValueError("Synthetic bearer token required")
        headers={"apikey":self.key,"Authorization":"Bearer "+token,
                 "Accept":"application/json","Content-Type":content_type}
        if extra_headers: headers.update(extra_headers)
        # All paths are constants or validated synthetic path fragments.
        req=Request(self.origin+path, data=payload, headers=headers, method=method)
        try:
            with self.opener.open(req, timeout=5) as response:
                return HttpResult(response.status,response.read(65536))
        except HTTPError as error:
            return HttpResult(error.code,error.read(65536))

    def identity(self, token: str) -> tuple[int,str]:
        response=self.request("GET","/auth/v1/user",token)
        if response.status != 200: return response.status,""
        user=json.loads(response.body)
        uid=user.get("id","")
        if not isinstance(uid,str) or not UUID.fullmatch(uid):
            raise ValueError("Auth identity cannot be trusted")
        return response.status,uid

    def upload(self, token: str, path: str, payload: bytes,
               upsert: bool = False) -> HttpResult:
        path=safe_object_path(path)
        if not 0<len(payload)<=1024: raise ValueError("Only tiny synthetic files")
        return self.request("POST",
            "/storage/v1/object/"+BUCKET+"/"+quote(path,safe="/"),token,payload,
            "audio/mp4",{"x-upsert":"true" if upsert else "false"})

    def replace(self, token: str, path: str, payload: bytes) -> HttpResult:
        path=safe_object_path(path)
        if not 0<len(payload)<=1024: raise ValueError("Only tiny synthetic files")
        return self.request("PUT",
            "/storage/v1/object/"+BUCKET+"/"+quote(path,safe="/"),token,payload,"audio/mp4")

    def read(self, token: str, path: str) -> HttpResult:
        path=safe_object_path(path)
        return self.request("GET",
            "/storage/v1/object/authenticated/"+BUCKET+"/"+quote(path,safe="/"),token)

    def _transfer(self, operation: str, token: str, source: str, dest: str) -> HttpResult:
        if operation not in ("copy","move"): raise ValueError("Invalid operation")
        payload=json.dumps({"bucketId":BUCKET,"sourceKey":safe_object_path(source),
                            "destinationKey":safe_object_path(dest)}).encode()
        return self.request("POST","/storage/v1/object/"+operation,token,payload)

    def copy(self, token: str, source: str, dest: str) -> HttpResult:
        return self._transfer("copy",token,source,dest)

    def move(self, token: str, source: str, dest: str) -> HttpResult:
        return self._transfer("move",token,source,dest)
