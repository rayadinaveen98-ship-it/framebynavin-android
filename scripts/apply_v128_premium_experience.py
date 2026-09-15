from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def p(rel): return ROOT / rel

def replace_once(rel, old, new, required=True):
    path = p(rel)
    text = path.read_text()
    if old not in text:
        if required:
            raise SystemExit(f"Missing expected block in {rel}: {old[:160]!r}")
        return
    path.write_text(text.replace(old, new, 1))

# --- Launch ident -----------------------------------------------------------
welcome = "app/src/main/java/com/framebynavin/app/ui/V174CinematicWelcome.kt"
replace_once(welcome, "private const val V20_WELCOME_STRIPE_COUNT = 26", "private const val V20_WELCOME_STRIPE_COUNT = 30")
replace_once(welcome, "strips.animateTo(1f, tween(1350, easing = FastOutSlowInEasing))", "strips.animateTo(1f, tween(2350, easing = FastOutSlowInEasing))")

# Remove the old static center divider completely. The center is now just another moving thread.
path = p(welcome)
text = path.read_text()
start = text.find("        Canvas(Modifier.fillMaxSize()) {\n            val centerX = size.width / 2f")
if start >= 0:
    end = text.find("\n        Canvas(Modifier.fillMaxSize()) {\n            if (strips.value <= 0f)", start)
    if end < 0:
        raise SystemExit("Could not locate end of static center divider block")
    text = text[:start] + text[end:]
path.write_text(text)

replace_once(
    welcome,
    "                val x = startX + (endX - startX) * local\n",
    "                val eased = local * local * (3f - 2f * local)\n"
    "                val drift = kotlin.math.sin((eased * 3.1415926f) + index * .47f) * size.width * .012f\n"
    "                val x = startX + (endX - startX) * eased + drift\n",
)
replace_once(welcome, "            fontSize = 24.sp,\n            lineHeight = 29.sp,", "            fontSize = 27.6.sp,\n            lineHeight = 33.sp,")
replace_once(welcome, "            color = Color(0xFFF7F1E8),", "            color = ProjectorIvory,")
replace_once(welcome, "                .offset(y = 82.dp)", "                .offset(y = 84.dp)")

# --- Setup copy reduction ---------------------------------------------------
onboarding = "app/src/main/java/com/framebynavin/app/ui/V18CreatorOnboarding.kt"
replace_once(
    onboarding,
    "                        Spacer(Modifier.height(7.dp))\n                        Text(\"Your primary mode sets smart defaults. Secondary modes keep FrameByNavin flexible when your work crosses niches.\", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)\n                        if (primaryMode.isNotBlank()) {\n                            Spacer(Modifier.height(7.dp))\n                            Text(selectedModeDefinition.description, color = MutedGold.copy(alpha = .82f), fontSize = 10.5.sp, lineHeight = 15.sp)\n                        }\n",
    "",
)
replace_once(
    onboarding,
    "                            Spacer(Modifier.height(5.dp))\n                            Text(\"Choose up to ${CreatorProfile.MAX_SECONDARY_MODES}. These influence suggestions but never restrict what you can make.\", color = MutedText, fontSize = 10.5.sp, lineHeight = 15.sp)\n",
    "                            Spacer(Modifier.height(5.dp))\n                            Text(\"Add up to ${CreatorProfile.MAX_SECONDARY_MODES}.\", color = MutedText, fontSize = 10.sp)\n",
)
replace_once(
    onboarding,
    "                        Spacer(Modifier.height(7.dp))\n                        Text(\"Choose every platform you actively create for. Platforms define delivery formats; your creator mode will define what kind of content is suggested.\", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)\n",
    "",
)
replace_once(
    onboarding,
    "                        Spacer(Modifier.height(7.dp))\n                        Text(\"This describes production, not your niche. Suggestions for ${selectedModeDefinition.label} appear first, but every production style stays available.\", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)\n",
    "                        Spacer(Modifier.height(5.dp))\n                        Text(\"Recommended styles for ${selectedModeDefinition.label} appear first.\", color = MutedText, fontSize = 10.sp)\n",
)
replace_once(
    onboarding,
    "                        Spacer(Modifier.height(7.dp))\n                        Text(\"Choose up to 3 goals. One primary goal breaks soft trade-offs; secondary goals still influence planning and review.\", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)\n",
    "                        Spacer(Modifier.height(5.dp))\n                        Text(\"Choose up to 3.\", color = MutedText, fontSize = 10.sp)\n",
)
replace_once(onboarding, "V18OnboardingLabel(\"REALISTIC PUBLISHING TARGET\")", "V18OnboardingLabel(\"WEEKLY TARGET\")")
replace_once(
    onboarding,
    "                        Text(\"How many pieces would you like to publish in a normal week?\", color = MutedText, fontSize = 11.sp)\n",
    "                        Text(\"Pieces per week\", color = MutedText, fontSize = 10.sp)\n",
)
replace_once(
    onboarding,
    "                        Spacer(Modifier.height(7.dp))\n                        Text(\"These Android permissions are optional system capabilities. They are kept separate from your creator identity and goals.\", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)\n",
    "",
)
# Give compact screens extra breathing room above the fixed CTA.
replace_once(onboarding, "                Spacer(Modifier.height(18.dp))\n            }\n\n            Row(Modifier.fillMaxWidth()", "                Spacer(Modifier.height(32.dp))\n            }\n\n            Row(Modifier.fillMaxWidth()")

# --- New Project content type progressive disclosure -----------------------
wizard = "app/src/main/java/com/framebynavin/app/ui/V20NewProjectWizard.kt"
replace_once(
    wizard,
    "    val typeOptions = if (showAllTypes) ContentArchetypeRegistry.definitions else recommendedTypes\n",
    "    val typeOptions = recommendedTypes.take(4)\n",
)
replace_once(
    wizard,
    "                                Text(if (showAllTypes) \"RECOMMENDED ONLY\" else \"SEE ALL TYPES\", fontSize = 8.sp, fontWeight = FontWeight.Black)\n",
    "                                Text(\"BROWSE ALL TYPES\", fontSize = 8.sp, fontWeight = FontWeight.Black)\n",
)
replace_once(
    wizard,
    "                            TextButton(onClick = { showAllTypes = !showAllTypes }) {\n",
    "                            TextButton(onClick = { showAllTypes = true }) {\n",
)

picker = r'''
    if (showAllTypes) {
        Dialog(
            onDismissRequest = { showAllTypes = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
                Column(
                    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("CONTENT TYPES", color = RecRed, fontSize = 8.4.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text("Choose the right format", color = ProjectorIvory, fontSize = 21.sp, fontWeight = FontWeight.Black)
                            Text(CreatorModeRegistry.definition(creatorModeId).label, color = MutedGold, fontSize = 9.sp)
                        }
                        IconButton(onClick = { showAllTypes = false }) {
                            Icon(Icons.Outlined.Close, "Close content types", tint = ProjectorIvory)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        V20FieldLabel("RECOMMENDED")
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recommendedTypes.forEach { archetype ->
                                val label = runCatching { ContentArchetypeRegistry.labelForMode(archetype.id, creatorModeId) }.getOrDefault(archetype.label)
                                FilterChip(
                                    selected = archetypeId == archetype.id,
                                    onClick = { onArchetypeChange(archetype.id); showAllTypes = false },
                                    leadingIcon = if (archetypeId == archetype.id) { { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(15.dp)) } } else null,
                                    label = { Text(label, fontSize = 9.sp) },
                                )
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                        V20FieldLabel("ALL TYPES")
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ContentArchetypeRegistry.definitions.filter { option -> recommendedTypes.none { it.id == option.id } }.forEach { archetype ->
                                val label = runCatching { ContentArchetypeRegistry.labelForMode(archetype.id, creatorModeId) }.getOrDefault(archetype.label)
                                FilterChip(
                                    selected = archetypeId == archetype.id,
                                    onClick = { onArchetypeChange(archetype.id); showAllTypes = false },
                                    leadingIcon = if (archetypeId == archetype.id) { { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(15.dp)) } } else null,
                                    label = { Text(label, fontSize = 9.sp) },
                                )
                            }
                        }
                        Spacer(Modifier.height(30.dp))
                    }
                }
            }
        }
    }

'''
replace_once(wizard, "    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {\n", picker + "    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {\n")

# --- Frame Pulse orb V2 -----------------------------------------------------
voice = "app/src/main/java/com/framebynavin/app/ui/V117VoiceQuickIdeaUi.kt"
path = p(voice)
text = path.read_text()
start = text.find("@Composable\nprivate fun FramePulseOrb")
end = text.find("\nprivate fun listeningSubtitle", start)
if start < 0 or end < 0:
    raise SystemExit("Could not locate FramePulseOrb block")
new_orb = r'''@Composable
private fun FramePulseOrb(rmsDb: Float, onStop: () -> Unit, modifier: Modifier = Modifier) {
    val raw = ((rmsDb + 2f) / 12f).coerceIn(0f, 1f)
    val amplitude by animateFloatAsState(raw, spring(dampingRatio = .72f, stiffness = 155f), label = "voiceAmplitudeV2")
    val infinite = rememberInfiniteTransition(label = "voiceOrbV2")
    val spin by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(6200, easing = LinearEasing)), label = "orbSpinV2")
    val counterSpin by infinite.animateFloat(360f, 0f, infiniteRepeatable(tween(8800, easing = LinearEasing)), label = "orbCounterSpin")
    val breathe by infinite.animateFloat(.96f, 1.045f, infiniteRepeatable(tween(1450, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "orbBreatheV2")

    Canvas(modifier.clickable(onClick = onStop)) {
        val c = center
        val base = size.minDimension * (.20f + amplitude * .040f)
        val halo = base * (2.35f + amplitude * .60f)

        // Atmospheric bloom.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    RecRed.copy(alpha = .34f + amplitude * .18f),
                    FrameTertiary.copy(alpha = .18f + amplitude * .12f),
                    MutedGold.copy(alpha = .10f),
                    Color.Transparent,
                ),
                center = c,
                radius = halo,
            ),
            radius = halo,
            center = c,
        )

        // Fluid translucent shells.
        drawCircle(FrameTertiary.copy(alpha = .075f + amplitude * .07f), radius = base * 1.78f * breathe, center = c)
        drawCircle(RecRed.copy(alpha = .10f + amplitude * .10f), radius = base * 1.53f, center = c)
        drawCircle(MutedGold.copy(alpha = .07f + amplitude * .06f), radius = base * 1.30f * (2f - breathe), center = c)

        // Dark optical cavity + luminous inner field.
        drawCircle(CinemaBlack.copy(alpha = .90f), radius = base * 1.03f, center = c)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    ProjectorIvory.copy(alpha = .55f + amplitude * .18f),
                    FrameTertiary.copy(alpha = .58f),
                    RecRed.copy(alpha = .82f),
                    RecRedDeep.copy(alpha = .98f),
                ),
                center = Offset(c.x - base * .27f, c.y - base * .31f),
                radius = base * 1.55f,
            ),
            radius = base * .86f,
            center = c,
        )

        // Thin premium light filaments.
        val ringRadius = base * 1.36f
        repeat(5) { index ->
            val r = ringRadius + index * 5.5f
            val direction = if (index % 2 == 0) spin else counterSpin
            val tint = when (index % 3) {
                0 -> MutedGold
                1 -> FrameTertiary
                else -> RecRed
            }
            drawArc(
                color = tint.copy(alpha = .45f + amplitude * .28f),
                startAngle = direction + index * 71f,
                sweepAngle = 24f + index * 7f + amplitude * 28f,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                style = Stroke(width = 1.1f + amplitude * 1.4f),
            )
        }

        // Small floating glints give depth without particle noise.
        repeat(4) { i ->
            val angle = (spin + i * 90f) * 0.017453292f
            val r = base * (1.12f + i * .10f)
            val point = Offset(c.x + kotlin.math.cos(angle) * r, c.y + kotlin.math.sin(angle) * r)
            drawCircle(ProjectorIvory.copy(alpha = .38f + amplitude * .30f), radius = 1.2f + amplitude * 1.4f, center = point)
        }
        drawCircle(ProjectorIvory.copy(alpha = .90f), radius = 1.8f + amplitude * 1.8f, center = c)
    }
}
'''
path.write_text(text[:start] + new_orb + text[end:])

# --- Version ---------------------------------------------------------------
build = "app/build.gradle.kts"
replace_once(build, '        versionCode = 127\n        versionName = "2.0.0-rc3-visual-experience-v2"', '        versionCode = 128\n        versionName = "2.0.0-rc4-premium-experience-v3"')

print("v128 premium experience patch applied")
