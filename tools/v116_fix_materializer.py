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
text = text.replace(old, new, 1)

post = '''\n\n# Opportunity details use a lightweight disclosure instead of an always-expanded technical block.\n_opp_path = "app/src/main/java/com/framebynavin/app/ui/V20OpportunityEngineUi.kt"\n_opp = load(_opp_path)\nif "import androidx.compose.animation.AnimatedVisibility" not in _opp:\n    _opp = _opp.replace(\n        "import androidx.compose.foundation.BorderStroke\\n",\n        "import androidx.compose.animation.AnimatedVisibility\\nimport androidx.compose.foundation.BorderStroke\\n",\n        1,\n    )\n_opp = _opp.replace("rememberSaveable(primary.id)", "remember(primary.id)")\nsave(_opp_path, _opp)\n'''
if "Opportunity details use a lightweight disclosure" not in text:
    text += post

cleanup = '''\n\n# Normalize generated Kotlin whitespace so git diff --check stays strict.\nfor _path in [\n    "app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt",\n    "app/src/main/java/com/framebynavin/app/ui/V20OpportunityEngineUi.kt",\n    "app/src/main/java/com/framebynavin/app/ui/V18CoreScreens.kt",\n    "app/src/main/java/com/framebynavin/app/ui/V18TodayScreen.kt",\n]:\n    _value = load(_path)\n    _had_newline = _value.endswith("\\n")\n    _value = "\\n".join(line.rstrip() for line in _value.splitlines())\n    save(_path, _value + ("\\n" if _had_newline else ""))\n'''
if "Normalize generated Kotlin whitespace" not in text:
    text += cleanup
path.write_text(text, encoding="utf-8")
print("v116 materializer IME, opportunity disclosure and whitespace patch fixed")
