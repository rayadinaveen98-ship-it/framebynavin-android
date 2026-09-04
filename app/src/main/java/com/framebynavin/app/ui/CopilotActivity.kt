package com.framebynavin.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CopilotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FrameByNavinTheme { CreatorCopilotScreen(onClose = { finish() }) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreatorCopilotScreen(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val taskStore = remember { TaskStore(context.applicationContext) }
    val ideaStore = remember { IdeaVaultStore(context.applicationContext) }
    val secretStore = remember { CreatorCopilotSecretStore(context.applicationContext) }
    val configStore = remember { CreatorCopilotConfigStore(context.applicationContext) }
    val client = remember { CreatorCopilotClient(context.applicationContext) }

    var tasks by remember { mutableStateOf<List<CreatorTask>>(emptyList()) }
    var selectedTaskId by rememberSaveable { mutableStateOf("") }
    var tool by rememberSaveable { mutableStateOf(CreatorCopilotTool.IDEA_TO_PLAN) }
    var input by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var setupOpen by remember { mutableStateOf(!secretStore.hasKey()) }
    var keyInput by rememberSaveable { mutableStateOf("") }
    var modelInput by rememberSaveable { mutableStateOf(configStore.snapshot().model) }
    var projectMenu by remember { mutableStateOf(false) }
    var keySaved by remember { mutableStateOf(secretStore.hasKey()) }

    LaunchedEffect(Unit) {
        tasks = withContext(Dispatchers.IO) {
            taskStore.load().filter { it.status != TaskStatus.SKIPPED && it.archivedAtMillis == 0L }
                .sortedWith(compareBy<CreatorTask> { it.status == TaskStatus.DONE }.thenBy { it.dueAtMillis.takeIf { d -> d > 0L } ?: Long.MAX_VALUE })
        }
    }

    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 42.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text("V1.8 · OPTIONAL AI", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                    Text("Creator Copilot", color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = { setupOpen = !setupOpen }) {
                    Icon(Icons.Outlined.Key, "Copilot setup", tint = if (keySaved) MutedGold else MutedText)
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Draft faster. You stay in control.", color = ProjectorIvory, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text("Copilot can suggest plans, scripts and packaging. Nothing is saved or changed until you explicitly choose it.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)

            if (setupOpen) {
                Spacer(Modifier.height(18.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                    Column(Modifier.padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Security, null, tint = MutedGold, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("COPILOT SETUP", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text(if (keySaved) "CONNECTED" else "OFF", color = if (keySaved) SuccessGreen else MutedText, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Bring your own OpenAI API key. It is encrypted with Android Keystore and is not included in FrameByNavin backups. Requests go directly from this phone to the API; your API account may be billed by the provider.", color = MutedText, fontSize = 8.8.sp, lineHeight = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (keySaved) "Replace API key" else "OpenAI API key") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = modelInput,
                            onValueChange = { modelInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Model") },
                            singleLine = true,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val clean = keyInput.trim()
                                    runCatching {
                                        if (clean.isNotBlank()) secretStore.saveKey(clean)
                                        configStore.setModel(modelInput)
                                    }.onSuccess {
                                        keySaved = secretStore.hasKey()
                                        keyInput = ""
                                        setupOpen = !keySaved
                                        isError = false
                                        message = if (keySaved) "Copilot setup saved on this device." else "Model preference saved. Add an API key to enable generation."
                                    }.onFailure {
                                        isError = true
                                        message = it.message ?: "Could not save Copilot setup."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                            ) { Text("SAVE", fontWeight = FontWeight.Black, fontSize = 9.sp) }
                            if (keySaved) {
                                OutlinedButton(onClick = {
                                    secretStore.clear()
                                    keySaved = false
                                    keyInput = ""
                                    isError = false
                                    message = "Copilot API key removed from this device."
                                }) { Text("REMOVE KEY", fontSize = 8.5.sp) }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("WHAT DO YOU WANT HELP WITH?", color = ProjectorIvory, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                CreatorCopilotTool.entries.forEach { option ->
                    FilterChip(
                        selected = tool == option,
                        onClick = { tool = option; output = ""; message = null },
                        label = { Text(option.shortLabel, fontSize = 8.3.sp) },
                    )
                }
            }

            Spacer(Modifier.height(17.dp))
            Text("PROJECT CONTEXT", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            Text("Optional · Copilot can use an existing project title, format and notes.", color = MutedText, fontSize = 8.8.sp)
            Spacer(Modifier.height(7.dp))
            Box {
                OutlinedButton(onClick = { projectMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.MovieEdit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(selectedTask?.title ?: "No project selected", maxLines = 1, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.KeyboardArrowDown, null)
                }
                DropdownMenu(expanded = projectMenu, onDismissRequest = { projectMenu = false }) {
                    DropdownMenuItem(text = { Text("No project") }, onClick = { selectedTaskId = ""; projectMenu = false })
                    tasks.take(30).forEach { task ->
                        DropdownMenuItem(
                            text = { Column { Text(task.title, maxLines = 1); Text("${task.platform} · ${task.contentType}", fontSize = 10.sp, color = MutedText) } },
                            onClick = { selectedTaskId = task.id; projectMenu = false },
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                label = { Text(copilotInputLabel(tool)) },
                placeholder = { Text(copilotPlaceholder(tool), color = MutedText) },
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    if (!keySaved) {
                        setupOpen = true
                        isError = true
                        message = "Add your API key in Copilot setup first."
                    } else if (input.isBlank()) {
                        isError = true
                        message = "Add something for Copilot to work with first."
                    } else {
                        scope.launch {
                            busy = true
                            message = null
                            val result = runCatching {
                                withContext(Dispatchers.IO) { client.generate(tool, input, selectedTask) }
                            }
                            busy = false
                            result.onSuccess {
                                output = it
                                isError = false
                            }.onFailure {
                                isError = true
                                message = it.message ?: "Copilot generation failed."
                            }
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                shape = RoundedCornerShape(15.dp),
            ) {
                if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ProjectorIvory)
                else Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(if (busy) "GENERATING…" else "ASK COPILOT", fontWeight = FontWeight.Black, fontSize = 9.5.sp)
            }

            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = if (isError) RecRed else SuccessGreen, fontSize = 9.2.sp, lineHeight = 13.sp)
            }

            if (output.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Text("COPILOT DRAFT", color = ProjectorIvory, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                    Text(output, color = ProjectorIvory, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(16.dp))
                }
                Spacer(Modifier.height(10.dp))
                selectedTask?.let { project ->
                    Button(
                        onClick = {
                            scope.launch {
                                val stamp = SimpleDateFormat("dd MMM yyyy · h:mm a", Locale.getDefault()).format(Date())
                                val addition = "[COPILOT · ${tool.label.uppercase()} · $stamp]\n$output"
                                val saved = withContext(Dispatchers.IO) {
                                    taskStore.updateTask(project.id) { old ->
                                        val merged = if (old.notes.isBlank()) addition else "${old.notes.trim()}\n\n$addition"
                                        old.copy(notes = merged)
                                    }
                                }
                                if (saved != null) {
                                    tasks = tasks.map { if (it.id == saved.id) saved else it }
                                    isError = false
                                    message = "Copilot draft appended to ${project.title}."
                                } else {
                                    isError = true
                                    message = "That project could not be found."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MutedGold),
                    ) {
                        Icon(Icons.Outlined.NoteAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("APPEND TO PROJECT NOTES", fontSize = 8.8.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(7.dp))
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val now = System.currentTimeMillis()
                            val title = input.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(90)
                                ?.ifBlank { "Copilot idea" } ?: "Copilot idea"
                            val idea = CreatorIdea(
                                id = UUID.randomUUID().toString(),
                                title = title,
                                topic = input.take(300),
                                category = IdeaCategory.CINEMATIC_ANALYSIS,
                                status = IdeaStatus.INBOX,
                                potential = IdeaPotential.MEDIUM,
                                platformHint = selectedTask?.platform ?: "YouTube",
                                formatHint = selectedTask?.contentType ?: "Long-form",
                                notes = "[COPILOT · ${tool.label.uppercase()}]\n$output",
                                createdAtMillis = now,
                                updatedAtMillis = now,
                                sourceRefId = "copilot:v18:${tool.name.lowercase()}",
                            )
                            withContext(Dispatchers.IO) {
                                val current = ideaStore.load()
                                ideaStore.save(listOf(idea) + current)
                            }
                            isError = false
                            message = "Saved to Idea Vault Inbox."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Lightbulb, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("SAVE TO IDEA VAULT", fontSize = 8.8.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(24.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), RecRed.copy(alpha = .06f), border = BorderStroke(1.dp, RecRed.copy(alpha = .2f))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.VerifiedUser, null, tint = RecRed, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Copilot is a drafting tool. Verify factual claims before publishing, especially current movie news, release dates, quotes, credits and numbers.", color = MutedText, fontSize = 8.7.sp, lineHeight = 13.sp)
                }
            }
        }
    }
}

private fun copilotInputLabel(tool: CreatorCopilotTool): String = when (tool) {
    CreatorCopilotTool.IDEA_TO_PLAN -> "Rough idea"
    CreatorCopilotTool.OUTLINE -> "Topic, angle or rough notes"
    CreatorCopilotTool.HOOKS -> "Topic or current intro"
    CreatorCopilotTool.REWRITE -> "Draft to rewrite"
    CreatorCopilotTool.TITLE_PROMO -> "Video/content summary"
}

private fun copilotPlaceholder(tool: CreatorCopilotTool): String = when (tool) {
    CreatorCopilotTool.IDEA_TO_PLAN -> "Example: Pawan Kalyan's most cinematic entrances across his films…"
    CreatorCopilotTool.OUTLINE -> "What should this video explain, prove or make the viewer feel?"
    CreatorCopilotTool.HOOKS -> "Give the exact topic and what makes it interesting."
    CreatorCopilotTool.REWRITE -> "Paste your Telugu/English narration draft here."
    CreatorCopilotTool.TITLE_PROMO -> "Describe the finished content, key takeaway and target audience."
}
