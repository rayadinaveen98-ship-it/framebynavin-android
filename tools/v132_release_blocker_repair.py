from pathlib import Path

path = Path("app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt")
text = path.read_text()

# Use normal packaged Android resources for the hero wall. The previous loader looked for
# app/src/main/assets/*.jpg even though those files were never packaged.
for obsolete in (
    "import android.os.SystemClock\n",
    "import android.graphics.BitmapFactory\n",
    "import androidx.compose.foundation.gestures.detectTapGestures\n",
    "import androidx.compose.ui.graphics.asImageBitmap\n",
    "import androidx.compose.ui.input.pointer.pointerInput\n",
):
    text = text.replace(obsolete, "")

if "import androidx.compose.foundation.combinedClickable\n" not in text:
    text = text.replace(
        "import androidx.compose.foundation.background\n",
        "import androidx.compose.foundation.background\nimport androidx.compose.foundation.combinedClickable\n",
    )
if "import com.framebynavin.app.R\n" not in text:
    text = text.replace(
        "import com.framebynavin.app.data.CreatorTask\n",
        "import com.framebynavin.app.R\nimport com.framebynavin.app.data.CreatorTask\n",
    )

old_loader = '''    val context = LocalContext.current
    val assetImages = remember {
        (1..10).mapNotNull { frameIndex ->
            val name = "hero_frame_${frameIndex.toString().padStart(2, '0')}.jpg"
            runCatching {
                context.assets.open(name).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(assetImages.size) {
        if (assetImages.size > 1) {
            while (true) {
                delay(4_500L)
                index = (index + 1) % assetImages.size
            }
        }
    }
    val quoteIndex = if (assetImages.isEmpty()) 0 else index % heroQuotes.size
'''
new_loader = '''    val resourceIds = remember {
        listOf(
            R.drawable.hero_frame_01,
            R.drawable.hero_frame_02,
            R.drawable.hero_frame_03,
            R.drawable.hero_frame_04,
            R.drawable.hero_frame_05,
            R.drawable.hero_frame_06,
            R.drawable.hero_frame_07,
            R.drawable.hero_frame_08,
            R.drawable.hero_frame_09,
            R.drawable.hero_frame_10,
        )
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(resourceIds.size) {
        while (resourceIds.size > 1) {
            delay(4_000L)
            index = (index + 1) % resourceIds.size
        }
    }
    val quoteIndex = index % heroQuotes.size
'''
if old_loader not in text:
    raise SystemExit("hero loader block not found")
text = text.replace(old_loader, new_loader)
text = text.replace("assetImages", "resourceIds")
text = text.replace(
    '''                    Image(
                        bitmap = resourceIds[visibleIndex],
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )''',
    '''                    Image(
                        painter = painterResource(resourceIds[visibleIndex]),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )''',
)

# Replace the fragile custom 2-second press detector with Compose's standard click/long-click
# semantics. Plan tap always enters/toggles selection; Reminder tap keeps its edit action.
text = text.replace(
    'Text("Hold a project for 2 seconds to select and manage it.", color = MutedText, fontSize = 9.4.sp)',
    'Text("Tap a project to select it. Long-press also works.", color = MutedText, fontSize = 9.4.sp)',
)
text = text.replace(
    '''modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).v131Hold2s(
                onHold = { onHold(task.id) },
                onTap = { if (selectionMode) onToggleSelection(task.id) },
            ),''',
    '''modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).combinedClickable(
                onClick = { onToggleSelection(task.id) },
                onLongClick = { onHold(task.id) },
            ),''',
)
text = text.replace(
    'Text("Hold a reminder for 2 seconds to select it. Deleting here removes only the reminder, never the project or Plan item.", color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)',
    'Text("Tap a reminder to edit it. Long-press to select one or more. Deleting here removes only the reminder, never the project or Plan item.", color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)',
)
text = text.replace(
    '''modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).v131Hold2s(
                onHold = { onHold(task.id) },
                onTap = { if (selectionMode) onToggle(task.id) else onEdit(task.id) },
            ),''',
    '''modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).combinedClickable(
                onClick = { if (selectionMode) onToggle(task.id) else onEdit(task.id) },
                onLongClick = { onHold(task.id) },
            ),''',
)

old_helper = '''private fun Modifier.v131Hold2s(onHold: () -> Unit, onTap: () -> Unit): Modifier = pointerInput(onHold, onTap) {
    detectTapGestures(
        onPress = {
            val started = SystemClock.elapsedRealtime()
            val released = tryAwaitRelease()
            if (released) {
                val held = SystemClock.elapsedRealtime() - started
                if (held >= 1_950L) onHold() else onTap()
            }
        }
    )
}

'''
if old_helper not in text:
    raise SystemExit("legacy hold helper not found")
text = text.replace(old_helper, "")

path.write_text(text)
