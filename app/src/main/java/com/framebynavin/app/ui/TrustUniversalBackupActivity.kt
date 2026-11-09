package com.framebynavin.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.cloud.CloudSyncManager
import com.framebynavin.app.cloud.TrustCloudBackupDeletionCoordinator
import com.framebynavin.app.cloud.TrustOwnerCloudPublisher
import com.framebynavin.app.cloud.TrustOwnerCloudReview
import com.framebynavin.app.cloud.TrustOwnerEncryptedLocalCopy
import com.framebynavin.app.cloud.TrustRestorableSourceReview
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Unified Creator + Personal owner archive management, separate from legacy
 * Sync Now. No automatic upload and never a direct "replace phone data" action.
 * Cloud operations are unavailable until the staged Trust SQL is deployed and
 * owner-only RLS/storage policies are effective. No API service key is used.
 */
class TrustUniversalBackupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrameByNavinTheme {
                TrustUniversalBackupScreen(onClose = ::finish)
            }
        }
    }
}

@Composable
internal fun TrustUniversalBackupScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = remember(context) { CloudSyncManager(context.applicationContext) }
    val local = remember(context) { TrustOwnerEncryptedLocalCopy(context.applicationContext) }
    val publisher = remember(context) { TrustOwnerCloudPublisher(context.applicationContext) }
    val cloudReview = remember(context) { TrustOwnerCloudReview(context.applicationContext) }
    val cloudDeletion = remember(context) {
        TrustCloudBackupDeletionCoordinator(account)
    }
    val owner = account.localState().session?.userId

    var passphrase by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf<List<TrustOwnerEncryptedLocalCopy.Receipt>>(emptyList()) }
    var selected by remember { mutableStateOf<TrustOwnerEncryptedLocalCopy.Receipt?>(null) }
    var localReview by remember { mutableStateOf<TrustRestorableSourceReview.Result?>(null) }
    var publishReview by remember { mutableStateOf<TrustOwnerCloudPublisher.Review?>(null) }
    var publishConfirmation by remember { mutableStateOf("") }
    var remoteReview by remember { mutableStateOf<TrustOwnerCloudReview.OwnerHeadReview?>(null) }
    var downloadedReview by remember { mutableStateOf<TrustOwnerCloudReview.SourceCoverageReview?>(null) }
    var unifiedReview by remember { mutableStateOf<TrustOwnerCloudReview.UnifiedRestoreReview?>(null) }
    var preparationConfirmation by remember { mutableStateOf("") }
    var preparedRestore by remember {
        mutableStateOf<TrustOwnerCloudReview.PreparedUnifiedSource?>(null)
    }
    var deletionReview by remember { mutableStateOf<TrustCloudBackupDeletionCoordinator.Review?>(null) }
    var deletionConfirmation by remember { mutableStateOf("") }
    var orphaned by remember { mutableIntStateOf(0) }

    // Never retain an authenticated confirmation after switching accounts.
    LaunchedEffect(owner) {
        selected = null
        localReview = null
        publishReview = null
        remoteReview = null
        downloadedReview = null
        unifiedReview = null
        preparedRestore = null
        preparationConfirmation = ""
        deletionReview = null
        passphrase = ""
        publishConfirmation = ""
        deletionConfirmation = ""
        saved = emptyList()
        orphaned = 0
        if (owner != null) {
            runCatching { local.availableCopiesForSignedInOwner() }
                .onSuccess { inventory ->
                    saved = inventory.receipts
                    orphaned = inventory.orphanedEncryptedFiles
                }
                .onFailure { message = it.message ?: "Encrypted copy catalog needs review" }
        }
    }

    fun runAction(block: suspend () -> String) {
        if (busy) return
        busy = true
        message = null
        scope.launch {
            try {
                message = block()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                message = failure.message ?: "The requested archive action was not confirmed"
            } finally {
                busy = false
                // Never keep a typed destructive phrase after a failed request.
                publishConfirmation = ""
                deletionConfirmation = ""
            }
        }
    }

    Surface(Modifier.fillMaxSize().testTag("fc13-trust-root"), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TextButton(onClick = onClose, enabled = !busy) {
                Text("BACK TO DATA & PRIVACY", color = RecRed)
            }
            Text("YOUR ENCRYPTED BACKUPS", color = ProjectorIvory,
                fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text(
                "One Creator + Personal source copy. Photos, 21-day receipts and " +
                    "voice originals are checked without replacing data on this phone.",
                color = MutedText, fontSize = 12.sp, lineHeight = 17.sp,
            )
            if (owner == null) {
                TrustUiSection("Sign in required") {
                    Text("Connect your Backlot account in Cloud Sync before creating " +
                        "an owner-bound encrypted backup.", color = MutedText,
                        fontSize = 12.sp)
                }
            } else {
                TrustUiSection("Private encryption") {
                    Text("Your password is required to create or read a copy. " +
                        "It is never uploaded to the server and cannot be recovered " +
                        "from your Google login.", color = MutedText, fontSize = 12.sp)
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it.take(512) },
                        label = { Text("Backup password (12+ characters)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                            .testTag("fc13-trust-private-passphrase"),
                        enabled = !busy,
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            runAction {
                                val password = passphrase.toCharArray()
                                try {
                                    val receipt = local.createAfterOwnerRequest(password)
                                    selected = receipt
                                    val inventory = local.availableCopiesForSignedInOwner()
                                    saved = inventory.receipts
                                    orphaned = inventory.orphanedEncryptedFiles
                                    localReview = null
                                    publishReview = null
                                    "Encrypted local copy saved and verified. " +
                                        "It is NOT yet a complete cloud restore guarantee."
                                } finally {
                                    password.fill('\u0000')
                                    passphrase = ""
                                }
                            }
                        },
                        enabled = !busy && passphrase.length >= 12,
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        modifier = Modifier.fillMaxWidth()
                            .testTag("fc13-trust-create-encrypted"),
                    ) { Text("CREATE PRIVATE ENCRYPTED COPY") }
                }

                TrustUiSection("Saved encrypted copies") {
                    if (saved.isEmpty()) {
                        Text("No verified encrypted copies listed on this phone.",
                            color = MutedText, fontSize = 12.sp)
                    }
                    saved.forEach { receipt ->
                        val chosen = selected?.copyId == receipt.copyId
                        OutlinedButton(
                            onClick = {
                                selected = receipt
                                localReview = null
                                publishReview = null
                                publishConfirmation = ""
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().testTag("fc13-trust-copy-row"),
                            border = BorderStroke(
                                1.dp, if (chosen) RecRed else CinemaLine,
                            ),
                        ) {
                            Text(
                                "COPY ${receipt.copyId.take(8)} · " +
                                    "${receipt.capturedRecords} records · " +
                                    "${receipt.encryptedBytes / 1024} KiB",
                                color = if (chosen) ProjectorIvory else MutedText,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    if (orphaned > 0) {
                        Text(
                            "$orphaned encrypted copies have no validated catalog entry. " +
                                "They have NOT been deleted or exposed as restorable.",
                            color = RecRed, fontSize = 11.sp,
                        )
                    }
                    if (selected != null) {
                        Button(
                            onClick = {
                                val receipt = checkNotNull(selected)
                                runAction {
                                    val password = passphrase.toCharArray()
                                    try {
                                        val result = local.inspectExisting(receipt, password)
                                        localReview = result
                                        "Verified ${result.sourcePartsValidated} encrypted " +
                                            "Creator and Personal sources locally."
                                    } finally {
                                        password.fill('\u0000')
                                        passphrase = ""
                                    }
                                }
                            },
                            enabled = !busy && passphrase.length >= 12,
                            modifier = Modifier.fillMaxWidth()
                                .testTag("fc13-trust-inspect-copy"),
                        ) { Text("VERIFY SELECTED COPY") }
                        localReview?.let { result ->
                            Text(
                                "Sources: ${result.sourcePartsValidated} · " +
                                    "Voice originals: ${result.referencedVoiceOriginals} · " +
                                    "Space photos: ${result.referencedSpacePhotos}",
                                color = ProjectorIvory, fontSize = 11.sp,
                            )
                            if (result.uncoveredPreferenceKeys.isNotEmpty()) {
                                Text(
                                    "Not all saved preferences have restore coverage. " +
                                        "Remote publication is blocked until this is fixed.",
                                    color = RecRed, fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }

                TrustUiSection("Owner-reviewed cloud backup") {
                    Text(
                        "This uses a separate private, encrypted Trust bucket. " +
                            "Existing Creator/Personal Sync Now is unchanged. " +
                            "Cloud upload only becomes available once the Trust " +
                            "owner-only server protocol is provisioned.",
                        color = MutedText, fontSize = 12.sp,
                    )
                    OutlinedButton(
                        onClick = {
                            val receipt = selected ?: return@OutlinedButton
                            runAction {
                                publishReview = publisher.inspectForOwnerReview(receipt)
                                "Reviewed selected copy and latest cloud revision. " +
                                    "Confirm only if this is the version you intend to publish."
                            }
                        },
                        enabled = !busy && selected != null,
                        modifier = Modifier.fillMaxWidth()
                            .testTag("fc13-trust-review-cloud-publish"),
                    ) { Text("REVIEW CLOUD REVISION") }
                    publishReview?.let { review ->
                        Text(
                            "Existing cloud revision: ${review.remoteBefore?.revision ?: 0L}. " +
                                "Proposed revision: ${review.wouldCreateNextRevision}.",
                            color = ProjectorIvory, fontSize = 12.sp,
                        )
                        if (review.localCopyIsOld) {
                            Text("An even newer local encrypted copy exists. " +
                                "You are reviewing an older version.",
                                color = RecRed, fontSize = 11.sp)
                        }
                        OutlinedTextField(
                            value = publishConfirmation,
                            onValueChange = { publishConfirmation = it.take(64) },
                            label = { Text("Type BACK UP THIS ENCRYPTED COPY") },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                val selectedReview = review
                                runAction {
                                    val password = passphrase.toCharArray()
                                    try {
                                        val confirmed = publisher.publishOwnerApproved(
                                            selectedReview, password,
                                            publishConfirmation,
                                        )
                                        publishReview = null
                                        "Encrypted cloud revision ${confirmed.revision} " +
                                            "verified byte-for-byte. Cross-domain " +
                                            "restore acceptance is still required."
                                    } finally {
                                        password.fill('\u0000')
                                        passphrase = ""
                                    }
                                }
                            },
                            enabled = !busy && passphrase.length >= 12 &&
                                publishConfirmation == "BACK UP THIS ENCRYPTED COPY",
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                            modifier = Modifier.fillMaxWidth()
                                .testTag("fc13-trust-owner-approve-publish"),
                        ) { Text("UPLOAD VERIFIED ENCRYPTED COPY") }
                    }
                }

                TrustUiSection("Preview cloud recovery") {
                    OutlinedButton(
                        onClick = {
                            runAction {
                                remoteReview = cloudReview.inspectCurrentRemote()
                                downloadedReview = null
                                unifiedReview = null
                                preparedRestore = null
                                preparationConfirmation = ""
                                if (remoteReview?.hasEncryptedBackup == true)
                                    "Verified remote revision ${remoteReview?.head?.revision}. " +
                                        "Inspect its encrypted source before any recovery."
                                else "No current universal encrypted cloud revision exists."
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                            .testTag("fc13-trust-review-remote-backup"),
                    ) { Text("CHECK ENCRYPTED CLOUD COPY") }
                    if (remoteReview?.hasEncryptedBackup == true) {
                        OutlinedButton(
                            onClick = {
                                val before = checkNotNull(remoteReview)
                                runAction {
                                    val password = passphrase.toCharArray()
                                    try {
                                        downloadedReview =
                                            cloudReview.inspectEncryptedSource(before, password)
                                        unifiedReview = null
                                        preparedRestore = null
                                        preparationConfirmation = ""
                                        "Downloaded and authenticated encrypted backup. " +
                                            "No phone records were replaced."
                                    } finally {
                                        password.fill('\u0000')
                                        passphrase = ""
                                    }
                                }
                            },
                            enabled = !busy && passphrase.length >= 12,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("DOWNLOAD & VERIFY PRIVATELY") }
                    }
                    downloadedReview?.let { verified ->
                        OutlinedButton(
                            onClick = {
                                runAction {
                                    val password = passphrase.toCharArray()
                                    try {
                                        unifiedReview =
                                            cloudReview.previewUnifiedRestore(verified, password)
                                        "Creator and Personal conflict review completed. " +
                                            "Original phone data remains untouched."
                                    } finally {
                                        password.fill('\u0000')
                                        passphrase = ""
                                    }
                                }
                            },
                            enabled = !busy && passphrase.length >= 12,
                            modifier = Modifier.fillMaxWidth()
                                .testTag("fc13-trust-unified-restore-preview"),
                        ) { Text("REVIEW CREATOR + PERSONAL RESTORE") }
                    }
                    unifiedReview?.let { review ->
                        Text(
                            "Creator: ${review.creator.incomingProjects} projects, " +
                                "${review.creator.incomingIdeas} ideas · " +
                                "${review.creator.projectIdConflicts + review.creator.ideaIdConflicts} " +
                                "ID conflicts.",
                            color = ProjectorIvory, fontSize = 12.sp,
                        )
                        Text(
                            "Personal: ${review.personal.proposedAdditions} proposed additions, " +
                                "${review.personal.conflicts} conflicts.",
                            color = ProjectorIvory, fontSize = 12.sp,
                        )
                        val canPrepare = !review.creator.hasExistingOwnerWork &&
                            review.creator.projectIdConflicts == 0 &&
                            review.creator.ideaIdConflicts == 0 &&
                            review.personal.conflicts == 0 &&
                            review.uncoveredPreferenceKeys.isEmpty()
                        if (canPrepare && downloadedReview != null) {
                            Text(
                                "Prepare both native Creator and Personal import files " +
                                    "without replacing any live records. Temporary " +
                                    "files are erased after verification.",
                                color = MutedText, fontSize = 11.sp,
                            )
                            OutlinedTextField(
                                value = preparationConfirmation,
                                onValueChange = {
                                    preparationConfirmation = it.take(64)
                                },
                                label = { Text("Type PREPARE VERIFIED RESTORE") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy,
                            )
                            OutlinedButton(
                                onClick = {
                                    val selectedCloud = checkNotNull(downloadedReview)
                                    val approvedDiff = review
                                    runAction {
                                        val trustPassword = passphrase.toCharArray()
                                        val personalPassword = passphrase.toCharArray()
                                        try {
                                            preparedRestore = cloudReview
                                                .prepareUnifiedRestoreSource(
                                                    selectedCloud,
                                                    approvedDiff,
                                                    trustPassword,
                                                    personalPassword,
                                                    preparationConfirmation,
                                                )
                                            "Creator and Personal native recovery sources " +
                                                "verified. No restore was executed."
                                        } finally {
                                            trustPassword.fill('\u0000')
                                            personalPassword.fill('\u0000')
                                            passphrase = ""
                                            preparationConfirmation = ""
                                        }
                                    }
                                },
                                enabled = !busy && passphrase.length >= 12 &&
                                    preparationConfirmation == "PREPARE VERIFIED RESTORE",
                                modifier = Modifier.fillMaxWidth()
                                    .testTag("fc13-trust-prepare-native-restores"),
                            ) { Text("PREPARE VERIFIED RESTORE SOURCES") }
                            preparedRestore?.let { prepared ->
                                Text(
                                    "Native Creator: ${prepared.creatorPortableBytes / 1024} KiB; " +
                                        "Personal: ${prepared.personalEncryptedBytes / 1024} KiB " +
                                        "verified and temporary files erased.",
                                    color = ProjectorIvory, fontSize = 11.sp,
                                )
                            }
                        } else {
                            Text(
                                "An occupied Creator workspace, conflicting Personal IDs " +
                                    "or incomplete preference coverage blocks restore " +
                                    "preparation. Existing phone data stays intact.",
                                color = RecRed, fontSize = 11.sp,
                            )
                        }
                    }
                    Text(
                        "RESTORE BACKUP remains disabled until a single " +
                            "Creator + Personal recovery transaction, rollback and " +
                            "schedule reconciliation are accepted. Preview never " +
                            "replaces your current phone records.",
                        color = MutedText, fontSize = 11.sp,
                        modifier = Modifier.testTag("fc13-trust-restore-safety-gate"),
                    )
                }

                TrustUiSection("Delete cloud backup — not your account") {
                    Text(
                        "Deleting the encrypted Trust cloud copy does not " +
                            "delete your Backlot account or local reminders.",
                        color = MutedText, fontSize = 12.sp,
                    )
                    OutlinedButton(
                        onClick = {
                            runAction {
                                deletionReview = cloudDeletion.inspectForOwnerReview()
                                "Owner cloud backup reviewed. Deletion requires " +
                                    "a second explicit confirmation."
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                            .testTag("fc13-trust-review-delete-backup"),
                    ) { Text("REVIEW CLOUD BACKUP DELETION") }
                    deletionReview?.let { review ->
                        Text(
                            "Revisions currently stored: ${review.revisionFileCount}; " +
                                "active pointer: ${review.currentHead?.revision ?: 0}.",
                            color = ProjectorIvory, fontSize = 12.sp,
                        )
                        OutlinedTextField(
                            value = deletionConfirmation,
                            onValueChange = { deletionConfirmation = it.take(64) },
                            label = { Text("Type DELETE CLOUD BACKUP") },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedButton(
                            onClick = {
                                val snapshot = review
                                runAction {
                                    cloudDeletion.deleteOwnerApproved(
                                        reviewed = snapshot,
                                        exactConfirmation = deletionConfirmation,
                                    )
                                    deletionReview = null
                                    publishReview = null
                                    remoteReview = null
                                    downloadedReview = null
                                    unifiedReview = null
                                    "Encrypted cloud backup deletion confirmed. " +
                                        "Local copies and your account were retained."
                                }
                            },
                            enabled = !busy && review.canDelete &&
                                deletionConfirmation == "DELETE CLOUD BACKUP",
                            border = BorderStroke(1.dp, RecRed),
                            modifier = Modifier.fillMaxWidth()
                                .testTag("fc13-trust-confirm-cloud-backup-delete"),
                        ) { Text("DELETE REVIEWED CLOUD BACKUP", color = RecRed) }
                    }
                }
            }
            message?.let { Text(it, color = ProjectorIvory, fontSize = 12.sp,
                modifier = Modifier.testTag("fc13-trust-action-result")) }
        }
    }
}

@Composable
private fun TrustUiSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Text(title, color = ProjectorIvory, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold)
                content()
            },
        )
    }
}
