from pathlib import Path

p = Path('app/src/main/java/com/framebynavin/app/ui/CopilotActivity.kt')
s = p.read_text()
needle = 'import androidx.compose.runtime.*\n'
replacement = 'import androidx.compose.runtime.*\nimport androidx.compose.runtime.saveable.rememberSaveable\n'
if replacement in s:
    print('saveable import already present')
elif s.count(needle) == 1:
    p.write_text(s.replace(needle, replacement, 1))
    print('saveable import added')
else:
    raise SystemExit(f'Expected one runtime import, found {s.count(needle)}')
