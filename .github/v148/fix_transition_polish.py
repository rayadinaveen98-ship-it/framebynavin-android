from __future__ import annotations

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]

PATCHES: dict[str, list[tuple[str, str]]] = {
    "app/src/main/res/values/themes.xml": [
        ("#B43B37", "#171513"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V140CinematicWelcome.kt": [
        ("private const val V140_IDENT_DURATION_MS = 5_000", "private const val V140_IDENT_DURATION_MS = 3_000"),
        ("V148 keeps the same five-second motion language in both appearances;", "V148 keeps the same three-second motion language in both appearances;"),
        ("val formation = v140Smooth(((t - 0.30f) / 0.54f).coerceIn(0f, 1f))", "val formation = v140Smooth(((t - 0.14f) / 0.58f).coerceIn(0f, 1f))"),
        ("val settle = v140Smooth(((t - 0.84f) / 0.16f).coerceIn(0f, 1f))", "val settle = v140Smooth(((t - 0.72f) / 0.28f).coerceIn(0f, 1f))"),
        ("val ignition = v140Smooth((t / 0.16f).coerceIn(0f, 1f))", "val ignition = v140Smooth((t / 0.12f).coerceIn(0f, 1f))"),
        ("val phase = (t * 1.35f + index * 0.071f) % 1f", "val phase = (t * 1.58f + index * 0.071f) % 1f"),
        ("if (t > .79f) {", "if (t > .66f) {"),
        ("val sweep = v140Smooth(((t - .79f) / .17f).coerceIn(0f, 1f))", "val sweep = v140Smooth(((t - .66f) / .24f).coerceIn(0f, 1f))"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V131LaunchGate.kt": [
        (" * Five-second cinematic studio-ident on normal cold launches.", " * Three-second cinematic studio-ident on normal cold launches."),
        ("import androidx.compose.runtime.*\n", "import androidx.compose.runtime.*\nimport androidx.compose.foundation.layout.fillMaxSize\nimport androidx.compose.material3.Surface\nimport androidx.compose.ui.Modifier\nimport com.framebynavin.app.ui.theme.BacklotBackground\n"),
        (
            "    AnimatedContent(\n        targetState = welcomeDone,\n        transitionSpec = {\n            fadeIn(tween(180)) togetherWith fadeOut(tween(140))\n        },\n        label = \"launchGate\",\n    ) { ready ->\n        if (ready) {\n            V144GuideChoiceGate {\n                FrameByNavinV101BApp(externalLaunch = externalLaunch)\n            }\n        } else {\n            V140CinematicWelcome(onFinished = { welcomeDone = true })\n        }\n    }",
            "    Surface(modifier = Modifier.fillMaxSize(), color = BacklotBackground) {\n        AnimatedContent(\n            targetState = welcomeDone,\n            transitionSpec = {\n                fadeIn(tween(140)) togetherWith fadeOut(tween(110))\n            },\n            label = \"launchGate\",\n        ) { ready ->\n            if (ready) {\n                V144GuideChoiceGate {\n                    FrameByNavinV101BApp(externalLaunch = externalLaunch)\n                }\n            } else {\n                V140CinematicWelcome(onFinished = { welcomeDone = true })\n            }\n        }\n    }",
        ),
    ],
    "app/src/main/java/com/framebynavin/app/ui/ContentWorkspaceActivity.kt": [
        ("import android.os.Bundle\n", "import android.os.Bundle\nimport android.graphics.drawable.ColorDrawable\n"),
        ("import androidx.compose.foundation.layout.*\n", "import androidx.compose.foundation.background\nimport androidx.compose.foundation.layout.*\n"),
        ("import androidx.compose.ui.graphics.Color\n", "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.toArgb\n"),
        (
            "        enableEdgeToEdge()\n        restoreModeAfterLoad =",
            "        enableEdgeToEdge()\n        VisualExperiencePrefs.initialize(applicationContext)\n        window.setBackgroundDrawable(ColorDrawable(BacklotBackground.toArgb()))\n        restoreModeAfterLoad =",
        ),
        ("                Box(Modifier.fillMaxSize()) {", "                Box(Modifier.fillMaxSize().background(BacklotBackground)) {"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/theme/FrameByNavinTheme.kt": [
        ("import android.content.ContextWrapper\n", "import android.content.ContextWrapper\nimport android.graphics.drawable.ColorDrawable\n"),
        (
            "        val window = view.context.frameActivity()?.window ?: return@SideEffect\n        window.statusBarColor = palette.background.toArgb()",
            "        val window = view.context.frameActivity()?.window ?: return@SideEffect\n        window.setBackgroundDrawable(ColorDrawable(palette.background.toArgb()))\n        window.statusBarColor = palette.background.toArgb()",
        ),
    ],
    "app/build.gradle.kts": [
        ("// Backlot V148 dark/light appearance release candidate.\n        versionCode = 148\n        versionName = \"2.0.0-rc23-light-appearance\"", "// Backlot V148 dark/light appearance + startup/transition polish candidate.\n        versionCode = 148\n        versionName = \"2.0.0-rc24-light-transition-polish\""),
    ],
}


def apply_patch(path: pathlib.Path, old: str, new: str) -> bool:
    text = path.read_text(encoding="utf-8")
    if new in text:
        return False
    if old not in text:
        raise RuntimeError(f"Expected source text not found in {path}: {old[:100]!r}")
    path.write_text(text.replace(old, new), encoding="utf-8")
    return True


def verify() -> None:
    theme_xml = (ROOT / "app/src/main/res/values/themes.xml").read_text(encoding="utf-8")
    welcome = (ROOT / "app/src/main/java/com/framebynavin/app/ui/V140CinematicWelcome.kt").read_text(encoding="utf-8")
    gate = (ROOT / "app/src/main/java/com/framebynavin/app/ui/V131LaunchGate.kt").read_text(encoding="utf-8")
    workspace = (ROOT / "app/src/main/java/com/framebynavin/app/ui/ContentWorkspaceActivity.kt").read_text(encoding="utf-8")
    theme = (ROOT / "app/src/main/java/com/framebynavin/app/ui/theme/FrameByNavinTheme.kt").read_text(encoding="utf-8")
    gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")

    checks = {
        "red native window removed": "#B43B37" not in theme_xml,
        "neutral native window present": "#171513" in theme_xml,
        "welcome is three seconds": "V140_IDENT_DURATION_MS = 3_000" in welcome,
        "launch gate has opaque themed root": "Surface(modifier = Modifier.fillMaxSize(), color = BacklotBackground)" in gate,
        "workspace root is opaque": "fillMaxSize().background(BacklotBackground)" in workspace,
        "workspace native window follows appearance": "window.setBackgroundDrawable(ColorDrawable(BacklotBackground.toArgb()))" in workspace,
        "all Compose activities synchronize window background": "window.setBackgroundDrawable(ColorDrawable(palette.background.toArgb()))" in theme,
        "candidate version bumped": "2.0.0-rc24-light-transition-polish" in gradle,
    }
    failed = [name for name, ok in checks.items() if not ok]
    for name, ok in checks.items():
        print(f"{'PASS' if ok else 'FAIL'}: {name}")
    if failed:
        raise RuntimeError("Transition polish verification failed: " + ", ".join(failed))


def main() -> int:
    changed: list[str] = []
    for relative, replacements in PATCHES.items():
        path = ROOT / relative
        for old, new in replacements:
            if apply_patch(path, old, new):
                changed.append(relative)
    verify()
    if changed:
        print("Patched:")
        for item in sorted(set(changed)):
            print(f" - {item}")
    else:
        print("Transition polish already applied; verification passed.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        raise
