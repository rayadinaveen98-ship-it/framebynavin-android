#!/usr/bin/env python3
"""RC27 HTTP gateway tests against genuine disposable LOCAL Supabase services."""
from __future__ import annotations
import json
import threading
import unittest
from urllib.request import Request,build_opener,HTTPRedirectHandler
from urllib.error import HTTPError

from http_request_gateway import (
    start_local_server,client_request,API_PATH,STATUS_PATH,CONFIRM
)
import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"rc22-local"))
from test_real_auth_storage import api,sql,user,uploaded,path,BUCKET,ADMIN,ANON
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"rc26-local"))
from local_deletion_worker import (
    receipt_state,owned_storage_names,resume_internal
)

class LocalGatewayE2E(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        state,body=api("POST","/storage/v1/bucket",ADMIN,
                       {"id":BUCKET,"name":BUCKET,"public":False,"file_size_limit":1048576})
        assert state in (200,201),(state,body)
        cls.a,cls.ta=user("rc27-owner-a@example.invalid")
        cls.b,cls.tb=user("rc27-owner-b@example.invalid")
        assert cls.a!=cls.b
        sql(f"INSERT INTO backlot_rc22_ci.deletion_fences(user_id) VALUES('{cls.a}'),('{cls.b}')")
        for uid,tok,short in ((cls.a,cls.ta,"a"),(cls.b,cls.tb,"b")):
            for table,entry in (
                ("creator_profiles",{"user_id":uid,"username":"rc27-"+short}),
                ("creator_sync_snapshots",{"user_id":uid,"revision":1,
                      "content_sha256":"a"*64,"payload":"{}"}),
                ("creator_sync_heads",{"user_id":uid,"revision":1,
                      "content_sha256":"a"*64}),
            ):
                status,data=api("POST","/rest/v1/"+table,tok,entry)
                assert status in (200,201),(table,status,data)
            status,data=uploaded(tok,path(uid,"x"),b"real-test-bytes")
            assert status in (200,201),(status,data)
        cls.server=start_local_server()
        assert cls.server.server_address[0]=="127.0.0.1"
        cls.thread=threading.Thread(target=cls.server.serve_forever,daemon=True)
        cls.thread.start()
        cls.origin="http://127.0.0.1:"+str(cls.server.server_port)
        cls.receipt=None

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join(timeout=3)

    def send(self,method,path,token=None,body=None,headers=None):
        return client_request(self.origin,method,path,token,body,headers)

    def test_01_local_status_is_not_a_live_deployment(self):
        status,body=self.send("GET","/health")
        self.assertEqual(status,200)
        self.assertEqual(body["state"],"not-deployed")
        self.assertEqual(self.server.server_address[0],"127.0.0.1")

    def test_02_request_needs_auth(self):
        status,body=self.send("POST",API_PATH,body={"confirmation":CONFIRM})
        self.assertEqual((status,body["error"]),(401,"auth_required"))

    def test_03_anonymous_and_admin_cannot_act_as_account_owner(self):
        for token in ("forged-jwt",ANON,ADMIN):
            status,_=self.send("POST",API_PATH,token,{"confirmation":CONFIRM})
            self.assertEqual(status,401)

    def test_04_requires_exact_destruction_confirmation(self):
        for txt in ("DELETE","",CONFIRM.lower()):
            status,body=self.send("POST",API_PATH,self.ta,{"confirmation":txt})
            self.assertEqual((status,body["error"]),(422,"confirmation_required"))

    def test_05_never_trusts_supplied_owner_or_request_fields(self):
        for obj in (
            {"confirmation":CONFIRM,"user_id":self.b},
            {"confirmation":CONFIRM,"email":"forged@example.invalid"},
            {"confirmation":CONFIRM,"request_id":"anything"},
        ):
            status,body=self.send("POST",API_PATH,self.ta,obj)
            self.assertEqual(status,400)
            self.assertEqual(body["error"],"invalid_fields")

    def test_06_reject_host_header_and_off_origin_browser(self):
        status,body=self.send("GET","/health",headers={"Host":"evil.example.com"})
        self.assertEqual(status,403)
        status,body=self.send("POST",API_PATH,self.ta,
                  {"confirmation":CONFIRM},headers={"Origin":"https://evil.example.com"})
        self.assertEqual(status,403)
        self.assertEqual(body["error"],"origin_rejected")

    def test_07_cors_preflight_and_http_delete_never_execute_worker(self):
        for method in ("OPTIONS","DELETE"):
            status,_=self.send(method,API_PATH,self.ta)
            self.assertEqual(status,405)
        self.assertEqual(owned_storage_names(self.a),[path(self.a,"x")])

    def test_08_unknown_routes_and_query_parameters_rejected(self):
        status,_=self.send("POST","/v1/account/deletion/execute",self.ta,{"confirmation":CONFIRM})
        self.assertEqual(status,404)
        status,_=self.send("POST",API_PATH+"?user_id="+self.b,self.ta,{"confirmation":CONFIRM})
        self.assertEqual(status,400)

    def test_09_invalid_json_missing_content_type_large_payload(self):
        status,_=self.send("POST",API_PATH,self.ta,{"confirmation":CONFIRM},
                           headers={"Content-Type":"text/plain"})
        self.assertEqual(status,415)
        status,_=self.send("POST",API_PATH,self.ta,{"confirmation":CONFIRM,"filler":"x"*500})
        self.assertEqual(status,413)
        # Real HTTP unparseable JSON, rejecting two keys with same name.
        raw=b'{"confirmation":"DELETE MY BACKLOT ACCOUNT","confirmation":"NO"}'
        req=Request(self.origin+API_PATH,data=raw,method="POST",
            headers={"Authorization":"Bearer "+self.ta,"Content-Type":"application/json"})
        try:
            with build_opener().open(req,timeout=10) as r:
                result=r.status
        except HTTPError as e:
            result=e.code
            e.close()
        self.assertEqual(result,400)

    def test_10_unrequested_user_has_no_receipt(self):
        for t in (self.ta,self.tb):
            status,body=self.send("GET",STATUS_PATH,t)
            self.assertEqual(status,200)
            self.assertEqual(body,{"status":"none"})

    def test_11_unauthenticated_status_is_private(self):
        status,body=self.send("GET",STATUS_PATH)
        self.assertEqual(status,401)
        self.assertNotIn("request_id",body)

    def test_12_submit_creates_private_receipt_and_write_fence_only(self):
        status,body=self.send("POST",API_PATH,self.ta,{"confirmation":CONFIRM})
        self.assertEqual(status,202)
        self.assertEqual(body["status"],"pending")
        self.assertEqual(body["note"],"local_validation_only")
        self.assertNotIn("user_id",body)
        self.assertNotIn("email",json.dumps(body))
        self.__class__.receipt=body["request_id"]
        self.assertEqual(receipt_state(self.receipt),(self.a,"fenced","pending"))
        # Intentional separation: public HTTP request does NOT delete.
        self.assertEqual(len(owned_storage_names(self.a)),1)
        self.assertEqual(sql(f"SELECT count(*) FROM auth.users WHERE id='{self.a}'"),"1")
        self.assertEqual(sql(f"SELECT blocked::text FROM backlot_rc22_ci.deletion_fences "
                             f"WHERE user_id='{self.a}'"),"true")

    def test_13_repeat_request_returns_same_receipt(self):
        status,body=self.send("POST",API_PATH,self.ta,{"confirmation":CONFIRM})
        self.assertEqual(status,202)
        self.assertEqual(body["request_id"],self.receipt)

    def test_14_user_b_cannot_read_a_receipt(self):
        status,body=self.send("GET",STATUS_PATH,self.tb)
        self.assertEqual(status,200)
        self.assertEqual(body,{"status":"none"})
        self.assertNotIn(self.receipt,json.dumps(body))

    def test_15_owner_can_poll_before_worker_runs(self):
        status,body=self.send("GET",STATUS_PATH,self.ta)
        self.assertEqual(status,200)
        self.assertEqual(body["request_id"],self.receipt)
        self.assertEqual(body["stage"],"fenced")
        self.assertEqual(body["status"],"pending")

    def test_16_fenced_a_cannot_write_b_continues_working(self):
        s,_=uploaded(self.ta,path(self.a,"fenced"),b"blocked")
        self.assertIn(s,(400,401,403,422))
        s,_=uploaded(self.tb,path(self.b,"still-ok"),b"hello")
        self.assertIn(s,(200,201))

    def test_17_failure_injection_yields_owner_scoped_retry_status(self):
        with self.assertRaisesRegex(RuntimeError,"Injected"):
            resume_internal(self.receipt,fail_after_fence=True)
        s,body=self.send("GET",STATUS_PATH,self.ta)
        self.assertEqual(s,200)
        self.assertEqual(body["status"],"retryable")
        self.assertEqual(body["stage"],"fenced")
        self.assertEqual(len(owned_storage_names(self.a)),1)

    def test_18_internal_worker_completes_actual_local_deletion(self):
        self.assertEqual(resume_internal(self.receipt),"complete")
        self.assertEqual(receipt_state(self.receipt),(self.a,"complete","complete"))
        self.assertEqual(owned_storage_names(self.a),[])
        self.assertEqual(sql(f"SELECT count(*) FROM auth.users WHERE id='{self.a}'"),"0")
        self.assertEqual(sql(f"SELECT count(*) FROM auth.users WHERE id='{self.b}'"),"1")
        self.assertEqual(len(owned_storage_names(self.b)),2)
        self.assertEqual(resume_internal(self.receipt),"complete")

    def test_19_b_still_has_auth_login_and_creator_data(self):
        status,_=api("POST","/auth/v1/token?grant_type=password",ANON,
          {"email":"rc27-owner-b@example.invalid",
           "password":"rc22-local-test-only-Password77!"})
        self.assertEqual(status,200)
        self.assertEqual(sql(f"SELECT count(*) FROM public.creator_sync_snapshots "
                             f"WHERE user_id='{self.b}'"),"1")

    def test_20_no_source_code_service_key_or_requester_id_leaks(self):
        status,body=self.send("GET","/health")
        txt=json.dumps(body).lower()
        self.assertNotIn("service_role",txt)
        self.assertNotIn("apikey",txt)
        self.assertNotIn(self.a,txt)
        self.assertNotIn(self.b,txt)
        self.assertNotIn("password",txt)

if __name__=="__main__":
    unittest.main(verbosity=2)
