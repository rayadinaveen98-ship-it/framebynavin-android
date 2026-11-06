package com.framebynavin.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.V300PersonalArchiveSource
import com.framebynavin.app.personal.V301ArchiveConflictPreview
import com.framebynavin.app.personal.V301DomainDiff
import com.framebynavin.app.personal.V301RestoreDryRunReport
import com.framebynavin.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Opt-in, password-protected Personal bundle workflow.
 *
 * Export checks the bytes saved by the selected Android document provider.
 * Preview authenticates every part AND compares to current local data, but
 * never installs/restores anything. The owner must keep the passphrase.
 */
@Composable
internal fun V302EncryptedPersonalArchivePanel() {
    val context = LocalContext.current.applicationContext
    val exporter = remember(context) { V300PersonalArchiveSource(context) }
    val checker = remember(context) { V301ArchiveConflictPreview(context) }
    val scope = rememberCoroutineScope()
    var passphrase by remember { mutableStateOf("") } // Not saveable to a Bundle.
    var exportingSecret by remember { mutableStateOf<CharArray?>(null) }
    var checkingSecret by remember { mutableStateOf<CharArray?>(null) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<V301RestoreDryRunReport?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var detailOpen by remember { mutableStateOf(false) }

    val createArchive = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri: Uri? ->
        val secret = exportingSecret
        exportingSecret = null
        if (uri == null || secret == null) {
            secret?.fill('\u0000')
        } else {
            scope.launch {
                busy = true
                result = null
                try {
                    val saved = withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                            exporter.export(secret, output)
                        } ?: error("The selected document cannot be written")
                        // A write acknowledgement is not proof the file persists.
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            exporter.verify(secret, input)
                        } ?: error("The saved document cannot be verified")
                    }
                    notice = "Encrypted archive saved, reopened and fully authenticated. " +
                        "Verified ${saved.verifiedPhotoFiles} photo files. Keep your passphrase " +
                        "safe; this file contains Personal data and cannot be restored by this screen yet."
                    failed = false
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    notice = "Export could not be verified. A partial file may exist at " +
                        "the selected destination. Do not treat it as a backup."
                    failed = true
                } finally {
                    secret.fill('\u0000')
                    busy = false
                }
            }
        }
    }

    val selectArchive = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        val secret = checkingSecret
        checkingSecret = null
        if (uri == null || secret == null) {
            secret?.fill('\u0000')
        } else {
            scope.launch {
                busy = true
                result = null
                try {
                    val checked = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            checker.check(input, secret)
                        } ?: error("Unable to open the selected archive")
                    }
                    result = checked
                    detailOpen = false
                    failed = false
                    notice = "The encrypted archive was authenticated and compared " +
                        "with local records. Nothing was imported or replaced."
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    failed = true
                    notice = "The archive or local inventory could not be verified. " +
                        "No Personal records, journal photos or reminders were changed."
                } finally {
                    secret.fill('\u0000')
                    busy = false
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("v302-encrypted-archive"),
        color = CinemaSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Text("ENCRYPTED PERSONAL ARCHIVE",
                color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Save a private copy", color = ProjectorIvory,
                fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                "Includes Personal reminders, Done history, challenges, groups, " +
                    "Spaces, notes and private Space photos. Creator projects " +
                    "and custom reminder songs are separate.",
                color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
            )
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it.take(128) },
                label = { Text("Private passphrase (12+ characters)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().testTag("v302-archive-passphrase"),
            )
            Text(
                "This passphrase cannot be recovered. It is not included in " +
                    "the saved archive. Do not use your account password.",
                color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
            )
            val enabled = !busy && passphrase.length >= 12
            Button(
                onClick = {
                    exportingSecret?.fill('\u0000')
                    exportingSecret = passphrase.toCharArray()
                    passphrase = ""
                    failed = false
                    notice = null
                    val filename = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
                    createArchive.launch("Backlot-Personal-Encrypted-$filename.zip")
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("v302-export-encrypted"),
            ) { Text("EXPORT & VERIFY FILE") }
            OutlinedButton(
                onClick = {
                    checkingSecret?.fill('\u0000')
                    checkingSecret = passphrase.toCharArray()
                    passphrase = ""
                    failed = false
                    notice = null
                    selectArchive.launch(arrayOf(
                        "application/zip", "application/octet-stream", "*/*",
                    ))
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("v302-preview-archive"),
            ) { Text("CHECK A FILE — NO RESTORE") }
            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Verifying Personal archive…", color = MutedText, fontSize = 12.sp)
            }
            notice?.let {
                Text(
                    it,
                    color = if (failed) RecRed else ProjectorIvory,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.testTag("v302-archive-result"),
                )
            }
            result?.let { report ->
                Text(report.neutralSummary, color = ProjectorIvory,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("v302-conflict-summary"))
                Text(report.warning, color = MutedGold,
                    fontSize = 12.sp, lineHeight = 18.sp)
                TextButton(onClick = { detailOpen = !detailOpen }) {
                    Text(if (detailOpen) "HIDE COUNTS" else "VIEW CATEGORY COUNTS")
                }
                if (detailOpen) {
                    V302DiffRow("Reminders", report.reminders)
                    V302DiffRow("Done receipts", report.completedActions)
                    V302DiffRow("Challenges", report.challenges)
                    V302DiffRow("Reminder Groups", report.groups)
                    V302DiffRow("Spaces", report.spaces)
                    V302DiffRow("Reminder links", report.spaceLinks)
                    V302DiffRow("Journal entries", report.journalEntries)
                    V302DiffRow("Private photos", report.photos)
                }
            }
            Text(
                "No restore action is available here. This is an encrypted file " +
                    "export and a read-only conflict preview, NOT proof of recovery " +
                    "on a new phone.",
                color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun V302DiffRow(label: String, diff: V301DomainDiff) {
    Text(
        "$label · ${diff.newRecords} new · ${diff.identical} identical · " +
            "${diff.conflicting} conflicts · ${diff.localOnly} phone-only",
        color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
    )
}
