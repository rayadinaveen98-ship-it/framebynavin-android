from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one anchor in {path}, got {count}: {old[:140]!r}")
    p.write_text(text.replace(old, new, 1))


# Content Workspace mode remains additive. Blueprint is read-only guidance and never becomes a
# second workflow authority.
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/ContentWorkspaceActivity.kt",
    "private enum class Alpha6WorkspaceMode { HUB, PROJECT, SCRIPT, PUBLISH }",
    "private enum class Alpha6WorkspaceMode { HUB, BLUEPRINT, PROJECT, SCRIPT, PUBLISH }",
)

replace_once(
    "app/src/main/java/com/framebynavin/app/ui/ContentWorkspaceActivity.kt",
    '''                        mode == Alpha6WorkspaceMode.HUB -> V19ContentWorkspaceAlpha6Hub(\n                            task = task!!,\n                            onDismiss = { finish() },\n                            onOpenProject = { mode = Alpha6WorkspaceMode.PROJECT },\n                            onOpenScript = { openScriptStudio(task!!) },\n                            onOpenPublish = { mode = Alpha6WorkspaceMode.PUBLISH },\n                        )\n                        mode == Alpha6WorkspaceMode.PROJECT -> V19ContentWorkspaceAlpha3Dialog(''',
    '''                        mode == Alpha6WorkspaceMode.HUB -> V19ContentWorkspaceAlpha6Hub(\n                            task = task!!,\n                            blueprint = CreatorContentBlueprintEngine.forTask(\n                                task = task!!,\n                                profile = CreatorOsSettingsStore(applicationContext).snapshot().creatorProfile,\n                            ),\n                            onDismiss = { finish() },\n                            onOpenBlueprint = { mode = Alpha6WorkspaceMode.BLUEPRINT },\n                            onOpenProject = { mode = Alpha6WorkspaceMode.PROJECT },\n                            onOpenScript = { openScriptStudio(task!!) },\n                            onOpenPublish = { mode = Alpha6WorkspaceMode.PUBLISH },\n                        )\n                        mode == Alpha6WorkspaceMode.BLUEPRINT -> V20ContentBlueprintDialog(\n                            task = task!!,\n                            blueprint = CreatorContentBlueprintEngine.forTask(\n                                task = task!!,\n                                profile = CreatorOsSettingsStore(applicationContext).snapshot().creatorProfile,\n                            ),\n                            onDismiss = { mode = Alpha6WorkspaceMode.HUB },\n                        )\n                        mode == Alpha6WorkspaceMode.PROJECT -> V19ContentWorkspaceAlpha3Dialog(''',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/ui/ContentWorkspaceActivity.kt",
    '''                    when (restore) {\n                        Alpha6WorkspaceMode.PROJECT -> mode = Alpha6WorkspaceMode.PROJECT\n                        Alpha6WorkspaceMode.PUBLISH -> mode = Alpha6WorkspaceMode.PUBLISH\n                        Alpha6WorkspaceMode.SCRIPT -> openScriptStudio(loaded)\n                        else -> mode = Alpha6WorkspaceMode.HUB\n                    }''',
    '''                    when (restore) {\n                        Alpha6WorkspaceMode.BLUEPRINT -> mode = Alpha6WorkspaceMode.BLUEPRINT\n                        Alpha6WorkspaceMode.PROJECT -> mode = Alpha6WorkspaceMode.PROJECT\n                        Alpha6WorkspaceMode.PUBLISH -> mode = Alpha6WorkspaceMode.PUBLISH\n                        Alpha6WorkspaceMode.SCRIPT -> openScriptStudio(loaded)\n                        else -> mode = Alpha6WorkspaceMode.HUB\n                    }''',
)

replace_once(
    "app/src/main/java/com/framebynavin/app/ui/ContentWorkspaceActivity.kt",
    '''                                        when (mode) {\n                                            Alpha6WorkspaceMode.PROJECT -> editorDrafts.clearProject(projectId)\n                                            Alpha6WorkspaceMode.SCRIPT -> editorDrafts.clearScript(projectId)\n                                            Alpha6WorkspaceMode.PUBLISH -> editorDrafts.clearPublish(projectId)\n                                            Alpha6WorkspaceMode.HUB -> Unit\n                                        }''',
    '''                                        when (mode) {\n                                            Alpha6WorkspaceMode.PROJECT -> editorDrafts.clearProject(projectId)\n                                            Alpha6WorkspaceMode.SCRIPT -> editorDrafts.clearScript(projectId)\n                                            Alpha6WorkspaceMode.PUBLISH -> editorDrafts.clearPublish(projectId)\n                                            Alpha6WorkspaceMode.BLUEPRINT, Alpha6WorkspaceMode.HUB -> Unit\n                                        }''',
)

# Hub gets one extra card and becomes scrollable so no accepted card is removed to make room.
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V19ContentWorkspaceAlpha6Ui.kt",
    "import androidx.compose.foundation.layout.*\n",
    "import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll\n",
)
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V19ContentWorkspaceAlpha6Ui.kt",
    "import androidx.compose.material.icons.outlined.CloudDone\n",
    "import androidx.compose.material.icons.outlined.AutoAwesome\nimport androidx.compose.material.icons.outlined.CloudDone\n",
)
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V19ContentWorkspaceAlpha6Ui.kt",
    "import com.framebynavin.app.data.CreatorDeliverableStatus\n",
    "import com.framebynavin.app.data.CreatorContentBlueprint\nimport com.framebynavin.app.data.CreatorDeliverableStatus\n",
)
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V19ContentWorkspaceAlpha6Ui.kt",
    '''internal fun V19ContentWorkspaceAlpha6Hub(\n    task: CreatorTask,\n    onDismiss: () -> Unit,\n    onOpenProject: () -> Unit,''',
    '''internal fun V19ContentWorkspaceAlpha6Hub(\n    task: CreatorTask,\n    blueprint: CreatorContentBlueprint,\n    onDismiss: () -> Unit,\n    onOpenBlueprint: () -> Unit,\n    onOpenProject: () -> Unit,''',
)
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V19ContentWorkspaceAlpha6Ui.kt",
    '''                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp),''',
    '''                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),''',
)
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V19ContentWorkspaceAlpha6Ui.kt",
    '''                Spacer(Modifier.height(18.dp))\n                Alpha6HubCard(\n                    icon = Icons.Outlined.Dashboard,''',
    '''                Spacer(Modifier.height(18.dp))\n                Alpha6HubCard(\n                    icon = Icons.Outlined.AutoAwesome,\n                    eyebrow = "CONTENT DNA · PERSONALIZED",\n                    title = blueprint.headline,\n                    body = "Recommended workflow, project prompts, writing structure, quality checks and useful working tools for this project.",\n                    meta = "${blueprint.modeLabel} · ${blueprint.archetypeLabel}",\n                    onClick = onOpenBlueprint,\n                )\n                Spacer(Modifier.height(10.dp))\n                Alpha6HubCard(\n                    icon = Icons.Outlined.Dashboard,''',
)
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V19ContentWorkspaceAlpha6Ui.kt",
    "                Spacer(Modifier.weight(1f))\n",
    "                Spacer(Modifier.height(24.dp))\n",
)

# FlowRow is intentionally used for compact recommendation chips.
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V20ContentBlueprintUi.kt",
    "@Composable\ninternal fun V20ContentBlueprintDialog(",
    "@OptIn(ExperimentalLayoutApi::class)\n@Composable\ninternal fun V20ContentBlueprintDialog(",
)

print("Materialized v97 DNA Blueprint integration")
