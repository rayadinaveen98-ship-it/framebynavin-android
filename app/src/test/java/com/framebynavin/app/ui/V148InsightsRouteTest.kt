package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class V148InsightsRouteTest {
    @Test
    fun resolvesTheExactRequestedVideoId() {
        assertEquals(
            "video-b",
            v148ResolveRequestedVideoId(
                availableVideoIds = listOf("video-a", "video-b", "video-c"),
                requestedVideoId = "video-b",
            ),
        )
    }

    @Test
    fun neverFallsBackToAWrongVideo() {
        assertNull(
            v148ResolveRequestedVideoId(
                availableVideoIds = listOf("video-a", "video-b"),
                requestedVideoId = "missing-video",
            ),
        )
    }
}
