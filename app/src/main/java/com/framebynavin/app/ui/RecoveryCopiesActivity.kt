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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import java.io.File
import java.text.DateFormat
import java.util.Date

class RecoveryCopiesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FrameByNavinTheme { RecoveryCopiesScreen(onClose = { finish() }) } }
    }
}

@Composable
private fun RecoveryCopiesScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val manager = remember { CreatorBackupManager(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf(manager.recoveryCopies()) }
    var exportFile by remember { mutableStateOf<File?>(null) }
    var restoreFile by remember { mutableStateOf<File?>(null) }
    var deleteFile by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun refresh() { files = manager.recoveryCopies() }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val file = exportFile
        exportFile = null
        if (uri != null && file != null) {
            scope.launch {
                busy = true
                val result = runCatching {
                    val raw = withContext(Dispatchers.IO) {
                        val text = file.readText()
                        manager.validate(text)
                        text
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(raw) }
                            ?: error("Could not open the selected destination")
                    }
                }
                busy = false
                isError = result.isFailure
                message = if (result.isSuccess) "Recovery copy exported." else "Could not export this recovery copy."
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 36.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text("RECOVERY COPIES", color = RecRed, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("Restore journal history", color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "FrameByNavin creates one retained local copy before replacing creator data. These copies are not uploaded. Export one before restoring if you want an external copy.",
                color = MutedText,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )

            if (busy) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = RecRed, trackColor = Color(0xFF292929))
            }

            message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = if (isError) RecRed else SuccessGreen, fontSize = 10.sp)
            }

            Spacer(Modifier.height(18.dp))
            if (files.isEmpty()) {
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                    Text("No retained recovery copies on this device.", color = MutedText, modifier = Modifier.padding(16.dp), fontSize = 11.sp)
                }
            } else {
                files.forEach { file ->
                    RecoveryCopyCard(
                        file = file,
                        enabled = !busy,
                        onExport = {
                            exportFile = file
                            createDocument.launch(file.name)
                        },
                        onRestore = { restoreFile = file },
                        onDelete = { deleteFile = file },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }

    restoreFile?.let { file ->
        AlertDialog(
            onDismissRequest = { if (!busy) restoreFile = null },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Restore this recovery copy?", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = { Text("Current creator data will be replaced. FrameByNavin will first create another retained recovery copy of the current state.", color = MutedText) },
            confirmButton = {
                Button(
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    onClick = {
                        scope.launch {
                            busy = true
                            val result = runCatching {
                                val raw = withContext(Dispatchers.IO) {
                                    file.readText().also(manager::validate)
                                }
                                manager.restore(raw)
                            }
                            busy = false
                            restoreFile = null
                            if (result.isSuccess) {
                                isError = false
                                message = "Recovery restored. Restarting FrameByNavin…"
                                refresh()
                                withContext(Dispatchers.Main) {
                                    activity?.finishAffinity()
                                    context.startActivity(
                                        Intent(context, MainActivity::class.java)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    )
                                }
                            } else {
                                isError = true
                                message = "Recovery restore failed. Existing data and retained copies were kept."
                                refresh()
                            }
                        }
                    },
                ) { Text("RESTORE", fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { restoreFile = null }) { Text("CANCEL") } },
        )
    }

    deleteFile?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteFile = null },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Delete recovery copy?", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = { Text("This only deletes this retained internal recovery file. Your current creator data is not changed.", color = MutedText) },
            confirmButton = {
                TextButton(onClick = {
                    val deleted = runCatching { file.delete() }.getOrDefault(false)
                    deleteFile = null
                    refresh()
                    isError = !deleted
                    message = if (deleted) "Recovery copy deleted." else "Could not delete the recovery copy."
                }) { Text("DELETE", color = RecRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { deleteFile = null }) { Text("CANCEL") } },
        )
    }
}

@Composable
private fun RecoveryCopyCard(
    file: File,
    enabled: Boolean,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(file.lastModified()))
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(14.dp)) {
            Text(date, color = ProjectorIvory, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(3.dp))
            Text("${formatBytes(file.length())} · ${file.name}", color = MutedText, fontSize = 8.5.sp, maxLines = 2)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(enabled = enabled, onClick = onExport, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Download, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("EXPORT", fontSize = 8.sp)
                }
                Button(enabled = enabled, onClick = onRestore, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF292929))) {
                    Icon(Icons.Outlined.Restore, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("RESTORE", fontSize = 8.sp)
                }
                IconButton(enabled = enabled, onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, "Delete recovery copy", tint = RecRed) }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
