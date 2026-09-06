package com.pianostudio.alpha

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.max

private val LearnBlack = Color(0xFF11110F)
private val LearnCharcoal = Color(0xFF1B1B18)
private val LearnCarbon = Color(0xFF24231F)
private val LearnIvory = Color(0xFFF6F1E7)
private val LearnWhite = Color(0xFFFFFDF8)
private val LearnGold = Color(0xFFC6A768)
private val LearnMuted = Color(0xFFAAA69E)
private val LearnSuccess = Color(0xFF72A67C)
private val LearnError = Color(0xFFC66C64)

class LearningActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = LearnGold,
                    onPrimary = LearnBlack,
                    background = LearnBlack,
                    onBackground = LearnWhite,
                    surface = LearnCharcoal,
                    onSurface = LearnWhite,
                    surfaceVariant = LearnCarbon,
                    onSurfaceVariant = LearnMuted,
                    error = LearnError,
                ),
            ) { LearningApp() }
        }
    }
}

private data class LearnMidiDevice(val id: Int, val name: String, val outputs: Int)
private data class LearnMidiEvent(val midi: Int, val pressed: Boolean, val velocity: Int, val token: Long)
private data class LearnKey(val midi: Int, val rect: Rect, val black: Boolean)

private class LessonAudioEngine {
    private var started = false

    fun start(): Boolean {
        if (!started) started = nativeStart()
        return started
    }

    fun stop() {
        if (started) {
            nativeAllNotesOff()
            nativeStop()
            started = false
        }
    }

    fun noteOn(midi: Int, velocity: Int) {
        if (started) nativeNoteOn(midi.coerceIn(0, 127), velocity.coerceIn(1, 127))
    }

    fun noteOff(midi: Int) {
        if (started) nativeNoteOff(midi.coerceIn(0, 127))
    }

    fun allOff() {
        if (started) nativeAllNotesOff()
    }

    private external fun nativeStart(): Boolean
    private external fun nativeStop()
    private external fun nativeNoteOn(midi: Int, velocity: Int)
    private external fun nativeNoteOff(midi: Int)
    private external fun nativeAllNotesOff()

    companion object {
        init { System.loadLibrary("pianostudio_audio") }
    }
}

private class LearnMidiManager(context: Context) : AutoCloseable {
    interface Listener {
        fun onDevices(value: List<LearnMidiDevice>)
        fun onConnected(value: LearnMidiDevice?)
        fun onNote(value: LearnMidiEvent)
    }

    private val manager = context.getSystemService(MidiManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var listener: Listener? = null
    private var device: MidiDevice? = null
    private var port: MidiOutputPort? = null
    private var connected: LearnMidiDevice? = null
    private var runningStatus = 0
    private var firstData = -1
    private var token = 0L

    private val receiver = object : MidiReceiver() {
        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            parse(data, offset, count)
        }
    }

    private val callback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) = publish()
        override fun onDeviceStatusChanged(status: MidiDeviceStatus) = publish()
        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            if (device.id == connected?.id) disconnect()
            publish()
        }
    }

    fun start(value: Listener) {
        listener = value
        runCatching { manager.registerDeviceCallback(callback, handler) }
        publish()
    }

    private fun summaries(): List<LearnMidiDevice> = runCatching {
        manager.devices.map { info ->
            val outputs = info.ports.count { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }
            val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
                ?: "MIDI device ${info.id}"
            LearnMidiDevice(info.id, name, outputs)
        }.filter { it.outputs > 0 }.sortedBy { it.name.lowercase() }
    }.getOrDefault(emptyList())

    private fun publish() {
        listener?.onDevices(summaries())
    }

    fun connect(id: Int) {
        val info = runCatching { manager.devices.firstOrNull { it.id == id } }.getOrNull() ?: return
        disconnect()
        manager.openDevice(info, { opened ->
            if (opened == null) return@openDevice
            val outputInfo = info.ports.firstOrNull { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }
                ?: return@openDevice
            val openedPort = opened.openOutputPort(outputInfo.portNumber) ?: return@openDevice
            openedPort.connect(receiver)
            device = opened
            port = openedPort
            connected = summaries().firstOrNull { it.id == id } ?: LearnMidiDevice(id, "MIDI piano", 1)
            listener?.onConnected(connected)
        }, handler)
    }

    fun disconnect() {
        runCatching { port?.disconnect(receiver) }
        runCatching { port?.close() }
        runCatching { device?.close() }
        port = null
        device = null
        connected = null
        listener?.onConnected(null)
    }

    private fun emit(midi: Int, pressed: Boolean, velocity: Int) {
        token += 1
        val event = LearnMidiEvent(midi, pressed, velocity, token)
        handler.post { listener?.onNote(event) }
    }

    private fun parse(data: ByteArray, offset: Int, count: Int) {
        for (index in offset until (offset + count).coerceAtMost(data.size)) {
            val value = data[index].toInt() and 0xff
            if (value >= 0xf8) continue
            if (value and 0x80 != 0) {
                runningStatus = value
                firstData = -1
                continue
            }
            val type = runningStatus and 0xf0
            if (type !in listOf(0x80, 0x90, 0xa0, 0xb0, 0xe0)) continue
            if (firstData < 0) {
                firstData = value
                continue
            }
            val d1 = firstData
            val d2 = value
            firstData = -1
            when (type) {
                0x80 -> emit(d1, false, d2)
                0x90 -> emit(d1, d2 > 0, d2)
            }
        }
    }

    override fun close() {
        disconnect()
        runCatching { manager.unregisterDeviceCallback(callback) }
        listener = null
    }
}

@Composable
private fun LearningApp() {
    val context = LocalContext.current
    val progressStore = remember { LessonProgressStore(context.applicationContext) }
    val midi = remember { LearnMidiManager(context.applicationContext) }
    val progress by progressStore.progress.collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()

    var selectedLessonId by remember { mutableStateOf<String?>(null) }
    var devices by remember { mutableStateOf<List<LearnMidiDevice>>(emptyList()) }
    var connected by remember { mutableStateOf<LearnMidiDevice?>(null) }
    var midiEvent by remember { mutableStateOf<LearnMidiEvent?>(null) }

    DisposableEffect(midi) {
        midi.start(object : LearnMidiManager.Listener {
            override fun onDevices(value: List<LearnMidiDevice>) { devices = value }
            override fun onConnected(value: LearnMidiDevice?) { connected = value }
            override fun onNote(value: LearnMidiEvent) { midiEvent = value }
        })
        onDispose { midi.close() }
    }

    val selected = selectedLessonId?.let(LessonCatalog::byId)
    if (selected == null) {
        LearningDashboard(
            progress = progress,
            devices = devices,
            connected = connected,
            onConnect = midi::connect,
            onDisconnect = midi::disconnect,
            onLesson = { selectedLessonId = it.id },
            onFreePiano = { context.startActivity(Intent(context, MainActivity::class.java)) },
        )
    } else {
        LessonPlayer(
            lesson = selected,
            savedProgress = progress[selected.id] ?: LessonProgress(),
            midiEvent = midiEvent,
            connected = connected,
            onBack = { selectedLessonId = null },
            onSave = { lesson, step, complete, accuracy ->
                scope.launch { progressStore.save(lesson, step, complete, accuracy) }
            },
        )
    }
}

@Composable
private fun LearningDashboard(
    progress: Map<String, LessonProgress>,
    devices: List<LearnMidiDevice>,
    connected: LearnMidiDevice?,
    onConnect: (Int) -> Unit,
    onDisconnect: () -> Unit,
    onLesson: (PianoLesson) -> Unit,
    onFreePiano: () -> Unit,
) {
    val lessons = LessonCatalog.foundations
    val completedCount = lessons.count { progress[it.id]?.completed == true }
    val nextLesson = lessons.firstOrNull { progress[it.id]?.completed != true }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LearnBlack).systemBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("PIANO STUDIO", color = LearnGold, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Text("Learn the piano.", color = LearnWhite, style = MaterialTheme.typography.displaySmall)
            Text("Alpha 0.2 · Learning Engine", color = LearnMuted)
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = LearnCharcoal), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.School, null, tint = LearnGold, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Foundations", style = MaterialTheme.typography.titleLarge)
                            Text("$completedCount of ${lessons.size} lessons complete", color = LearnMuted)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { if (lessons.isEmpty()) 0f else completedCount.toFloat() / lessons.size.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    nextLesson?.let { lesson ->
                        Button(onClick = { onLesson(lesson) }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                            val inProgress = (progress[lesson.id]?.stepIndex ?: 0) > 0
                            Text(if (inProgress) "Continue ${lesson.title}" else "Start ${lesson.title}")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("FIRST COURSE", color = LearnGold, style = MaterialTheme.typography.labelMedium)
                AssistChip(
                    onClick = {
                        if (connected != null) onDisconnect()
                        else devices.firstOrNull()?.let { onConnect(it.id) }
                    },
                    label = { Text(connected?.name ?: if (devices.isEmpty()) "MIDI optional" else "Connect MIDI") },
                    leadingIcon = {
                        Icon(
                            if (connected != null) Icons.Rounded.CheckCircle else Icons.Rounded.Usb,
                            null,
                            Modifier.size(16.dp),
                        )
                    },
                )
            }
        }

        itemsIndexed(lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
            val lessonProgress = progress[lesson.id] ?: LessonProgress()
            val unlocked = index == 0 || progress[lessons[index - 1].id]?.completed == true
            LessonRow(lesson, lessonProgress, unlocked) { onLesson(lesson) }
        }

        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onFreePiano, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                Icon(Icons.Rounded.Piano, null)
                Spacer(Modifier.width(8.dp))
                Text("Free piano & practice tools")
            }
            Spacer(Modifier.height(20.dp))
            Text("Offline-first · no ads · MIDI optional", color = LearnMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LessonRow(
    lesson: PianoLesson,
    progress: LessonProgress,
    unlocked: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = unlocked, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (unlocked) LearnCarbon else LearnCharcoal.copy(alpha = .55f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = when {
                    progress.completed -> Icons.Rounded.CheckCircle
                    !unlocked -> Icons.Rounded.Lock
                    else -> Icons.Rounded.MusicNote
                },
                contentDescription = null,
                tint = when {
                    progress.completed -> LearnSuccess
                    unlocked -> LearnGold
                    else -> LearnMuted
                },
                modifier = Modifier.size(32.dp),
            )
            Column {
                Text(lesson.title, style = MaterialTheme.typography.titleMedium, color = if (unlocked) LearnWhite else LearnMuted)
                Text(lesson.subtitle, color = LearnMuted, style = MaterialTheme.typography.bodySmall)
                val status = when {
                    progress.completed -> "Complete · best ${progress.bestAccuracy}%"
                    progress.stepIndex > 0 -> "Continue · step ${progress.stepIndex + 1}/${lesson.steps.size}"
                    else -> "${lesson.minutes} min"
                }
                Text(status, color = if (progress.completed) LearnSuccess else LearnMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LessonPlayer(
    lesson: PianoLesson,
    savedProgress: LessonProgress,
    midiEvent: LearnMidiEvent?,
    connected: LearnMidiDevice?,
    onBack: () -> Unit,
    onSave: (PianoLesson, Int, Boolean, Int) -> Unit,
) {
    val startStep = if (savedProgress.completed) 0 else savedProgress.stepIndex.coerceAtMost(lesson.steps.lastIndex)
    val runtime = remember(lesson.id, savedProgress.completed) { LessonRuntime(lesson, startStep) }
    val audio = remember { LessonAudioEngine() }

    var revision by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("Listen, then play when you're ready.") }
    var feedbackGood by remember { mutableStateOf<Boolean?>(null) }
    var touchNotes by remember { mutableStateOf(setOf<Int>()) }
    var midiNotes by remember { mutableStateOf(setOf<Int>()) }
    var completed by remember { mutableStateOf(false) }
    var audioReady by remember { mutableStateOf(false) }
    var lastToken by remember { mutableLongStateOf(-1L) }

    DisposableEffect(audio) {
        audioReady = audio.start()
        onDispose {
            audio.allOff()
            audio.stop()
        }
    }

    fun applyResult(result: LessonInputResult) {
        if (result.feedback.isNotBlank()) feedback = result.feedback
        feedbackGood = result.correct
        revision += 1
        if (result.stepCompleted || result.lessonCompleted) {
            onSave(lesson, runtime.stepIndex, result.lessonCompleted, runtime.accuracy)
        }
        if (result.lessonCompleted) completed = true
    }

    fun processNote(midi: Int, pressed: Boolean, velocity: Int) {
        if (pressed) audio.noteOn(midi, max(1, velocity)) else audio.noteOff(midi)
        applyResult(runtime.onNote(midi, pressed))
    }

    LaunchedEffect(midiEvent?.token) {
        val event = midiEvent ?: return@LaunchedEffect
        if (event.token == lastToken) return@LaunchedEffect
        lastToken = event.token
        midiNotes = if (event.pressed) midiNotes + event.midi else midiNotes - event.midi
        processNote(event.midi, event.pressed, event.velocity)
    }

    BackHandler { onBack() }
    @Suppress("UNUSED_VARIABLE") val observeRuntime = revision
    val step = runtime.currentStep

    Column(Modifier.fillMaxSize().background(LearnBlack).systemBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
            Column {
                Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (completed) "Lesson complete" else "Step ${(runtime.stepIndex + 1).coerceAtMost(lesson.steps.size)} of ${lesson.steps.size}",
                    color = LearnMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (connected != null) {
                Spacer(Modifier.width(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("MIDI") },
                    leadingIcon = { Icon(Icons.Rounded.CheckCircle, null, Modifier.size(14.dp)) },
                )
            }
        }

        LinearProgressIndicator(
            progress = { if (completed) 1f else runtime.stepIndex.toFloat() / lesson.steps.size.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )

        if (completed) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(50.dp))
                Icon(Icons.Rounded.CheckCircle, null, tint = LearnSuccess, modifier = Modifier.size(68.dp))
                Text("Lesson complete", style = MaterialTheme.typography.headlineMedium)
                Text(lesson.title, color = LearnGold, style = MaterialTheme.typography.titleLarge)
                Text("${runtime.accuracy}% input accuracy", color = LearnMuted)
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                    Text("Back to Foundations")
                }
            }
            return@Column
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(step?.title.orEmpty(), style = MaterialTheme.typography.headlineSmall)
            Text(step?.instruction.orEmpty(), color = LearnWhite, style = MaterialTheme.typography.bodyLarge)
            if (!step?.hint.isNullOrBlank()) {
                Text(step?.hint.orEmpty(), color = LearnMuted, style = MaterialTheme.typography.bodySmall)
            }

            if (step?.kind != LessonStepKind.INFO) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    runtime.currentExpected.forEach { midi ->
                        AssistChip(
                            onClick = {},
                            label = { Text(noteLabel(midi)) },
                            leadingIcon = { Icon(Icons.Rounded.MusicNote, null, Modifier.size(14.dp)) },
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (feedbackGood) {
                        true -> LearnSuccess.copy(alpha = .16f)
                        false -> LearnError.copy(alpha = .16f)
                        null -> LearnCarbon
                    },
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    feedback,
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    color = if (feedbackGood == false) LearnError else LearnWhite,
                )
            }

            if (step?.kind == LessonStepKind.INFO) {
                Button(
                    onClick = { applyResult(runtime.continueInfo()) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                ) { Text("Continue") }
            }
        }

        if (step?.kind != LessonStepKind.INFO) {
            Text(
                if (audioReady) "Play on screen${if (connected != null) " or your MIDI piano" else ""}" else "Audio unavailable",
                color = LearnMuted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            LessonKeyboard(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                pressed = touchNotes + midiNotes,
                targets = runtime.currentExpected.toSet(),
                onTouch = { touchNotes = it },
                onEvent = { midi, pressed -> processNote(midi, pressed, if (pressed) 100 else 0) },
            )
        } else {
            Spacer(Modifier.height(32.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Accuracy ${runtime.accuracy}%", color = LearnMuted, style = MaterialTheme.typography.labelSmall)
            Text("Mistakes ${runtime.mistakes}", color = if (runtime.mistakes > 0) LearnError else LearnMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LessonKeyboard(
    modifier: Modifier,
    pressed: Set<Int>,
    targets: Set<Int>,
    onTouch: (Set<Int>) -> Unit,
    onEvent: (Int, Boolean) -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun isBlack(midi: Int): Boolean = (midi % 12 + 12) % 12 in setOf(1, 3, 6, 8, 10)

    fun geometry(width: Float, height: Float): List<LearnKey> {
        if (width <= 0f || height <= 0f) return emptyList()
        val start = 48
        val whiteCount = 15
        val whiteWidth = width / whiteCount
        val whites = mutableListOf<LearnKey>()
        val whiteLeft = mutableMapOf<Int, Float>()
        var whiteIndex = 0
        var midi = start
        while (whiteIndex < whiteCount) {
            if (!isBlack(midi)) {
                val left = whiteIndex * whiteWidth
                whiteLeft[midi] = left
                whites += LearnKey(midi, Rect(left, 0f, left + whiteWidth, height), false)
                whiteIndex += 1
            }
            midi += 1
        }
        val end = whites.last().midi
        val blacks = (start..end).filter(::isBlack).mapNotNull { note ->
            val previous = (note - 1 downTo start).firstOrNull { whiteLeft.containsKey(it) } ?: return@mapNotNull null
            val center = whiteLeft.getValue(previous) + whiteWidth
            val blackWidth = whiteWidth * .62f
            LearnKey(note, Rect(center - blackWidth / 2f, 0f, center + blackWidth / 2f, height * .62f), true)
        }
        return whites + blacks
    }

    fun hit(position: Offset, keys: List<LearnKey>): Int? =
        keys.firstOrNull { it.black && it.rect.contains(position) }?.midi
            ?: keys.firstOrNull { !it.black && it.rect.contains(position) }?.midi

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
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                hit(change.position, keys)?.let { next[change.id] = it }
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
                        onTouch(active.values.toSet())
                        if (event.changes.none { it.pressed }) {
                            onTouch(emptySet())
                            break
                        }
                    }
                }
            },
    ) {
        val keys = geometry(size.width.toFloat(), size.height.toFloat())
        keys.filter { !it.black }.forEach { drawLearnWhite(it, it.midi in pressed, it.midi in targets) }
        keys.filter { it.black }.forEach { drawLearnBlack(it, it.midi in pressed, it.midi in targets) }
    }
}

private fun DrawScope.drawLearnWhite(key: LearnKey, pressed: Boolean, target: Boolean) {
    val fill = when {
        pressed -> Color(0xFFD9C99E)
        target -> Color(0xFFE9DDBB)
        else -> LearnIvory
    }
    drawRect(fill, key.rect.topLeft, key.rect.size)
    drawRect(Color(0xFF6F6B62), key.rect.topLeft, key.rect.size, style = Stroke(1f))
    if (target) drawCircle(LearnGold, 6f, Offset(key.rect.center.x, key.rect.bottom - 24f))
}

private fun DrawScope.drawLearnBlack(key: LearnKey, pressed: Boolean, target: Boolean) {
    val fill = when {
        pressed -> LearnGold
        target -> Color(0xFF806E49)
        else -> Color(0xFF171714)
    }
    drawRoundRect(fill, key.rect.topLeft, key.rect.size, CornerRadius(5f, 5f))
}
