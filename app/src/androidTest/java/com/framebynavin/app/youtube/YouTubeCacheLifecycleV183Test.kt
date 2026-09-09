package com.framebynavin.app.youtube

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorBackupManager
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorWriteConflict
import com.framebynavin.app.data.HardeningTestEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class YouTubeCacheLifecycleV183Test {
    private fun snapshot(channel: String, days: Int = 28, views: Long = 100L,
                         videoId: String = "synthetic-${UUID.randomUUID()}"): YouTubeAnalyticsSnapshot {
        val now = System.currentTimeMillis()
        val video = YouTubeVideoSnapshot(videoId, "Synthetic video", now - 35L * 86_400_000L,
            views, views, 10L, 30L, 0L, 0L, 1L, 0L)
        return YouTubeAnalyticsSnapshot(
            channel = YouTubeChannelSnapshot(channel, "Synthetic $channel", 10L, views, 1L, "uploads-$channel"),
            windowDays = days, startDate = "2026-01-01", endDate = "2026-01-28",
            views = views, watchMinutes = 10L, averageViewDurationSeconds = 30L,
            subscribersGained = 0L, subscribersLost = 0L, likes = 1L, comments = 0L,
            topVideos = listOf(video), recentVideos = listOf(video), trend = emptyList(),
            fetchedAtMillis = now,
        )
    }

    private suspend fun fixture(block: suspend (Context, CreatorBackupManager, YouTubeAnalyticsStore) -> Unit) {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = CreatorBackupManager(context)
        val original = manager.createBackup()
        val store = YouTubeAnalyticsStore(context)
        try {
            store.disconnect()
            block(context, manager, store)
        } finally {
            withContext(NonCancellable) { manager.restore(original) }
        }
    }

    @Test fun delayedSyncCannotResurrectDisconnectedCacheOrCaptureMilestones(): Unit = runBlocking {
        fixture { context, _, store ->
            val videoId = "disconnect-${UUID.randomUUID()}"
            store.link(videoId, "synthetic-project", CreatorDataGate.generation(context))
            val request = store.beginRequest()
            store.disconnect()
            assertFalse(store.save(snapshot("channel-A", videoId = videoId), request))
            assertFalse(store.isCurrent(request))
            assertFalse(store.hasConnection())
            assertNull(store.loadAny())
            assertNull(YouTubeAnalyticsStore.latest24HourReport)
            assertEquals("synthetic-project", store.links()[videoId])
            assertTrue(YouTubeMilestoneStore(context).load(videoId).isEmpty())
            assertEquals(0, YouTubePulseStore(context).sampleCount())
        }
    }

    @Test fun accountSwitchReplacesOnlyDerivedCacheAndRejectsOldRequests(): Unit = runBlocking {
        fixture { context, _, store ->
            val id = "switch-${UUID.randomUUID()}"
            store.link(id, "project-A", CreatorDataGate.generation(context))
            val first = store.beginRequest()
            assertTrue(store.save(snapshot("channel-A", 28), first))
            val stale = store.beginRequest()
            val newer = store.beginRequest(allowChannelChange = true)
            assertTrue(store.save(snapshot("channel-B", 7), newer))
            assertFalse(store.save(snapshot("channel-A", 90), stale))
            assertNull(store.load(28))
            assertNull(store.load(90))
            assertEquals("channel-B", store.load(7)?.channel?.channelId)
            assertEquals("project-A", store.links()[id])
            val refresh = store.beginRequest()
            val failure = runCatching { store.save(snapshot("channel-C", 7), refresh) }.exceptionOrNull()
            assertTrue(failure is CreatorWriteConflict)
            assertEquals("channel-B", store.load(7)?.channel?.channelId)
        }
    }

    @Test fun newerRequestWinsAndGenerationIsSharedAcrossInstances(): Unit = runBlocking {
        fixture { context, _, store ->
            val old = store.beginRequest()
            val current = store.beginRequest()
            val other = YouTubeAnalyticsStore(context)
            assertFalse(other.isCurrent(old))
            assertTrue(other.isCurrent(current))
            withTimeout(15_000) {
                val first = async(Dispatchers.Default) { store.save(snapshot("channel-A", views = 10L), old) }
                val second = async(Dispatchers.Default) { other.save(snapshot("channel-A", views = 20L), current) }
                assertFalse(first.await())
                assertTrue(second.await())
            }
            store.cancelRequest(old)
            assertTrue(other.isCurrent(current))
            assertEquals(20L, store.load(28)?.views)
            other.cancelRequest(current)
            assertFalse(store.isCurrent(current))
        }
    }

    @Test fun restoreInvalidatesPendingSyncButPreservesLinksAndMilestones(): Unit = runBlocking {
        fixture { context, manager, store ->
            val id = "restore-${UUID.randomUUID()}"
            store.link(id, "preserved-project", CreatorDataGate.generation(context))
            val milestonePrefs = context.getSharedPreferences("youtube_milestones_v12", Context.MODE_PRIVATE)
            assertTrue(milestonePrefs.edit().putString("${id}_24", "synthetic-milestone").commit())
            val checkpoint = manager.createBackup()
            val request = store.beginRequest()
            manager.restore(checkpoint)
            assertFalse(store.save(snapshot("channel-A"), request))
            assertNull(store.loadAny())
            assertEquals("preserved-project", store.links()[id])
            assertEquals("synthetic-milestone", milestonePrefs.getString("${id}_24", null))
            val fresh = store.beginRequest()
            assertTrue(store.save(snapshot("channel-A"), fresh))
        }
    }

    @Test fun queuedLinkEditCannotReplayAfterRestore(): Unit = runBlocking {
        fixture { context, manager, store ->
            val id = "link-${UUID.randomUUID()}"
            store.link(id, "original-project", CreatorDataGate.generation(context))
            val checkpoint = manager.createBackup()
            val oldGeneration = CreatorDataGate.generation(context)
            manager.restore(checkpoint)
            val failure = runCatching { store.link(id, "stale-project", oldGeneration) }.exceptionOrNull()
            assertTrue(failure is CreatorWriteConflict)
            assertEquals("original-project", store.links()[id])
        }
    }

    @Test fun legacyUnownedCacheIsDiscardedWithoutDeletingCreatorMetadata(): Unit = runBlocking {
        fixture { context, _, store ->
            val id = "legacy-${UUID.randomUUID()}"
            store.link(id, "preserved-project", CreatorDataGate.generation(context))
            val prefs = context.getSharedPreferences("youtube_analytics_v11", Context.MODE_PRIVATE)
            assertTrue(prefs.edit().putString("channel_id", "old-channel")
                .putString("snapshot_28", "{}")
                .remove("creator_generation_v183").commit())
            val migrated = YouTubeAnalyticsStore(context)
            assertFalse(migrated.hasConnection())
            assertNull(migrated.loadAny())
            assertEquals("preserved-project", migrated.links()[id])
            assertFalse(prefs.contains("snapshot_28"))
        }
    }
}
