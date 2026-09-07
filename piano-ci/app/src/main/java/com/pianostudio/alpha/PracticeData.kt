package com.pianostudio.alpha

// Source-compatible migration facade. The database and preferences now have
// exactly one implementation in core:data. Keep the existing storage names so
// an upgrade does not discard a learner's recorded sessions or settings.
typealias PianoPracticeSettings = com.pianostudio.core.data.PianoPracticeSettings
typealias PianoSettingsStore = com.pianostudio.core.data.PianoSettingsStore
typealias RecordedNoteEvent = com.pianostudio.core.data.RecordedNoteEvent
typealias PracticeSessionSummary = com.pianostudio.core.data.PracticeSessionSummary
typealias PianoSessionStore = com.pianostudio.core.data.PianoSessionStore
