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
internal fun V23AccountOnboarding(
    onComplete: (String) -> Unit,
    onContinueLocally: () -> Unit,
    onReturningAccount: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { CloudSyncManager(context.applicationContext) }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val credentialContext = remember(context) { MutableContextWrapper(context) }
    var session by remember { mutableStateOf(manager.localState().session) }
    var profile by remember { mutableStateOf(manager.cachedCreatorProfile()) }
    // In-flight credentials are not restorable after Activity recreation.
    // Never retain a permanently disabled Google button from a saved busy flag.
    var busy by remember { mutableStateOf(false) }
    var entryMode by rememberSaveable { mutableStateOf(UX11AuthEntryMode.CHOOSE) }
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
        onReturningAccount()
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


    // UX11-D changes presentation only. Both Google paths continue through
    // startGoogleSignIn -> CloudCreatorAccountPolicy, and ONLY real returned
    // identity/session state decides whether the user is new or returning.
    when (route) {
        CloudCreatorAccountRoute.SIGN_IN_REQUIRED -> UX11PremiumAccountEntry(
            mode = entryMode,
            busy = busy,
            error = error,
            onSelectSignUp = {
                if (!busy) { entryMode = UX11AuthEntryMode.SIGN_UP; error = null }
            },
            onSelectLogin = {
                if (!busy) { entryMode = UX11AuthEntryMode.LOG_IN; error = null }
            },
            onBack = {
                if (!busy) { entryMode = UX11AuthEntryMode.CHOOSE; error = null }
            },
            onGoogle = ::startGoogleSignIn,
            onContinueLocally = onContinueLocally,
        )
        CloudCreatorAccountRoute.CREATOR_ID_REQUIRED -> UX11PremiumAccountIdentity(
            displayName = displayName,
            username = username,
            busy = busy,
            error = error,
            onDisplayNameChange = { displayName = it.take(40) },
            onUsernameChange = {
                username = it.lowercase()
                    .filter { c -> c.isLetterOrDigit() || c == '_' }
                    .take(24)
            },
            onCreateAccount = ::claim,
        )
        CloudCreatorAccountRoute.RETURNING_CREATOR ->
            UX11PremiumAccountReturning(error)
    }
}
