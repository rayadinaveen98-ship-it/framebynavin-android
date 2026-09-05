from pathlib import Path

ROOT = Path('.')
BUILD = ROOT / 'app/build.gradle.kts'
TODAY = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V18TodayScreen.kt'
FRAMES = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V175BestFramesUi.kt'
UNIT_TEST = ROOT / 'app/src/test/java/com/framebynavin/app/ui/V18CinematicHomeHeroAlpha16Test.kt'
UI_TEST = ROOT / 'app/src/androidTest/java/com/framebynavin/app/ui/V18CinematicHomeHeroAlpha16UiTest.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


# Version
build = BUILD.read_text()
build = replace_once(build, 'versionCode = 55', 'versionCode = 56', 'versionCode')
build = replace_once(
    build,
    'versionName = "1.8.0-product-alpha15"',
    'versionName = "1.8.0-product-alpha16"',
    'versionName',
)
BUILD.write_text(build)

# The old greeting + separate Best Frames section becomes one optional hero.
today = TODAY.read_text()
today = replace_once(
    today,
    '''            PHomeGreetingHeader(creatorProfile.safeDisplayName, onAdd)
            Spacer(Modifier.height(12.dp))
            V131HomeHeroSlideshow()
            Spacer(Modifier.height(18.dp))''',
    '''            V18CinematicHomeHero(creatorProfile.safeDisplayName)
            Spacer(Modifier.height(20.dp))''',
    'unified cinematic home hero',
)
TODAY.write_text(today)

# Rewrite the old Best Frames implementation as the unified home hero while preserving
# the same on-device storage directory and preference keys so existing images survive upgrade.
FRAMES.write_text(r'''package com.framebynavin.app.ui

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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
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

private const val V18_HERO_FRAME_MS = 5_200L
private const val V18_HERO_MOTION_MS = 6_100
private const val V18_HERO_DISSOLVE_MS = 850

internal data class V18HeroMotionPlan(
    val startScale: Float,
    val endScale: Float,
    val startX: Float,
    val endX: Float,
    val startY: Float,
    val endY: Float,
)

internal object V18CinematicHeroMotion {
    private val plans = listOf(
        V18HeroMotionPlan(1.00f, 1.07f, 0f, -12f, 2f, 7f),
        V18HeroMotionPlan(1.06f, 1.01f, 10f, -5f, -4f, 5f),
        V18HeroMotionPlan(1.02f, 1.08f, -11f, 8f, 5f, -4f),
        V18HeroMotionPlan(1.07f, 1.02f, 8f, -8f, 0f, -6f),
        V18HeroMotionPlan(1.00f, 1.06f, -2f, 10f, 8f, -6f),
        V18HeroMotionPlan(1.05f, 1.01f, -8f, 8f, -5f, 6f),
    )

    fun planFor(index: Int): V18HeroMotionPlan = plans[Math.floorMod(index, plans.size)]
}

private object V18HomeHeroStore {
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

    suspend fun replace(context: Context, uris: List<Uri>): List<File> = withContext(Dispatchers.IO) {
        val selected = uris.take(MAX_FRAMES)
        if (selected.isEmpty()) return@withContext current(context)

        val staging = File(context.cacheDir, "home_hero_stage_${System.nanoTime()}").apply { mkdirs() }
        val staged = mutableListOf<File>()
        try {
            selected.forEachIndexed { index, uri ->
                val out = File(staging, "frame_${index + 1}.${extensionFor(context, uri)}")
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(out).use { output -> input.copyTo(output, 256 * 1024) }
                    } ?: error("No readable stream")
                }.onSuccess {
                    if (out.length() > 0L) staged += out else out.delete()
                }.onFailure { out.delete() }
            }

            if (staged.isEmpty()) return@withContext current(context)

            val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }
            val previous = current(context)
            val stamp = System.currentTimeMillis()
            val finalFiles = staged.mapIndexed { index, source ->
                val ext = source.extension.ifBlank { "img" }
                File(directory, "frame_${stamp}_${index + 1}.$ext").also { target ->
                    source.copyTo(target, overwrite = true)
                }
            }
            previous.filter { old -> finalFiles.none { it.absolutePath == old.absolutePath } }.forEach { it.delete() }
            persist(context, finalFiles)
            finalFiles
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
        val displayName = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()
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

private fun v18DecodeHeroFrame(file: File, maxDimension: Int = 2300): ImageBitmap? = try {
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
        while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
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

private fun v18Greeting(name: String): String {
    val safeName = name.trim().ifBlank { "Creator" }
    return when (java.time.ZonedDateTime.now().hour) {
        in 5..11 -> "Good Morning, $safeName"
        in 12..16 -> "Good Afternoon, $safeName"
        in 17..20 -> "Good Evening, $safeName"
        else -> "Good Night, $safeName"
    }
}

private fun v18Lerp(start: Float, end: Float, progress: Float): Float = start + (end - start) * progress

@Composable
internal fun V18CinematicHomeHero(creatorName: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var frames by remember { mutableStateOf(V18HomeHeroStore.current(context)) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var importing by remember { mutableStateOf(false) }
    var manageExpanded by rememberSaveable { mutableStateOf(false) }
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    val decoded = remember { mutableStateMapOf<String, ImageBitmap>() }
    val framePaths = remember(frames) { frames.map { it.absolutePath } }
    val greeting = v18Greeting(creatorName)

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(V18HomeHeroStore.MAX_FRAMES),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                importing = true
                manageExpanded = false
                frames = V18HomeHeroStore.replace(context, uris)
                decoded.clear()
                index = 0
                importing = false
            }
        }
    }
    val chooseFrames = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(framePaths) {
        if (framePaths.isEmpty()) index = 0
        else if (index !in framePaths.indices) index = 0
        while (framePaths.size > 1) {
            delay(V18_HERO_FRAME_MS)
            index = (index + 1) % framePaths.size
        }
    }

    LaunchedEffect(framePaths, index) {
        if (framePaths.isEmpty()) {
            decoded.clear()
            return@LaunchedEffect
        }
        val current = index.coerceIn(framePaths.indices)
        val previous = (current - 1 + framePaths.size) % framePaths.size
        val next = (current + 1) % framePaths.size
        val wanted = listOf(framePaths[current], framePaths[previous], framePaths[next]).distinct()
        wanted.forEach { path ->
            if (decoded[path] == null) {
                val bitmap = withContext(Dispatchers.IO) { v18DecodeHeroFrame(File(path)) }
                if (bitmap != null) decoded[path] = bitmap
            }
        }
        decoded.keys.toList().filterNot { it in wanted }.forEach { decoded.remove(it) }
    }

    if (frames.isEmpty()) {
        Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("FRAME BY NAVIN", color = RecRed, fontSize = 8.3.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text(greeting, color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(11.dp))
            Surface(
                onClick = { if (!importing) chooseFrames() },
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = CinemaSurfaceRaised,
                border = BorderStroke(1.dp, CinemaLine),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (importing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ProjectorIvory)
                    } else {
                        Icon(Icons.Outlined.Add, contentDescription = "Add slideshow images", tint = ProjectorIvory, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth().height(252.dp),
            shape = RoundedCornerShape(26.dp),
            color = CinemaBlack,
            border = BorderStroke(1.dp, CinemaLine.copy(alpha = .72f)),
            shadowElevation = 10.dp,
        ) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp)).background(CinemaBlack)) {
                AnimatedContent(
                    targetState = framePaths.getOrNull(index).orEmpty(),
                    transitionSpec = {
                        fadeIn(tween(V18_HERO_DISSOLVE_MS)) togetherWith fadeOut(tween(V18_HERO_DISSOLVE_MS))
                    },
                    label = "cinematicHomeHeroFrame",
                ) { path ->
                    val frameIndex = framePaths.indexOf(path).coerceAtLeast(0)
                    decoded[path]?.let { bitmap ->
                        V18LivingHeroFrame(bitmap = bitmap, path = path, frameIndex = frameIndex)
                    }
                }

                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = .18f),
                                Color.Transparent,
                                Color.Black.copy(alpha = .18f),
                                Color.Black.copy(alpha = .76f),
                            )
                        )
                    )
                )

                Column(
                    Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 66.dp, bottom = 18.dp)
                ) {
                    Text("FRAME BY NAVIN", color = RecRed, fontSize = 8.6.sp, fontWeight = FontWeight.Black, letterSpacing = 1.25.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(greeting, color = ProjectorIvory, fontSize = 21.sp, fontWeight = FontWeight.Black)
                }

                if (importing) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(38.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = .55f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ProjectorIvory)
                        }
                    }
                } else {
                    V18HeroManageActions(
                        expanded = manageExpanded,
                        onToggle = { manageExpanded = !manageExpanded },
                        onChange = { manageExpanded = false; chooseFrames() },
                        onRemove = { manageExpanded = false; confirmRemove = true },
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    )
                }
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove slideshow images?") },
            text = { Text("The homepage will return to the normal Frame By Navin greeting.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemove = false
                        scope.launch {
                            V18HomeHeroStore.clear(context)
                            decoded.clear()
                            frames = emptyList()
                            index = 0
                        }
                    }
                ) { Text("REMOVE", color = RecRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("CANCEL", color = MutedText) }
            },
            containerColor = CinemaSurfaceRaised,
            titleContentColor = ProjectorIvory,
            textContentColor = MutedText,
        )
    }
}

@Composable
private fun V18LivingHeroFrame(bitmap: ImageBitmap, path: String, frameIndex: Int) {
    val motion = remember(frameIndex) { V18CinematicHeroMotion.planFor(frameIndex) }
    val progress = remember(path) { Animatable(0f) }
    val density = LocalDensity.current
    val startXPx = with(density) { motion.startX.dp.toPx() }
    val endXPx = with(density) { motion.endX.dp.toPx() }
    val startYPx = with(density) { motion.startY.dp.toPx() }
    val endYPx = with(density) { motion.endY.dp.toPx() }

    LaunchedEffect(path) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(V18_HERO_MOTION_MS, easing = LinearEasing))
    }

    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier.fillMaxSize().graphicsLayer {
            val p = progress.value
            val scale = v18Lerp(motion.startScale, motion.endScale, p)
            scaleX = scale
            scaleY = scale
            translationX = v18Lerp(startXPx, endXPx, p)
            translationY = v18Lerp(startYPx, endYPx, p)
        },
        contentScale = ContentScale.Crop,
    )
}

@Composable
internal fun V18HeroManageActions(
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(160)) + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut(tween(120)) + shrinkHorizontally(shrinkTowards = Alignment.End),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                V18HeroIconButton(Icons.Outlined.Collections, "Change slideshow images", onChange)
                V18HeroIconButton(Icons.Outlined.DeleteOutline, "Remove slideshow images", onRemove, danger = true)
                Spacer(Modifier.width(1.dp))
            }
        }
        V18HeroIconButton(Icons.Outlined.MoreVert, "Slideshow options", onToggle)
    }
}

@Composable
private fun V18HeroIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(38.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = .58f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .10f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = if (danger) RecRed else ProjectorIvory,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/** Legacy compatibility only. The separate Best Frames card was intentionally retired in Alpha16. */
@Composable
internal fun V175BestFramesOfToday() = Unit
''')

UNIT_TEST.parent.mkdir(parents=True, exist_ok=True)
UNIT_TEST.write_text(r'''package com.framebynavin.app.ui

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V18CinematicHomeHeroAlpha16Test {
    @Test
    fun motionPlansStaySubtleAndLoopDeterministically() {
        (0 until 12).forEach { index ->
            val plan = V18CinematicHeroMotion.planFor(index)
            assertTrue(plan.startScale in 1.0f..1.08f)
            assertTrue(plan.endScale in 1.0f..1.08f)
            val movement = abs(plan.endScale - plan.startScale) +
                abs(plan.endX - plan.startX) + abs(plan.endY - plan.startY)
            assertTrue(movement > 0.01f)
        }
        assertEquals(V18CinematicHeroMotion.planFor(0), V18CinematicHeroMotion.planFor(6))
    }
}
''')

UI_TEST.parent.mkdir(parents=True, exist_ok=True)
UI_TEST.write_text(r'''package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V18CinematicHomeHeroAlpha16UiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyHeroKeepsGreetingAndOffersImageAddAction() {
        composeRule.setContent { V18CinematicHomeHero("King") }

        composeRule.onNodeWithText("FRAME BY NAVIN").assertIsDisplayed()
        composeRule.onNode(hasText("King", substring = true)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add slideshow images").assertIsDisplayed()
    }

    @Test
    fun expandedHeroManagementUsesIconActions() {
        composeRule.setContent {
            V18HeroManageActions(
                expanded = true,
                onToggle = {},
                onChange = {},
                onRemove = {},
            )
        }

        composeRule.onNodeWithContentDescription("Change slideshow images").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Remove slideshow images").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Slideshow options").assertIsDisplayed()
    }
}
''')

print('Applied v1.8 Product Alpha16 cinematic home hero redesign')