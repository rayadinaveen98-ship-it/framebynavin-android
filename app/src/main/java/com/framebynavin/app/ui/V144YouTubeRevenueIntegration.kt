package com.framebynavin.app.ui

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.framebynavin.app.youtube.YouTubeAnalyticsSnapshot
import com.framebynavin.app.youtube.YouTubeAnalyticsStore
import com.framebynavin.app.youtube.YouTubeAuthorization
import com.framebynavin.app.youtube.YouTubeRevenueClient
import com.framebynavin.app.youtube.YouTubeRevenueException
import com.framebynavin.app.youtube.YouTubeRevenuePeriod
import com.framebynavin.app.youtube.YouTubeRevenueStore
import com.framebynavin.app.youtube.shouldAutoFetchRevenue
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Revenue consent belongs to the connected YouTube channel, not to a date range.
 * Period changes only swap data filters. They must never clear monetary access by themselves.
 */
@Composable
internal fun V144YouTubeRevenueIntegration(snapshot: YouTubeAnalyticsSnapshot) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val activity = context as? ComponentActivity
    val channelId = snapshot.channel.channelId
    val ownerPrefs = remember {
        appContext.getSharedPreferences(REVENUE_OWNER_PREFS, android.content.Context.MODE_PRIVATE)
    }
    val revenueStore = remember(channelId) {
        val store = YouTubeRevenueStore(appContext)
        val previousChannel = ownerPrefs.getString(REVENUE_OWNER_CHANNEL, null)
        if (previousChannel != null && previousChannel != channelId) {
            store.clear()
        }
        if (previousChannel != channelId) {
            ownerPrefs.edit()
                .putString(REVENUE_OWNER_CHANNEL, channelId)
                .remove(REVENUE_PERMISSION_ESTABLISHED)
                .apply()
        }
        store
    }
    val revenueApi = remember { YouTubeRevenueClient() }
    val authClient = remember(activity) { activity?.let { Identity.getAuthorizationClient(it) } }
    val scope = rememberCoroutineScope()

    var selectedPeriodName by rememberSaveable(channelId) {
        mutableStateOf(YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS.name)
    }
    val selectedPeriod = remember(selectedPeriodName) {
        runCatching { YouTubeRevenuePeriod.valueOf(selectedPeriodName) }
            .getOrDefault(YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS)
    }
    var revenueSnapshot by remember(channelId, selectedPeriodName) {
        mutableStateOf(revenueStore.load(selectedPeriod))
    }
    var revenueLoading by remember(channelId) { mutableStateOf(false) }
    var revenueError by rememberSaveable(channelId) { mutableStateOf<String?>(null) }
    var revenueAuthorized by remember(channelId) {
        mutableStateOf(
            ownerPrefs.getBoolean(REVENUE_PERMISSION_ESTABLISHED, false) || revenueStore.hasAnySnapshot(),
        )
    }
    var pendingPeriodName by remember(channelId) { mutableStateOf<String?>(null) }
    var requestNonce by remember(channelId) { mutableLongStateOf(0L) }

    fun markRevenueAuthorized(authorized: Boolean) {
        revenueAuthorized = authorized
        ownerPrefs.edit().putBoolean(REVENUE_PERMISSION_ESTABLISHED, authorized).apply()
    }

    LaunchedEffect(channelId) {
        if (revenueAuthorized && !ownerPrefs.getBoolean(REVENUE_PERMISSION_ESTABLISHED, false)) {
            ownerPrefs.edit().putBoolean(REVENUE_PERMISSION_ESTABLISHED, true).apply()
        }
    }

    DisposableEffect(channelId, revenueStore) {
        onDispose {
            if (YouTubeAnalyticsStore(appContext).loadAny() == null) {
                revenueStore.clear()
                ownerPrefs.edit()
                    .remove(REVENUE_PERMISSION_ESTABLISHED)
                    .remove(REVENUE_OWNER_CHANNEL)
                    .apply()
            }
        }
    }

    fun syncRevenueWithToken(token: String, period: YouTubeRevenuePeriod) {
        val nonce = requestNonce + 1L
        requestNonce = nonce
        scope.launch {
            revenueLoading = true
            revenueError = null
            try {
                val fresh = withContext(Dispatchers.IO) { revenueApi.sync(token, period) }
                withContext(Dispatchers.IO) { revenueStore.save(fresh) }
                markRevenueAuthorized(true)
                if (requestNonce == nonce && selectedPeriodName == period.name) {
                    revenueSnapshot = fresh
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                // Only an actual authentication failure revokes our persisted access marker.
                // 403 can also mean unavailable monetary data for this channel/range, so it must
                // not make every other period ask the creator to enable revenue again.
                if (error is YouTubeRevenueException && error.httpCode == 401) {
                    markRevenueAuthorized(false)
                }
                if (requestNonce == nonce) {
                    revenueError = v144RevenueFriendlyError(error)
                }
            } finally {
                if (requestNonce == nonce) revenueLoading = false
            }
        }
    }

    val revenueResolutionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val period = pendingPeriodName
            ?.let { runCatching { YouTubeRevenuePeriod.valueOf(it) }.getOrNull() }
            ?: selectedPeriod
        pendingPeriodName = null
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            // Cancelling a token refresh does not revoke already-established monetary consent.
            revenueLoading = false
            revenueError = "YouTube revenue refresh was cancelled. Your existing revenue access is unchanged."
            return@rememberLauncherForActivityResult
        }
        val authResult = runCatching {
            authClient?.getAuthorizationResultFromIntent(result.data!!)
        }.getOrNull()
        val token = authResult?.accessToken
        if (token.isNullOrBlank()) {
            revenueLoading = false
            revenueError = "Google did not return a YouTube revenue access token."
        } else {
            markRevenueAuthorized(true)
            syncRevenueWithToken(token, period)
        }
    }

    fun authorizeRevenue(
        period: YouTubeRevenuePeriod = selectedPeriod,
        allowResolution: Boolean = true,
    ) {
        val client = authClient ?: run {
            revenueError = "Google authorization is unavailable on this device."
            return
        }
        if (revenueLoading) return
        revenueLoading = true
        if (allowResolution) revenueError = null
        pendingPeriodName = period.name
        client.authorize(YouTubeAuthorization.revenueRequest())
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    if (!allowResolution) {
                        // Silent period switches are never allowed to turn monetary access off.
                        // Keep the channel-level grant and let the creator explicitly refresh only
                        // if Google needs a fresh interactive token.
                        pendingPeriodName = null
                        revenueLoading = false
                        revenueError = "Revenue access is still enabled. Tap refresh once if Google asks to reconnect."
                        return@addOnSuccessListener
                    }
                    val pending = result.pendingIntent
                    if (pending == null) {
                        pendingPeriodName = null
                        revenueLoading = false
                        revenueError = "Google needs revenue consent, but no consent screen was available."
                    } else {
                        revenueResolutionLauncher.launch(
                            IntentSenderRequest.Builder(pending.intentSender).build(),
                        )
                    }
                } else {
                    val token = result.accessToken
                    pendingPeriodName = null
                    if (token.isNullOrBlank()) {
                        revenueLoading = false
                        if (allowResolution) {
                            revenueError = "Google did not return a YouTube revenue access token."
                        }
                    } else {
                        markRevenueAuthorized(true)
                        syncRevenueWithToken(token, period)
                    }
                }
            }
            .addOnFailureListener { error ->
                pendingPeriodName = null
                revenueLoading = false
                if (allowResolution) revenueError = v144RevenueFriendlyError(error)
            }
    }

    LaunchedEffect(channelId, selectedPeriodName, snapshot.fetchedAtMillis, revenueAuthorized) {
        val cached = revenueStore.load(selectedPeriod)
        revenueSnapshot = cached
        revenueError = null
        if (
            shouldAutoFetchRevenue(
                permissionEstablished = revenueAuthorized,
                cached = cached,
                nowMillis = System.currentTimeMillis(),
                staleAfterMillis = REVENUE_REFRESH_AGE_MS,
            ) && !revenueLoading
        ) {
            // Missing/stale period data is fetched silently. Consent state survives every tab.
            authorizeRevenue(period = selectedPeriod, allowResolution = false)
        }
    }

    V144YouTubeRevenueCard(
        snapshot = revenueSnapshot,
        selectedPeriod = selectedPeriod,
        authorized = revenueAuthorized,
        loading = revenueLoading,
        error = revenueError,
        onPeriod = { period ->
            selectedPeriodName = period.name
            revenueError = null
        },
        onRefresh = { authorizeRevenue(selectedPeriod, allowResolution = true) },
    )
}

private fun v144RevenueFriendlyError(error: Throwable): String {
    val raw = error.message.orEmpty()
    return when {
        error is YouTubeRevenueException && error.httpCode == 401 ->
            "Your YouTube revenue permission expired. Reconnect revenue access."
        error is YouTubeRevenueException && error.httpCode == 403 ->
            "Revenue data is unavailable for this channel or range. Your existing revenue access stays enabled."
        raw.contains("DEVELOPER_ERROR", ignoreCase = true) || raw.contains("10:") ->
            "YouTube revenue sign-in is not fully set up for this app yet."
        raw.contains("403") || raw.contains("permission", ignoreCase = true) ->
            "YouTube revenue could not refresh this range. Your saved revenue access is unchanged."
        else ->
            "YouTube revenue couldn't refresh right now. Your normal Insights are unaffected."
    }
}

private const val REVENUE_OWNER_PREFS = "backlot_youtube_revenue_owner"
private const val REVENUE_OWNER_CHANNEL = "channel_id"
private const val REVENUE_PERMISSION_ESTABLISHED = "permission_established"
private const val REVENUE_REFRESH_AGE_MS = 15L * 60L * 1000L
