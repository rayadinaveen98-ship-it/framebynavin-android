package com.framebynavin.app.ui

import android.net.Uri
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.PersonalBackupPreview
import com.framebynavin.app.personal.PersonalBackupTransfer
import com.framebynavin.app.personal.PersonalBackupIo
import com.framebynavin.app.ui.theme.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Explicit, separate Personal export/import: never connected to Creator cloud sync. */
class PersonalBackupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FrameByNavinTheme { PersonalBackupScreen(onClose = { finish() }) }
        }
    }
}

@Composable
private fun PersonalBackupScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val transfer = remember { PersonalBackupTransfer(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingDigest by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<PersonalBackupPreview?>(null) }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val raw = transfer.export()
                    val encoded = raw.toByteArray(Charsets.UTF_8)
                    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        output.write(encoded)
                        output.flush()
                    } ?: error("Could not open the chosen Personal backup destination")
                    // A provider may acknowledge a write without persisting it.
                    // Only claim verification when reading the SAVED copy back
                    // from that same destination succeeds and bytes match.
                    val saved = context.contentResolver.openInputStream(uri)?.use {
                        PersonalBackupIo.readUtf8(it)
                    } ?: error("Could not reopen the Personal backup to verify it")
                    require(digest(saved) == digest(raw)) {
                        "Saved Personal backup did not match the original data"
                    }
                    transfer.preview(saved)
                }
                error = false
                message = "Personal backup saved and verified. Keep the file private: it includes reminder names and notes and is not encrypted."
            } catch (failure: Exception) {
                error = true
                message = "Personal backup could not be verified at the selected destination. A partial file may exist. Try exporting again to local storage; your reminders were not changed."
            } finally {
                busy = false
            }
        }
    }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val checked = withContext(Dispatchers.IO) {
                    val raw = context.contentResolver.openInputStream(uri)?.use {
                        PersonalBackupIo.readUtf8(it)
                    } ?: error("Could not open Personal backup")
                    transfer.preview(raw) to digest(raw)
                }
                pendingUri = uri
                preview = checked.first
                pendingDigest = checked.second
                error = false
                message = null
            } catch (failure: Exception) {
                pendingUri = null
                pendingDigest = null
                preview = null
                error = true
                message = "This Personal backup could not be verified. Nothing was replaced. Select the original unmodified JSON export."
            } finally {
                busy = false
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            TextButton(onClick = onClose, enabled = !busy) {
                Text("‹  BACK TO BACKUPS", color = ProjectorIvory)
            }
            Spacer(Modifier.height(16.dp))
            Text("PERSONAL DATA", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Protect your reminders", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(
                "Personal Reminders use their own private backup file. They are not part of Creator, YouTube or Drive vault backups.",
                color = MutedText, fontSize = 13.sp, lineHeight = 20.sp,
            )
            Spacer(Modifier.height(22.dp))
            PersonalBackupCard(
                "Export Personal Reminders",
                "Save all Personal reminder names, notes, schedules and history to a separate JSON file. Nothing is uploaded by Backlot.",
                "EXPORT PERSONAL",
                enabled = !busy,
            ) {
                val name = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.getDefault()).format(Date())
                createDocument.launch("Backlot-Personal-$name.json")
            }
            Spacer(Modifier.height(14.dp))
            PersonalBackupCard(
                "Restore Personal Reminders",
                "Review the verified reminder counts before replacing your Personal records. Creator projects and ideas will not be touched.",
                "CHOOSE PERSONAL FILE",
                enabled = !busy,
            ) {
                openDocument.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                color = CinemaSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CinemaLine),
            ) {
                Text(
                    "PRIVACY NOTE  •  Personal exports are not encrypted. Anyone with the file can read its reminder titles and notes. Store it in a private location. Backlot keeps one prior Personal data revision on this device as an additional safety net.",
                    color = MutedText,
                    fontSize = 12.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (busy) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            message?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = if (error) RecRed else ProjectorIvory, fontSize = 12.sp)
            }
        }
    }

    preview?.let { checked ->
        AlertDialog(
            onDismissRequest = { if (!busy) { preview = null; pendingUri = null; pendingDigest = null } },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Replace Personal Reminders?", color = ProjectorIvory, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Verified backup: ${checked.reminderCount} reminders, ${checked.activeCount} active.",
                        color = ProjectorIvory, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "All existing Personal Reminders on this phone will be replaced. A previous data revision is retained locally. Imported alarm tokens are reset and future reminders are rescheduled. Creator projects and ideas stay untouched.",
                        color = MutedText, fontSize = 12.sp,
                    )
                    if (checked.reminderCount == 0) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "WARNING: This file contains ZERO reminders. Confirming will clear existing Personal Reminders.",
                            color = RecRed, fontSize = 12.sp,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val selected = pendingUri ?: return@Button
                    val expected = pendingDigest ?: return@Button
                    scope.launch {
                        busy = true
                        try {
                            val receipt = withContext(Dispatchers.IO) {
                                val raw = context.contentResolver.openInputStream(selected)?.use {
                                    PersonalBackupIo.readUtf8(it)
                                } ?: error("Could not reopen Personal backup")
                                require(digest(raw) == expected) { "The selected file changed" }
                                transfer.restoreApproved(raw)
                            }
                            error = receipt.recoveryAttentionNeeded
                            message = if (receipt.recoveryAttentionNeeded) {
                                "${receipt.importedCount} Personal Reminders restored, but some alarm cleanup or rescheduling needs attention. Reopen Backlot and check your next reminder."
                            } else {
                                "${receipt.importedCount} Personal Reminders restored. Upcoming schedules have been requested; please check your next reminder on this phone."
                            }
                        } catch (failure: Exception) {
                            error = true
                            message = "Personal restore could not be confirmed. Verify existing reminders before retrying; the app retains a previous revision locally."
                        } finally {
                            busy = false
                            preview = null
                            pendingUri = null
                            pendingDigest = null
                        }
                    }
                }, enabled = !busy) {
                    Text("REPLACE PERSONAL DATA")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    preview = null
                    pendingUri = null
                    pendingDigest = null
                }, enabled = !busy) { Text("CANCEL", color = MutedText) }
            },
        )
    }
}

@Composable
private fun PersonalBackupCard(
    title: String,
    description: String,
    action: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = CinemaSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = ProjectorIvory, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text(description, color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onClick, enabled = enabled) { Text(action) }
        }
    }
}

private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
