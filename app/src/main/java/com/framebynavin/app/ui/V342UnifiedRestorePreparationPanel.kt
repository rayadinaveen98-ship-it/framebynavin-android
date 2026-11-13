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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.framebynavin.app.data.V342UnifiedRestorePreparationStore
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Restore preparation is separate from (and cannot perform) a restore.
 * On-disk, owner-selected archive is kept encrypted, with bounded copy,
 * a checksum journal and a fresh conflict check for every inspection.
 * Missing, corrupted or interrupted plans are never silently discarded.
 */
@Composable
internal fun V342UnifiedRestorePreparationPanel() {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { V342UnifiedRestorePreparationStore(context) }
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }
    var pendingSecret by remember { mutableStateOf<CharArray?>(null) }
    var status by remember { mutableStateOf<V342UnifiedRestorePreparationStore.Status?>(null) }
    var loading by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var proposed by remember { mutableStateOf<Int?>(null) }
    var conflicts by remember { mutableStateOf<Int?>(null) }
    var prepareConfirmation by remember { mutableStateOf(false) }
    var discardConfirmation by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { pendingSecret?.fill('\u0000') }
    }

    // Pending owner preparation is visible even when the advanced controls
    // are collapsed. Only read checksum evidence; never auto-restore.
    LaunchedEffect(expanded) {
        status = withContext(Dispatchers.IO) { store.status() }
    }

    val chooseSource = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        val secret = pendingSecret
        pendingSecret = null
        if (uri == null || secret == null) {
            secret?.fill('\u0000')
        } else {
            scope.launch {
                loading = true
                proposed = null
                conflicts = null
                try {
                    val review = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { source ->
                            store.prepare(source, secret, ownerRequested = true)
                        } ?: error("Could not read chosen encrypted owner archive")
                    }
                    proposed = review.personalDiff.proposedAdditions
                    conflicts = review.personalDiff.conflicts
                    failure = false
                    message = "Encrypted restore source saved and authenticated for owner review. " +
                        "No Creator, Personal, alarm, challenge or photo data was restored."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    failure = true
                    message = "Restore preparation did not finish. A partial private source may " +
                        "need your review or explicit discard; no live Backlot data was changed."
                } finally {
                    secret.fill('\u0000')
                    status = withContext(Dispatchers.IO) { store.status() }
                    loading = false
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("v342-prepare-root"),
        shape = RoundedCornerShape(18.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("RESTORE PREPARATION", color = RecRed,
                fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text("Protect the next restore step", color = ProjectorIvory,
                fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                "Prepare a selected encrypted unified archive for restart-safe review. " +
                    "This keeps an authenticated source on this phone, but does not " +
                    "write or restore any app records.",
                color = MutedText, fontSize = 11.sp, lineHeight = 17.sp,
            )
            if (!expanded) {
                when (status) {
                    is V342UnifiedRestorePreparationStore.Status.ChecksumMatched ->
                        Text("Encrypted source retained for owner reauthentication.",
                            color = MutedGold, fontSize = 11.sp,
                            modifier = Modifier.testTag("v342-prepare-pending-summary"))
                    V342UnifiedRestorePreparationStore.Status.NeedsOwnerReview ->
                        Text("Interrupted preparation needs owner review; no restore occurred.",
                            color = RecRed, fontSize = 11.sp,
                            modifier = Modifier.testTag("v342-prepare-warning-summary"))
                    else -> Unit
                }
            }
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().testTag("v342-prepare-expand"),
            ) {
                Text(if (expanded) "HIDE RESTORE PREPARATION"
                    else "OPEN ADVANCED RESTORE PREPARATION")
            }
            if (expanded) {
                when (val value = status) {
                    null -> Text("Checking for previously selected archive…",
                        color = MutedText, fontSize = 11.sp)
                    V342UnifiedRestorePreparationStore.Status.None ->
                        Text("No prepared restore source on this phone.",
                            color = MutedText, fontSize = 11.sp,
                            modifier = Modifier.testTag("v342-preparation-none"))
                    is V342UnifiedRestorePreparationStore.Status.ChecksumMatched ->
                        Text(
                            "Stored encrypted source matches its saved checksum (" +
                                "${value.sizeBytes / 1024L} KiB). Re-enter your " +
                                "passphrase to authenticate its contents against today's " +
                                "Personal records. No restore is enabled.",
                            color = ProjectorIvory, fontSize = 11.sp,
                            modifier = Modifier.testTag("v342-preparation-verified"),
                        )
                    V342UnifiedRestorePreparationStore.Status.NeedsOwnerReview ->
                        Text(
                            "Interrupted, incomplete or changed preparation detected. " +
                                "No restore was attempted. Review or explicitly discard it.",
                            color = RecRed, fontSize = 11.sp,
                            modifier = Modifier.testTag("v342-preparation-interrupted"),
                        )
                }
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it.take(128) },
                    enabled = !loading,
                    label = { Text("Unified archive passphrase · 12+ characters") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("v342-prepare-passphrase"),
                )
                Text(
                    "Preparing makes another encrypted copy in app-private storage and " +
                        "requires enough free space. Your original selected file is untouched. " +
                        "The passphrase is never saved.",
                    color = MutedGold, fontSize = 11.sp, lineHeight = 16.sp,
                )
                Button(
                    enabled = !loading && passphrase.length >= 12 &&
                        status == V342UnifiedRestorePreparationStore.Status.None,
                    onClick = { prepareConfirmation = true },
                    modifier = Modifier.fillMaxWidth().testTag("v342-prepare-file"),
                ) { Text("PREPARE ENCRYPTED SOURCE · NO RESTORE") }
                if (status is V342UnifiedRestorePreparationStore.Status.ChecksumMatched) {
                    OutlinedButton(
                        enabled = !loading && passphrase.length >= 12,
                        onClick = {
                            val secret = passphrase.toCharArray()
                            passphrase = ""
                            scope.launch {
                                loading = true
                                try {
                                    val review = withContext(Dispatchers.IO) {
                                        store.inspectPrepared(secret)
                                    }
                                    proposed = review.personalDiff.proposedAdditions
                                    conflicts = review.personalDiff.conflicts
                                    failure = false
                                    message = "Reauthenticated the retained file against current " +
                                        "Personal records. No data was restored."
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    failure = true
                                    message = "Stored archive or current Personal records could " +
                                        "not be reverified. No changes were made."
                                } finally {
                                    secret.fill('\u0000')
                                    status = withContext(Dispatchers.IO) { store.status() }
                                    loading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("v342-prepare-recheck"),
                    ) { Text("RECHECK PREPARED SOURCE · NO WRITE") }
                }
                if (status != null &&
                    status != V342UnifiedRestorePreparationStore.Status.None) {
                    OutlinedButton(
                        enabled = !loading,
                        onClick = { discardConfirmation = true },
                        modifier = Modifier.fillMaxWidth().testTag("v342-prepare-discard"),
                    ) { Text("DISCARD THIS LOCAL PREPARATION") }
                }
                if (loading) CircularProgressIndicator(
                    modifier = Modifier.testTag("v342-prepare-loading"))
                message?.let {
                    Text(it, color = if (failure) RecRed else ProjectorIvory,
                        fontSize = 11.sp, lineHeight = 17.sp,
                        modifier = Modifier.testTag("v342-prepare-message"))
                }
                proposed?.let { additions ->
                    Text(
                        "Current Personal comparison · $additions potential additions · " +
                            "${conflicts ?: 0} conflicts. Creator replacement remains disabled.",
                        color = MutedText, fontSize = 11.sp,
                        modifier = Modifier.testTag("v342-prepare-review"),
                    )
                }
            }
        }
    }

    if (prepareConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!loading) prepareConfirmation = false },
            title = { Text("Keep a private restore source?") },
            text = { Text(
                "Backlot will copy the selected encrypted archive to its private " +
                    "storage and verify both original backup sections. This uses " +
                    "device space but changes no Creator or Personal data. " +
                    "Unified restore is still disabled."
            ) },
            confirmButton = {
                TextButton(onClick = {
                    prepareConfirmation = false
                    pendingSecret?.fill('\u0000')
                    pendingSecret = passphrase.toCharArray()
                    passphrase = ""
                    try {
                        chooseSource.launch(arrayOf(
                            "application/zip", "application/octet-stream", "*/*",
                        ))
                    } catch (_: Exception) {
                        pendingSecret?.fill('\u0000')
                        pendingSecret = null
                        failure = true
                        message = "Could not open the document picker. No data was changed."
                    }
                }) { Text("CHOOSE ENCRYPTED FILE") }
            },
            dismissButton = {
                TextButton(onClick = { prepareConfirmation = false }) { Text("CANCEL") }
            },
        )
    }

    if (discardConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!loading) discardConfirmation = false },
            title = { Text("Discard prepared source?") },
            text = { Text(
                "This only removes Backlot's private staged encrypted copy and its " +
                    "preparation record. It does not delete your original backup, " +
                    "existing Creator projects, Personal reminders or recovery escrows."
            ) },
            confirmButton = {
                TextButton(onClick = {
                    discardConfirmation = false
                    scope.launch {
                        loading = true
                        try {
                            withContext(Dispatchers.IO) {
                                store.discard(ownerConfirmed = true)
                            }
                            failure = false
                            message = "Local encrypted preparation discarded. " +
                                "Original backup and app records were not changed."
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            failure = true
                            message = "Prepared files could not be safely discarded; " +
                                "they were not silently replaced."
                        } finally {
                            status = withContext(Dispatchers.IO) { store.status() }
                            loading = false
                        }
                    }
                }) { Text("DISCARD LOCAL COPY") }
            },
            dismissButton = {
                TextButton(onClick = { discardConfirmation = false }) { Text("KEEP COPY") }
            },
        )
    }
}
