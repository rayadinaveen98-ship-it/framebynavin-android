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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max

class R2FreePianoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { R2Theme { R2FreePiano(onBack = ::finish) } }
    }
}

private data class R2MidiDevice(val id: Int, val name: String)
private data class R2MidiNote(val midi: Int, val pressed: Boolean, val velocity: Int, val token: Long)

private class R2AudioEngine {
    private var started = false
    fun start(): Boolean { if (!started) started = nativeStart(); return started }
    fun stop() { if (started) { nativeSetMetronome(false, 80); nativeAllNotesOff(); nativeStop(); started = false } }
    fun noteOn(midi: Int, velocity: Int = 100) { if (started) nativeNoteOn(midi.coerceIn(0,127), velocity.coerceIn(1,127)) }
    fun noteOff(midi: Int) { if (started) nativeNoteOff(midi.coerceIn(0,127)) }
    fun sustain(on: Boolean) { if (started) nativeSetSustain(on) }
    fun metronome(on: Boolean, bpm: Int) { if (started) nativeSetMetronome(on, bpm.coerceIn(40,220)) }
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

private class R2Midi(context: Context) : AutoCloseable {
    private val manager = context.getSystemService(MidiManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var device: MidiDevice? = null
    private var port: MidiOutputPort? = null
    private var connected: R2MidiDevice? = null
    private var status = 0
    private var d1 = -1
    private var token = 0L
    var onDevices: (List<R2MidiDevice>) -> Unit = {}
    var onConnected: (R2MidiDevice?) -> Unit = {}
    var onNote: (R2MidiNote) -> Unit = {}

    private val receiver = object : MidiReceiver() {
        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            for (i in offset until (offset + count).coerceAtMost(data.size)) {
                val value = data[i].toInt() and 0xff
                if (value >= 0xf8) continue
                if (value and 0x80 != 0) { status = value; d1 = -1; continue }
                val type = status and 0xf0
                if (type !in listOf(0x80,0x90,0xa0,0xb0,0xe0)) continue
                if (d1 < 0) { d1 = value; continue }
                val first = d1; val second = value; d1 = -1
                if (type == 0x80 || type == 0x90) {
                    token++
                    val pressed = type == 0x90 && second > 0
                    val event = R2MidiNote(first, pressed, second, token)
                    handler.post { onNote(event) }
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
    private fun devices(): List<R2MidiDevice> = runCatching {
        manager.devices.mapNotNull { info ->
            if (info.ports.none { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }) return@mapNotNull null
            val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
                ?: "MIDI ${info.id}"
            R2MidiDevice(info.id, name)
        }.sortedBy { it.name.lowercase() }
    }.getOrDefault(emptyList())
    private fun publish() { onDevices(devices()) }
    fun connect(id: Int) {
        val info = runCatching { manager.devices.firstOrNull { it.id == id } }.getOrNull() ?: return
        disconnect()
        manager.openDevice(info, { opened ->
            if (opened == null) return@openDevice
            val pInfo = info.ports.firstOrNull { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT } ?: return@openDevice
            val openedPort = opened.openOutputPort(pInfo.portNumber) ?: return@openDevice
            openedPort.connect(receiver)
            device = opened; port = openedPort
            connected = devices().firstOrNull { it.id == id } ?: R2MidiDevice(id, "MIDI piano")
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
private fun R2FreePiano(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val audio = remember { R2AudioEngine() }
    val midi = remember { R2Midi(context.applicationContext) }
    var audioReady by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<R2MidiDevice>>(emptyList()) }
    var connected by remember { mutableStateOf<R2MidiDevice?>(null) }
    var pressed by remember { mutableStateOf(setOf<Int>()) }
    var sustain by remember { mutableStateOf(false) }
    var metronome by remember { mutableStateOf(false) }
    var bpm by remember { mutableIntStateOf(80) }
    var lastToken by remember { mutableStateOf(-1L) }

    DisposableEffect(Unit) {
        audioReady = audio.start()
        midi.onDevices = { devices = it }
        midi.onConnected = { connected = it }
        midi.onNote = { event ->
            if (event.token != lastToken) {
                lastToken = event.token
                pressed = if (event.pressed) pressed + event.midi else pressed - event.midi
                if (event.pressed) audio.noteOn(event.midi, max(1,event.velocity)) else audio.noteOff(event.midi)
            }
        }
        midi.start()
        onDispose { audio.stop(); midi.close() }
    }

    BackHandler(onBack = onBack)

    R2LandscapeScaffold(
        title = "Free Piano",
        subtitle = when {
            connected != null -> connected!!.name
            audioReady -> "Concert Grand · low-latency audio"
            else -> "Audio unavailable"
        },
        progress = null,
        onBack = onBack,
        actions = {
            R2ActionChip(if (metronome) "$bpm BPM" else "Metronome", onClick = {
                metronome = !metronome
                audio.metronome(metronome, bpm)
            }, highlighted = metronome)
            R2ActionChip(if (sustain) "Sustain On" else "Sustain", onClick = {
                sustain = !sustain
                audio.sustain(sustain)
            }, highlighted = sustain)
            R2ActionChip(
                label = if (connected != null) "MIDI ✓" else if (devices.isEmpty()) "MIDI" else "Connect MIDI",
                onClick = { if (connected != null) midi.disconnect() else devices.firstOrNull()?.let { midi.connect(it.id) } },
                highlighted = connected != null,
            )
        },
        instruction = {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Play without interruption.", style = MaterialTheme.typography.titleLarge, color = R2White, fontWeight = FontWeight.SemiBold)
                    Text("The keyboard stays fixed. Controls stay out of the way.", color = R2Muted, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.width(12.dp))
                Text(if (pressed.isEmpty()) "Ready" else pressed.sorted().joinToString(" · ") { noteLabel(it) }, color = R2Gold, style = MaterialTheme.typography.titleMedium)
            }
        },
        keyboard = {
            R2ResponsiveKeyboard(
                pressed = pressed,
                centerMidi = if (pressed.isEmpty()) 60 else pressed.first(),
                onTouchState = { touch -> pressed = (pressed.filter { it !in touch }.toSet()) + touch },
                onEvent = { note, down ->
                    pressed = if (down) pressed + note else pressed - note
                    if (down) audio.noteOn(note) else audio.noteOff(note)
                },
            )
        },
        footerLeft = if (connected != null) "MIDI connected" else "On-screen input",
        footerCenter = if (metronome) "$bpm BPM" else "Free play",
        footerRight = if (sustain) "Sustain on" else "Sustain off",
    )
}
