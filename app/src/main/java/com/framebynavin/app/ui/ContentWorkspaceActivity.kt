package com.framebynavin.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.framebynavin.app.data.CreatorContentWorkspace
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorScriptStudio
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ScriptStudioStore
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Alpha6WorkspaceMode { HUB, PROJECT, SCRIPT, PUBLISH }

class ContentWorkspaceActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PROJECT_ID = "project_id"
    }

    private val store by lazy { TaskStore(applicationContext) }
    /** Alpha4/5 compatibility source only. Alpha6 saves structured scripts inside the project workspace. */
    private val legacyScriptStore by lazy { ScriptStudioStore(applicationContext) }
    private var task by mutableStateOf<CreatorTask?>(null)
    private var scriptStudio by mutableStateOf<CreatorScriptStudio?>(null)
    private var mode by mutableStateOf(Alpha6WorkspaceMode.HUB)
    private var error by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val projectId = intent.getStringExtra(EXTRA_PROJECT_ID).orEmpty()
        setContent {
            FrameByNavinTheme {
                when {
                    error != null -> Surface(Modifier.fillMaxSize(), color = Color(0xFF101010)) {
                        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                            Text("Workspace needs attention", color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            Text(error.orEmpty(), color = Color.LightGray)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { load(projectId) }) { Text("RELOAD PROJECT") }
                            TextButton(onClick = { error = null; mode = Alpha6WorkspaceMode.HUB }) { Text("BACK TO HUB") }
                            TextButton(onClick = { finish() }) { Text("CLOSE") }
                        }
                    }
                    task == null -> Surface(Modifier.fillMaxSize(), color = Color(0xFF101010)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    mode == Alpha6WorkspaceMode.HUB -> V19ContentWorkspaceAlpha6Hub(
                        task = task!!,
                        onDismiss = { finish() },
                        onOpenProject = { mode = Alpha6WorkspaceMode.PROJECT },
                        onOpenScript = { openScriptStudio(task!!) },
                        onOpenPublish = { mode = Alpha6WorkspaceMode.PUBLISH },
                    )
                    mode == Alpha6WorkspaceMode.PROJECT -> V19ContentWorkspaceAlpha3Dialog(
                        task = task!!,
                        onDismiss = { mode = Alpha6WorkspaceMode.HUB },
                        onSave = { id, revision, workspace -> saveProject(id, revision, workspace) },
                    )
                    mode == Alpha6WorkspaceMode.SCRIPT && scriptStudio != null -> V19ScriptStudioAlpha4Dialog(
                        task = task!!,
                        studio = scriptStudio!!,
                        onDismiss = { mode = Alpha6WorkspaceMode.HUB },
                        onSave = { studioRevision, workspaceRevision, draft ->
                            saveScriptStudio(task!!.id, studioRevision, workspaceRevision, draft)
                        },
                    )
                    mode == Alpha6WorkspaceMode.PUBLISH -> V19PublishStudioAlpha5Dialog(
                        task = task!!,
                        onDismiss = { mode = Alpha6WorkspaceMode.HUB },
                        onSave = { id, revision, workspace -> saveProject(id, revision, workspace) },
                    )
                    else -> Surface(Modifier.fillMaxSize(), color = Color(0xFF101010)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
        load(projectId)
    }

    private fun load(projectId: String) {
        error = null
        scriptStudio = null
        mode = Alpha6WorkspaceMode.HUB
        if (projectId.isBlank()) {
            error = "Project id is missing."
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val loaded = CreatorDataGate.readyTransaction(applicationContext) {
                    store.load().firstOrNull { it.id == projectId }
                        ?: error("This project no longer exists.")
                }
                migrateLegacyScriptIfNeeded(loaded)
            }
            withContext(Dispatchers.Main) {
                result.onSuccess { task = it }
                    .onFailure { error = it.message ?: "Could not open the latest project workspace." }
            }
        }
    }

    /** One-way Alpha4/5 migration. Existing sidecar data is never allowed to overwrite an embedded Alpha6 script. */
    private suspend fun migrateLegacyScriptIfNeeded(project: CreatorTask): CreatorTask {
        if (project.workspace.scriptStudio != null) return project
        val legacy = legacyScriptStore.load(
            projectId = project.id,
            legacyHook = project.workspace.hook,
            legacyScript = project.workspace.script,
        ).normalized(projectId = project.id)
        if (legacy.isEmpty()) return project
        val expectedGeneration = CreatorDataGate.generation(applicationContext)
        return store.updateTask(project.id, expectedGeneration = expectedGeneration) { current ->
            if (current.workspace.scriptStudio != null) current
            else current.copy(
                workspace = current.workspace.copy(
                    revision = current.workspace.revision + 1L,
                    hook = current.workspace.hook.ifBlank { legacy.selectedHook() },
                    script = current.workspace.script.ifBlank { legacy.compiledNarration() },
                    scriptStudio = legacy,
                )
            )
        } ?: error("This project no longer exists.")
    }

    private fun openScriptStudio(project: CreatorTask) {
        error = null
        scriptStudio = null
        mode = Alpha6WorkspaceMode.SCRIPT
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                project.workspace.scriptStudio?.normalized(projectId = project.id)
                    ?: legacyScriptStore.load(
                        projectId = project.id,
                        legacyHook = project.workspace.hook,
                        legacyScript = project.workspace.script,
                    ).normalized(projectId = project.id)
            }
            withContext(Dispatchers.Main) {
                result.onSuccess { scriptStudio = it }
                    .onFailure { error = it.message ?: "Could not open Script Studio." }
            }
        }
    }

    private fun saveProject(projectId: String, expectedRevision: Long, draft: CreatorContentWorkspace) {
        val expectedGeneration = CreatorDataGate.generation(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                store.updateTask(projectId, expectedGeneration = expectedGeneration) { current ->
                    check(current.workspace.revision == expectedRevision) {
                        "This workspace changed while you were editing. Reopen it to keep the newest version."
                    }
                    val normalized = normalizeWorkspace(draft, expectedRevision + 1L)
                    current.copy(
                        workspace = normalized.copy(
                            // Alpha3/Alpha5 editors predate the embedded field; never let a normal project/publish save erase it.
                            scriptStudio = normalized.scriptStudio ?: current.workspace.scriptStudio,
                        )
                    )
                } ?: error("This project no longer exists.")
            }
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    task = it
                    mode = Alpha6WorkspaceMode.HUB
                }.onFailure {
                    error = it.message ?: "Could not save the workspace. Your previous project data was retained."
                }
            }
        }
    }

    private fun saveScriptStudio(
        projectId: String,
        expectedStudioRevision: Long,
        expectedWorkspaceRevision: Long,
        draft: CreatorScriptStudio,
    ) {
        val expectedGeneration = CreatorDataGate.generation(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val savedStudio = draft.normalized(
                    projectId = projectId,
                    revision = expectedStudioRevision + 1L,
                )
                val updatedProject = store.updateTask(projectId, expectedGeneration = expectedGeneration) { current ->
                    check(current.workspace.revision == expectedWorkspaceRevision) {
                        "The project changed while Script Studio was open. Reopen it to keep the newest project state."
                    }
                    val actualStudioRevision = current.workspace.scriptStudio?.revision ?: 0L
                    check(actualStudioRevision == expectedStudioRevision) {
                        "This structured script changed while you were editing. Reopen Script Studio to keep the newest version."
                    }
                    current.copy(
                        workspace = current.workspace.copy(
                            revision = current.workspace.revision + 1L,
                            hook = savedStudio.selectedHook(),
                            script = savedStudio.compiledNarration(),
                            scriptStudio = savedStudio,
                        )
                    )
                } ?: error("This project no longer exists.")
                savedStudio to updatedProject
            }
            withContext(Dispatchers.Main) {
                result.onSuccess { (saved, updated) ->
                    scriptStudio = saved
                    task = updated
                    mode = Alpha6WorkspaceMode.HUB
                }.onFailure {
                    error = it.message ?: "Could not save Script Studio. Existing project data was retained."
                }
            }
        }
    }

    private fun normalizeWorkspace(draft: CreatorContentWorkspace, revision: Long): CreatorContentWorkspace = draft.copy(
        revision = revision,
        audience = draft.audience.trim(),
        viewerProblem = draft.viewerProblem.trim(),
        promise = draft.promise.trim(),
        angle = draft.angle.trim(),
        hook = draft.hook.trim(),
        script = draft.script.trimEnd(),
        references = draft.references
            .map { it.copy(label = it.label.trim(), url = it.url.trim()) }
            .filter { it.url.isNotBlank() }
            .distinctBy { it.id },
        checklist = draft.checklist
            .map { it.copy(title = it.title.trim()) }
            .filter { it.title.isNotBlank() }
            .distinctBy { it.id },
        assets = draft.assets
            .map { it.copy(label = it.label.trim(), location = it.location.trim(), notes = it.notes.trim()) }
            .filter { it.location.isNotBlank() }
            .distinctBy { it.id },
        deliverables = draft.deliverables
            .map {
                it.copy(
                    platform = it.platform.trim(),
                    format = it.format.trim(),
                    title = it.title.trim(),
                    deadlineLabel = it.deadlineLabel.trim(),
                    description = it.description.trimEnd(),
                    tags = it.tags.trim(),
                    thumbnailConcept = it.thumbnailConcept.trim(),
                    parentDeliverableId = it.parentDeliverableId.trim(),
                    publishedUrl = it.publishedUrl.trim(),
                    titleVariants = it.titleVariants
                        .map { variant -> variant.copy(text = variant.text.trim()) }
                        .filter { variant -> variant.text.isNotBlank() }
                        .distinctBy { variant -> variant.id },
                    thumbnailVariants = it.thumbnailVariants
                        .map { variant -> variant.copy(text = variant.text.trim()) }
                        .filter { variant -> variant.text.isNotBlank() }
                        .distinctBy { variant -> variant.id },
                    publishGate = it.publishGate
                        .map { gate -> gate.copy(title = gate.title.trim()) }
                        .filter { gate -> gate.title.isNotBlank() }
                        .distinctBy { gate -> gate.id },
                    publicationHistory = it.publicationHistory
                        .map { event ->
                            event.copy(
                                titleSnapshot = event.titleSnapshot.trim(),
                                thumbnailSnapshot = event.thumbnailSnapshot.trim(),
                                descriptionSnapshot = event.descriptionSnapshot.trimEnd(),
                                tagsSnapshot = event.tagsSnapshot.trim(),
                                url = event.url.trim(),
                                note = event.note.trim(),
                            )
                        }
                        .filter { event -> event.atMillis > 0L }
                        .distinctBy { event -> event.id },
                )
            }
            .filter { it.platform.isNotBlank() && it.format.isNotBlank() }
            .distinctBy { it.id },
        learnings = draft.learnings.trimEnd(),
        scriptStudio = draft.scriptStudio?.normalized(),
    )
}
