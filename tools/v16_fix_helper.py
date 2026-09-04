from pathlib import Path

p = Path('tools/v16_apply_creator_intelligence.py')
s = p.read_text()
old = "s = rep(s, '            syncWithToken(token)\\n', '            syncWithToken(token, pendingSyncDays)\\n', 'YT resolution token')"
new = "token_call = '            syncWithToken(token)\\n'\ntoken_count = s.count(token_call)\nif token_count != 2:\n    raise RuntimeError(f'YT token calls: expected 2, found {token_count}')\ns = s.replace(token_call, '            syncWithToken(token, pendingSyncDays)\\n', 2)"
if old not in s:
    raise RuntimeError('resolution-token helper line not found')
s = s.replace(old, new, 1)
old2 = "s = rep(s, '                        syncWithToken(token)\\n', '                        syncWithToken(token, pendingSyncDays)\\n', 'YT direct token')\n"
if old2 not in s:
    raise RuntimeError('direct-token helper line not found')
s = s.replace(old2, '', 1)
p.write_text(s)
print('fixed v1.6 helper token wiring')
