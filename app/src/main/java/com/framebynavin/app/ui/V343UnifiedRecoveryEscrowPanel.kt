package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.framebynavin.app.data.V343UnifiedRecoveryEscrow
import com.framebynavin.app.data.V344UnifiedRestorePrecommit
import com.framebynavin.app.data.V344UnifiedRecoveryInspector
import com.framebynavin.app.data.V344UnifiedRecoveryCopy
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
 * H3 optional, collapsed-by-default pre-restore protection.
 *
 * A newly encrypted copy of the owner's CURRENT original Creator + Personal
 * data is only captured with explicit consent and an H2 prepared target.
 * Can reauthenticate after restart; has NO live restore execution button.
 */
@Composable
internal fun V343UnifiedRecoveryEscrowPanel() {
    val context = LocalContext.current.applicationContext
    val escrow = remember(context) { V343UnifiedRecoveryEscrow(context) }
    val precommit = remember(context) { V344UnifiedRestorePrecommit(context) }
    val recoveryInspector = remember(context) { V344UnifiedRecoveryInspector(context) }
    val scope = rememberCoroutineScope()
    var opened by remember { mutableStateOf(false) }
    var ownerPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<V343UnifiedRecoveryEscrow.Status?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    var confirmCapture by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var combinedRecovery by remember {
        mutableStateOf<V344UnifiedRecoveryInspector.Assessment?>(null)
    }

    LaunchedEffect(opened) {
        status = withContext(Dispatchers.IO) { escrow.status() }
        combinedRecovery = withContext(Dispatchers.IO) {
            recoveryInspector.inspect()
        }
    }

    suspend fun refresh() {
        status = withContext(Dispatchers.IO) { escrow.status() }
        combinedRecovery = withContext(Dispatchers.IO) {
            recoveryInspector.inspect()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("v343-escrow-root"),
        color = CinemaSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text("ORIGINAL-DATA RECOVERY", color = RecRed,
                fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text("Protect the data already on this phone",
                color = ProjectorIvory, fontSize = 15.sp,
                fontWeight = FontWeight.Bold)
            Text(
                "Before any future combined restore, keep an independent " +
                    "encrypted copy of your CURRENT Creator and Personal data. " +
                    "This is a recovery escrow, not a completed restore.",
                color = MutedText, fontSize = 11.sp, lineHeight = 17.sp,
            )
            (combinedRecovery as?
                V344UnifiedRecoveryInspector.Assessment.OwnerReview)?.let { review ->
                Text(
                    "INTERRUPTED COMBINED RECOVERY · " +
                        V344UnifiedRecoveryCopy.describe(review.kind),
                    color = RecRed, fontSize = 11.sp, lineHeight = 16.sp,
                    modifier = Modifier.testTag("v344-recovery-owner-review"),
                )
                Text(
                    "Original escrow: " +
                        (if (review.originalRetained) "checksum retained" else "unverified") +
                        " · Target: " +
                        (if (review.targetRetained) "checksum retained" else "unverified"),
                    color = MutedGold, fontSize = 10.sp,
                    modifier = Modifier.testTag("v344-recovery-evidence"),
                )
            }
            if (!opened) {
                when (status) {
                    is V343UnifiedRecoveryEscrow.Status.BaselineRetained ->
                        Text("Encrypted original is retained; reauthenticate to review.",
                            color = MutedGold, fontSize = 11.sp,
                            modifier = Modifier.testTag("v343-escrow-retained-summary"))
                    V343UnifiedRecoveryEscrow.Status.NeedsOwnerReview ->
                        Text("Incomplete or altered recovery escrow needs owner review.",
                            color = RecRed, fontSize = 11.sp,
                            modifier = Modifier.testTag("v343-escrow-warning-summary"))
                    else -> Unit
                }
            }
            OutlinedButton(
                onClick = { opened = !opened },
                modifier = Modifier.fillMaxWidth().testTag("v343-escrow-expand"),
            ) {
                Text(if (opened) "HIDE ORIGINAL-DATA PROTECTION"
                    else "REVIEW ORIGINAL-DATA PROTECTION")
            }
            if (opened) {
                when (val state = status) {
                    null -> Text("Checking recovery status…", color = MutedText,
                        fontSize = 11.sp)
                    V343UnifiedRecoveryEscrow.Status.None ->
                        Text(
                            "No encrypted original-data escrow exists. First prepare " +
                                "and verify the intended backup in the section above.",
                            color = MutedText, fontSize = 11.sp,
                            modifier = Modifier.testTag("v343-escrow-none"),
                        )
                    is V343UnifiedRecoveryEscrow.Status.BaselineRetained ->
                        Text(
                            "Original encrypted copy: ${state.sizeBytes / 1024L} KiB " +
                                "stored. A checksum matches, but owner passphrase " +
                                "is required to authenticate after restart.",
                            color = MutedGold, fontSize = 11.sp,
                            modifier = Modifier.testTag("v343-escrow-retained"),
                        )
                    V343UnifiedRecoveryEscrow.Status.NeedsOwnerReview ->
                        Text(
                            "An interrupted, modified or disconnected escrow was " +
                                "detected. No Creator or Personal data was restored. " +
                                "Do not discard unknown recovery evidence.",
                            color = RecRed, fontSize = 11.sp,
                            modifier = Modifier.testTag("v343-escrow-interrupted"),
                        )
                }
                OutlinedTextField(
                    value = ownerPassword,
                    onValueChange = { ownerPassword = it.take(128) },
                    label = { Text("Owner encrypted-archive passphrase · 12+ characters") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().testTag("v343-escrow-passphrase"),
                )
                Text(
                    "Uses your prepared archive's passphrase; the original escrow " +
                        "gets independent encryption salt and media checks. " +
                        "Neither password nor plaintext backup is stored here.",
                    color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                )
                if (status == V343UnifiedRecoveryEscrow.Status.None) {
                    OutlinedButton(
                        enabled = !loading && ownerPassword.length >= 12,
                        onClick = { confirmCapture = true },
                        modifier = Modifier.fillMaxWidth().testTag("v343-escrow-create"),
                    ) { Text("KEEP VERIFIED ORIGINAL COPY · NO RESTORE") }
                }
                if (status is V343UnifiedRecoveryEscrow.Status.BaselineRetained) {
                    OutlinedButton(
                        enabled = !loading && ownerPassword.length >= 12,
                        onClick = {
                            val password = ownerPassword.toCharArray()
                            ownerPassword = ""
                            scope.launch {
                                loading = true
                                try {
                                    val reviewed = withContext(Dispatchers.IO) {
                                        precommit.inspectOwnerReviewed(password)
                                    }
                                    error = false
                                    notice = "Original and target archives are authenticated; " +
                                        "Creator and Personal fingerprints still match. " +
                                        "${reviewed.potentialPersonalAdditions} potential " +
                                        "Personal additions were reviewed, but the shared " +
                                        "write/rollback executor is NOT enabled."
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    error = true
                                    notice = "Pre-commit evidence failed: original or target " +
                                        "archive, local data, or an independent recovery " +
                                        "checkpoint needs review. No restore ran."
                                } finally {
                                    password.fill('\u0000')
                                    refresh()
                                    loading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("v343-escrow-review"),
                    ) { Text("REAUTHENTICATE BOTH COPIES · NO WRITE") }
                }
                if (status != null && status != V343UnifiedRecoveryEscrow.Status.None) {
                    OutlinedButton(
                        enabled = !loading,
                        onClick = { confirmDiscard = true },
                        modifier = Modifier.fillMaxWidth().testTag("v343-escrow-discard"),
                    ) { Text("DISCARD ONLY THIS RECOVERY ESCROW") }
                }
                if (loading) CircularProgressIndicator(
                    modifier = Modifier.testTag("v343-escrow-loading"))
                notice?.let {
                    Text(it, color = if (error) RecRed else ProjectorIvory,
                        fontSize = 11.sp, lineHeight = 17.sp,
                        modifier = Modifier.testTag("v343-escrow-notice"))
                }
                Text(
                    "Combined restore is LOCKED. A real transaction needs " +
                        "an exclusive Creator+Personal write fence and owner-reviewed " +
                        "crash-safe checkpoints, not merely two backup files.",
                    color = MutedGold, fontSize = 11.sp, lineHeight = 16.sp,
                    modifier = Modifier.testTag("v343-escrow-no-commit"),
                )
            }
        }
    }

    if (confirmCapture) AlertDialog(
        onDismissRequest = { if (!loading) confirmCapture = false },
        title = { Text("Protect your original app data?") },
        text = { Text(
            "Backlot will make an additional encrypted copy of CURRENT Creator " +
                "and Personal data, including supported app-owned media. This " +
                "may require significant private device storage. It won't " +
                "change any reminder, project, photo or live app record."
        ) },
        confirmButton = {
            TextButton(onClick = {
                confirmCapture = false
                val secret = ownerPassword.toCharArray()
                ownerPassword = ""
                scope.launch {
                    loading = true
                    try {
                        withContext(Dispatchers.IO) {
                            escrow.retainOriginal(ownerConfirmed = true, passphrase = secret)
                        }
                        error = false
                        notice = "Original encrypted escrow saved and authenticated. " +
                            "Both Creator and Personal data remained unchanged."
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        error = true
                        notice = "Original-data escrow could not be proven complete. " +
                            "A private interrupted copy may require owner review. " +
                            "No live app data was restored or replaced."
                    } finally {
                        secret.fill('\u0000')
                        refresh()
                        loading = false
                    }
                }
            }) { Text("KEEP ENCRYPTED ORIGINAL") }
        },
        dismissButton = {
            TextButton(onClick = { confirmCapture = false }) { Text("CANCEL") }
        },
    )

    if (confirmDiscard) AlertDialog(
        onDismissRequest = { if (!loading) confirmDiscard = false },
        title = { Text("Discard this recovery escrow?") },
        text = { Text(
            "This removes only the optional, encrypted H3 original-data escrow " +
                "and its checkpoint. Your selected backup, H2 preparation and " +
                "live app data stay untouched. Corrupt/unknown recovery journals " +
                "are deliberately protected from deletion."
        ) },
        confirmButton = {
            TextButton(onClick = {
                confirmDiscard = false
                scope.launch {
                    loading = true
                    try {
                        withContext(Dispatchers.IO) {
                            escrow.discard(ownerConfirmed = true)
                        }
                        error = false
                        notice = "The optional original-data escrow was discarded. " +
                            "No backup outside this folder or live records changed."
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        error = true
                        notice = "The escrow could not be safely discarded. " +
                            "The recovery evidence is still protected."
                    } finally {
                        refresh()
                        loading = false
                    }
                }
            }) { Text("DISCARD ONLY ESCROW") }
        },
        dismissButton = {
            TextButton(onClick = { confirmDiscard = false }) { Text("KEEP RECOVERY") }
        },
    )
}
