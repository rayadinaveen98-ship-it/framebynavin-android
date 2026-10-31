#!/usr/bin/env python3
"""Safe static Backlot privacy/deletion PREVIEW contract.

This test deliberately refuses any claim of a functioning deletion endpoint
while Backlot's real account-deletion backend is not deployed. It checks the
mobile privacy screen copy, site navigability and explicit launch-blocker text.
It sends no account info and requires no network or paid services.
"""
from __future__ import annotations

from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit
import re
import unittest

ROOT=Path(__file__).resolve().parents[2]
SITE=ROOT/"release-site"
ANDROID=ROOT/"app/src/main/java/com/framebynavin/app/ui/V269DataPrivacyActivity.kt"

class Scanner(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.links=[]
        self.tags=[]
        self.text=[]
    def handle_starttag(self,tag,attrs):
        d=dict(attrs);self.tags.append((tag,d))
        if tag=="a":self.links.append(d.get("href",""))
    def handle_data(self,data):self.text.append(data)

class PlayDraftGuard(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.paths=[SITE/"index.html",SITE/"privacy/index.html",SITE/"delete-account/index.html"]
        cls.pages={}
        for p in cls.paths:
            html=p.read_text(encoding="utf-8")
            parser=Scanner();parser.feed(html);parser.close()
            cls.pages[p]=(html,parser)

    def test_all_pages_exist_and_have_no_external_tracking(self):
        for path,(html,doc) in self.pages.items():
            with self.subTest(page=str(path.relative_to(SITE))):
                self.assertIn('name="robots" content="noindex,nofollow"',html)
                self.assertNotIn("<script",html.lower())
                self.assertNotIn("<iframe",html.lower())
                self.assertNotIn("<form",html.lower())
                self.assertFalse(any(t=="img" for t,a in doc.tags))
                self.assertTrue(any(t=="meta" and a.get("name")=="viewport" for t,a in doc.tags))
                self.assertTrue(any(t=="main" for t,a in doc.tags))
                self.assertTrue(any(t=="footer" for t,a in doc.tags))

    def test_local_hrefs_all_resolve_and_no_mailto_guess(self):
        for path,(_,doc) in self.pages.items():
            for href in doc.links:
                with self.subTest(path=path.name,href=href):
                    self.assertFalse(href.startswith(("https:","http:","mailto:","javascript:")))
                    url=urlsplit(href)
                    self.assertFalse(url.netloc or url.scheme)
                    target=(path.parent/unquote(url.path)).resolve()
                    self.assertTrue(target.is_relative_to(SITE),"Path escaped site tree")
                    if target.is_dir():target/= "index.html"
                    self.assertTrue(target.is_file(),f"Broken relative link: {href}")

    def test_all_pages_have_local_css(self):
        for path,(html,doc) in self.pages.items():
            styles=[a.get("href") for t,a in doc.tags if t=="link" and a.get("rel")=="stylesheet"]
            self.assertEqual(len(styles),1)
            self.assertTrue((path.parent/styles[0]).is_file())

    def test_never_represent_preview_as_live_public_policy(self):
        privacy=self.pages[SITE/"privacy/index.html"][0]
        deletion=self.pages[SITE/"delete-account/index.html"][0]
        home=self.pages[SITE/"index.html"][0]
        self.assertIn('data-policy-state="draft"',privacy)
        self.assertIn('data-deletion-state="unavailable"',deletion)
        self.assertIn('data-site-state="draft"',home)
        self.assertIn("not an approved or published privacy policy",privacy)
        self.assertIn("not being accepted through this preview page",deletion)
        self.assertIn("not an eligible Google Play account-deletion URL",deletion)
        self.assertIn("No request was submitted or stored",deletion)
        self.assertIn("not live",home.lower())

    def test_privacy_discloses_all_known_categories_and_exposes_uncertainty(self):
        privacy=self.pages[SITE/"privacy/index.html"][0]
        for text in ("Supabase","Google sign-in","YouTube","Google Calendar",
                     "Firebase AI","Personal Reminders","not encrypted",
                     "recording","permission","retention","pending owner confirmation"):
            self.assertIn(text.lower(),privacy.lower())
        self.assertNotIn('data-policy-state="published"',privacy)
        self.assertNotIn("we delete within 30 days",privacy.lower())

    def test_deletion_scope_includes_real_service_and_excludes_google_account(self):
        deletion=self.pages[SITE/"delete-account/index.html"][0]
        for text in ("Verify the requester","Voice Idea","Storage API",
                     "account profile","authentication account",
                     "Google account","Personal Reminders","not live yet"):
            self.assertIn(text.lower(),deletion.lower())

    def test_source_android_explicitly_reports_deletion_unavailable(self):
        src=ANDROID.read_text(encoding="utf-8")
        self.assertIn('Full Backlot account deletion is not available in this preview.',src)
        self.assertIn('No account deletion request was sent.',src)
        self.assertIn('app-owned recordings',src)
        self.assertIn('not encrypted',src)
        self.assertIn('Signing out does not erase',src)

    def test_site_is_not_falsely_claimed_as_play_submission(self):
        docs=(ROOT/"docs/release/PLAY_PRIVACY_SITE_REVIEW_NOT_LIVE.md").read_text()
        for item in ("NOT PLAY-READY","NO HOSTED DEPLOYMENT","verified support contact",
                     "real deletion endpoint","owner approval"):
            self.assertIn(item.lower(),docs.lower())

if __name__=="__main__":
    unittest.main(verbosity=2)
