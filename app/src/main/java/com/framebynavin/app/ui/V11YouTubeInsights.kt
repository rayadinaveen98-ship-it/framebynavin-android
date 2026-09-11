package com.framebynavin.app.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import androidx.compose.material.icons.outlined.*
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
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.youtube.*
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun V11InsightsScreen(
    creatorProfile: CreatorProfile,
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    onAdd: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val store = remember { YouTubeAnalyticsStore(context.applicationContext) }
    val api = remember { YouTubeApiClient() }
    val foundationApi = remember { YouTubeInsightsFoundationClient() }
    val foundationStore = remember { YouTubeInsightsFoundationStore(context.applicationContext) }
    val checkpointStore = remember { YouTubePublishCheckpointStore(context.applicationContext) }
    val reachStore = remember { YouTubeReachStore(context.applicationContext) }
    val reachApi = remember { YouTubeReachReportingClient(reachStore) }
    val authClient = remember(activity) { activity?.let { Identity.getAuthorizationClient(it) } }
    val scope = rememberCoroutineScope()

    var windowDays by rememberSaveable { mutableIntStateOf(28) }
    var snapshot by remember { mutableStateOf(store.load(windowDays) ?: store.loadAny()) }
    var syncing by remember { mutableStateOf(false) }
    var revoking by remember { mutableStateOf(false) }
    var authError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVideo by remember { mutableStateOf<YouTubeVideoSnapshot?>(null) }
    var links by remember { mutableStateOf(store.links()) }
    var activeRequest by remember { mutableStateOf<YouTubeCacheRequest?>(null) }
    var pendingResolution by remember { mutableStateOf<YouTubeCacheRequest?>(null) }
    var pendingResolutionDays by remember { mutableIntStateOf(28) }
    val personalization by remember(creatorProfile) {
        derivedStateOf { CreatorPersonalizationEngine.snapshot(creatorProfile, tasks) }
    }

    fun refreshCacheView() {
        snapshot = store.load(windowDays) ?: store.loadAny()
        links = store.links()
        val request = activeRequest
        if (request != null && !store.isCurrent(request)) {
            activeRequest = null
            pendingResolution = null
            syncing = false
            selectedVideo = null
        }
    }

    LaunchedEffect(windowDays) {
        refreshCacheView()
        authError = null
    }

    // A restore may happen from another screen. Re-read the cache on return.
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

    fun isActive(request: YouTubeCacheRequest): Boolean =
        activeRequest == request && store.isCurrent(request)

    fun finishRequest(request: YouTubeCacheRequest, error: String? = null) {
        if (!isActive(request)) return
        store.cancelRequest(request)
        activeRequest = null
        pendingResolution = null
        syncing = false
        authError = error
    }

    fun syncWithToken(token: String, request: YouTubeCacheRequest, days: Int) {
        if (!isActive(request)) return
        scope.launch {
            syncing = true
            authError = null
            try {
                val (fresh, foundation) = withContext(Dispatchers.IO) {
                    val base = api.sync(token, days)
                    base to foundationApi.sync(token, base)
                }
                if (store.save(fresh, request) && isActive(request)) {
                    val reach = withContext(Dispatchers.IO) {
                        reachApi.sync(token, fresh.channel.channelId)
                    }
                    val withReach = foundation.copy(
                        health = foundation.health
                            .filterNot { it.dataset == YouTubeFoundationDataset.REACH } +
                            YouTubeDatasetHealth(YouTubeFoundationDataset.REACH, reach.state, reach.note)
                    )
                    foundationStore.save(withReach)
                    checkpointStore.captureFrom(fresh, store.links())
                    snapshot = fresh
                    links = store.links()
                    selectedVideo = null
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (isActive(request)) {
                    // A revoked/invalid credential must not leave an apparently connected cache.
                    if (error is YouTubeApiException && error.httpCode == 401) {
                        store.disconnect()
                        snapshot = null
                        selectedVideo = null
                    }
                    authError = ytFriendlyError(error)
                }
            } finally {
                if (activeRequest == request) {
                    activeRequest = null
                    pendingResolution = null
                    syncing = false
                }
            }
        }
    }

    val resolutionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val request = pendingResolution
        pendingResolution = null
        if (request == null || !isActive(request)) return@rememberLauncherForActivityResult
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            finishRequest(request, "YouTube connection was cancelled.")
            return@rememberLauncherForActivityResult
        }
        val authResult = runCatching { authClient?.getAuthorizationResultFromIntent(result.data!!) }.getOrNull()
        val token = authResult?.accessToken
        if (token.isNullOrBlank()) {
            finishRequest(request, "Google did not return a YouTube access token.")
        } else {
            syncWithToken(token, request, pendingResolutionDays)
        }
    }

    fun authorize(selectAccount: Boolean = false, days: Int = windowDays) {
        if (revoking) return
        val client = authClient ?: run {
            authError = "Google authorization is unavailable on this device."
            return
        }
        val request = store.beginRequest(selectAccount)
        activeRequest = request
        pendingResolution = null
        syncing = true
        authError = null
        client.authorize(YouTubeAuthorization.request(selectAccount))
            .addOnSuccessListener { result ->
                if (!isActive(request)) return@addOnSuccessListener
                if (result.hasResolution()) {
                    val pending = result.pendingIntent
                    if (pending == null) {
                        finishRequest(request, "Google authorization needs attention, but no consent screen was available.")
                    } else {
                        pendingResolution = request
                        pendingResolutionDays = days
                        resolutionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                    }
                } else {
                    val token = result.accessToken
                    if (token.isNullOrBlank()) {
                        finishRequest(request, "Google authorization completed without a YouTube access token.")
                    } else {
                        syncWithToken(token, request, days)
                    }
                }
            }
            .addOnFailureListener { error ->
                if (isActive(request)) finishRequest(request, ytFriendlyError(error))
            }
    }

    fun disconnect() {
        // Local invalidation must not wait for the network or Google Play services.
        store.disconnect()
        foundationStore.clear()
        reachStore.clear()
        activeRequest = null
        pendingResolution = null
        syncing = false
        revoking = true
        snapshot = null
        selectedVideo = null
        links = store.links() // Creator-owned links are deliberately preserved.
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
            } catch (error: Throwable) {
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
                    Text("FRAMEBYNAVIN", color = RecRed, fontSize = 8.3.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text("Insights", color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black)
                }
                Surface(onClick = onAdd, shape = CircleShape, color = CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine), modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, "Create project", tint = ProjectorIvory, modifier = Modifier.size(20.dp)) }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(personalization.insightTitle, color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
            Text(personalization.insightBody, color = MutedText, fontSize = 10.5.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(14.dp))
            YTProfileFocusCard(creatorProfile, personalization)
            Spacer(Modifier.height(18.dp))

            if (snapshot == null) {
                YTConnectCard(
                    syncing = syncing || revoking,
                    error = authError,
                    packageName = context.packageName,
                    sha1 = remember { YouTubeAuthorization.signingSha1(context) },
                    onConnect = { authorize(true) },
                )
                Spacer(Modifier.height(18.dp))
                YTLocalCreatorSection(tasks, ideas, personalization)
            } else {
                val data = snapshot!!
                YTChannelHeader(
                    data = data,
                    syncing = syncing || revoking,
                    windowDays = data.windowDays,
                    onWindow = { days ->
                        if (days != windowDays) {
                            windowDays = days
                            val cached = store.load(days)
                            if (cached != null) snapshot = cached else authorize(false, days)
                        }
                    },
                    onSync = { authorize(false, windowDays) },
                    onSwitchAccount = { authorize(true, windowDays) },
                    onDisconnect = ::disconnect,
                )

                authError?.let {
                    Spacer(Modifier.height(10.dp))
                    YTErrorCard(it, context.packageName, remember { YouTubeAuthorization.signingSha1(context) })
                }

                Spacer(Modifier.height(14.dp))
                V172InsightsBody(
                    snapshot = data,
                    tasks = tasks,
                    ideas = ideas,
                    links = links,
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
                val epoch = CreatorDataGate.generation(context.applicationContext)
                scope.launch {
                    try {
                        store.link(video.videoId, taskId, epoch)
                        if (CreatorDataGate.generation(context.applicationContext) == epoch) {
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
private fun YTProfileFocusCard(
    profile: CreatorProfile,
    personalization: CreatorPersonalizationSnapshot,
) {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(19.dp),
        Color(0xFF171310),
        border = BorderStroke(1.dp, MutedGold.copy(alpha = .28f)),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("YOUR CREATOR LENS", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(profile.primaryGoal, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "${personalization.publishedThisWeek} / ${personalization.weeklyTarget}",
                    color = if (personalization.weeklyProgress >= 1f) SuccessGreen else MutedGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(
                progress = { personalization.weeklyProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (personalization.weeklyProgress >= 1f) SuccessGreen else MutedGold,
                trackColor = CinemaLine,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "${profile.category} · ${personalization.platformSummary} · ${personalization.publishedThisWeek} / ${personalization.weeklyTarget} published this week",
                color = MutedText,
                fontSize = 8.7.sp,
                lineHeight = 12.sp,
            )
        }
    }
}

@Composable
private fun YTConnectCard(
    syncing: Boolean,
    error: String?,
    packageName: String,
    sha1: String,
    onConnect: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(RecRed.copy(alpha = .12f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.SmartDisplay, null, tint = RecRed)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Connect YouTube", color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("See your channel performance", color = MutedText, fontSize = 9.2.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Bring views, watch time, subscribers and video performance into FrameByNavin. It cannot upload or delete your videos.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onConnect, enabled = !syncing, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(15.dp)) {
                if (syncing) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ProjectorIvory)
                else { Icon(Icons.Outlined.Link, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("CONNECT YOUTUBE", fontWeight = FontWeight.Black, fontSize = 10.sp) }
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                YTErrorCard(it, packageName, sha1)
            }
        }
    }
}

@Composable
private fun YTErrorCard(message: String, packageName: String, sha1: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color(0xFF17110F), border = BorderStroke(1.dp, RecRed.copy(alpha = .35f))) {
        Column(Modifier.padding(13.dp)) {
            Text("YOUTUBE CONNECTION", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(message, color = ProjectorIvory, fontSize = 9.5.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text("Try connecting again. If it still fails, the app setup may need attention.", color = MutedText, fontSize = 8.2.sp)
        }
    }
}

@Composable
private fun YTChannelHeader(
    data: YouTubeAnalyticsSnapshot,
    syncing: Boolean,
    windowDays: Int,
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
                }
                TextButton(onClick = onSync, enabled = !syncing) {
                    if (syncing) CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = RecRed)
                    else Text("REFRESH", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(7, 28, 90).forEach { days ->
                    FilterChip(selected = windowDays == days, onClick = { onWindow(days) }, label = { Text("${days}D", fontSize = 10.sp) })
                }
                Spacer(Modifier.weight(1f))
                Box {
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.MoreVert, "YouTube account options", tint = MutedText, modifier = Modifier.size(20.dp)) }
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
private fun YTMetrics(data: YouTubeAnalyticsSnapshot) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            YTMetric("VIEWS", ytCompact(data.views), RecRed, Modifier.weight(1f))
            YTMetric("WATCH TIME", ytWatch(data.watchMinutes), MutedGold, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            YTMetric("NET SUBS", ytSigned(data.netSubscribers), if (data.netSubscribers >= 0) SuccessGreen else RecRed, Modifier.weight(1f))
            YTMetric("AVG VIEW", ytDuration(data.averageViewDurationSeconds), ProjectorIvory, Modifier.weight(1f))
        }
    }
}

@Composable
private fun YTMetric(label: String, value: String, accent: Color, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = MutedText, fontSize = 7.8.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
            Spacer(Modifier.height(5.dp))
            Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun YTSignalCard(data: YouTubeAnalyticsSnapshot) {
    val best = data.topVideos.maxByOrNull { it.periodViews }
    val avgTop = data.topVideos.map { it.periodViews }.filter { it > 0 }.average().takeIf { !it.isNaN() } ?: 0.0
    val bestSignal = when {
        best == null -> "Sync again after YouTube has enough report data."
        avgTop > 0 && best.periodViews >= avgTop * 1.5 -> "${best.title} is clearly leading this ${data.windowDays}-day window."
        else -> "Your top videos are relatively close together in this window."
    }
    val subscriberSignal = when {
        data.netSubscribers > 0 -> "Subscriber momentum is positive at ${ytSigned(data.netSubscribers)} net."
        data.netSubscribers < 0 -> "Subscriber movement is negative at ${data.netSubscribers}; check which uploads are losing viewers."
        else -> "Subscriber movement is flat in this window."
    }
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), Color(0xFF15130F), border = BorderStroke(1.dp, MutedGold.copy(alpha = .35f))) {
        Column(Modifier.padding(16.dp)) {
            Text("WHAT TO WATCH", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            Spacer(Modifier.height(7.dp))
            Text(bestSignal, color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text(subscriberSignal, color = MutedText, fontSize = 9.2.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun YTTrendCard(data: YouTubeAnalyticsSnapshot) {
    val points = data.trend.takeLast(14)
    val max = points.maxOfOrNull { it.views }?.coerceAtLeast(1L) ?: 1L
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp)) {
            Text("VIEW TREND", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text("Last ${points.size} reported days", color = MutedText, fontSize = 8.5.sp)
            Spacer(Modifier.height(14.dp))
            if (points.isEmpty()) {
                Text("No daily trend returned yet.", color = MutedText, fontSize = 9.5.sp)
            } else {
                Row(Modifier.fillMaxWidth().height(58.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                    points.forEach { point ->
                        val ratio = point.views.toFloat() / max.toFloat()
                        Box(Modifier.weight(1f).height((7f + ratio * 49f).dp).background(if (point.views == max) RecRed else MutedGold.copy(alpha = .55f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                    }
                }
            }
        }
    }
}

@Composable
private fun YTTopVideos(data: YouTubeAnalyticsSnapshot, tasks: List<CreatorTask>, links: Map<String, String>, onVideo: (YouTubeVideoSnapshot) -> Unit) {
    Text("TOP VIDEOS", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
    Text("Performance inside the selected ${data.windowDays}-day window.", color = MutedText, fontSize = 9.sp)
    Spacer(Modifier.height(9.dp))
    if (data.topVideos.isEmpty()) {
        YTEmpty("No video performance data yet.")
        return
    }
    data.topVideos.take(6).forEachIndexed { index, video ->
        val linked = tasks.firstOrNull { it.id == links[video.videoId] }
        YTVideoRow(video, "#${index + 1}", linked, onVideo)
    }
}

@Composable
private fun YTRecentVideos(data: YouTubeAnalyticsSnapshot, tasks: List<CreatorTask>, links: Map<String, String>, onVideo: (YouTubeVideoSnapshot) -> Unit) {
    Text("RECENT UPLOADS", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
    Text("Tap a video to connect it to the project that made it.", color = MutedText, fontSize = 9.sp)
    Spacer(Modifier.height(9.dp))
    data.recentVideos.take(10).forEach { video ->
        val linked = tasks.firstOrNull { it.id == links[video.videoId] }
        YTVideoRow(video, ytCompact(video.lifetimeViews), linked, onVideo)
    }
}

@Composable
private fun YTVideoRow(video: YouTubeVideoSnapshot, lead: String, linked: CreatorTask?, onVideo: (YouTubeVideoSnapshot) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp).clickable { onVideo(video) },
        shape = RoundedCornerShape(17.dp), color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(RecRed.copy(alpha = .10f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Text(lead, color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(video.title, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text("${ytCompact(video.periodViews)} views · ${ytWatch(video.watchMinutes)} · ${ytDuration(video.averageViewDurationSeconds)} avg", color = MutedText, fontSize = 8.3.sp)
                Text(linked?.let { "Connected · ${it.title}" } ?: "Connect project", color = if (linked != null) MutedGold else RecRed, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun YTFormatSignal(data: YouTubeAnalyticsSnapshot, tasks: List<CreatorTask>, links: Map<String, String>) {
    val rows = data.recentVideos.mapNotNull { video ->
        val task = tasks.firstOrNull { it.id == links[video.videoId] } ?: return@mapNotNull null
        ytPillar(task) to video
    }.groupBy({ it.first }, { it.second })
        .map { (label, videos) -> Triple(label, videos.sumOf { it.periodViews }, videos.size) }
        .sortedByDescending { it.second }

    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp)) {
            Text("WHAT'S WORKING", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text("Based on published videos you connect to projects.", color = MutedText, fontSize = 8.6.sp)
            Spacer(Modifier.height(11.dp))
            if (rows.isEmpty()) {
                Text("Connect a few published videos to see which content types work best.", color = MutedText, fontSize = 9.4.sp, lineHeight = 14.sp)
            } else {
                rows.take(5).forEach { (label, views, count) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = ProjectorIvory, fontSize = 9.7.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$count video${if (count == 1) "" else "s"}", color = MutedText, fontSize = 8.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(ytCompact(views), color = MutedGold, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun YTLocalCreatorSection(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    personalization: CreatorPersonalizationSnapshot,
) {
    val active = tasks.count { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
    val readyIdeas = ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE }
    Text("YOUR CREATOR PROGRESS", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
    Text("Your publishing rhythm matters alongside channel numbers.", color = MutedText, fontSize = 9.sp)
    Spacer(Modifier.height(9.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        YTMetric("THIS WEEK", personalization.publishedThisWeek.toString(), SuccessGreen, Modifier.weight(1f))
        YTMetric("TARGET", personalization.weeklyTarget.toString(), MutedGold, Modifier.weight(1f))
        YTMetric("ACTIVE", active.toString(), RecRed, Modifier.weight(1f))
    }
    Spacer(Modifier.height(7.dp))
    Text("$readyIdeas idea${if (readyIdeas == 1) "" else "s"} ready to produce · ${personalization.primaryPlatform} is your primary publishing lane.", color = MutedText, fontSize = 8.7.sp)
}

@Composable
private fun YTLinkProjectDialog(video: YouTubeVideoSnapshot, tasks: List<CreatorTask>, currentTaskId: String?, onDismiss: () -> Unit, onLink: (String?) -> Unit) {
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

@Composable
private fun YTEmpty(text: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Text(text, color = MutedText, fontSize = 9.4.sp, modifier = Modifier.padding(15.dp))
    }
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

private fun ytPillar(task: CreatorTask): String = YouTubeContentClassifier.label(task)

private fun ytCompact(value: Long): String = when {
    value >= 1_000_000_000L -> String.format(Locale.US, "%.1fB", value / 1_000_000_000.0)
    value >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}

private fun ytWatch(minutes: Long): String {
    val hours = minutes / 60.0
    return if (hours >= 1000) String.format(Locale.US, "%.1fK h", hours / 1000.0) else String.format(Locale.US, "%.1f h", hours)
}

private fun ytDuration(seconds: Long): String = "%d:%02d".format(seconds / 60, seconds % 60)
private fun ytSigned(value: Long): String = if (value > 0) "+${ytCompact(value)}" else ytCompact(value)
private fun ytSyncTime(millis: Long): String = if (millis <= 0L) "never" else SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()).format(Date(millis))
