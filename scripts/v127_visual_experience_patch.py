from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def path(rel): return ROOT / rel

def replace(rel, old, new):
    target = path(rel)
    text = target.read_text()
    if old not in text:
        raise SystemExit(f'Missing expected block in {rel}: {old[:160]!r}')
    target.write_text(text.replace(old, new, 1))

# Main app: reminder deep-link route, stage completion event, reward aura, appearance settings.
main = "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
replace(
    main,
    '    var guidedTourStepName by rememberSaveable { mutableStateOf<String?>(null) }\n',
    '    var guidedTourStepName by rememberSaveable { mutableStateOf<String?>(null) }\n'
    '    var stageCompletionEvent by rememberSaveable { mutableLongStateOf(0L) }\n',
)
replace(
    main,
    '            CreatorWidgetContract.ACTION_AUTOMATION_CENTER -> overlay = POverlay.AUTOMATION\n',
    '            CreatorWidgetContract.ACTION_AUTOMATION_CENTER -> overlay = POverlay.AUTOMATION\n'
    '            CreatorWidgetContract.ACTION_OPEN_REMINDERS -> { overlay = POverlay.NONE; showReminders = true }\n',
)
replace(
    main,
    '        vm.advanceWorkflow(id)\n        routeJourney(destination)\n',
    '        vm.advanceWorkflow(id)\n        stageCompletionEvent = System.nanoTime()\n        routeJourney(destination)\n',
)
replace(
    main,
    '        vm.rewardFeedback?.let { reward ->\n',
    '        V127StageCompletionPulse(stageCompletionEvent, modifier = Modifier.align(Alignment.Center))\n\n'
    '        vm.rewardFeedback?.let { reward ->\n',
)
replace(
    main,
    '            V22RewardToast(\n                entry = reward,\n',
    '            V127RewardAura(\n'
    '                eventKey = reward.eventKey,\n'
    '                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 0.dp).size(190.dp),\n'
    '            )\n'
    '            V22RewardToast(\n                entry = reward,\n',
)
replace(
    main,
    '            Spacer(Modifier.height(22.dp))\n            PSettingsHeading("GUIDED TOUR", "Replay the creator journey whenever you want.")\n',
    '            Spacer(Modifier.height(22.dp))\n'
    '            V127AppearanceSettings()\n'
    '            Spacer(Modifier.height(22.dp))\n'
    '            PSettingsHeading("GUIDED TOUR", "Replay the creator journey whenever you want.")\n',
)

# Creator setup: the companion walks every setup page.
onboarding = "app/src/main/java/com/framebynavin/app/ui/V18CreatorOnboarding.kt"
replace(
    onboarding,
    '            Spacer(Modifier.height(20.dp))\n\n            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {\n',
    '            Spacer(Modifier.height(12.dp))\n'
    '            V127SetupGuideStrip(page)\n'
    '            Spacer(Modifier.height(14.dp))\n\n'
    '            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {\n',
)

# Cinematic welcome: denser, slower stripes, theme-aware light and original sonic ident.
welcome = "app/src/main/java/com/framebynavin/app/ui/V174CinematicWelcome.kt"
replace(welcome, 'import androidx.compose.ui.graphics.graphicsLayer\n', 'import androidx.compose.ui.graphics.graphicsLayer\nimport androidx.compose.ui.platform.LocalContext\n')
replace(welcome, 'import kotlinx.coroutines.delay\n', 'import com.framebynavin.app.ui.theme.*\nimport kotlinx.coroutines.delay\n')
replace(welcome, 'private const val V20_WELCOME_STRIPE_COUNT = 12\n', 'private const val V20_WELCOME_STRIPE_COUNT = 26\n')
replace(
    welcome,
    'internal fun V174CinematicWelcome() {\n    val ignition = remember { Animatable(0f) }\n',
    'internal fun V174CinematicWelcome() {\n    val context = LocalContext.current\n    val ignition = remember { Animatable(0f) }\n',
)
replace(welcome, '    LaunchedEffect(Unit) {\n        delay(60)\n', '    LaunchedEffect(Unit) {\n        WelcomeSonicIdent.play(context.applicationContext)\n        delay(60)\n')
replace(welcome, '        strips.animateTo(1f, tween(980, easing = FastOutSlowInEasing))\n', '        strips.animateTo(1f, tween(1350, easing = FastOutSlowInEasing))\n')
replace(
    welcome,
    '            val revealGlow = 0.05f + 0.16f * mark.value + 0.08f * impact.value - 0.035f * settle.value\n',
    '            val revealGlow = 0.08f + 0.24f * mark.value + 0.12f * impact.value - 0.035f * settle.value\n',
)
replace(
    welcome,
    '            val palette = listOf(\n                Color(0xFFF4C06B), Color(0xFFD72B29), Color(0xFF6A607B),\n                Color(0xFFE55A3C), Color(0xFFFFD39A), Color(0xFF8B3040),\n            )\n',
    '            val palette = listOf(\n                MutedGold, RecRed, ProjectorIvory.copy(alpha = .72f),\n                RecRed.copy(alpha = .78f), MutedGold.copy(alpha = .86f), RecRedDeep,\n                ProjectorIvory.copy(alpha = .48f), MutedGold.copy(alpha = .62f),\n            )\n',
)
replace(welcome, '                val stagger = index * 0.035f\n', '                val stagger = index * 0.014f\n')
replace(welcome, '                val alpha = (0.54f + (index % 4) * 0.10f) * fadeOut\n', '                val alpha = (0.66f + (index % 4) * 0.075f) * fadeOut\n')

launch_gate = "app/src/main/java/com/framebynavin/app/ui/V131LaunchGate.kt"
replace(launch_gate, '            delay(2_850L)\n', '            delay(3_150L)\n')

# Version bump.
build = "app/build.gradle.kts"
replace(build, '        versionCode = 126\n        versionName = "2.0.0-rc2-new-project-feature-parity"\n', '        versionCode = 127\n        versionName = "2.0.0-rc3-visual-experience-v2"\n')

# Existing large widget gets a stable root id for runtime theme backgrounds.
# (Safe if already applied directly before workflow.)
large_layout = path("app/src/main/res/layout/widget_creator_large.xml")
large_text = large_layout.read_text()
if 'android:id="@+id/widget_large_root"' not in large_text:
    large_text = large_text.replace('<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"\n', '<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"\n    android:id="@+id/widget_large_root"\n', 1)
    large_layout.write_text(large_text)

# Sweep common legacy palette literals only in files already importing theme aliases.
legacy = {
    'Color(0xFF070707)': 'CinemaBlack',
    'Color(0xFF101010)': 'CinemaSurface',
    'Color(0xFF151515)': 'CinemaSurfaceRaised',
    'Color(0xFF292929)': 'CinemaLine',
    'Color(0xFFF3EFE7)': 'ProjectorIvory',
    'Color(0xFF918C85)': 'MutedText',
    'Color(0xFFFF3D3D)': 'RecRed',
    'Color(0xFF311010)': 'RecRedDeep',
    'Color(0xFFD8B56B)': 'MutedGold',
    'Color(0xFF6BAF83)': 'SuccessGreen',
}
ui_root = path("app/src/main/java/com/framebynavin/app/ui")
for file in ui_root.rglob("*.kt"):
    if file.name == "V174CinematicWelcome.kt":
        continue
    text = file.read_text()
    if 'import com.framebynavin.app.ui.theme.*' not in text:
        continue
    updated = text
    for old, new in legacy.items():
        updated = updated.replace(old, new)
    if updated != text:
        file.write_text(updated)

# Revision note: compile retry after explicit launch-sound preference updater fix.
print('v127 visual experience integration patch applied')
