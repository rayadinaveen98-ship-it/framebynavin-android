from pathlib import Path

# Compatibility shim for the Compose UI test API used by this Android build.
TEST = Path('app/src/androidTest/java/com/framebynavin/app/ui/V18CinematicHomeHeroAlpha16UiTest.kt')
text = TEST.read_text()
old = 'import androidx.compose.ui.test.onNode\n'
if text.count(old) != 1:
    raise SystemExit(f'alpha16 test import: expected one match, found {text.count(old)}')
TEST.write_text(text.replace(old, '', 1))
print('Applied Alpha16 Compose instrumentation test compatibility fix')
