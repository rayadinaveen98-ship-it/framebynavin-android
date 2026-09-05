from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]

# 1) Replace the legacy bundled slideshow implementation with the v1.7.5 component.
polish_path = ROOT / "app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt"
polish = polish_path.read_text()
pattern = re.compile(
    r"@Composable\ninternal fun V131HomeHeroSlideshow\(\) \{.*?\n\}\n@Composable\ninternal fun V131PlanScreen",
    re.S,
)
replacement = """@Composable
internal fun V131HomeHeroSlideshow() {
    V175BestFramesOfToday()
}
@Composable
internal fun V131PlanScreen"""
polish, count = pattern.subn(replacement, polish, count=1)
if count != 1:
    raise SystemExit("Could not locate V131HomeHeroSlideshow for v1.7.5 wiring")
polish_path.write_text(polish)

# 2) Original-quality, user-managed Best Frames gallery.
best_frames = r'''package com.framebynavin.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

private data class V175ImportResult(
    val files: List<File>,
    val note: String? = null,
)

private object V175BestFramesStore {
    const val MAX_FRAMES = 10
    private const val DIRECTORY = "best_frames_original"
    private const val PREFS = "best_frames_original_prefs"
    private const val KEY_ORDER = "frame_order"

    fun current(context: Context): List<File> {
        val directory = File(context.filesDir, DIRECTORY)
        val names = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ORDER, "")
            .orEmpty()
            .split('\n')
            .filter { it.isNotBlank() }
        return names.map { File(directory, it) }.filter { it.isFile }
    }

    suspend fun replace(context: Context, uris: List<Uri>): V175ImportResult = withContext(Dispatchers.IO) {
        val selected = uris.take(MAX_FRAMES)
        if (selected.isEmpty()) return@withContext V175ImportResult(current(context))

        val staging = File(context.cacheDir, "best_frames_stage_${System.nanoTime()}").apply { mkdirs() }
        val staged = mutableListOf<File>()
        var failed = 0

        try {
            selected.forEachIndexed { index, uri ->
                val extension = extensionFor(context, uri)
                val out = File(staging, "frame_${index + 1}.$extension")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(out).use { output ->
                            // Byte-for-byte copy. No bitmap decode, resize, or image recompression here.
                            input.copyTo(output, 256 * 1024)
                        }
                    } ?: error("No readable stream")
                    if (out.length() > 0L) staged += out else failed++
                } catch (_: Exception) {
                    out.delete()
                    failed++
                }
            }

            if (staged.isEmpty()) {
                return@withContext V175ImportResult(
                    files = current(context),
                    note = "Those images could not be imported. Please try the original files again.",
                )
            }

            val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }
            val old = current(context)
            val stamp = System.currentTimeMillis()
            val finalFiles = staged.mapIndexed { index, source ->
                val extension = source.extension.ifBlank { "img" }
                val target = File(directory, "frame_${stamp}_${index + 1}.$extension")
                source.copyTo(target, overwrite = true)
                target
            }

            // Only remove the previous gallery after every readable replacement has been copied.
            old.filter { existing -> finalFiles.none { it.absolutePath == existing.absolutePath } }
                .forEach { it.delete() }
            persist(context, finalFiles)

            val note = if (failed > 0) {
                "Imported ${finalFiles.size} original frame${if (finalFiles.size == 1) "" else "s"}; $failed could not be read."
            } else {
                "${finalFiles.size} original-quality frame${if (finalFiles.size == 1) "" else "s"} ready."
            }
            V175ImportResult(finalFiles, note)
        } finally {
            staging.deleteRecursively()
        }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        File(context.filesDir, DIRECTORY).deleteRecursively()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ORDER).apply()
    }

    private fun persist(context: Context, files: List<File>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ORDER, files.joinToString("\n") { it.name })
            .apply()
    }

    private fun extensionFor(context: Context, uri: Uri): String {
        val displayName = try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
        val fromName = displayName?.substringAfterLast('.', "")
            ?.lowercase(Locale.US)
            ?.takeIf { it.matches(Regex("[a-z0-9]{1,6}")) }
        if (fromName != null) return fromName
        return when (context.contentResolver.getType(uri)?.lowercase(Locale.US)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            else -> "jpg"
        }
    }
}

private fun v175DecodeForDisplay(file: File, maxDimension: Int = 4096): ImageBitmap? = try {
    val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val width = info.size.width
            val height = info.size.height
            val largest = maxOf(width, height)
            if (largest > maxDimension) {
                val ratio = maxDimension.toFloat() / largest.toFloat()
                decoder.setTargetSize(
                    (width * ratio).toInt().coerceAtLeast(1),
                    (height * ratio).toInt().coerceAtLeast(1),
                )
            }
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (
            bounds.outWidth / sample > maxDimension ||
            bounds.outHeight / sample > maxDimension
        ) {
            sample *= 2
        }
        BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )
    }
    bitmap?.asImageBitmap()
} catch (_: Exception) {
    null
}

@Composable
internal fun V175BestFramesOfToday() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var frames by remember { mutableStateOf(V175BestFramesStore.current(context)) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var importing by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(V175BestFramesStore.MAX_FRAMES),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                importing = true
                note = null
                val result = V175BestFramesStore.replace(context, uris)
                frames = result.files
                index = 0
                expanded = frames.isNotEmpty()
                note = result.note
                importing = false
            }
        }
    }

    val selectOriginals = {
        picker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val pulse = rememberInfiniteTransition(label = "v175BestFramesPulse")
    val pulseStrength by pulse.animateFloat(
        initialValue = .30f,
        targetValue = .94f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "v175BestFramesRedPulse",
    )

    LaunchedEffect(frames.size) {
        if (frames.isEmpty()) index = 0
        else if (index !in frames.indices) index = 0
    }

    LaunchedEffect(expanded, frames.size) {
        while (expanded && frames.size > 1) {
            delay(4_000L)
            index = (index + 1) % frames.size
        }
    }

    Column(
        Modifier.fillMaxWidth().animateContentSize(
            animationSpec = tween(360, easing = FastOutSlowInEasing),
        )
    ) {
        Surface(
            onClick = {
                when {
                    importing -> Unit
                    frames.isEmpty() -> selectOriginals()
                    else -> expanded = !expanded
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF120C0E),
            border = BorderStroke(1.dp, RecRed.copy(alpha = .52f)),
            shadowElevation = 10.dp,
        ) {
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.matchParentSize().background(
                        Brush.horizontalGradient(
                            listOf(
                                RecRed.copy(alpha = pulseStrength),
                                Color(0xFF751321).copy(alpha = .76f),
                                Color(0xFF1A0D11),
                            )
                        )
                    )
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(9.dp)
                            .background(ProjectorIvory.copy(alpha = .94f), CircleShape)
                    )
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "BEST FRAMES OF TODAY",
                            color = ProjectorIvory,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.25.sp,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            when {
                                importing -> "Copying original files…"
                                frames.isEmpty() -> "Tap to select up to 10 original images"
                                expanded -> "Tap to close  •  ${frames.size} original frame${if (frames.size == 1) "" else "s"}"
                                else -> "Tap to open  •  ${frames.size} original frame${if (frames.size == 1) "" else "s"}"
                            },
                            color = ProjectorIvory.copy(alpha = .74f),
                            fontSize = 8.4.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (importing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ProjectorIvory,
                            strokeWidth = 2.dp,
                        )
                    } else if (frames.isNotEmpty()) {
                        Icon(
                            Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (expanded) "Close best frames" else "Open best frames",
                            tint = ProjectorIvory,
                            modifier = Modifier.size(22.dp).graphicsLayer {
                                rotationZ = if (expanded) 180f else 0f
                            },
                        )
                    }
                }
            }
        }

        if (note != null) {
            Spacer(Modifier.height(7.dp))
            Text(
                note.orEmpty(),
                color = MutedText,
                fontSize = 8.4.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        AnimatedVisibility(
            visible = expanded && frames.isNotEmpty(),
            enter = expandVertically(animationSpec = tween(330, easing = FastOutSlowInEasing)) + fadeIn(tween(220)),
            exit = shrinkVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeOut(tween(170)),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().height(236.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, CinemaLine.copy(alpha = .52f)),
                    shadowElevation = 7.dp,
                ) {
                    Box(
                        Modifier.fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CinemaBlack)
                    ) {
                        AnimatedContent(
                            targetState = frames.getOrNull(index)?.absolutePath.orEmpty(),
                            transitionSpec = { fadeIn(tween(480)) togetherWith fadeOut(tween(480)) },
                            label = "v175OriginalFrame",
                        ) { path ->
                            val frame = frames.firstOrNull { it.absolutePath == path }
                            val bitmap = remember(path, frame?.lastModified()) {
                                frame?.let(::v175DecodeForDisplay)
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Best frame ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "This frame could not be displayed.",
                                        color = MutedText,
                                        fontSize = 9.sp,
                                    )
                                }
                            }
                        }

                        Box(
                            Modifier.fillMaxWidth().height(52.dp).align(Alignment.BottomCenter).background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = .76f))
                                )
                            )
                        )

                        if (frames.size > 1) {
                            IconButton(
                                onClick = { index = (index - 1 + frames.size) % frames.size },
                                modifier = Modifier.align(Alignment.CenterStart)
                                    .padding(start = 4.dp)
                                    .background(Color.Black.copy(alpha = .36f), CircleShape),
                            ) {
                                Icon(
                                    Icons.Outlined.KeyboardArrowLeft,
                                    contentDescription = "Previous frame",
                                    tint = ProjectorIvory,
                                )
                            }
                            IconButton(
                                onClick = { index = (index + 1) % frames.size },
                                modifier = Modifier.align(Alignment.CenterEnd)
                                    .padding(end = 4.dp)
                                    .background(Color.Black.copy(alpha = .36f), CircleShape),
                            ) {
                                Icon(
                                    Icons.Outlined.KeyboardArrowRight,
                                    contentDescription = "Next frame",
                                    tint = ProjectorIvory,
                                )
                            }
                        }

                        Row(
                            Modifier.align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 13.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "ORIGINAL FILE",
                                color = ProjectorIvory,
                                fontSize = 8.1.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${index + 1} / ${frames.size}",
                                color = ProjectorIvory.copy(alpha = .82f),
                                fontSize = 8.2.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(7.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    frames.indices.forEach { dot ->
                        Box(
                            Modifier.padding(horizontal = 2.5.dp)
                                .size(if (dot == index) 6.dp else 4.dp)
                                .background(
                                    if (dot == index) RecRed else MutedText.copy(alpha = .42f),
                                    CircleShape,
                                )
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = selectOriginals) {
                        Text("REPLACE FRAMES", color = ProjectorIvory, fontSize = 8.6.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                V175BestFramesStore.clear(context)
                                frames = emptyList()
                                index = 0
                                expanded = false
                                note = "Frames cleared. Tap Best Frames of Today to choose new originals."
                            }
                        }
                    ) {
                        Text("CLEAR", color = RecRed, fontSize = 8.6.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
'''
(ROOT / "app/src/main/java/com/framebynavin/app/ui/V175BestFramesUi.kt").write_text(best_frames)

# 3) Richer cinematic welcome background while preserving the approved red title band.
welcome = r'''package com.framebynavin.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.ProjectorIvory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * v1.7.5 cinematic launch ident.
 * Keeps the approved FRAME BY NAVIN red band, but places it inside a dark theatre/projector atmosphere.
 */
@Composable
internal fun V174CinematicWelcome() {
    val aperture = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val beamTravel = remember { Animatable(0f) }
    val ambience = remember { Animatable(0f) }
    val flare = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { ambience.animateTo(1f, tween(1_350, easing = FastOutSlowInEasing)) }
        launch { beamTravel.animateTo(1f, tween(2_650, easing = LinearEasing)) }
        delay(120)
        aperture.animateTo(1f, tween(820, easing = FastOutSlowInEasing))
        delay(90)
        launch { title.animateTo(1f, tween(620, easing = LinearOutSlowInEasing)) }
        delay(590)
        flare.animateTo(1f, tween(180, easing = LinearOutSlowInEasing))
        flare.animateTo(.16f, tween(520, easing = FastOutSlowInEasing))
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF020203),
                    Color(0xFF08090C),
                    Color(0xFF0C090B),
                    Color(0xFF050507),
                    CinemaBlack,
                )
            )
        )
    ) {
        // Soft theatre glow: it makes the centre feel dimensional without turning the screen red.
        Box(
            Modifier.align(Alignment.Center)
                .fillMaxWidth()
                .height(520.dp)
                .alpha(.52f * ambience.value)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF6D1618).copy(alpha = .30f),
                            Color(0xFF2A0B10).copy(alpha = .15f),
                            Color.Transparent,
                        ),
                        radius = 850f,
                    )
                )
        )

        // Two extremely restrained projector shafts falling into the title gate.
        Box(
            Modifier.align(Alignment.TopCenter)
                .offset(x = (-74).dp, y = 54.dp)
                .width(150.dp)
                .fillMaxHeight(.53f)
                .graphicsLayer(rotationZ = 7f)
                .alpha(.08f * ambience.value)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFFFFC88A).copy(alpha = .26f),
                            Color.Transparent,
                        )
                    )
                )
        )
        Box(
            Modifier.align(Alignment.TopCenter)
                .offset(x = 104.dp, y = 18.dp)
                .width(98.dp)
                .fillMaxHeight(.58f)
                .graphicsLayer(rotationZ = -9f)
                .alpha(.055f * ambience.value)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFFFFE0B3).copy(alpha = .22f),
                            Color.Transparent,
                        )
                    )
                )
        )

        Canvas(Modifier.fillMaxSize().alpha(.72f * ambience.value)) {
            // Fine deterministic film grain / dust. No random flicker or noisy visual clutter.
            repeat(82) { i ->
                val x = (((i * 47) % 101) / 101f) * size.width
                val y = (((i * 73 + 19) % 103) / 103f) * size.height
                val a = .012f + ((i % 5) * .004f)
                drawCircle(
                    color = Color.White.copy(alpha = a),
                    radius = if (i % 7 == 0) 1.25f else .58f,
                    center = Offset(x, y),
                )
            }

            // Faint 2.39:1 framing guides, almost like a projection gate in a dark auditorium.
            val guide = Color(0xFFFFD6A0).copy(alpha = .035f)
            drawLine(guide, Offset(0f, size.height * .335f), Offset(size.width, size.height * .335f), 1f)
            drawLine(guide, Offset(0f, size.height * .665f), Offset(size.width, size.height * .665f), 1f)

            // Ghosted film perforations at the extreme sides.
            repeat(9) { slot ->
                val y = size.height * (.15f + slot * .087f)
                drawRect(
                    color = Color.White.copy(alpha = .022f),
                    topLeft = Offset(size.width * .027f, y),
                    size = androidx.compose.ui.geometry.Size(size.width * .016f, size.height * .027f),
                )
                drawRect(
                    color = Color.White.copy(alpha = .022f),
                    topLeft = Offset(size.width * .957f, y),
                    size = androidx.compose.ui.geometry.Size(size.width * .016f, size.height * .027f),
                )
            }
        }

        // Red ambient bloom behind the band.
        Box(
            Modifier.align(Alignment.Center)
                .fillMaxWidth()
                .height((155f + 80f * flare.value).dp)
                .alpha(.24f + .18f * flare.value)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFF7D0B12).copy(alpha = .28f),
                            Color(0xFFB51218).copy(alpha = .18f),
                            Color.Transparent,
                        )
                    )
                )
        )

        // Approved red title gate, now with deeper edge falloff and a warm projector core.
        Box(
            Modifier.align(Alignment.Center)
                .fillMaxWidth()
                .height((2f + 116f * aperture.value).dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF310407),
                            Color(0xFF71090E),
                            Color(0xFFC91618),
                            Color(0xFFFF6B2E),
                            Color(0xFFE52B1F),
                            Color(0xFF8D0B10),
                            Color(0xFF350407),
                        )
                    )
                )
        )

        // A narrow travelling projector flare gives the band a living photographic highlight.
        Box(
            Modifier.align(Alignment.Center)
                .offset(x = (-178f + 356f * beamTravel.value).dp)
                .size(width = 62.dp, height = (18f + 134f * aperture.value).dp)
                .alpha(.10f + .30f * aperture.value)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFFFFE5B4).copy(alpha = .92f),
                            Color(0xFFFFB466).copy(alpha = .52f),
                            Color.Transparent,
                        )
                    )
                )
        )

        Box(
            Modifier.align(Alignment.Center)
                .size((120f + 250f * flare.value).dp)
                .alpha(.14f * flare.value)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFD09A), Color(0xFFE62A21), Color.Transparent)
                    ),
                    CircleShape,
                )
        )

        Text(
            "FRAME BY NAVIN",
            modifier = Modifier.align(Alignment.Center)
                .alpha(title.value)
                .graphicsLayer {
                    scaleX = .94f + (.06f * title.value)
                    scaleY = .94f + (.06f * title.value)
                    translationY = 10f * (1f - title.value)
                },
            color = ProjectorIvory,
            fontSize = 26.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.7.sp,
            textAlign = TextAlign.Center,
        )

        Box(
            Modifier.align(Alignment.Center)
                .fillMaxWidth(.54f)
                .height(1.dp)
                .offset(y = 48.dp)
                .alpha(.42f * title.value)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFFFFA167), Color.Transparent)
                    )
                )
        )
    }
}
'''
(ROOT / "app/src/main/java/com/framebynavin/app/ui/V174CinematicWelcome.kt").write_text(welcome)

# 4) Android 12+ / compat system splash: very short, charcoal, transparent custom mark.
main_path = ROOT / "app/src/main/java/com/framebynavin/app/MainActivity.kt"
main = main_path.read_text()
if "androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen" not in main:
    main = main.replace(
        "import androidx.lifecycle.lifecycleScope\n",
        "import androidx.lifecycle.lifecycleScope\nimport androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen\n",
    )
main = main.replace(
    "    override fun onCreate(savedInstanceState: Bundle?) {\n        super.onCreate(savedInstanceState)",
    "    override fun onCreate(savedInstanceState: Bundle?) {\n        installSplashScreen()\n        super.onCreate(savedInstanceState)",
)
main_path.write_text(main)

manifest_path = ROOT / "app/src/main/AndroidManifest.xml"
manifest = manifest_path.read_text()
manifest = manifest.replace(
    '        <activity\n            android:name=".MainActivity"\n            android:exported="true"\n            android:launchMode="singleTop">',
    '        <activity\n            android:name=".MainActivity"\n            android:exported="true"\n            android:launchMode="singleTop"\n            android:theme="@style/Theme.FrameByNavin.Starting">',
)
manifest_path.write_text(manifest)

themes = '''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FrameByNavin" parent="android:style/Theme.Material.NoActionBar">
        <item name="android:fontFamily">sans</item>
        <item name="android:windowLightStatusBar">false</item>
        <item name="android:statusBarColor">#050506</item>
        <item name="android:navigationBarColor">#050506</item>
        <item name="android:windowBackground">#050506</item>
    </style>

    <!-- The OS splash cannot be removed on modern Android. Keep it minimal and visually continuous. -->
    <style name="Theme.FrameByNavin.Starting" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">#050506</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_framebynavin_splash</item>
        <item name="windowSplashScreenAnimationDuration">0</item>
        <item name="postSplashScreenTheme">@style/Theme.FrameByNavin</item>
    </style>
</resources>
'''
(ROOT / "app/src/main/res/values/themes.xml").write_text(themes)

splash_mark = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Transparent cinematic frame/door mark: deliberately no white icon tile. -->
    <path
        android:fillColor="#F2E7D6"
        android:pathData="M24,22 L79,22 L79,27 L29,27 L29,81 L24,81 Z" />
    <path
        android:fillColor="#473C52"
        android:pathData="M69,31 L82,27 L82,76 L69,72 Z" />
    <path
        android:fillColor="#F4C86D"
        android:pathData="M31,42 L48,33 L48,76 L31,68 Z" />
    <path
        android:fillColor="#E3302D"
        android:pathData="M50,32 L68,39 L68,76 L50,75 Z" />
    <path
        android:fillColor="#FF8A49"
        android:pathData="M51,35 L56,37 L56,72 L51,73 Z" />
    <path
        android:fillColor="#FF403A"
        android:pathData="M74,78 C74,75.8 75.8,74 78,74 C80.2,74 82,75.8 82,78 C82,80.2 80.2,82 78,82 C75.8,82 74,80.2 74,78 Z" />
</vector>
'''
(ROOT / "app/src/main/res/drawable/ic_framebynavin_splash.xml").write_text(splash_mark)

# 5) Bump release and add the official splash-screen compatibility dependency.
gradle_path = ROOT / "app/build.gradle.kts"
gradle = gradle_path.read_text()
gradle = gradle.replace("versionCode = 36", "versionCode = 37")
gradle = gradle.replace(
    'versionName = "1.7.4-cinematic-home-welcome-rc1"',
    'versionName = "1.7.5-original-frames-splash-rc1"',
)
if 'androidx.core:core-splashscreen' not in gradle:
    gradle = gradle.replace(
        '    implementation("androidx.core:core-ktx:1.16.0")\n',
        '    implementation("androidx.core:core-ktx:1.16.0")\n    implementation("androidx.core:core-splashscreen:1.0.1")\n',
    )
gradle = re.sub(
    r"// v1\.7\.4 RC1:.*\Z",
    "// v1.7.5 RC1: original-quality user-managed Best Frames + launch polish.\n"
    "// Best Frames stores byte-for-byte copies of up to ten selected images in private app storage; no image re-encoding.\n"
    "// The Android system splash uses a transparent cinematic mark on charcoal, followed by the richer projector/theatre welcome.\n",
    gradle,
    flags=re.S,
)
gradle_path.write_text(gradle)

# 6) Remove the obsolete low-resolution bundled frame resources so they can never re-enter the slideshow.
for asset in (ROOT / "app/src/main/res/drawable-nodpi").glob("hero_frame_*.webp"):
    asset.unlink()

print("v1.7.5 integration applied")
