#!/usr/bin/env python3
"""RC27 loopback-only HTTP request gateway for RC26's genuine local Supabase.

PROTOTYPE ONLY. Do not deploy in production. Backend and UI not wired to real
Backlot and do not meet reauthentication/SSO, concurrency, abuse or TLS gates.
Only the separate INTERNAL worker can perform irreversible deletion.
"""
from __future__ import annotations

import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener
from urllib.error import HTTPError

from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"rc26-local"))
from local_deletion_worker import (
    CONFIRM, DeletionError, receipt_state, request_deletion, uid_of
)
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"rc22-local"))
from test_real_auth_storage import sql  # actual LOCAL PostgreSQL, never hosted

MAX_BODY=256
API_PATH="/v1/account/deletion/requests"
STATUS_PATH="/v1/account/deletion/requests/current"

def json_pairs_unique(pairs):
    result={}
    for name,value in pairs:
        if name in result:
            raise ValueError("Repeated JSON property")
        result[name]=value
    return result

def current_receipt_for_verified_user(access_token):
    """Derive id from real /auth/v1/user, NOT an untrusted query param."""
    owner=uid_of(access_token)
    # uuid already verified by uid_of. Never leak email or internal schema.
    receipt=sql("SELECT id::text FROM backlot_rc26_ci.requests "
                f"WHERE user_id='{owner}'")
    if not receipt:
        return None
    uid,stage,status=receipt_state(receipt)
    if uid!=owner:
        raise DeletionError("No matching owner receipt")
    return {"request_id":receipt,"status":status,"stage":stage}

def gateway():
    class Handler(BaseHTTPRequestHandler):
        server_version="BacklotLocalGateway"
        sys_version=""

        def log_message(self, format, *args):
            # Never log Authorization, URL query, user ids, personal fields.
            return

        def send_json(self, code, obj):
            body=json.dumps(obj,separators=(",",":"),sort_keys=True).encode("utf-8")
            self.send_response(code)
            self.send_header("Content-Type","application/json; charset=utf-8")
            self.send_header("Content-Length",str(len(body)))
            self.send_header("Cache-Control","no-store, private")
            self.send_header("Pragma","no-cache")
            self.send_header("X-Content-Type-Options","nosniff")
            self.send_header("Referrer-Policy","no-referrer")
            self.send_header("X-Frame-Options","DENY")
            # Deliberately never issue CORS wildcard, cookies or redirects.
            self.end_headers()
            self.wfile.write(body)

        def preflight(self):
            host=self.headers.get("Host","")
            # No forwarding/X-Forwarded-* trusted, no wildcard vhost.
            if host not in (f"127.0.0.1:{self.server.server_port}",
                            f"localhost:{self.server.server_port}"):
                self.send_json(403,{"error":"forbidden_host"})
                return False
            origin=self.headers.get("Origin")
            if origin and origin not in (f"http://127.0.0.1:{self.server.server_port}",
                                         f"http://localhost:{self.server.server_port}"):
                self.send_json(403,{"error":"origin_rejected"})
                return False
            if urlsplit(self.path).query or urlsplit(self.path).fragment:
                self.send_json(400,{"error":"query_not_supported"})
                return False
            return True

        def bearer(self):
            auth=self.headers.get("Authorization","")
            if not auth.startswith("Bearer ") or not auth[7:].strip():
                raise DeletionError("Bearer required")
            if "," in auth or len(auth)>9000 or "\n" in auth or "\r" in auth:
                raise DeletionError("Malformed bearer")
            return auth[7:]

        def do_OPTIONS(self):
            if not self.preflight():return
            self.send_json(405,{"error":"method_not_allowed"})

        def do_GET(self):
            if not self.preflight():return
            if self.path=="/health":
                return self.send_json(200,{"service":"backlot-local-deletion-gateway",
                                           "state":"not-deployed"})
            if self.path!=STATUS_PATH:
                return self.send_json(404,{"error":"not_found"})
            try:
                receipt=current_receipt_for_verified_user(self.bearer())
            except DeletionError:
                return self.send_json(401,{"error":"auth_required"})
            except Exception:
                return self.send_json(503,{"error":"temporarily_unavailable"})
            if receipt is None:
                return self.send_json(200,{"status":"none"})
            return self.send_json(200,receipt)

        def do_POST(self):
            if not self.preflight():return
            if self.path!=API_PATH:
                return self.send_json(404,{"error":"not_found"})
            if self.headers.get("Content-Type","").split(";")[0].strip().lower()!="application/json":
                return self.send_json(415,{"error":"json_required"})
            try:
                length=int(self.headers.get("Content-Length","-1"))
            except ValueError:
                return self.send_json(400,{"error":"invalid_length"})
            if length<1 or length>MAX_BODY:
                return self.send_json(413,{"error":"payload_invalid"})
            raw=self.rfile.read(length)
            if len(raw)!=length:return self.send_json(400,{"error":"truncated_body"})
            try:
                data=json.loads(raw.decode("utf-8"),object_pairs_hook=json_pairs_unique)
            except (ValueError,UnicodeError,TypeError):
                return self.send_json(400,{"error":"malformed_json"})
            if not isinstance(data,dict) or set(data)!={"confirmation"}:
                return self.send_json(400,{"error":"invalid_fields"})
            if data["confirmation"]!=CONFIRM:
                return self.send_json(422,{"error":"confirmation_required"})
            try:
                token=self.bearer()
                receipt=request_deletion(token,data["confirmation"])
                # Internal worker NOT called from an untrusted HTTP request:
                # only accepted request + durable blocked-account receipt.
                return self.send_json(202,{"request_id":receipt,
                                           "status":"pending",
                                           "note":"local_validation_only"})
            except DeletionError:
                return self.send_json(401,{"error":"auth_required"})
            except Exception:
                return self.send_json(503,{"error":"temporarily_unavailable"})

        def do_DELETE(self):
            if not self.preflight():return
            return self.send_json(405,{"error":"method_not_allowed"})

    return Handler

def start_local_server():
    return ThreadingHTTPServer(("127.0.0.1",0),gateway())

def client_request(base,method,path,token=None,body=None,headers=None):
    """Test-only HTTP helper that refuses any remote destination or redirect."""
    parts=urlsplit(base)
    if parts.scheme!="http" or parts.hostname not in ("localhost","127.0.0.1"):
        raise ValueError("Only localhost gateway is permitted")
    if not parts.port or parts.path not in ("","/") or parts.query or parts.fragment:
        raise ValueError("Unsafe gateway URL")
    if not path.startswith("/") or path.startswith("//"):raise ValueError("Unsafe route")
    extra={"Accept":"application/json"}
    if token is not None:extra["Authorization"]="Bearer "+token
    if body is not None:
        extra["Content-Type"]="application/json"
    if headers:extra.update(headers)
    payload=json.dumps(body).encode("utf-8") if body is not None else None
    class NoRedirect(HTTPRedirectHandler):
        def redirect_request(self,req,fp,code,msg,headers,newurl):
            raise ValueError("No redirects")
    try:
        with build_opener(NoRedirect).open(
            Request(base+path,data=payload,headers=extra,method=method),timeout=10
        ) as result:
            return result.status,json.loads(result.read(8192))
    except HTTPError as e:
        return e.code,json.loads(e.read(8192))
