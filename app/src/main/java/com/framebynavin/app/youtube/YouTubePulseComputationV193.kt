package com.framebynavin.app.youtube

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A local comparison of captured lifetime counters. This is not YouTube
 * Realtime Analytics and must not be presented as an exact rolling 24h count.
 */
internal object YouTubePulseComputation {
    private const val HOUR_MS = 3_600_000L
    private const val MAX_FRESH_AGE_MS = 36L * HOUR_MS
    private const val MAX_FUTURE_SKEW_MS = 5L * 60_000L

    fun build(samples: List<YouTubePulseSample>, nowMillis: Long): YouTube24HourReport? {
        val latest = samples.maxByOrNull { it.capturedAtMillis } ?: return null
        if (latest.channelId.isBlank() ||
            nowMillis - latest.capturedAtMillis > MAX_FRESH_AGE_MS ||
            latest.capturedAtMillis - nowMillis > MAX_FUTURE_SKEW_MS) return null

        // Never blend historical records from different connected channels.
        val ordered = samples.filter { it.channelId == latest.channelId }
            .distinctBy { it.capturedAtMillis }
            .sortedBy { it.capturedAtMillis }
        val baseline = closest(
            ordered.dropLast(1), latest.capturedAtMillis, targetHours = 24,
        ) ?: return null
        val windowMillis = latest.capturedAtMillis - baseline.capturedAtMillis
        val previousBase = closest(
            ordered.filter { it.capturedAtMillis < baseline.capturedAtMillis },
            baseline.capturedAtMillis,
            targetHours = 24,
        )
        val previousMillis = previousBase?.let { baseline.capturedAtMillis - it.capturedAtMillis }
        val rawDelta = latest.lifetimeViews - baseline.lifetimeViews
        val correction = rawDelta.coerceAtMost(0L)
        val views = rawDelta.coerceAtLeast(0L)
        val previousRaw = previousBase?.let { baseline.lifetimeViews - it.lifetimeViews }
        val previousViews = previousRaw?.takeIf { it >= 0L }
        // Normalize by actual, potentially different, 18–30-hour durations.
        val rateChange = if (rawDelta >= 0L && previousViews != null && previousViews > 0L &&
            previousMillis != null && previousMillis > 0L) {
            val currentRate = views.toDouble() / windowMillis.toDouble()
            val previousRate = previousViews.toDouble() / previousMillis.toDouble()
            (((currentRate - previousRate) * 100.0) / previousRate).roundToInt()
        } else null

        val momentum = when {
            rawDelta < 0L -> YouTubePulseMomentum.CORRECTION
            rateChange == null -> YouTubePulseMomentum.INSUFFICIENT
            rateChange >= 25 -> YouTubePulseMomentum.RISING
            rateChange <= -25 -> YouTubePulseMomentum.COOLING
            else -> YouTubePulseMomentum.STEADY
        }

        val oldVideos = baseline.videos.associateBy { it.videoId }
        val movers = if (rawDelta < 0L) emptyList() else latest.videos.mapNotNull { video ->
            val older = oldVideos[video.videoId]
            val gained = when {
                older != null -> (video.lifetimeViews - older.lifetimeViews).coerceAtLeast(0L)
                video.publishedAtMillis >= baseline.capturedAtMillis ->
                    video.lifetimeViews.coerceAtLeast(0L)
                else -> 0L
            }
            if (gained <= 0L) return@mapNotNull null
            YouTubePulseMover(
                videoId = video.videoId,
                title = video.title,
                viewsGained = gained,
                channelGainSharePercent = if (views > 0L)
                    ((gained * 100.0) / views.toDouble()).roundToInt().coerceIn(0, 100)
                else 0,
            )
        }.sortedByDescending { it.viewsGained }.take(5)

        return YouTube24HourReport(
            sampleHours = (windowMillis / HOUR_MS.toDouble()).roundToInt().coerceAtLeast(1),
            viewsGained = views,
            subscribersDelta = latest.subscribers - baseline.subscribers,
            previousViewsGained = previousViews,
            viewsChangePercent = rateChange,
            momentum = momentum,
            topMovers = movers,
            currentCapturedAtMillis = latest.capturedAtMillis,
            baselineCapturedAtMillis = baseline.capturedAtMillis,
            previousWindowHours = previousMillis
                ?.let { (it / HOUR_MS.toDouble()).roundToInt().coerceAtLeast(1) },
            counterAdjustment = correction,
            channelId = latest.channelId,
        )
    }

    private fun closest(
        samples: List<YouTubePulseSample>,
        endMillis: Long,
        targetHours: Int,
    ): YouTubePulseSample? = samples
        .filter { endMillis - it.capturedAtMillis in (18L * HOUR_MS)..(30L * HOUR_MS) }
        .minByOrNull { abs((endMillis - it.capturedAtMillis) - targetHours * HOUR_MS) }
}
