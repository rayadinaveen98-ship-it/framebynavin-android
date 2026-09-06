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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

class R2LessonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val lessonId = intent.getStringExtra(EXTRA_LESSON_ID) ?: LessonCatalog.foundations.first().id
        setContent { R2Theme { R2Lesson(lessonId = lessonId, onBack = ::finish) } }
    }

    companion object { const val EXTRA_LESSON_ID = "lesson_id" }
}

private data class R2LessonMidiDevice(val id: Int, val name: String)
private data class R2LessonMidiEvent(val midi: Int, val pressed: Boolean, val velocity: Int, val token: Long)

private class R2LessonAudio {
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

private class R2LessonMidi(context: Context) : AutoCloseable {
    private val manager = context.getSystemService(MidiManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var device: MidiDevice? = null
    private var port: MidiOutputPort? = null
    private var connected: R2LessonMidiDevice? = null
    private var status = 0
    private var d1 = -1
    private var token = 0L
    var onDevices: (List<R2LessonMidiDevice>) -> Unit = {}
    var onConnected: (R2LessonMidiDevice?) -> Unit = {}
    var onEvent: (R2LessonMidiEvent) -> Unit = {}

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
                    val event = R2LessonMidiEvent(first, pressed, second, token)
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
    private fun devices(): List<R2LessonMidiDevice> = runCatching {
        manager.devices.mapNotNull { info ->
            if (info.ports.none { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }) return@mapNotNull null
            val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
                ?: "MIDI ${info.id}"
            R2LessonMidiDevice(info.id, name)
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
            connected = devices().firstOrNull { it.id == id } ?: R2LessonMidiDevice(id,"MIDI piano")
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
private fun R2Lesson(lessonId: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lesson = remember(lessonId) { LessonCatalog.byId(lessonId) ?: LessonCatalog.foundations.first() }
    val store = remember { LessonProgressStore(context.applicationContext) }
    val allProgress by store.progress.collectAsState(initial = emptyMap())
    val saved = allProgress[lesson.id] ?: LessonProgress()
    val runtime = remember(lesson.id, saved.completed) {
        LessonRuntime(lesson, if (saved.completed) 0 else saved.stepIndex.coerceAtMost(lesson.steps.lastIndex))
    }
    val audio = remember { R2LessonAudio() }
    val midi = remember { R2LessonMidi(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var feedback by remember { mutableStateOf("Play when you're ready.") }
    var good by remember { mutableStateOf<Boolean?>(null) }
    var completed by remember { mutableStateOf(false) }
    var touchNotes by remember { mutableStateOf(setOf<Int>()) }
    var midiNotes by remember { mutableStateOf(setOf<Int>()) }
    var devices by remember { mutableStateOf<List<R2LessonMidiDevice>>(emptyList()) }
    var connected by remember { mutableStateOf<R2LessonMidiDevice?>(null) }
    var lastToken by remember { mutableLongStateOf(-1L) }
    var revision by remember { mutableIntStateOf(0) }

    fun save(result: LessonInputResult) {
        if (result.stepCompleted || result.lessonCompleted) {
            scope.launch { store.save(lesson, runtime.stepIndex, result.lessonCompleted, runtime.accuracy) }
        }
    }

    fun apply(result: LessonInputResult) {
        good = result.correct
        feedback = when {
            result.lessonCompleted -> "Beautiful — lesson complete."
            result.correct == true && result.stepCompleted -> "Great — you have it."
            result.correct == true && runtime.currentExpected.isNotEmpty() -> "Nice. Next: ${runtime.currentExpected.joinToString(" + ") { noteLabel(it) }}."
            result.correct == false && result.expected.isNotEmpty() -> "Almost — try ${result.expected.joinToString(" + ") { noteLabel(it) }}."
            result.feedback.isNotBlank() -> result.feedback
            else -> "Play when you're ready."
        }
        save(result)
        if (result.lessonCompleted) completed = true
        revision++
    }

    fun process(note: Int, down: Boolean, velocity: Int = 100) {
        if (down) audio.noteOn(note, max(1,velocity)) else audio.noteOff(note)
        apply(runtime.onNote(note, down))
    }

    fun demo() {
        val targets = runtime.currentExpected.ifEmpty { runtime.currentStep?.targets?.flatten().orEmpty() }.distinct()
        if (targets.isEmpty()) return
        scope.launch {
            audio.allOff()
            val chord = runtime.currentStep?.kind == LessonStepKind.CHORD
            if (chord) {
                targets.forEach { audio.noteOn(it,88) }; delay(650); targets.forEach(audio::noteOff)
            } else {
                targets.forEach { n -> audio.noteOn(n,88); delay(250); audio.noteOff(n); delay(90) }
            }
        }
    }

    DisposableEffect(Unit) {
        audio.start()
        midi.onDevices = { devices = it }
        midi.onConnected = { connected = it }
        midi.onEvent = { event ->
            if (event.token != lastToken) {
                lastToken = event.token
                midiNotes = if (event.pressed) midiNotes + event.midi else midiNotes - event.midi
                process(event.midi, event.pressed, event.velocity)
            }
        }
        midi.start()
        onDispose { audio.stop(); midi.close() }
    }

    BackHandler(onBack = onBack)
    @Suppress("UNUSED_VARIABLE") val redraw = revision

    val step = runtime.currentStep
    val attempts = runtime.correctHits + runtime.mistakes
    val expected = runtime.currentExpected
    val progress = if (completed) 1f else (runtime.stepIndex.toFloat() / lesson.steps.size.toFloat()).coerceIn(0f,1f)

    R2LandscapeScaffold(
        title = lesson.title,
        subtitle = "Step ${(runtime.stepIndex + 1).coerceAtMost(lesson.steps.size)} of ${lesson.steps.size}",
        progress = progress,
        onBack = onBack,
        actions = {
            R2ActionChip("Demo", onClick = ::demo)
            R2ActionChip(
                label = if (connected != null) "MIDI ✓" else if (devices.isEmpty()) "MIDI" else "Connect MIDI",
                onClick = { if (connected != null) midi.disconnect() else devices.firstOrNull()?.let { midi.connect(it.id) } },
                highlighted = connected != null,
            )
        },
        instruction = {
            if (completed) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = R2Success)
                    Column {
                        Text("Lesson complete", color = R2Gold, style = MaterialTheme.typography.labelLarge)
                        Text("${lesson.title} · ${runtime.accuracy}% accuracy", color = R2White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (step?.kind == LessonStepKind.INFO) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    Text(step.title, color = R2Gold, style = MaterialTheme.typography.labelLarge)
                    Text(step.instruction, color = R2White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    if (!step.hint.isNullOrBlank()) Text(step.hint, color = R2Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            } else {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(step?.title.orEmpty(), color = R2Gold, style = MaterialTheme.typography.labelLarge)
                        Text(step?.instruction.orEmpty(), color = R2White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Text(feedback, color = when (good) { true -> R2Success; false -> R2Error; null -> R2Muted }, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    Surface(color = R2Carbon, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            expected.forEach { note ->
                                Icon(Icons.Rounded.MusicNote, null, tint = R2Gold)
                                Text(noteLabel(note), color = R2White, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        },
        keyboard = {
            if (step?.kind == LessonStepKind.INFO && !completed) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Button(onClick = { apply(runtime.continueInfo()) }) { Text("Continue") }
                }
            } else {
                R2ResponsiveKeyboard(
                    pressed = touchNotes + midiNotes,
                    targets = expected.toSet(),
                    centerMidi = expected.firstOrNull() ?: 60,
                    onTouchState = { touchNotes = it },
                    onEvent = { note, down -> process(note, down) },
                )
            }
        },
        footerLeft = when {
            completed -> "Complete"
            attempts < 3 -> "Guided mode"
            else -> "Accuracy ${runtime.accuracy}%"
        },
        footerCenter = when {
            completed -> "Beautiful work"
            step?.kind == LessonStepKind.INFO -> "Learn first. Play second."
            expected.isNotEmpty() -> "Play ${expected.joinToString(" + ") { noteLabel(it) }}"
            else -> "Play when ready"
        },
        footerRight = when {
            completed -> "${runtime.accuracy}%"
            attempts < 3 -> "Explore freely"
            else -> "Mistakes ${runtime.mistakes}"
        },
        footerRightEmphasis = !completed && attempts >= 3 && runtime.mistakes > 0,
    )
}
