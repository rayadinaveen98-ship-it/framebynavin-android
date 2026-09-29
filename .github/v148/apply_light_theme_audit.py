from __future__ import annotations

import argparse
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]

# Exact, reviewable and idempotent cross-file migrations. If neither the old nor the new form is
# present, the workflow stops instead of silently editing an unexpected source revision.
PATCHES: dict[str, list[tuple[str, str]]] = {
    "app/src/main/java/com/framebynavin/app/ui/theme/FrameByNavinTheme.kt": [
        (
            "val BacklotSecondaryAccent: Color get() = VisualExperiencePrefs.palette.secondary\nval BacklotSuccess: Color get() = VisualExperiencePrefs.palette.success",
            "val BacklotSecondaryAccent: Color get() = VisualExperiencePrefs.palette.secondary\nval BacklotOnSecondaryAccent: Color get() = if (VisualExperiencePrefs.isLight) Color.White else Color(0xFF171310)\nval BacklotSuccess: Color get() = VisualExperiencePrefs.palette.success",
        ),
    ],
    "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt": [
        (
            "Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(17.dp), Color(0xFF15110F), border = BorderStroke(1.dp, Color(0xFF3B2521)))",
            "Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(17.dp), BacklotSurfaceSelected, border = BorderStroke(1.dp, RecRed.copy(alpha = .24f)))",
        ),
        ("if (autoPlanEnabled) Color(0xFF17130F) else CinemaSurface", "if (autoPlanEnabled) BacklotSurfaceSelected else CinemaSurface"),
        ("if (VisualExperiencePrefs.isLumen) CinemaSurface.copy(alpha = .88f) else Color(0xF2161618)", "BacklotNavigationSurface.copy(alpha = .96f)"),
        ("done || i < current -> SuccessGreen.copy(alpha = .75f); i == current -> RecRed; else -> Color(0xFF34312E)", "done || i < current -> SuccessGreen.copy(alpha = .75f); i == current -> RecRed; else -> CinemaLine"),
        ("background(if (i == index) RecRed else Color(0xFF44413D), RoundedCornerShape(100.dp))", "background(if (i == index) RecRed else MutedText.copy(alpha = .42f), RoundedCornerShape(100.dp))"),
        ("Box(Modifier.size(34.dp).background(Color(0xFF1F1F21), RoundedCornerShape(11.dp))", "Box(Modifier.size(34.dp).background(CinemaSurfaceRaised, RoundedCornerShape(11.dp))"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/ContentWorkspaceActivity.kt": [
        ("import com.framebynavin.app.ui.theme.FrameByNavinTheme", "import com.framebynavin.app.ui.theme.*"),
        ("color = Color(0xFF101010)", "color = CinemaBlack"),
        ("color = Color.White", "color = ProjectorIvory"),
        ("color = Color.LightGray", "color = MutedText"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V071WorkflowInlineContent.kt": [
        ("trackColor = Color(0xFF303030)", "trackColor = CinemaLine"),
        ("else -> Color(0xFF141414)", "else -> CinemaSurface"),
        ("else -> Color(0xFF5C5852)", "else -> MutedText.copy(alpha = .55f)"),
        ("color = if (upcoming) Color(0xFF77726C) else ProjectorIvory", "color = if (upcoming) MutedText else ProjectorIvory"),
        ("else -> Color(0xFF5F5B56)", "else -> MutedText.copy(alpha = .72f)"),
        ("color = Color(0xFF10100F)", "color = CinemaSurface"),
        ("containerColor = Color(0xFF272727)", "containerColor = CinemaSurfaceRaised"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V08WeeklyScheduleUi.kt": [
        ("Color(0xFF15120F),\n                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A2B22))", "MutedGold.copy(alpha = .06f),\n                                border = androidx.compose.foundation.BorderStroke(1.dp, MutedGold.copy(alpha = .22f))"),
        ("Surface(modifier, RoundedCornerShape(14.dp), Color(0xFF111111), border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine))", "Surface(modifier, RoundedCornerShape(14.dp), CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine))"),
        ("color = if (slot.enabled) CinemaSurfaceRaised else Color(0xFF0E0E0E)", "color = if (slot.enabled) CinemaSurfaceRaised else CinemaSurface"),
        ("border = androidx.compose.foundation.BorderStroke(1.dp, if (slot.enabled) CinemaLine else Color(0xFF181818))", "border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine.copy(alpha = if (slot.enabled) 1f else .65f))"),
        ("background(if (slot.enabled) RecRed else Color(0xFF494641), CircleShape)", "background(if (slot.enabled) RecRed else MutedText.copy(alpha = .55f), CircleShape)"),
        ("color = if (slot.enabled) ProjectorIvory else Color(0xFF77726C)", "color = if (slot.enabled) ProjectorIvory else MutedText"),
        ("Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFF111111), border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine))", "Surface(shape = RoundedCornerShape(100.dp), color = CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine))"),
        ("Text(text, color = Color(0xFF97918A)", "Text(text, color = MutedText"),
        ("Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color(0xFF10100F), border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine))", "Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine))"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt": [
        ("color = Color(0xFF1A1710)", "color = MutedGold.copy(alpha = .08f)"),
        ("color = if (isOpportunity) Color(0xFF1A1712) else CinemaSurfaceRaised", "color = if (isOpportunity) MutedGold.copy(alpha = .07f) else CinemaSurfaceRaised"),
        ("Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFF14110D), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF342C21)))", "Surface(shape = RoundedCornerShape(100.dp), color = MutedGold.copy(alpha = .08f), border = androidx.compose.foundation.BorderStroke(1.dp, MutedGold.copy(alpha = .22f)))"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V09ReleaseDayUi.kt": [
        ("color = if (selected) Color(0xFF19120F) else CinemaSurface", "color = if (selected) RecRed.copy(alpha = .07f) else CinemaSurface"),
        ("color = if (deepDive) Color(0xFF15130E) else CinemaSurface", "color = if (deepDive) MutedGold.copy(alpha = .07f) else CinemaSurface"),
        ("Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), Color(0xFF101812), border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = .38f)))", "Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), SuccessGreen.copy(alpha = .08f), border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = .38f)))"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt": [
        ("color = Color(0xFF17130F), border = BorderStroke(1.dp, MutedGold.copy(alpha = .35f))", "color = MutedGold.copy(alpha = .07f), border = BorderStroke(1.dp, MutedGold.copy(alpha = .35f))"),
        ("Surface(color = Color(0xF20B0B0C), tonalElevation = 8.dp)", "Surface(color = BacklotNavigationSurface.copy(alpha = .96f), tonalElevation = 8.dp)"),
        ("color = if (valid) Color(0xFF101812) else Color(0xFF1A1110)", "color = if (valid) SuccessGreen.copy(alpha = .08f) else RecRed.copy(alpha = .07f)"),
        ("color = if (selected) Color(0xFF19130F) else CinemaSurface", "color = if (selected) RecRed.copy(alpha = .07f) else CinemaSurface"),
        ("tint = if (!enabled) Color(0xFF56524E) else if (selected) RecRed else MutedGold", "tint = if (!enabled) MutedText.copy(alpha = .50f) else if (selected) RecRed else MutedGold"),
        ("color = if (enabled) ProjectorIvory else Color(0xFF6D6964)", "color = if (enabled) ProjectorIvory else MutedText.copy(alpha = .65f)"),
        ("color = if (enabled) MutedText else Color(0xFF56524E)", "color = if (enabled) MutedText else MutedText.copy(alpha = .50f)"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt": [
        ("RoundedCornerShape(19.dp),\n        Color(0xFF171310),", "RoundedCornerShape(19.dp),\n        MutedGold.copy(alpha = .07f),"),
        ("Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color(0xFF17110F), border = BorderStroke(1.dp, RecRed.copy(alpha = .35f)))", "Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), RecRed.copy(alpha = .07f), border = BorderStroke(1.dp, RecRed.copy(alpha = .35f)))"),
        ("Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), Color(0xFF15130F), border = BorderStroke(1.dp, MutedGold.copy(alpha = .35f)))", "Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), MutedGold.copy(alpha = .06f), border = BorderStroke(1.dp, MutedGold.copy(alpha = .35f)))"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt": [
        ("listOf(RecRed.copy(alpha = .13f), Color(0xFF0B0B0D), CinemaBlack)", "listOf(RecRed.copy(alpha = .13f), CinemaSurface, CinemaBlack)"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V133CreatorHome.kt": [
        ("Modifier.size(36.dp).background(Color(0xFF1C1714), RoundedCornerShape(11.dp))", "Modifier.size(36.dp).background(MutedGold.copy(alpha = .08f), RoundedCornerShape(11.dp))"),
        ("Modifier.size(34.dp).background(Color(0xFF1B1714), CircleShape)", "Modifier.size(34.dp).background(MutedGold.copy(alpha = .08f), CircleShape)"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V137GuideSpotlight.kt": [
        ("import com.framebynavin.app.ui.theme.RecRed", "import com.framebynavin.app.ui.theme.BacklotScrim\nimport com.framebynavin.app.ui.theme.RecRed"),
        ("drawRect(Color.Black.copy(alpha = maxOf(profile.guideScrimAlpha, .58f)))", "drawRect(BacklotScrim.copy(alpha = maxOf(profile.guideScrimAlpha, if (VisualExperiencePrefs.isLight) .48f else .68f)))"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V144GuideSystem.kt": [
        ("private val V144GuideStage = Color(0xFF15191F)", "private val V144GuideStage: Color get() = CinemaSurfaceRaised"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V144YouTubeRevenueUi.kt": [
        ("import com.framebynavin.app.ui.theme.CinemaSurface\n", "import com.framebynavin.app.ui.theme.CinemaSurface\nimport com.framebynavin.app.ui.theme.BacklotOnSecondaryAccent\n"),
        ("RoundedCornerShape(22.dp),\n        Color(0xFF171310),", "RoundedCornerShape(22.dp),\n        MutedGold.copy(alpha = .06f),"),
        ("Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color(0xFF151517), border = BorderStroke(1.dp, CinemaLine))", "Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine))"),
        ("contentColor = Color(0xFF171310)", "contentColor = BacklotOnSecondaryAccent"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V16CreatorIntelligenceUi.kt": [
        ("color = androidx.compose.ui.graphics.Color(0xFF171717)", "color = CinemaSurface"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V172InsightsUi.kt": [
        ("RoundedCornerShape(18.dp),\n        Color(0xFF151517),", "RoundedCornerShape(18.dp),\n        CinemaSurface,"),
        ("color = if (active) Color(0xFF2A2323) else Color.Transparent", "color = if (active) BacklotSurfaceSelected else Color.Transparent"),
        ("private val BrushCard = Color(0xFF171413)", "private val BrushCard: Color get() = MutedGold.copy(alpha = .06f)"),
        ("Surface(modifier.clickable(onClick = onClick), RoundedCornerShape(15.dp), Color(0xFF202020))", "Surface(modifier.clickable(onClick = onClick), RoundedCornerShape(15.dp), CinemaSurfaceRaised)"),
        ("Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color(0xFF1C1C1E))", "Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurfaceRaised)"),
        ("Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color(0xFF171719), border = BorderStroke(1.dp, MutedGold.copy(alpha = .25f)))", "Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), MutedGold.copy(alpha = .06f), border = BorderStroke(1.dp, MutedGold.copy(alpha = .25f)))"),
        ("Surface(modifier, RoundedCornerShape(12.dp), Color(0xFF202022))", "Surface(modifier, RoundedCornerShape(12.dp), CinemaSurfaceRaised)"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V18CoreScreens.kt": [
        ("else if (expanded) Color(0xFF16130F) else CinemaSurface", "else if (expanded) MutedGold.copy(alpha = .06f) else CinemaSurface"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V18TodayScreen.kt": [
        ("shape = RoundedCornerShape(16.dp),\n        color = Color(0xFF171310),", "shape = RoundedCornerShape(16.dp),\n        color = MutedGold.copy(alpha = .06f),"),
        ("Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF171310), border = BorderStroke(1.dp, MutedGold.copy(alpha = .25f)))", "Surface(shape = RoundedCornerShape(18.dp), color = MutedGold.copy(alpha = .07f), border = BorderStroke(1.dp, MutedGold.copy(alpha = .25f)))"),
        ("color = if (overdue) RecRed.copy(alpha = .14f) else Color(0xFF171410)", "color = if (overdue) RecRed.copy(alpha = .14f) else CinemaSurfaceRaised"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V19ContentWorkspaceUi.kt": [
        ("Surface(color = Color(0xF20B0B0C), tonalElevation = 8.dp)", "Surface(color = BacklotNavigationSurface.copy(alpha = .96f), tonalElevation = 8.dp)"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V20CreativeIntelligenceUi.kt": [
        ("RoundedCornerShape(20.dp),\n        Color(0xFF171617),", "RoundedCornerShape(20.dp),\n        MutedGold.copy(alpha = .05f),"),
        ("RoundedCornerShape(15.dp),\n        Color(0xFF202022),", "RoundedCornerShape(15.dp),\n        CinemaSurfaceRaised,"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V20CreatorDrilldownUi.kt": [
        ("Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), Color(0xFF1E1E20), border = BorderStroke(1.dp, accent.copy(alpha = .22f)))", "Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, accent.copy(alpha = .22f)))"),
        ("Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), Color(0xFF1D1D1F))", "Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), CinemaSurfaceRaised)"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V20GeminiIntelligenceUi.kt": [
        ("RoundedCornerShape(16.dp),\n        Color(0xFF171719),", "RoundedCornerShape(16.dp),\n        CinemaSurface,"),
        ("Surface(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp), Color(0xFF202022))", "Surface(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp), CinemaSurfaceRaised)"),
        ("Surface(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp), Color(0xFF2A1718))", "Surface(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp), RecRed.copy(alpha = .08f))"),
        ("RoundedCornerShape(12.dp),\n        Color(0xFF121A17),", "RoundedCornerShape(12.dp),\n        SuccessGreen.copy(alpha = .08f),"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V20GuidedFirstRun.kt": [
        ("drawRect(CinemaBlack.copy(alpha = .89f))", "drawRect(BacklotScrim.copy(alpha = if (VisualExperiencePrefs.isLight) .48f else .72f))"),
        ("color = ProjectorIvory,\n        border = BorderStroke(1.dp, Color.Black.copy(alpha = .08f))", "color = CinemaSurfaceRaised,\n        border = BorderStroke(1.dp, CinemaLine)"),
        ("color = CinemaBlack,\n            fontSize = 10.sp", "color = ProjectorIvory,\n            fontSize = 10.sp"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V20InsightsDrilldownUi.kt": [
        ("Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), Color(0xFF131517), border = BorderStroke(1.dp, CinemaLine))", "Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine))"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V20InsightsFoundationUi.kt": [
        ("color = Color(0xFF141619)", "color = CinemaSurface"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V20NewProjectWizard.kt": [
        ("color = Color(0xFF17130F),\n                                border = BorderStroke(1.dp, MutedGold.copy(alpha = .35f))", "color = MutedGold.copy(alpha = .07f),\n                                border = BorderStroke(1.dp, MutedGold.copy(alpha = .35f))"),
        ("color = Color(0xFF111113),\n                    border = BorderStroke(1.dp, RecRed.copy(alpha = .38f))", "color = CinemaSurfaceRaised,\n                    border = BorderStroke(1.dp, RecRed.copy(alpha = .38f))"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V20OpportunityEngineUi.kt": [
        ("color = Color(0xFF171412)", "color = MutedGold.copy(alpha = .06f)"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V20VideoPostmortemUi.kt": [
        ("RoundedCornerShape(16.dp),\n        Color(0xFF171719),", "RoundedCornerShape(16.dp),\n        CinemaSurface,"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/V20WorkflowIntelligenceUi.kt": [
        ("RoundedCornerShape(20.dp),\n        Color(0xFF151618),", "RoundedCornerShape(20.dp),\n        CinemaSurface,"),
        ("Surface(modifier, RoundedCornerShape(13.dp), Color(0xFF202124))", "Surface(modifier, RoundedCornerShape(13.dp), CinemaSurfaceRaised)"),
    ],
    "app/src/main/java/com/framebynavin/app/ui/BackupActivity.kt": [
        ("if (isError) Color(0xFF1A1110) else Color(0xFF101812)", "if (isError) RecRed.copy(alpha = .07f) else SuccessGreen.copy(alpha = .08f)"),
    ],
    "app/build.gradle.kts": [
        (
            "// Backlot V147 idea reminders release candidate.\n        versionCode = 147\n        versionName = \"2.0.0-rc22-device-stability\"",
            "// Backlot V148 dark/light appearance release candidate.\n        versionCode = 148\n        versionName = \"2.0.0-rc23-light-appearance\"",
        ),
    ],
}

RETIRED_WIDGET_BACKGROUNDS = [
    "app/src/main/res/drawable/widget_bg_studio_gold.xml",
    "app/src/main/res/drawable/widget_bg_midnight.xml",
    "app/src/main/res/drawable/widget_bg_lumen.xml",
    "app/src/main/res/drawable/widget_bg_paper.xml",
    "app/src/main/res/drawable/widget_bg_moss.xml",
    "app/src/main/res/drawable/widget_bg_ember.xml",
    "app/src/main/res/drawable/widget_bg_violet.xml",
    "app/src/main/res/drawable/widget_bg_aurora.xml",
    "app/src/main/res/drawable/widget_bg_storyboard.xml",
]

# Explicit non-UI-surface exceptions: self-contained artwork and media overlays must retain authored
# contrast independently from app appearance. The theme system itself is also the source of tokens.
AUDIT_EXCLUSIONS = {
    "app/src/main/java/com/framebynavin/app/ui/theme/FrameByNavinTheme.kt",
    "app/src/main/java/com/framebynavin/app/ui/V133BacklotAppIcon.kt",
    "app/src/main/java/com/framebynavin/app/ui/V133BrandWelcome.kt",
    "app/src/main/java/com/framebynavin/app/ui/V140CinematicWelcome.kt",
    "app/src/main/java/com/framebynavin/app/ui/V174CinematicWelcome.kt",
    "app/src/main/java/com/framebynavin/app/ui/V175BestFramesUi.kt",
    "app/src/main/java/com/framebynavin/app/ui/WelcomeSonicIdent.kt",
    "app/src/main/java/com/framebynavin/app/ui/V137CharacterSystem.kt",
    "app/src/main/java/com/framebynavin/app/ui/V144RasterGuideCharacter.kt",
}

# After V148, dark appearance literals outside the semantic theme/artwork/media layers are a bug.
DARK_LITERAL = re.compile(r"(?:androidx\.compose\.ui\.graphics\.)?Color\((?:0x(?:FF|F[0-9A-Fa-f])[0-7][0-9A-Fa-f]{5})\)|Color\.Black")


def apply_pair(path: pathlib.Path, rel: str, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old in text:
        path.write_text(text.replace(old, new), encoding="utf-8")
        return
    if new in text:
        return
    raise SystemExit(f"Expected V148 patch anchor not found in {rel}: {old[:140]!r}")


def apply_replacements() -> None:
    for rel, replacements in PATCHES.items():
        path = ROOT / rel
        for old, new in replacements:
            apply_pair(path, rel, old, new)

    for rel in RETIRED_WIDGET_BACKGROUNDS:
        path = ROOT / rel
        if path.exists():
            path.unlink()


def audit() -> list[str]:
    findings: list[str] = []
    roots = [
        ROOT / "app/src/main/java/com/framebynavin/app/ui",
        ROOT / "app/src/main/java/com/framebynavin/app/widget",
    ]
    for source_root in roots:
        for path in sorted(source_root.rglob("*.kt")):
            rel = path.relative_to(ROOT).as_posix()
            if rel in AUDIT_EXCLUSIONS:
                continue
            for line_no, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
                if DARK_LITERAL.search(line):
                    findings.append(f"{rel}:{line_no}: {line.strip()}")
    return findings


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()

    if args.apply:
        apply_replacements()

    findings = audit()
    print("V148_THEME_AUDIT_BEGIN")
    if findings:
        for item in findings:
            print(item)
    else:
        print("No dark-only UI literals found outside the explicit theme/artwork/media whitelist.")
    print("V148_THEME_AUDIT_END")
    return 1 if args.strict and findings else 0


if __name__ == "__main__":
    sys.exit(main())
