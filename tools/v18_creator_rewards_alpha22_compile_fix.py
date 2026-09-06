from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
path = ROOT / "app/src/main/java/com/framebynavin/app/ui/V18TodayScreen.kt"
text = path.read_text()
old = "    rewardLedger: List<CreatorRewardLedgerEntry>,\n"
new = "    rewardLedger: List<CreatorRewardLedgerEntry> = emptyList(),\n"
if old not in text:
    if new in text:
        print("Alpha22 Today compatibility fix already applied")
        raise SystemExit(0)
    raise SystemExit("Alpha22 Today compatibility target not found")
text = text.replace(old, new, 1)
path.write_text(text)
print("Applied Alpha22 Today instrumentation compatibility fix")
