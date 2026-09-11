package com.framebynavin.app.cloud

import android.accounts.Account
import android.app.Activity
import android.content.Intent
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
        setContent { FrameByNavinTheme { DriveVaultScreen(onClose = ::finish) } }
    }
}

@Composable
private fun DriveVaultScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountManager = remember { CloudSyncManager(context.applicationContext) }
    val vault = remember { DriveVaultManager(context.applicationContext) }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val credentialContext = remember(context) { MutableContextWrapper(context) }
    val authorizationClient = remember(context) { Identity.getAuthorizationClient(context) }

    var session by remember { mutableStateOf(accountManager.localState().session) }
    var token by remember(session?.email) { mutableStateOf(session?.email?.let(DriveVaultTokenMemory::get)) }
    var points by remember { mutableStateOf<List<DriveVaultRestorePoint>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var restoreTarget by remember { mutableStateOf<DriveVaultRestorePoint?>(null) }

    fun reloadIdentity() { session = accountManager.localState().session }

    fun loadPoints(accessToken: String) {
        loading = true
        scope.launch {
            runCatching { vault.list(accessToken) }
                .onSuccess { points = it; message = null }
                .onFailure { message = it.message ?: "Could not read this Google account's Drive vault." }
            loading = false
        }
    }

    val resolutionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching { authorizationClient.getAuthorizationResultFromIntent(result.data!!) }
                .onSuccess { auth ->
                    val value = auth.accessToken
                    if (value.isNullOrBlank()) message = "Google Drive did not return an access token."
                    else {
                        token = value
                        session?.email?.let { DriveVaultTokenMemory.put(it, value) }
                        loadPoints(value)
                    }
                }
                .onFailure { message = it.message ?: "Google Drive authorization failed." }
        } else message = "Google Drive authorization was cancelled."
    }

    fun authorizeDrive() {
        val account = session ?: return
        loading = true
        message = null
        val request = AuthorizationRequest.builder()
            .setAccount(Account(account.email, "com.google"))
            .setRequestedScopes(listOf(Scope(Scopes.DRIVE_APPFOLDER)))
            .build()
        authorizationClient.authorize(request)
            .addOnSuccessListener { auth ->
                loading = false
                if (auth.hasResolution()) {
                    val pending = auth.pendingIntent
                    if (pending == null) message = "Google Drive authorization needs attention."
                    else resolutionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                } else {
                    val value = auth.accessToken
                    if (value.isNullOrBlank()) message = "Google Drive permission was not granted."
                    else {
                        token = value
                        DriveVaultTokenMemory.put(account.email, value)
                        loadPoints(value)
                    }
                }
            }
            .addOnFailureListener { loading = false; message = it.message ?: "Google Drive authorization failed." }
    }

    fun startGoogleSignIn() {
        if (loading || CloudConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return
        loading = true
        message = null
        scope.launch {
            try {
                val option = GetSignInWithGoogleOption.Builder(CloudConfig.GOOGLE_WEB_CLIENT_ID).build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val result = credentialManager.getCredential(context = credentialContext, request = request)
                val c = result.credential
                val idToken = if (c is CustomCredential && c.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    GoogleIdTokenCredential.createFrom(c.data).idToken
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
                }
            } catch (_: GetCredentialCancellationException) { message = "Google sign-in cancelled" }
            catch (_: NoCredentialException) { message = "No Google account is available on this device." }
            catch (_: GoogleIdTokenParsingException) { message = "Google couldn't verify the sign-in response." }
            catch (_: GetCredentialException) { message = "Google sign-in failed." }
            catch (e: Throwable) { message = e.message ?: "Google sign-in failed." }
            finally { loading = false }
        }
    }

    LaunchedEffect(session?.email, token) { token?.let(::loadPoints) }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 32.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Column {
                    Text("GOOGLE ACCOUNT VAULT", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("Backup & Restore", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(18.dp))

            VaultCard {
                Icon(Icons.Outlined.AdminPanelSettings, null, tint = MutedGold, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(9.dp))
                Text("Your creator work belongs to your Google account.", color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                Text("Projects, ideas and creator setup are saved as private FrameByNavin snapshots in Google Drive's hidden app-data area. Supabase is no longer the backup store.", color = MutedText, fontSize = 13.sp, lineHeight = 19.sp)
            }
            Spacer(Modifier.height(12.dp))

            if (session == null) {
                VaultCard {
                    Text("Sign in first", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                    Text("The Google account you choose owns its own independent FrameByNavin vault.", color = MutedText, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = ::startGoogleSignIn, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("CONTINUE WITH GOOGLE") }
                }
            } else {
                VaultCard {
                    Text("ACCOUNT", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(session?.email.orEmpty(), color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    if (token == null) {
                        Button(onClick = ::authorizeDrive, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Cloud, null); Spacer(Modifier.width(7.dp)); Text("CONNECT PRIVATE DRIVE VAULT")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CloudDone, null, tint = Color(0xFF86C995))
                            Spacer(Modifier.width(7.dp))
                            Text("Private Drive vault connected for this session", color = Color(0xFF86C995), fontSize = 12.sp)
                        }
                    }
                }

                if (token != null) {
                    Spacer(Modifier.height(12.dp))
                    VaultCard {
                        Text("SAFE SNAPSHOT", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("Create a new restore point", color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text("Snapshots are append-only in this milestone. An empty phone cannot silently replace meaningful Drive history.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val access = token ?: return@Button
                                loading = true
                                scope.launch {
                                    runCatching { vault.backupNow(access) }
                                        .onSuccess { message = "Backup saved to this Google account."; points = vault.list(access) }
                                        .onFailure { message = it.message ?: "Drive backup failed." }
                                    loading = false
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (loading) "WORKING…" else "BACK UP NOW") }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("RESTORE POINTS", color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text(if (loading && points.isEmpty()) "Reading this account's private vault…" else "${points.size} snapshot${if (points.size == 1) "" else "s"}", color = MutedText, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    if (!loading && points.isEmpty()) {
                        VaultCard { Text("No FrameByNavin snapshots in this Google account yet.", color = MutedText, fontSize = 13.sp) }
                    } else {
                        val recommended = DriveVaultPolicy.recommended(points)
                        points.forEach { point ->
                            Surface(
                                onClick = { restoreTarget = point },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                color = CinemaSurface,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (point.fileId == recommended?.fileId) MutedGold.copy(alpha = .5f) else CinemaLine),
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    if (point.fileId == recommended?.fileId) Text("RECOMMENDED", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    Text(vaultTime(point.capturedAtMillis), color = ProjectorIvory, fontWeight = FontWeight.Bold)
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
            onDismissRequest = { if (!loading) restoreTarget = null },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Restore this Drive snapshot?", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = { Text("This replaces covered creator data on this phone. FrameByNavin first keeps a local recovery copy, validates the snapshot hash, then restores it. Other Drive snapshots stay untouched.", color = MutedText) },
            confirmButton = {
                TextButton(onClick = {
                    val access = token ?: return@TextButton
                    restoreTarget = null
                    loading = true
                    scope.launch {
                        runCatching { vault.restore(access, point) }
                            .onSuccess { message = "Workspace restored: ${it.projectCount} projects and ${it.ideaCount} ideas." }
                            .onFailure { message = it.message ?: "Restore failed. Local recovery data was retained." }
                        loading = false
                    }
                }) { Text("RESTORE", color = RecRed) }
            },
            dismissButton = { TextButton(onClick = { restoreTarget = null }) { Text("CANCEL", color = MutedText) } },
        )
    }
}

@Composable
private fun VaultCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = CinemaSurface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

private fun vaultTime(value: Long): String = if (value <= 0L) "Unknown time" else
    SimpleDateFormat("dd MMM yyyy · h:mm a", Locale.getDefault()).format(Date(value))
