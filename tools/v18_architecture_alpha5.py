from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_required(rel: str, old: str, new: str, count: int = 1) -> None:
    text = read(rel)
    hits = text.count(old)
    if hits != count:
        raise SystemExit(f"Expected {count} occurrence(s) in {rel}, found {hits}: {old!r}")
    write(rel, text.replace(old, new, count))


# Alpha5: continue decomposing the active app shell by moving Today into its own screen file.
build = "app/build.gradle.kts"
replace_required(build, "versionCode = 44", "versionCode = 45")
replace_required(
    build,
    'versionName = "1.8.0-foundation-alpha4"',
    'versionName = "1.8.0-foundation-alpha5"',
)

root_rel = "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
text = read(root_rel)
start_marker = "@Composable\nprivate fun PTodayScreen("
end_marker = "@Composable\nprivate fun PControlCenter("
if text.count(start_marker) != 1 or text.count(end_marker) != 1:
    raise SystemExit("Could not locate the Today extraction block exactly once")

start = text.index(start_marker)
end = text.index(end_marker)
segment = text[start:end]
segment = segment.replace(
    "@Composable\nprivate fun PTodayScreen(",
    "@Composable\ninternal fun PTodayScreen(",
    1,
)

header_end = text.index("private enum class PTab")
header = text[:header_end].rstrip() + "\n\n"
new_rel = "app/src/main/java/com/framebynavin/app/ui/V18TodayScreen.kt"
if (ROOT / new_rel).exists():
    raise SystemExit(f"{new_rel} already exists; refusing to overwrite")
write(new_rel, header + segment.rstrip() + "\n")

text = text[:start] + text[end:]
for name in ["PHomeGreetingHeader", "PQueueDots"]:
    old = f"private fun {name}("
    hits = text.count(old)
    if hits != 1:
        raise SystemExit(f"Expected one helper {name}, found {hits}")
    text = text.replace(old, f"internal fun {name}(", 1)

write(root_rel, text)

root_after = read(root_rel)
today_after = read(new_rel)
if "fun PTodayScreen(" in root_after:
    raise SystemExit("PTodayScreen still exists in the app shell after extraction")
if "internal fun PTodayScreen(" not in today_after:
    raise SystemExit("PTodayScreen missing from extracted Today file")

root_lines = len(root_after.splitlines())
if root_lines > 1120:
    raise SystemExit(f"App shell is still unexpectedly large after Today extraction: {root_lines} lines")

print("v1.8 alpha5 Today architecture split applied")
print(f"FrameByNavinV101BApp.kt: {root_lines} lines")
print(f"V18TodayScreen.kt: {len(today_after.splitlines())} lines")
