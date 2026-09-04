from pathlib import Path

path = Path('app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt')
text = path.read_text()

if 'import android.graphics.BitmapFactory' not in text:
    text = text.replace('import android.os.SystemClock\n', 'import android.os.SystemClock\nimport android.graphics.BitmapFactory\n')
if 'import androidx.compose.ui.graphics.asImageBitmap' not in text:
    text = text.replace('import androidx.compose.ui.graphics.Color\n', 'import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.asImageBitmap\n')

old = '''    val resourceIds = remember {
        (1..10).mapNotNull { index ->
            val name = "hero_frame_${index.toString().padStart(2, '0')}"
            context.resources.getIdentifier(name, "drawable", context.packageName).takeIf { it != 0 }
        }
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(resourceIds.size) {
        if (resourceIds.size > 1) {
            while (true) {
                delay(4_500L)
                index = (index + 1) % resourceIds.size
            }
        }
    }
    val quoteIndex = if (resourceIds.isEmpty()) 0 else index % heroQuotes.size
'''
new = '''    val assetImages = remember {
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
if old not in text:
    raise SystemExit('resource loader block not found')
text = text.replace(old, new)
text = text.replace('if (resourceIds.isNotEmpty()) {', 'if (assetImages.isNotEmpty()) {', 1)
text = text.replace('targetState = index.coerceIn(0, resourceIds.lastIndex),', 'targetState = index.coerceIn(0, assetImages.lastIndex),', 1)
text = text.replace('''                    Image(
                        painter = painterResource(resourceIds[visibleIndex]),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )''', '''                    Image(
                        bitmap = assetImages[visibleIndex],
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )''', 1)
text = text.replace('if (resourceIds.size > 1) {', 'if (assetImages.size > 1) {', 1)
text = text.replace('(resourceIds.indices).forEach { dot ->', '(assetImages.indices).forEach { dot ->', 1)

path.write_text(text)
