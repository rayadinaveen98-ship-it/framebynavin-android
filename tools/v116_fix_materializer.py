from pathlib import Path

path = Path(__file__).with_name("v20_beta1_ux_stabilization.py")
text = path.read_text(encoding="utf-8")
old = '''text = replace_once(
    text,
    "Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {",
    "Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {",
    "composer ime padding",
)
'''
new = '''composer_fn = text.find("internal fun PProjectComposer(")
if composer_fn < 0:
    raise RuntimeError("PProjectComposer missing")
composer_column = text.find("Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {", composer_fn)
if composer_column < 0:
    raise RuntimeError("composer root column missing")
old_column = "Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {"
new_column = "Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {"
text = text[:composer_column] + new_column + text[composer_column + len(old_column):]
'''
if old not in text:
    raise RuntimeError("IME materializer block not found")
path.write_text(text.replace(old, new, 1), encoding="utf-8")
print("v116 materializer IME patch fixed")
