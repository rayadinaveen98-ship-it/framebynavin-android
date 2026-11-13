package com.framebynavin.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.V341UnifiedBackupManager
import com.framebynavin.app.data.V341UnifiedBackupPreview
import com.framebynavin.app.data.V341UnifiedRestoreInspection
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One owner-selected, passphrase-protected file containing BOTH canonical
 * Creator and Personal backups. Preview does not restore anything.
 *
 * Never claim a full-app transactional restore: the two existing restore
 * engines have independent journals and different replacement/addition rules.
 * Until a cross-domain owner recovery journal exists, this surface only
 * enables safe export and inspect, not an unsafe "restore all" shortcut.
 */
@Composable
internal fun V341UnifiedBackupPanel() {
    val context = LocalContext.current.applicationContext
    val manager = remember(context) { V341UnifiedBackupManager(context) }
    val scope = rememberCoroutineScope()
    var passphrase by remember { mutableStateOf("") }
    var exportingKey by remember { mutableStateOf<CharArray?>(null) }
    var verifyingKey by remember { mutableStateOf<CharArray?>(null) }
    var inspectionKey by remember { mutableStateOf<CharArray?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<V341UnifiedBackupPreview?>(null) }
    var restoreReview by remember { mutableStateOf<V341UnifiedRestoreInspection?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            exportingKey?.fill('\u0000')
            verifyingKey?.fill('\u0000')
            inspectionKey?.fill('\u0000')
        }
    }

    val createArchive = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri: Uri? ->
        val secret = exportingKey
        exportingKey = null
        if (secret == null || uri == null) {
            secret?.fill('\u0000')
        } else {
            scope.launch {
                busy = true
                preview = null
                restoreReview = null
                try {
                    val verified = withContext(Dispatchers.IO) {
                        // The document provider might commit partial bytes.
                        // Compare the EXACT exported ciphertext with the saved
                        // file, then fully authenticate both inner archives.
                        val digest = MessageDigest.getInstance("SHA-256")
                        context.contentResolver.openOutputStream(uri, "wt")?.use { target ->
                            DigestOutputStream(target, digest).use { output ->
                                manager.export(secret, output)
                            }
                        } ?: error("Cannot write selected backup location")
                        val original = digest.digest()
                        val saved = context.contentResolver.openInputStream(uri)?.use { source ->
                            val check = MessageDigest.getInstance("SHA-256")
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val n = source.read(buffer)
                                if (n == -1) break
                                if (n > 0) check.update(buffer, 0, n)
                            }
                            check.digest()
                        } ?: error("Saved backup cannot be reopened")
                        require(MessageDigest.isEqual(original, saved)) {
                            "Saved file does not match the exported archive"
                        }
                        context.contentResolver.openInputStream(uri)?.use {
                            manager.verify(secret, it)
                        } ?: error("Saved archive could not be authenticated")
                    }
                    preview = verified
                    error = false
                    message = "Unified encrypted backup saved, reopened and fully authenticated. " +
                        "Keep both this file and your private passphrase safe."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    error = true
                    message = "Unified export was not verified. A partial file may remain at " +
                        "the selected destination; do not rely on it. No Backlot data was replaced."
                } finally {
                    secret.fill('\u0000')
                    busy = false
                }
            }
        }
    }

    val openArchive = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        val secret = verifyingKey
        verifyingKey = null
        if (uri == null || secret == null) {
            secret?.fill('\u0000')
        } else {
            scope.launch {
                busy = true
                preview = null
                restoreReview = null
                try {
                    preview = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use {
                            manager.verify(secret, it)
                        } ?: error("Cannot open selected archive")
                    }
                    error = false
                    message = "Both encrypted backup sections and their media were authenticated. " +
                        "This check did not restore or change anything."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    preview = null
                    error = true
                    message = "Archive verification failed. Check the passphrase and original file. " +
                        "No Creator, Personal or Space data was changed."
                } finally {
                    secret.fill('\u0000')
                    busy = false
                }
            }
        }
    }

    val inspectForRestore = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        val secret = inspectionKey
        inspectionKey = null
        if (uri == null || secret == null) {
            secret?.fill('\u0000')
        } else {
            scope.launch {
                busy = true
                preview = null
                restoreReview = null
                try {
                    val inspection = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use {
                            manager.inspectRestore(secret, it)
                        } ?: error("Cannot open selected archive")
                    }
                    restoreReview = inspection
                    preview = inspection.backup
                    error = false
                    message = "Both archive parts authenticated. Existing Personal data " +
                        "was compared without changing any records. Unified restore " +
                        "remains disabled until cross-domain recovery is ready."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    preview = null
                    restoreReview = null
                    error = true
                    message = "Restore inspection could not be completed. A missing local " +
                        "record, invalid archive or password may need review. No data was changed."
                } finally {
                    secret.fill('\u0000')
                    busy = false
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("v341-unified-backup"),
        shape = RoundedCornerShape(18.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Text("UNIFIED PRIVATE BACKUP", color = RecRed, fontSize = 11.sp,
                fontWeight = FontWeight.Black)
            Text("One file. Creator + Personal.", color = ProjectorIvory,
                fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                "Combines the existing verified Creator archive (projects, ideas, " +
                    "Voice Idea recordings and Best Frames) with the encrypted " +
                    "Personal archive (reminders, Done receipts, challenges, " +
                    "groups, Spaces, journals and private Space photo files).",
                color = MutedText, fontSize = 12.sp, lineHeight = 17.sp,
            )
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it.take(128) },
                label = { Text("Private passphrase · 12+ characters") },
                singleLine = true,
                enabled = !busy,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().testTag("v341-unified-passphrase"),
            )
            Text("AES-256-GCM encrypted. No automatic cloud upload. Keep the passphrase " +
                "separate from the backup file; Backlot cannot recover it.",
                color = MutedGold, fontSize = 11.sp, lineHeight = 16.sp)
            Button(
                enabled = !busy && passphrase.length >= 12,
                onClick = {
                    exportingKey?.fill('\u0000')
                    exportingKey = passphrase.toCharArray()
                    passphrase = ""
                    val date = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
                    try {
                        createArchive.launch("Backlot-Unified-$date.zip")
                    } catch (_: Exception) {
                        exportingKey?.fill('\u0000')
                        exportingKey = null
                        error = true
                        message = "Could not open the backup document picker. No data was changed."
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("v341-unified-export"),
            ) {
                Text("EXPORT & VERIFY UNIFIED FILE")
            }
            OutlinedButton(
                enabled = !busy && passphrase.length >= 12,
                onClick = {
                    verifyingKey?.fill('\u0000')
                    verifyingKey = passphrase.toCharArray()
                    passphrase = ""
                    try {
                        openArchive.launch(arrayOf("application/zip",
                            "application/octet-stream", "*/*"))
                    } catch (_: Exception) {
                        verifyingKey?.fill('\u0000')
                        verifyingKey = null
                        error = true
                        message = "Could not open the file picker. Nothing was imported."
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("v341-unified-inspect"),
            ) {
                Text("VERIFY A UNIFIED FILE · NO RESTORE")
            }
            OutlinedButton(
                enabled = !busy && passphrase.length >= 12,
                onClick = {
                    inspectionKey?.fill('\u0000')
                    inspectionKey = passphrase.toCharArray()
                    passphrase = ""
                    try {
                        inspectForRestore.launch(arrayOf(
                            "application/zip", "application/octet-stream", "*/*",
                        ))
                    } catch (_: Exception) {
                        inspectionKey?.fill('\u0000')
                        inspectionKey = null
                        error = true
                        message = "The restore-review picker could not open. Nothing changed."
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("v341-unified-restore-review"),
            ) {
                Text("REVIEW RESTORE DIFFERENCES · NO WRITE")
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.testTag("v341-unified-busy"))
                Text("Authenticating the complete file and all media…",
                    color = MutedText, fontSize = 11.sp)
            }
            message?.let {
                Text(it, color = if (error) RecRed else ProjectorIvory,
                    fontSize = 12.sp, lineHeight = 17.sp,
                    modifier = Modifier.testTag("v341-unified-notice"))
            }
            preview?.let { snapshot ->
                HorizontalDivider(color = CinemaLine)
                Text(
                    "CREATOR  ·  ${snapshot.creator.projectCount} projects, " +
                        "${snapshot.creator.ideaCount} ideas, " +
                        "${snapshot.creator.activeReminderCount} project reminders, " +
                        "${snapshot.creator.personalFrameCount} Best Frames",
                    color = ProjectorIvory, fontSize = 11.sp, lineHeight = 16.sp,
                    modifier = Modifier.testTag("v341-unified-creator-preview"),
                )
                Text(
                    "PERSONAL  ·  " + snapshot.personal.label,
                    color = ProjectorIvory, fontSize = 11.sp, lineHeight = 16.sp,
                    modifier = Modifier.testTag("v341-unified-personal-preview"),
                )
                if (snapshot.creator.externalProjectAssetCount > 0) {
                    Text(
                        "ATTENTION  ·  ${snapshot.creator.externalProjectAssetCount} " +
                            "project attachment references have no portable file bytes " +
                            "in this archive. Keep those original media files separately.",
                        color = RecRed, fontSize = 11.sp, lineHeight = 16.sp,
                        modifier = Modifier.testTag("v341-unified-external-assets"),
                    )
                }
            }
            restoreReview?.let { review ->
                HorizontalDivider(color = CinemaLine)
                Text(
                    "PERSONAL RESTORE REVIEW  ·  ${review.personalDiff.proposedAdditions} " +
                        "potential additions · ${review.personalDiff.conflicts} conflicts",
                    color = if (review.personalDiff.conflicts > 0) RecRed else MutedGold,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("v341-unified-restore-diff"),
                )
                Text(
                    if (review.personalDiff.needsOwnerReview)
                        "Conflict or local integrity review required. No automatic overwrite."
                    else
                        "No Personal ID conflicts found. This is NOT permission to " +
                            "overwrite Creator records or start a combined restore.",
                    color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                )
            }
            HorizontalDivider(color = CinemaLine)
            Text(
                "RESTORE SAFETY  ·  Unified restore is not enabled yet. " +
                    "Creator restore replaces supported records; Personal encrypted " +
                    "restore adds only reviewed nonconflicting records. One combined " +
                    "restore requires an independent cross-domain recovery journal, " +
                    "so this screen will not risk a half-restored account.",
                color = MutedText, fontSize = 11.sp, lineHeight = 17.sp,
                modifier = Modifier.testTag("v341-unified-restore-gate"),
            )
            Text(
                "NOT PORTABLE  ·  External project attachments are references, not " +
                    "file copies. The existing Personal export resets CUSTOM music " +
                    "choices to piano because SAF music files and access grants " +
                    "are device-specific; Creator custom soundtrack preferences " +
                    "also remain local. Sign-in, device permissions and some " +
                    "appearance preferences are excluded. Keep separate backups until " +
                    "this full coverage is independently validated.",
                color = MutedText, fontSize = 11.sp, lineHeight = 17.sp,
            )
        }
    }
}
