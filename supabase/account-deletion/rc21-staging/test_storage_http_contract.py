"""Synthetic HTTP contract tests. NOT the real hosted Supabase Auth or Storage API."""
from __future__ import annotations
import json
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import unquote
from storage_http_contract import StorageHttpContract, BUCKET, safe_local_origin

A="7eb549e3-b454-4acd-9bba-dc1cf7d6ba73"
B="c719ec81-a544-456b-ac02-1c5039c77739"
TOKENS={"rc21-token-A":A,"rc21-token-B":B}
def object_path(uid: str, suffix: str="a")->str:
    return f"{uid}/idea-{suffix}/media/{suffix*4}.m4a"

class SyntheticState:
    def __init__(self):
        self.lock=threading.RLock()
        self.objects:dict[str,bytes]={}
        self.fenced:set[str]=set()
        self.calls:list[tuple[str,str]]=[]
        self.redirect=False

class Handler(BaseHTTPRequestHandler):
    def log_message(self,*args): pass
    def reply(self,status:int,body:bytes=b"{}",content_type="application/json"):
        self.send_response(status)
        self.send_header("Content-Type",content_type)
        self.send_header("Content-Length",str(len(body)))
        self.end_headers()
        self.wfile.write(body)
    def _process(self):
        state: SyntheticState=self.server.state  # type: ignore[attr-defined]
        with state.lock:
            state.calls.append((self.command,self.path))
            if state.redirect:
                self.send_response(302)
                self.send_header("Location","https://kukkqgpzxnfanynbddiw.supabase.co/storage/v1/object")
                self.end_headers()
                return
            if self.headers.get("apikey")!="rc21-local-publishable":
                return self.reply(401)
            auth=self.headers.get("Authorization","")
            uid=TOKENS.get(auth.removeprefix("Bearer ")) if auth.startswith("Bearer ") else None
            if uid is None: return self.reply(401)
            if self.path=="/auth/v1/user" and self.command=="GET":
                return self.reply(200,json.dumps({"id":uid}).encode())
            prefix=f"/storage/v1/object/{BUCKET}/"
            read_prefix=f"/storage/v1/object/authenticated/{BUCKET}/"
            if self.command=="GET" and self.path.startswith(read_prefix):
                path=unquote(self.path[len(read_prefix):])
                if not path.startswith(uid+"/"): return self.reply(403)
                return self.reply(200,state.objects[path],"audio/mp4") if path in state.objects else self.reply(404)
            if self.command in ("POST","PUT") and self.path.startswith(prefix):
                path=unquote(self.path[len(prefix):])
                if not path.startswith(uid+"/") or uid in state.fenced:
                    return self.reply(403)
                length=int(self.headers.get("Content-Length","0"))
                if not 0<length<=1024:return self.reply(400)
                data=self.rfile.read(length)
                if self.command=="POST" and path in state.objects and self.headers.get("x-upsert")!="true":
                    return self.reply(409)
                if self.command=="PUT" and path not in state.objects:
                    return self.reply(404)
                state.objects[path]=data
                return self.reply(200)
            if self.command=="POST" and self.path in ("/storage/v1/object/copy","/storage/v1/object/move"):
                size=int(self.headers.get("Content-Length","0"))
                if not 0<size<1500:return self.reply(400)
                try:
                    body=json.loads(self.rfile.read(size))
                    src=body["sourceKey"]
                    dst=body["destinationKey"]
                    assert body["bucketId"]==BUCKET
                    assert isinstance(src,str) and isinstance(dst,str)
                except (KeyError,ValueError,AssertionError,TypeError):
                    return self.reply(400)
                if (not src.startswith(uid+"/") or not dst.startswith(uid+"/")
                    or uid in state.fenced):
                    return self.reply(403)
                if src not in state.objects:return self.reply(404)
                if dst in state.objects:return self.reply(409)
                state.objects[dst]=state.objects[src]
                if self.path.endswith("/move"): del state.objects[src]
                return self.reply(200)
            return self.reply(404)
    def do_GET(self): return self._process()
    def do_POST(self): return self._process()
    def do_PUT(self): return self._process()

class Rc21StorageHttpContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.state=SyntheticState()
        cls.server=ThreadingHTTPServer(("127.0.0.1",0),Handler)
        cls.server.state=cls.state
        cls.thread=threading.Thread(target=cls.server.serve_forever,daemon=True)
        cls.thread.start()
        cls.client=StorageHttpContract(f"http://127.0.0.1:{cls.server.server_address[1]}")

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join(3)

    def setUp(self):
        with self.state.lock:
            self.state.objects={}
            self.state.fenced=set()
            self.state.calls=[]
            self.state.redirect=False

    def test_01_block_known_live_backlot_project(self):
        with self.assertRaises(ValueError):
            StorageHttpContract("https://kukkqgpzxnfanynbddiw.supabase.co")

    def test_02_block_other_supabase_project(self):
        with self.assertRaises(ValueError):
            StorageHttpContract("https://example-staging.supabase.co")

    def test_03_block_remote_http_and_malformed_origin(self):
        for url in ("http://example.com:54321","http://10.0.0.8:80","http://localhost",
                    "http://localhost:7788/elsewhere","http://localhost:7788/?token=1"):
            with self.subTest(url=url),self.assertRaises(ValueError):
                safe_local_origin(url)

    def test_04_prove_two_distinct_server_verified_auth_identities(self):
        self.assertEqual(self.client.identity("rc21-token-A"),(200,A))
        self.assertEqual(self.client.identity("rc21-token-B"),(200,B))

    def test_05_reject_missing_and_unknown_auth(self):
        self.assertEqual(self.client.identity("invalid-token")[0],401)
        with self.assertRaises(ValueError):
            self.client.identity("")

    def test_06_owner_upload_before_fence(self):
        p=object_path(A)
        self.assertEqual(self.client.upload("rc21-token-A",p,b"a").status,200)
        self.assertEqual(self.state.objects[p],b"a")

    def test_07_sibling_cannot_upload_other_user_prefix(self):
        self.assertEqual(self.client.upload("rc21-token-B",object_path(A),b"bad").status,403)
        self.assertEqual(self.state.objects,{})

    def test_08_fenced_owner_upload_rejected(self):
        self.state.fenced.add(A)
        self.assertEqual(self.client.upload("rc21-token-A",object_path(A),b"bad").status,403)

    def test_09_unfenced_sibling_upload_remains_allowed(self):
        self.state.fenced.add(A)
        self.assertEqual(self.client.upload("rc21-token-B",object_path(B),b"ok").status,200)

    def test_10_fenced_owner_cannot_replace_existing_file(self):
        p=object_path(A)
        self.state.objects[p]=b"old"
        self.state.fenced.add(A)
        self.assertEqual(self.client.replace("rc21-token-A",p,b"new").status,403)
        self.assertEqual(self.state.objects[p],b"old")

    def test_11_unfenced_sibling_can_replace(self):
        p=object_path(B)
        self.state.objects[p]=b"old"
        self.state.fenced.add(A)
        self.assertEqual(self.client.replace("rc21-token-B",p,b"new").status,200)
        self.assertEqual(self.state.objects[p],b"new")

    def test_12_fenced_owner_upsert_cannot_bypass(self):
        p=object_path(A)
        self.state.objects[p]=b"old"
        self.state.fenced.add(A)
        self.assertEqual(self.client.upload("rc21-token-A",p,b"new",upsert=True).status,403)
        self.assertEqual(self.state.objects[p],b"old")

    def test_13_upload_without_upsert_refuses_overwrite(self):
        p=object_path(A)
        self.state.objects[p]=b"old"
        self.assertEqual(self.client.upload("rc21-token-A",p,b"new").status,409)

    def test_14_owner_can_copy_own_file_before_fence(self):
        src=object_path(A,"x");dst=object_path(A,"y")
        self.state.objects[src]=b"source"
        self.assertEqual(self.client.copy("rc21-token-A",src,dst).status,200)
        self.assertEqual(self.state.objects[src],self.state.objects[dst])

    def test_15_fenced_owner_copy_is_denied(self):
        src=object_path(A,"x");dst=object_path(A,"y")
        self.state.objects[src]=b"source";self.state.fenced.add(A)
        self.assertEqual(self.client.copy("rc21-token-A",src,dst).status,403)
        self.assertNotIn(dst,self.state.objects)

    def test_16_unfenced_sibling_can_copy(self):
        src=object_path(B,"x");dst=object_path(B,"y")
        self.state.objects[src]=b"source";self.state.fenced.add(A)
        self.assertEqual(self.client.copy("rc21-token-B",src,dst).status,200)

    def test_17_fenced_owner_move_is_denied(self):
        src=object_path(A,"x");dst=object_path(A,"y")
        self.state.objects[src]=b"source";self.state.fenced.add(A)
        self.assertEqual(self.client.move("rc21-token-A",src,dst).status,403)
        self.assertIn(src,self.state.objects)
        self.assertNotIn(dst,self.state.objects)

    def test_18_unfenced_sibling_can_move(self):
        src=object_path(B,"x");dst=object_path(B,"y")
        self.state.objects[src]=b"source";self.state.fenced.add(A)
        self.assertEqual(self.client.move("rc21-token-B",src,dst).status,200)
        self.assertNotIn(src,self.state.objects)
        self.assertEqual(self.state.objects[dst],b"source")

    def test_19_copy_move_between_accounts_is_forbidden(self):
        src=object_path(A,"x");dst=object_path(B,"y")
        self.state.objects[src]=b"source"
        self.assertEqual(self.client.copy("rc21-token-A",src,dst).status,403)
        self.assertEqual(self.client.move("rc21-token-A",src,dst).status,403)

    def test_20_existing_owner_data_remains_readable_until_deletion(self):
        src=object_path(A)
        self.state.objects[src]=b"existing"
        self.state.fenced.add(A)
        x=self.client.read("rc21-token-A",src)
        self.assertEqual((x.status,x.body),(200,b"existing"))
        self.assertEqual(self.client.read("rc21-token-B",src).status,403)

    def test_21_reject_path_traversal_and_unbounded_payload(self):
        for bad in (A+"/../media/a.m4a",A+"/idea1/media/../file.m4a",
                    A+"/idea/media/"+("x"*200)+".m4a"):
            with self.subTest(path=bad),self.assertRaises(ValueError):
                self.client.upload("rc21-token-A",bad,b"bad")
        with self.assertRaises(ValueError):
            self.client.upload("rc21-token-A",object_path(A),b"x"*1025)
        self.assertEqual(len(self.state.calls),0)

    def test_22_reject_redirect_to_production_host(self):
        self.state.redirect=True
        with self.assertRaises(ValueError):
            self.client.identity("rc21-token-A")
        self.assertEqual(len(self.state.calls),1)

if __name__=="__main__":
    unittest.main(verbosity=2)
