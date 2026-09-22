package com.framebynavin.app.ui

import android.app.Activity
import android.content.Context
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
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Revenue consent belongs to the connected channel, not to a date range.
 * Once monetary access succeeds, every range may fetch with silent token refreshes.
 */
@Composable
internal fun V144YouTubeRevenueIntegration(snapshot: YouTubeAnalyticsSnapshot) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val activity = context as? ComponentActivity
    val channelId = snapshot.channel.channelId
    val ownerPrefs = remember {
        appContext.getSharedPreferences(REVENUE_OWNER_PREFS, Context.MODE_PRIVATE)
    }
    val revenueStore = remember(channelId) {
        val store = YouTubeRevenueStore(appContext)
        val previousChannel = ownerPrefs.getString(REVENUE_OWNER_CHANNEL, null)
        if (previousChannel != null && previousChannel != channelId) {
            store.clear()
            ownerPrefs.edit().remove(REVENUE_ACCESS_GRANTED).apply()
        }
        if (previousChannel != channelId) {
            ownerPrefs.edit().putString(REVENUE_OWNER_CHANNEL, channelId).apply()
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
    var pendingPeriodName by remember(channelId) { mutableStateOf<String?>(null) }
    var requestNonce by remember(channelId) { mutableLongStateOf(0L) }
    var revenueAccessGranted by remember(channelId) {
        val persisted = ownerPrefs.getBoolean(REVENUE_ACCESS_GRANTED, false)
        val migratedFromV144Cache = YouTubeRevenuePeriod.entries.any { revenueStore.load(it) != null }
        mutableStateOf(persisted || migratedFromV144Cache)
    }

    fun setRevenueAccess(granted: Boolean) {
        revenueAccessGranted = granted
        ownerPrefs.edit().putBoolean(REVENUE_ACCESS_GRANTED, granted).apply()
    }

    LaunchedEffect(channelId, revenueAccessGranted) {
        if (revenueAccessGranted && !ownerPrefs.getBoolean(REVENUE_ACCESS_GRANTED, false)) {
            ownerPrefs.edit().putBoolean(REVENUE_ACCESS_GRANTED, true).apply()
        }
    }

    // Disconnect owns both caches and the monetary-access marker. Ordinary navigation owns none.
    DisposableEffect(channelId, revenueStore) {
        onDispose {
            if (YouTubeAnalyticsStore(appContext).loadAny() == null) {
                revenueStore.clear()
                ownerPrefs.edit().clear().apply()
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
                if (requestNonce == nonce && selectedPeriodName == period.name) {
                    revenueSnapshot = fresh
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (requestNonce == nonce) {
                    if (error is YouTubeRevenueException && error.httpCode == 401) {
                        setRevenueAccess(false)
                    }
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
            revenueLoading = false
            revenueError = "YouTube revenue connection was cancelled. Normal Insights are unaffected."
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
            setRevenueAccess(true)
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
        revenueLoading = true
        if (allowResolution) revenueError = null
        pendingPeriodName = period.name
        client.authorize(YouTubeAuthorization.revenueRequest())
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    if (!allowResolution) {
                        pendingPeriodName = null
                        revenueLoading = false
                        setRevenueAccess(false)
                        revenueError = "Revenue permission needs reconnecting once."
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
                        setRevenueAccess(true)
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

    LaunchedEffect(channelId, selectedPeriodName, snapshot.fetchedAtMillis, revenueAccessGranted) {
        val cached = revenueStore.load(selectedPeriod)
        revenueSnapshot = cached
        revenueError = null
        if (!revenueAccessGranted || revenueLoading) return@LaunchedEffect

        val ageMillis = cached?.let { System.currentTimeMillis() - it.fetchedAtMillis }
        val needsFetch = cached == null || (ageMillis != null && ageMillis >= REVENUE_REFRESH_AGE_MS)
        if (needsFetch) {
            // Date-range changes use silent authorization. Never show consent for every period.
            authorizeRevenue(period = selectedPeriod, allowResolution = false)
        }
    }

    V144YouTubeRevenueCard(
        snapshot = revenueSnapshot,
        selectedPeriod = selectedPeriod,
        accessEnabled = revenueAccessGranted,
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
            "Revenue data is unavailable for this channel or needs read-only monetary permission."
        raw.contains("DEVELOPER_ERROR", ignoreCase = true) || raw.contains("10:") ->
            "YouTube revenue sign-in is not fully set up for this app yet."
        raw.contains("403") || raw.contains("permission", ignoreCase = true) ->
            "Reconnect once to grant read-only YouTube revenue access."
        else ->
            "YouTube revenue couldn't refresh right now. Your normal Insights are unaffected."
    }
}

private const val REVENUE_OWNER_PREFS = "backlot_youtube_revenue_owner"
private const val REVENUE_OWNER_CHANNEL = "channel_id"
private const val REVENUE_ACCESS_GRANTED = "monetary_access_granted"
private const val REVENUE_REFRESH_AGE_MS = 15L * 60L * 1000L
