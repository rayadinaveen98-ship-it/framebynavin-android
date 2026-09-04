from pathlib import Path
p = Path('app/src/main/java/com/framebynavin/app/ui/V17AutomationCenter.kt')
s = p.read_text()
old = 'it.sourceRefId.startsWith("postpublish:")'
new = 'it.sourceRefId.startsWith("post-publish:")'
if s.count(old) != 1:
    raise RuntimeError(f'expected one post-publish status key, found {s.count(old)}')
p.write_text(s.replace(old, new, 1))
