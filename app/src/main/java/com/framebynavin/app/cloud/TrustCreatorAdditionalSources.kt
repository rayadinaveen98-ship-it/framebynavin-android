package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.data.CreatorAutomationPreferencesStore
import com.framebynavin.app.data.CreatorRewardStore
import com.framebynavin.app.youtube.YouTubePublishCheckpointStore
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reuses the Creator backup's EXISTING author-owned record formats, without
 * instantiating settings stores that seed on read or capturing an OAuth token.
 *
 * This is source material for one owner-authenticated encrypted archive; it
 * does not push, restore, change alarms or automatically publish any video.
 */
internal object TrustCreatorAdditionalSources {
    internal data class Source(
        val id: String,
        val raw: String,
        val count: Int,
    )

    suspend fun capture(context: Context): List<Source> {
        val app = context.applicationContext
        val rewards = CreatorRewardStore(app)
        val rewardsRaw = rewards.exportJson()
        val automation = CreatorAutomationPreferencesStore(app)
        val automationRaw = automation.exportJson()
        automation.validateJson(automationRaw)

        // Backlot's existing CreatorBackupManager uses precisely these local
        // keys; never serialize broader YouTube connection/authorization prefs.
        val linksRaw = app.getSharedPreferences(
            "youtube_analytics_v11", Context.MODE_PRIVATE,
        ).getString("video_project_links", "{}").orEmpty()
        val links = JSONObject(linksRaw)
        require(links.length() <= 20_000) { "Too many YouTube project links" }

        val milestones = JSONObject()
        app.getSharedPreferences(
            "youtube_milestones_v12", Context.MODE_PRIVATE,
        ).all.keys.sorted().forEach { key ->
            val raw = app.getSharedPreferences(
                "youtube_milestones_v12", Context.MODE_PRIVATE,
            ).all.getValue(key) as? String
                ?: error("Saved YouTube milestone has an unknown storage type")
            require(key.isNotBlank() && key.length <= 256 &&
                raw.length <= 16_384) {
                "Invalid portable milestone"
            }
            milestones.put(key, raw)
        }
        val checkpoints = YouTubePublishCheckpointStore(app).exportJson()
        require(checkpoints.length() <= 100_000) {
            "Saved publishing history exceeds portable backup limits"
        }

        return listOf(
            Source(
                "creator/history/rewards", rewardsRaw,
                rewards.validateJson(rewardsRaw),
            ),
            Source("creator/config/automation", automationRaw, 3),
            Source("creator/history/youtube-links", linksRaw, links.length()),
            Source(
                "creator/history/youtube-milestones", milestones.toString(),
                milestones.length(),
            ),
            Source(
                "creator/history/youtube-checkpoints", checkpoints.toString(),
                checkpoints.length(),
            ),
        )
    }

    fun validate(source: Source) {
        when (source.id) {
            "creator/history/rewards" -> require(JSONArray(source.raw).length() == source.count)
            "creator/config/automation" ->
                require(JSONObject(source.raw).length() == source.count)
            "creator/history/youtube-links",
            "creator/history/youtube-milestones" ->
                require(JSONObject(source.raw).length() == source.count)
            "creator/history/youtube-checkpoints" ->
                require(JSONArray(source.raw).length() == source.count)
            else -> error("Unrecognized extra Creator backup domain")
        }
    }
}
