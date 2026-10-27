package com.framebynavin.app.youtube

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorBackupManager
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.HardeningTestEnvironment
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class V188RevenueAndReachOwnerTest {
    private fun core(channel: String) = YouTubeAnalyticsSnapshot(
        channel = YouTubeChannelSnapshot(channel, channel, 10, 100, 1, "uploads-$channel"),
        windowDays = 28, startDate = "2026-09-01", endDate = "2026-09-28",
        views = 100, watchMinutes = 5, averageViewDurationSeconds = 30,
        subscribersGained = 0, subscribersLost = 0, likes = 0, comments = 0,
        topVideos = emptyList(), recentVideos = emptyList(), trend = emptyList(),
        fetchedAtMillis = System.currentTimeMillis(),
    )
    private fun revenue(channel: String) = YouTubeRevenueSnapshot(
        period = YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS,
        startDate = "2026-09-01", endDate = "2026-09-28",
        currencyCode = "INR", views = 100, estimatedRevenue = 1.0,
        estimatedAdRevenue = 0.0, playbackBasedCpm = 0.0, impressionCpm = 0.0,
        monetizedPlaybacks = 0, adImpressions = 0, trend = emptyList(),
        fetchedAtMillis = System.currentTimeMillis(), channelId = channel,
    )
    private suspend fun fixture(block: suspend (Context, CreatorBackupManager, YouTubeAnalyticsStore,
        YouTubeRevenueStore, YouTubeReachStore) -> Unit) {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val backup = CreatorBackupManager(context)
        val original = backup.createBackup()
        val coreStore = YouTubeAnalyticsStore(context)
        val revenueStore = YouTubeRevenueStore(context)
        val reachStore = YouTubeReachStore(context)
        try {
            coreStore.disconnect()
            revenueStore.clear()
            reachStore.clear()
            block(context, backup, coreStore, revenueStore, reachStore)
        } finally {
            withContext(NonCancellable) {
                backup.restore(original)
                revenueStore.clear()
                reachStore.clear()
            }
        }
    }

    @Test fun revenueWriteRequiresExactChannelAndLatestGeneration(): Unit = runBlocking {
        fixture { context, _, store, earnings, _ ->
            val first = store.beginRequest()
            assertTrue(store.save(core("channel-A"), first))
            val generation = CreatorDataGate.generation(context)
            assertFalse(earnings.saveIfOwner(revenue("channel-B"), generation))
            assertTrue(earnings.saveIfOwner(revenue("channel-A"), generation))
            assertNotNull(earnings.load(YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS, "channel-A"))
            val second = store.beginRequest(allowChannelChange = true)
            assertTrue(store.save(core("channel-B"), second))
            assertNull(earnings.load(YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS, "channel-B"))
            assertNull(earnings.load(YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS, "channel-A"))
            assertTrue(earnings.saveIfOwner(revenue("channel-B"), generation))
            assertNotNull(earnings.load(YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS, "channel-B"))
            store.disconnect()
            assertFalse(earnings.saveIfOwner(revenue("channel-B"), generation))
            assertNull(earnings.load(YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS, "channel-B"))
        }
    }

    @Test fun revenueWriteRejectedAfterBackupRestore(): Unit = runBlocking {
        fixture { context, backup, store, earnings, _ ->
            val active = store.beginRequest()
            assertTrue(store.save(core("channel-A"), active))
            val generation = CreatorDataGate.generation(context)
            val saved = backup.createBackup()
            backup.restore(saved)
            assertFalse(earnings.saveIfOwner(revenue("channel-A"), generation))
            assertNull(earnings.load(YouTubeRevenuePeriod.TWENTY_EIGHT_DAYS, "channel-A"))
        }
    }

    @Test fun reachWriteRequiresActiveRequestAndActualReportCoverage(): Unit = runBlocking {
        fixture { _, _, store, _, reach ->
            val first = store.beginRequest()
            assertTrue(store.save(core("channel-A"), first))
            assertTrue(store.applyReachIfCurrent(first, "channel-A") {
                reach.bindChannel("channel-A")
                assertTrue(reach.replaceDay("2026-09-04", "report-${UUID.randomUUID()}",
                    "2026-09-05T01:00:00Z", emptyList()))
            })
            val coverage = reach.summary("2026-09-01", "2026-09-28")
            assertNotNull(coverage)
            assertEquals(1, coverage!!.coveredDays)
            assertEquals(28, coverage.requestedDays)
            assertEquals(0L, coverage.impressions)
            store.disconnect()
            assertFalse(store.applyReachIfCurrent(first, "channel-A") {
                reach.bindChannel("channel-A")
            })
        }
    }
}
