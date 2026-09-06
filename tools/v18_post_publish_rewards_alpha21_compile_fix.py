from pathlib import Path

path = Path("app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt")
text = path.read_text()

required = "import com.framebynavin.app.data.PostPublishCheckpoint\n"
anchor = "import com.framebynavin.app.data.ProjectAttentionPlan\n"

if required not in text:
    if anchor not in text:
        raise RuntimeError("Alpha21 compile-fix anchor missing in V131PolishUi.kt")
    text = text.replace(anchor, required + anchor, 1)
    path.write_text(text)

if required not in path.read_text():
    raise RuntimeError("Alpha21 PostPublishCheckpoint import was not applied")

print("Applied Alpha21 PostPublishCheckpoint UI compile compatibility fix")
