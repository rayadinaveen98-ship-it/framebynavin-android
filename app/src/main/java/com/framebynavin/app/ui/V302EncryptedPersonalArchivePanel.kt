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
import com.framebynavin.app.personal.Phase1V2ArchiveDryRun
import com.framebynavin.app.personal.Phase1V2ArchiveDryRunReport
import com.framebynavin.app.personal.V301DomainDiff
import com.framebynavin.app.ui.theme.*
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
    val checker = remember(context) { Phase1V2ArchiveDryRun(context) }
    val scope = rememberCoroutineScope()
    var passphrase by remember { mutableStateOf("") } // Not saveable to a Bundle.
    var exportingSecret by remember { mutableStateOf<CharArray?>(null) }
    var checkingSecret by remember { mutableStateOf<CharArray?>(null) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Phase1V2ArchiveDryRunReport?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var detailOpen by remember { mutableStateOf(false) }
    // The activity-result picker can outlive this Composable. Never keep a
    // reusable passphrase buffer after the screen is disposed.
    DisposableEffect(Unit) {
        onDispose {
            exportingSecret?.fill('\u0000')
            checkingSecret?.fill('\u0000')
        }
    }

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
                        val writtenDigest = MessageDigest.getInstance("SHA-256")
                        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                            DigestOutputStream(output, writtenDigest).use { tracked ->
                                exporter.export(secret, tracked)
                            }
                        } ?: error("The selected document cannot be written")
                        // The document provider may return success for a partial
                        // or different file. Read EVERY saved byte and compare.
                        val expected = writtenDigest.digest()
                        val actual = context.contentResolver.openInputStream(uri)?.use {
                            V302OwnerArchiveFingerprint.sha256OfAllBytes(it)
                        } ?: error("The saved file cannot be reopened for verification")
                        require(V302OwnerArchiveFingerprint.matchesExportedBytes(expected, actual)) {
                            "Saved archive differs from the exported archive"
                        }
                        // Independently authenticate the complete encrypted
                        // payload, every domain and each recorded private photo.
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            exporter.verify(secret, input)
                        } ?: error("The saved document cannot be authenticated")
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
                color = RecRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    try {
                        createArchive.launch("Backlot-Personal-Encrypted-$filename.zip")
                    } catch (_: Exception) {
                        exportingSecret?.fill('\u0000')
                        exportingSecret = null
                        failed = true
                        notice = "No compatible document picker was available. Nothing was exported."
                    }
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
                    try {
                        selectArchive.launch(arrayOf(
                            "application/zip", "application/octet-stream", "*/*",
                        ))
                    } catch (_: Exception) {
                        checkingSecret?.fill('\u0000')
                        checkingSecret = null
                        failed = true
                        notice = "No compatible document picker was available. Nothing was imported."
                    }
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
                Text(
                    "${report.proposedAdditions} new records · ${report.conflicts} conflicts",
                    color = ProjectorIvory,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("v302-conflict-summary"))
                Text(
                    if (report.needsOwnerReview)
                        "Some records need your review. Nothing was changed."
                    else "All nine data categories checked. No restore performed.",
                    color = if (report.needsOwnerReview) RecRed else ProjectorIvory,
                    fontSize = 12.sp, lineHeight = 18.sp)
                TextButton(onClick = { detailOpen = !detailOpen }) {
                    Text(if (detailOpen) "HIDE COUNTS" else "VIEW CATEGORY COUNTS")
                }
                if (detailOpen) {
                    val older = report.eightDomains
                    V302DiffRow("Reminders", older.reminders)
                    V302DiffRow("Done receipts", older.completedActions)
                    V302DiffRow("Challenges", older.challenges)
                    V302DiffRow("Reminder Groups", older.groups)
                    V302DiffRow("Spaces", older.spaces)
                    V302DiffRow("Reminder links", older.spaceLinks)
                    V302DiffRow("Journal entries", older.journalEntries)
                    V302DiffRow("Private photos", older.photos)
                    Text(
                        "Single-activity 21-day challenges · " +
                            "${report.newChallenges.additions} new · " +
                            "${report.newChallenges.identical} identical · " +
                            "${report.newChallenges.conflicts} conflicts · " +
                            "${report.newChallenges.localOnly} phone-only",
                        color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                    )
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
