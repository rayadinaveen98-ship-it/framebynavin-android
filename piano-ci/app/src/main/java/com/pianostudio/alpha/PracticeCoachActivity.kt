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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Speed
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

private val CoachBlack = Color(0xFF11110F)
private val CoachCharcoal = Color(0xFF1B1B18)
private val CoachCarbon = Color(0xFF24231F)
private val CoachIvory = Color(0xFFF6F1E7)
private val CoachWhite = Color(0xFFFFFDF8)
private val CoachGold = Color(0xFFC6A768)
private val CoachMuted = Color(0xFFAAA69E)
private val CoachSuccess = Color(0xFF72A67C)
private val CoachError = Color(0xFFC66C64)
private val CoachAmber = Color(0xFFD6A34B)

private val diagnosticSequence = listOf(60, 62, 64, 67, 64, 62, 60, 60, 62, 64, 67, 64, 62, 60)

class PracticeCoachActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = CoachGold,
                    onPrimary = CoachBlack,
                    background = CoachBlack,
                    onBackground = CoachWhite,
                    surface = CoachCharcoal,
                    onSurface = CoachWhite,
                    surfaceVariant = CoachCarbon,
                    onSurfaceVariant = CoachMuted,
                    error = CoachError,
                ),
            ) { PracticeCoachApp() }
        }
    }
}

private class PracticeAudioEngine {
    private var started = false

    fun start(): Boolean {
        if (!started) started = nativeStart()
        return started
    }

    fun stop() {
        if (started) {
            nativeSetMetronome(false, 80)
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

    fun metronome(enabled: Boolean, bpm: Int) {
        if (started) nativeSetMetronome(enabled, bpm.coerceIn(50, 120))
    }

    fun allOff() {
        if (started) nativeAllNotesOff()
    }

    private external fun nativeStart(): Boolean
    private external fun nativeStop()
    private external fun nativeNoteOn(midi: Int, velocity: Int)
    private external fun nativeNoteOff(midi: Int)
    private external fun nativeSetMetronome(enabled: Boolean, bpm: Int)
    private external fun nativeAllNotesOff()

    companion object {
        init { System.loadLibrary("pianostudio_audio") }
    }
}

private data class CoachMidiDevice(val id: Int, val name: String)
private data class CoachMidiEvent(val midi: Int, val pressed: Boolean, val velocity: Int, val token: Long)

private class CoachMidiManager(context: Context) : AutoCloseable {
    interface Listener {
        fun onDevices(value: List<CoachMidiDevice>)
        fun onConnected(value: CoachMidiDevice?)
        fun onNote(value: CoachMidiEvent)
    }

    private val manager = context.getSystemService(MidiManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var listener: Listener? = null
    private var device: MidiDevice? = null
    private var port: MidiOutputPort? = null
    private var connected: CoachMidiDevice? = null
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

    private fun summaries(): List<CoachMidiDevice> = runCatching {
        manager.devices.mapNotNull { info ->
            val outputCount = info.ports.count { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }
            if (outputCount == 0) return@mapNotNull null
            val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
                ?: "MIDI device ${info.id}"
            CoachMidiDevice(info.id, name)
        }.sortedBy { it.name.lowercase() }
    }.getOrDefault(emptyList())

    private fun publish() {
        listener?.onDevices(summaries())
    }

    fun connect(id: Int) {
        val info = runCatching { manager.devices.firstOrNull { it.id == id } }.getOrNull() ?: return
        disconnect()
        manager.openDevice(info, { opened ->
            if (opened == null) return@openDevice
            val output = info.ports.firstOrNull { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }
                ?: return@openDevice
            val openedPort = opened.openOutputPort(output.portNumber) ?: return@openDevice
            openedPort.connect(receiver)
            device = opened
            port = openedPort
            connected = summaries().firstOrNull { it.id == id } ?: CoachMidiDevice(id, "MIDI piano")
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
        val event = CoachMidiEvent(midi, pressed, velocity, token)
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
private fun PracticeCoachApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { PracticeCoachStore(context.applicationContext) }
    val snapshot by store.snapshot.collectAsState(initial = null)
    val midi = remember { CoachMidiManager(context.applicationContext) }
    val audio = remember { PracticeAudioEngine() }

    var screen by remember { mutableStateOf("home") }
    var devices by remember { mutableStateOf<List<CoachMidiDevice>>(emptyList()) }
    var connected by remember { mutableStateOf<CoachMidiDevice?>(null) }
    var midiEvent by remember { mutableStateOf<CoachMidiEvent?>(null) }
    var lastMidiToken by remember { mutableLongStateOf(-1L) }
    var runtime by remember { mutableStateOf<GuidedPracticeRuntime?>(null) }
    var report by remember { mutableStateOf<PracticeReport?>(null) }
    var feedback by remember { mutableStateOf("Follow the highlighted note. The target waits for you.") }
    var feedbackGood by remember { mutableStateOf<Boolean?>(null) }
    var pressedNotes by remember { mutableStateOf(setOf<Int>()) }
    var revision by remember { mutableIntStateOf(0) }
    var audioReady by remember { mutableStateOf(false) }

    DisposableEffect(audio, midi) {
        audioReady = audio.start()
        midi.start(object : CoachMidiManager.Listener {
            override fun onDevices(value: List<CoachMidiDevice>) { devices = value }
            override fun onConnected(value: CoachMidiDevice?) { connected = value }
            override fun onNote(value: CoachMidiEvent) { midiEvent = value }
        })
        onDispose {
            audio.metronome(false, 80)
            audio.allOff()
            audio.stop()
            midi.close()
        }
    }

    fun begin(sequence: List<Int>, bpm: Int, destination: String) {
        audio.allOff()
        runtime = GuidedPracticeRuntime(sequence = sequence, bpm = bpm)
        report = null
        feedback = if (destination == "diagnostic") {
            "Start with ${noteLabel(sequence.firstOrNull())}. Keep each correct note close to the click."
        } else {
            "Focused loop at $bpm BPM. The expected note stays highlighted until it is correct."
        }
        feedbackGood = null
        pressedNotes = emptySet()
        revision += 1
        screen = destination
        audio.metronome(true, bpm)
    }

    fun process(midiNote: Int, pressed: Boolean, velocity: Int) {
        val activeRuntime = runtime ?: return
        if (pressed) audio.noteOn(midiNote, max(1, velocity)) else audio.noteOff(midiNote)
        if (!pressed) return
        val result = activeRuntime.onNote(midiNote, true)
        feedback = result.feedback
        feedbackGood = result.correct
        revision += 1
        if (result.completed) {
            audio.metronome(false, activeRuntime.bpm)
            val completedReport = activeRuntime.report()
            report = completedReport
            scope.launch { store.save(completedReport) }
            screen = if (screen == "focus") "focus-report" else "report"
        }
    }

    LaunchedEffect(midiEvent?.token) {
        val event = midiEvent ?: return@LaunchedEffect
        if (event.token == lastMidiToken) return@LaunchedEffect
        lastMidiToken = event.token
        pressedNotes = if (event.pressed) pressedNotes + event.midi else pressedNotes - event.midi
        process(event.midi, event.pressed, event.velocity)
    }

    if (screen != "home") {
        BackHandler {
            audio.metronome(false, runtime?.bpm ?: 80)
            audio.allOff()
            screen = "home"
            runtime = null
        }
    }

    when (screen) {
        "home" -> CoachHome(
            snapshot = snapshot,
            devices = devices,
            connected = connected,
            audioReady = audioReady,
            onConnect = midi::connect,
            onDisconnect = midi::disconnect,
            onDiagnostic = {
                begin(diagnosticSequence, snapshot?.recommendedBpm ?: 80, "diagnostic")
            },
            onLearning = { context.startActivity(Intent(context, LearningActivity::class.java)) },
            onFreePiano = { context.startActivity(Intent(context, MainActivity::class.java)) },
        )

        "diagnostic", "focus" -> {
            @Suppress("UNUSED_VARIABLE") val observe = revision
            PracticePlayer(
                title = if (screen == "diagnostic") "Smart diagnostic" else "Weak-note focus",
                subtitle = if (screen == "diagnostic") "Practice Intelligence" else "Adaptive retry",
                runtime = runtime ?: GuidedPracticeRuntime(diagnosticSequence, 80),
                feedback = feedback,
                feedbackGood = feedbackGood,
                pressedNotes = pressedNotes,
                connected = connected,
                onBack = {
                    audio.metronome(false, runtime?.bpm ?: 80)
                    audio.allOff()
                    screen = "home"
                },
                onTouchState = { pressedNotes = it },
                onEvent = { note, isPressed -> process(note, isPressed, if (isPressed) 100 else 0) },
            )
        }

        "report", "focus-report" -> {
            val current = report
            if (current == null) {
                screen = "home"
            } else {
                PracticeReportScreen(
                    report = current,
                    wasFocus = screen == "focus-report",
                    onBack = { screen = "home" },
                    onFocus = {
                        val sequence = current.focusSequence.ifEmpty { diagnosticSequence }
                        begin(sequence, current.recommendedBpm, "focus")
                    },
                )
            }
        }
    }
}

@Composable
private fun CoachHome(
    snapshot: PracticeCoachSnapshot?,
    devices: List<CoachMidiDevice>,
    connected: CoachMidiDevice?,
    audioReady: Boolean,
    onConnect: (Int) -> Unit,
    onDisconnect: () -> Unit,
    onDiagnostic: () -> Unit,
    onLearning: () -> Unit,
    onFreePiano: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CoachBlack).systemBarsPadding(),
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("PIANO STUDIO", color = CoachGold, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Text("Practice smarter.", color = CoachWhite, style = MaterialTheme.typography.displaySmall)
            Text("Alpha 0.3 · Practice Intelligence", color = CoachMuted)
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = CoachCharcoal), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.AutoGraph, null, tint = CoachGold, modifier = Modifier.size(32.dp))
                        Column {
                            Text("Smart Practice Coach", style = MaterialTheme.typography.titleLarge)
                            Text("Detect weak notes · score timing · adapt tempo", color = CoachMuted)
                        }
                    }
                    snapshot?.let { latest ->
                        Text(
                            latest.weakestMidi?.let { "Current focus: ${noteLabel(it)} at ${latest.recommendedBpm} BPM" }
                                ?: "Your last phrase was clean at ${latest.recommendedBpm} BPM",
                            color = CoachWhite,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = {}, label = { Text("Notes ${latest.noteAccuracy}%") })
                            AssistChip(onClick = {}, label = { Text("Timing ${latest.timingScore}%") })
                        }
                    } ?: Text("Run a short diagnostic and the coach will build your first recommendation.", color = CoachMuted)
                    Button(onClick = onDiagnostic, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(if (snapshot == null) "Run smart diagnostic" else "Recheck my playing")
                    }
                }
            }
        }

        item {
            Text("INPUT", color = CoachGold, style = MaterialTheme.typography.labelMedium)
            Card(colors = CardDefaults.cardColors(containerColor = CoachCarbon), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (connected != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = CoachSuccess)
                            Text(connected.name, modifier = Modifier.padding(end = 6.dp))
                            OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
                        }
                    } else if (devices.isNotEmpty()) {
                        OutlinedButton(onClick = { onConnect(devices.first().id) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Usb, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Connect ${devices.first().name}")
                        }
                    } else {
                        Text("On-screen piano is ready. USB/Bluetooth MIDI is optional.", color = CoachMuted)
                    }
                    Text(if (audioReady) "Native practice audio ready" else "Native audio unavailable on this device", color = if (audioReady) CoachSuccess else CoachError, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        item {
            Text("KEEP LEARNING", color = CoachGold, style = MaterialTheme.typography.labelMedium)
            OutlinedButton(onClick = onLearning, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                Icon(Icons.Rounded.School, null)
                Spacer(Modifier.width(8.dp))
                Text("Foundations lessons")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onFreePiano, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                Icon(Icons.Rounded.Piano, null)
                Spacer(Modifier.width(8.dp))
                Text("Free piano & recording")
            }
            Spacer(Modifier.height(16.dp))
            Text("Offline-first · no ads · deterministic coaching", color = CoachMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PracticePlayer(
    title: String,
    subtitle: String,
    runtime: GuidedPracticeRuntime,
    feedback: String,
    feedbackGood: Boolean?,
    pressedNotes: Set<Int>,
    connected: CoachMidiDevice?,
    onBack: () -> Unit,
    onTouchState: (Set<Int>) -> Unit,
    onEvent: (Int, Boolean) -> Unit,
) {
    val progress = if (runtime.sequence.isEmpty()) 1f else runtime.index.toFloat() / runtime.sequence.size.toFloat()
    Column(Modifier.fillMaxSize().background(CoachBlack).systemBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text("$subtitle · ${runtime.bpm} BPM", color = CoachMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (connected != null) {
                AssistChip(onClick = {}, label = { Text("MIDI") }, leadingIcon = { Icon(Icons.Rounded.CheckCircle, null, Modifier.size(14.dp)) })
            }
        }

        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())

        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Listen to the click. Play the highlighted note.", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Speed, null, tint = CoachGold)
                Text("${runtime.bpm} BPM", color = CoachGold, style = MaterialTheme.typography.titleMedium)
                runtime.expected?.let { expected ->
                    AssistChip(onClick = {}, label = { Text("Target ${noteLabel(expected)}") }, leadingIcon = { Icon(Icons.Rounded.MusicNote, null, Modifier.size(14.dp)) })
                }
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (feedbackGood) {
                        true -> CoachSuccess.copy(alpha = .16f)
                        false -> CoachError.copy(alpha = .16f)
                        null -> CoachCarbon
                    },
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    feedback,
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    color = if (feedbackGood == false) CoachError else CoachWhite,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Notes ${runtime.noteAccuracy}%", color = CoachMuted, style = MaterialTheme.typography.labelSmall)
                Text("Mistakes ${runtime.mistakes}", color = if (runtime.mistakes > 0) CoachAmber else CoachMuted, style = MaterialTheme.typography.labelSmall)
                Text("${runtime.index}/${runtime.sequence.size}", color = CoachMuted, style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(Modifier.height(8.dp))
        CoachKeyboard(
            modifier = Modifier.fillMaxWidth().height(320.dp),
            pressed = pressedNotes,
            target = runtime.expected,
            onTouch = onTouchState,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun PracticeReportScreen(
    report: PracticeReport,
    wasFocus: Boolean,
    onBack: () -> Unit,
    onFocus: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CoachBlack).systemBarsPadding(),
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
            Text(if (wasFocus) "Focus loop complete" else "Practice report", color = CoachWhite, style = MaterialTheme.typography.displaySmall)
            Text(if (wasFocus) "The coach recalculated your next tempo." else "Here is what your playing actually showed.", color = CoachMuted)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("NOTES", "${report.noteAccuracy}%", Icons.Rounded.Piano)
                MetricCard("TIMING", "${report.timingScore}%", Icons.Rounded.GraphicEq)
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = CoachCharcoal), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Coach recommendation", color = CoachGold, style = MaterialTheme.typography.labelMedium)
                    Text(report.message, style = MaterialTheme.typography.titleMedium)
                    Text(
                        report.weakestMidi?.let { "Weakest target: ${noteLabel(it)}" } ?: "No recurring wrong note detected",
                        color = CoachMuted,
                    )
                    Text("Next tempo: ${report.recommendedBpm} BPM", color = CoachWhite, style = MaterialTheme.typography.titleLarge)
                    Text("${report.mistakes} incorrect press${if (report.mistakes == 1) "" else "es"} in this run", color = CoachMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Button(onClick = onFocus, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(
                    if (report.focusSequence.isNotEmpty()) {
                        "Practice ${noteLabel(report.weakestMidi)} at ${report.recommendedBpm} BPM"
                    } else {
                        "Repeat phrase at ${report.recommendedBpm} BPM"
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                Text("Finish")
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.width(150.dp),
        colors = CardDefaults.cardColors(containerColor = CoachCarbon),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = CoachGold)
            Text(label, color = CoachMuted, style = MaterialTheme.typography.labelSmall)
            Text(value, color = CoachWhite, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

private data class CoachKey(val midi: Int, val rect: Rect, val black: Boolean)

@Composable
private fun CoachKeyboard(
    modifier: Modifier,
    pressed: Set<Int>,
    target: Int?,
    onTouch: (Set<Int>) -> Unit,
    onEvent: (Int, Boolean) -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun isBlack(midi: Int): Boolean = (midi % 12 + 12) % 12 in setOf(1, 3, 6, 8, 10)

    fun geometry(width: Float, height: Float): List<CoachKey> {
        if (width <= 0f || height <= 0f) return emptyList()
        val start = 48
        val whiteCount = 15
        val whiteWidth = width / whiteCount
        val whites = mutableListOf<CoachKey>()
        val whiteLeft = mutableMapOf<Int, Float>()
        var whiteIndex = 0
        var midi = start
        while (whiteIndex < whiteCount) {
            if (!isBlack(midi)) {
                val left = whiteIndex * whiteWidth
                whiteLeft[midi] = left
                whites += CoachKey(midi, Rect(left, 0f, left + whiteWidth, height), false)
                whiteIndex += 1
            }
            midi += 1
        }
        val end = whites.last().midi
        val blacks = (start..end).filter(::isBlack).mapNotNull { note ->
            val previous = (note - 1 downTo start).firstOrNull { whiteLeft.containsKey(it) } ?: return@mapNotNull null
            val center = whiteLeft.getValue(previous) + whiteWidth
            val blackWidth = whiteWidth * .62f
            CoachKey(note, Rect(center - blackWidth / 2f, 0f, center + blackWidth / 2f, height * .62f), true)
        }
        return whites + blacks
    }

    fun hit(position: Offset, keys: List<CoachKey>): Int? =
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
        keys.filter { !it.black }.forEach { drawCoachWhite(it, it.midi in pressed, it.midi == target) }
        keys.filter { it.black }.forEach { drawCoachBlack(it, it.midi in pressed, it.midi == target) }
    }
}

private fun DrawScope.drawCoachWhite(key: CoachKey, pressed: Boolean, target: Boolean) {
    val fill = when {
        pressed -> Color(0xFFD9C99E)
        target -> Color(0xFFE9DDBB)
        else -> CoachIvory
    }
    drawRect(fill, key.rect.topLeft, key.rect.size)
    drawRect(Color(0xFF6F6B62), key.rect.topLeft, key.rect.size, style = Stroke(1f))
    if (target) drawCircle(CoachGold, 6f, Offset(key.rect.center.x, key.rect.bottom - 24f))
}

private fun DrawScope.drawCoachBlack(key: CoachKey, pressed: Boolean, target: Boolean) {
    val fill = when {
        pressed -> CoachGold
        target -> Color(0xFF806E49)
        else -> Color(0xFF171714)
    }
    drawRoundRect(fill, key.rect.topLeft, key.rect.size, CornerRadius(5f, 5f))
}
