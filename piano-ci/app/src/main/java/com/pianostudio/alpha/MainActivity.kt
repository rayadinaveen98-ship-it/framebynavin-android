package com.pianostudio.alpha

import android.content.Context
import android.media.midi.*
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

private val PianoBlack = Color(0xFF11110F)
private val Charcoal = Color(0xFF1B1B18)
private val Carbon = Color(0xFF24231F)
private val Ivory = Color(0xFFF6F1E7)
private val WarmWhite = Color(0xFFFFFDF8)
private val Champagne = Color(0xFFC6A768)
private val Muted = Color(0xFFAAA69E)
private val Success = Color(0xFF72A67C)
private val Error = Color(0xFFC66C64)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Champagne,
                    onPrimary = PianoBlack,
                    background = PianoBlack,
                    onBackground = WarmWhite,
                    surface = Charcoal,
                    onSurface = WarmWhite,
                    surfaceVariant = Carbon,
                    onSurfaceVariant = Muted,
                    error = Error,
                ),
            ) { PianoAlphaApp() }
        }
    }
}

private data class NoteEvent(val midi: Int, val pressed: Boolean, val velocity: Int = 100)
private data class MidiDeviceSummary(val id: Int, val name: String, val outputs: Int)
private data class KeyGeometry(val midi: Int, val rect: Rect, val black: Boolean)

private class PianoAudioEngine {
    private var started = false
    fun start(): Boolean { if (!started) started = nativeStart(); return started }
    fun stop() { if (started) { nativeAllNotesOff(); nativeStop(); started = false } }
    fun noteOn(midi: Int, velocity: Int) { if (started) nativeNoteOn(midi.coerceIn(0, 127), velocity.coerceIn(1, 127)) }
    fun noteOff(midi: Int) { if (started) nativeNoteOff(midi.coerceIn(0, 127)) }
    fun sustain(on: Boolean) { if (started) nativeSetSustain(on) }
    fun metronome(on: Boolean, bpm: Int) { if (started) nativeSetMetronome(on, bpm.coerceIn(40, 220)) }
    fun allOff() { if (started) nativeAllNotesOff() }
    private external fun nativeStart(): Boolean
    private external fun nativeStop()
    private external fun nativeNoteOn(midi: Int, velocity: Int)
    private external fun nativeNoteOff(midi: Int)
    private external fun nativeSetSustain(enabled: Boolean)
    private external fun nativeSetMetronome(enabled: Boolean, bpm: Int)
    private external fun nativeAllNotesOff()
    companion object { init { System.loadLibrary("pianostudio_audio") } }
}

private class MidiInputManager(context: Context) : AutoCloseable {
    interface Listener {
        fun devices(value: List<MidiDeviceSummary>)
        fun connected(value: MidiDeviceSummary?)
        fun note(value: NoteEvent)
        fun sustain(value: Boolean)
    }

    private val manager = context.getSystemService(MidiManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var listener: Listener? = null
    private var device: MidiDevice? = null
    private var port: MidiOutputPort? = null
    private var connected: MidiDeviceSummary? = null
    private var running = 0
    private var first = -1

    private val receiver = object : MidiReceiver() {
        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) = parse(data, offset, count)
    }

    private val callback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) = publish()
        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            if (device.id == connected?.id) disconnect()
            publish()
        }
        override fun onDeviceStatusChanged(status: MidiDeviceStatus) = publish()
    }

    fun start(value: Listener) {
        listener = value
        runCatching { manager.registerDeviceCallback(callback, handler) }
        publish()
    }

    private fun summaries(): List<MidiDeviceSummary> = runCatching {
        manager.devices.map { info ->
            val outputs = info.ports.count { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }
            val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
                ?: "MIDI device ${info.id}"
            MidiDeviceSummary(info.id, name, outputs)
        }.filter { it.outputs > 0 }.sortedBy { it.name.lowercase() }
    }.getOrDefault(emptyList())

    private fun publish() = listener?.devices(summaries()) ?: Unit

    fun connect(id: Int) {
        val info = runCatching { manager.devices.firstOrNull { it.id == id } }.getOrNull() ?: return
        disconnect()
        runCatching {
            manager.openDevice(info, { opened ->
                if (opened == null) return@openDevice
                val out = info.ports.firstOrNull { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT } ?: return@openDevice
                val openedPort = opened.openOutputPort(out.portNumber) ?: return@openDevice
                openedPort.connect(receiver)
                device = opened
                port = openedPort
                connected = summaries().firstOrNull { it.id == id } ?: MidiDeviceSummary(id, "MIDI piano", 1)
                listener?.connected(connected)
            }, handler)
        }
    }

    fun disconnect() {
        runCatching { port?.disconnect(receiver) }
        runCatching { port?.close() }
        runCatching { device?.close() }
        port = null
        device = null
        connected = null
        listener?.connected(null)
    }

    private fun parse(data: ByteArray, offset: Int, count: Int) {
        for (i in offset until (offset + count).coerceAtMost(data.size)) {
            val b = data[i].toInt() and 0xff
            if (b >= 0xf8) continue
            if (b and 0x80 != 0) {
                running = b
                first = -1
                continue
            }
            val type = running and 0xf0
            if (type !in listOf(0x80, 0x90, 0xa0, 0xb0, 0xe0)) continue
            if (first < 0) {
                first = b
                continue
            }
            val d1 = first
            val d2 = b
            first = -1
            when (type) {
                0x80 -> handler.post { listener?.note(NoteEvent(d1, false, d2)) }
                0x90 -> handler.post { listener?.note(NoteEvent(d1, d2 > 0, d2)) }
                0xb0 -> if (d1 == 64) handler.post { listener?.sustain(d2 >= 64) }
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
private fun PianoAlphaApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val midi = remember { MidiInputManager(context.applicationContext) }
    val settingsStore = remember { PianoSettingsStore(context.applicationContext) }
    val sessionStore = remember { PianoSessionStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val settings by settingsStore.settings.collectAsState(initial = PianoPracticeSettings())

    var screen by remember { mutableStateOf("home") }
    var devices by remember { mutableStateOf<List<MidiDeviceSummary>>(emptyList()) }
    var connected by remember { mutableStateOf<MidiDeviceSummary?>(null) }
    var midiEvent by remember { mutableStateOf<NoteEvent?>(null) }
    var midiSustain by remember { mutableStateOf<Boolean?>(null) }
    var recentSessions by remember { mutableStateOf<List<PracticeSessionSummary>>(emptyList()) }

    fun refreshSessions() {
        scope.launch {
            recentSessions = withContext(Dispatchers.IO) { sessionStore.recentSessions() }
        }
    }

    LaunchedEffect(sessionStore) { refreshSessions() }

    DisposableEffect(midi, sessionStore) {
        midi.start(object : MidiInputManager.Listener {
            override fun devices(value: List<MidiDeviceSummary>) { devices = value }
            override fun connected(value: MidiDeviceSummary?) { connected = value }
            override fun note(value: NoteEvent) { midiEvent = value.copy() }
            override fun sustain(value: Boolean) { midiSustain = value }
        })
        onDispose {
            midi.close()
            sessionStore.close()
        }
    }

    val saveSession: (Long, Long, Int, List<RecordedNoteEvent>) -> Unit = { startedAt, endedAt, bpm, events ->
        scope.launch {
            withContext(Dispatchers.IO) { sessionStore.saveSession(startedAt, endedAt, bpm, events) }
            refreshSessions()
        }
    }

    if (screen == "piano") {
        PianoScreen(
            onBack = { screen = "home" },
            devices = devices,
            connected = connected,
            onConnect = midi::connect,
            onDisconnect = midi::disconnect,
            midiEvent = midiEvent,
            midiSustain = midiSustain,
            settings = settings,
            onBpmChange = { value -> scope.launch { settingsStore.setBpm(value) } },
            onSustainChange = { value -> scope.launch { settingsStore.setSustain(value) } },
            onMetronomeChange = { value -> scope.launch { settingsStore.setMetronome(value) } },
            onSaveSession = saveSession,
        )
    } else {
        HomeScreen(
            devices = devices,
            connected = connected,
            recentSessions = recentSessions,
            onPiano = { screen = "piano" },
            onConnect = midi::connect,
            onDisconnect = midi::disconnect,
        )
    }
}

@Composable
private fun HomeScreen(
    devices: List<MidiDeviceSummary>,
    connected: MidiDeviceSummary?,
    recentSessions: List<PracticeSessionSummary>,
    onPiano: () -> Unit,
    onConnect: (Int) -> Unit,
    onDisconnect: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(PianoBlack).systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text("PIANO STUDIO", style = MaterialTheme.typography.labelMedium, color = Champagne)
        Text("Your piano is ready.", style = MaterialTheme.typography.displaySmall, color = WarmWhite)
        Text("Alpha 0.1C · Practice-ready Piano Core", color = Muted)

        Card(colors = CardDefaults.cardColors(containerColor = Charcoal), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Piano, null, tint = Champagne, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Low-latency instrument", style = MaterialTheme.typography.titleLarge)
                        Text("Native AAudio · metronome · session recording", color = Muted)
                    }
                }
                Button(onClick = onPiano, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("Open piano")
                }
            }
        }

        if (recentSessions.isNotEmpty()) {
            val latest = recentSessions.first()
            Text("PRACTICE", style = MaterialTheme.typography.labelMedium, color = Champagne)
            Card(colors = CardDefaults.cardColors(containerColor = Carbon), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Last session", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${durationLabel(latest.durationMs)} · ${latest.eventCount} note events · ${latest.bpm} BPM",
                        color = Muted,
                    )
                    Text("${recentSessions.size} recent session${if (recentSessions.size == 1) "" else "s"} stored offline", style = MaterialTheme.typography.labelSmall, color = Success)
                }
            }
        }

        Text("MIDI", style = MaterialTheme.typography.labelMedium, color = Champagne)
        if (connected != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Charcoal)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Success)
                    Spacer(Modifier.width(12.dp))
                    Text(connected.name, Modifier.weight(1f))
                    TextButton(onClick = onDisconnect) { Text("Disconnect") }
                }
            }
        } else if (devices.isEmpty()) {
            Text("Connect a USB/Bluetooth MIDI keyboard, or use the on-screen piano.", color = Muted)
        } else {
            devices.take(3).forEach { device ->
                OutlinedButton(onClick = { onConnect(device.id) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Usb, null)
                    Spacer(Modifier.width(8.dp))
                    Text(device.name)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text("Engine build 03 · no ads · offline-first", style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

@Composable
private fun PianoScreen(
    onBack: () -> Unit,
    devices: List<MidiDeviceSummary>,
    connected: MidiDeviceSummary?,
    onConnect: (Int) -> Unit,
    onDisconnect: () -> Unit,
    midiEvent: NoteEvent?,
    midiSustain: Boolean?,
    settings: PianoPracticeSettings,
    onBpmChange: (Int) -> Unit,
    onSustainChange: (Boolean) -> Unit,
    onMetronomeChange: (Boolean) -> Unit,
    onSaveSession: (Long, Long, Int, List<RecordedNoteEvent>) -> Unit,
) {
    val audio = remember { PianoAudioEngine() }
    var audioReady by remember { mutableStateOf(false) }
    var sustain by remember { mutableStateOf(settings.sustain) }
    var metronome by remember { mutableStateOf(settings.metronome) }
    var bpm by remember { mutableIntStateOf(settings.bpm) }
    var recording by remember { mutableStateOf(false) }
    var recordingStartedAt by remember { mutableLongStateOf(0L) }
    val recordedEvents = remember { mutableStateListOf<RecordedNoteEvent>() }
    var touchNotes by remember { mutableStateOf(setOf<Int>()) }
    var midiNotes by remember { mutableStateOf(setOf<Int>()) }
    var lastMidi by remember { mutableIntStateOf(-1) }

    DisposableEffect(audio) {
        audioReady = audio.start()
        audio.sustain(sustain)
        audio.metronome(metronome, bpm)
        onDispose {
            audio.metronome(false, bpm)
            audio.allOff()
            audio.stop()
        }
    }

    LaunchedEffect(settings.bpm) {
        bpm = settings.bpm
        audio.metronome(metronome, bpm)
    }
    LaunchedEffect(settings.sustain) {
        sustain = settings.sustain
        audio.sustain(sustain)
    }
    LaunchedEffect(settings.metronome) {
        metronome = settings.metronome
        audio.metronome(metronome, bpm)
    }

    fun handle(event: NoteEvent, source: String) {
        if (event.pressed) audio.noteOn(event.midi, max(1, event.velocity)) else audio.noteOff(event.midi)
        if (recording && recordingStartedAt > 0L) {
            recordedEvents += RecordedNoteEvent(
                offsetMs = System.currentTimeMillis() - recordingStartedAt,
                midi = event.midi,
                pressed = event.pressed,
                velocity = event.velocity,
                source = source,
            )
        }
        lastMidi = event.midi
    }

    fun beginRecording() {
        recordedEvents.clear()
        recordingStartedAt = System.currentTimeMillis()
        recording = true
    }

    fun finishRecording() {
        if (!recording) return
        val endedAt = System.currentTimeMillis()
        val startedAt = recordingStartedAt.takeIf { it > 0L } ?: endedAt
        val snapshot = recordedEvents.toList()
        recording = false
        recordingStartedAt = 0L
        onSaveSession(startedAt, endedAt, bpm, snapshot)
    }

    fun leavePiano() {
        if (recording) finishRecording()
        onBack()
    }

    BackHandler(onBack = ::leavePiano)

    LaunchedEffect(midiEvent) {
        val event = midiEvent ?: return@LaunchedEffect
        midiNotes = if (event.pressed) midiNotes + event.midi else midiNotes - event.midi
        handle(event, "midi")
    }

    LaunchedEffect(midiSustain) {
        val pedal = midiSustain ?: return@LaunchedEffect
        sustain = pedal
        audio.sustain(pedal)
        onSustainChange(pedal)
    }

    Column(Modifier.fillMaxSize().background(PianoBlack).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::leavePiano) { Icon(Icons.Rounded.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text("Concert Grand", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (lastMidi >= 0) "Last note · ${midiLabel(lastMidi)}" else "C3–C5 · ready",
                    color = Muted,
                )
            }
            AssistChip(
                onClick = {
                    if (connected != null) onDisconnect() else devices.firstOrNull()?.let { onConnect(it.id) }
                },
                label = { Text(connected?.name ?: if (devices.isEmpty()) "MIDI" else "Connect") },
                leadingIcon = {
                    Icon(
                        if (connected != null) Icons.Rounded.CheckCircle else Icons.Rounded.Usb,
                        null,
                        Modifier.size(16.dp),
                    )
                },
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Toggle("Sustain", sustain, Modifier.weight(1f)) {
                sustain = !sustain
                audio.sustain(sustain)
                onSustainChange(sustain)
            }
            Toggle("Metro", metronome, Modifier.weight(1f)) {
                metronome = !metronome
                audio.metronome(metronome, bpm)
                onMetronomeChange(metronome)
            }
            Toggle("Record", recording, Modifier.weight(1f)) {
                if (recording) finishRecording() else beginRecording()
            }
        }

        Card(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            colors = CardDefaults.cardColors(containerColor = Carbon),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    bpm = (bpm - 1).coerceAtLeast(40)
                    audio.metronome(metronome, bpm)
                    onBpmChange(bpm)
                }) { Icon(Icons.Rounded.Remove, "Decrease tempo") }

                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$bpm BPM", style = MaterialTheme.typography.titleMedium, color = Champagne)
                    Slider(
                        value = bpm.toFloat(),
                        onValueChange = {
                            bpm = it.toInt().coerceIn(40, 220)
                            audio.metronome(metronome, bpm)
                        },
                        onValueChangeFinished = { onBpmChange(bpm) },
                        valueRange = 40f..220f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                IconButton(onClick = {
                    bpm = (bpm + 1).coerceAtMost(220)
                    audio.metronome(metronome, bpm)
                    onBpmChange(bpm)
                }) { Icon(Icons.Rounded.Add, "Increase tempo") }
            }
        }

        PianoKeyboard(
            Modifier.fillMaxWidth().weight(1f),
            pressed = touchNotes + midiNotes,
            onTouch = { touchNotes = it },
            onEvent = { handle(it, "touch") },
        )

        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (audioReady) "Native AAudio · active" else "Audio unavailable",
                color = if (audioReady) Muted else Error,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                when {
                    recording -> "REC · ${recordedEvents.size} events"
                    metronome -> "Metro · $bpm BPM"
                    else -> "${(touchNotes + midiNotes).size} active"
                },
                color = if (recording) Error else Champagne,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun Toggle(label: String, active: Boolean, modifier: Modifier, click: () -> Unit) {
    FilledTonalButton(
        onClick = click,
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (active) Champagne.copy(alpha = .18f) else Charcoal,
            contentColor = if (active) Champagne else Muted,
        ),
    ) { Text(label, maxLines = 1) }
}

@Composable
private fun PianoKeyboard(
    modifier: Modifier,
    pressed: Set<Int>,
    onTouch: (Set<Int>) -> Unit,
    onEvent: (NoteEvent) -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun isBlack(midi: Int) = (midi % 12 + 12) % 12 in setOf(1, 3, 6, 8, 10)

    fun keys(width: Float, height: Float): List<KeyGeometry> {
        if (width <= 0f || height <= 0f) return emptyList()
        val start = 48
        val whiteCount = 15
        val whiteWidth = width / whiteCount
        val whites = mutableListOf<KeyGeometry>()
        val whiteLeft = mutableMapOf<Int, Float>()
        var whiteIndex = 0
        var midi = start
        while (whiteIndex < whiteCount) {
            if (!isBlack(midi)) {
                val left = whiteIndex * whiteWidth
                whiteLeft[midi] = left
                whites += KeyGeometry(midi, Rect(left, 0f, left + whiteWidth, height), false)
                whiteIndex++
            }
            midi++
        }
        val end = whites.last().midi
        val blacks = (start..end).filter(::isBlack).mapNotNull { note ->
            val previous = (note - 1 downTo start).firstOrNull { whiteLeft.containsKey(it) } ?: return@mapNotNull null
            val center = whiteLeft.getValue(previous) + whiteWidth
            val blackWidth = whiteWidth * .62f
            KeyGeometry(
                note,
                Rect(center - blackWidth / 2, 0f, center + blackWidth / 2, height * .62f),
                true,
            )
        }
        return whites + blacks
    }

    fun hit(position: Offset, values: List<KeyGeometry>): Int? =
        values.firstOrNull { it.black && it.rect.contains(position) }?.midi
            ?: values.firstOrNull { !it.black && it.rect.contains(position) }?.midi

    Canvas(
        modifier.onSizeChanged { size = it }.pointerInput(size) {
            awaitEachGesture {
                var active = mutableMapOf<PointerId, Int>()
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val geometry = keys(this.size.width.toFloat(), this.size.height.toFloat())
                    val next = mutableMapOf<PointerId, Int>()
                    event.changes.forEach { change ->
                        if (change.pressed) {
                            hit(change.position, geometry)?.let { next[change.id] = it }
                            change.consume()
                        }
                    }
                    val before = active.values.groupingBy { it }.eachCount()
                    val after = next.values.groupingBy { it }.eachCount()
                    val changed = before.keys + after.keys
                    changed.forEach { note ->
                        val was = before[note] ?: 0
                        val now = after[note] ?: 0
                        if (was == 0 && now > 0) onEvent(NoteEvent(note, true, 100))
                        else if (was > 0 && now == 0) onEvent(NoteEvent(note, false, 0))
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
        val geometry = keys(size.width.toFloat(), size.height.toFloat())
        geometry.filter { !it.black }.forEach { drawWhite(it, pressed.contains(it.midi)) }
        geometry.filter { it.black }.forEach { drawBlack(it, pressed.contains(it.midi)) }
    }
}

private fun DrawScope.drawWhite(key: KeyGeometry, pressed: Boolean) {
    drawRect(if (pressed) Color(0xFFD9C99E) else Ivory, key.rect.topLeft, key.rect.size)
    drawRect(Color(0xFF6F6B62), key.rect.topLeft, key.rect.size, style = Stroke(1f))
    if (key.midi % 12 == 0) {
        drawCircle(Color(0xFFB7AA8B), 4f, Offset(key.rect.center.x, key.rect.bottom - 20f))
    }
}

private fun DrawScope.drawBlack(key: KeyGeometry, pressed: Boolean) {
    drawRoundRect(
        if (pressed) Champagne else Color(0xFF171714),
        key.rect.topLeft,
        key.rect.size,
        androidx.compose.ui.geometry.CornerRadius(5f, 5f),
    )
}

private fun midiLabel(midi: Int): String {
    val names = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    return names[(midi % 12 + 12) % 12] + (midi / 12 - 1)
}

private fun durationLabel(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) "${minutes}m ${seconds}s" else "${seconds}s"
}
