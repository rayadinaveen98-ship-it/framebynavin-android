#!/usr/bin/env python3
"""RC2.3: align Studio test with the production workflow action label."""
from pathlib import Path
import hashlib

ROOT = Path(__file__).resolve().parents[2]
path = ROOT / "app/src/androidTest/java/com/framebynavin/app/ui/V18CoreInteractionUiTest.kt"
source = path.read_text()
assert hashlib.sha256(source.encode()).hexdigest() == "02deb5e4cb54c5f50a5667fd9853827395aba2ea4bb8740d02a64f2e01414b15", "Unexpected RC2.2 Studio baseline"

def replace_once(old, new):
    global source
    if source.count(old) != 1:
        raise RuntimeError("Studio label patch did not match exactly once")
    source = source.replace(old, new, 1)

replace_once("import com.framebynavin.app.data.CreatorTask\n", "import com.framebynavin.app.data.CreatorTask\nimport com.framebynavin.app.data.CreatorWorkflowEngine\n")
replace_once('        composeRule.onNodeWithText("MARK STEP DONE")\n            .performScrollTo()\n', '        composeRule.onNodeWithText(CreatorWorkflowEngine.stageActionLabel(task))\n            .performScrollTo()\n')
assert hashlib.sha256(source.encode()).hexdigest() == "ca221152a234fea771ecbf86d51caf37b7aaa3655eae6b87d6d9f2519e1a8cc9", "Unexpected patched Studio source"
path.write_text(source)
print("RC2.3 Studio workflow label regression fix applied")
