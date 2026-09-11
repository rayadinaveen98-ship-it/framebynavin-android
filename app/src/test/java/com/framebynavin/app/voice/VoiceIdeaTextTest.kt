package com.framebynavin.app.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceIdeaTextTest {
    @Test
    fun blankDraftUsesRecognizedSpeechDirectly() {
        assertEquals("why this scene works", VoiceIdeaText.append("", "  why this scene works  "))
    }

    @Test
    fun voiceAppendsWithoutOverwritingTypedIdea() {
        assertEquals(
            "Video about OG opening breakdown add the lighting comparison",
            VoiceIdeaText.append("Video about OG opening breakdown", "add the lighting comparison"),
        )
    }

    @Test
    fun recognizedWhitespaceIsCleanedButWordsAreNotRewritten() {
        assertEquals(
            "Pawan Kalyan OG scene analysis",
            VoiceIdeaText.append("Pawan Kalyan", "OG   scene\nanalysis"),
        )
    }

    @Test
    fun emptyRecognitionDoesNotChangeDraft() {
        assertEquals("Existing idea", VoiceIdeaText.append("Existing idea", "   "))
    }
}
