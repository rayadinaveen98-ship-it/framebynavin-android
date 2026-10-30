#!/usr/bin/env python3
"""RC30 deterministic no-network unit tests of privileged DELETE-only facade."""
from __future__ import annotations
import unittest
from uuid import uuid4
from privileged_storage_delete_only import (
    DeleteOnlyDenied,DeleteOnlyStorageWorker,VOICE_BUCKET,check_voice_path
)

class BoundaryTests(unittest.TestCase):
    def setUp(self):
        self.a=str(uuid4())
        self.b=str(uuid4())
        self.rid=str(uuid4())
        self.name=f"{self.a}/idea-abc/media/deadbeef.m4a"
        self.calls=[]
        self.fenced=True
        self.stage="fenced"
        self.status="pending"
        self.worker=DeleteOnlyStorageWorker(
            lambda _id:(self.a,self.stage,self.status),
            lambda uid:self.fenced and uid==self.a,
            lambda bucket,names:self._transport(bucket,names),
        )

    def _transport(self,bucket,names):
        self.calls.append((bucket,names))
        return 200,{"removed":names}

    def denied(self,names,receipt=None):
        with self.assertRaises(DeleteOnlyDenied):
            self.worker.remove_objects_for_receipt(receipt or self.rid,names)
        self.assertEqual(self.calls,[])

    def test_delete_single_owned_file_only(self):
        self.assertEqual(self.worker.remove_objects_for_receipt(self.rid,[self.name]),1)
        self.assertEqual(self.calls,[(VOICE_BUCKET,[self.name])])

    def test_cross_account_denied_before_transport(self):
        self.denied([self.name,f"{self.b}/idea-abc/media/x.m4a"])

    def test_bucket_is_fixed_and_fails_closed(self):
        with self.assertRaises(DeleteOnlyDenied):
            DeleteOnlyStorageWorker(lambda _id:None,lambda uid:True,
                                    lambda b,n:(200,None),bucket="another-bucket")

    def test_missing_valid_private_receipt_required(self):
        self.denied([self.name],"not-a-receipt")

    def test_receipt_owner_not_client_supplied(self):
        self.denied([f"{self.b}/idea-abc/media/x.m4a"])

    def test_fence_is_mandatory_even_if_receipt_exists(self):
        self.fenced=False
        self.denied([self.name])

    def test_only_fenced_stage_can_mutate_storage(self):
        for state in ("storage_deleted","data_deleted","auth_deleted","complete"):
            self.stage=state
            self.denied([self.name])

    def test_completed_and_invalid_status_rejected(self):
        for status in ("complete","unknown",""):
            self.status=status
            self.denied([self.name])

    def test_missing_or_empty_or_large_batch_is_denied(self):
        self.denied([])
        self.denied([self.name]*101)

    def test_duplicate_paths_cannot_cause_repeated_deletes(self):
        self.denied([self.name,self.name])

    def test_invalid_names_rejected(self):
        invalid=(
            f"{self.a}/../media/x.m4a",
            f"{self.a}/idea-x/media/../../x.m4a",
            f"{self.a}/idea-x/media/x.txt",
            f"{self.a}/idea-x/media/x.m4a?bad",
            f"{self.a}/idea-x/media/x%2fm4a",
            f"{self.a}/idea-x/media/x.m4a/",
            f"{self.a}\\idea-x\\media\\x.m4a",
        )
        for name in invalid:
            with self.subTest(name=name):
                self.denied([name])

    def test_only_exact_deletion_transport_exposed(self):
        for attribute in ("upload","upsert","replace","copy","move",
                          "admin_upload","request","execute","send"):
            self.assertFalse(hasattr(self.worker,attribute),attribute)

    def test_failed_storage_api_does_not_report_success(self):
        self.worker=DeleteOnlyStorageWorker(
            lambda _id:(self.a,"fenced","pending"),
            lambda _id:True,
            lambda _bucket,_names:(403,{"error":"denied"}),
        )
        self.denied([self.name])

    def test_empty_or_wrong_receipt_result_denied(self):
        self.worker=DeleteOnlyStorageWorker(
            lambda _id:("not-a-uuid","fenced","pending"),
            lambda _id:True,
            lambda bucket,names:self._transport(bucket,names),
        )
        self.denied([self.name])

if __name__=="__main__":
    unittest.main(verbosity=2)
