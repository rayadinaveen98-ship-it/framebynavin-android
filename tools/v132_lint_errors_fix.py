from pathlib import Path

v04 = Path("app/src/main/java/com/framebynavin/app/ui/FrameByNavinV04App.kt")
text = v04.read_text(encoding="utf-8")
text = text.replace(
    "import androidx.compose.ui.platform.LocalContext\n",
    "import androidx.activity.compose.LocalActivity\n",
    1,
)
text = text.replace(
    "    val activity = LocalContext.current as? ComponentActivity\n",
    "    val activity = LocalActivity.current as? ComponentActivity\n",
    1,
)
v04.write_text(text, encoding="utf-8")

app = Path("app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt")
text = app.read_text(encoding="utf-8")
old = '''                    AnimatedContent(targetState = selected.id, label = "todayProject") {
                        PTodayProjectCard(selected)
                    }
'''
new = '''                    AnimatedContent(targetState = selected.id, label = "todayProject") { targetId ->
                        queue.firstOrNull { it.id == targetId }?.let { targetTask ->
                            PTodayProjectCard(targetTask)
                        }
                    }
'''
if old not in text:
    raise SystemExit("AnimatedContent block not found")
text = text.replace(old, new, 1)
app.write_text(text, encoding="utf-8")

print("Fixed the two blocking Android lint errors")
