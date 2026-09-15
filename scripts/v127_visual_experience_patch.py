from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def p(rel): return ROOT / rel

def replace(rel, old, new):
    path = p(rel)
    text = path.read_text()
    if old not in text:
        raise SystemExit(f'Missing expected block in {rel}: {old[:120]!r}')
    path.write_text(text.replace(old, new))

def append_once(rel, marker, text):
    path = p(rel)
    cur = path.read_text()
    if marker not in cur:
        path.write_text(cur + text)

print('v127 patch placeholder; implementation will be filled by workflow revision')
