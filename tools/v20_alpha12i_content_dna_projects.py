from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"Expected anchor missing in {path}: {old[:120]!r}")
    if text.count(old) != 1:
        raise SystemExit(f"Anchor not unique in {path}: {old[:120]!r} count={text.count(old)}")
    p.write_text(text.replace(old, new, 1))


def append_before(path: str, anchor: str, addition: str) -> None:
    replace_once(path, anchor, addition + anchor)


# Version
replace_once(
    "app/build.gradle.kts",
    'versionCode = 95\n        versionName = "2.0.0-alpha1.2h-content-dna"',
    'versionCode = 96\n        versionName = "2.0.0-alpha1.2i-content-dna-projects"',
)

# Persist explicit project DNA on CreatorTask. Old tasks decode with the empty default.
replace_once(
    "app/src/main/java/com/framebynavin/app/data/CreatorTask.kt",
    '    val acknowledgedCheckpointDueAtMillis: Long = 0L,\n    val workspace: CreatorContentWorkspace = CreatorContentWorkspace(),',
    '    val acknowledgedCheckpointDueAtMillis: Long = 0L,\n    val contentDna: CreatorContentDna = CreatorContentDna(),\n    val workspace: CreatorContentWorkspace = CreatorContentWorkspace(),',
)

# Content DNA effective resolver: explicit project choices win dimension-by-dimension; missing pieces
# continue to fall back to the non-destructive legacy/profile resolver.
append_before(
    "app/src/main/java/com/framebynavin/app/data/CreatorContentDna.kt",
    "    fun forNewProject(\n",
    '''    fun effective(task: CreatorTask, profile: CreatorProfile = CreatorProfile()): CreatorContentDna {\n        val legacy = resolve(task, profile)\n        val explicit = task.contentDna.normalized()\n        if (explicit.isEmpty) return legacy\n\n        val selectedPlatform = explicit.platform.ifBlank { legacy.platform }\n        val selectedFormat = when {\n            explicit.deliveryFormat.isNotBlank() -> explicit.deliveryFormat\n            legacy.deliveryFormat.isNotBlank() && CreatorPlatformRegistry.acceptsFormat(selectedPlatform, legacy.deliveryFormat) -> legacy.deliveryFormat\n            else -> selectedPlatform.takeIf { it.isNotBlank() }?.let(CreatorPlatformRegistry::defaultFormat).orEmpty()\n        }\n        val merged = CreatorContentDna(\n            creatorModeId = explicit.creatorModeId.ifBlank { legacy.creatorModeId },\n            archetypeId = explicit.archetypeId.ifBlank { legacy.archetypeId },\n            productionStyles = explicit.productionStyles.ifEmpty { legacy.productionStyles },\n            platform = selectedPlatform,\n            deliveryFormat = selectedFormat,\n            legacyContentType = legacy.legacyContentType,\n            inferredFromLegacy = explicit.creatorModeId.isBlank() || explicit.archetypeId.isBlank() ||\n                explicit.productionStyles.isEmpty() || explicit.platform.isBlank() || explicit.deliveryFormat.isBlank(),\n        )\n        return merged.normalized()\n    }\n\n''',
)

# TaskStore persistence. Nested object is optional, making this backward-compatible with every old
# task JSON and portable backup.
replace_once(
    "app/src/main/java/com/framebynavin/app/data/TaskStore.kt",
    '                    .put("acknowledgedCheckpointDueAtMillis", task.acknowledgedCheckpointDueAtMillis)\n                    .put("workspace", encodeWorkspace(portableWorkspace))',
    '                    .put("acknowledgedCheckpointDueAtMillis", task.acknowledgedCheckpointDueAtMillis)\n                    .put("contentDna", encodeContentDna(task.contentDna))\n                    .put("workspace", encodeWorkspace(portableWorkspace))',
)
replace_once(
    "app/src/main/java/com/framebynavin/app/data/TaskStore.kt",
    '                        publicationIsLegacy = item.optBoolean("publicationIsLegacy", false) ||\n                            (!item.has("publishedAtMillis") && item.optString("status") == TaskStatus.DONE.name),\n                        workspace = decodeWorkspace(id, item.optJSONObject("workspace")),',
    '                        publicationIsLegacy = item.optBoolean("publicationIsLegacy", false) ||\n                            (!item.has("publishedAtMillis") && item.optString("status") == TaskStatus.DONE.name),\n                        contentDna = decodeContentDna(item.optJSONObject("contentDna")),\n                        workspace = decodeWorkspace(id, item.optJSONObject("workspace")),',
)
append_before(
    "app/src/main/java/com/framebynavin/app/data/TaskStore.kt",
    "    private fun encodeWorkspace(workspace: CreatorContentWorkspace): JSONObject {\n",
    '''    private fun encodeContentDna(dna: CreatorContentDna): JSONObject {\n        val normalized = dna.normalized()\n        val styles = JSONArray()\n        normalized.productionStyles.forEach(styles::put)\n        return JSONObject()\n            .put("creatorModeId", normalized.creatorModeId)\n            .put("archetypeId", normalized.archetypeId)\n            .put("productionStyles", styles)\n            .put("platform", normalized.platform)\n            .put("deliveryFormat", normalized.deliveryFormat)\n            .put("legacyContentType", normalized.legacyContentType)\n            .put("inferredFromLegacy", normalized.inferredFromLegacy)\n    }\n\n    private fun decodeContentDna(item: JSONObject?): CreatorContentDna {\n        if (item == null) return CreatorContentDna()\n        val styles = item.optJSONArray("productionStyles") ?: JSONArray()\n        val decodedStyles = buildSet {\n            for (i in 0 until styles.length()) {\n                styles.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)\n            }\n        }\n        return CreatorContentDna(\n            creatorModeId = item.optString("creatorModeId"),\n            archetypeId = item.optString("archetypeId"),\n            productionStyles = decodedStyles,\n            platform = item.optString("platform"),\n            deliveryFormat = item.optString("deliveryFormat"),\n            legacyContentType = item.optString("legacyContentType"),\n            inferredFromLegacy = item.optBoolean("inferredFromLegacy", false),\n        ).normalized()\n    }\n\n''',
)

# ViewModel: accept project DNA, normalize it once, and mirror platform/format into the legacy
# top-level fields so all existing workflow/reminder code keeps working unchanged.
replace_once(
    "app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt",
    '        contentType: String,\n        dueLabel: String,',
    '        contentType: String,\n        contentDna: CreatorContentDna = CreatorContentDna(),\n        dueLabel: String,',
)
replace_once(
    "app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt",
    '        if (title.isBlank()) return null\n        val enabled = reminderMode != ReminderMode.NONE',
    '''        if (title.isBlank()) return null\n        val normalizedDna = contentDna.normalized()\n        val projectPlatform = normalizedDna.platform.ifBlank { platform }\n        val projectFormat = normalizedDna.deliveryFormat.ifBlank { contentType }\n        val storedDna = if (normalizedDna.isEmpty) CreatorContentDna() else normalizedDna.copy(\n            platform = projectPlatform,\n            deliveryFormat = projectFormat,\n            inferredFromLegacy = false,\n        ).normalized()\n        val enabled = reminderMode != ReminderMode.NONE''',
)
replace_once(
    "app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt",
    '                platform = platform,\n                contentType = contentType,\n                dueLabel = dueLabel.ifBlank { "Today" },',
    '                platform = projectPlatform,\n                contentType = projectFormat,\n                contentDna = storedDna,\n                dueLabel = dueLabel.ifBlank { "Today" },',
)
replace_once(
    "app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt",
    '        val formatChanged = current.platform != platform || current.contentType != contentType\n        val newTemplate = CreatorWorkflowEngine.templateFor(platform, contentType)',
    '        val formatChanged = current.platform != projectPlatform || current.contentType != projectFormat\n        val newTemplate = CreatorWorkflowEngine.templateFor(projectPlatform, projectFormat)',
)
replace_once(
    "app/src/main/java/com/framebynavin/app/data/CreatorViewModel.kt",
    '            platform = platform,\n            contentType = contentType,\n            dueLabel = dueLabel.ifBlank { current.dueLabel },',
    '            platform = projectPlatform,\n            contentType = projectFormat,\n            contentDna = storedDna,\n            dueLabel = dueLabel.ifBlank { current.dueLabel },',
)

# Composer draft now carries the explicit project DNA.
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt",
    '    val contentType: String,\n    val dueAtMillis: Long,',
    '    val contentType: String,\n    val contentDna: CreatorContentDna,\n    val dueAtMillis: Long,',
)

# Establish effective DNA before composer state. Legacy projects are inferred for display but are not
# persisted until the creator actually taps Save.
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt",
    '    val defaultPlatform = remember(defaults.creatorProfile) { CreatorPlatformRegistry.primaryPlatform(defaults.creatorProfile) }\n    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }',
    '''    val defaultPlatform = remember(defaults.creatorProfile) { CreatorPlatformRegistry.primaryPlatform(defaults.creatorProfile) }\n    val initialDna = remember(task?.id, task?.contentDna, defaults.creatorProfile, defaultPlatform) {\n        task?.let { CreatorContentDnaEngine.effective(it, defaults.creatorProfile) } ?: run {\n            val mode = defaults.creatorProfile.resolvedPrimaryCreatorMode\n            val archetype = ContentArchetypeRegistry.suggestedForMode(mode).firstOrNull()?.id ?: "video"\n            val styles = defaults.creatorProfile.productionStyles.ifEmpty {\n                ProductionStyleRegistry.orderedForMode(mode).take(1).toSet()\n            }\n            CreatorContentDnaEngine.forNewProject(\n                profile = defaults.creatorProfile,\n                platform = defaultPlatform,\n                archetypeId = archetype,\n                productionStyles = styles,\n            )\n        }\n    }\n    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }''',
)
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt",
    '    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title.orEmpty()) }\n    var platform by rememberSaveable(task?.id) { mutableStateOf(task?.platform ?: defaultPlatform) }\n    var contentType by rememberSaveable(task?.id) { mutableStateOf(task?.contentType ?: CreatorPlatformRegistry.defaultFormat(task?.platform ?: defaultPlatform)) }',
    '''    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title.orEmpty()) }\n    var creatorModeId by rememberSaveable(task?.id) { mutableStateOf(initialDna.creatorModeId) }\n    var archetypeId by rememberSaveable(task?.id) { mutableStateOf(initialDna.archetypeId) }\n    var productionStyles by remember(task?.id) { mutableStateOf(initialDna.productionStyles) }\n    var platform by rememberSaveable(task?.id) { mutableStateOf(initialDna.platform.ifBlank { task?.platform ?: defaultPlatform }) }\n    var contentType by rememberSaveable(task?.id) {\n        mutableStateOf(initialDna.deliveryFormat.ifBlank {\n            val candidate = task?.contentType.orEmpty()\n            if (CreatorPlatformRegistry.acceptsFormat(initialDna.platform.ifBlank { task?.platform ?: defaultPlatform }, candidate)) candidate\n            else CreatorPlatformRegistry.defaultFormat(initialDna.platform.ifBlank { task?.platform ?: defaultPlatform })\n        })\n    }''',
)

# DNA option lists are recommendations first, never permission gates.
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt",
    '    val platformOptions = remember(defaults.creatorProfile, task?.platform) {\n        CreatorPlatformRegistry.orderedSelected(defaults.creatorProfile, include = task?.platform)\n    }\n    val formats = pFormats(platform)',
    '''    val modeOptions = CreatorModeRegistry.definitions\n    val archetypeOptions = remember(creatorModeId, archetypeId) {\n        (ContentArchetypeRegistry.suggestedForMode(creatorModeId) + ContentArchetypeRegistry.definitions)\n            .distinctBy { it.id }\n    }\n    val productionStyleOptions = remember(creatorModeId) { ProductionStyleRegistry.orderedForMode(creatorModeId) }\n    val platformOptions = remember(defaults.creatorProfile, task?.platform, initialDna.platform) {\n        CreatorPlatformRegistry.orderedSelected(defaults.creatorProfile, include = initialDna.platform.ifBlank { task?.platform })\n    }\n    val formats = pFormats(platform)''',
)

# Additive DNA controls after title; existing Publish On / Format / reminders remain in place.
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt",
    '                    OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Project title") }, singleLine = true, shape = RoundedCornerShape(16.dp))\n                    Spacer(Modifier.height(22.dp))\n                    PComposerLabel("PUBLISH ON")',
    '''                    OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Project title") }, singleLine = true, shape = RoundedCornerShape(16.dp))\n                    Spacer(Modifier.height(22.dp))\n                    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {\n                        Column(Modifier.padding(14.dp)) {\n                            Text("CONTENT DNA", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)\n                            Text("What this project is and how you are making it.", color = MutedText, fontSize = 8.5.sp)\n                            Spacer(Modifier.height(14.dp))\n                            PComposerLabel("CREATOR MODE")\n                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {\n                                modeOptions.forEach { mode ->\n                                    FilterChip(selected = creatorModeId == mode.id, onClick = { creatorModeId = mode.id }, label = { Text(mode.label, fontSize = 8.8.sp) })\n                                }\n                            }\n                            Spacer(Modifier.height(14.dp))\n                            PComposerLabel("CONTENT TYPE")\n                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {\n                                archetypeOptions.forEach { archetype ->\n                                    val label = runCatching { ContentArchetypeRegistry.labelForMode(archetype.id, creatorModeId) }.getOrDefault(archetype.label)\n                                    FilterChip(selected = archetypeId == archetype.id, onClick = { archetypeId = archetype.id }, label = { Text(label, fontSize = 8.6.sp) })\n                                }\n                            }\n                            Spacer(Modifier.height(14.dp))\n                            PComposerLabel("PRODUCTION STYLE · UP TO ${CreatorProfile.MAX_PRODUCTION_STYLES}")\n                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {\n                                productionStyleOptions.forEach { style ->\n                                    val selected = style in productionStyles\n                                    FilterChip(\n                                        selected = selected,\n                                        onClick = {\n                                            productionStyles = if (selected) productionStyles - style\n                                            else if (productionStyles.size < CreatorProfile.MAX_PRODUCTION_STYLES) productionStyles + style\n                                            else productionStyles\n                                        },\n                                        label = { Text(style, fontSize = 8.6.sp) },\n                                    )\n                                }\n                            }\n                        }\n                    }\n                    Spacer(Modifier.height(22.dp))\n                    PComposerLabel("PUBLISH ON")''',
)

# Save explicit DNA with the draft.
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V101BReminderUi.kt",
    '                                    platform = platform,\n                                    contentType = contentType,\n                                    dueAtMillis = dueAt,',
    '''                                    platform = platform,\n                                    contentType = contentType,\n                                    contentDna = CreatorContentDna(\n                                        creatorModeId = creatorModeId,\n                                        archetypeId = archetypeId,\n                                        productionStyles = productionStyles,\n                                        platform = platform,\n                                        deliveryFormat = contentType,\n                                        inferredFromLegacy = false,\n                                    ).normalized(),\n                                    dueAtMillis = dueAt,''',
)

# Wire composer draft through to ViewModel.
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt",
    '                    platform = draft.platform,\n                    contentType = draft.contentType,\n                    dueLabel = pDueLabel(draft.dueAtMillis),',
    '                    platform = draft.platform,\n                    contentType = draft.contentType,\n                    contentDna = draft.contentDna,\n                    dueLabel = pDueLabel(draft.dueAtMillis),',
)

# Focused unit coverage for fallback, override and compatibility semantics.
Path("app/src/test/java/com/framebynavin/app/data/CreatorContentDnaProjectIntegrationV96Test.kt").write_text('''package com.framebynavin.app.data\n\nimport org.junit.Assert.assertEquals\nimport org.junit.Assert.assertFalse\nimport org.junit.Assert.assertTrue\nimport org.junit.Test\n\nclass CreatorContentDnaProjectIntegrationV96Test {\n    private fun task(\n        platform: String = "YouTube",\n        contentType: String = "Cinematic Moment",\n        dna: CreatorContentDna = CreatorContentDna(),\n    ) = CreatorTask(\n        id = "dna-project",\n        title = "DNA project",\n        platform = platform,\n        contentType = contentType,\n        dueLabel = "Today",\n        dueAtMillis = 1L,\n        contentDna = dna,\n    )\n\n    @Test\n    fun legacyProjectStillResolvesWithoutBeingPersistentlyMigrated() {\n        val legacy = task()\n        assertTrue(legacy.contentDna.isEmpty)\n        val effective = CreatorContentDnaEngine.effective(legacy)\n        assertEquals("highlights", effective.archetypeId)\n        assertEquals("YouTube", effective.platform)\n        assertTrue(legacy.contentDna.isEmpty)\n    }\n\n    @Test\n    fun explicitProjectDnaOverridesLegacyMeaning() {\n        val explicit = CreatorContentDna(\n            creatorModeId = "gaming",\n            archetypeId = "review",\n            productionStyles = setOf("Gameplay Capture"),\n            platform = "YouTube",\n            deliveryFormat = "Long-form",\n        )\n        val effective = CreatorContentDnaEngine.effective(task(dna = explicit))\n        assertEquals("gaming", effective.creatorModeId)\n        assertEquals("review", effective.archetypeId)\n        assertEquals(setOf("Gameplay Capture"), effective.productionStyles)\n        assertEquals("Long-form", effective.deliveryFormat)\n        assertFalse(effective.inferredFromLegacy)\n    }\n\n    @Test\n    fun partialExplicitDnaUsesSafeFallbackDimensionByDimension() {\n        val explicit = CreatorContentDna(archetypeId = "analysis")\n        val effective = CreatorContentDnaEngine.effective(task(dna = explicit))\n        assertEquals("analysis", effective.archetypeId)\n        assertEquals("YouTube", effective.platform)\n        assertTrue(effective.inferredFromLegacy)\n    }\n\n    @Test\n    fun invalidDeliveryFormatCannotPollutePlatformCompatibility() {\n        val normalized = CreatorContentDna(\n            creatorModeId = "film_entertainment",\n            archetypeId = "analysis",\n            productionStyles = setOf("Voiceover"),\n            platform = "YouTube",\n            deliveryFormat = "Movie Review",\n        ).normalized()\n        assertEquals("", normalized.deliveryFormat)\n    }\n}\n''')

# Instrumented storage test is compiled in CI and documents the actual DataStore round-trip contract.
Path("app/src/androidTest/java/com/framebynavin/app/ContentDnaPersistenceV96Test.kt").write_text('''package com.framebynavin.app\n\nimport android.content.Context\nimport androidx.test.core.app.ApplicationProvider\nimport androidx.test.ext.junit.runners.AndroidJUnit4\nimport com.framebynavin.app.data.CreatorContentDna\nimport com.framebynavin.app.data.CreatorTask\nimport com.framebynavin.app.data.TaskStore\nimport kotlinx.coroutines.runBlocking\nimport org.junit.Assert.assertEquals\nimport org.junit.Assert.assertTrue\nimport org.junit.Test\nimport org.junit.runner.RunWith\n\n@RunWith(AndroidJUnit4::class)\nclass ContentDnaPersistenceV96Test {\n    @Test\n    fun explicitContentDnaSurvivesPortableRoundTrip() = runBlocking {\n        val context = ApplicationProvider.getApplicationContext<Context>()\n        val store = TaskStore(context)\n        val before = store.exportJson()\n        try {\n            val dna = CreatorContentDna(\n                creatorModeId = "gaming",\n                archetypeId = "review",\n                productionStyles = setOf("Gameplay Capture", "Voiceover"),\n                platform = "YouTube",\n                deliveryFormat = "Long-form",\n            )\n            store.save(listOf(CreatorTask("v96", "V96", "YouTube", "Long-form", "Today", 1L, contentDna = dna)))\n            val portable = store.exportJson()\n            assertTrue(portable.contains("contentDna"))\n            val restored = store.importJson(portable).single()\n            assertEquals("gaming", restored.contentDna.creatorModeId)\n            assertEquals("review", restored.contentDna.archetypeId)\n            assertEquals(setOf("Gameplay Capture", "Voiceover"), restored.contentDna.productionStyles)\n        } finally {\n            store.importJson(before)\n        }\n    }\n}\n''')

print("Materialized v96 Content DNA project integration")
