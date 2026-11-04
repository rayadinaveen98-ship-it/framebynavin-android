package com.framebynavin.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.spaces.V290InterruptedPhoto
import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290PrivatePhotoFiles
import com.framebynavin.app.personal.spaces.V290PhotoJournal
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import com.framebynavin.app.ui.theme.BacklotAccent
import com.framebynavin.app.ui.theme.BacklotBorder
import com.framebynavin.app.ui.theme.BacklotPrimaryText
import com.framebynavin.app.ui.theme.BacklotSecondaryAccent
import com.framebynavin.app.ui.theme.BacklotSecondaryText
import com.framebynavin.app.ui.theme.BacklotSurface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Explicitly owner-created photographic history. Neither the number nor visual
 * difference between images is an automatic claim of health or beauty improvement.
 */
@Composable
internal fun V290PersonalPhotoSection(
    photos: List<V290SpacePhotoEntity>,
    interruptedPhotos: List<V290InterruptedPhoto>,
    journal: V290PhotoJournal,
    enabled: Boolean,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (V290SpacePhotoEntity) -> Unit,
    onRecoverPhoto: (V290InterruptedPhoto) -> Unit,
    onDiscardPhoto: (V290InterruptedPhoto) -> Unit,
) {
    val nowMillis = System.currentTimeMillis()
    val pair = remember(photos) {
        V290PhotoTimelinePolicy.compare(photos, nowMillis)
    }
    Surface(
        color = BacklotSurface,
        border = BorderStroke(1.dp, BacklotBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("v290-photo-journal"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Outlined.PhotoLibrary, null, tint = BacklotSecondaryAccent,
                    modifier = Modifier.size(23.dp))
                Column(Modifier.weight(1f)) {
                    Text("Photo journal", color = BacklotPrimaryText,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Private to this device", color = BacklotSecondaryText,
                        fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onAddPhoto,
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.testTag("v290-pick-photo"),
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("ADD", fontSize = 11.sp)
                }
            }
            Text(
                "Choose your own pictures. They are copied to private storage with original photo metadata removed. Current backups do not include pictures.",
                color = BacklotSecondaryText, fontSize = 11.sp,
                modifier = Modifier.testTag("v290-photo-private-notice"),
            )
            when {
                photos.isEmpty() -> {
                    Text("No photos yet. Add a baseline whenever you're ready.",
                        color = BacklotSecondaryText, fontSize = 12.sp,
                        modifier = Modifier.testTag("v290-photo-empty"))
                }
                pair == null -> {
                    Text("Baseline saved. Add a new photo after 14 days to compare.",
                        color = BacklotSecondaryText, fontSize = 12.sp,
                        modifier = Modifier.testTag("v290-photo-baseline"))
                }
                else -> {
                    val remaining = (V290PhotoTimelinePolicy.MIN_COMPARISON_DAYS - pair.daysBetween)
                        .coerceAtLeast(0)
                    Text(
                        if (pair.meetsFourteenDayGoal)
                            "Photos recorded ${pair.daysBetween} days apart"
                        else "Only ${pair.daysBetween} days apart · ${remaining} more days until 14-day check-in",
                        color = BacklotSecondaryAccent, fontSize = 12.sp,
                        modifier = Modifier.testTag("v290-compare-days"),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        V290PreviewTile(
                            item = pair.baseline, title = "BEFORE",
                            journal = journal, modifier = Modifier.weight(1f),
                        )
                        V290PreviewTile(
                            item = pair.comparison, title = "LATER",
                            journal = journal, modifier = Modifier.weight(1f),
                        )
                    }
                    Text("Images are shown side by side; no automatic changes or results are inferred.",
                        color = BacklotSecondaryText, fontSize = 10.sp)
                }
            }
            if (interruptedPhotos.isNotEmpty()) {
                Surface(
                    color = BacklotBorder,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("v290-interrupted-photo"),
                ) {
                    Column(
                        Modifier.padding(11.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            "Unfinished photo save detected",
                            color = BacklotPrimaryText, fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                        )
                        Text(
                            "The photo copy is still on this device, but its journal entry was interrupted. Choose to recover or discard it.",
                            color = BacklotSecondaryText, fontSize = 11.sp,
                        )
                        interruptedPhotos.take(3).forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    enabled = enabled,
                                    onClick = { onRecoverPhoto(item) },
                                    modifier = Modifier.testTag("v290-recover-photo-open"),
                                ) { Text("RECOVER", fontSize = 11.sp) }
                                TextButton(
                                    enabled = enabled,
                                    onClick = { onDiscardPhoto(item) },
                                    modifier = Modifier.testTag("v290-discard-photo-open"),
                                ) { Text("DISCARD", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
            if (photos.isNotEmpty()) {
                Text("Saved photos · ${photos.size}", color = BacklotPrimaryText,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                photos.asReversed().forEach { photo ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        modifier = Modifier.fillMaxWidth().testTag("v290-photo-row")) {
                        Text(
                            Instant.ofEpochMilli(photo.takenAtMillis)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())),
                            color = BacklotSecondaryText, fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (photo.note.isNotBlank()) {
                            Text(photo.note, color = BacklotSecondaryText,
                                fontSize = 11.sp, maxLines = 1,
                                modifier = Modifier.weight(1.2f))
                        }
                        IconButton(
                            enabled = enabled,
                            onClick = { onRemovePhoto(photo) },
                            modifier = Modifier.testTag("v290-delete-photo"),
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, "Remove this photo",
                                tint = BacklotAccent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V290PreviewTile(
    item: V290SpacePhotoEntity,
    title: String,
    journal: V290PhotoJournal,
    modifier: Modifier = Modifier,
) {
    var loadingError by remember(item.id) { mutableStateOf(false) }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, item.id, journal) {
        value = try {
            withContext(Dispatchers.IO) {
                val bytes = journal.readVerifiedPhotoBytes(item)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 900 || bounds.outHeight / sample > 900) {
                    sample *= 2
                }
                BitmapFactory.decodeByteArray(
                    bytes, 0, bytes.size,
                    BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    }
                )?.asImageBitmap()
            }
        } catch (_: Exception) {
            loadingError = true
            null
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = BacklotSecondaryAccent,
            fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Surface(shape = RoundedCornerShape(10.dp), color = BacklotBorder,
            modifier = Modifier.fillMaxWidth().height(190.dp)) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!, contentDescription = "$title photo",
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (loadingError) "Photo missing or altered" else "Loading photo",
                        color = BacklotSecondaryText, fontSize = 11.sp,
                    )
                }
            }
        }
    }
}
