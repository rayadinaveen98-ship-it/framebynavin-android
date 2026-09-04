package com.framebynavin.app.ui

import android.app.Activity
import android.content.Intent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun V11InsightsScreen(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    onAdd: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val store = remember { YouTubeAnalyticsStore(context.applicationContext) }
    val milestoneStore = remember { YouTubeMilestoneStore(context.applicationContext) }
    val api = remember { YouTubeApiClient() }
    val authClient = remember(activity) { activity?.let { Identity.getAuthorizationClient(it) } }
    val scope = rememberCoroutineScope()

    var windowDays by rememberSaveable { mutableIntStateOf(28) }
    var snapshot by remember { mutableStateOf(store.load(windowDays) ?: store.loadAny()) }
    var pendingSyncDays by rememberSaveable { mutableIntStateOf(windowDays) }
    var syncing by rememberSaveable { mutableStateOf(false) }
    var authError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVideo by remember { mutableStateOf<YouTubeVideoSnapshot?>(null) }
    var links by remember { mutableStateOf(store.links()) }

    LaunchedEffect(windowDays) {
        store.load(windowDays)?.let { snapshot = it }
        authError = null
    }

    fun syncWithToken(token: String, days: Int = windowDays) {
        scope.launch {
            syncing = true
            authError = null
            runCatching {
                withContext(Dispatchers.IO) { api.sync(token, days) }
            }.onSuccess { fresh ->
                store.save(fresh)
                milestoneStore.captureFrom(fresh, store.links())
                snapshot = fresh
            }.onFailure { error ->
                authError = ytFriendlyError(error)
            }
            syncing = false
        }
    }

    val resolutionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            syncing = false
            authError = "YouTube connection was cancelled."
            return@rememberLauncherForActivityResult
        }
        val authResult = runCatching { authClient?.getAuthorizationResultFromIntent(result.data!!) }.getOrNull()
        val token = authResult?.accessToken
        if (token.isNullOrBlank()) {
            syncing = false
            authError = "Google did not return a YouTube access token."
        } else {
            syncWithToken(token, pendingSyncDays)
        }
    }

    fun authorize(selectAccount: Boolean = false, days: Int = windowDays) {
        val client = authClient ?: run {
            authError = "Google authorization is unavailable on this device."
            return
        }
        syncing = true
        authError = null
        pendingSyncDays = days
        client.authorize(YouTubeAuthorization.request(selectAccount))
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    val pending = result.pendingIntent
                    if (pending == null) {
                        syncing = false
                        authError = "Google authorization needs attention, but no consent screen was available."
                    } else {
                        resolutionLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                    }
                } else {
                    val token = result.accessToken
                    if (token.isNullOrBlank()) {
                        syncing = false
                        authError = "Google authorization completed without a YouTube access token."
                    } else {
                        syncWithToken(token, pendingSyncDays)
                    }
                }
            }
            .addOnFailureListener {
                syncing = false
                authError = ytFriendlyError(it)
            }
    }

    fun disconnect() {
        val request = RevokeAccessRequest.builder().setScopes(YouTubeAuthorization.scopes).build()
        authClient?.revokeAccess(request)?.addOnCompleteListener {
            store.clearAll()
            snapshot = null
            links = emptyMap()
            authError = null
        } ?: run {
            store.clearAll()
            snapshot = null
            links = emptyMap()
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
            Text("What matters — and what next?", color = ProjectorIvory, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Performance, causes and creator decisions in one place.", color = MutedText, fontSize = 10.5.sp)
            Spacer(Modifier.height(18.dp))

            if (snapshot == null) {
                YTConnectCard(
                    syncing = syncing,
                    error = authError,
                    packageName = context.packageName,
                    sha1 = remember { YouTubeAuthorization.signingSha1(context) },
                    onConnect = { authorize(true) },
                )
                Spacer(Modifier.height(18.dp))
                YTLocalCreatorSection(tasks, ideas)
            } else {
                val data = snapshot!!
                YTChannelHeader(
                    data = data,
                    syncing = syncing,
                    windowDays = windowDays,
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
                store.link(video.videoId, taskId)
                links = store.links()
                selectedVideo = null
            },
        )
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
                    Text("Read-only channel + analytics access", color = MutedText, fontSize = 9.2.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Bring views, watch time, subscribers and video performance into Creator OS. FrameByNavin never asks for upload or delete permission.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onConnect, enabled = !syncing, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(15.dp)) {
                if (syncing) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ProjectorIvory)
                else { Icon(Icons.Outlined.Link, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("CONNECT YOUTUBE", fontWeight = FontWeight.Black, fontSize = 9.5.sp) }
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
            Text("YOUTUBE SETUP", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(message, color = ProjectorIvory, fontSize = 9.5.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text("Package · $packageName", color = MutedText, fontSize = 8.2.sp)
            Text("SHA-1 · $sha1", color = MutedText, fontSize = 8.2.sp)
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
                    else Text("SYNC", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(7, 28, 90).forEach { days ->
                    FilterChip(selected = windowDays == days, onClick = { onWindow(days) }, label = { Text("${days}D", fontSize = 8.5.sp) })
                }
                Spacer(Modifier.weight(1f))
                Box {
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(34.dp)) { Icon(Icons.Outlined.MoreVert, null, tint = MutedText, modifier = Modifier.size(18.dp)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Switch account") }, onClick = { menu = false; onSwitchAccount() })
                        DropdownMenuItem(text = { Text("Disconnect") }, onClick = { menu = false; onDisconnect() })
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Text("${data.startDate} → ${data.endDate} · synced ${ytSyncTime(data.fetchedAtMillis)}", color = MutedText, fontSize = 8.sp)
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
        YTEmpty("No video-level analytics returned yet.")
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
    Text("Tap a video to link it to the Creator OS project that produced it.", color = MutedText, fontSize = 9.sp)
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
                Text(linked?.let { "Linked · ${it.title}" } ?: "Link project", color = if (linked != null) MutedGold else RecRed, fontSize = 8.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Text("CONTENT SIGNAL", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text("Built from YouTube videos you link back to Creator OS projects.", color = MutedText, fontSize = 8.6.sp)
            Spacer(Modifier.height(11.dp))
            if (rows.isEmpty()) {
                Text("Link a few published videos to unlock format and pillar performance here.", color = MutedText, fontSize = 9.4.sp, lineHeight = 14.sp)
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
private fun YTLocalCreatorSection(tasks: List<CreatorTask>, ideas: List<CreatorIdea>) {
    val done = tasks.count { it.status == TaskStatus.DONE }
    val active = tasks.count { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
    val readyIdeas = ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE }
    Text("CREATOR OS", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
    Text("Your local production momentum still matters beside platform numbers.", color = MutedText, fontSize = 9.sp)
    Spacer(Modifier.height(9.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        YTMetric("DONE", done.toString(), SuccessGreen, Modifier.weight(1f))
        YTMetric("ACTIVE", active.toString(), RecRed, Modifier.weight(1f))
        YTMetric("IDEAS READY", readyIdeas.toString(), MutedGold, Modifier.weight(1f))
    }
}

@Composable
private fun YTLinkProjectDialog(video: YouTubeVideoSnapshot, tasks: List<CreatorTask>, currentTaskId: String?, onDismiss: () -> Unit, onLink: (String?) -> Unit) {
    val youtubeTasks = tasks.filter { it.platform.equals("YouTube", true) }
        .sortedWith(compareByDescending<CreatorTask> { it.status == TaskStatus.DONE }.thenByDescending { it.dueAtMillis })
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link project", fontWeight = FontWeight.Black) },
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
        raw.contains("DEVELOPER_ERROR", true) || raw.contains("10:") -> "Google OAuth is not configured for this app signature yet. Enable YouTube Data API + YouTube Analytics API and add this Android package/SHA-1 in Google Cloud."
        raw.contains("403") || raw.contains("accessNotConfigured", true) || raw.contains("has not been used", true) -> "The required YouTube APIs are not enabled for the Google Cloud project yet."
        raw.contains("401") || raw.contains("invalid credentials", true) -> "YouTube authorization expired. Connect again and retry."
        raw.isNotBlank() -> raw
        else -> "YouTube sync failed. Check internet access and Google authorization."
    }
}

private fun ytPillar(task: CreatorTask): String {
    val title = task.title.lowercase(Locale.getDefault())
    val type = task.contentType.lowercase(Locale.getDefault())
    return when {
        title.contains("frame breakdown") -> "Frame Breakdown"
        title.contains("why this scene works") -> "Why This Scene Works"
        type.contains("cinematic moment") -> "Every Cinematic Moment"
        type.contains("long-form") || type.contains("long form") -> "FrameByNavin Analysis"
        title.contains("review") || title.contains("recommend") -> "Reviews / Recommendations"
        type.contains("short") -> "YouTube Shorts"
        else -> task.contentType
    }
}

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
