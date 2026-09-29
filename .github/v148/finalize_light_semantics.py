from __future__ import annotations

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]

PATCHES: dict[str, list[tuple[str, str]]] = {
    "app/src/main/java/com/framebynavin/app/ui/V071WorkflowInlineContent.kt": [
        (
            "import com.framebynavin.app.ui.theme.CinemaLine\nimport com.framebynavin.app.ui.theme.CinemaSurfaceRaised",
            "import com.framebynavin.app.ui.theme.CinemaLine\nimport com.framebynavin.app.ui.theme.CinemaSurface\nimport com.framebynavin.app.ui.theme.CinemaSurfaceRaised",
        ),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V20InsightsFoundationUi.kt": [
        (
            "import com.framebynavin.app.ui.theme.CinemaLine\nimport com.framebynavin.app.ui.theme.MutedGold",
            "import com.framebynavin.app.ui.theme.CinemaLine\nimport com.framebynavin.app.ui.theme.CinemaSurface\nimport com.framebynavin.app.ui.theme.MutedGold",
        ),
    ],
    "app/src/main/java/com/framebynavin/app/ui/theme/FrameByNavinTheme.kt": [
        (
            "val BacklotError: Color get() = if (VisualExperiencePrefs.isLight) Color(0xFFB33C38) else Color(0xFFE65F5A)\n",
            "val BacklotError: Color get() = if (VisualExperiencePrefs.isLight) Color(0xFFB33C38) else Color(0xFFE65F5A)\n\n// Media is content, not app chrome. Imported frames/video remain on a stable dark canvas in both appearances.\nval BacklotMediaCanvas: Color get() = Color(0xFF050505)\nval BacklotOnMedia: Color get() = Color(0xFFF4F0E8)\nval BacklotMediaBorder: Color get() = Color.White.copy(alpha = .14f)\n",
        ),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V175BestFramesUi.kt": [
        (
            "import com.framebynavin.app.ui.theme.CinemaBlack\nimport com.framebynavin.app.ui.theme.CinemaLine",
            "import com.framebynavin.app.ui.theme.BacklotMediaBorder\nimport com.framebynavin.app.ui.theme.BacklotMediaCanvas\nimport com.framebynavin.app.ui.theme.BacklotOnMedia\nimport com.framebynavin.app.ui.theme.CinemaLine",
        ),
        (
            "color = CinemaBlack,\n            border = BorderStroke(1.dp, CinemaLine.copy(alpha = .72f)),",
            "color = BacklotMediaCanvas,\n            border = BorderStroke(1.dp, BacklotMediaBorder),",
        ),
        (
            "Box(Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp)).background(CinemaBlack))",
            "Box(Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp)).background(BacklotMediaCanvas))",
        ),
        (
            "Text(greeting, color = ProjectorIvory, fontSize = 21.sp, fontWeight = FontWeight.Black)",
            "Text(greeting, color = BacklotOnMedia, fontSize = 21.sp, fontWeight = FontWeight.Black)",
        ),
        (
            "shape = CircleShape,\n                        color = Color.Black.copy(alpha = .55f),\n                    ) {\n                        Box(contentAlignment = Alignment.Center) {\n                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ProjectorIvory)",
            "shape = CircleShape,\n                        color = Color.Black.copy(alpha = .55f),\n                    ) {\n                        Box(contentAlignment = Alignment.Center) {\n                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = BacklotOnMedia)",
        ),
        (
            "tint = if (danger) RecRed else ProjectorIvory,",
            "tint = if (danger) RecRed else BacklotOnMedia,",
        ),
    ],
    "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt": [
        (
            "private fun pPickDateTime(context: Context, currentMillis: Long, onPicked: (Long) -> Unit) {\n    val initial = Calendar.getInstance().apply { timeInMillis = currentMillis.takeIf { it > System.currentTimeMillis() } ?: (System.currentTimeMillis() + 60 * 60_000L) }\n    DatePickerDialog(context, { _, year, month, day ->\n        TimePickerDialog(context, { _, hour, minute ->",
            "private fun pPickDateTime(context: Context, currentMillis: Long, onPicked: (Long) -> Unit) {\n    val initial = Calendar.getInstance().apply { timeInMillis = currentMillis.takeIf { it > System.currentTimeMillis() } ?: (System.currentTimeMillis() + 60 * 60_000L) }\n    val dialogTheme = if (VisualExperiencePrefs.isLight) android.R.style.Theme_Material_Light_Dialog_Alert else android.R.style.Theme_Material_Dialog_Alert\n    DatePickerDialog(context, dialogTheme, { _, year, month, day ->\n        TimePickerDialog(context, dialogTheme, { _, hour, minute ->",
        ),
    ],
}


def apply_pair(path: pathlib.Path, rel: str, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old in text:
        path.write_text(text.replace(old, new), encoding="utf-8")
        return
    if new in text:
        return
    raise SystemExit(f"Expected V148 semantic anchor not found in {rel}: {old[:150]!r}")


def main() -> int:
    for rel, patches in PATCHES.items():
        path = ROOT / rel
        for old, new in patches:
            apply_pair(path, rel, old, new)

    # Guard the two cross-layer regressions this pass is specifically designed to prevent.
    shell = (ROOT / "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt").read_text(encoding="utf-8")
    media = (ROOT / "app/src/main/java/com/framebynavin/app/ui/V175BestFramesUi.kt").read_text(encoding="utf-8")
    errors: list[str] = []
    if "DatePickerDialog(context, {" in shell or "TimePickerDialog(context, {" in shell:
        errors.append("Native date/time picker still inherits the fixed Activity theme instead of selected appearance.")
    if ".background(CinemaBlack)" in media or "color = CinemaBlack," in media:
        errors.append("Media hero still uses app-background semantics instead of a stable media canvas.")
    if errors:
        print("V148_SEMANTIC_GUARD_FAILED")
        for error in errors:
            print(error)
        return 1
    print("V148_SEMANTIC_GUARD_OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
