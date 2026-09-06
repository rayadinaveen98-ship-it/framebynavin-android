package com.pianostudio.alpha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun R2LandscapeScaffold(
    title: String,
    subtitle: String,
    progress: Float?,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    instruction: @Composable () -> Unit,
    keyboard: @Composable () -> Unit,
    footerLeft: String,
    footerCenter: String,
    footerRight: String,
    footerRightEmphasis: Boolean = false,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(R2Black).systemBarsPadding(),
    ) {
        val compact = maxHeight < 390.dp
        val headerHeight = if (compact) 48.dp else 54.dp
        val infoHeight = if (compact) 82.dp else (maxHeight * .23f).coerceIn(88.dp, 116.dp)
        val footerHeight = if (compact) 40.dp else 44.dp

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(headerHeight).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = R2White)
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = R2White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = R2Muted, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { actions() }
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = R2Gold,
                    trackColor = R2Raised,
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth().height(infoHeight).padding(horizontal = 12.dp, vertical = 8.dp),
                color = R2Charcoal,
                shape = RoundedCornerShape(18.dp),
            ) {
                Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 12.dp)) { instruction() }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) { keyboard() }
            Row(
                modifier = Modifier.fillMaxWidth().height(footerHeight).background(R2Charcoal).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(footerLeft, color = R2Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 1)
                Surface(color = R2Carbon, shape = RoundedCornerShape(14.dp)) {
                    Text(footerCenter, color = R2White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), maxLines = 1)
                }
                Text(
                    footerRight,
                    color = if (footerRightEmphasis) R2Error else R2Muted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun R2ActionChip(label: String, onClick: () -> Unit, highlighted: Boolean = false) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        modifier = Modifier.height(34.dp),
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = if (highlighted) R2Gold.copy(alpha = .15f) else R2Carbon,
            labelColor = if (highlighted) R2Gold else R2White,
        ),
        border = null,
    )
}

private data class R2Key(val midi: Int, val rect: Rect, val black: Boolean)

@Composable
fun R2ResponsiveKeyboard(
    modifier: Modifier = Modifier.fillMaxSize(),
    pressed: Set<Int>,
    targets: Set<Int> = emptySet(),
    centerMidi: Int? = null,
    onTouchState: (Set<Int>) -> Unit,
    onEvent: (Int, Boolean) -> Unit,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(modifier = modifier.background(Color(0xFF090908))) {
        val whiteCount = when {
            maxWidth < 680.dp -> 9
            maxWidth < 820.dp -> 11
            maxWidth < 980.dp -> 13
            else -> 15
        }
        val targetCenter = centerMidi ?: targets.firstOrNull() ?: 60

        Canvas(
            modifier = Modifier.fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .pointerInput(canvasSize, whiteCount, targetCenter) {
                    awaitEachGesture {
                        var active = mutableMapOf<PointerId, Int>()
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val keys = r2Geometry(this.size.width.toFloat(), this.size.height.toFloat(), whiteCount, targetCenter)
                            val next = mutableMapOf<PointerId, Int>()
                            event.changes.forEach { change ->
                                if (change.pressed) {
                                    r2Hit(change.position, keys)?.let { next[change.id] = it }
                                    change.consume()
                                }
                            }
                            val before = active.values.groupingBy { it }.eachCount()
                            val after = next.values.groupingBy { it }.eachCount()
                            (before.keys + after.keys).forEach { midi ->
                                val was = before[midi] ?: 0
                                val now = after[midi] ?: 0
                                if (was == 0 && now > 0) onEvent(midi, true)
                                else if (was > 0 && now == 0) onEvent(midi, false)
                            }
                            active = next
                            onTouchState(active.values.toSet())
                            if (event.changes.none { it.pressed }) {
                                onTouchState(emptySet())
                                break
                            }
                        }
                    }
                },
        ) {
            val keys = r2Geometry(size.width, size.height, whiteCount, targetCenter)
            drawRect(Color(0xFF090908))
            keys.filter { !it.black }.forEach { drawR2WhiteKey(it, it.midi in pressed, it.midi in targets) }
            keys.filter { it.black }.forEach { drawR2BlackKey(it, it.midi in pressed, it.midi in targets) }
        }
    }
}

private fun r2Black(midi: Int): Boolean = (midi % 12 + 12) % 12 in setOf(1, 3, 6, 8, 10)

private fun r2StartWhite(centerMidi: Int, whiteCount: Int): Int {
    var center = centerMidi.coerceIn(24, 108)
    while (r2Black(center)) center--
    var start = center
    repeat(whiteCount / 2) {
        start--
        while (r2Black(start)) start--
    }
    return start.coerceAtLeast(21)
}

private fun r2Geometry(width: Float, height: Float, whiteCount: Int, centerMidi: Int): List<R2Key> {
    if (width <= 0f || height <= 0f) return emptyList()
    val start = r2StartWhite(centerMidi, whiteCount)
    val whiteWidth = width / whiteCount
    val whites = mutableListOf<R2Key>()
    val lefts = mutableMapOf<Int, Float>()
    var whiteIndex = 0
    var midi = start
    while (whiteIndex < whiteCount && midi <= 127) {
        if (!r2Black(midi)) {
            val left = whiteIndex * whiteWidth
            lefts[midi] = left
            whites += R2Key(midi, Rect(left, 0f, left + whiteWidth, height), false)
            whiteIndex++
        }
        midi++
    }
    if (whites.isEmpty()) return emptyList()
    val end = whites.last().midi
    val blacks = (start..end).filter(::r2Black).mapNotNull { note ->
        val previous = (note - 1 downTo start).firstOrNull { lefts.containsKey(it) } ?: return@mapNotNull null
        val center = lefts.getValue(previous) + whiteWidth
        val blackWidth = whiteWidth * .60f
        R2Key(note, Rect(center - blackWidth / 2f, 0f, center + blackWidth / 2f, height * .62f), true)
    }
    return whites + blacks
}

private fun r2Hit(position: Offset, keys: List<R2Key>): Int? =
    keys.firstOrNull { it.black && it.rect.contains(position) }?.midi
        ?: keys.firstOrNull { !it.black && it.rect.contains(position) }?.midi

private fun DrawScope.drawR2WhiteKey(key: R2Key, pressed: Boolean, target: Boolean) {
    val fill = when {
        pressed -> Color(0xFFE4CB8D)
        target -> Color(0xFFF1E1B5)
        else -> R2Ivory
    }
    val inset = 1.1f
    val width = (key.rect.width - inset * 2f).coerceAtLeast(1f)
    val height = (key.rect.height - 3f).coerceAtLeast(1f)
    drawRoundRect(fill, key.rect.topLeft + Offset(inset, 0f), androidx.compose.ui.geometry.Size(width, height), CornerRadius(4f, 4f))
    drawRoundRect(Color(0xFF6F6A61), key.rect.topLeft + Offset(inset, 0f), androidx.compose.ui.geometry.Size(width, height), CornerRadius(4f, 4f), style = Stroke(1f))
    if (target) {
        drawCircle(R2Gold, radius = 6f, center = Offset(key.rect.center.x, key.rect.bottom - 22f))
        drawLine(R2Gold.copy(alpha = .55f), Offset(key.rect.left + 4f, key.rect.bottom - 4f), Offset(key.rect.right - 4f, key.rect.bottom - 4f), strokeWidth = 3f)
    }
}

private fun DrawScope.drawR2BlackKey(key: R2Key, pressed: Boolean, target: Boolean) {
    val fill = when {
        pressed -> R2Gold
        target -> Color(0xFF7D6845)
        else -> Color(0xFF151512)
    }
    val width = (key.rect.width - 2f).coerceAtLeast(1f)
    val height = (key.rect.height - 2f).coerceAtLeast(1f)
    drawRoundRect(fill, key.rect.topLeft + Offset(1f, 0f), androidx.compose.ui.geometry.Size(width, height), CornerRadius(5f, 5f))
    if (!pressed) {
        drawLine(Color.White.copy(alpha = .10f), Offset(key.rect.left + 4f, key.rect.top + 2f), Offset(key.rect.right - 4f, key.rect.top + 2f), strokeWidth = 1.5f)
    }
}
