from pathlib import Path

p = Path(__file__).resolve().parents[1] / "app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt"
text = p.read_text()
marker = "import androidx.compose.foundation.shape.RoundedCornerShape\n"
addition = marker + "import androidx.compose.foundation.verticalScroll\n"
if "import androidx.compose.foundation.verticalScroll\n" not in text:
    if marker not in text:
        raise SystemExit("verticalScroll import marker missing")
    text = text.replace(marker, addition, 1)
    p.write_text(text)
print("v1.3.1 compile import fixed")
