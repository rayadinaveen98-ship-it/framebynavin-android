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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.max

private val r2Diagnostic = listOf(60, 62, 64, 67, 64, 62, 60, 60, 62, 64, 67, 64, 62, 60)

class R2PracticeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { R2Theme { R2Practice(onBack = ::finish) } }
    }
}

private data class R2PracticeMidiDevice(val id: Int, val name: String)
private data class R2PracticeMidiEvent(val midi: Int, val pressed: Boolean, val velocity: Int, val token: Long)

private class R2PracticeAudio {
    private var started = false
    fun start(): Boolean { if (!started) started = nativeStart(); return started }
    fun stop() { if (started) { nativeSetMetronome(false,80); nativeAllNotesOff(); nativeStop(); started = false } }
    fun noteOn(midi: Int, velocity: Int = 100) { if (started) nativeNoteOn(midi.coerceIn(0,127), velocity.coerceIn(1,127)) }
    fun noteOff(midi: Int) { if (started) nativeNoteOff(midi.coerceIn(0,127)) }
    fun metronome(on: Boolean, bpm: Int) { if (started) nativeSetMetronome(on, bpm.coerceIn(50,120)) }
    fun allOff() { if (started) nativeAllNotesOff() }
    private external fun nativeStart(): Boolean
    private external fun nativeStop()
    private external fun nativeNoteOn(midi: Int, velocity: Int)
    private external fun nativeNoteOff(midi: Int)
    private external fun nativeSetMetronome(enabled: Boolean, bpm: Int)
    private external fun nativeAllNotesOff()
    companion object { init { System.loadLibrary("pianostudio_audio") } }
}

private class R2PracticeMidi(context: Context) : AutoCloseable {
    private val manager = context.getSystemService(MidiManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var device: MidiDevice? = null
    private var port: MidiOutputPort? = null
    private var connected: R2PracticeMidiDevice? = null
    private var status = 0
    private var d1 = -1
    private var token = 0L
    var onDevices: (List<R2PracticeMidiDevice>) -> Unit = {}
    var onConnected: (R2PracticeMidiDevice?) -> Unit = {}
    var onEvent: (R2PracticeMidiEvent) -> Unit = {}

    private val receiver = object : MidiReceiver() {
        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            for (i in offset until (offset + count).coerceAtMost(data.size)) {
                val v = data[i].toInt() and 0xff
                if (v >= 0xf8) continue
                if (v and 0x80 != 0) { status = v; d1 = -1; continue }
                val type = status and 0xf0
                if (type !in listOf(0x80,0x90,0xa0,0xb0,0xe0)) continue
                if (d1 < 0) { d1 = v; continue }
                val first = d1; val second = v; d1 = -1
                if (type == 0x80 || type == 0x90) {
                    token++
                    val pressed = type == 0x90 && second > 0
                    val event = R2PracticeMidiEvent(first, pressed, second, token)
                    handler.post { onEvent(event) }
                }
            }
        }
    }
    private val callback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) = publish()
        override fun onDeviceRemoved(device: MidiDeviceInfo) { if (device.id == connected?.id) disconnect(); publish() }
        override fun onDeviceStatusChanged(status: MidiDeviceStatus) = publish()
    }
    fun start() { runCatching { manager.registerDeviceCallback(callback, handler) }; publish() }
    private fun devices(): List<R2PracticeMidiDevice> = runCatching {
        manager.devices.mapNotNull { info ->
            if (info.ports.none { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }) return@mapNotNull null
            val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
                ?: "MIDI ${info.id}"
            R2PracticeMidiDevice(info.id, name)
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
            connected = devices().firstOrNull { it.id == id } ?: R2PracticeMidiDevice(id,"MIDI piano")
            onConnected(connected)
        }, handler)
    }
    fun disconnect() {
        runCatching { port?.disconnect(receiver) }; runCatching { port?.close() }; runCatching { device?.close() }
        port = null; device = null; connected = null; onConnected(null)
    }
    override fun close() { disconnect(); runCatching { manager.unregisterDeviceCallback(callback) } }
}

@Composable
private fun R2Practice(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val audio = remember { R2PracticeAudio() }
    val midi = remember { R2PracticeMidi(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val store = remember { PracticeCoachStore(context.applicationContext) }
    var runtime by remember { mutableStateOf(GuidedPracticeRuntime(r2Diagnostic, 80)) }
    var feedback by remember { mutableStateOf("Listen to the click, then play C4.") }
    var good by remember { mutableStateOf<Boolean?>(null) }
    var pressed by remember { mutableStateOf(setOf<Int>()) }
    var devices by remember { mutableStateOf<List<R2PracticeMidiDevice>>(emptyList()) }
    var connected by remember { mutableStateOf<R2PracticeMidiDevice?>(null) }
    var lastToken by remember { mutableStateOf(-1L) }
    var revision by remember { mutableIntStateOf(0) }
    var report by remember { mutableStateOf<PracticeReport?>(null) }
    var focusMode by remember { mutableStateOf(false) }

    fun process(note: Int, down: Boolean, velocity: Int = 100) {
        if (down) audio.noteOn(note, max(1,velocity)) else audio.noteOff(note)
        if (!down || report != null) return
        val result = runtime.onNote(note, true)
        good = result.correct
        feedback = when {
            result.completed -> "Nice — phrase complete."
            result.correct -> runtime.expected?.let { "Good. Next: ${noteLabel(it)}." } ?: "Good."
            else -> runtime.expected?.let { "Not quite — stay on ${noteLabel(it)}." } ?: result.feedback
        }
        revision++
        if (result.completed) {
            audio.metronome(false, runtime.bpm)
            val ready = runtime.report()
            report = ready
            scope.launch { store.save(ready) }
        }
    }

    DisposableEffect(Unit) {
        audio.start(); audio.metronome(true, runtime.bpm)
        midi.onDevices = { devices = it }
        midi.onConnected = { connected = it }
        midi.onEvent = { event ->
            if (event.token != lastToken) {
                lastToken = event.token
                pressed = if (event.pressed) pressed + event.midi else pressed - event.midi
                process(event.midi, event.pressed, event.velocity)
            }
        }
        midi.start()
        onDispose { audio.stop(); midi.close() }
    }

    BackHandler(onBack = onBack)
    @Suppress("UNUSED_VARIABLE") val redraw = revision

    val currentReport = report
    val expected = runtime.expected
    val progress = if (runtime.sequence.isEmpty()) 1f else runtime.index.toFloat() / runtime.sequence.size.toFloat()
    val attempts = runtime.index + runtime.mistakes

    R2LandscapeScaffold(
        title = if (focusMode) "Focused practice" else "Smart Practice",
        subtitle = "${runtime.bpm} BPM · ${if (connected != null) connected!!.name else "guided diagnostic"}",
        progress = progress,
        onBack = onBack,
        actions = {
            R2ActionChip(
                label = if (connected != null) "MIDI ✓" else if (devices.isEmpty()) "MIDI" else "Connect MIDI",
                onClick = { if (connected != null) midi.disconnect() else devices.firstOrNull()?.let { midi.connect(it.id) } },
                highlighted = connected != null,
            )
            if (currentReport != null) {
                R2ActionChip("Retry", onClick = {
                    val seq = currentReport.focusSequence.ifEmpty { r2Diagnostic }
                    focusMode = currentReport.focusSequence.isNotEmpty()
                    runtime = GuidedPracticeRuntime(seq, currentReport.recommendedBpm)
                    feedback = "Ready. Follow the highlighted note."
                    good = null; report = null; pressed = emptySet(); revision++
                    audio.allOff(); audio.metronome(true, runtime.bpm)
                }, highlighted = true)
            }
        },
        instruction = {
            if (currentReport == null) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Play ${expected?.let(::noteLabel) ?: "the highlighted note"}", color = R2Gold, style = MaterialTheme.typography.labelLarge)
                        Text(feedback, color = R2White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    }
                    Surface(color = when (good) { true -> R2Success.copy(alpha=.15f); false -> R2Error.copy(alpha=.15f); null -> R2Carbon }, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) {
                        Text(
                            when {
                                good == true -> "Correct"
                                good == false -> "Try again"
                                attempts < 3 -> "Guided"
                                else -> "${runtime.noteAccuracy}% notes"
                            },
                            color = when (good) { true -> R2Success; false -> R2Error; null -> R2Muted },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            } else {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("COACH RECOMMENDATION", color = R2Gold, style = MaterialTheme.typography.labelMedium)
                        Text(currentReport.message, color = R2White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${currentReport.noteAccuracy}% notes", color = R2White)
                        Text("${currentReport.timingScore}% timing", color = R2Muted)
                        Text("Next ${currentReport.recommendedBpm} BPM", color = R2Gold)
                    }
                }
            }
        },
        keyboard = {
            R2ResponsiveKeyboard(
                pressed = pressed,
                targets = expected?.let { setOf(it) } ?: emptySet(),
                centerMidi = expected ?: 60,
                onTouchState = { touch -> pressed = touch },
                onEvent = { note, down -> process(note, down) },
            )
        },
        footerLeft = if (currentReport == null && attempts < 3) "Guided mode" else if (currentReport == null) "Notes ${runtime.noteAccuracy}%" else "${currentReport.mistakes} misses",
        footerCenter = if (currentReport == null) expected?.let { "Play ${noteLabel(it)}" } ?: "Keep the beat" else "${currentReport.recommendedBpm} BPM next",
        footerRight = if (currentReport == null && attempts < 3) "Explore freely" else if (currentReport == null) "Mistakes ${runtime.mistakes}" else currentReport.weakestMidi?.let { "Focus ${noteLabel(it)}" } ?: "Clean phrase",
        footerRightEmphasis = currentReport == null && attempts >= 3 && runtime.mistakes > 0,
    )
}
