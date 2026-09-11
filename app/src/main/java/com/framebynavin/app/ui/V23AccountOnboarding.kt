package com.framebynavin.app.ui

import android.content.MutableContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.cloud.*
import com.framebynavin.app.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

@Composable
internal fun V23AccountOnboarding(onComplete: (String) -> Unit, onContinueLocally: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { CloudSyncManager(context.applicationContext) }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val credentialContext = remember(context) { MutableContextWrapper(context) }
    var session by remember { mutableStateOf(manager.localState().session) }
    var profile by remember { mutableStateOf(manager.cachedCreatorProfile()) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var displayName by rememberSaveable { mutableStateOf(session?.displayName.orEmpty()) }
    var username by rememberSaveable { mutableStateOf(profile?.username.orEmpty()) }

    fun reload() {
        session = manager.localState().session
        profile = manager.cachedCreatorProfile()
        if (displayName.isBlank()) displayName = profile?.displayName.orEmpty().ifBlank { session?.displayName.orEmpty() }
        if (username.isBlank()) username = profile?.username.orEmpty()
    }

    fun finishReturning(): Boolean {
        val s = manager.localState().session
        val p = manager.cachedCreatorProfile()
        if (CloudCreatorAccountPolicy.route(s, p) != CloudCreatorAccountRoute.RETURNING_CREATOR) return false
        onComplete(p!!.displayName.ifBlank { s!!.displayName })
        return true
    }

    fun startGoogleSignIn() {
        if (busy || CloudConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return
        busy = true; error = null
        scope.launch {
            try {
                val option = GetSignInWithGoogleOption.Builder(CloudConfig.GOOGLE_WEB_CLIENT_ID).build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val result = credentialManager.getCredential(context = credentialContext, request = request)
                val c = result.credential
                val token = if (c is CustomCredential && c.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                    GoogleIdTokenCredential.createFrom(c.data).idToken else null
                if (token.isNullOrBlank()) error = "Google couldn't complete sign-in."
                else when (val op = manager.completeGoogleSignIn(token)) {
                    is CloudOperationResult.Success -> {
                        reload()
                        if (!finishReturning()) {
                            manager.refreshCreatorIdentity(); reload(); finishReturning()
                        }
                    }
                    is CloudOperationResult.Skipped -> error = op.message
                    is CloudOperationResult.Failure -> error = op.message
                }
            } catch (_: GetCredentialCancellationException) { error = "Google sign-in cancelled" }
            catch (_: NoCredentialException) { error = "No Google account is available on this device." }
            catch (_: GoogleIdTokenParsingException) { error = "Google couldn't verify the sign-in response." }
            catch (_: GetCredentialException) { error = "Google sign-in failed." }
            catch (e: Throwable) { error = e.message ?: "Google sign-in failed." }
            finally { busy = false }
        }
    }

    fun claim() {
        if (busy) return
        busy = true; error = null
        scope.launch {
            when (val op = manager.claimUsername(username, displayName)) {
                is CloudOperationResult.Success -> { reload(); busy = false; onComplete(displayName.ifBlank { session?.displayName.orEmpty() }) }
                is CloudOperationResult.Skipped -> { busy = false; error = op.message }
                is CloudOperationResult.Failure -> { busy = false; error = op.message }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (session != null && !finishReturning()) { manager.refreshCreatorIdentity(); reload(); finishReturning() }
    }
    val route = CloudCreatorAccountPolicy.route(session, profile)

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(22.dp)) {
            Text("FRAMEBYNAVIN", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(34.dp))
            when (route) {
                CloudCreatorAccountRoute.SIGN_IN_REQUIRED -> {
                    Surface(Modifier.size(58.dp), RoundedCornerShape(18.dp), RecRed.copy(alpha=.13f)) { Box(contentAlignment=Alignment.Center) { Icon(Icons.Outlined.PersonOutline, null, tint=RecRed) } }
                    Spacer(Modifier.height(18.dp))
                    Text("Your creator identity starts here.", color=ProjectorIvory, fontSize=31.sp, lineHeight=35.sp, fontWeight=FontWeight.Black)
                    Text("Sign in with Google. Each Google account owns its own private FrameByNavin workspace vault.", color=MutedText, fontSize=12.sp, lineHeight=18.sp)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick=::startGoogleSignIn, enabled=!busy, modifier=Modifier.fillMaxWidth().height(54.dp), colors=ButtonDefaults.buttonColors(containerColor=ProjectorIvory, contentColor=CinemaBlack)) {
                        Text(if (busy) "CONNECTING…" else "CONTINUE WITH GOOGLE", fontWeight=FontWeight.Black)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick=onContinueLocally, modifier=Modifier.fillMaxWidth().height(48.dp), border=BorderStroke(1.dp, CinemaLine)) { Text("USE LOCALLY FOR NOW", color=MutedText) }
                }
                CloudCreatorAccountRoute.RETURNING_CREATOR -> {
                    Surface(Modifier.size(58.dp), CircleShape, MutedGold.copy(alpha=.13f)) { Box(contentAlignment=Alignment.Center) { Icon(Icons.Outlined.CloudDone, null, tint=MutedGold) } }
                    Spacer(Modifier.height(18.dp)); Text("Welcome back.", color=ProjectorIvory, fontSize=31.sp, fontWeight=FontWeight.Black)
                    Text("Creator ID found. Next we'll check this Google account's private Drive vault before deciding whether setup is needed.", color=MutedText, fontSize=12.sp, lineHeight=18.sp)
                    Spacer(Modifier.height(18.dp)); CircularProgressIndicator(Modifier.size(20.dp), color=MutedGold, strokeWidth=2.dp)
                }
                CloudCreatorAccountRoute.CREATOR_ID_REQUIRED -> {
                    Text("Create your Creator ID.", color=ProjectorIvory, fontSize=31.sp, fontWeight=FontWeight.Black)
                    Text("This public-facing identifier is the only creator record kept in Supabase. Your projects and private workspace will live in your Google account vault.", color=MutedText, fontSize=12.sp, lineHeight=18.sp)
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(value=displayName, onValueChange={ displayName=it.take(40) }, modifier=Modifier.fillMaxWidth(), label={Text("Display name")}, singleLine=true)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value=username, onValueChange={ username=it.lowercase().filter { c -> c.isLetterOrDigit() || c=='_' }.take(24) }, modifier=Modifier.fillMaxWidth(), prefix={Text("@")}, label={Text("Username")}, supportingText={Text("3–24 letters, numbers or underscore")}, singleLine=true)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick=::claim, enabled=!busy && username.length in 3..24, modifier=Modifier.fillMaxWidth().height(54.dp), colors=ButtonDefaults.buttonColors(containerColor=RecRed)) { Text("CREATE CREATOR ID", fontWeight=FontWeight.Black) }
                }
            }
            error?.let { Spacer(Modifier.height(14.dp)); Text(it, color=ProjectorIvory, fontSize=12.sp) }
        }
    }
}
