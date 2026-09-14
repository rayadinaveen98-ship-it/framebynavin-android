package com.framebynavin.app.cloud

import android.accounts.Account
import android.app.Activity
import android.content.MutableContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.Scopes
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
        setContent { FrameByNavinTheme { CreatorCloudScreen(onClose = ::finish) } }
    }
}

@Composable
private fun CreatorCloudScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountManager = remember { CloudSyncManager(context.applicationContext) }
    val cloud = remember { CreatorCloudSyncManager(context.applicationContext) }
    val drive = remember { DriveVaultManager(context.applicationContext) }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val credentialContext = remember(context) { MutableContextWrapper(context) }
    val authorizationClient = remember(context) { Identity.getAuthorizationClient(context) }

    var session by remember { mutableStateOf(accountManager.localState().session) }
    var syncResult by remember { mutableStateOf<CreatorCloudSyncResult?>(null) }
    var syncStatus by remember { mutableStateOf(cloud.status()) }
    var driveToken by remember(session?.email) { mutableStateOf(session?.email?.let(DriveVaultTokenMemory::get)) }
    var drivePoints by remember { mutableStateOf<List<DriveVaultRestorePoint>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var restoreTarget by remember { mutableStateOf<DriveVaultRestorePoint?>(null) }

    fun reloadIdentity() {
        session = accountManager.localState().session
        syncStatus = cloud.status()
    }

    fun describe(result: CreatorCloudSyncResult): String = when (result) {
        is CreatorCloudSyncResult.Synced -> result.message
        is CreatorCloudSyncResult.Restored -> "Cloud workspace restored: ${result.projectCount} projects and ${result.ideaCount} ideas."
        is CreatorCloudSyncResult.Conflict -> "This phone and cloud both changed. Choose which workspace to keep; nothing was overwritten."
        is CreatorCloudSyncResult.Skipped -> result.message
        is CreatorCloudSyncResult.Failure -> result.message
    }

    fun runCloudSync() {
        if (session == null || busy) return
        busy = true
        message = null
        scope.launch {
            val result = cloud.syncNow()
            syncResult = result
            syncStatus = cloud.status()
            message = describe(result)
            busy = false
        }
    }

    fun resolveCloud(restore: Boolean) {
        if (busy) return
        busy = true
        message = null
        scope.launch {
            val result = if (restore) cloud.restoreCloud() else cloud.keepThisPhone()
            syncResult = result
            syncStatus = cloud.status()
            message = describe(result)
            busy = false
        }
    }

    fun loadDrivePoints(accessToken: String) {
        busy = true
        scope.launch {
            runCatching { drive.list(accessToken) }
                .onSuccess { drivePoints = it; message = null }
                .onFailure { message = it.message ?: "Could not read the optional Google Drive copies." }
            busy = false
        }
    }

    val resolutionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching { authorizationClient.getAuthorizationResultFromIntent(result.data!!) }
                .onSuccess { auth ->
                    val value = auth.accessToken
                    if (value.isNullOrBlank()) message = "Google Drive did not return an access token."
                    else {
                        driveToken = value
                        session?.email?.let { DriveVaultTokenMemory.put(it, value) }
                        loadDrivePoints(value)
                    }
                }
                .onFailure { message = it.message ?: "Google Drive authorization failed." }
        } else message = "Google Drive authorization was cancelled."
    }

    fun authorizeDrive() {
        val account = session ?: return
        busy = true
        message = null
        val request = AuthorizationRequest.builder()
            .setAccount(Account(account.email, "com.google"))
            .setRequestedScopes(listOf(Scope(Scopes.DRIVE_APPFOLDER)))
            .build()
        authorizationClient.authorize(request)
            .addOnSuccessListener { auth ->
                busy = false
                if (auth.hasResolution()) {
                    val pending = auth.pendingIntent
                    if (pending == null) message = "Google Drive authorization needs attention."
                    else resolutionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                } else {
                    val value = auth.accessToken
                    if (value.isNullOrBlank()) message = "Google Drive permission was not granted."
                    else {
                        driveToken = value
                        DriveVaultTokenMemory.put(account.email, value)
                        loadDrivePoints(value)
                    }
                }
            }
            .addOnFailureListener { busy = false; message = it.message ?: "Google Drive authorization failed." }
    }

    fun startGoogleSignIn() {
        if (busy || CloudConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return
        busy = true
        message = null
        scope.launch {
            try {
                val option = GetSignInWithGoogleOption.Builder(CloudConfig.GOOGLE_WEB_CLIENT_ID).build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val result = credentialManager.getCredential(context = credentialContext, request = request)
                val credential = result.credential
                val idToken = if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    GoogleIdTokenCredential.createFrom(credential.data).idToken
                } else null
                if (idToken.isNullOrBlank()) message = "Google couldn't complete sign-in."
                else {
                    val operation = accountManager.completeGoogleSignIn(idToken)
                    message = when (operation) {
                        is CloudOperationResult.Success -> operation.message
                        is CloudOperationResult.Skipped -> operation.message
                        is CloudOperationResult.Failure -> operation.message
                    }
                    reloadIdentity()
                    if (operation is CloudOperationResult.Success) {
                        val cloudResult = cloud.syncNow()
                        syncResult = cloudResult
                        syncStatus = cloud.status()
                        message = describe(cloudResult)
                    }
                }
            } catch (_: GetCredentialCancellationException) { message = "Google sign-in cancelled" }
            catch (_: NoCredentialException) { message = "No Google account is available on this device." }
            catch (_: GoogleIdTokenParsingException) { message = "Google couldn't verify the sign-in response." }
            catch (_: GetCredentialException) { message = "Google sign-in failed." }
            catch (e: Throwable) { message = e.message ?: "Google sign-in failed." }
            finally { busy = false }
        }
    }

    LaunchedEffect(session?.userId) {
        if (session != null) {
            val result = cloud.syncNow()
            syncResult = result
            syncStatus = cloud.status()
        }
    }
    LaunchedEffect(session?.email, driveToken) { driveToken?.let(::loadDrivePoints) }

    val conflict = syncResult as? CreatorCloudSyncResult.Conflict

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 32.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Column {
                    Text("CREATOR CLOUD", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("Backup & Recovery", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(18.dp))

            CloudCard {
                Icon(Icons.Outlined.CloudDone, null, tint = MutedGold, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(9.dp))
                Text("Automatic creator backup", color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Your phone remains the working copy. When you connect Google, FrameByNavin privately syncs portable creator snapshots to your Supabase account and restores cloud changes only when it is safe.",
                    color = MutedText,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }
            Spacer(Modifier.height(12.dp))

            if (session == null) {
                CloudCard {
                    Text("Connect your creator account", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                    Text("Google is your identity. Supabase keeps the private automatic backup for that identity.", color = MutedText, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = ::startGoogleSignIn, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("CONTINUE WITH GOOGLE") }
                }
            } else {
                CloudCard {
                    Text("ACCOUNT", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(session?.email.orEmpty(), color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    val last = syncStatus.lastSuccessMillis
                    Text(
                        when {
                            conflict != null || syncStatus.needsResolution -> "Sync needs your choice"
                            last > 0L -> "Last automatic sync · ${cloudTime(last)} · revision ${syncStatus.revision}"
                            else -> "Automatic sync will start when the account is ready"
                        },
                        color = if (conflict != null || syncStatus.needsResolution) MutedGold else Color(0xFF86C995),
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = ::runCloudSync, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Sync, null); Spacer(Modifier.width(7.dp)); Text(if (busy) "SYNCING…" else "SYNC NOW")
                    }
                }

                if (conflict != null) {
                    Spacer(Modifier.height(12.dp))
                    CloudCard {
                        Text("SYNC CONFLICT", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("Nothing was overwritten", color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text("Phone: ${conflict.localProjectCount} projects · ${conflict.localIdeaCount} ideas", color = MutedText, fontSize = 12.sp)
                        Text("Cloud: ${conflict.cloudProjectCount} projects · ${conflict.cloudIdeaCount} ideas", color = MutedText, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { resolveCloud(true) }, enabled = !busy && conflict.cloudRevision > 0L, modifier = Modifier.fillMaxWidth()) {
                            Text("RESTORE CLOUD WORKSPACE")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { resolveCloud(false) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text("KEEP THIS PHONE'S WORK", color = ProjectorIvory)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("OPTIONAL MANUAL COPY", color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("Google Drive is no longer the automatic backup system. Use it only when you want an extra manual export/import restore point.", color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(8.dp))
                CloudCard {
                    if (driveToken == null) {
                        OutlinedButton(onClick = ::authorizeDrive, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.AddToDrive, null); Spacer(Modifier.width(7.dp)); Text("CONNECT DRIVE FOR IMPORT / EXPORT")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CloudDone, null, tint = Color(0xFF86C995))
                            Spacer(Modifier.width(7.dp))
                            Text("Optional Drive access connected for this session", color = Color(0xFF86C995), fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val access = driveToken ?: return@Button
                                busy = true
                                scope.launch {
                                    runCatching { drive.backupNow(access) }
                                        .onSuccess { message = "Manual Drive copy exported."; drivePoints = drive.list(access) }
                                        .onFailure { message = it.message ?: "Drive export failed." }
                                    busy = false
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("EXPORT MANUAL DRIVE COPY") }
                    }
                }

                if (driveToken != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("DRIVE IMPORT POINTS", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    if (!busy && drivePoints.isEmpty()) {
                        Text("No manual Drive copies found.", color = MutedText, fontSize = 12.sp)
                    } else {
                        drivePoints.forEach { point ->
                            Surface(
                                onClick = { restoreTarget = point },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                color = CinemaSurface,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, CinemaLine),
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(cloudTime(point.capturedAtMillis), color = ProjectorIvory, fontWeight = FontWeight.Bold)
                                    Text("${point.projectCount} projects · ${point.ideaCount} ideas · ${point.appVersion}", color = MutedText, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            message?.let {
                Spacer(Modifier.height(14.dp))
                Surface(Modifier.fillMaxWidth(), color = CinemaSurfaceRaised, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, CinemaLine)) {
                    Text(it, color = ProjectorIvory, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(13.dp))
                }
            }
        }
    }

    restoreTarget?.let { point ->
        AlertDialog(
            onDismissRequest = { if (!busy) restoreTarget = null },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Import this Drive copy?", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = { Text("This is a manual import. FrameByNavin validates the copy and keeps a local recovery journal before replacing covered creator data. Automatic creator cloud will reconcile afterward.", color = MutedText) },
            confirmButton = {
                TextButton(onClick = {
                    val access = driveToken ?: return@TextButton
                    restoreTarget = null
                    busy = true
                    scope.launch {
                        runCatching { drive.restore(access, point) }
                            .onSuccess {
                                message = "Manual Drive copy imported: ${it.projectCount} projects and ${it.ideaCount} ideas."
                                val cloudResult = cloud.syncNow()
                                syncResult = cloudResult
                                syncStatus = cloud.status()
                            }
                            .onFailure { message = it.message ?: "Drive import failed. Local recovery data was retained." }
                        busy = false
                    }
                }) { Text("IMPORT", color = RecRed) }
            },
            dismissButton = { TextButton(onClick = { restoreTarget = null }) { Text("CANCEL", color = MutedText) } },
        )
    }
}

@Composable
private fun CloudCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = CinemaSurface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

private fun cloudTime(value: Long): String = if (value <= 0L) "Unknown time" else
    SimpleDateFormat("dd MMM yyyy · h:mm a", Locale.getDefault()).format(Date(value))
