#!/usr/bin/env python3
"""Apply the narrow RC2.2 Studio scrollability regression fix, fail-closed."""
from pathlib import Path
import hashlib

ROOT = Path(__file__).resolve().parents[2]
path = ROOT / "app/src/androidTest/java/com/framebynavin/app/ui/V18CoreInteractionUiTest.kt"
original = path.read_text()
expected = "a6c0daaaa74d1a6f09a69e1889a6b7252bd0861affa8b80a5d4f89d2e58f031d"
assert hashlib.sha256(original.encode()).hexdigest() == expected, "Unexpected Studio test baseline"

def replace_once(old, new):
    global original
    if original.count(old) != 1:
        raise RuntimeError("Studio regression patch did not match exactly once")
    original = original.replace(old, new, 1)

replace_once("import androidx.compose.ui.test.performClick\n", "import androidx.compose.ui.test.performClick\nimport androidx.compose.ui.test.performScrollTo\n")
replace_once('        composeRule.onNodeWithText(task.title).performClick()\n        composeRule.onNodeWithText("WORK ON · IDEA").assertIsDisplayed().performClick()\n        composeRule.onNodeWithText("MARK STEP DONE").assertIsDisplayed().performClick()\n', '        composeRule.onNodeWithText(task.title).performClick()\n        composeRule.onNodeWithText("WORK ON · IDEA")\n            .performScrollTo()\n            .assertIsDisplayed()\n            .performClick()\n        composeRule.onNodeWithText("MARK STEP DONE")\n            .performScrollTo()\n            .assertIsDisplayed()\n            .performClick()\n')
assert hashlib.sha256(original.encode()).hexdigest() == "02deb5e4cb54c5f50a5667fd9853827395aba2ea4bb8740d02a64f2e01414b15"
path.write_text(original)
print("RC2.2 Studio scrollability regression fix applied")
