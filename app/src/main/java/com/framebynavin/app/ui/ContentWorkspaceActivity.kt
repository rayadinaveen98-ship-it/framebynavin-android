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

private enum class Alpha5WorkspaceMode { HUB, PROJECT, SCRIPT, PUBLISH }

class ContentWorkspaceActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PROJECT_ID = "project_id"
    }

    private val store by lazy { TaskStore(applicationContext) }
    private val scriptStore by lazy { ScriptStudioStore(applicationContext) }
    private var task by mutableStateOf<CreatorTask?>(null)
    private var scriptStudio by mutableStateOf<CreatorScriptStudio?>(null)
    private var mode by mutableStateOf(Alpha5WorkspaceMode.HUB)
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
                            TextButton(onClick = { error = null; mode = Alpha5WorkspaceMode.HUB }) { Text("BACK TO HUB") }
                            TextButton(onClick = { finish() }) { Text("CLOSE") }
                        }
                    }
                    task == null -> Surface(Modifier.fillMaxSize(), color = Color(0xFF101010)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    mode == Alpha5WorkspaceMode.HUB -> V19ContentWorkspaceAlpha5Hub(
                        task = task!!,
                        onDismiss = { finish() },
                        onOpenProject = { mode = Alpha5WorkspaceMode.PROJECT },
                        onOpenScript = { openScriptStudio(task!!) },
                        onOpenPublish = { mode = Alpha5WorkspaceMode.PUBLISH },
                    )
                    mode == Alpha5WorkspaceMode.PROJECT -> V19ContentWorkspaceAlpha3Dialog(
                        task = task!!,
                        onDismiss = { mode = Alpha5WorkspaceMode.HUB },
                        onSave = { id, revision, workspace -> saveProject(id, revision, workspace) },
                    )
                    mode == Alpha5WorkspaceMode.SCRIPT && scriptStudio != null -> V19ScriptStudioAlpha4Dialog(
                        task = task!!,
                        studio = scriptStudio!!,
                        onDismiss = { mode = Alpha5WorkspaceMode.HUB },
                        onSave = { studioRevision, workspaceRevision, draft ->
                            saveScriptStudio(task!!.id, studioRevision, workspaceRevision, draft)
                        },
                    )
                    mode == Alpha5WorkspaceMode.PUBLISH -> V19PublishStudioAlpha5Dialog(
                        task = task!!,
                        onDismiss = { mode = Alpha5WorkspaceMode.HUB },
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
        mode = Alpha5WorkspaceMode.HUB
        if (projectId.isBlank()) {
            error = "Project id is missing."
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                CreatorDataGate.readyTransaction(applicationContext) {
                    store.load().firstOrNull { it.id == projectId }
                        ?: error("This project no longer exists.")
                }
            }
            withContext(Dispatchers.Main) {
                result.onSuccess { task = it }
                    .onFailure { error = it.message ?: "Could not open the latest project workspace." }
            }
        }
    }

    private fun openScriptStudio(project: CreatorTask) {
        error = null
        scriptStudio = null
        mode = Alpha5WorkspaceMode.SCRIPT
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                scriptStore.load(
                    projectId = project.id,
                    legacyHook = project.workspace.hook,
                    legacyScript = project.workspace.script,
                )
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
                CreatorDataGate.readyTransaction(applicationContext) {
                    store.updateTask(projectId, expectedGeneration = expectedGeneration) { current ->
                        check(current.workspace.revision == expectedRevision) {
                            "This workspace changed while you were editing. Reopen it to keep the newest version."
                        }
                        current.copy(workspace = normalizeWorkspace(draft, expectedRevision + 1L))
                    } ?: error("This project no longer exists.")
                }
            }
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    task = it
                    mode = Alpha5WorkspaceMode.HUB
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
                val savedStudio = scriptStore.save(
                    projectId = projectId,
                    expectedRevision = expectedStudioRevision,
                    expectedGeneration = expectedGeneration,
                    draft = draft,
                )
                val updatedProject = CreatorDataGate.readyTransaction(applicationContext) {
                    store.updateTask(projectId, expectedGeneration = expectedGeneration) { current ->
                        check(current.workspace.revision == expectedWorkspaceRevision) {
                            "The project changed while Script Studio was open. Your structured script is safe; reopen the project before syncing its summary."
                        }
                        current.copy(
                            workspace = current.workspace.copy(
                                revision = current.workspace.revision + 1L,
                                hook = savedStudio.selectedHook(),
                                script = savedStudio.compiledNarration(),
                            )
                        )
                    } ?: error("This project no longer exists.")
                }
                savedStudio to updatedProject
            }
            withContext(Dispatchers.Main) {
                result.onSuccess { (saved, updated) ->
                    scriptStudio = saved
                    task = updated
                    mode = Alpha5WorkspaceMode.HUB
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
    )
}
