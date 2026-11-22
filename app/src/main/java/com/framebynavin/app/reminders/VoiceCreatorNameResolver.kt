package com.framebynavin.app.reminders

import android.content.Context
import com.framebynavin.app.cloud.CloudLocalStore
import com.framebynavin.app.data.CreatorOsSettingsStore

/** Resolves the spoken creator identity from durable local state; no live cloud request required. */
internal object VoiceCreatorNameResolver {
    fun resolve(context: Context): String =
        resolveKnownName(context).ifBlank { "Creator" }

    /** Safe for all three reminder domains: blank means no name is available. */
    fun resolveKnownName(context: Context): String {
        val app = context.applicationContext
        val localCloud = CloudLocalStore(app)
        val session = runCatching { localCloud.loadSession() }.getOrNull()
        val cachedProfile = runCatching { localCloud.loadCreatorProfile() }.getOrNull()
            ?.takeIf { profile -> session != null && profile.userId == session.userId }
        val creatorProfileName = runCatching {
            CreatorOsSettingsStore(app).snapshot().creatorProfile.displayName
        }.getOrDefault("")
        return VoiceGreetingBuilder.preferredKnownName(
            googleAccountName = session?.displayName.orEmpty(),
            cachedAccountName = cachedProfile?.displayName.orEmpty(),
            creatorProfileName = creatorProfileName,
        )
    }
}
