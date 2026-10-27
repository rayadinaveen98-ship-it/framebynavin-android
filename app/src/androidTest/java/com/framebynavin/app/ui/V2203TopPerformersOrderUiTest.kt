package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import com.framebynavin.app.youtube.*
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic YouTube analytics saved on isolated CI emulator only. */
@RunWith(AndroidJUnit4::class)
class V2203TopPerformersOrderUiTest {
    @get:Rule val rule = createComposeRule()
    @Test fun connectedTopThreeAreAboveUpNextAndStayInInsightsOrder() {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = YouTubeAnalyticsStore(context)
        val now = System.currentTimeMillis()
        val films = listOf(300L, 200L, 100L).mapIndexed { index, views ->
            YouTubeVideoSnapshot(
                videoId = "v2203-$index", title = "V2203 film $index",
                publishedAtMillis = now - 86_400_000L,
                periodViews = views, lifetimeViews = views,
                watchMinutes = views * 2, averageViewDurationSeconds = 60L,
                subscribersGained = 0L, subscribersLost = 0L,
                likes = 2L, comments = 1L,
            )
        }
        val snapshot = YouTubeAnalyticsSnapshot(
            channel = YouTubeChannelSnapshot("v2203-synthetic", "Test channel",
                subscribers = 10L, lifetimeViews = 600L, videoCount = 3L,
                uploadsPlaylistId = "v2203-playlist"),
            windowDays = 28,
            startDate = "2026-10-01", endDate = "2026-10-27",
            views = 600L, watchMinutes = 1200L, averageViewDurationSeconds = 60L,
            subscribersGained = 0L, subscribersLost = 0L, likes = 6L,
            comments = 3L, topVideos = films, recentVideos = films,
            trend = emptyList(), fetchedAtMillis = now,
        )
        try {
            runBlocking {
                assertTrue(store.save(snapshot, store.beginRequest(allowChannelChange = true)))
            }
            rule.setContent {
                FrameByNavinTheme {
                    PTodayScreen(
                        creatorProfile = CreatorProfile(
                            displayName = "Film Creator",
                            primaryCreatorMode = "Filmmaker",
                            platforms = setOf("YouTube"),
                            primaryGoal = "Tell stories",
                            weeklyPublishingTarget = 2,
                        ),
                        tasks = listOf(CreatorTask(id = "v2203-project",
                            title = "Project after the top videos",
                            platform = "YouTube", contentType = "Film",
                            dueLabel = "Tomorrow")),
                        onAdd = {}, onStart = {}, onAdvance = {},
                        onViewAllReminders = {}, onFocus = {},
                    )
                }
            }
            val performers = rule.onNodeWithText("TOP PERFORMERS")
                .getUnclippedBoundsInRoot()
            val upNext = rule.onNodeWithTag("v215-up-next-header")
                .getUnclippedBoundsInRoot()
            assertTrue("YouTube Top 3 must remain above Up Next: $performers vs $upNext",
                performers.bottom < upNext.top)
            rule.onNodeWithText("1/3").assertExists()
            val name = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
            check(name == "default" || name == "compact")
            val folder = File(context.filesDir, "v2203-top-video-evidence")
            assertTrue(folder.isDirectory || folder.mkdirs())
            val png = rule.onRoot().captureToImage().asAndroidBitmap()
            val output = File(folder, "v2203-$name-top-three-above-up-next.png")
            output.outputStream().use { assertTrue(png.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            assertTrue(output.length() > 10_000)
        } finally {
            store.disconnect()
        }
    }
}
