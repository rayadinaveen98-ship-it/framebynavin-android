from pathlib import Path
import re

ROOT = Path('app/src/main')
# Technical identifiers are intentionally allowed when they are not user-facing strings.
STRING_PATTERNS = [
    re.compile(r'"[^"\n]*(?:FrameByNavin|Frame by Navin|FRAME BY NAVIN)[^"\n]*"'),
]
ALLOW = {
    'FrameByNavinCloudBackup',
    'FrameByNavinBackup',
    'com.framebynavin.app',
}

violations = []
for path in ROOT.rglob('*'):
    if path.suffix.lower() not in {'.kt', '.xml', '.java'}:
        continue
    text = path.read_text(encoding='utf-8', errors='ignore')
    for lineno, line in enumerate(text.splitlines(), 1):
        for pattern in STRING_PATTERNS:
            for match in pattern.findall(line):
                if any(token in match for token in ALLOW):
                    continue
                # Ignore source/class/resource identifiers that only appear in code references.
                if 'Theme.FrameByNavin' in match:
                    continue
                violations.append(f'{path}:{lineno}: {match}')

if violations:
    print('User-facing legacy brand strings found:')
    print('\n'.join(violations))
    raise SystemExit(1)
print('V137 visible brand audit: PASS')
