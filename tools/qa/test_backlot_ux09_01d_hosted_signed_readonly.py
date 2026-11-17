#!/usr/bin/env python3
"""No-network safety and execution-contract tests for UX09-01D."""
from __future__ import annotations

import contextlib
import importlib.util
import io
import os
from pathlib import Path
from unittest import TestCase, main, mock

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("ux09_01d", HERE / "backlot_ux09_01d_hosted_signed_readonly.py")
assert spec and spec.loader
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)

class HostedSafetyTests(TestCase):
    def setUp(self):
        self.batch = "cafebabe"
        self.mail_a = f"ux09-qa-a-{self.batch}@example.invalid"
        self.mail_b = f"ux09-qa-b-{self.batch}@example.invalid"
        self.env = {
            "UX09_QA_MODE": runner.EXACT_MODE,
            "UX09_QA_A_EMAIL": self.mail_a,
            "UX09_QA_B_EMAIL": self.mail_b,
            "UX09_QA_A_PASSWORD": "only-disposable-qa-secret-a",
            "UX09_QA_B_PASSWORD": "only-disposable-qa-secret-b",
        }

    def test_live_host_is_exact_lab(self):
        self.assertEqual(runner.HOST, "https://vasleafrmsovdbdlrcsw.supabase.co")
        self.assertTrue(runner.key_from_checked_in_lab_client().startswith("sb_publishable_"))

    def test_qa_email_batch_and_role_fencing(self):
        runner.validate_qa_emails(self.mail_a, self.mail_b)
        for a, b in [
            ("person@gmail.com", self.mail_b),
            (self.mail_b, self.mail_a),
            (self.mail_a, "ux09-qa-b-12345678@example.invalid"),
            (self.mail_a, "ux09-qa-b-cafebabe@real.com"),
            (self.mail_a, self.mail_a),
        ]:
            with self.subTest(a=a, b=b), self.assertRaises(runner.StopCheck):
                runner.validate_qa_emails(a, b)

    def test_profile_auth_email_id_and_anonymous_gate(self):
        uid = "f4242073-7ff0-4828-a9ac-924edbb6339f"
        self.assertEqual(runner.validate_auth_user({"email": self.mail_a, "id": uid, "is_anonymous": False}, self.mail_a), uid)
        for bad in [
            {"email": self.mail_b, "id": uid},
            {"email": self.mail_a, "id": uid, "is_anonymous": True},
            {"email": self.mail_a, "id": "garbage"},
            {"email": self.mail_a, "id": str("00000000-0000-0000-0000-000000000000")},
            {"email": self.mail_a},
            None,
        ]:
            with self.subTest(bad=bad), self.assertRaises(runner.StopCheck):
                runner.validate_auth_user(bad, self.mail_a)

    def test_empty_pull_fails_closed(self):
        runner.validate_empty_pull({"changes": [], "next_cursor": 0, "has_more": False})
        for bad in [
            {"changes": [{"payload": {"title": "private"}}], "next_cursor": 1, "has_more": False},
            {"changes": [], "next_cursor": 1, "has_more": False},
            {"changes": [], "next_cursor": 0, "has_more": True},
            {},
            [],
            None,
        ]:
            with self.subTest(bad=bad), self.assertRaises(runner.StopCheck):
                runner.validate_empty_pull(bad)

    def test_private_schema_rejection_mandatory(self):
        for code in [404, 406]:
            runner.validate_private_denial(code)
        for code in [200, 201, 204, 400, 401, 403, 500]:
            with self.subTest(code=code), self.assertRaises(runner.StopCheck):
                runner.validate_private_denial(code)

    def test_forbid_signup_admin_and_entity_mutations(self):
        for path, verb in [
            ("/auth/v1/signup", "POST"),
            ("/auth/v1/admin/users", "DELETE"),
            ("/rest/v1/entities", "DELETE"),
            ("/rest/v1/rpc/backlot_ux09_apply_v1", "POST"),
            ("/rest/v1/rpc/backlot_ux09_pull_v1", "POST"),
            ("/rest/v1/rpc/backlot_ux09_pull_v1?p_after_sequence=0&p_limit=200", "GET"),
        ]:
            with self.subTest(path=path, verb=verb), self.assertRaises(runner.StopCheck):
                runner.request_json(path, verb, "public-test-key")

    def test_explicit_mode_required_before_network(self):
        with mock.patch.dict(os.environ, {**self.env, "UX09_QA_MODE": "other"}, clear=True):
            with self.assertRaises(runner.StopCheck):
                runner.main()

    def test_mocked_full_flow_no_writes_and_logout(self):
        calls = []
        profiles = {
            "jwt-a": {"id": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", "email": self.mail_a, "is_anonymous": False},
            "jwt-b": {"id": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb", "email": self.mail_b, "is_anonymous": False},
        }
        def fake_request(path, method, key, token=None, body=None, extra_headers=None):
            calls.append((path, method))
            if path == runner.AUTH_PATH:
                return 200, {"access_token": "jwt-a" if body["email"] == self.mail_a else "jwt-b"}
            if path == runner.USER_PATH:
                return 200, profiles[token]
            if path == runner.PULL_PATH:
                return 200, {"changes": [], "next_cursor": 0, "has_more": False}
            if path == runner.PRIVATE_PATH:
                self.assertEqual(extra_headers, {"Accept-Profile": "backlot_sync_internal"})
                return 406, {"code": "PGRST106"}
            if path == runner.LOGOUT_PATH:
                return 204, None
            self.fail("unplanned path: " + path)
        output = io.StringIO()
        with mock.patch.dict(os.environ, self.env, clear=True), mock.patch.object(runner, "request_json", side_effect=fake_request), contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            self.assertEqual(runner.main(), 0)
        self.assertEqual(len(calls), 10)
        self.assertEqual(sum(path == runner.LOGOUT_PATH for path, _ in calls), 2)
        self.assertTrue(all((path, method) in runner.ALLOWED for path, method in calls))
        for secret in self.env.values():
            self.assertNotIn(secret, output.getvalue())
        self.assertNotIn(self.mail_a, output.getvalue())
        self.assertNotIn(self.mail_b, output.getvalue())

    def test_cleanup_failure_is_not_certified(self):
        def fake_request(path, method, key, token=None, body=None, extra_headers=None):
            if path == runner.AUTH_PATH:
                return 200, {"access_token": "jwt-a" if body["email"] == self.mail_a else "jwt-b"}
            if path == runner.USER_PATH:
                email = self.mail_a if token == "jwt-a" else self.mail_b
                user_id = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa" if token == "jwt-a" else "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
                return 200, {"email": email, "id": user_id, "is_anonymous": False}
            if path == runner.PULL_PATH:
                return 200, {"changes": [], "next_cursor": 0, "has_more": False}
            if path == runner.PRIVATE_PATH:
                return 406, {}
            if path == runner.LOGOUT_PATH:
                return 500, {}
            self.fail("unknown route")
        with mock.patch.dict(os.environ, self.env, clear=True), mock.patch.object(runner, "request_json", side_effect=fake_request), contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(runner.main(), 3)

if __name__ == "__main__":
    main()
