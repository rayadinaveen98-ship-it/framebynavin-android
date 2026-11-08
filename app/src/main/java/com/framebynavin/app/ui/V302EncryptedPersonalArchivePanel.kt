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
import com.framebynavin.app.cloud.PersonalArchiveCloudV2Manager
import com.framebynavin.app.personal.Phase1V2ArchiveDryRun
import com.framebynavin.app.personal.Phase1V2ArchiveDryRunReport
import com.framebynavin.app.personal.Phase1V2OwnerRestoreCoordinator
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
    val cloud = remember(context) { PersonalArchiveCloudV2Manager(context) }
    val restorer = remember(context) { Phase1V2OwnerRestoreCoordinator(context) }
    val scope = rememberCoroutineScope()
    var reviewedUri by remember { mutableStateOf<Uri?>(null) }
    var reviewedCloud by remember { mutableStateOf(false) }
    var importConfirmationOpen by remember { mutableStateOf(false) }
    var recoveryConfirmationOpen by remember { mutableStateOf(false) }
    var pendingRecovery by remember { mutableStateOf(restorer.hasPendingOwnerRecovery()) }
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
                    reviewedUri = uri
                    reviewedCloud = false
                    detailOpen = false
                    failed = false
                    notice = "The encrypted archive was authenticated and compared " +
                        "with local records. Nothing was imported or replaced."
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    reviewedUri = null
                    reviewedCloud = false
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
            HorizontalDivider(color = CinemaLine)
            Text("PRIVATE ENCRYPTED CLOUD COPY", color = RecRed,
                fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                "Encrypted cloud backup for all nine Personal data areas, " +
                    "including photos and exact 21-day Done history. Maximum " +
                    "archive size: 25 MB. New revisions require all existing " +
                    "cloud records to be present on this phone; no silent overwrite.",
                color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
            )
            OutlinedButton(
                onClick = {
                    val password = passphrase.toCharArray()
                    passphrase = ""
                    scope.launch {
                        busy = true
                        result = null
                        try {
                            val revision = cloud.createFirstCloudCopy(password)
                            failed = false
                            notice = "Cloud archive revision " + revision +
                                " uploaded, read back and authenticated. " +
                                "No local records were changed."
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (_: Exception) {
                            failed = true
                            notice = "Private cloud backup was not confirmed. " +
                                "Sign in via Sync, or review an existing cloud copy. " +
                                "Nothing local was replaced."
                        } finally {
                            password.fill('\u0000')
                            busy = false
                        }
                    }
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("v302-upload-encrypted-cloud"),
            ) { Text("SAVE FIRST ENCRYPTED CLOUD COPY") }
            OutlinedButton(
                onClick = {
                    val password = passphrase.toCharArray()
                    passphrase = ""
                    scope.launch {
                        busy = true
                        result = null
                        reviewedUri = null
                        reviewedCloud = false
                        try {
                            val revision = cloud.publishReviewedNewCloudRevision(
                                password, ownerApprovedCloudRevision = true,
                            )
                            failed = false
                            notice = "All nine Personal cloud data categories were " +
                                "verified. Current saved revision: " + revision + ". " +
                                "Existing cloud records were not overwritten or lost."
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (_: Exception) {
                            failed = true
                            notice = "Cloud revision was not confirmed. Another phone " +
                                "may have newer work, a conflicting Done receipt, " +
                                "or the file may exceed the private cloud limit. " +
                                "Review the existing cloud copy before retrying."
                        } finally {
                            password.fill('\u0000')
                            busy = false
                        }
                    }
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
                    .testTag("v302-update-encrypted-cloud"),
            ) { Text("SAVE VERIFIED NEW CLOUD REVISION") }
            OutlinedButton(
                onClick = {
                    val password = passphrase.toCharArray()
                    passphrase = ""
                    scope.launch {
                        busy = true
                        result = null
                        try {
                            val checked = cloud.inspectCloudCopy(password)
                            result = checked.report
                            reviewedUri = null
                            reviewedCloud = true
                            detailOpen = false
                            failed = false
                            notice = "Private cloud archive revision " +
                                checked.revision + " downloaded and authenticated. " +
                                "Nothing was imported."
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (_: Exception) {
                            reviewedUri = null
                            reviewedCloud = false
                            failed = true
                            notice = "No readable Personal cloud archive was verified. " +
                                "Check your signed-in account and archive passphrase. " +
                                "Local records were not changed."
                        } finally {
                            password.fill('\u0000')
                            busy = false
                        }
                    }
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("v302-check-encrypted-cloud"),
            ) { Text("CHECK ENCRYPTED CLOUD COPY — NO RESTORE") }
            if (pendingRecovery) {
                HorizontalDivider(color = CinemaLine)
                Text("INTERRUPTED PERSONAL RESTORE", color = RecRed,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Changes are paused to protect your existing records. " +
                        "Enter the same archive passphrase to reauthenticate " +
                        "the encrypted original and intended backup.",
                    color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
                )
                OutlinedButton(
                    onClick = { recoveryConfirmationOpen = true },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                        .testTag("v302-resume-owner-recovery"),
                ) { Text("REVIEW INTERRUPTED RESTORE") }
            }
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
                if (report.conflicts == 0 && report.proposedAdditions > 0 &&
                    (reviewedCloud || reviewedUri != null) && !pendingRecovery) {
                    Text(
                        "Only new nonconflicting records will be added. Your " +
                            "existing reminders, challenge Done receipts and Space " +
                            "photos remain unchanged.",
                        color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
                    )
                    OutlinedButton(
                        onClick = { importConfirmationOpen = true },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                            .testTag("v302-owner-approve-import"),
                    ) { Text("REVIEW ADDITIVE RESTORE") }
                } else if (report.conflicts > 0) {
                    Text(
                        "Restoration is blocked until conflicting owner records " +
                            "can be explicitly resolved. No records were overwritten.",
                        color = RecRed, fontSize = 12.sp, lineHeight = 18.sp,
                    )
                }
            }
            Text(
                "Restoration requires explicit owner review, an authenticated " +
                    "encrypted ZIP and a verified private rollback escrow. " +
                    "No automatic cloud import or replacement is performed.",
                color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
            )
        }
    }

    if (importConfirmationOpen || recoveryConfirmationOpen) {
        val resuming = recoveryConfirmationOpen
        AlertDialog(
            onDismissRequest = {
                if (!busy) {
                    importConfirmationOpen = false
                    recoveryConfirmationOpen = false
                }
            },
            title = {
                Text(if (resuming) "Resume interrupted restore?"
                    else "Add verified Personal records?", color = ProjectorIvory)
            },
            text = {
                Text(
                    if (resuming)
                        "Backlot will reauthenticate the encrypted original and " +
                            "target, verify every recovery checkpoint and resume " +
                            "only with your approval. Existing files are never " +
                            "silently overwritten."
                    else
                        "Backlot will import only nonconflicting new records. " +
                            "It first saves an encrypted copy of your current " +
                            "Personal data, including photos and exact Done " +
                            "history. Device-specific alarm leases and custom " +
                            "song file paths are not portable. Keep this passphrase.",
                    color = MutedText, fontSize = 13.sp, lineHeight = 19.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val password = passphrase.toCharArray()
                        val escrowPassword = password.copyOf()
                        val chosenUri = reviewedUri
                        val cloudSource = reviewedCloud
                        passphrase = ""
                        importConfirmationOpen = false
                        recoveryConfirmationOpen = false
                        scope.launch {
                            busy = true
                            result = null
                            try {
                                val receipt = when {
                                    resuming -> restorer.resumeOwnerApproved(
                                        escrowPassword, password,
                                        ownerConfirmedResume = true,
                                    )
                                    cloudSource -> cloud.restoreOwnerApprovedFromCloud(
                                        password, ownerApprovedAdditions = true,
                                    )
                                    chosenUri != null -> withContext(Dispatchers.IO) {
                                        context.contentResolver.openInputStream(chosenUri)
                                            ?.use { stream ->
                                                restorer.restoreOwnerApproved(
                                                    stream, password, escrowPassword,
                                                    ownerConfirmedAdditions = true,
                                                    acknowledgedNonPortableAlarmsAndSongs = true,
                                                )
                                            } ?: error("Selected Personal ZIP cannot be reopened")
                                    }
                                    else -> error("Choose and verify a ZIP before importing")
                                }
                                notice = "Verified Personal restore completed. " +
                                    receipt.importedNewRecords + " new records added; " +
                                    "original history and private photos checked."
                                failed = false
                                reviewedUri = null
                                reviewedCloud = false
                            } catch (cancel: CancellationException) {
                                throw cancel
                            } catch (_: Exception) {
                                failed = true
                                notice = "Restore was not confirmed complete. " +
                                    "If an interrupted recovery appears above, " +
                                    "review it using the same passphrase. " +
                                    "Your recovery files have not been discarded."
                            } finally {
                                password.fill('\u0000')
                                escrowPassword.fill('\u0000')
                                pendingRecovery = restorer.hasPendingOwnerRecovery()
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && passphrase.length >= 12,
                ) { Text(if (resuming) "RESUME VERIFIED RESTORE" else "APPROVE ADDITIONS") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        importConfirmationOpen = false
                        recoveryConfirmationOpen = false
                    },
                    enabled = !busy,
                ) { Text("CANCEL") }
            },
            containerColor = CinemaSurface,
        )
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
