package com.pianostudio.core.music

private val NOTE_NAMES = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")

fun noteLabel(midi: Int?): String {
    if (midi == null) return "—"
    val safe = midi.coerceIn(0, 127)
    return NOTE_NAMES[(safe % 12 + 12) % 12] + (safe / 12 - 1)
}

fun isBlackKey(midi: Int): Boolean = (midi % 12 + 12) % 12 in setOf(1, 3, 6, 8, 10)

fun previousWhiteKey(midi: Int): Int {
    var note = midi.coerceIn(21, 108)
    while (isBlackKey(note) && note > 21) note--
    return note
}

fun whiteKeyCountForWidth(widthDp: Float, minWhiteKeyWidthDp: Float = 48f): Int {
    val raw = (widthDp / minWhiteKeyWidthDp).toInt().coerceIn(7, 15)
    return when {
        raw >= 15 -> 15
        raw >= 13 -> 13
        raw >= 11 -> 11
        raw >= 9 -> 9
        else -> 7
    }
}
