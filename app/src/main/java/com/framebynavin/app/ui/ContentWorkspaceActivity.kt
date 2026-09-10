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
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContentWorkspaceActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PROJECT_ID = "project_id"
    }

    private val store by lazy { TaskStore(applicationContext) }
    private var task by mutableStateOf<CreatorTask?>(null)
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
                            Button(onClick = { load(projectId) }) { Text("RELOAD") }
                            TextButton(onClick = { finish() }) { Text("CLOSE") }
                        }
                    }
                    task != null -> V19ContentWorkspaceDialog(
                        task = task!!,
                        onDismiss = { finish() },
                        onSave = { id, revision, workspace -> save(id, revision, workspace) },
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

    private fun save(projectId: String, expectedRevision: Long, draft: CreatorContentWorkspace) {
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
                    finish()
                }.onFailure {
                    error = it.message ?: "Could not save the workspace. Your previous project data was retained."
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
        deliverables = draft.deliverables
            .map { it.copy(platform = it.platform.trim(), format = it.format.trim(), title = it.title.trim()) }
            .filter { it.platform.isNotBlank() && it.format.isNotBlank() }
            .distinctBy { it.id },
    )
}
