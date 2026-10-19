package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.youtube.YouTubeAnalyticsSnapshot
import com.framebynavin.app.youtube.YouTubeVideoSnapshot
import com.framebynavin.app.data.CreatorTask
import java.util.Locale

internal data class V189CatalogUiState(
    val videos: List<YouTubeVideoSnapshot> = emptyList(),
    val pageToken: String? = null,
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
)

internal enum class V189CatalogFilter(val label: String) {
    ALL("All"), LINKED("Linked"), UNLINKED("Unlinked"), REPORTED("Reported")
}
internal enum class V189CatalogSort(val label: String) {
    RECENT("Recent"), VIEWS("Views")
}

/** Client-side search intentionally limits itself to explicitly fetched pages. */
internal object V189CatalogPolicy {
    fun visible(
        videos: List<YouTubeVideoSnapshot>,
        query: String,
        filter: V189CatalogFilter,
        sort: V189CatalogSort,
        links: Map<String, String>,
    ): List<YouTubeVideoSnapshot> {
        val search = query.trim()
        val filtered = videos.distinctBy { it.videoId }.filter { video ->
            (search.isEmpty() || video.title.contains(search, ignoreCase = true)) &&
                when (filter) {
                    V189CatalogFilter.ALL -> true
                    V189CatalogFilter.LINKED -> links[video.videoId] != null
                    V189CatalogFilter.UNLINKED -> links[video.videoId] == null
                    V189CatalogFilter.REPORTED -> video.hasPeriodData
                }
        }
        return when (sort) {
            V189CatalogSort.RECENT -> filtered.sortedWith(
                compareByDescending<YouTubeVideoSnapshot> { it.publishedAtMillis }.thenBy { it.videoId })
            V189CatalogSort.VIEWS -> filtered.sortedWith(
                compareByDescending<YouTubeVideoSnapshot> { if (it.hasPeriodData) it.periodViews else -1L }
                    .thenByDescending { it.publishedAtMillis })
        }
    }
}

@Composable
internal fun V189VideoCatalog(
    snapshot: YouTubeAnalyticsSnapshot,
    state: V189CatalogUiState,
    links: Map<String, String>,
    onLoad: () -> Unit,
    onVideo: (YouTubeVideoSnapshot) -> Unit,
) {
    var query by rememberSaveable(snapshot.channel.channelId) { mutableStateOf("") }
    var filterName by rememberSaveable(snapshot.channel.channelId) { mutableStateOf(V189CatalogFilter.ALL.name) }
    var sortName by rememberSaveable(snapshot.channel.channelId) { mutableStateOf(V189CatalogSort.RECENT.name) }

    // The current period sample is a fallback only; don't label it a complete catalog.
    val sample = remember(snapshot) {
        (snapshot.recentVideos + snapshot.topVideos).distinctBy { it.videoId }
    }
    val source = if (state.loaded) state.videos else sample
    val visible = remember(source, query, filterName, sortName, links) {
        V189CatalogPolicy.visible(source, query, V189CatalogFilter.valueOf(filterName),
            V189CatalogSort.valueOf(sortName), links)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Videos", color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f))
        Text("${source.size} loaded", color = MutedText, fontSize = 10.sp)
    }
    Spacer(Modifier.height(9.dp))
    OutlinedTextField(
        value = query, onValueChange = { query = it }, singleLine = true,
        modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search loaded videos") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ProjectorIvory, unfocusedTextColor = ProjectorIvory,
            focusedBorderColor = MutedGold, unfocusedBorderColor = CinemaLine,
        ),
        shape = RoundedCornerShape(13.dp),
    )
    Spacer(Modifier.height(9.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        V189CatalogFilter.entries.forEach { item ->
            FilterChip(selected = filterName == item.name, onClick = { filterName = item.name },
                label = { Text(item.label, fontSize = 10.sp) })
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Sort", color = MutedText, fontSize = 10.sp)
        V189CatalogSort.entries.forEach { option ->
            FilterChip(selected = sortName == option.name, onClick = { sortName = option.name },
                label = { Text(option.label, fontSize = 10.sp) })
        }
    }
    Spacer(Modifier.height(6.dp))
    if (visible.isEmpty()) {
        Text("No videos", color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(vertical = 20.dp))
    } else {
        visible.forEachIndexed { index, video ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp)
                    .clickable { onVideo(video) },
                shape = RoundedCornerShape(14.dp),
                color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine),
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    V148YouTubeThumbnail(video.videoId, index + 1,
                        Modifier.width(96.dp).aspectRatio(16f / 9f))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(video.title, color = ProjectorIvory, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (video.hasPeriodData) "${v189Count(video.periodViews)} views"
                            else "Period data unavailable",
                            color = MutedText, fontSize = 9.sp,
                        )
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MutedText,
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
    state.error?.let {
        Text(it, color = RecRed, fontSize = 10.sp, modifier = Modifier.padding(vertical = 8.dp))
    }
    val canLoad = !state.loaded || state.pageToken != null || state.error != null
    if (canLoad) {
        Button(onClick = onLoad, enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CinemaSurfaceRaised)) {
            if (state.loading) CircularProgressIndicator(
                modifier = Modifier.size(16.dp), color = MutedGold, strokeWidth = 2.dp)
            else Text(if (state.error != null) "Retry"
                else if (state.loaded) "Load more" else "Load videos", color = ProjectorIvory)
        }
    }
}

internal fun v189Count(n: Long): String = when {
    n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format(Locale.US, "%.1fK", n / 1_000.0)
    else -> n.toString()
}
