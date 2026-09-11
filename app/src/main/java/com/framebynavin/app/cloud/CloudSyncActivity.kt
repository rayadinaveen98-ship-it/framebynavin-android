package com.framebynavin.app.cloud

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.content.MutableContextWrapper
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CloudSyncActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FrameByNavinTheme { CloudSyncScreen(onClose = ::finish) } }
    }
}

@Composable
private fun CloudSyncScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { CloudSyncManager(context.applicationContext) }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val credentialContext = remember(context) { MutableContextWrapper(context) }
    var state by remember { mutableStateOf(manager.localState()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var restoreTarget by remember { mutableStateOf<CloudRestorePoint?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmResume by remember { mutableStateOf(false) }
    var confirmAbandonDeletion by remember { mutableStateOf(false) }
    var confirmKeepLocal by remember { mutableStateOf(false) }
    var restorePointsLoading by remember { mutableStateOf(state.session != null) }
    var restorePointsError by remember { mutableStateOf<String?>(null) }

    fun reloadPoints() {
        state = manager.localState()
        if (state.session == null) {
            restorePointsLoading = false
            restorePointsError = null
            return
        }
        restorePointsLoading = true
        restorePointsError = null
        scope.launch {
            val result = manager.restorePoints()
            result.onSuccess {
                state = manager.localState().copy(restorePoints = it)
                restorePointsError = null
            }.onFailure { error ->
                restorePointsError = error.message ?: "Couldn't load restore points. Your cloud data was not changed."
            }
            restorePointsLoading = false
        }
    }

    fun runOperation(block: suspend () -> CloudOperationResult) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val result = block()
                message = when (result) {
                    is CloudOperationResult.Success -> result.message
                    is CloudOperationResult.Skipped -> result.message
                    is CloudOperationResult.Failure -> result.message
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                message = "Cloud operation could not finish. Your local data is kept; review the account status before retrying."
            } finally {
                busy = false
                reloadPoints()
            }
        }
    }

    fun startGoogleSignIn() {
        if (busy || CloudConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return
        busy = true
        scope.launch {
            try {
                val googleOption = GetSignInWithGoogleOption.Builder(CloudConfig.GOOGLE_WEB_CLIENT_ID).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleOption)
                    .build()
                val result = credentialManager.getCredential(
                    context = credentialContext,
                    request = request,
                )
                val credential = result.credential
                val idToken = if (
                    credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    GoogleIdTokenCredential.createFrom(credential.data).idToken
                } else {
                    null
                }

                if (idToken.isNullOrBlank()) {
                    message = "Google couldn't complete sign-in. Try again."
                } else {
                    val operation = manager.completeGoogleSignIn(idToken)
                    message = when (operation) {
                        is CloudOperationResult.Success -> operation.message
                        is CloudOperationResult.Skipped -> operation.message
                        is CloudOperationResult.Failure -> operation.message
                    }
                }
            } catch (_: GetCredentialCancellationException) {
                message = "Google sign-in cancelled"
            } catch (_: NoCredentialException) {
                message = "No Google account is available on this device."
            } catch (_: GoogleIdTokenParsingException) {
                message = "Google couldn't verify the sign-in response. Try again."
            } catch (_: GetCredentialException) {
                message = "Google sign-in failed. Try again."
            } catch (_: Throwable) {
                message = "Google sign-in failed. Try again."
            } finally {
                busy = false
                reloadPoints()
            }
        }
    }

    fun signOut() {
        if (busy) return
        busy = true
        scope.launch {
            val operation = manager.signOut()
            runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
            message = when (operation) {
                is CloudOperationResult.Success -> operation.message
                is CloudOperationResult.Skipped -> operation.message
                is CloudOperationResult.Failure -> operation.message
            }
            busy = false
            reloadPoints()
        }
    }

    LaunchedEffect(Unit) { reloadPoints() }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Column(Modifier.weight(1f)) {
                    Text("CLOUD", color = RecRed, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text("Cloud Backup", color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Your phone stays the main copy.", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(
                "FrameByNavin works offline. Create a separate, private restore point when you choose. Automatic uploads and multi-device merging are not available in this release.",
                color = MutedText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(20.dp))

            if (state.session == null) {
                CloudCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                            Text("G", color = Color(0xFF202124), fontWeight = FontWeight.Black, fontSize = 17.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("FrameByNavin Account", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Google is only used to sign you in.", color = MutedText, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (CloudConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
                        Text(
                            "Google sign-in isn’t ready yet.",
                            color = MutedGold,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Button(
                        onClick = ::startGoogleSignIn,
                        enabled = CloudConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank() && !busy,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProjectorIvory, contentColor = CinemaBlack),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Text("CONTINUE WITH GOOGLE", fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            } else {
                val session = state.session!!
                CloudCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).background(RecRed.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Person, null, tint = RecRed, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(manager.cachedCreatorProfile()?.displayName.orEmpty().ifBlank { session.displayName }.ifBlank { "FrameByNavin" }, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(session.email, color = MutedText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("CONNECTED", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(Modifier.height(12.dp))
                if (state.settings.deletionPending) {
                    CloudCard {
                        Text("CLOUD DELETION NEEDS ATTENTION", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text("A previous deletion did not finish or could not be verified. Some remote records may already be gone. Automatic uploads are off. Your phone data and sign-in identity remain. Only this account can retry its deletion.", color = ProjectorIvory, fontSize = 14.sp, lineHeight = 20.sp)
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { confirmDelete = true },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        ) { Text("RETRY CLOUD DELETION", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { confirmAbandonDeletion = true }, enabled = !busy) {
                            Text("STOP RETRYING", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                } else if (state.settings.lifecyclePhase != "active") {
                    CloudCard {
                        Text("CLOUD HISTORY IS ${if (state.settings.lifecyclePhase == "deleted") "DELETED" else "UNVERIFIED"}", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text("Your phone remains the main copy. Cloud uploads are blocked until the server state is verified and you explicitly review the starting copy.", color = ProjectorIvory, fontSize = 14.sp, lineHeight = 20.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { runOperation { manager.refreshCloudStatus() } }, enabled = !busy) {
                            Text("REFRESH CLOUD STATUS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (state.settings.lifecyclePhase == "deleted") {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { confirmResume = true }, enabled = !busy) {
                                Text("START NEW EMPTY CLOUD HISTORY", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                } else if (state.settings.reconciliationRequired) {
                    CloudCard {
                        Text("CHOOSE YOUR STARTING COPY", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text("This account has existing backup history or has not yet been reviewed. Nothing has been uploaded from this phone. Restore a backup below, or keep this phone's data and create a separate new backup.", color = ProjectorIvory, fontSize = 14.sp, lineHeight = 20.sp)
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { confirmKeepLocal = true },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text("KEEP THIS PHONE'S DATA", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                CloudCard {
                    Text("MANUAL, APPEND-ONLY BACKUP", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(6.dp))
                    Text("Each backup creates a new restore point. Older backups are preserved. This does not merge edits between phones. Use one primary device until conflict-aware sync is available.", color = MutedText, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(12.dp))
                    CloudToggleRow(
                        title = "Wi-Fi only",
                        subtitle = "Require Wi-Fi for manual uploads",
                        checked = state.settings.wifiOnly,
                        onChecked = { manager.setWifiOnly(it); state = manager.localState() },
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("LAST UPLOAD", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(cloudTime(state.settings.lastSyncAtMillis), color = ProjectorIvory, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { runOperation { manager.syncNow(force = true) } },
                        enabled = !busy && !state.settings.reconciliationRequired && state.settings.lifecyclePhase == "active",
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        shape = RoundedCornerShape(13.dp),
                    ) { Text(if (busy) "WORKING…" else "BACK UP NOW", fontSize = 12.sp, fontWeight = FontWeight.Black) }
                    if (state.settings.reconciliationRequired) {
                        Spacer(Modifier.height(9.dp))
                        Text("Backup is locked until you restore a cloud copy or explicitly keep this phone's data. This prevents an empty reinstall from overwriting your recovery path.", color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                    if (state.settings.lastError.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(state.settings.lastError, color = MutedGold, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text("RESTORE POINTS", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                Spacer(Modifier.height(8.dp))
                when {
                    restorePointsLoading -> CloudCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MutedGold)
                            Spacer(Modifier.width(10.dp))
                            Text("Loading restore points…", color = MutedText, fontSize = 13.sp)
                        }
                    }
                    restorePointsError != null -> CloudCard {
                        Text("RESTORE HISTORY COULDN'T LOAD", color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(7.dp))
                        Text(restorePointsError.orEmpty(), color = ProjectorIvory, fontSize = 13.sp, lineHeight = 19.sp)
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = ::reloadPoints, enabled = !busy) { Text("RETRY") }
                    }
                    state.restorePoints.isEmpty() -> {
                        CloudCard { Text("No restore points exist for this account yet.", color = MutedText, fontSize = 13.sp) }
                    }
                    else -> {
                        val recommended = CloudRecoveryPolicy.recommended(state.restorePoints)
                        recommended?.let {
                            CloudCard {
                                Text("RECOMMENDED RECOVERY", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(5.dp))
                                Text("${it.projectCount} projects · ${it.ideaCount} ideas · ${it.snapshotDay}", color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(7.dp))
                                Text("Newest restore point that contains creator work.", color = MutedText, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        state.restorePoints.forEachIndexed { index, point ->
                            CloudRestoreRow(point = point, onClick = { if (!state.settings.deletionPending) restoreTarget = point })
                            if (index != state.restorePoints.lastIndex) Spacer(Modifier.height(7.dp))
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text("ACCOUNT & DATA", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                Spacer(Modifier.height(8.dp))
                CloudCard {
                    CloudActionRow(Icons.Outlined.Info, "Refresh cloud status", "Check the server before reviewing deletion or starting a new history") { runOperation { manager.refreshCloudStatus() } }
                    HorizontalDivider(color = CinemaLine, modifier = Modifier.padding(vertical = 9.dp))
                    CloudActionRow(Icons.Outlined.DeleteOutline, if (state.settings.deletionPending) "Retry cloud creator-data deletion" else "Delete cloud creator data", "Atomically deletes remote creator records and blocks old devices from recreating them; keeps your phone and sign-in account") { confirmDelete = true }
                    HorizontalDivider(color = CinemaLine, modifier = Modifier.padding(vertical = 9.dp))
                    CloudActionRow(Icons.Outlined.Logout, "Sign out", "Phone data stays; backup approval is cleared") { signOut() }
                }
            }

            message?.let {
                Spacer(Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(14.dp), color = CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Info, null, tint = MutedGold, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(9.dp))
                        Text(it, color = ProjectorIvory, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { message = null }, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Close, "Dismiss message", tint = MutedText, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }

    restoreTarget?.let { point ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Restore this backup?", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = { Text("This replaces the covered app data on this phone with the selected backup. A local recovery copy is retained first. Personal frame images and credentials are not included in older backups. Other phones are not automatically merged.", color = MutedText, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { restoreTarget = null; runOperation { manager.restore(point) } }) { Text("RESTORE", color = RecRed, fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { restoreTarget = null }) { Text("CANCEL", color = MutedText) } },
        )
    }

    if (confirmKeepLocal) {
        AlertDialog(
            onDismissRequest = { confirmKeepLocal = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Keep this phone's data?", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = { Text("Your existing cloud restore points will not be deleted or overwritten. You can then create a separate new backup of this phone. This is not a multi-device merge.", color = MutedText, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { confirmKeepLocal = false; runOperation { manager.keepLocalData() } }) { Text("KEEP LOCAL", color = RecRed, fontWeight = FontWeight.Black) } },
            dismissButton = { TextButton(onClick = { confirmKeepLocal = false }) { Text("CANCEL", color = MutedText) } },
        )
    }

    if (confirmAbandonDeletion) {
        AlertDialog(
            onDismissRequest = { confirmAbandonDeletion = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Stop retrying cloud deletion?", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = { Text("This does not undo already-deleted records or claim that deletion completed. After checking this account's backup history, the pending retry is cleared. Some remote records may remain. Uploads stay off until you explicitly review the starting copy again.", color = MutedText, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { confirmAbandonDeletion = false; runOperation { manager.abandonCloudDeletion() } }) { Text("STOP RETRYING", color = RecRed, fontWeight = FontWeight.Black) } },
            dismissButton = { TextButton(onClick = { confirmAbandonDeletion = false }) { Text("KEEP RETRY", color = MutedText) } },
        )
    }

    if (confirmResume) {
        AlertDialog(
            onDismissRequest = { confirmResume = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Start a new empty cloud history?", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = { Text("This explicitly reactivates cloud storage for this account. It does not recover deleted backups or merge another phone's edits. Your local data remains unchanged. Uploads stay off until you review the starting copy and create a manual backup. An old deletion request cannot be replayed into the new generation.", color = MutedText, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { confirmResume = false; runOperation { manager.resumeCloudData() } }) { Text("START NEW HISTORY", color = RecRed, fontWeight = FontWeight.Black) } },
            dismissButton = { TextButton(onClick = { confirmResume = false }) { Text("CANCEL", color = MutedText) } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text(if (state.settings.deletionPending) "Retry cloud deletion?" else "Delete cloud creator data?", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = { Text("This removes your remote creator backups, profile and device records. Already-deleted rows may be absent. Your phone data and sign-in account remain. Automatic uploads stay off. A failed or cancelled request keeps its retry record until you explicitly finish or stop it. Another device may create new backups, so this is not full account deletion.", color = MutedText, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; runOperation { manager.deleteCloudData() } }) { Text("DELETE CLOUD DATA", color = RecRed, fontWeight = FontWeight.Black) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("CANCEL", color = MutedText) } },
        )
    }
}

@Composable
private fun CloudCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CinemaSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
private fun CloudToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MutedText, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedThumbColor = ProjectorIvory, checkedTrackColor = RecRed),
        )
    }
}

@Composable
private fun CloudRestoreRow(point: CloudRestorePoint, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick),
        color = CinemaSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(MutedGold.copy(alpha = .10f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CloudDone, null, tint = MutedGold, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(cloudPointLabel(point), color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${point.projectCount} projects · ${point.ideaCount} ideas · ${point.activeReminderCount} active reminders", color = MutedText, fontSize = 12.sp)
            }
            Icon(Icons.Outlined.Restore, null, tint = MutedText, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CloudActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MutedText, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MutedText, fontSize = 12.sp)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(17.dp))
    }
}

private fun cloudPointLabel(point: CloudRestorePoint): String = when (point.kind) {
    "latest" -> "Latest · ${cloudTime(point.capturedAtMillis)}"
    "daily" -> "${point.snapshotDay.ifBlank { "Daily" }} · ${cloudTime(point.capturedAtMillis)}"
    else -> "Safety backup · ${cloudTime(point.capturedAtMillis)}"
}

private fun cloudTime(millis: Long): String = if (millis <= 0L) "Not backed up yet"
else SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()).format(Date(millis))
