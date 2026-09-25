package com.framebynavin.app.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorPersonalizationEngine
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWriteConflict
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.youtube.*
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backlot Insights V148 shell.
 *
 * Core analytics and deep audience/reach datasets intentionally have different loading lifecycles.
 * The selected range is considered loaded as soon as the core YouTube Analytics snapshot is
 * accepted. Foundation/reach then refresh independently and must never keep the header spinner or
 * "Updating" copy alive after the selected-range numbers are already visible.
 */
@Composable
internal fun V11InsightsScreen(
    creatorProfile: CreatorProfile,
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    onAdd: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val activity = context as? ComponentActivity
    val store = remember { YouTubeAnalyticsStore(appContext) }
    val api = remember { YouTubeApiClient() }
    val foundationApi = remember { YouTubeInsightsFoundationClient() }
    val foundationStore = remember { YouTubeInsightsFoundationStore(appContext) }
    val checkpointStore = remember { YouTubePublishCheckpointStore(appContext) }
    val reachStore = remember { YouTubeReachStore(appContext) }
    val reachApi = remember { YouTubeReachReportingClient(reachStore) }
    val authClient = remember(activity) { activity?.let { Identity.getAuthorizationClient(it) } }
    val scope = rememberCoroutineScope()

    var windowDays by rememberSaveable { mutableIntStateOf(28) }
    var snapshot by remember { mutableStateOf(store.load(windowDays) ?: store.loadAny()) }
    var coreSyncing by remember { mutableStateOf(false) }
    var revoking by remember { mutableStateOf(false) }
    var authError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVideo by remember { mutableStateOf<YouTubeVideoSnapshot?>(null) }
    var links by remember { mutableStateOf(store.links()) }
    var activeRequest by remember { mutableStateOf<YouTubeCacheRequest?>(null) }
    var activeRequestDays by remember { mutableIntStateOf(0) }
    var pendingResolution by remember { mutableStateOf<YouTubeCacheRequest?>(null) }
    var pendingResolutionDays by remember { mutableIntStateOf(28) }
    var autoRefreshKey by rememberSaveable { mutableStateOf("") }
    var foundationRevision by remember { mutableIntStateOf(0) }
    val personalization by remember(creatorProfile, tasks) {
        derivedStateOf { CreatorPersonalizationEngine.snapshot(creatorProfile, tasks) }
    }

    fun refreshCacheView() {
        // Prefer the exact selected range. loadAny() is only a continuity fallback so the screen
        // does not blank while an unseen range is fetched for the first time.
        snapshot = store.load(windowDays) ?: snapshot ?: store.loadAny()
        links = store.links()
        val request = activeRequest
        if (request != null && !store.isCurrent(request)) {
            activeRequest = null
            activeRequestDays = 0
            pendingResolution = null
            coreSyncing = false
            selectedVideo = null
        }
    }

    LaunchedEffect(windowDays) {
        refreshCacheView()
        authError = null
    }

    DisposableEffect(activity, store) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshCacheView()
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
            activeRequest?.let(store::cancelRequest)
        }
    }

    fun isActive(request: YouTubeCacheRequest, days: Int): Boolean =
        activeRequest == request &&
            activeRequestDays == days &&
            windowDays == days &&
            store.isCurrent(request)

    fun finishCoreRequest(request: YouTubeCacheRequest, error: String? = null) {
        if (activeRequest != request) return
        store.cancelRequest(request)
        activeRequest = null
        activeRequestDays = 0
        pendingResolution = null
        coreSyncing = false
        authError = error
    }

    fun launchDeepRefresh(token: String, fresh: YouTubeAnalyticsSnapshot) {
        scope.launch {
            val (foundationResult, reachResult) = withContext(Dispatchers.IO) {
                val foundationDeferred = async { runCatching { foundationApi.sync(token, fresh) } }
                val reachDeferred = async { runCatching { reachApi.sync(token, fresh.channel.channelId) } }
                foundationDeferred.await() to reachDeferred.await()
            }
            val foundation = foundationResult.getOrNull() ?: return@launch
            val reach = reachResult.getOrNull()
            val reachHealth = if (reach != null) {
                YouTubeDatasetHealth(YouTubeFoundationDataset.REACH, reach.state, reach.note)
            } else {
                YouTubeDatasetHealth(
                    YouTubeFoundationDataset.REACH,
                    YouTubeDatasetState.UNAVAILABLE,
                    "Reach could not be refreshed right now. Core Insights are up to date.",
                )
            }
            val withReach = foundation.copy(
                health = foundation.health.filterNot { it.dataset == YouTubeFoundationDataset.REACH } + reachHealth,
            )
            withContext(Dispatchers.IO) { foundationStore.save(withReach) }
            // Only nudge the currently visible channel/range. Saving the cache is safe regardless.
            if (snapshot?.channel?.channelId == fresh.channel.channelId && windowDays == fresh.windowDays) {
                foundationRevision += 1
            }
        }
    }

    fun syncWithToken(token: String, request: YouTubeCacheRequest, days: Int) {
        if (!isActive(request, days)) return
        scope.launch {
            coreSyncing = true
            authError = null
            try {
                val fresh = withContext(Dispatchers.IO) { api.sync(token, days) }
                if (!isActive(request, days)) return@launch
                val accepted = store.save(fresh, request)
                if (!accepted || !isActive(request, days)) return@launch

                snapshot = fresh
                links = store.links()
                selectedVideo = null
                withContext(Dispatchers.IO) { checkpointStore.captureFrom(fresh, store.links()) }

                // Phase-1 fix: selected-range loading ends HERE. Deep audience/reach refresh is
                // independent and no longer owns the header loading indicator.
                finishCoreRequest(request)
                launchDeepRefresh(token, fresh)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (activeRequest == request) {
                    if (error is YouTubeApiException && error.httpCode == 401) {
                        store.disconnect()
                        snapshot = null
                        selectedVideo = null
                    }
                    finishCoreRequest(request, ytFriendlyError(error))
                }
            } finally {
                if (activeRequest == request && activeRequestDays == days) {
                    finishCoreRequest(request)
                }
            }
        }
    }

    val resolutionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val request = pendingResolution
        val days = pendingResolutionDays
        pendingResolution = null
        if (request == null || !isActive(request, days)) return@rememberLauncherForActivityResult
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            finishCoreRequest(request, "YouTube connection was cancelled.")
            return@rememberLauncherForActivityResult
        }
        val authResult = runCatching { authClient?.getAuthorizationResultFromIntent(result.data!!) }.getOrNull()
        val token = authResult?.accessToken
        if (token.isNullOrBlank()) {
            finishCoreRequest(request, "Google did not return a YouTube access token.")
        } else {
            syncWithToken(token, request, days)
        }
    }

    fun authorize(selectAccount: Boolean = false, days: Int = windowDays, allowResolution: Boolean = true) {
        if (revoking) return
        val client = authClient ?: run {
            authError = "Google authorization is unavailable on this device."
            return
        }

        // A range switch supersedes the previous request. This prevents a slow 90D response from
        // replacing a newer 7D selection (or vice versa).
        activeRequest?.let(store::cancelRequest)
        val request = store.beginRequest(selectAccount)
        activeRequest = request
        activeRequestDays = days
        pendingResolution = null
        coreSyncing = true
        authError = null

        client.authorize(YouTubeAuthorization.request(selectAccount))
            .addOnSuccessListener { result ->
                if (!isActive(request, days)) return@addOnSuccessListener
                if (result.hasResolution()) {
                    if (!allowResolution) {
                        finishCoreRequest(request)
                        return@addOnSuccessListener
                    }
                    val pending = result.pendingIntent
                    if (pending == null) {
                        finishCoreRequest(request, "Google authorization needs attention, but no consent screen was available.")
                    } else {
                        pendingResolution = request
                        pendingResolutionDays = days
                        resolutionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                    }
                } else {
                    val token = result.accessToken
                    if (token.isNullOrBlank()) {
                        finishCoreRequest(request, "Google authorization completed without a YouTube access token.")
                    } else {
                        syncWithToken(token, request, days)
                    }
                }
            }
            .addOnFailureListener { error ->
                if (activeRequest == request) finishCoreRequest(request, ytFriendlyError(error))
            }
    }

    LaunchedEffect(snapshot?.fetchedAtMillis, windowDays, coreSyncing, revoking) {
        val cached = store.load(windowDays) ?: return@LaunchedEffect
        if (coreSyncing || revoking) return@LaunchedEffect
        val ageMillis = System.currentTimeMillis() - cached.fetchedAtMillis
        val refreshKey = "${cached.channel.channelId}:$windowDays:${cached.fetchedAtMillis}"
        if (ageMillis >= 15L * 60L * 1000L && autoRefreshKey != refreshKey) {
            autoRefreshKey = refreshKey
            authorize(selectAccount = false, days = windowDays, allowResolution = false)
        }
    }

    fun disconnect() {
        store.disconnect()
        foundationStore.clear()
        reachStore.clear()
        activeRequest = null
        activeRequestDays = 0
        pendingResolution = null
        coreSyncing = false
        revoking = true
        snapshot = null
        selectedVideo = null
        links = store.links()
        authError = null
        val request = RevokeAccessRequest.builder().setScopes(YouTubeAuthorization.scopes).build()
        val client = authClient
        if (client == null) {
            revoking = false
            authError = "Local analytics disconnected. Google access could not be revoked on this device."
        } else {
            try {
                client.revokeAccess(request).addOnCompleteListener { task ->
                    revoking = false
                    if (!task.isSuccessful) {
                        authError = "Local analytics disconnected. Google permission revocation could not be confirmed."
                    }
                }
            } catch (_: Throwable) {
                revoking = false
                authError = "Local analytics disconnected. Google permission revocation could not be confirmed."
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 124.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("BACKLOT", color = RecRed, fontSize = 8.3.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text("Insights", color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black)
                }
                Surface(onClick = onAdd, shape = CircleShape, color = CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine), modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Add, "Create project", tint = ProjectorIvory, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(personalization.insightTitle, color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
            Text(personalization.insightBody, color = MutedText, fontSize = 10.5.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(18.dp))

            val data = snapshot
            if (data == null) {
                YTConnectCard(
                    syncing = coreSyncing || revoking,
                    error = authError,
                    onConnect = { authorize(true) },
                )
            } else {
                YTChannelHeader(
                    data = data,
                    syncing = coreSyncing || revoking,
                    selectedWindowDays = windowDays,
                    onWindow = { days ->
                        if (days != windowDays) {
                            activeRequest?.let(store::cancelRequest)
                            activeRequest = null
                            activeRequestDays = 0
                            pendingResolution = null
                            coreSyncing = false
                            windowDays = days
                            authError = null
                            val cached = store.load(days)
                            if (cached != null) {
                                snapshot = cached
                            } else {
                                // Keep the previous snapshot visible for continuity while fetching
                                // the requested range; the header tells the user what is happening.
                                authorize(selectAccount = false, days = days, allowResolution = false)
                            }
                        }
                    },
                    onSync = { authorize(false, windowDays, allowResolution = true) },
                    onSwitchAccount = { authorize(true, windowDays, allowResolution = true) },
                    onDisconnect = ::disconnect,
                )

                authError?.let {
                    Spacer(Modifier.height(10.dp))
                    YTErrorCard(it)
                }

                Spacer(Modifier.height(14.dp))
                V172InsightsBody(
                    snapshot = data,
                    tasks = tasks,
                    ideas = ideas,
                    links = links,
                    foundationRevision = foundationRevision,
                    onLinkVideo = { selectedVideo = it },
                )
            }
        }
    }

    selectedVideo?.let { video ->
        YTLinkProjectDialog(
            video = video,
            tasks = tasks,
            currentTaskId = links[video.videoId],
            onDismiss = { selectedVideo = null },
            onLink = { taskId ->
                val epoch = CreatorDataGate.generation(appContext)
                scope.launch {
                    try {
                        store.link(video.videoId, taskId, epoch)
                        if (CreatorDataGate.generation(appContext) == epoch) {
                            links = store.links()
                            selectedVideo = null
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        Toast.makeText(context, error.message ?: "Could not save project link.", Toast.LENGTH_LONG).show()
                    }
                }
            },
        )
    }
}

@Composable
private fun YTConnectCard(syncing: Boolean, error: String?, onConnect: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(RecRed.copy(alpha = .12f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.SmartDisplay, null, tint = RecRed)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Connect YouTube", color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("Bring real channel performance into Backlot", color = MutedText, fontSize = 9.2.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Backlot requests read-only analytics. It cannot upload or delete your videos.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onConnect,
                enabled = !syncing,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                shape = RoundedCornerShape(15.dp),
            ) {
                if (syncing) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ProjectorIvory)
                else {
                    Icon(Icons.Outlined.Link, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("CONNECT YOUTUBE", fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
            error?.let { Spacer(Modifier.height(12.dp)); YTErrorCard(it) }
        }
    }
}

@Composable
private fun YTErrorCard(message: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color(0xFF17110F), border = BorderStroke(1.dp, RecRed.copy(alpha = .35f))) {
        Column(Modifier.padding(13.dp)) {
            Text("YOUTUBE CONNECTION", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(message, color = ProjectorIvory, fontSize = 9.5.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun YTChannelHeader(
    data: YouTubeAnalyticsSnapshot,
    syncing: Boolean,
    selectedWindowDays: Int,
    onWindow: (Int) -> Unit,
    onSync: () -> Unit,
    onSwitchAccount: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(RecRed.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.SmartDisplay, null, tint = RecRed, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(data.channel.title, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${ytCompact(data.channel.subscribers)} subscribers · ${data.channel.videoCount} videos", color = MutedText, fontSize = 8.8.sp)
                    when {
                        syncing -> Text("Refreshing ${selectedWindowDays}D…", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        data.windowDays != selectedWindowDays -> Text("Showing saved ${data.windowDays}D while ${selectedWindowDays}D loads", color = MutedGold, fontSize = 8.sp)
                    }
                }
                TextButton(onClick = onSync, enabled = !syncing) {
                    if (syncing) CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = RecRed)
                    else Text("REFRESH", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(7, 28, 90).forEach { days ->
                    FilterChip(
                        selected = selectedWindowDays == days,
                        onClick = { onWindow(days) },
                        label = { Text("${days}D", fontSize = 10.sp) },
                    )
                }
                Spacer(Modifier.weight(1f))
                Box {
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.MoreVert, "YouTube account options", tint = MutedText, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Switch account") }, onClick = { menu = false; onSwitchAccount() })
                        DropdownMenuItem(text = { Text("Disconnect") }, onClick = { menu = false; onDisconnect() })
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Text("${data.startDate} → ${data.endDate} · updated ${ytSyncTime(data.fetchedAtMillis)}", color = MutedText, fontSize = 8.sp)
        }
    }
}

@Composable
private fun YTLinkProjectDialog(
    video: YouTubeVideoSnapshot,
    tasks: List<CreatorTask>,
    currentTaskId: String?,
    onDismiss: () -> Unit,
    onLink: (String?) -> Unit,
) {
    val youtubeTasks = tasks.filter { it.platform.equals("YouTube", true) }
        .sortedWith(compareByDescending<CreatorTask> { it.status == TaskStatus.DONE }.thenByDescending { it.dueAtMillis })
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect project", fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState())) {
                Text(video.title, color = MutedText, fontSize = 9.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(12.dp))
                if (youtubeTasks.isEmpty()) {
                    Text("No YouTube projects exist yet. Create or finish a YouTube project first.")
                } else {
                    youtubeTasks.take(30).forEach { task ->
                        val selected = task.id == currentTaskId
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).clickable { onLink(task.id) },
                            shape = RoundedCornerShape(13.dp),
                            color = if (selected) MutedGold.copy(alpha = .12f) else CinemaSurface,
                            border = BorderStroke(1.dp, if (selected) MutedGold.copy(alpha = .5f) else CinemaLine),
                        ) {
                            Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selected, onClick = { onLink(task.id) })
                                Column(Modifier.weight(1f)) {
                                    Text(task.title, color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${task.contentType} · ${task.status.name.lowercase().replaceFirstChar { it.uppercase() }}", color = MutedText, fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (currentTaskId != null) TextButton(onClick = { onLink(null) }) { Text("UNLINK", color = RecRed) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } },
        containerColor = CinemaSurfaceRaised,
    )
}

private fun ytFriendlyError(error: Throwable): String {
    val raw = error.message.orEmpty()
    return when {
        error is CreatorWriteConflict -> raw.ifBlank { "The creator data changed. Review the latest state and retry." }
        error is YouTubeApiException && error.httpCode == 401 -> "Your YouTube connection expired or was revoked. Connect again."
        raw.contains("DEVELOPER_ERROR", true) || raw.contains("10:") -> "YouTube sign-in is not fully set up for this app yet."
        raw.contains("403") || raw.contains("accessNotConfigured", true) || raw.contains("has not been used", true) -> "YouTube connection is not fully enabled yet."
        raw.contains("401") || raw.contains("invalid credentials", true) -> "Your YouTube connection expired. Connect again."
        else -> "YouTube couldn't refresh right now. Check your connection and try again."
    }
}

private fun ytCompact(value: Long): String = when {
    value >= 1_000_000_000L -> String.format(Locale.US, "%.1fB", value / 1_000_000_000.0)
    value >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}

private fun ytSyncTime(millis: Long): String =
    if (millis <= 0L) "never" else SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()).format(Date(millis))
