package com.framebynavin.app.youtube

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeReachReportingTest {
    @Test
    fun csvParserHandlesQuotedCommasAndEscapedQuotes() {
        assertEquals(
            listOf("2026-09-10", "channel", "video", "1000", "6.5", "label, with comma", "say \"hi\""),
            YouTubeReachReportingClient.parseCsvLine(
                "2026-09-10,channel,video,1000,6.5,\"label, with comma\",\"say \"\"hi\"\"\""
            ),
        )
    }

    @Test
    fun reachReportTypeIsPinnedToOfficialChannelBasicReachDataset() {
        assertEquals("channel_reach_basic_a1", YouTubeReachReportingClient.REPORT_TYPE)
    }
}
