package com.pianostudio.alpha

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiDeviceStatus
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

class LandscapeLessonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val lessonId = intent.getStringExtra(EXTRA_LESSON_ID) ?: LessonCatalog.foundations.first().id
        setContent { QuietConcertStudioTheme { LandscapeLesson(lessonId = lessonId, onBack = ::finish) } }
    }

    companion object { const val EXTRA_LESSON_ID = "lesson_id" }
}

private data class StudioMidiEvent(val midi: Int, val pressed: Boolean, val velocity: Int, val token: Long)
private data class StudioMidiDevice(val id: Int, val name: String)
private data class StudioKey(val midi: Int, val rect: Rect, val black: Boolean)

private class StudioLessonAudioEngine {
    private var started = false
    fun start(): Boolean { if (!started) started = nativeStart(); return started }
    fun stop() { if (started) { nativeAllNotesOff(); nativeStop(); started = false } }
    fun noteOn(midi: Int, velocity: Int = 100) { if (started) nativeNoteOn(midi.coerceIn(0,127), velocity.coerceIn(1,127)) }
    fun noteOff(midi: Int) { if (started) nativeNoteOff(midi.coerceIn(0,127)) }
    fun allOff() { if (started) nativeAllNotesOff() }
    private external fun nativeStart(): Boolean
    private external fun nativeStop()
    private external fun nativeNoteOn(midi: Int, velocity: Int)
    private external fun nativeNoteOff(midi: Int)
    private external fun nativeAllNotesOff()
    companion object { init { System.loadLibrary("pianostudio_audio") } }
}

private class StudioLessonMidi(context: Context) : AutoCloseable {
    private val manager = context.getSystemService(MidiManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var device: MidiDevice? = null
    private var port: MidiOutputPort? = null
    private var status = 0
    private var d1 = -1
    private var token = 0L
    var onDevices: (List<StudioMidiDevice>) -> Unit = {}
    var onConnected: (StudioMidiDevice?) -> Unit = {}
    var onEvent: (StudioMidiEvent) -> Unit = {}

    private val receiver = object : MidiReceiver() {
        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            for (i in offset until (offset + count).coerceAtMost(data.size)) {
                val v = data[i].toInt() and 0xff
                if (v >= 0xf8) continue
                if (v and 0x80 != 0) { status = v; d1 = -1; continue }
                val type = status and 0xf0
                if (type !in listOf(0x80, 0x90, 0xa0, 0xb0, 0xe0)) continue
                if (d1 < 0) { d1 = v; continue }
                val first = d1; val second = v; d1 = -1
                if (type == 0x80 || type == 0x90) {
                    token++
                    val pressed = type == 0x90 && second > 0
                    val event = StudioMidiEvent(first, pressed, second, token)
                    handler.post { onEvent(event) }
                }
            }
        }
    }
    private val callback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) = publish()
        override fun onDeviceRemoved(device: MidiDeviceInfo) = publish()
        override fun onDeviceStatusChanged(status: MidiDeviceStatus) = publish()
    }

    fun start() { runCatching { manager.registerDeviceCallback(callback, handler) }; publish() }
    private fun devices(): List<StudioMidiDevice> = runCatching {
        manager.devices.mapNotNull { info ->
            if (info.ports.none { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }) return@mapNotNull null
            val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
                ?: "MIDI ${info.id}"
            StudioMidiDevice(info.id, name)
        }
    }.getOrDefault(emptyList())
    private fun publish() { onDevices(devices()) }
    fun connect(id: Int) {
        val info = runCatching { manager.devices.firstOrNull { it.id == id } }.getOrNull() ?: return
        disconnect()
        manager.openDevice(info, { opened ->
            if (opened == null) return@openDevice
            val pInfo = info.ports.firstOrNull { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT } ?: return@openDevice
            val openedPort = opened.openOutputPort(pInfo.portNumber) ?: return@openDevice
            openedPort.connect(receiver); device = opened; port = openedPort
            onConnected(devices().firstOrNull { it.id == id } ?: StudioMidiDevice(id, "MIDI piano"))
        }, handler)
    }
    fun disconnect() { runCatching { port?.disconnect(receiver) }; runCatching { port?.close() }; runCatching { device?.close() }; port = null; device = null; onConnected(null) }
    override fun close() { disconnect(); runCatching { manager.unregisterDeviceCallback(callback) } }
}

@Composable
private fun LandscapeLesson(lessonId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val lesson = remember(lessonId) { LessonCatalog.byId(lessonId) ?: LessonCatalog.foundations.first() }
    val store = remember { LessonProgressStore(context.applicationContext) }
    val allProgress by store.progress.collectAsState(initial = emptyMap())
    val saved = allProgress[lesson.id] ?: LessonProgress()
    val runtime = remember(lesson.id, saved.completed) {
        LessonRuntime(lesson, if (saved.completed) 0 else saved.stepIndex.coerceAtMost(lesson.steps.lastIndex))
    }
    val audio = remember { StudioLessonAudioEngine() }
    val midi = remember { StudioLessonMidi(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var revision by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("Play when you're ready.") }
    var feedbackGood by remember { mutableStateOf<Boolean?>(null) }
    var completed by remember { mutableStateOf(false) }
    var touchNotes by remember { mutableStateOf(setOf<Int>()) }
    var midiNotes by remember { mutableStateOf(setOf<Int>()) }
    var devices by remember { mutableStateOf<List<StudioMidiDevice>>(emptyList()) }
    var connected by remember { mutableStateOf<StudioMidiDevice?>(null) }
    var lastMidiToken by remember { mutableLongStateOf(-1L) }
    var audioReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        audioReady = audio.start()
        midi.onDevices = { devices = it }
        midi.onConnected = { connected = it }
        midi.onEvent = { event ->
            if (event.token != lastMidiToken) {
                lastMidiToken = event.token
                midiNotes = if (event.pressed) midiNotes + event.midi else midiNotes - event.midi
                if (event.pressed) audio.noteOn(event.midi, max(event.velocity, 1)) else audio.noteOff(event.midi)
                val result = runtime.onNote(event.midi, event.pressed)
                feedback = studioFeedback(result, runtime)
                feedbackGood = result.correct
                if (result.stepCompleted || result.lessonCompleted) scope.launch { store.save(lesson, runtime.stepIndex, result.lessonCompleted, runtime.accuracy) }
                if (result.lessonCompleted) completed = true
                revision++
            }
        }
        midi.start()
        onDispose { audio.stop(); midi.close() }
    }

    fun apply(result: LessonInputResult) {
        feedback = studioFeedback(result, runtime)
        feedbackGood = result.correct
        if (result.stepCompleted || result.lessonCompleted) scope.launch { store.save(lesson, runtime.stepIndex, result.lessonCompleted, runtime.accuracy) }
        if (result.lessonCompleted) completed = true
        revision++
    }

    fun playTouch(note: Int, pressed: Boolean) {
        if (pressed) audio.noteOn(note) else audio.noteOff(note)
        apply(runtime.onNote(note, pressed))
    }

    fun demo() {
        val targets = runtime.currentExpected.ifEmpty { runtime.currentStep?.targets?.flatten().orEmpty() }.distinct()
        if (targets.isEmpty()) return
        scope.launch {
            audio.allOff()
            val chord = runtime.currentStep?.kind == LessonStepKind.CHORD
            if (chord) {
                targets.forEach { audio.noteOn(it, 88) }; delay(650); targets.forEach(audio::noteOff)
            } else {
                targets.forEach { n -> audio.noteOn(n, 88); delay(240); audio.noteOff(n); delay(90) }
            }
        }
    }

    BackHandler(onBack = onBack)
    @Suppress("UNUSED_VARIABLE") val observe = revision
    val step = runtime.currentStep
    val attempts = runtime.correctHits + runtime.mistakes

    Column(Modifier.fillMaxSize().background(StudioBlack).systemBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
            Text(lesson.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Step ${(runtime.stepIndex + 1).coerceAtMost(lesson.steps.size)} of ${lesson.steps.size}", color = StudioMuted)
            LinearProgressIndicator(
                progress = { if (completed) 1f else runtime.stepIndex.toFloat() / lesson.steps.size.toFloat() },
                modifier = Modifier.weight(1f),
            )
            AssistChip(onClick = ::demo, label = { Text("Demo") }, leadingIcon = { Icon(Icons.Rounded.PlayArrow, null, Modifier.size(16.dp)) })
            AssistChip(
                onClick = { if (connected != null) midi.disconnect() else devices.firstOrNull()?.let { midi.connect(it.id) } },
                label = { Text(connected?.let { "MIDI" } ?: if (devices.isEmpty()) "MIDI" else "Connect") },
                leadingIcon = { Icon(if (connected != null) Icons.Rounded.CheckCircle else Icons.Rounded.Usb, null, Modifier.size(16.dp)) },
            )
        }

        if (completed) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(.36f).padding(horizontal = 28.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = StudioSuccess.copy(alpha = .16f)), shape = RoundedCornerShape(24.dp)) {
                    Row(Modifier.padding(horizontal = 28.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = StudioSuccess, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Lesson complete", style = MaterialTheme.typography.headlineSmall)
                            Text(lesson.title, color = StudioGold)
                            Text("${runtime.accuracy}% input accuracy", color = StudioMuted)
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().weight(.36f).padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Card(
                    modifier = Modifier.weight(1.05f).fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = StudioCharcoal),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(step?.title.orEmpty(), color = StudioGold, style = MaterialTheme.typography.labelLarge)
                        Text(step?.instruction.orEmpty(), style = MaterialTheme.typography.headlineSmall)
                        if (!step?.hint.isNullOrBlank()) Text(step?.hint.orEmpty(), color = StudioMuted, style = MaterialTheme.typography.bodySmall)
                        if (step?.kind != LessonStepKind.INFO) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                runtime.currentExpected.forEach { n -> AssistChip(onClick = {}, label = { Text(noteLabel(n)) }, leadingIcon = { Icon(Icons.Rounded.MusicNote, null, Modifier.size(14.dp)) }) }
                            }
                        }
                        if (step?.kind == LessonStepKind.INFO) {
                            Button(onClick = { apply(runtime.continueInfo()) }, modifier = Modifier.height(46.dp)) { Text("Continue") }
                        }
                    }
                }
                Card(
                    modifier = Modifier.weight(.75f).fillMaxHeight(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (feedbackGood) {
                            true -> StudioSuccess.copy(alpha = .16f)
                            false -> StudioError.copy(alpha = .12f)
                            null -> StudioCarbon
                        },
                    ),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center) {
                        Icon(
                            if (feedbackGood == true) Icons.Rounded.CheckCircle else Icons.Rounded.Headphones,
                            null,
                            tint = if (feedbackGood == false) StudioError else if (feedbackGood == true) StudioSuccess else StudioGold,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(feedback, style = MaterialTheme.typography.titleMedium)
                        if (connected != null) Text("Listening to ${connected?.name}", color = StudioMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (!completed && step?.kind != LessonStepKind.INFO) {
            StudioLessonKeyboard(
                modifier = Modifier.fillMaxWidth().weight(.54f),
                pressed = touchNotes + midiNotes,
                targets = runtime.currentExpected.toSet(),
                onTouch = { touchNotes = it },
                onEvent = ::playTouch,
            )
        } else {
            Box(Modifier.fillMaxWidth().weight(.54f), contentAlignment = Alignment.Center) {
                if (completed) Button(onClick = onBack) { Text("Back to Foundations") }
                else Text("Listen to the instruction above, then continue.", color = StudioMuted)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp).background(StudioCharcoal).padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val left = if (attempts < 3) "Guided mode" else "Accuracy ${runtime.accuracy}%"
            Text(left, color = StudioMuted)
            Text(
                when {
                    completed -> "Beautiful. Keep the sound, not the score."
                    step?.kind == LessonStepKind.INFO -> "Learn first. Play second."
                    runtime.currentExpected.isNotEmpty() -> "Play ${runtime.currentExpected.joinToString(" + ") { noteLabel(it) }}"
                    else -> "Play when you're ready"
                },
                color = StudioWhite,
                fontWeight = FontWeight.Medium,
            )
            Text(if (attempts < 3) "Explore freely" else "Mistakes ${runtime.mistakes}", color = if (attempts >= 3 && runtime.mistakes > 0) StudioError else StudioMuted)
        }
    }
}

private fun studioFeedback(result: LessonInputResult, runtime: LessonRuntime): String = when {
    result.lessonCompleted -> "Beautiful — lesson complete."
    result.correct == true && result.stepCompleted -> "Great — you have it."
    result.correct == true && runtime.currentExpected.isNotEmpty() -> "Nice. Now add ${runtime.currentExpected.joinToString(" + ") { noteLabel(it) }}."
    result.correct == false && result.expected.isNotEmpty() -> "Almost — try ${result.expected.joinToString(" + ") { noteLabel(it) }}."
    result.feedback.isNotBlank() -> result.feedback
    else -> "Play when you're ready."
}

@Composable
private fun StudioLessonKeyboard(
    modifier: Modifier,
    pressed: Set<Int>,
    targets: Set<Int>,
    onTouch: (Set<Int>) -> Unit,
    onEvent: (Int, Boolean) -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    fun black(midi: Int) = (midi % 12 + 12) % 12 in setOf(1,3,6,8,10)
    fun geometry(width: Float, height: Float): List<StudioKey> {
        if (width <= 0f || height <= 0f) return emptyList()
        val start = 48
        val whiteCount = 22
        val ww = width / whiteCount
        val whites = mutableListOf<StudioKey>()
        val lefts = mutableMapOf<Int, Float>()
        var wi = 0; var midi = start
        while (wi < whiteCount) {
            if (!black(midi)) { val l = wi * ww; lefts[midi] = l; whites += StudioKey(midi, Rect(l,0f,l+ww,height),false); wi++ }
            midi++
        }
        val end = whites.last().midi
        val blacks = (start..end).filter(::black).mapNotNull { n ->
            val prev = (n-1 downTo start).firstOrNull { lefts.containsKey(it) } ?: return@mapNotNull null
            val center = lefts.getValue(prev) + ww
            val bw = ww * .62f
            StudioKey(n, Rect(center-bw/2f,0f,center+bw/2f,height*.62f),true)
        }
        return whites + blacks
    }
    fun hit(p: Offset, keys: List<StudioKey>): Int? = keys.firstOrNull { it.black && it.rect.contains(p) }?.midi ?: keys.firstOrNull { !it.black && it.rect.contains(p) }?.midi

    Canvas(
        modifier = modifier
            .onSizeChanged { size = it }
            .pointerInput(size) {
                awaitEachGesture {
                    var active = mutableMapOf<PointerId, Int>()
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val keys = geometry(this.size.width.toFloat(), this.size.height.toFloat())
                        val next = mutableMapOf<PointerId, Int>()
                        event.changes.forEach { change -> if (change.pressed) { hit(change.position, keys)?.let { next[change.id] = it }; change.consume() } }
                        val before = active.values.groupingBy { it }.eachCount(); val after = next.values.groupingBy { it }.eachCount()
                        (before.keys + after.keys).forEach { n ->
                            val was = before[n] ?: 0; val now = after[n] ?: 0
                            if (was == 0 && now > 0) onEvent(n, true) else if (was > 0 && now == 0) onEvent(n, false)
                        }
                        active = next; onTouch(active.values.toSet())
                        if (event.changes.none { it.pressed }) { onTouch(emptySet()); break }
                    }
                }
            },
    ) {
        val keys = geometry(size.width.toFloat(), size.height.toFloat())
        drawRect(Color(0xFF090908))
        keys.filter { !it.black }.forEach { drawStudioWhiteKey(it, it.midi in pressed, it.midi in targets) }
        keys.filter { it.black }.forEach { drawStudioBlackKey(it, it.midi in pressed, it.midi in targets) }
    }
}

private fun DrawScope.drawStudioWhiteKey(key: StudioKey, pressed: Boolean, target: Boolean) {
    val fill = when { pressed -> Color(0xFFE5C57B); target -> Color(0xFFF0DDA7); else -> StudioIvory }
    drawRoundRect(fill, key.rect.topLeft + Offset(1.2f,0f), key.rect.size - androidx.compose.ui.geometry.Size(2.4f,3f), CornerRadius(4f,4f))
    drawRoundRect(Color(0xFF777169), key.rect.topLeft + Offset(1.2f,0f), key.rect.size - androidx.compose.ui.geometry.Size(2.4f,3f), CornerRadius(4f,4f), style = Stroke(1f))
    if (target) drawCircle(StudioGold, radius = 5.5f, center = Offset(key.rect.center.x, key.rect.bottom - 24f))
}

private fun DrawScope.drawStudioBlackKey(key: StudioKey, pressed: Boolean, target: Boolean) {
    val fill = when { pressed -> StudioGold; target -> Color(0xFF776342); else -> Color(0xFF131311) }
    drawRoundRect(fill, key.rect.topLeft + Offset(1f,0f), key.rect.size - androidx.compose.ui.geometry.Size(2f,2f), CornerRadius(6f,6f))
}
