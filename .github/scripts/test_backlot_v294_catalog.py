#!/usr/bin/env python3
"""Regression checks for the audit scanner only; no Android app data touched."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

SCRIPT=Path(__file__).with_name("backlot_v294_catalog.py")
spec=importlib.util.spec_from_file_location("v294_catalog",SCRIPT)
mod=importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

class CatalogTest(unittest.TestCase):
    def test_ignores_comments_and_char_literals(self):
        source=r'''// Text("not visible")
/* Nested /* Text("hidden") */ comment */
Text("Visible")
val character = 'x'
Text("a \"quoted\" string")
'''
        tokens,mask=mod.lex(source)
        self.assertEqual([t[1] for t in tokens],["Visible",r'a \"quoted\" string'])
        calls=mod.ancestors(mask,[t[0] for t in tokens])
        self.assertTrue(all("Text" in c for c in calls))
    def test_handles_raw_strings_and_multiple_calls(self):
        src='''Text("One")
V144CategoryCard("Account", "Connected") {}
val internal = "not published"
Text("""multi
line""")'''
        tokens,mask=mod.lex(src)
        calls=mod.ancestors(mask,[t[0] for t in tokens])
        self.assertEqual(calls[0][-1],"Text")
        self.assertEqual(calls[1][-1],"V144CategoryCard")
        self.assertEqual(calls[3],())
        self.assertEqual(tokens[-1][1],"multi\nline")
    def test_nested_kotlin_template_does_not_leak_fake_labels(self):
        source='Text("Mode: ' + '
        self.assertEqual(mod.classify("https://example.com","Text(",["Text"]),"excluded")
        self.assertEqual(mod.classify("secret-value","val apiKey = ",()),"excluded")
    def test_audit_only_writes_reports_not_source(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            file=root/"app/src/main/java/example/Main.kt"
            file.parent.mkdir(parents=True)
            original='''@Composable fun Screen() {
Text("Hello")
Text("Tiny", fontSize = 9.sp)
}
fun seedDemo() = "not visible"
'''
            file.write_text(original,encoding="utf-8")
            report=mod.audit(root,root/"audit-result")
            self.assertEqual(file.read_text(encoding="utf-8"),original)
            self.assertEqual(report["kotlin_files"],1)
            self.assertEqual(report["font_declarations_under_12sp"],1)
            self.assertGreaterEqual(report["copy_candidates_total"],2)
            self.assertNotIn("not visible",(root/"audit-result/copy-candidates.csv").read_text("utf-8"))
            self.assertTrue((root/"audit-result/summary.json").exists())

if __name__=="__main__":
    unittest.main()
 + '{if (ready) "Ready" else "Not ready"}")'
        tokens,mask=mod.lex(source)
        self.assertEqual(len(tokens),1)
        self.assertIn('else "Not ready"',tokens[0][1])
        self.assertEqual(mod.ancestors(mask,[tokens[0][0]])[0][-1],"Text")
    def test_nested_internal_test_tag_is_not_visible_copy(self):
        source='Text("Actual text", modifier = Modifier.testTag("v289-error"))'
        tokens,mask=mod.lex(source)
        calls=mod.ancestors(mask,[x[0] for x in tokens])
        self.assertEqual(mod.classify(tokens[0][1],"Text(",calls[0]),"direct-ui")
        self.assertEqual(mod.classify(tokens[1][1],"",calls[1]),"excluded")
    def test_excludes_machine_strings_and_secrets(self):
        self.assertEqual(mod.classify("https://example.com","Text(",["Text"]),"excluded")
        self.assertEqual(mod.classify("secret-value","val apiKey = ",()),"excluded")
    def test_audit_only_writes_reports_not_source(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            file=root/"app/src/main/java/example/Main.kt"
            file.parent.mkdir(parents=True)
            original='''@Composable fun Screen() {
Text("Hello")
Text("Tiny", fontSize = 9.sp)
}
fun seedDemo() = "not visible"
'''
            file.write_text(original,encoding="utf-8")
            report=mod.audit(root,root/"audit-result")
            self.assertEqual(file.read_text(encoding="utf-8"),original)
            self.assertEqual(report["kotlin_files"],1)
            self.assertEqual(report["font_declarations_under_12sp"],1)
            self.assertGreaterEqual(report["copy_candidates_total"],2)
            self.assertNotIn("not visible",(root/"audit-result/copy-candidates.csv").read_text("utf-8"))
            self.assertTrue((root/"audit-result/summary.json").exists())

if __name__=="__main__":
    unittest.main()
