package com.framebynavin.app.ui

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2293SparkVoicePolicyTest {
    @Test fun neverFallBackToPotentialCloudRecognizerOnAndroid11() {
        assertFalse(V2293SparkVoicePolicy.supportsLocalInput(30, true))
        assertFalse(V2293SparkVoicePolicy.supportsLocalInput(29, false))
    }

    @Test fun disabledIfNoInstalledOnDeviceRecognizer() {
        assertFalse(V2293SparkVoicePolicy.supportsLocalInput(31, false))
        assertTrue(V2293SparkVoicePolicy.supportsLocalInput(31, true))
        assertTrue(V2293SparkVoicePolicy.supportsLocalInput(35, true))
    }

    @Test fun transcriptIsReviewableAndBounded() {
        assertEquals("my next project", V2293SparkVoicePolicy.transcript(" my next project "))
        assertEquals(V2292SparkConversationPolicy.INPUT_MAX,
            V2293SparkVoicePolicy.transcript("x".repeat(1000)).length)
        assertEquals("", V2293SparkVoicePolicy.transcript(null))
    }

    @Test fun onlyShortTextIsPassedToOfflineReadAloud() {
        assertEquals("Hello Spark", V2293SparkVoicePolicy.spokenReply("  Hello Spark "))
        assertEquals(450, V2293SparkVoicePolicy.spokenReply("a".repeat(2000)).length)
    }
}
