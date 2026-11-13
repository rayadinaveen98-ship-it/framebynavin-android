package com.framebynavin.app.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

/**
 * Home is a read-only observer: no Space, journal, photo or linked reminder is
 * created or changed here. Database absence is a genuine first-use state;
 * do not create a new Room file until the owner opens the existing Space UI.
 */
@Composable
internal fun V340HomeSpacesEntry(
    onOpenSpaces: (String?) -> Unit,
    modifier: Modifier = Modifier,
    refreshVersion: Int = 0,
) {
    val context = LocalContext.current.applicationContext
    val database = remember(context, refreshVersion) {
        runCatching {
            if (context.getDatabasePath(V289SpacesDatabase.FILE_NAME).isFile)
                V289SpacesDatabase.open(context) else null
        }.getOrNull()
    }
    DisposableEffect(database) {
        onDispose { database?.close() }
    }
    var spaces by remember(database) { mutableStateOf(emptyList<V289SpaceEntity>()) }
    var loaded by remember(database) { mutableStateOf(database == null) }
    var unavailable by remember(database) { mutableStateOf(false) }
    LaunchedEffect(database) {
        if (database != null) {
            try {
                database.spaces().observeSpaces().collect {
                    spaces = it
                    loaded = true
                    unavailable = false
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                unavailable = true
                loaded = true
            }
        }
    }

    val snapshot = V340HomeSpacesPolicy.preview(spaces)
    val openOverview = { onOpenSpaces(null) }

    Surface(
        modifier = modifier.fillMaxWidth().testTag("v340-home-spaces-entry"),
        shape = RoundedCornerShape(18.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Surface(
                onClick = openOverview,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                    .testTag("v340-home-spaces-open"),
                color = androidx.compose.ui.graphics.Color.Transparent,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null,
                        tint = MutedGold, modifier = Modifier.size(21.dp))
                    Column(Modifier.weight(1f)) {
                        Text("MY SPACES", color = ProjectorIvory,
                            fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Text(
                            when {
                                unavailable -> "Spaces couldn't be read · open to review"
                                !loaded -> "Opening saved Spaces…"
                                snapshot.activeCount > 0 ->
                                    "${snapshot.activeCount} active" +
                                        if (snapshot.archivedCount > 0)
                                            " · ${snapshot.archivedCount} archived" else ""
                                snapshot.archivedCount > 0 ->
                                    "No active Spaces · ${snapshot.archivedCount} archived"
                                else -> "Your private journals, goals and photos"
                            },
                            color = MutedText, fontSize = 10.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(Icons.Outlined.ChevronRight,
                        contentDescription = "Open My Spaces",
                        tint = MutedGold, modifier = Modifier.size(21.dp))
                }
            }

            if (!unavailable && loaded && snapshot.activeCount > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshot.featured.forEach { space ->
                        Surface(
                            onClick = {
                                onOpenSpaces(V340HomeSpacesPolicy.openableSpaceId(
                                    space.id, spaces))
                            },
                            modifier = Modifier.weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("v340-home-space-" + space.id),
                            shape = RoundedCornerShape(12.dp),
                            color = CinemaSurfaceRaised,
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Icon(V289SpaceIcon(space.type), contentDescription = null,
                                    tint = MutedGold, modifier = Modifier.size(17.dp))
                                Text(space.title, color = ProjectorIvory,
                                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (snapshot.activeCount > snapshot.featured.size) {
                    Text("+${snapshot.activeCount - snapshot.featured.size} more · tap My Spaces to browse",
                        color = MutedText, fontSize = 10.sp)
                }
            } else if (loaded && !unavailable && snapshot.activeCount == 0) {
                Text(
                    if (snapshot.archivedCount > 0) "Open to review or restore archived Spaces"
                    else "Start a Space to keep notes, photos and progress together",
                    color = MutedText, fontSize = 10.sp,
                    modifier = Modifier.testTag("v340-home-spaces-empty"),
                )
            } else if (unavailable) {
                Text("Your Spaces were not changed. Open Personal Spaces to review.",
                    color = MutedText, fontSize = 10.sp,
                    modifier = Modifier.testTag("v340-home-spaces-unavailable"))
            }
        }
    }
}
