from pathlib import Path

TEST = Path('app/src/androidTest/java/com/framebynavin/app/ui/V18CreatorJourneyAlpha12UiTest.kt')

text = TEST.read_text()
old_import = 'import androidx.compose.ui.test.assertDoesNotExist\n'
old_assertion = '        composeRule.onNodeWithText("The project will enter Studio with the correct workflow and an automatic reminder mode for its format.").assertDoesNotExist()\n'

if text.count(old_import) != 1:
    raise SystemExit(f'expected one assertDoesNotExist import, found {text.count(old_import)}')
if text.count(old_assertion) != 1:
    raise SystemExit(f'expected one stale-copy negative assertion, found {text.count(old_assertion)}')

text = text.replace(old_import, '', 1)
text = text.replace(old_assertion, '', 1)
TEST.write_text(text)
print('Applied Alpha12 Compose test API compatibility fix')
