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


# Alpha4: first architecture-decomposition pass. The approved v1.7.5 line stays untouched.
build = "app/build.gradle.kts"
replace_required(build, "versionCode = 43", "versionCode = 44")
replace_required(
    build,
    'versionName = "1.8.0-foundation-alpha3"',
    'versionName = "1.8.0-foundation-alpha4"',
)

root_rel = "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
text = read(root_rel)
start_marker = "@Composable\nprivate fun PPlanScreen("
end_marker = "@Composable\nprivate fun PControlCenter("
if text.count(start_marker) != 1 or text.count(end_marker) != 1:
    raise SystemExit("Could not locate the Plan/Studio/Insights extraction block exactly once")

start = text.index(start_marker)
end = text.index(end_marker)
segment = text[start:end]

# These screen entry points are package-internal so the app shell can call them from the
# extracted file without exposing them outside the UI package.
segment = segment.replace(
    "@Composable\nprivate fun PPlanScreen(",
    "@Composable\ninternal fun PPlanScreen(",
    1,
)
segment = segment.replace(
    "@Composable\nprivate fun PStudioScreen(",
    "@Composable\ninternal fun PStudioScreen(",
    1,
)
segment = segment.replace(
    "@Composable\nprivate fun PInsightsScreen(",
    "@Composable\ninternal fun PInsightsScreen(",
    1,
)

# Reuse the app-shell import header for this deterministic extraction. Kotlin tolerates unused
# imports; later cleanup passes can narrow them once the shell has been fully decomposed.
header_end = text.index("private enum class PTab")
header = text[:header_end].rstrip() + "\n\n"
new_rel = "app/src/main/java/com/framebynavin/app/ui/V18CoreScreens.kt"
if (ROOT / new_rel).exists():
    raise SystemExit(f"{new_rel} already exists; refusing to overwrite")
write(new_rel, header + segment.rstrip() + "\n")

# Remove the extracted implementations from the app-shell monolith.
text = text[:start] + text[end:]

# Shared render helpers stay in the shell for now but become package-internal so the extracted
# screens can reuse one implementation instead of duplicating visual behavior.
for name in [
    "PTopBar",
    "PEmptyState",
    "PStageRail",
    "PMetric",
    "PSmallStat",
    "pActiveQueue",
    "pDate",
]:
    old = f"private fun {name}("
    new = f"internal fun {name}("
    hits = text.count(old)
    if hits != 1:
        raise SystemExit(f"Expected one helper {name}, found {hits}")
    text = text.replace(old, new, 1)

write(root_rel, text)

# Structural safety checks.
root_after = read(root_rel)
new_after = read(new_rel)
for name in ["PPlanScreen", "PStudioScreen", "PInsightsScreen"]:
    if f"fun {name}(" in root_after:
        raise SystemExit(f"{name} still exists in the app shell after extraction")
    if f"internal fun {name}(" not in new_after:
        raise SystemExit(f"{name} missing from extracted screen file")

root_lines = len(root_after.splitlines())
if root_lines > 1320:
    raise SystemExit(f"App shell is still unexpectedly large after extraction: {root_lines} lines")

print("v1.8 alpha4 architecture split applied")
print(f"FrameByNavinV101BApp.kt: {root_lines} lines")
print(f"V18CoreScreens.kt: {len(new_after.splitlines())} lines")
