package com.framebynavin.app.cloud

import org.json.JSONObject

/** Preserves the distinction between absent legacy metadata and an explicitly empty value. */
internal object CloudBackupEnvelope {
    data class Parsed(
        val localBackup: String,
        val youtubeProjectLinks: String?,
        val youtubeMilestones: String?,
    )

    fun parse(raw: String): Parsed {
        val root = JSONObject(raw)
        require(root.optString("format") == CloudConfig.CLOUD_FORMAT) { "Not a FrameByNavin cloud backup" }
        require(root.optInt("schemaVersion", -1) in 1..CloudConfig.CLOUD_SCHEMA_VERSION) {
            "Unsupported cloud backup version"
        }
        return Parsed(
            localBackup = root.getString("localBackup"),
            youtubeProjectLinks = optionalObject(root, "youtubeProjectLinks"),
            youtubeMilestones = optionalObject(root, "youtubeMilestones"),
        )
    }

    private fun optionalObject(root: JSONObject, key: String): String? {
        if (!root.has(key)) return null
        // JSON null, malformed JSON and non-object values are invalid, not omissions.
        val value = root.getString(key)
        JSONObject(value)
        return value
    }
}
