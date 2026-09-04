package com.framebynavin.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.MainActivity
import com.framebynavin.app.data.CreatorBackupManager
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FrameByNavinTheme { BackupScreen(onClose = { finish() }) } }
    }
}

@Composable
private fun BackupScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val manager = remember { CreatorBackupManager(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var pendingRestoreRaw by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<CreatorBackupManager.BackupPreview?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val raw = pendingExport
        pendingExport = null
        if (uri != null && raw != null) {
            scope.launch {
                busy = true
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(raw) }
                            ?: error("Could not open the selected file.")
                    }
                }
                busy = false
                isError = result.isFailure
                message = if (result.isSuccess) "Backup saved successfully." else result.exceptionOrNull()?.message ?: "Backup failed."
            }
        }
    }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val result = runCatching {
                    val raw = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: error("Could not read the selected file.")
                    }
                    val checked = manager.validate(raw)
                    raw to checked
                }
                busy = false
                result.onSuccess { (raw, checked) ->
                    pendingRestoreRaw = raw
                    preview = checked
                    message = null
                }.onFailure {
                    isError = true
                    message = it.message ?: "That file is not a valid FrameByNavin backup."
                }
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 40.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text("DATA & BACKUP", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text("Keep your creator data safe", color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Everything stays local unless you choose where to save a backup.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(20.dp))

            BackupActionCard(
                title = "Export backup",
                body = "Projects, Studio progress, reminders, Smart timing, Idea Vault, weekly schedule and settings.",
                icon = Icons.Outlined.CloudUpload,
                button = "EXPORT BACKUP",
                enabled = !busy,
            ) {
                scope.launch {
                    busy = true
                    val result = runCatching { manager.createBackup() }
                    busy = false
                    result.onSuccess { raw ->
                        pendingExport = raw
                        val name = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.getDefault()).format(Date())
                        createDocument.launch("FrameByNavin-Backup-$name.fbnbackup")
                    }.onFailure {
                        isError = true
                        message = it.message ?: "Could not create backup."
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            BackupActionCard(
                title = "Restore backup",
                body = "The app validates the file and shows what it contains before replacing anything.",
                icon = Icons.Outlined.CloudDownload,
                button = "CHOOSE BACKUP",
                enabled = !busy,
            ) {
                openDocument.launch(arrayOf("application/octet-stream", "application/json", "text/plain", "*/*"))
            }

            if (busy) {
                Spacer(Modifier.height(18.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = RecRed, trackColor = Color(0xFF292929))
            }

            message?.let { text ->
                Spacer(Modifier.height(16.dp))
                Surface(
                    Modifier.fillMaxWidth(),
                    RoundedCornerShape(16.dp),
                    if (isError) Color(0xFF1A1110) else Color(0xFF101812),
                    border = BorderStroke(1.dp, if (isError) RecRed.copy(alpha = .4f) else SuccessGreen.copy(alpha = .35f)),
                ) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle, null, tint = if (isError) RecRed else SuccessGreen)
                        Spacer(Modifier.width(9.dp))
                        Text(text, color = ProjectorIvory, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                Column(Modifier.padding(15.dp)) {
                    Text("RESTORE SAFETY", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Before restore, FrameByNavin keeps a temporary local snapshot. If importing fails, your current data is put back automatically.", color = MutedText, fontSize = 9.3.sp, lineHeight = 14.sp)
                }
            }
        }
    }

    preview?.let { checked ->
        AlertDialog(
            onDismissRequest = { if (!busy) { preview = null; pendingRestoreRaw = null } },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Restore this backup?", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = {
                Column {
                    BackupPreviewRow("Projects", checked.projectCount.toString())
                    BackupPreviewRow("Ideas", checked.ideaCount.toString())
                    BackupPreviewRow("Weekly slots", checked.weeklySlotCount.toString())
                    BackupPreviewRow("Active reminders", checked.activeReminderCount.toString())
                    BackupPreviewRow("Settings", if (checked.settingsIncluded) "Included" else "Missing")
                    Spacer(Modifier.height(10.dp))
                    Text("This replaces the current local Creator OS data.", color = MutedText, fontSize = 9.5.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val raw = pendingRestoreRaw ?: return@Button
                        scope.launch {
                            busy = true
                            val result = runCatching { manager.restore(raw) }
                            busy = false
                            preview = null
                            pendingRestoreRaw = null
                            if (result.isSuccess) {
                                isError = false
                                message = "Backup restored. Restarting FrameByNavin…"
                                withContext(Dispatchers.Main) {
                                    activity?.finishAffinity()
                                    context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                                }
                            } else {
                                isError = true
                                message = result.exceptionOrNull()?.message ?: "Restore failed. Your previous data was put back."
                            }
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text("RESTORE", fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { preview = null; pendingRestoreRaw = null }, enabled = !busy) { Text("CANCEL", color = MutedText) }
            },
        )
    }
}

@Composable
private fun BackupActionCard(
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    button: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(21.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(17.dp)) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                Surface(shape = CircleShape, color = MutedGold.copy(alpha = .10f), modifier = Modifier.fillMaxSize()) { }
                Icon(icon, null, tint = MutedGold, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(body, color = MutedText, fontSize = 9.5.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF292929)), shape = RoundedCornerShape(14.dp)) {
                Text(button, color = ProjectorIvory, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BackupPreviewRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = MutedText, fontSize = 10.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
